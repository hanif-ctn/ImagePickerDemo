// androidMain
package org.hanif.imagepickerdemo

import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

actual suspend fun openDocumentInExternalViewer(document: SharedDocument): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val context = AndroidContext.applicationContext
            val documentData = document.data ?: run { return@withContext false }

            // Create a temporary file
            val tempDir = File(context.cacheDir, "documents")
            if (!tempDir.exists()) {
                val created = tempDir.mkdirs()
                println("Created temp directory: $created")
            }

            // Get file extension
            val extension = getFileExtensionFromMimeType(document.mimeType)
                ?: document.name.substringAfterLast(".", "tmp")

            val sanitizedName = document.name.replace("/", "_").replace("\\", "_")
            val tempFile = File(tempDir, "$sanitizedName.$extension")
            println("Temp file path: ${tempFile.absolutePath}")

            // Write document data to temporary file
            FileOutputStream(tempFile).use { fos ->
                fos.write(documentData)
                fos.flush()
            }
            println("File written successfully, size: ${tempFile.length()}")

            // Create content URI using FileProvider
            val contentUri = FileProvider.getUriForFile(
                context,
                "org.hanif.imagepickerdemo.provider",
                tempFile
            )
            println("Content URI created: $contentUri")

            // Create intent to open document
            val mimeType = document.mimeType ?: "application/octet-stream"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mimeType)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            println("Intent created with mimeType: $mimeType")

            // Check if there's an app that can handle this intent
            val packageManager = context.packageManager
            val resolveInfo = intent.resolveActivity(packageManager)

            if (resolveInfo != null) {
                println("Found app to handle intent: ${resolveInfo.packageName}")
                withContext(Dispatchers.Main) {
                    context.startActivity(intent)
                }
                true
            } else {
                println("No app found to handle intent, trying chooser")
                // Fallback: try to open with any available app
                val chooserIntent = Intent.createChooser(intent, "Open document with...")
                chooserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                withContext(Dispatchers.Main) {
                    context.startActivity(chooserIntent)
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("FileProvider error: ${e.message}") // Debug log
            println("Error details: ${e.javaClass.simpleName}: ${e.localizedMessage}")
            false
        }
    }
}

private fun getFileExtensionFromMimeType(mimeType: String?): String? {
    return when (mimeType) {
        "application/pdf" -> "pdf"
        "application/msword" -> "doc"
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx"
        "application/vnd.ms-excel" -> "xls"
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx"
        "application/vnd.ms-powerpoint" -> "ppt"
        "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> "pptx"
        "text/plain" -> "txt"
        "text/html" -> "html"
        "text/css" -> "css"
        "text/javascript" -> "js"
        "application/json" -> "json"
        "application/xml", "text/xml" -> "xml"
        "application/zip" -> "zip"
        "application/x-rar-compressed" -> "rar"
        "application/x-7z-compressed" -> "7z"
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/gif" -> "gif"
        "image/webp" -> "webp"
        "video/mp4" -> "mp4"
        "video/avi" -> "avi"
        "video/mov", "video/quicktime" -> "mov"
        "audio/mp3", "audio/mpeg" -> "mp3"
        "audio/wav" -> "wav"
        else -> null
    }
}
