# Watermelon Vector Converter — Android

Native Android app (Jetpack Compose + Material 3). Shares the Rust
conversion core with the desktop apps via JNI.

## Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Language | Kotlin |
| Native bridge | JNI (`libsvg_converter_core.so`, built via `cargo-ndk`) |
| Core | `svg-converter-core` (shared with desktop) |

- `minSdk` 26, `targetSdk`/`compileSdk` 35.
- Uses `MANAGE_EXTERNAL_STORAGE` ("All files access") so the file manager
  tab can browse the whole device — internal shared storage and any SD
  card — from one grant, without per-folder SAF picking. This is a
  sensitive permission: it's granted via a system Settings screen (no
  runtime dialog), and Play Store distribution requires the app's core
  purpose to be file management to qualify for it.
- `androidx.documentfile` is a separate, still-active dependency used only
  by the optional custom output-destination picker (`util/OutputDestination.kt`),
  not the file manager tab.

## Dev setup

```bash
cd android
./gradlew assembleDebug
```

Release builds require signing config (`digital_raven_release.jks`) and are
built via GitHub Actions CI, not locally — this project has no local Android
build/emulator environment; all validation runs through CI.

## Project structure

```
android/app/src/main/java/com/watermelon/converter/
  ui/
    screens/            HomeScreen, FilesScreen, PreviewScreen, ExportScreen,
                         BatchScreen (direction-parameterized), SettingsScreen,
                         HistoryScreen, AboutScreen
    components/         Shared composables (icons, panels, loaders)
    theme/               Colors, typography
  viewmodel/
    ConversionViewModel / ReverseConversionViewModel   Single-file, forward/reverse
    BatchViewModel / ReverseBatchViewModel              Batch ZIP, forward/reverse
    FileManagerViewModel                                File browser tab
    SettingsViewModel
  data/
    files/              FileNode, RealFileRepository, MarkedFilesStore (java.io.File-based)
    model/               VectorProperties, HistoryStore, BatchReport
    prefs/               SettingsRepository (DataStore)
    repository/          FileRepository (URI ↔ File helpers, zipping loose files)
  jni/                  SvgConverter (interface) / RealSvgConverter (impl) / SvgConverterNative (externs)
  util/                 StoragePermission, WvgcPaths, OutputDestination, AppLogger
```

## Home screen

Two conversion directions, each with **Single** and **Batch** buttons
beneath it:

- **Convert SVG to XML** → Single opens a file picker for one `.svg`;
  Batch navigates to the batch screen for a ZIP of `.svg` files.
- **Convert XML to SVG** → same shape, for `.xml` (VectorDrawable) input.

The "About" link is on this screen; "History" lives in Settings.

## SVG/VectorDrawable viewer (file association)

`ui/viewer/SvgViewerActivity.kt` is registered for `ACTION_VIEW` on
`image/svg+xml`, `text/xml`, and `application/xml`, so double-tapping either
file type elsewhere on the device (a file manager, a download notification,
etc.) opens this lightweight preview instead of the full app. It detects SVG
vs. VectorDrawable by the root tag (`<svg>` vs `<vector>`), not by file
extension or declared MIME type, mirroring the desktop viewer app's
detection logic.

## File manager tab

Browses the real device filesystem (internal storage + any detected SD
card), filtered to SVG/XML by default with toggle switches to show/hide
each type. Listing is two-phase: file names and extensions render
immediately, then size/last-modified are filled in shortly after — so the
list appears instantly even in large folders.

## Native bridge (JNI)

| Kotlin (`SvgConverterNative`) | Direction | Notes |
|---|---|---|
| `nativeConvertSvg` | SVG → XML | Single file |
| `nativeConvertVd` | XML → SVG | Single file |
| `nativeConvertZip` | SVG → XML | Batch, with `ProgressCallback` |
| `nativeConvertVdZip` | XML → SVG | Batch, with `ProgressCallback` |
| `nativeRenderSvgPreview` / `nativeRenderVdPreview` | — | Preview PNG rendering, either direction |
| `nativeAnalyzeVector` / `nativeAnalyzeVdVector` | — | Properties panel JSON, SVG input vs. VD input respectively |
| `nativeCancel` | — | Shared cancel flag for whichever batch job is running |

`SvgConverter` is the Kotlin-side interface all ViewModels depend on;
`RealSvgConverter` is the only implementation, delegating to the externs
above.
