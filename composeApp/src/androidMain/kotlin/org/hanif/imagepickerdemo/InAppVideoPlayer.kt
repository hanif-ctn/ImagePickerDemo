// androidMain - InAppVideoPlayer.kt
package org.hanif.imagepickerdemo

import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File
import java.io.FileOutputStream

@Composable
actual fun InAppVideoPlayer(
    video: SharedVideo,
    modifier: Modifier
) {
    val context = AndroidContext.applicationContext

    // Convert ByteArray to temp file URI
    val videoUri: Uri? = remember(video) {
        video.data?.let { bytes ->
            val tempDir = File(context.cacheDir, "videos")
            if (!tempDir.exists()) tempDir.mkdirs()

            // Pick extension based on mimeType
            val extension = when (video.mimeType) {
                "video/mp4" -> ".mp4"
                "video/avi" -> ".avi"
                "video/mov", "video/quicktime" -> ".mov"
                "video/3gpp" -> ".3gp"
                "video/webm" -> ".webm"
                else -> ".mp4"
            }

            val tempFile = File(tempDir, "${video.name.replace("/", "_")}$extension")
            FileOutputStream(tempFile).use { it.write(bytes) }
            Uri.fromFile(tempFile)
        }
    }

    if (videoUri != null) {
        val exoPlayer = remember {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(videoUri))
                prepare()
                playWhenReady = true
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                exoPlayer.release()
            }
        }

        AndroidView(
            modifier = modifier,
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = true // playback controls
                }
            }
        )
    }
}
