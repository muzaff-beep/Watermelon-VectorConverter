# Watermelon Vector Viewer

A lightweight, standalone Tauri app (`wvgc-viewer`) for previewing `.svg`
and Android VectorDrawable `.xml` files directly from the OS file explorer.
Separate binary from the main converter app, sharing the same Rust core.

## Why a separate app

The main converter (`tauri/`) is a full conversion workflow; this viewer is
meant to be registered as the **default file handler** for `.svg`/`.xml` so
double-clicking a file in Explorer/Finder/file manager opens a quick preview
instead of launching the whole converter UI. Single-instance: opening a
second file while the viewer is already running re-uses the same window
rather than spawning a new one.

## Stack

| Layer | Technology |
|---|---|
| Shell | Tauri v2 |
| Frontend | Svelte 4 + Vite 5 (SvelteKit, static adapter, SSR disabled) |
| Backend | Rust (`wvgc-viewer` binary) |
| Core | `svg-converter-core` (shared with Android and the main desktop app) |
| Plugins | `tauri-plugin-single-instance`, `tauri-plugin-dialog` |

## Dev setup

```bash
cd viewer
npm install
npm run tauri dev
```

## How it opens a file

1. **Launched directly with a file path** (e.g. via file association):
   `main.rs`/`lib.rs` extracts the first non-flag CLI argument as the
   pending file path, stored in `PendingFile` app state.
2. **Frontend calls `take_pending_file`** once on startup to retrieve and
   clear that path, then renders it.
3. **A second launch while already running**: `tauri-plugin-single-instance`
   intercepts it, stores the new path in the same `PendingFile` state, emits
   a `"viewer://open-file"` event to the existing window, and brings that
   window to focus — no second process/window is created.

## Commands

| Command | Input | Output | Notes |
|---|---|---|---|
| `render_file_preview` | `path: String`, `px: u32` | `Vec<u8>` (PNG) | Reads the file, detects SVG vs. VectorDrawable by root tag (`<svg>` vs `<vector>`) rather than file extension, renders accordingly |
| `take_pending_file` | — | `Option<String>` | One-shot: returns and clears the file path that launched this instance |

Errors are surfaced as `ViewerErrorDto { message: String }`, built via two
concrete `From` impls (`std::io::Error` for file-read failures,
`svg_converter_core::error::ConversionError` for parse/render failures) —
deliberately not a blanket `impl<T: Display> From<T>`, which conflicts with
the standard library's reflexive `impl<T> From<T> for T` and fails to
compile (`E0119`).

## Project structure

```
viewer/
  src/routes/+page.svelte    # The entire UI: image + filename bar + "Open…" button
  src-tauri/
    src/
      main.rs
      lib.rs                 # Builder, single-instance handling, PendingFile state
      commands.rs            # render_file_preview, take_pending_file
    tauri.conf.json
    Cargo.toml
```
