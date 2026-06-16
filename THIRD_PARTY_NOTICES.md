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
| resvg | 0.47.x | MPL-2.0 / Apache-2.0 (see project) |
| usvg | 0.47.x | MPL-2.0 / Apache-2.0 (see project) |
| tiny-skia | 0.12.x | BSD-3-Clause |
| roxmltree | 0.20.x | MIT / Apache-2.0 |
| quick-xml | 0.37.x | MIT |
| zip | 2.2.x | MIT |
| base64 | 0.22.x | MIT / Apache-2.0 |
| jni | 0.21.x | MIT / Apache-2.0 |
| rayon | 1.10.x | MIT / Apache-2.0 |
| anyhow | 1.0.x | MIT / Apache-2.0 |
| serde | 1.0.x | MIT / Apache-2.0 |

> **Note on resvg/usvg/tiny-skia:** confirm the current upstream license at
> release time. These crates have used MPL-2.0; MPL-2.0 is file-level
> copyleft, meaning modifications to *those files* must be shared, but it
> does **not** impose copyleft on your separate proprietary code that merely
> depends on them. Do not modify their source if you wish to avoid any
> disclosure obligation; use them as unmodified dependencies.

## Desktop (Tauri)

| Component | Version (baseline) | License |
|-----------|--------------------|---------|
| Tauri (tauri, tauri-build, plugins) | 2.x (stable) | MIT / Apache-2.0 |
| wry | (per Tauri) | MIT / Apache-2.0 |
| tao | (per Tauri) | MIT / Apache-2.0 |
| Frontend framework (Svelte or React) | TBD | MIT |
| Frontend toolchain (Vite, TypeScript, etc.) | TBD | MIT / Apache-2.0 |

## Android

| Component | Version (baseline) | License |
|-----------|--------------------|---------|
| Jetpack Compose (androidx.compose.*) | per BOM | Apache-2.0 |
| AndroidX libraries (core, lifecycle, documentfile, etc.) | per BOM | Apache-2.0 |
| Kotlin standard library | per toolchain | Apache-2.0 |
| cargo-ndk (build tooling) | latest | MIT / Apache-2.0 |

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
