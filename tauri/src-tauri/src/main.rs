// Watermelon Vector Converter — Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Tauri entry point — delegates to the shared builder in lib.rs.

// Prevents an extra console window on Windows in release.
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

fn main() {
    wvgc_desktop::run();
}
