// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Type-safe invoke() wrappers over Tauri commands (Contract C-3, desktop side).

import { invoke } from "@tauri-apps/api/core";

export interface ConversionErrorDto {
  code: number;
  message: string;
}

/**
 * Convert a single SVG (Uint8Array) → VectorDrawable XML string.
 */
export async function convertSvg(svg: Uint8Array): Promise<string> {
  return invoke<string>("convert_svg", { svg: Array.from(svg) });
}

/**
 * Render an SVG preview PNG at the given pixel size.
 * Returns raw PNG bytes as number[].
 */
export async function renderSvgPreview(svg: Uint8Array, px: number): Promise<number[]> {
  return invoke<number[]>("render_svg_preview", { svg: Array.from(svg), px });
}

/**
 * Render a VectorDrawable XML preview PNG at the given pixel size.
 */
export async function renderVdPreview(vdXml: string, px: number): Promise<number[]> {
  return invoke<number[]>("render_vd_preview", { vdXml, px });
}

/**
 * Batch-convert a ZIP of SVGs → ZIP of VectorDrawable XMLs.
 */
export async function convertZip(zip: Uint8Array): Promise<Uint8Array> {
  const result = await invoke<number[]>("convert_zip", { zip: Array.from(zip) });
  return new Uint8Array(result);
}
