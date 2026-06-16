// Watermelon Vector Converter — Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Tauri commands wrapping the Rust core (Contract C-3, desktop side).

use serde::Serialize;

#[derive(Serialize)]
pub struct ConversionErrorDto { pub code: u16, pub message: String }

// TODO(B): #[tauri::command] convert_svg / convert_zip / render_* / cancel
// convert_zip emits "batch://progress" events.
