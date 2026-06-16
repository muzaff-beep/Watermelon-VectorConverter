// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Module-level Gradle config. preBuild runs cargo-ndk to build the Rust core.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.watermelon.converter"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.watermelon.converter"
        minSdk = 26          // TODO: confirm min-SDK (affects NDK ABIs + Compose baseline)
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
    splits { abi { isEnable = true; reset(); include("arm64-v8a", "armeabi-v7a") } }
    buildFeatures { compose = true }
}
// TODO(M/B): preBuild dependsOn cargoNdkBuild
