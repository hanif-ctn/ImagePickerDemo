package org.hanif.imagepickerdemo

import android.content.ContentResolver
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

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
//            val media = uri?.let { listOf(SharedImage(it, contentResolver)) }
//            onResult(media)
        }

    val multiPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
         //   val media = uris.map { SharedImage(it, contentResolver) }
           // onResult(media)
        }

    val documentPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            //val files = uris.map { SharedImage(it, contentResolver) }
          //  onResult(files)
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
//                PickerType.ANY_FILE -> documentPickerLauncher.launch(arrayOf("*/*"))
            }
        })
    }
}


actual class GalleryManager actual constructor(private val onLaunch: () -> Unit) {
    actual fun launch() {
        onLaunch()
    }
}

