// Fill/stroke inheritance and inline style="" parsing (bugfix). Copyright (c) 2026 Suhail Muzaffari.

use svg_converter_core::convert_svg;

#[test]
fn group_fill_inherits_to_child_path_with_no_own_fill() {
    let svg = br##"<svg viewBox="0 0 24 24">
        <g fill="#ff0000">
            <path d="M0,0 L10,0 L10,10 Z"/>
            <path d="M12,0 L22,0 L22,10 Z" fill="#00ff00"/>
        </g>
    </svg>"##;
    let out = convert_svg(svg).expect("should convert");
    // First path has no own fill -> inherits red from the group.
    assert!(out.contains("#FFFF0000"), "{out}");
    // Second path overrides with its own fill -> green, not the inherited red.
    assert!(out.contains("#FF00FF00"), "{out}");
    // Must NOT have collapsed both paths to the same color.
    assert_ne!(
        out.matches("#FFFF0000").count() + out.matches("#FF00FF00").count(),
        0
    );
}

#[test]
fn inline_style_fill_is_read() {
    let svg = br##"<svg viewBox="0 0 24 24">
        <path d="M0,0 L10,0 L10,10 Z" style="fill:#3366ff"/>
    </svg>"##;
    let out = convert_svg(svg).expect("should convert");
    assert!(out.contains("#FF3366FF"), "{out}");
}

#[test]
fn own_attribute_wins_over_inline_style() {
    // Per CSS cascade for presentation attrs vs inline style, style="" wins
    // over the plain attribute when both are present on the SAME element.
    let svg = br##"<svg viewBox="0 0 24 24">
        <path d="M0,0 L10,0 L10,10 Z" fill="#000000" style="fill:#ffaa00"/>
    </svg>"##;
    let out = convert_svg(svg).expect("should convert");
    assert!(out.contains("#FFFFAA00"), "{out}");
    assert!(!out.contains("#FF000000"), "{out}");
}

#[test]
fn nested_groups_cascade_fill_through_multiple_levels() {
    let svg = br##"<svg viewBox="0 0 24 24">
        <g fill="#123456">
            <g>
                <path d="M0,0 L10,0 L10,10 Z"/>
            </g>
        </g>
    </svg>"##;
    let out = convert_svg(svg).expect("should convert");
    assert!(out.contains("#FF123456"), "{out}");
}

#[test]
fn multiple_distinct_colors_are_preserved_not_collapsed() {
    // Regression guard for the exact reported bug: a multi-color SVG must
    // not collapse to a single fill color in the VD output.
    let svg = br##"<svg viewBox="0 0 24 24">
        <path d="M0,0 L5,0 L5,5 Z" fill="#ff0000"/>
        <path d="M6,0 L11,0 L11,5 Z" fill="#00ff00"/>
        <path d="M12,0 L17,0 L17,5 Z" fill="#0000ff"/>
    </svg>"##;
    let out = convert_svg(svg).expect("should convert");
    assert!(out.contains("#FFFF0000"), "{out}");
    assert!(out.contains("#FF00FF00"), "{out}");
    assert!(out.contains("#FF0000FF"), "{out}");
}
