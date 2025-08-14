// iosMain - Add this to your iOS module
package org.hanif.imagepickerdemo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.*
import platform.UIKit.*
import platform.AVKit.*
import platform.AVFoundation.*
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
actual suspend fun openVideoInExternalPlayer(video: SharedVideo): Boolean {
    return withContext(Dispatchers.Main) {
        try {
            val videoData = video.data ?: return@withContext false

            // Create NSData from ByteArray
            val nsData = videoData.usePinned { pinned ->
                NSData.dataWithBytes(pinned.addressOf(0), videoData.size.toULong())
            }

            // Create temporary file URL
            val tempDirectory = NSFileManager.defaultManager.temporaryDirectory
            val fileName = video.name.replace("/", "_")
            val fileExtension = when (video.mimeType) {
                "video/mp4" -> "mp4"
                "video/avi" -> "avi"
                "video/mov", "video/quicktime" -> "mov"
                "video/3gpp" -> "3gp"
                "video/webm" -> "webm"
                else -> "mp4"
            }

            val tempFileURL = tempDirectory.URLByAppendingPathComponent("$fileName.$fileExtension")
                ?: return@withContext false

            // Write data to temporary file
            memScoped {
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                val writeSuccess = nsData.writeToURL(
                    url = tempFileURL,
                    options = NSDataWritingAtomic,
                    error = errorPtr.ptr
                )
                if (!writeSuccess) {
                    return@withContext false
                }
            }

            // Create AVPlayer and AVPlayerViewController
            val player = AVPlayer.playerWithURL(tempFileURL)
            val playerViewController = AVPlayerViewController()
            playerViewController.player = player

            // Present the video player
            val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            rootViewController?.presentViewController(playerViewController, animated = true) {
                // Start playing automatically
                player.play()
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}