# Watermelon Vector Converter — Desktop (Windows / Linux)

Tauri v2 + Svelte desktop app. Shares the Rust conversion core with the
Android app and the standalone viewer.

## Stack

| Layer | Technology |
|---|---|
| Shell | Tauri v2 |
| Frontend | Svelte 4 + Vite 5 |
| Backend | Rust (Tauri commands, this crate) |
| Core | `svg-converter-core` (shared with Android and the viewer) |

## Dev setup

```bash
# Prerequisites: Node 20+, Rust stable, Tauri CLI v2
cd tauri
npm install
npm run tauri dev
```

## Release build (Windows / Linux)

```bash
npm run tauri build
# Windows output: src-tauri/target/release/bundle/nsis/*.exe
#                 src-tauri/target/release/bundle/msi/*.msi
# Linux output:   src-tauri/target/release/bundle/appimage/*.AppImage
#                 src-tauri/target/release/bundle/deb/*.deb
```

## Project structure

```
tauri/
  src/                       # Svelte frontend
    routes/
      +layout.svelte         # Sidebar nav shell
      +page.svelte           # Convert: SVG↔XML, one smart drop zone per direction
      settings/+page.svelte  # Output destination, preview size, file associations
      about/+page.svelte
    components/
      Nav.svelte
      FileDropZone.svelte    # single/multi/zip drop + click-to-browse, per direction
      PreviewPane.svelte     # dual preview (direction-aware labels)
      WatermelonButton.svelte
      ProgressBar.svelte
      AssocNoticeModal.svelte
  src-tauri/                 # Rust backend
    src/
      main.rs                # Tauri entry point
      lib.rs                 # Builder, invoke_handler registration
      commands.rs            # #[tauri::command] handlers
    tauri.conf.json
    Cargo.toml
```

## Convert page behavior

The main page has two stacked sections — **Convert SVG to XML** and
**Convert XML to SVG** — each with a single drop zone that classifies
whatever you give it:

- **One loose file** → converts immediately, single-file result.
- **Two or more loose files** → zipped in memory (`zip_loose_files`), then
  run as one batch job.
- **One ZIP** → run as a batch job directly.
- **Multiple ZIPs** → each ZIP is its own independent batch job, producing
  its own output ZIP (they are never merged together).

Drag-and-drop uses Tauri's native `onDragDropEvent` API rather than the
browser's DOM drop event — Tauri v2's webview intercepts OS-level file drops
before the DOM sees them (`dragDropEnabled` defaults to `true`), so a plain
`on:drop` handler never fires for files dragged in from the file explorer.

## Commands

| Command | Input | Output | Notes |
|---|---|---|---|
| `convert_svg` | `svg: Vec<u8>` (JSON) | `String` (VD XML) | Single file, forward direction |
| `convert_vd` | `vdXml: Vec<u8>` (JSON) | `String` (SVG) | Single file, reverse direction |
| `render_svg_preview` | `svg: Vec<u8>`, `px: u32` (JSON) | `Vec<u8>` (PNG) | |
| `render_vd_preview` | `vdXml: String`, `px: u32` (JSON) | `Vec<u8>` (PNG) | |
| `convert_zip` | **raw request body** (ZIP bytes) | `Vec<u8>` (ZIP of XML) | Batch, forward. Emits `batch://progress` |
| `convert_vd_zip` | **raw request body** (ZIP bytes) | `Vec<u8>` (ZIP of SVG) | Batch, reverse. Emits `batch://progress` |
| `zip_loose_files` | `files: [{name, bytes}]` (JSON) | `Vec<u8>` (ZIP) | Packages multi-select before handing off to `convert_zip`/`convert_vd_zip` |
| `open_url` | `url: String` | — | |
| `set_file_association` / `get_file_association` | `ext: String`, `enabled: bool` | — | OS file-association registration |

**Why `convert_zip`/`convert_vd_zip` take a raw body instead of a JSON
argument:** Tauri's default IPC JSON-encodes a `Vec<u8>` argument as a
`number[]`, which is slow enough for a real-world ZIP to look like a hang
with no error. These two commands instead read `tauri::ipc::Request`'s raw
body directly. The frontend must call `invoke(command, bytes)` with `bytes`
as a raw `Uint8Array`/`ArrayBuffer` — not `invoke(command, { zip: [...] })`.
Every other command still uses ordinary JSON args/returns since their
payloads are small (a single file, or a rendered preview PNG).

## Progress events

Both batch commands emit `"batch://progress"` with `{ done, total,
current_name }`. The frontend listens for this once (in `+page.svelte`) and
applies it to whichever batch job is currently running — only one batch job
runs at a time even though a multi-ZIP selection queues several.
