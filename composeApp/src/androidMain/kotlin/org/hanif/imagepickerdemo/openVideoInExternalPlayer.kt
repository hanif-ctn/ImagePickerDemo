// androidMain - Add this to your Android module
package org.hanif.imagepickerdemo

import android.content.Context
import android.content.Intent
import android.net.Uri
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
            
            // Create a temporary file
            val tempDir = File(context.cacheDir, "videos")
            if (!tempDir.exists()) {
                tempDir.mkdirs()
            }
            
            // Get file extension from mime type
            val extension = when (video.mimeType) {
                "video/mp4" -> ".mp4"
                "video/avi" -> ".avi"
                "video/mov", "video/quicktime" -> ".mov"
                "video/3gpp" -> ".3gp"
                "video/webm" -> ".webm"
                else -> ".mp4" // Default to mp4
            }
            
            val tempFile = File(tempDir, "${video.name.replace("/", "_")}$extension")
            
            // Write video data to temporary file
            FileOutputStream(tempFile).use { fos ->
                fos.write(videoData)
                fos.flush()
            }
            
            // Create content URI using FileProvider
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider", // You'll need to configure this in AndroidManifest.xml
                tempFile
            )
            
            // Create intent to open video
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, video.mimeType)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            
            // Check if there's an app that can handle this intent
            val packageManager = context.packageManager
            if (intent.resolveActivity(packageManager) != null) {
                withContext(Dispatchers.Main) {
                    context.startActivity(intent)
                }
                true
            } else {
                // Fallback: try to open with any available app
                val chooserIntent = Intent.createChooser(intent, "Open video with...")
                chooserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                withContext(Dispatchers.Main) {
                    context.startActivity(chooserIntent)
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

// You'll need to add this object to get application context
object AndroidContext {
    lateinit var applicationContext: Context
    
    fun init(context: Context) {
        applicationContext = context.applicationContext
    }
}