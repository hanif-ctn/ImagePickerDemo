// androidMain
package org.hanif.imagepickerdemo

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

@Composable
actual fun rememberGalleryManager(
    type: PickerType,
    isSingleSelection: Boolean,
    onResult: (List<SharedImage>?) -> Unit
): GalleryManager {
    val context = LocalContext.current
    val contentResolver: ContentResolver = context.contentResolver

    val singlePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri == null) {
                onResult(null)
                return@rememberLauncherForActivityResult
            }
            val item = uriToSharedImage(contentResolver, uri)
            onResult(listOfNotNull(item))
        }

    val multiPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
            val items = uris.mapNotNull { uri -> uriToSharedImage(contentResolver, uri) }
            onResult(items)
        }

    val documentPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            val items = uris.mapNotNull { uri -> uriToSharedImage(contentResolver, uri) }
            onResult(items)
        }

    val mediaType = when (type) {
        PickerType.IMAGE -> ActivityResultContracts.PickVisualMedia.ImageOnly
        PickerType.VIDEO -> ActivityResultContracts.PickVisualMedia.VideoOnly
        PickerType.IMAGE_AND_VIDEO -> ActivityResultContracts.PickVisualMedia.ImageAndVideo
        else -> null
    }

    return remember {
        GalleryManager(onLaunch = {
            when (type) {
                PickerType.DOCUMENT -> documentPickerLauncher.launch(arrayOf("*/*"))
                PickerType.IMAGE, PickerType.VIDEO, PickerType.IMAGE_AND_VIDEO -> {
                    if (isSingleSelection) {
                        singlePickerLauncher.launch(PickVisualMediaRequest(mediaType = mediaType!!))
                    } else {
                        multiPickerLauncher.launch(PickVisualMediaRequest(mediaType = mediaType!!))
                    }
                }
            }
        })
    }
}

actual class GalleryManager actual constructor(private val onLaunch: () -> Unit) {
    actual fun launch() {
        onLaunch()
    }
}

internal fun uriToSharedImage(contentResolver: ContentResolver, uri: Uri): SharedImage? {
    return try {
        contentResolver.openInputStream(uri)?.use { input ->
            val bytes = input.readBytes()
            val mime = contentResolver.getType(uri)
            val name = queryDisplayName(contentResolver, uri)
            SharedImage(bytes, mime, name)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun queryDisplayName(contentResolver: ContentResolver, uri: Uri): String? {
    var name: String? = null
    val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
    var cursor: Cursor? = null
    try {
        cursor = contentResolver.query(uri, projection, null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) name = cursor.getString(idx)
        }
    } finally {
        cursor?.close()
    }
    return name
}

actual class SharedImage(
    private val bytes: ByteArray?,
    actual val mimeType: String?,
    actual val name: String?
) {
    actual fun toByteArray(): ByteArray? = bytes

    actual fun toImageBitmap(): ImageBitmap? {
        if (bytes == null) return null
        // only decode for image MIME types
        if (mimeType?.startsWith("image/") != true) return null
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        return bmp.asImageBitmap()
    }
}
