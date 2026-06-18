// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
import org.gradle.api.tasks.Exec

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")   // Kotlin 2.0+ Compose compiler
    id("com.google.devtools.ksp")               // annotation processing
}

android {
    namespace = "com.watermelon.converter"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.watermelon.converter"
        minSdk = 26          // TODO: confirm; drives NDK ABIs + Compose baseline
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a")
            isUniversalApk = false
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures { compose = true }
    // composeOptions block removed: Kotlin 2.0+ uses the compose plugin above
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Where cargo-ndk places the built .so files.
    sourceSets["main"].jniLibs.srcDirs("src/main/jniLibs")
}

// Kotlin compiler options live on the kotlin {} extension (Kotlin 2.0 plugin),
// not inside android {} where the old kotlinOptions shim used to sit.
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// --- cargo-ndk hook: build the Rust core .so before packaging ---
// Requires: rustup target aarch64-linux-android
//           and `cargo install cargo-ndk`.
val cargoNdkBuild by tasks.registering(Exec::class) {
    workingDir = rootDir.parentFile.resolve("svg-converter-core")
    commandLine(
        "cargo", "ndk",
        "-t", "arm64-v8a",
        "-o", "${projectDir}/src/main/jniLibs",
        "build", "--release"
    )
}
tasks.named("preBuild") { dependsOn(cargoNdkBuild) }
