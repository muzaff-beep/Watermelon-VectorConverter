// Watermelon Vector Viewer
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.

use crate::PendingFile;
use serde::Serialize;
use std::fs;
use svg_converter_core::image_export::{render_svg_preview, render_vd_preview};
use tauri::State;

#[derive(Debug, Serialize)]
pub struct ViewerErrorDto {
    pub message: String,
}

impl From<std::io::Error> for ViewerErrorDto {
    fn from(e: std::io::Error) -> Self {
        ViewerErrorDto { message: e.to_string() }
    }
}

impl From<svg_converter_core::error::ConversionError> for ViewerErrorDto {
    fn from(e: svg_converter_core::error::ConversionError) -> Self {
        ViewerErrorDto { message: e.to_string() }
    }
}

/// Read a file from disk, detect whether it's a raw SVG or a VectorDrawable
/// XML, and render it to PNG bytes for display.
#[tauri::command]
pub fn render_file_preview(path: String, px: u32) -> Result<Vec<u8>, ViewerErrorDto> {
    let content = fs::read_to_string(&path).map_err(ViewerErrorDto::from)?;

    // VectorDrawable XML starts with <vector ...>, plain SVG starts with <svg ...>.
    // Check the root tag rather than the file extension, since a user could
    // rename either type.
    let trimmed = content.trim_start();
    let is_vector_drawable = trimmed
        .lines()
        .find(|l| !l.trim().is_empty() && !l.trim_start().starts_with("<?xml"))
        .map(|l| l.trim_start().starts_with("<vector"))
        .unwrap_or(false);

    if is_vector_drawable {
        render_vd_preview(&content, px).map_err(ViewerErrorDto::from)
    } else {
        render_svg_preview(content.as_bytes(), px).map_err(ViewerErrorDto::from)
    }
}

/// Called by the frontend once on startup to retrieve the file path that
/// launched this instance (from CLI args), then clears it so it's only
/// consumed once.
#[tauri::command]
pub fn take_pending_file(state: State<PendingFile>) -> Option<String> {
    state.0.lock().unwrap().take()
}
