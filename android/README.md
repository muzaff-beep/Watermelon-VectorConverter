# WVGC Android

Built in CI (GitHub Actions, `.github/workflows/android.yml`). The workflow
builds the Rust core into per-ABI `.so` files via `cargo-ndk`, then assembles
the APK and runs unit + instrumented tests.

Local build (optional): install Android Studio, Rust, and
`cargo install cargo-ndk`, add the Android Rust targets, then run the same
cargo-ndk command from `svg-converter-core/` before `./gradlew assembleDebug`.

Note: you must add the Gradle wrapper JAR + `gradlew` script (run
`gradle wrapper` once locally) — only `gradle-wrapper.properties` is checked in here.
