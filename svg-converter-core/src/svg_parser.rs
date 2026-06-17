// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. See LICENSE.

//! SVG parsing & normalization (Contract C-1).
//! Rejects out-of-scope features with UnsupportedFeature(1002).

use crate::error::ConversionError;
use crate::models::*;
use crate::utils::{fmt_num, parse_rgb, to_aarrggbb};

const UNSUPPORTED: &[&str] = &[
    "linearGradient", "radialGradient", "filter", "text", "tspan",
    "pattern", "mask", "image", "use", "clipPath",
];

pub fn parse(svg_bytes: &[u8]) -> Result<NormalizedSvg, ConversionError> {
    let text = std::str::from_utf8(svg_bytes)
        .map_err(|e| ConversionError::InvalidSvg(format!("not UTF-8: {e}")))?;
    let doc = roxmltree::Document::parse(text)
        .map_err(|e| ConversionError::InvalidSvg(e.to_string()))?;
    let root = doc.root_element();
    if root.tag_name().name() != "svg" {
        return Err(ConversionError::InvalidSvg("root is not <svg>".into()));
    }

    // Reject unsupported features anywhere in the tree (no silent drop).
    for node in doc.descendants().filter(|n| n.is_element()) {
        let name = node.tag_name().name();
        if UNSUPPORTED.contains(&name) {
            return Err(ConversionError::UnsupportedFeature(name.to_string()));
        }
    }

    let (vw, vh) = parse_viewbox(&root)?;
    let width = attr_f32(&root, "width").unwrap_or(vw);
    let height = attr_f32(&root, "height").unwrap_or(vh);
    let root_alpha = attr_f32(&root, "opacity").unwrap_or(1.0);

    let mut nodes = Vec::new();
    for child in root.children().filter(|n| n.is_element()) {
        if let Some(n) = parse_node(&child)? {
            nodes.push(n);
        }
    }

    Ok(NormalizedSvg {
        width, height, viewport_w: vw, viewport_h: vh, root_alpha, nodes,
    })
}

fn parse_viewbox(root: &roxmltree::Node) -> Result<(f32, f32), ConversionError> {
    if let Some(vb) = root.attribute("viewBox") {
        let parts: Vec<f32> = vb.split_whitespace()
            .filter_map(|s| s.parse().ok()).collect();
        if parts.len() == 4 {
            return Ok((parts[2], parts[3]));
        }
        return Err(ConversionError::InvalidSvg("bad viewBox".into()));
    }
    // Fall back to width/height
    let w = attr_f32(root, "width");
    let h = attr_f32(root, "height");
    match (w, h) {
        (Some(w), Some(h)) => Ok((w, h)),
        _ => Err(ConversionError::InvalidSvg("no viewBox or width/height".into())),
    }
}

fn parse_node(el: &roxmltree::Node) -> Result<Option<Node>, ConversionError> {
    match el.tag_name().name() {
        "path" => Ok(Some(Node::Path(parse_path(el)?))),
        "g" => {
            let mut group = parse_group_transform(el);
            for child in el.children().filter(|n| n.is_element()) {
                if let Some(n) = parse_node(&child)? {
                    group.children.push(n);
                }
            }
            Ok(Some(Node::Group(group)))
        }
        // Basic shapes could be added here; for v1 we focus on path + group.
        "rect" | "circle" | "ellipse" | "line" | "polyline" | "polygon" => {
            Err(ConversionError::UnsupportedFeature(
                format!("shape <{}> not yet converted; convert to <path>", el.tag_name().name())
            ))
        }
        _ => Ok(None), // ignore <title>, <desc>, <defs> wrappers, etc.
    }
}

fn parse_path(el: &roxmltree::Node) -> Result<VdPath, ConversionError> {
    let d = el.attribute("d")
        .ok_or_else(|| ConversionError::InvalidSvg("<path> missing d".into()))?;
    let path_data = normalize_path_data(d)?;

    let opacity = attr_f32(el, "opacity").unwrap_or(1.0);
    let fill_opacity = attr_f32(el, "fill-opacity").unwrap_or(1.0) * opacity;
    let stroke_opacity = attr_f32(el, "stroke-opacity").unwrap_or(1.0) * opacity;

    // SVG default fill is black; explicit "none" disables.
    let fill_raw = el.attribute("fill").unwrap_or("black");
    let fill_color = parse_rgb(fill_raw).map(|rgb| to_aarrggbb(rgb, fill_opacity));

    let stroke_color = el.attribute("stroke")
        .and_then(parse_rgb)
        .map(|rgb| to_aarrggbb(rgb, stroke_opacity));
    let stroke_width = attr_f32(el, "stroke-width").unwrap_or(0.0);

    let fill_type = match el.attribute("fill-rule") {
        Some("evenodd") => FillType::EvenOdd,
        _ => FillType::NonZero,
    };

    Ok(VdPath { path_data, fill_color, stroke_color, stroke_width, fill_type })
}

fn parse_group_transform(el: &roxmltree::Node) -> VdGroup {
    let mut g = VdGroup::default();
    if let Some(t) = el.attribute("transform") {
        apply_transform_str(t, &mut g);
    }
    g
}

/// Minimal transform parser: translate/scale/rotate. matrix() -> best-effort skip.
fn apply_transform_str(t: &str, g: &mut VdGroup) {
    let mut rest = t;
    while let Some(open) = rest.find('(') {
        let name = rest[..open].trim().to_string();
        let close = match rest[open..].find(')') { Some(c) => open + c, None => break };
        let args: Vec<f32> = rest[open + 1..close]
            .split([',', ' '])
            .filter(|s| !s.is_empty())
            .filter_map(|s| s.parse().ok())
            .collect();
        match name.as_str() {
            "translate" => {
                g.translate_x += args.first().copied().unwrap_or(0.0);
                g.translate_y += args.get(1).copied().unwrap_or(0.0);
            }
            "scale" => {
                let sx = args.first().copied().unwrap_or(1.0);
                g.scale_x *= sx;
                g.scale_y *= args.get(1).copied().unwrap_or(sx);
            }
            "rotate" => {
                g.rotation += args.first().copied().unwrap_or(0.0);
                if args.len() >= 3 { g.pivot_x = args[1]; g.pivot_y = args[2]; }
            }
            _ => {} // matrix/skew: out of v1 scope, left as identity
        }
        rest = &rest[close + 1..];
    }
}

/// Normalize path data: tokenize, convert relative->absolute, re-emit with
/// commas between coordinate pairs. Supports M L H V C S Q T Z (+ relative).
fn normalize_path_data(d: &str) -> Result<String, ConversionError> {
    let tokens = tokenize(d)?;
    let mut out = String::new();
    let mut i = 0;
    let (mut cx, mut cy) = (0.0f32, 0.0f32);   // current point
    let (mut sx, mut sy) = (0.0f32, 0.0f32);   // subpath start
    let mut last_cmd = ' ';

    macro_rules! num { () => {{
        let v = match tokens.get(i) { Some(Token::Num(n)) => *n, _ =>
            return Err(ConversionError::InvalidSvg("expected number in path".into())) };
        i += 1; v
    }}; }

    while i < tokens.len() {
        let cmd = match &tokens[i] {
            Token::Cmd(c) => { i += 1; last_cmd = *c; *c }
            Token::Num(_) => implicit_cmd(last_cmd), // implicit repeat
        };
        let abs = cmd.is_ascii_uppercase();
        match cmd.to_ascii_uppercase() {
            'M' => {
                let (mut x, mut y) = (num!(), num!());
                if !abs { x += cx; y += cy; }
                cx = x; cy = y; sx = x; sy = y;
                out.push_str(&format!("M{},{} ", fmt_num(x), fmt_num(y)));
                last_cmd = if abs { 'L' } else { 'l' }; // subsequent pairs are line-to
            }
            'L' => {
                let (mut x, mut y) = (num!(), num!());
                if !abs { x += cx; y += cy; }
                cx = x; cy = y;
                out.push_str(&format!("L{},{} ", fmt_num(x), fmt_num(y)));
            }
            'H' => {
                let mut x = num!(); if !abs { x += cx; }
                cx = x;
                out.push_str(&format!("L{},{} ", fmt_num(x), fmt_num(cy)));
            }
            'V' => {
                let mut y = num!(); if !abs { y += cy; }
                cy = y;
                out.push_str(&format!("L{},{} ", fmt_num(cx), fmt_num(y)));
            }
            'C' => {
                let mut c = [0f32; 6];
                for k in 0..6 { c[k] = num!(); if !abs { c[k] += if k % 2 == 0 { cx } else { cy }; } }
                cx = c[4]; cy = c[5];
                out.push_str(&format!("C{},{} {},{} {},{} ",
                    fmt_num(c[0]), fmt_num(c[1]), fmt_num(c[2]), fmt_num(c[3]), fmt_num(c[4]), fmt_num(c[5])));
            }
            'Q' => {
                let mut c = [0f32; 4];
                for k in 0..4 { c[k] = num!(); if !abs { c[k] += if k % 2 == 0 { cx } else { cy }; } }
                cx = c[2]; cy = c[3];
                out.push_str(&format!("Q{},{} {},{} ",
                    fmt_num(c[0]), fmt_num(c[1]), fmt_num(c[2]), fmt_num(c[3])));
            }
            'Z' => {
                cx = sx; cy = sy;
                out.push_str("Z ");
            }
            other => {
                return Err(ConversionError::UnsupportedFeature(
                    format!("path command '{other}' (S/T/A arcs not in v1 scope)")));
            }
        }
    }
    Ok(out.trim_end().to_string())
}

fn implicit_cmd(last: char) -> char {
    // After an M, implicit pairs are L (matching case handled by caller via last_cmd)
    last
}

#[derive(Debug)]
enum Token { Cmd(char), Num(f32) }

fn tokenize(d: &str) -> Result<Vec<Token>, ConversionError> {
    let mut tokens = Vec::new();
    let bytes = d.as_bytes();
    let mut i = 0;
    while i < bytes.len() {
        let c = bytes[i] as char;
        if c.is_ascii_whitespace() || c == ',' { i += 1; continue; }
        if c.is_ascii_alphabetic() {
            tokens.push(Token::Cmd(c));
            i += 1;
            continue;
        }
        // number: optional sign, digits, dot, exponent
        let start = i;
        if c == '+' || c == '-' { i += 1; }
        let mut seen_dot = false;
        while i < bytes.len() {
            let ch = bytes[i] as char;
            if ch.is_ascii_digit() { i += 1; }
            else if ch == '.' && !seen_dot { seen_dot = true; i += 1; }
            else if (ch == 'e' || ch == 'E') && i + 1 < bytes.len() {
                i += 1;
                if (bytes[i] as char) == '+' || (bytes[i] as char) == '-' { i += 1; }
            } else { break; }
        }
        if i == start { return Err(ConversionError::InvalidSvg(format!("bad char in path: {c}"))); }
        let num: f32 = d[start..i].parse()
            .map_err(|_| ConversionError::InvalidSvg(format!("bad number: {}", &d[start..i])))?;
        tokens.push(Token::Num(num));
    }
    Ok(tokens)
}

fn attr_f32(el: &roxmltree::Node, name: &str) -> Option<f32> {
    el.attribute(name).and_then(|s| {
        let t = s.trim_end_matches("px").trim();
        t.parse().ok()
    })
}
