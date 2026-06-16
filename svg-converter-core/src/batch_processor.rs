// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

//! ZIP batch conversion: Rayon workers + single coordinator for progress,
//! atomic cancellation (Contract C-2). Workers never call into the JVM.

use crate::error::ConversionError;
use std::sync::atomic::AtomicBool;

pub struct ProgressEvent { pub done: u32, pub total: u32, pub current_name: String }
pub type CancelFlag = AtomicBool;

/// C-2: convert a ZIP of SVGs; coordinator aggregates progress.
pub fn convert_zip(
    _zip_bytes: &[u8],
    _progress: &(dyn Fn(ProgressEvent) + Send + Sync),
    _cancel: &CancelFlag,
) -> Result<Vec<u8>, ConversionError> {
    todo!("P: implement against Contract C-2")
}
