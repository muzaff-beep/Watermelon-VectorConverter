// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. See LICENSE.

//! Tauri commands wrapping the Rust core (Contract C-3, desktop side).
//! Logic lives in free fns (do_*) so they remain testable without a webview.
//! #[tauri::command] wrappers are thin delegation only.

use serde::{Deserialize, Serialize};
use svg_converter_core::batch_processor::{
    convert_zip as core_convert_zip, convert_vd_zip as core_convert_vd_zip, ProgressEvent,
};
use svg_converter_core::convert_svg as core_convert_svg;
use svg_converter_core::convert_vd as core_convert_vd;
use svg_converter_core::error::ConversionError;
use svg_converter_core::image_export::{
    render_svg_preview as core_render_svg, render_vd_preview as core_render_vd,
};
use std::sync::atomic::AtomicBool;
use tauri::Emitter;

// ── Error DTO ────────────────────────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, PartialEq)]
pub struct ConversionErrorDto {
    pub code: u16,
    pub message: String,
}

impl From<ConversionError> for ConversionErrorDto {
    fn from(e: ConversionError) -> Self {
        ConversionErrorDto {
            code: e.code(),
            message: e.to_string(),
        }
    }
}

/// Progress payload emitted to the frontend as the "batch://progress" event.
#[derive(Clone, Serialize)]
pub struct BatchProgressDto {
    pub done: u32,
    pub total: u32,
    pub current_name: String,
}

impl From<ProgressEvent> for BatchProgressDto {
    fn from(e: ProgressEvent) -> Self {
        BatchProgressDto {
            done: e.done,
            total: e.total,
            current_name: e.current_name,
        }
    }
}

// ── Testable logic (no Tauri dependency) ────────────────────────────────────

pub fn do_convert_svg(svg: Vec<u8>) -> Result<String, ConversionErrorDto> {
    core_convert_svg(&svg).map_err(Into::into)
}

/// Reverse direction: VectorDrawable XML bytes -> SVG string.
pub fn do_convert_vd(vd_xml: Vec<u8>) -> Result<String, ConversionErrorDto> {
    core_convert_vd(&vd_xml).map_err(Into::into)
}

pub fn do_render_svg_preview(svg: Vec<u8>, px: u32) -> Result<Vec<u8>, ConversionErrorDto> {
    core_render_svg(&svg, px).map_err(Into::into)
}

pub fn do_render_vd_preview(vd_xml: String, px: u32) -> Result<Vec<u8>, ConversionErrorDto> {
    core_render_vd(&vd_xml, px).map_err(Into::into)
}

// ── Tauri command wrappers ───────────────────────────────────────────────────

/// Convert a single SVG file (bytes) → VectorDrawable XML string.
#[tauri::command]
pub fn convert_svg(svg: Vec<u8>) -> Result<String, ConversionErrorDto> {
    do_convert_svg(svg)
}

/// Reverse direction: convert a single VectorDrawable XML file (bytes) → SVG string.
#[tauri::command]
pub fn convert_vd(vd_xml: Vec<u8>) -> Result<String, ConversionErrorDto> {
    do_convert_vd(vd_xml)
}

/// Render an SVG preview PNG at the requested pixel size.
#[tauri::command]
pub fn render_svg_preview(svg: Vec<u8>, px: u32) -> Result<Vec<u8>, ConversionErrorDto> {
    do_render_svg_preview(svg, px)
}

/// Render a VectorDrawable XML preview PNG at the requested pixel size.
#[tauri::command]
pub fn render_vd_preview(vd_xml: String, px: u32) -> Result<Vec<u8>, ConversionErrorDto> {
    do_render_vd_preview(vd_xml, px)
}

/// Batch-convert a ZIP of SVG files → ZIP of VectorDrawable XML files.
/// Emits "batch://progress" events to the frontend as each file completes.
#[tauri::command]
pub fn convert_zip(
    window: tauri::Window,
    zip: Vec<u8>,
) -> Result<Vec<u8>, ConversionErrorDto> {
    let cancel = AtomicBool::new(false);
    let result = core_convert_zip(
        &zip,
        &|e: ProgressEvent| {
            // Emit progress to the frontend. Ignore emit errors (window may close).
            let _ = window.emit("batch://progress", BatchProgressDto::from(e));
        },
        &cancel,
    );
    result.map_err(Into::into)
}

/// Reverse batch: ZIP of VectorDrawable .xml files → ZIP of .svg files.
/// Emits "batch://progress" events on the same channel as convert_zip — the
/// frontend already listens once and only one batch direction runs at a time.
#[tauri::command]
pub fn convert_vd_zip(
    window: tauri::Window,
    zip: Vec<u8>,
) -> Result<Vec<u8>, ConversionErrorDto> {
    let cancel = AtomicBool::new(false);
    let result = core_convert_vd_zip(
        &zip,
        &|e: ProgressEvent| {
            let _ = window.emit("batch://progress", BatchProgressDto::from(e));
        },
        &cancel,
    );
    result.map_err(Into::into)
}

/// Open a URL in the system default browser.
#[tauri::command]
pub fn open_url(url: String) -> Result<(), ConversionErrorDto> {
    // Use the opener crate via tauri-plugin-shell if available,
    // otherwise fall back to std::process::Command.
    #[cfg(target_os = "windows")]
    std::process::Command::new("cmd")
        .args(["/c", "start", "", &url])
        .spawn()
        .map_err(|e| ConversionErrorDto { code: 1099, message: e.to_string() })?;

    #[cfg(target_os = "macos")]
    std::process::Command::new("open")
        .arg(&url)
        .spawn()
        .map_err(|e| ConversionErrorDto { code: 1099, message: e.to_string() })?;

    #[cfg(target_os = "linux")]
    std::process::Command::new("xdg-open")
        .arg(&url)
        .spawn()
        .map_err(|e| ConversionErrorDto { code: 1099, message: e.to_string() })?;

    Ok(())
}

/// Register or unregister the bundled viewer as the HKCU handler for a
/// given extension ("svg" or "xml"). No-op returning an error on non-Windows.
#[tauri::command]
pub fn set_file_association(ext: String, enabled: bool) -> Result<(), ConversionErrorDto> {
    #[cfg(windows)]
    {
        windows_assoc::set_association(&ext, enabled)
            .map_err(|e| ConversionErrorDto { code: 1098, message: e.to_string() })
    }
    #[cfg(not(windows))]
    {
        let _ = (ext, enabled);
        Err(ConversionErrorDto {
            code: 1098,
            message: "File association toggling is only supported on Windows.".into(),
        })
    }
}

/// Query whether the viewer is currently the HKCU handler for a given
/// extension. Always false on non-Windows.
#[tauri::command]
pub fn get_file_association(ext: String) -> bool {
    #[cfg(windows)]
    {
        windows_assoc::is_associated(&ext)
    }
    #[cfg(not(windows))]
    {
        let _ = ext;
        false
    }
}

#[cfg(windows)]
mod windows_assoc {
    use winreg::enums::*;
    use winreg::RegKey;

    const PROG_ID: &str = "WatermelonVectorFile";

    /// Registers/unregisters `WatermelonVectorFile` as the handler for
    /// `.{ext}` under HKCU\Software\Classes, and lists it in the
    /// OpenWithProgids key so it shows up in the Explorer "Open with" menu.
    /// HKCU (not HKCR) is used deliberately: it requires no admin rights and
    /// is the per-user override Explorer checks first.
    pub fn set_association(ext: &str, enabled: bool) -> std::io::Result<()> {
        let hkcu = RegKey::predef(HKEY_CURRENT_USER);
        let classes = hkcu.open_subkey_with_flags("Software\\Classes", KEY_ALL_ACCESS)
            .or_else(|_| hkcu.create_subkey("Software\\Classes").map(|(k, _)| k))?;

        let exe_path = std::env::current_exe()?
            .parent()
            .map(|p| p.join("wvgc-viewer.exe"))
            .unwrap_or_default();
        let exe_str = exe_path.to_string_lossy().to_string();

        if enabled {
            // ProgID definition (shared across extensions, written once/idempotent).
            let (prog_key, _) = classes.create_subkey(PROG_ID)?;
            prog_key.set_value("", &"Watermelon Vector File")?;
            let (icon_key, _) = prog_key.create_subkey("DefaultIcon")?;
            icon_key.set_value("", &format!("{},0", exe_str))?;
            let (cmd_key, _) = prog_key.create_subkey("shell\\open\\command")?;
            cmd_key.set_value("", &format!("\"{}\" \"%1\"", exe_str))?;

            // Extension -> OpenWithProgids entry (what Explorer's picker reads).
            let ext_path = format!(".{}", ext);
            let (ext_key, _) = classes.create_subkey(&ext_path)?;
            let (progids_key, _) = ext_key.create_subkey("OpenWithProgids")?;
            progids_key.set_value(PROG_ID, &"")?;
        } else {
            let ext_path = format!(".{}", ext);
            if let Ok(ext_key) = classes.open_subkey_with_flags(&ext_path, KEY_ALL_ACCESS) {
                if let Ok(progids_key) = ext_key.open_subkey_with_flags("OpenWithProgids", KEY_ALL_ACCESS) {
                    let _ = progids_key.delete_value(PROG_ID);
                }
            }
        }

        // Notify Explorer so the "Open with" list refreshes without a restart.
        unsafe {
            #[link(name = "shell32")]
            extern "system" {
                fn SHChangeNotify(event_id: i32, flags: u32, item1: *const u8, item2: *const u8);
            }
            const SHCNE_ASSOCCHANGED: i32 = 0x08000000;
            const SHCNF_IDLIST: u32 = 0;
            SHChangeNotify(SHCNE_ASSOCCHANGED, SHCNF_IDLIST, std::ptr::null(), std::ptr::null());
        }

        Ok(())
    }

    pub fn is_associated(ext: &str) -> bool {
        let hkcu = RegKey::predef(HKEY_CURRENT_USER);
        let ext_path = format!("Software\\Classes\\.{}\\OpenWithProgids", ext);
        hkcu.open_subkey(&ext_path)
            .and_then(|k| k.get_raw_value(PROG_ID))
            .is_ok()
    }
}
