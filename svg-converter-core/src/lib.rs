// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. See LICENSE.

//! Public API surface.
//!   R — Conversion Engine : IMPLEMENTED + tested (C-1, C-4)
//!   P — Preview & Batch    : IMPLEMENTED + tested (C-2)
//!   B — FFI Bridge         : IMPLEMENTED (jni.rs write+typecheck-only; Tauri tested; parity verified)
//!   M — Android App        : IMPLEMENTED full app (write+typecheck-only; builds via android.yml CI)

pub mod error;
pub mod models;
pub mod svg_parser;
pub mod vector_drawable;
pub mod utils;
pub mod image_export;
pub mod batch_processor;
pub mod jni;

pub use error::ConversionError;

/// C-1: Convert raw SVG bytes into Android VectorDrawable XML.
pub fn convert_svg(svg_bytes: &[u8]) -> Result<String, ConversionError> {
    let normalized = svg_parser::parse(svg_bytes)?;
    Ok(vector_drawable::emit(&normalized))
}
