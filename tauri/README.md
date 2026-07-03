# Watermelon Vector Converter — Desktop (Windows)

Tauri v2 + Svelte desktop app. Shares the Rust conversion core with the Android app.

## Stack

| Layer | Technology |
|---|---|
| Shell | Tauri v2 |
| Frontend | Svelte 4 + Vite 5 |
| Backend | Rust (`wvgc-desktop`) |
| Core | `svg-converter-core` (shared with Android) |

## Dev setup

```bash
# Prerequisites: Node 20+, Rust stable, Tauri CLI v2
cd tauri
npm install
npm run tauri dev
```

## Release build (Windows)

```bash
npm run tauri build
# Output: src-tauri/target/release/bundle/nsis/*.exe
#         src-tauri/target/release/bundle/msi/*.msi
```

## Project structure

```
tauri/
  src/                     # Svelte frontend
    routes/
      +layout.svelte       # Sidebar nav shell
      +page.svelte         # Single-file conversion
      batch/+page.svelte   # Batch ZIP conversion
    components/
      Nav.svelte
      FileDropZone.svelte
      PreviewPane.svelte
      WatermelonButton.svelte
      ProgressBar.svelte
    lib/
      tauri.ts             # Type-safe invoke() wrappers
  src-tauri/               # Rust backend
    src/
      main.rs              # Tauri builder
      commands.rs          # #[tauri::command] handlers
    tauri.conf.json
    Cargo.toml
```

## Commands (Contract C-3)

| Command | Input | Output |
|---|---|---|
| `convert_svg` | `svg: Vec<u8>` | `String` (VD XML) |
| `render_svg_preview` | `svg: Vec<u8>`, `px: u32` | `Vec<u8>` (PNG) |
| `render_vd_preview` | `vd_xml: String`, `px: u32` | `Vec<u8>` (PNG) |
| `convert_zip` | `zip: Vec<u8>` | `Vec<u8>` (ZIP of XMLs) |
