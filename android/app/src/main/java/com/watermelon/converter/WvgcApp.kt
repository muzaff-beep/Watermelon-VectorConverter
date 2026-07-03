// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter

import android.app.Application
import com.watermelon.converter.logging.AppLogger

/**
 * Custom Application. Installs the uncaught-exception handler as early as
 * possible so launch-time crashes (including those before MainActivity.onCreate
 * completes, and System.loadLibrary failures) are captured and written to disk.
 */
class WvgcApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
        AppLogger.log("WvgcApp", "onCreate — process start")
        installCrashHandler()
    }

    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                AppLogger.logError("CRASH", "uncaught on thread ${thread.name}", throwable)
                val saved = AppLogger.dump("uncaught exception")
                AppLogger.log("CRASH", "log saved as: $saved")
            } catch (_: Throwable) {
                // never let the handler itself throw
            } finally {
                // hand off to the platform/default handler so the OS still
                // shows the crash dialog and records its own tombstone.
                previous?.uncaughtException(thread, throwable)
            }
        }
    }
}
