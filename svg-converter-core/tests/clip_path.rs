// Watermelon Vector Converter — clip-path support (forward, C-1).
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.

use svg_converter_core::convert_svg;

#[test]
fn clip_path_on_group_emits_clip_path_element() {
    let svg = br##"<svg viewBox="0 0 24 24">
        <defs><clipPath id="c"><path d="M6,6 h12 v12 h-12 z"/></clipPath></defs>
        <g clip-path="url(#c)">
            <path d="M0,24 L12,0 L24,24 Z" fill="#000"/>
        </g>
    </svg>"##;
    let out = convert_svg(svg).expect("should convert");
    assert!(out.contains("<clip-path"), "{out}");
    assert!(out.contains("android:pathData=\"M6,6"), "{out}");
    // clip-path must be the first child of its group (VD semantics: clips siblings after it)
    let group_start = out.find("<group").expect("group present");
    let clip_pos = out.find("<clip-path").unwrap();
    let first_path_after_group = out[group_start..].find("<path").map(|i| i + group_start);
    assert!(clip_pos < first_path_after_group.unwrap(), "{out}");
}

#[test]
fn clip_path_on_bare_path_wraps_in_synthetic_group() {
    let svg = br##"<svg viewBox="0 0 24 24">
        <defs><clipPath id="c"><rect x="2" y="2" width="10" height="10"/></clipPath></defs>
        <path clip-path="url(#c)" d="M0,24 L12,0 L24,24 Z" fill="#000"/>
    </svg>"##;
    let out = convert_svg(svg).expect("should convert");
    assert!(out.contains("<clip-path"), "{out}");
    assert!(out.contains("<group"), "{out}");
}

#[test]
fn dangling_clip_path_reference_is_ignored_not_fatal() {
    let svg = br##"<svg viewBox="0 0 24 24">
        <g clip-path="url(#missing)"><path d="M0,0 L1,1" fill="#000"/></g>
    </svg>"##;
    let out = convert_svg(svg).expect("should still convert");
    assert!(!out.contains("<clip-path"), "{out}");
}
