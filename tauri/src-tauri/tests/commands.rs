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

// ── C-5.1 detection bridge ──────────────────────────────────────────────────

const STATIC_SVG: &[u8] = br##"<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 10 10"><rect width="10" height="10"/></svg>"##;

const SMIL_SVG: &[u8] = br##"<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 10 10">
    <rect width="10" height="10"><animate attributeName="x" from="0" to="9" dur="1s"/></rect>
</svg>"##;

const AVD_XML: &[u8] = br##"<animated-vector xmlns:android="http://schemas.android.com/apk/res/android">
    <target android:name="rect" android:animation="@anim/rotate"/>
</animated-vector>"##;

#[test]
fn detect_animation_reports_none_for_static_svg() {
    assert_eq!(do_detect_animation(STATIC_SVG.to_vec(), false), "None");
}

#[test]
fn detect_animation_reports_svg_smil() {
    assert_eq!(do_detect_animation(SMIL_SVG.to_vec(), false), "SvgSmil");
}

#[test]
fn detect_animation_reports_avd() {
    assert_eq!(do_detect_animation(AVD_XML.to_vec(), true), "Avd");
}

#[test]
fn detect_animation_never_panics_on_garbage() {
    let garbage = vec![0xFFu8, 0xFE, 0x00, 0x01];
    // Must not panic regardless of is_avd; result is always a valid string.
    let a = do_detect_animation(garbage.clone(), true);
    let b = do_detect_animation(garbage, false);
    assert_eq!(a, "None");
    assert_eq!(b, "None");
}

// ── C-5.2 AVD frame rendering bridge ────────────────────────────────────────

#[test]
fn render_avd_frames_surfaces_conversion_error_not_panic() {
    // Whatever the current state of the C-5.2 engine (scaffold or real),
    // an input with no usable base <vector> must come back as a normal
    // catchable Err, never a panic. This exercises the bridge's error path
    // specifically, independent of the engine's own correctness.
    let no_base_vector: &[u8] = b"<animated-vector/>";
    let err = do_render_avd_frames(no_base_vector.to_vec(), 30, 90, 64).unwrap_err();
    assert!(err.code >= 1001 && err.code <= 1099);
    assert!(!err.message.is_empty());
}

#[test]
fn render_avd_frames_rejects_out_of_range_params_as_render_error() {
    let dummy: &[u8] = b"<animated-vector/>";
    let err_px = do_render_avd_frames(dummy.to_vec(), 30, 90, 4).unwrap_err();
    assert_eq!(err_px.code, 1006);

    let err_fps = do_render_avd_frames(dummy.to_vec(), 0, 90, 64).unwrap_err();
    assert_eq!(err_fps.code, 1006);
}

#[test]
fn avd_frames_dto_serializes_to_json() {
    let dto = AvdFramesDto {
        width: 64,
        height: 64,
        frame_durations_ms: vec![33, 33],
        frames: vec![vec![1, 2, 3], vec![4, 5, 6]],
        loop_mode: "Repeat".to_string(),
    };
    let json = serde_json::to_string(&dto).unwrap();
    assert!(json.contains("\"width\":64"));
    assert!(json.contains("\"loop_mode\":\"Repeat\""));
}
