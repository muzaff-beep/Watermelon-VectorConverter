// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. See LICENSE.

//! Internal normalized types. Implementation detail of Module R; NOT frozen.

/// A normalized SVG document ready for VectorDrawable emission.
#[derive(Debug, Clone, PartialEq)]
pub struct NormalizedSvg {
    pub width: f32,
    pub height: f32,
    pub viewport_w: f32,
    pub viewport_h: f32,
    pub root_alpha: f32,
    pub nodes: Vec<Node>,
}

#[derive(Debug, Clone, PartialEq)]
pub enum Node {
    Path(VdPath),
    Group(VdGroup),
}

#[derive(Debug, Clone, PartialEq)]
pub struct VdGroup {
    pub translate_x: f32,
    pub translate_y: f32,
    pub scale_x: f32,
    pub scale_y: f32,
    pub rotation: f32,
    pub pivot_x: f32,
    pub pivot_y: f32,
    pub children: Vec<Node>,
}

impl Default for VdGroup {
    fn default() -> Self {
        VdGroup {
            translate_x: 0.0, translate_y: 0.0,
            scale_x: 1.0, scale_y: 1.0,
            rotation: 0.0, pivot_x: 0.0, pivot_y: 0.0,
            children: Vec::new(),
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum FillType { NonZero, EvenOdd }

#[derive(Debug, Clone, PartialEq)]
pub struct VdPath {
    pub path_data: String,
    /// #AARRGGBB or None (no fill).
    pub fill_color: Option<String>,
    pub stroke_color: Option<String>,
    pub stroke_width: f32,
    pub fill_type: FillType,
}
