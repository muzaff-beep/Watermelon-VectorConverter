// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

//! Contract C-4 — the frozen error taxonomy shared by R, P, and B.

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ConversionError {
    InvalidSvg(String),        // 1001
    UnsupportedFeature(String),// 1002
    ZipReadError(String),      // 1003
    ZipWriteError(String),     // 1004
    IoError(String),           // 1005
    RenderError(String),       // 1006
    Cancelled,                 // 1007
    Internal(String),          // 1099
}

impl ConversionError {
    /// Stable numeric code (never reused). Part of the frozen contract.
    pub fn code(&self) -> u16 {
        match self {
            ConversionError::InvalidSvg(_) => 1001,
            ConversionError::UnsupportedFeature(_) => 1002,
            ConversionError::ZipReadError(_) => 1003,
            ConversionError::ZipWriteError(_) => 1004,
            ConversionError::IoError(_) => 1005,
            ConversionError::RenderError(_) => 1006,
            ConversionError::Cancelled => 1007,
            ConversionError::Internal(_) => 1099,
        }
    }
}
