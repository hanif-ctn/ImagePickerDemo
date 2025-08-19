// androidMain - Add this to your Android module
package org.hanif.imagepickerdemo

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

actual suspend fun openVideoInExternalPlayer(video: SharedVideo): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val context = AndroidContext.applicationContext
            val videoData = video.data ?: return@withContext false

            // Create a temporary file directory
            val tempDir = File(context.cacheDir, "videos")
            if (!tempDir.exists()) tempDir.mkdirs()

            // Pick extension from mime type
            val extension = when (video.mimeType) {
                "video/mp4" -> ".mp4"
                "video/avi" -> ".avi"
                "video/mov", "video/quicktime" -> ".mov"
                "video/3gpp" -> ".3gp"
                "video/webm" -> ".webm"
                else -> ".mp4"
            }

            val safeName = video.name.replace("/", "_")
            val tempFile = File(tempDir, "$safeName$extension")

            // Write video data
            FileOutputStream(tempFile).use { fos ->
                fos.write(videoData)
                fos.flush()
            }

            Log.d("VideoPlayer", "Saved video to: ${tempFile.absolutePath}, size=${tempFile.length()} bytes")

            // Create content URI
            val contentUri = FileProvider.getUriForFile(
                context,
                "org.hanif.imagepickerdemo.provider",
                tempFile
            )

            // Create VIEW intent
            val intent = Intent(Intent.ACTION_VIEW).apply {
                // Use generic mime for better compatibility
                setDataAndType(contentUri, "video/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Check if there's an app that can handle this intent
            val packageManager = context.packageManager
            val resolveInfo = intent.resolveActivity(packageManager)
            Log.d("VideoPlayer", "Resolved activity: $resolveInfo")

            if (resolveInfo != null) {
                withContext(Dispatchers.Main) {
                    context.startActivity(intent)
                }
                true
            } else {
                // Fallback chooser
                val chooserIntent = Intent.createChooser(intent, "Open video with...")
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                withContext(Dispatchers.Main) {
                    context.startActivity(chooserIntent)
                }
                true
            }
        } catch (e: Exception) {
            Log.e("VideoPlayer", "Error opening video", e)
            false
        }
    }
}

// Application context holder
object AndroidContext {
    lateinit var applicationContext: Context
    fun init(context: Context) {
        applicationContext = context.applicationContext
    }
}