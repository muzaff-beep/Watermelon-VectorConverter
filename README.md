# Watermelon Vector Converter

[![Rust](https://img.shields.io/badge/Rust-2021-orange?logo=rust)](https://www.rust-lang.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Platforms](https://img.shields.io/badge/platforms-Android%20%7C%20Windows%20%7C%20Linux-brightgreen)]()
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)]()
[![Version](https://img.shields.io/badge/version-1.5-red)]()
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](http://makeapullrequest.com)
[![Rust Version](https://img.shields.io/badge/rust-1.75%2B-orange)](https://www.rust-lang.org/)
[![Android](https://img.shields.io/badge/Android-8.0%2B-green?logo=android)](https://developer.android.com/)
[![Tauri](https://img.shields.io/badge/Tauri-2.x-purple?logo=tauri)](https://tauri.app/)

**A fast, fully offline, multiplatform converter from SVG vector graphics to Android VectorDrawable XML.**

Convert single SVG files or large ZIP batches into high-fidelity Android
`VectorDrawable` XML, with an approximate dual visual preview and flexible
export — entirely offline, with zero network calls.

> **Copyright © 2026 Suhail Muzaffari. All rights reserved.**
> This is **proprietary, source-available** software. It is **not** open source.
> See [`LICENSE.md`](./LICENSE.md). The right to commercialize this software
> belongs exclusively to the copyright holder.

---

## Highlights

- **Offline-first.** No telemetry, no network, no external dependencies at runtime. Android builds ship without the `INTERNET` permission.
- **One shared core.** A single high-performance Rust engine (`svg-converter-core`) drives every platform, so conversion behaves identically everywhere.
- **Batch at scale.** Convert 500+ SVGs from a ZIP in seconds via parallel processing, with live progress and cancellation.
- **Dual preview.** See the original SVG and the generated VectorDrawable side by side (both rendered approximately via resvg).
- **Multiplatform.** Native Android (Jetpack Compose) and native-feeling desktop (Tauri) for Windows and Linux.

## Platforms

| Platform | UI | Status |
|----------|----|--------|
| Android (8.0+) | Jetpack Compose + Material 3 | Native |
| Windows 10/11 | Tauri (web frontend) | Desktop |
| Linux | Tauri (web frontend) | Desktop |

## Architecture

A single Rust core is shared across all platforms; thin platform layers
bridge to it.

```
svg-converter-core (Rust)   ← parsing, conversion, rendering, batch
        │
        ├── JNI bridge       → Android (Kotlin + Jetpack Compose)
        └── Tauri commands   → Desktop (Windows / Linux)
```

The project is built and documented using the **Interface-First Execution
Methodology**: frozen interface contracts are the governing artifact, with
modules built in parallel against those contracts and verified automatically
in CI. See `docs/` for the full handover package.

## Repository Layout

```
WVGC/
├── svg-converter-core/   Rust core (parser, converter, preview, batch, FFI)
├── android/              Android app (Compose UI, ViewModels, SAF, JNI)
├── tauri/                Desktop app (Tauri backend + web frontend)
├── docs/                 Contracts, ADRs, handover package, developer handbook
├── ci/                   Interface verification, test harness, packaging
└── .github/workflows/    CI pipeline
```

## Status

This repository is published in **source-available** form for reference and
transparency. It is under active development and is not accepting external
contributions or redistribution at this time.

## License

Proprietary and source-available — see [`LICENSE.md`](./LICENSE.md).

- You may **view** the source.
- You may **not** use, copy, modify, distribute, compile, run, or
  commercialize it without prior written permission.
- Third-party dependencies retain their own licenses; see
  [`THIRD_PARTY_NOTICES.md`](./THIRD_PARTY_NOTICES.md).

For commercial licensing or any other use, contact **Suhail Muzaffari** —
so.muzaff@gmail.com.
