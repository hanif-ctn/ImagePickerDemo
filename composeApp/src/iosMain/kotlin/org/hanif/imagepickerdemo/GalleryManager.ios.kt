// iosMain/kotlin/org/hanif/imagepickerdemo/GalleryManager.ios.kt
package org.hanif.imagepickerdemo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.Foundation.*
import platform.PhotosUI.*
import platform.UIKit.*
import platform.UniformTypeIdentifiers.*
import platform.darwin.NSObject
import platform.darwin.dispatch_group_create
import platform.darwin.dispatch_group_enter
import platform.darwin.dispatch_group_leave
import platform.darwin.dispatch_group_notify
import platform.posix.memcpy
import org.jetbrains.skia.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.addressOf

@Composable
actual fun rememberGalleryManager(
    type: PickerType,
    isSingleSelection: Boolean,
    onResult: (List<SharedImage>?) -> Unit
): GalleryManager {
    val scope = remember { CoroutineScope(Dispatchers.Main) }
    val app = UIApplication.sharedApplication

    // PHPickerViewController delegate for modern image/video picking with multiple selection support
    val phPickerDelegate = remember {
        object : NSObject(), PHPickerViewControllerDelegateProtocol {
            override fun picker(
                picker: PHPickerViewController,
                didFinishPicking: List<*>
            ) {
                picker.dismissViewControllerAnimated(flag = true, completion = null)

                @Suppress("UNCHECKED_CAST")
                val results = didFinishPicking as List<PHPickerResult>

                if (results.isEmpty()) {
                    onResult(null)
                    return
                }

                val dispatchGroup = dispatch_group_create()
                val sharedImages = mutableListOf<SharedImage>()

                for (result in results) {
                    dispatch_group_enter(dispatchGroup)

                    // Try to load as image first
                    if (result.itemProvider.hasItemConformingToTypeIdentifier("public.image")) {
                        result.itemProvider.loadDataRepresentationForTypeIdentifier(
                            typeIdentifier = "public.image"
                        ) { nsData, error ->
                            scope.launch(Dispatchers.Main) {
                                nsData?.let { data ->
                                    val bytes = nsDataToByteArray(data)
                                    val shared = SharedImage(bytes, "image/jpeg", null)
                                    sharedImages.add(shared)
                                }
                                dispatch_group_leave(dispatchGroup)
                            }
                        }
                    }
                    // Try to load as video
                    else if (result.itemProvider.hasItemConformingToTypeIdentifier("public.movie")) {
                        result.itemProvider.loadFileRepresentationForTypeIdentifier(
                            typeIdentifier = "public.movie"
                        ) { url, error ->
                            scope.launch(Dispatchers.Main) {
                                url?.let { fileUrl ->
                                    val data = NSData.dataWithContentsOfURL(fileUrl)
                                    data?.let {
                                        val bytes = nsDataToByteArray(it)
                                        val filename = fileUrl.lastPathComponent
                                        val shared = SharedImage(bytes, "video/mp4", filename)
                                        sharedImages.add(shared)
                                    }
                                }
                                dispatch_group_leave(dispatchGroup)
                            }
                        }
                    } else {
                        dispatch_group_leave(dispatchGroup)
                    }
                }

                dispatch_group_notify(dispatchGroup, platform.darwin.dispatch_get_main_queue()) {
                    scope.launch(Dispatchers.Main) {
                        onResult(if (sharedImages.isNotEmpty()) sharedImages else null)
                    }
                }
            }
        }
    }

    // Fallback UIImagePickerController delegate for video-only picking
    val imagePicker = UIImagePickerController()
    val galleryDelegate = remember {
        object : NSObject(), UIImagePickerControllerDelegateProtocol,
            UINavigationControllerDelegateProtocol {
            override fun imagePickerController(
                picker: UIImagePickerController,
                didFinishPickingMediaWithInfo: Map<Any?, *>
            ) {
                // image case
                val image = didFinishPickingMediaWithInfo[UIImagePickerControllerEditedImage]
                        as? UIImage
                    ?: didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage

                if (image != null) {
                    val imageData = UIImageJPEGRepresentation(image, 0.9) ?: run {
                        picker.dismissViewControllerAnimated(true, null)
                        onResult(null)
                        return
                    }
                    val bytes = nsDataToByteArray(imageData)
                    val shared = SharedImage(bytes, "image/jpeg", null)
                    onResult(listOf(shared))
                    picker.dismissViewControllerAnimated(true, null)
                    return
                }

                // video case
                val mediaURL =
                    didFinishPickingMediaWithInfo[UIImagePickerControllerMediaURL] as? NSURL
                if (mediaURL != null) {
                    val data = NSData.dataWithContentsOfURL(mediaURL) ?: run {
                        picker.dismissViewControllerAnimated(true, null)
                        onResult(null)
                        return
                    }
                    val bytes = nsDataToByteArray(data)
                    val filename = mediaURL.lastPathComponent
                    val mime = "video/mp4"
                    val shared = SharedImage(bytes, mime, filename)
                    onResult(listOf(shared))
                    picker.dismissViewControllerAnimated(true, null)
                    return
                }

                picker.dismissViewControllerAnimated(true, null)
                onResult(null)
            }
        }
    }

    // Document picker delegate
    val docDelegate = remember {
        object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(
                controller: UIDocumentPickerViewController,
                didPickDocumentsAtURLs: List<*>
            ) {
                val resultList = mutableListOf<SharedImage>()
                for (urlAny in didPickDocumentsAtURLs) {
                    val url = urlAny as? NSURL ?: continue
                    val data = NSData.dataWithContentsOfURL(url) ?: continue
                    val bytes = nsDataToByteArray(data)
                    val name = url.lastPathComponent
                    resultList += SharedImage(bytes, null, name)
                }
                onResult(if (resultList.isNotEmpty()) resultList else null)
            }
        }
    }

    return remember {
        GalleryManager {
            when (type) {
                PickerType.IMAGE -> {
                    // Use PHPickerViewController for images (supports multiple selection)
                    val config = PHPickerConfiguration().apply {
                        setSelectionLimit(if (isSingleSelection) 1 else 0) // 0 = unlimited
                        setFilter(PHPickerFilter.imagesFilter)
                        setSelection(PHPickerConfigurationSelectionOrdered)
                    }
                    val phPicker = PHPickerViewController(configuration = config)
                    phPicker.delegate = phPickerDelegate
                    val vc = app.keyWindow?.rootViewController
                    vc?.presentViewController(phPicker, true, null)
                }

                PickerType.VIDEO -> {
                    if (isSingleSelection) {
                        // Use UIImagePickerController for single video selection
                        imagePicker.sourceType =
                            UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
                        imagePicker.mediaTypes = listOf("public.movie")
                        imagePicker.allowsEditing = true
                        imagePicker.delegate = galleryDelegate
                        val vc = app.keyWindow?.rootViewController
                        vc?.presentViewController(imagePicker, true, null)
                    } else {
                        // Use PHPickerViewController for multiple video selection
                        val config = PHPickerConfiguration().apply {
                            setSelectionLimit(0) // unlimited
                            setFilter(PHPickerFilter.videosFilter)
                            setSelection(PHPickerConfigurationSelectionOrdered)
                        }
                        val phPicker = PHPickerViewController(configuration = config)
                        phPicker.delegate = phPickerDelegate
                        val vc = app.keyWindow?.rootViewController
                        vc?.presentViewController(phPicker, true, null)
                    }
                }

                PickerType.IMAGE_AND_VIDEO -> {
                    // Use PHPickerViewController for both images and videos
                    val config = PHPickerConfiguration().apply {
                        setSelectionLimit(if (isSingleSelection) 1 else 0)
                        setFilter(
                            PHPickerFilter.anyFilterMatchingSubfilters(
                                listOf(PHPickerFilter.imagesFilter, PHPickerFilter.videosFilter)
                            )
                        )
                        setSelection(PHPickerConfigurationSelectionOrdered)
                    }
                    val phPicker = PHPickerViewController(configuration = config)
                    phPicker.delegate = phPickerDelegate
                    val vc = app.keyWindow?.rootViewController
                    vc?.presentViewController(phPicker, true, null)
                }

                PickerType.DOCUMENT -> {
                    val docPicker = UIDocumentPickerViewController(
                        forOpeningContentTypes = listOf(UTTypeItem),
                        asCopy = true
                    )
                    docPicker.delegate = docDelegate
                    docPicker.allowsMultipleSelection = !isSingleSelection
                    docPicker.modalPresentationStyle = UIModalPresentationFormSheet
                    val vc = app.keyWindow?.rootViewController
                    vc?.presentViewController(docPicker, true, null)
                }
            }
        }
    }
}

actual class GalleryManager actual constructor(private val onLaunch: () -> Unit) {
    actual fun launch() {
        onLaunch()
    }
}

// helper: convert NSData -> ByteArray
@OptIn(ExperimentalForeignApi::class)
internal fun nsDataToByteArray(data: NSData): ByteArray {
    val length = data.length.toInt()
    if (length == 0) return ByteArray(0)

    return ByteArray(length).apply {
        usePinned { pinned ->
            memcpy(pinned.addressOf(0), data.bytes, length.toULong())
        }
    }
}

// actual SharedImage for iOS - ONLY DEFINE THIS ONCE IN YOUR PROJECT
actual class SharedImage(
    private val bytes: ByteArray?,
    actual val mimeType: String?,
    actual val name: String?
) {
    actual fun toByteArray(): ByteArray? = bytes

    actual fun toImageBitmap(): ImageBitmap? {
        if (bytes == null) return null
        if (mimeType?.startsWith("image/") != true) {
            // try to decode anyway (if it's actually an image but missing mime)
            return try {
                Image.makeFromEncoded(bytes).toComposeImageBitmap()
            } catch (_: Throwable) {
                null
            }
        }
        return try {
            Image.makeFromEncoded(bytes).toComposeImageBitmap()
        } catch (_: Throwable) {
            null
        }
    }
}