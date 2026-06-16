// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

//! Public API surface for the conversion engine (Contract C-1).
//! Re-exports the frozen public functions and error types.

pub mod error;
pub mod models;
pub mod svg_parser;
pub mod vector_drawable;
pub mod image_export;
pub mod batch_processor;
pub mod utils;

#[cfg(target_os = "android")]
pub mod jni;

pub use error::ConversionError;

/// C-1: Convert raw SVG bytes into Android VectorDrawable XML.
/// Pure, deterministic, thread-safe. No I/O.
pub fn convert_svg(_svg_bytes: &[u8]) -> Result<String, ConversionError> {
    todo!("R: implement against Contract C-1")
}
