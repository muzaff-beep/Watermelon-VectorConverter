// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Library target — exposes `commands` for integration tests and the binary.

pub mod commands;

/// Shared app builder, used by both main.rs and (potentially) mobile targets.
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_fs::init())
        .invoke_handler(tauri::generate_handler![
            commands::convert_svg,
            commands::convert_vd,
            commands::render_svg_preview,
            commands::render_vd_preview,
            commands::convert_zip,
            commands::convert_vd_zip,
            commands::open_url,
            commands::set_file_association,
            commands::get_file_association,
        ])
        .run(tauri::generate_context!())
        .expect("error while running Watermelon Vector Converter");
}
