# Third-Party Notices

**Watermelon Vector Converter**
Copyright © 2026 Suhail Muzaffari. All rights reserved.

This product incorporates third-party open-source components. Each component
is the property of its respective authors and is licensed under its own
terms, reproduced or referenced below. These notices apply **only** to the
third-party components listed here; all original code in this project is
governed by the proprietary `LICENSE.md` and is **not** open source.

The license texts of the components below are permissive (MIT / Apache-2.0)
and are compatible with redistribution in binary form within a proprietary
product, provided their copyright notices and license terms are preserved —
which is the purpose of this file.

> **Maintenance note:** Verify and pin the exact versions and licenses with
> `cargo about` / `cargo deny` (Rust) and `license-checker` (frontend) at
> release time. Versions below reflect the project blueprint baseline and
> should be regenerated from the actual lockfiles before each release.

---

## Rust Core (`svg-converter-core`)

| Component | Version (baseline) | License |
|-----------|--------------------|---------|
| resvg | 0.44.x | MPL-2.0 / Apache-2.0 (see project) |
| usvg | 0.44.x | MPL-2.0 / Apache-2.0 (see project) |
| tiny-skia | 0.11.x | BSD-3-Clause |
| roxmltree | 0.20.x | MIT / Apache-2.0 |
| zip | 2.x | MIT |
| rayon | 1.10.x | MIT / Apache-2.0 |
| jni (Android target only) | 0.21.x | MIT / Apache-2.0 |

> **Note on resvg/usvg/tiny-skia:** confirm the current upstream license at
> release time. These crates have used MPL-2.0; MPL-2.0 is file-level
> copyleft, meaning modifications to *those files* must be shared, but it
> does **not** impose copyleft on your separate proprietary code that merely
> depends on them. Do not modify their source if you wish to avoid any
> disclosure obligation; use them as unmodified dependencies.

## Desktop (Tauri) — converter app and viewer

| Component | Version (baseline) | License |
|-----------|--------------------|---------|
| Tauri (core, `tauri-build`) | 2.x (stable) | MIT / Apache-2.0 |
| `@tauri-apps/plugin-dialog` | 2.x | MIT / Apache-2.0 |
| `@tauri-apps/plugin-fs` | 2.x | MIT / Apache-2.0 |
| `tauri-plugin-single-instance` (viewer only) | 2.x | MIT / Apache-2.0 |
| wry | (per Tauri) | MIT / Apache-2.0 |
| tao | (per Tauri) | MIT / Apache-2.0 |
| serde / serde_json (Tauri crates, not the shared core) | 1.x | MIT / Apache-2.0 |
| winreg (Windows file-association registration) | 0.52.x | MIT / Apache-2.0 |
| Svelte 4 | 4.x | MIT |
| SvelteKit + `@sveltejs/adapter-static` | 2.x / 3.x | MIT |
| Vite | 5.x | MIT |

## Android

| Component | Version (baseline) | License |
|-----------|--------------------|---------|
| Jetpack Compose (androidx.compose.*) | per BOM | Apache-2.0 |
| AndroidX libraries (core, lifecycle, documentfile, etc.) | per BOM | Apache-2.0 |
| Kotlin standard library | per toolchain | Apache-2.0 |
| cargo-ndk (build tooling) | latest | MIT / Apache-2.0 |

> **Correction:** an earlier revision of this file claimed `androidx.documentfile`
> was unused dead weight. That was wrong — it's still used by
> `util/OutputDestination.kt` for the optional custom output-destination
> picker (Settings > choose where converted files are saved), which is a
> separate feature from the file manager tab (that one reverted to
> `java.io.File` + `MANAGE_EXTERNAL_STORAGE`, unrelated to this dependency).

---

## Full License Texts

The complete text of each license (MIT, Apache-2.0, BSD-3-Clause, MPL-2.0)
must accompany binary distributions. Generate the authoritative, version-
exact list and bundled license texts at build time, for example:

```bash
# Rust
cargo install cargo-about
cargo about generate about.hbs > THIRD_PARTY_RUST.html

cargo install cargo-deny
cargo deny check licenses

# Frontend
npx license-checker --production --out THIRD_PARTY_FRONTEND.txt
```

Replace the baseline tables above with the generated output before each
public release so the notices match the shipped dependency versions exactly.
Module B adds: jni 0.21 (MIT/Apache-2.0) on Android targets.
