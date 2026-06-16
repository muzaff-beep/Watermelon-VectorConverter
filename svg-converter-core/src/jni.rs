// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

//! Rust JNI side of the FFI bridge (Contract C-3). THIN marshalling only.
//! Signatures must match SvgConverterNative.kt byte-for-byte (verified in CI).

// TODO(B): Java_com_watermelon_converter_SvgConverterNative_nativeConvertSvg, etc.
// Throw com.watermelon.converter.ConversionException on error; never return null.
