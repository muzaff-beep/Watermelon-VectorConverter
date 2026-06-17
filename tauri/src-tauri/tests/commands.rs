use wvgc_desktop::commands::*;

const SVG: &[u8] = br##"<svg viewBox="0 0 24 24"><path d="M2 2 L22 2 L22 22 Z" fill="#ff0000"/></svg>"##;

#[test]
fn convert_svg_ok() {
    let xml = cmd_convert_svg(SVG.to_vec()).unwrap();
    assert!(xml.contains("android:pathData=\"M2,2 L22,2 L22,22 Z\""));
}
#[test]
fn error_maps_to_dto_with_code() {
    let err = cmd_convert_svg(b"<svg viewBox=\"0 0 1 1\"><text>x</text></svg>".to_vec()).unwrap_err();
    assert_eq!(err.code, 1002);
    assert!(!err.message.is_empty());
}
#[test]
fn invalid_svg_maps_to_1001() {
    let err = cmd_convert_svg(b"<svg><path d=".to_vec()).unwrap_err();
    assert_eq!(err.code, 1001);
}
#[test]
fn preview_dto_roundtrip() {
    let png = cmd_render_svg_preview(SVG.to_vec(), 48).unwrap();
    assert_eq!(&png[0..8], &[0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A]);
    let bad = cmd_render_svg_preview(SVG.to_vec(), 4).unwrap_err();
    assert_eq!(bad.code, 1006);
}
#[test]
fn dto_serializes_to_json() {
    let dto = ConversionErrorDto { code: 1002, message: "unsupported".into() };
    let json = serde_json::to_string(&dto).unwrap();
    assert!(json.contains("\"code\":1002"));
}
