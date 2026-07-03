// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. See LICENSE.

//! Tauri commands wrapping the Rust core (Contract C-3, desktop side).
//! Logic lives in free fns (do_*) so they remain testable without a webview.
//! #[tauri::command] wrappers are thin delegation only.

use serde::{Deserialize, Serialize};
use svg_converter_core::batch_processor::{convert_zip as core_convert_zip, ProgressEvent};
use svg_converter_core::convert_svg as core_convert_svg;
use svg_converter_core::error::ConversionError;
use svg_converter_core::image_export::{
    render_svg_preview as core_render_svg, render_vd_preview as core_render_vd,
};
use std::sync::atomic::AtomicBool;
use tauri::Emitter;

// ── Error DTO ────────────────────────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, PartialEq)]
pub struct ConversionErrorDto {
    pub code: u16,
    pub message: String,
}

impl From<ConversionError> for ConversionErrorDto {
    fn from(e: ConversionError) -> Self {
        ConversionErrorDto {
            code: e.code(),
            message: e.to_string(),
        }
    }
}

/// Progress payload emitted to the frontend as the "batch://progress" event.
#[derive(Clone, Serialize)]
pub struct BatchProgressDto {
    pub done: u32,
    pub total: u32,
    pub current_name: String,
}

impl From<ProgressEvent> for BatchProgressDto {
    fn from(e: ProgressEvent) -> Self {
        BatchProgressDto {
            done: e.done,
            total: e.total,
            current_name: e.current_name,
        }
    }
}

// ── Testable logic (no Tauri dependency) ────────────────────────────────────

pub fn do_convert_svg(svg: Vec<u8>) -> Result<String, ConversionErrorDto> {
    core_convert_svg(&svg).map_err(Into::into)
}

pub fn do_render_svg_preview(svg: Vec<u8>, px: u32) -> Result<Vec<u8>, ConversionErrorDto> {
    core_render_svg(&svg, px).map_err(Into::into)
}

pub fn do_render_vd_preview(vd_xml: String, px: u32) -> Result<Vec<u8>, ConversionErrorDto> {
    core_render_vd(&vd_xml, px).map_err(Into::into)
}

// ── Tauri command wrappers ───────────────────────────────────────────────────

/// Convert a single SVG file (bytes) → VectorDrawable XML string.
#[tauri::command]
pub fn convert_svg(svg: Vec<u8>) -> Result<String, ConversionErrorDto> {
    do_convert_svg(svg)
}

/// Render an SVG preview PNG at the requested pixel size.
#[tauri::command]
pub fn render_svg_preview(svg: Vec<u8>, px: u32) -> Result<Vec<u8>, ConversionErrorDto> {
    do_render_svg_preview(svg, px)
}

/// Render a VectorDrawable XML preview PNG at the requested pixel size.
#[tauri::command]
pub fn render_vd_preview(vd_xml: String, px: u32) -> Result<Vec<u8>, ConversionErrorDto> {
    do_render_vd_preview(vd_xml, px)
}

/// Batch-convert a ZIP of SVG files → ZIP of VectorDrawable XML files.
/// Emits "batch://progress" events to the frontend as each file completes.
#[tauri::command]
pub fn convert_zip(
    window: tauri::Window,
    zip: Vec<u8>,
) -> Result<Vec<u8>, ConversionErrorDto> {
    let cancel = AtomicBool::new(false);
    let result = core_convert_zip(
        &zip,
        &|e: ProgressEvent| {
            // Emit progress to the frontend. Ignore emit errors (window may close).
            let _ = window.emit("batch://progress", BatchProgressDto::from(e));
        },
        &cancel,
    );
    result.map_err(Into::into)
}

/// Open a URL in the system default browser.
#[tauri::command]
pub fn open_url(url: String) -> Result<(), ConversionErrorDto> {
    // Use the opener crate via tauri-plugin-shell if available,
    // otherwise fall back to std::process::Command.
    #[cfg(target_os = "windows")]
    std::process::Command::new("cmd")
        .args(["/c", "start", "", &url])
        .spawn()
        .map_err(|e| ConversionErrorDto { code: 1099, message: e.to_string() })?;

    #[cfg(target_os = "macos")]
    std::process::Command::new("open")
        .arg(&url)
        .spawn()
        .map_err(|e| ConversionErrorDto { code: 1099, message: e.to_string() })?;

    #[cfg(target_os = "linux")]
    std::process::Command::new("xdg-open")
        .arg(&url)
        .spawn()
        .map_err(|e| ConversionErrorDto { code: 1099, message: e.to_string() })?;

    Ok(())
}
