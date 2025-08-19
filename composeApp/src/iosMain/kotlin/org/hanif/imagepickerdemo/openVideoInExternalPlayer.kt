// iosMain - Simplified and Fixed iOS module implementation
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

            // Get documents directory for better reliability
            val fileManager = NSFileManager.defaultManager
            val urls = fileManager.URLsForDirectory(
                NSDocumentDirectory,
                NSUserDomainMask
            )
            val documentsDirectory = urls.firstOrNull() as? NSURL
                ?: return@withContext false

            // Create videos subdirectory
            val videosDirectory = documentsDirectory.URLByAppendingPathComponent("Videos")
            if (videosDirectory != null) {
                memScoped {
                    val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                    fileManager.createDirectoryAtURL(
                        videosDirectory,
                        withIntermediateDirectories = true,
                        attributes = null,
                        error = errorPtr.ptr
                    )
                }
            }

            // Create file URL with proper extension
            val fileName = video.name.replace(Regex("[/\\\\:*?\"<>|]"), "_")
            val fileExtension = when (video.mimeType?.lowercase()) {
                "video/mp4" -> "mp4"
                "video/avi" -> "avi"
                "video/mov", "video/quicktime" -> "mov"
                "video/3gpp" -> "3gp"
                "video/webm" -> "webm"
                else -> "mp4"
            }

            val timestamp = NSDate().timeIntervalSince1970.toLong()
            val finalFileName = "${fileName}_$timestamp.$fileExtension"
            val fileURL = videosDirectory?.URLByAppendingPathComponent(finalFileName)
                ?: return@withContext false

            // Write data to file
            memScoped {
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                val writeSuccess = nsData.writeToURL(
                    url = fileURL,
                    options = NSDataWritingAtomic,
                    error = errorPtr.ptr
                )

                if (!writeSuccess) {
                    val error = errorPtr.value
                    println("Failed to write video file: ${error?.localizedDescription}")
                    return@withContext false
                }
            }

            // Verify file exists and has content
            val fileExists = fileManager.fileExistsAtPath(fileURL.path!!)
            if (!fileExists) {
                println("File was not created successfully")
                return@withContext false
            }

            // Get file size for verification
            memScoped {
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                val attributes = fileManager.attributesOfItemAtPath(fileURL.path!!, errorPtr.ptr)
                val fileSize = attributes?.get(NSFileSize) as? NSNumber
                println("Video file created: ${fileURL.path}, size: ${fileSize?.longLongValue} bytes")
            }

            // Get the root view controller using the simpler approach
            val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            if (rootViewController == null) {
                println("Could not find root view controller")
                return@withContext false
            }

            // Create AVPlayer and AVPlayerViewController
            val player = AVPlayer.playerWithURL(fileURL)
            val playerViewController = AVPlayerViewController()
            playerViewController.player = player

            // Set additional properties for better user experience
            playerViewController.showsPlaybackControls = true
            if (playerViewController.respondsToSelector(NSSelectorFromString("setAllowsPictureInPicturePlayback:"))) {
                playerViewController.allowsPictureInPicturePlayback = true
            }

            // Present the video player
            rootViewController.presentViewController(
                playerViewController,
                animated = true
            ) {
                // Start playing automatically
                player.play()
            }

            true
        } catch (e: Exception) {
            println("Error opening video: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}