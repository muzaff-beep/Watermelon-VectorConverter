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
        paths.push_str(&format!(
            "<path d=\"{}\" fill=\"{}\" fill-opacity=\"{}\"/>",
            d, fill.0, fill.1
        ));
    }

    Ok(format!(
        "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 {} {}\">{}</svg>",
        vw, vh, paths
    ))
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
