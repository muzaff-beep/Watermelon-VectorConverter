// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

//! Dual approximate preview rendering via resvg (Contract C-2).

use crate::error::ConversionError;

/// C-2: render original SVG to PNG bytes.
pub fn render_svg_preview(_svg_bytes: &[u8], _px: u32) -> Result<Vec<u8>, ConversionError> {
    todo!("P: implement against Contract C-2")
}

/// C-2: render generated VectorDrawable XML to PNG bytes (reparsed, not reusing R tree).
pub fn render_vd_preview(_vd_xml: &str, _px: u32) -> Result<Vec<u8>, ConversionError> {
    todo!("P: implement against Contract C-2")
}
