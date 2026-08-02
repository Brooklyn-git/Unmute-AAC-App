package com.unmute.app.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Copies gallery photos into the app's private storage so cards keep working even
 * if the original content URI loses permission.
 */
object PhotoStore {

    fun save(context: Context, uri: Uri): String? {
        val dir = File(context.filesDir, PHOTOS_DIR).apply { mkdirs() }
        val dest = File(dir, "photo_${System.currentTimeMillis()}.jpg")
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            input.use { stream ->
                FileOutputStream(dest).use { out -> stream.copyTo(out) }
            }
            if (!dest.exists() || dest.length() == 0L) {
                dest.delete()
                null
            } else {
                dest.absolutePath
            }
        } catch (_: IOException) {
            dest.delete()
            null
        }
    }

    fun delete(path: String?) {
        if (path == null) return
        runCatching { File(path).delete() }
    }

    private const val PHOTOS_DIR = "photos"
}
