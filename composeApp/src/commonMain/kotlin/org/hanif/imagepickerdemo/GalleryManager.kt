package org.hanif.imagepickerdemo

import androidx.compose.runtime.Composable

@Composable
expect fun rememberGalleryManager(
    type: PickerType,
    isSingleSelection: Boolean = false,
    onResult: (List<SharedImage>?) -> Unit
): GalleryManager


expect class GalleryManager(
    onLaunch: () -> Unit
) {
    fun launch()
}

enum class PickerType {
    IMAGE,
    VIDEO,
    IMAGE_AND_VIDEO,
    DOCUMENT
}