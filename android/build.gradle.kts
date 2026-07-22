// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Top-level build file.
plugins {
    id("com.android.application") version "9.2.0" apply false
    id("org.jetbrains.kotlin.android") version "2.4.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
    id("com.google.devtools.ksp") version "2.4.10-1.0.30" apply false
}

// AGP 9.0+ ships built-in Kotlin support and deprecates the standalone
// org.jetbrains.kotlin.android plugin path. We stay opted OUT for now
// (old DSL/variant API) to avoid a blind breaking migration; AGP 10.0
// (mid-2026) removes this opt-out, so plan the built-in-Kotlin migration
// before then. See android/gradle.properties: android.builtInKotlin=false
