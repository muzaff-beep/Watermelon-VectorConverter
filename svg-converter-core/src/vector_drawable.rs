// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. See LICENSE.

//! Normalized tree -> Android VectorDrawable XML (Contract C-1).

use crate::models::*;
use crate::utils::fmt_num;

const NS: &str = "http://schemas.android.com/apk/res/android";

pub fn emit(svg: &NormalizedSvg) -> String {
    let mut s = String::new();
    s.push_str(&format!("<vector xmlns:android=\"{NS}\"\n"));
    s.push_str(&format!("    android:width=\"{}dp\"\n", fmt_num(svg.width)));
    s.push_str(&format!("    android:height=\"{}dp\"\n", fmt_num(svg.height)));
    s.push_str(&format!("    android:viewportWidth=\"{}\"\n", fmt_num(svg.viewport_w)));
    s.push_str(&format!("    android:viewportHeight=\"{}\"", fmt_num(svg.viewport_h)));
    if svg.root_alpha < 1.0 {
        s.push_str(&format!("\n    android:alpha=\"{}\"", fmt_num(svg.root_alpha)));
    }
    s.push_str(">\n");
    for node in &svg.nodes {
        emit_node(node, 1, &mut s);
    }
    s.push_str("</vector>\n");
    s
}

fn indent(level: usize) -> String { "    ".repeat(level) }

fn emit_node(node: &Node, level: usize, s: &mut String) {
    match node {
        Node::Path(p) => emit_path(p, level, s),
        Node::Group(g) => emit_group(g, level, s),
    }
}

fn emit_group(g: &VdGroup, level: usize, s: &mut String) {
    let pad = indent(level);
    s.push_str(&format!("{pad}<group"));
    if g.translate_x != 0.0 { s.push_str(&format!("\n{pad}    android:translateX=\"{}\"", fmt_num(g.translate_x))); }
    if g.translate_y != 0.0 { s.push_str(&format!("\n{pad}    android:translateY=\"{}\"", fmt_num(g.translate_y))); }
    if g.scale_x != 1.0 { s.push_str(&format!("\n{pad}    android:scaleX=\"{}\"", fmt_num(g.scale_x))); }
    if g.scale_y != 1.0 { s.push_str(&format!("\n{pad}    android:scaleY=\"{}\"", fmt_num(g.scale_y))); }
    if g.rotation != 0.0 { s.push_str(&format!("\n{pad}    android:rotation=\"{}\"", fmt_num(g.rotation))); }
    if g.pivot_x != 0.0 { s.push_str(&format!("\n{pad}    android:pivotX=\"{}\"", fmt_num(g.pivot_x))); }
    if g.pivot_y != 0.0 { s.push_str(&format!("\n{pad}    android:pivotY=\"{}\"", fmt_num(g.pivot_y))); }
    s.push_str(">\n");
    for child in &g.children {
        emit_node(child, level + 1, s);
    }
    s.push_str(&format!("{pad}</group>\n"));
}

fn emit_path(p: &VdPath, level: usize, s: &mut String) {
    let pad = indent(level);
    s.push_str(&format!("{pad}<path\n"));
    s.push_str(&format!("{pad}    android:pathData=\"{}\"", p.path_data));
    if let Some(fc) = &p.fill_color {
        s.push_str(&format!("\n{pad}    android:fillColor=\"{}\"", fc));
    }
    if let Some(sc) = &p.stroke_color {
        s.push_str(&format!("\n{pad}    android:strokeColor=\"{}\"", sc));
        if p.stroke_width > 0.0 {
            s.push_str(&format!("\n{pad}    android:strokeWidth=\"{}\"", fmt_num(p.stroke_width)));
        }
    }
    let ft = match p.fill_type { FillType::NonZero => "nonZero", FillType::EvenOdd => "evenOdd" };
    s.push_str(&format!("\n{pad}    android:fillType=\"{}\"", ft));
    s.push_str("/>\n");
}
