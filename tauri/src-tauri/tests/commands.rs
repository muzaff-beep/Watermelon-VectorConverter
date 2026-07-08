// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Unit tests for the testable command logic (do_* free functions).
// These run without a webview — the #[tauri::command] wrappers are thin.

use wvgc_desktop::commands::*;

const SVG: &[u8] = br##"<svg viewBox="0 0 24 24"><path d="M2 2 L22 2 L22 22 Z" fill="#ff0000"/></svg>"##;

#[test]
fn convert_svg_ok() {
    let xml = do_convert_svg(SVG.to_vec()).unwrap();
    assert!(xml.contains("android:pathData=\"M2,2 L22,2 L22,22 Z\""));
}

#[test]
fn invalid_svg_maps_to_error() {
    let err = do_convert_svg(b"<svg><path d=".to_vec()).unwrap_err();
    assert!(err.code >= 1001 && err.code <= 1099);
    assert!(!err.message.is_empty());
}

const VD: &[u8] = br##"<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:pathData="M2,2 L22,2 L22,22 Z" android:fillColor="#FFFF0000"/>
</vector>"##;

#[test]
fn convert_vd_ok() {
    let svg = do_convert_vd(VD.to_vec()).unwrap();
    assert!(svg.contains("<svg"));
    assert!(svg.contains("d=\"M2,2 L22,2 L22,22 Z\""));
}

#[test]
fn invalid_vd_maps_to_error() {
    let err = do_convert_vd(b"<not-a-vector/>".to_vec()).unwrap_err();
    assert!(err.code >= 1001 && err.code <= 1099);
    assert!(!err.message.is_empty());
}

#[test]
fn svg_preview_returns_png() {
    let png = do_render_svg_preview(SVG.to_vec(), 48).unwrap();
    // PNG magic bytes
    assert_eq!(&png[0..8], &[0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A]);
}

#[test]
fn vd_preview_returns_png() {
    let xml = do_convert_svg(SVG.to_vec()).unwrap();
    let png = do_render_vd_preview(xml, 48).unwrap();
    assert_eq!(&png[0..8], &[0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A]);
}

#[test]
fn error_dto_serializes_to_json() {
    let dto = ConversionErrorDto { code: 1002, message: "unsupported".into() };
    let json = serde_json::to_string(&dto).unwrap();
    assert!(json.contains("\"code\":1002"));
    assert!(json.contains("unsupported"));
}
