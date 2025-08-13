package org.hanif.imagepickerdemo
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

expect class PermissionsManager(callback: PermissionCallback) : PermissionHandler

interface PermissionCallback {
    fun onPermissionStatus(permissionType: PermissionType, status: PermissionStatus)
}

@Composable
expect fun createPermissionsManager(callback: PermissionCallback): PermissionsManager

enum class PermissionStatus {
    GRANTED, DENIED, SHOW_RATIONAL
}

enum class PermissionType {
    CAMERA, GALLERY
}


expect class SharedImage {
    fun toByteArray(): ByteArray?
    fun toImageBitmap(): ImageBitmap?
}