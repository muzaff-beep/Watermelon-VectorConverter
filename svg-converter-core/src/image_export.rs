// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. See LICENSE.

//! Dual approximate preview rendering via resvg (Contract C-2).
//! Renders the ORIGINAL SVG and the GENERATED VectorDrawable's pathData to PNG.
//! Both are explicitly approximate: resvg is not Android's Skia pipeline.

use crate::error::ConversionError;

const PX_MIN: u32 = 16;
const PX_MAX: u32 = 2048;

/// C-2: render original SVG bytes to a square px*px PNG (sRGB, transparent bg).
pub fn render_svg_preview(svg_bytes: &[u8], px: u32) -> Result<Vec<u8>, ConversionError> {
    check_px(px)?;
    let svg = std::str::from_utf8(svg_bytes)
        .map_err(|e| ConversionError::RenderError(format!("not UTF-8: {e}")))?;
    render_svg_string(svg, px)
}

/// C-2: render the GENERATED VectorDrawable by reconstructing an SVG from its
/// pathData and rendering THAT (keeps the preview honest about emitted XML).
pub fn render_vd_preview(vd_xml: &str, px: u32) -> Result<Vec<u8>, ConversionError> {
    check_px(px)?;
    let svg = vd_to_svg(vd_xml)?;
    render_svg_string(&svg, px)
}

fn check_px(px: u32) -> Result<(), ConversionError> {
    if !(PX_MIN..=PX_MAX).contains(&px) {
        return Err(ConversionError::RenderError(format!(
            "px {px} out of range {PX_MIN}..={PX_MAX}"
        )));
    }
    Ok(())
}

fn render_svg_string(svg: &str, px: u32) -> Result<Vec<u8>, ConversionError> {
    // usvg requires the SVG namespace. Real files have it; inject if absent
    // so a namespace-less snippet still renders for preview purposes.
    let svg_owned;
    let svg = if svg.contains("xmlns") {
        svg
    } else {
        svg_owned = svg.replacen("<svg", "<svg xmlns=\"http://www.w3.org/2000/svg\"", 1);
        &svg_owned
    };

    let opt = usvg::Options::default();
    let tree = usvg::Tree::from_str(svg, &opt)
        .map_err(|e| ConversionError::RenderError(e.to_string()))?;

    let mut pixmap = tiny_skia::Pixmap::new(px, px)
        .ok_or_else(|| ConversionError::RenderError("pixmap alloc failed".into()))?;

    // Fit the tree's viewbox into px*px.
    let size = tree.size();
    let scale = (px as f32 / size.width()).min(px as f32 / size.height());
    let transform = tiny_skia::Transform::from_scale(scale, scale);

    resvg::render(&tree, transform, &mut pixmap.as_mut());

    pixmap
        .encode_png()
        .map_err(|e| ConversionError::RenderError(e.to_string()))
}

/// Minimal VectorDrawable -> SVG reconstruction for preview only.
/// Reads viewportWidth/Height and each <path>'s pathData + fillColor.
fn vd_to_svg(vd_xml: &str) -> Result<String, ConversionError> {
    let doc = roxmltree::Document::parse(vd_xml)
        .map_err(|e| ConversionError::RenderError(e.to_string()))?;
    let root = doc.root_element();

    let android = "http://schemas.android.com/apk/res/android";
    let vw = root
        .attribute((android, "viewportWidth"))
        .or_else(|| root.attribute("viewportWidth"))
        .and_then(|s| s.parse::<f32>().ok())
        .unwrap_or(24.0);
    let vh = root
        .attribute((android, "viewportHeight"))
        .or_else(|| root.attribute("viewportHeight"))
        .and_then(|s| s.parse::<f32>().ok())
        .unwrap_or(24.0);

    let mut paths = String::new();
    for node in doc.descendants().filter(|n| n.has_tag_name("path")) {
        let d = node
            .attribute((android, "pathData"))
            .or_else(|| node.attribute("pathData"))
            .unwrap_or("");
        if d.is_empty() {
            continue;
        }
        let fill = node
            .attribute((android, "fillColor"))
            .or_else(|| node.attribute("fillColor"))
            .map(aarrggbb_to_svg_fill)
            .unwrap_or_else(|| ("none".to_string(), 1.0));

        // Accumulate the transform from every ancestor <group>. VectorDrawable
        // groups carry translateX/translateY/scaleX/scaleY/rotation, and a path's
        // pathData is relative to its group origin. Without re-applying these,
        // every path renders at (0,0) and the preview is scrambled/cropped.
        let transform = ancestor_group_transform(&node, android);

        if transform.is_empty() {
            paths.push_str(&format!(
                "<path d=\"{}\" fill=\"{}\" fill-opacity=\"{}\"/>",
                d, fill.0, fill.1
            ));
        } else {
            paths.push_str(&format!(
                "<g transform=\"{}\"><path d=\"{}\" fill=\"{}\" fill-opacity=\"{}\"/></g>",
                transform, d, fill.0, fill.1
            ));
        }
    }

    Ok(format!(
        "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 {} {}\">{}</svg>",
        vw, vh, paths
    ))
}

/// Walk up from a <path> through all ancestor <group> elements and build an
/// SVG transform string that reproduces the VectorDrawable group transforms.
/// Outermost group is applied first (leftmost in the string) so nesting composes
/// the same way Android applies it.
fn ancestor_group_transform(node: &roxmltree::Node, android: &str) -> String {
    let mut groups: Vec<String> = Vec::new();
    let mut cur = node.parent();
    while let Some(n) = cur {
        if n.has_tag_name("group") {
            let mut parts: Vec<String> = Vec::new();

            let tx = group_attr_f32(&n, android, "translateX");
            let ty = group_attr_f32(&n, android, "translateY");
            if tx != 0.0 || ty != 0.0 {
                parts.push(format!("translate({},{})", tx, ty));
            }

            let px = group_attr_f32(&n, android, "pivotX");
            let py = group_attr_f32(&n, android, "pivotY");

            let rot = group_attr_f32(&n, android, "rotation");
            if rot != 0.0 {
                if px != 0.0 || py != 0.0 {
                    parts.push(format!("rotate({},{},{})", rot, px, py));
                } else {
                    parts.push(format!("rotate({})", rot));
                }
            }

            let sx = group_attr_f32_default(&n, android, "scaleX", 1.0);
            let sy = group_attr_f32_default(&n, android, "scaleY", 1.0);
            if sx != 1.0 || sy != 1.0 {
                parts.push(format!("scale({},{})", sx, sy));
            }

            if !parts.is_empty() {
                groups.push(parts.join(" "));
            }
        }
        cur = n.parent();
    }
    // groups is innermost-first; reverse so outermost is applied first (SVG left-to-right).
    groups.reverse();
    groups.join(" ")
}

fn group_attr_f32(n: &roxmltree::Node, android: &str, name: &str) -> f32 {
    n.attribute((android, name))
        .or_else(|| n.attribute(name))
        .and_then(|s| s.parse::<f32>().ok())
        .unwrap_or(0.0)
}

fn group_attr_f32_default(n: &roxmltree::Node, android: &str, name: &str, default: f32) -> f32 {
    n.attribute((android, name))
        .or_else(|| n.attribute(name))
        .and_then(|s| s.parse::<f32>().ok())
        .unwrap_or(default)
}

/// #AARRGGBB -> (#RRGGBB, alpha 0..1) for SVG.
fn aarrggbb_to_svg_fill(c: &str) -> (String, f32) {
    let h = c.trim_start_matches('#');
    if h.len() == 8 {
        let a = u8::from_str_radix(&h[0..2], 16).unwrap_or(255) as f32 / 255.0;
        (format!("#{}", &h[2..8]), a)
    } else {
        (c.to_string(), 1.0)
    }
}
