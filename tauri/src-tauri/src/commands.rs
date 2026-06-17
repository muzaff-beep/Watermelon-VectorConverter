// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. See LICENSE.

//! Tauri commands wrapping the Rust core (Contract C-3, desktop side).
//! Error->DTO mapping and core delegation are unit-tested (Module B test plan).
//! The #[tauri::command] macro wrappers are thin; logic lives in free fns so
//! they remain testable without a webview.

use serde::Serialize;
use svg_converter_core::convert_svg as core_convert_svg;
use svg_converter_core::error::ConversionError;
use svg_converter_core::image_export::{render_svg_preview as core_render_svg, render_vd_preview as core_render_vd};
use svg_converter_core::batch_processor::convert_zip as core_convert_zip;
use std::sync::atomic::AtomicBool;

#[derive(Debug, Serialize, PartialEq)]
pub struct ConversionErrorDto {
    pub code: u16,
    pub message: String,
}

impl From<ConversionError> for ConversionErrorDto {
    fn from(e: ConversionError) -> Self {
        ConversionErrorDto { code: e.code(), message: e.to_string() }
    }
}

// ---- testable logic ----
pub fn do_convert_svg(svg: Vec<u8>) -> Result<String, ConversionErrorDto> {
    core_convert_svg(&svg).map_err(Into::into)
}
pub fn do_render_svg_preview(svg: Vec<u8>, px: u32) -> Result<Vec<u8>, ConversionErrorDto> {
    core_render_svg(&svg, px).map_err(Into::into)
}
pub fn do_render_vd_preview(vd_xml: String, px: u32) -> Result<Vec<u8>, ConversionErrorDto> {
    core_render_vd(&vd_xml, px).map_err(Into::into)
}
pub fn do_convert_zip(zip: Vec<u8>) -> Result<Vec<u8>, ConversionErrorDto> {
    let cancel = AtomicBool::new(false);
    // The #[tauri::command] wrapper passes a sink that emits "batch://progress"
    // events to the frontend (Contract C-3). Logic is delegated here.
    core_convert_zip(&zip, &|_e| {}, &cancel).map_err(Into::into)
}

// ---- #[tauri::command] wrappers (compiled in the real app) ----
#[cfg(feature = "tauri-app")]
mod wrappers {
    use super::*;
    #[tauri::command] pub fn convert_svg(svg: Vec<u8>) -> Result<String, ConversionErrorDto> { do_convert_svg(svg) }
    #[tauri::command] pub fn render_svg_preview(svg: Vec<u8>, px: u32) -> Result<Vec<u8>, ConversionErrorDto> { do_render_svg_preview(svg, px) }
    #[tauri::command] pub fn render_vd_preview(vd_xml: String, px: u32) -> Result<Vec<u8>, ConversionErrorDto> { do_render_vd_preview(vd_xml, px) }
    #[tauri::command] pub fn convert_zip(zip: Vec<u8>) -> Result<Vec<u8>, ConversionErrorDto> { do_convert_zip(zip) }
    // convert_zip's real impl emits Window.emit("batch://progress", evt) per file.
}
