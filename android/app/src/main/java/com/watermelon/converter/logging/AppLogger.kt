// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.logging

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Lightweight in-process logger + crash saver.
 *
 * - Keeps a rolling in-memory ring buffer of app lifecycle/function logs.
 * - On demand (or on crash) dumps: the ring buffer + this process's own logcat
 *   (which includes native .so output, e.g. Rust panics/aborts and signal
 *   traces) to a timestamped .txt in public Downloads via MediaStore.
 *
 * Note: a fatal native crash (SIGSEGV/SIGABRT in the .so) terminates the
 * process and cannot be fully caught in-process by any JVM handler. The logcat
 * dump captures the surrounding output, which is the practical best a
 * userspace app can do without an NDK signal-handler/breakpad integration.
 */
object AppLogger {

    private const val MAX_LINES = 2000
    private val ring = ConcurrentLinkedQueue<String>()
    private val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val fileTs = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    @Volatile private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        log("AppLogger", "initialized")
    }

    /** Add a line to the in-memory buffer (and to system logcat). */
    fun log(tag: String, message: String) {
        val line = "${ts.format(Date())}  $tag: $message"
        ring.add(line)
        while (ring.size > MAX_LINES) ring.poll()
        android.util.Log.d(tag, message)
    }

    fun logError(tag: String, message: String, t: Throwable? = null) {
        log(tag, "ERROR: $message" + (t?.let { "\n" + it.stackTraceToString() } ?: ""))
    }

    /** Dump current buffer + process logcat to Downloads. Returns the file name, or null. */
    fun dump(reason: String): String? {
        val ctx = appContext ?: return null
        val name = "wvgc_log_${fileTs.format(Date())}.txt"
        val content = buildString {
            appendLine("=== WVGC log dump ===")
            appendLine("reason: $reason")
            appendLine("time: ${ts.format(Date())}")
            appendLine("device: ${Build.MANUFACTURER} ${Build.MODEL}, API ${Build.VERSION.SDK_INT}")
            appendLine("app abi: ${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine()
            appendLine("=== in-app buffer (${ring.size} lines) ===")
            ring.forEach { appendLine(it) }
            appendLine()
            appendLine("=== logcat (this process) ===")
            append(readOwnLogcat())
        }
        return writeToDownloads(ctx, name, content)
    }

    /** Read this process's own logcat (includes native crash output). */
    private fun readOwnLogcat(): String = try {
        val pid = android.os.Process.myPid()
        // -d = dump and exit; filter to our pid; include the crash buffer.
        val proc = ProcessBuilder(
            "logcat", "-d", "-v", "threadtime", "-b", "main", "-b", "crash", "--pid", pid.toString()
        ).redirectErrorStream(true).start()
        proc.inputStream.bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        "logcat capture failed: ${e.message}\n(--pid requires API 24+; on older devices this may be empty)"
    }

    /** Write via MediaStore on API 29+, or to the public Downloads dir below that. */
    private fun writeToDownloads(ctx: Context, fileName: String, content: String): String? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/WVGC")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = ctx.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                fileName
            } else null
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "WVGC")
            dir.mkdirs()
            File(dir, fileName).writeText(content)
            fileName
        }
    } catch (e: Exception) {
        // Last-resort fallback: app-specific external dir (never throws on scoped storage).
        try {
            val dir = File(ctx.getExternalFilesDir(null), "logs").apply { mkdirs() }
            File(dir, fileName).writeText(content)
            "files/logs/$fileName (fallback)"
        } catch (e2: Exception) { null }
    }
}
