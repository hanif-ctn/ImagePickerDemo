package org.hanif.imagepickerdemo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import imagepickerdemo.composeapp.generated.resources.Res
import imagepickerdemo.composeapp.generated.resources.ic_camera
import imagepickerdemo.composeapp.generated.resources.ic_images
import imagepickerdemo.composeapp.generated.resources.ic_person_circle
import imagepickerdemo.composeapp.generated.resources.video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

@OptIn(ExperimentalResourceApi::class)
@Composable
fun App() {
    MaterialTheme {
        Scaffold {
            val coroutineScope = rememberCoroutineScope()
            var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
            var imageList = remember { mutableStateListOf<ImageBitmap?>() }
            var videoList = remember { mutableStateListOf<SharedVideo>() }
            var imageSourceOptionDialog by remember { mutableStateOf(value = false) }
            var launchCamera by remember { mutableStateOf(value = false) }
            var launchGallery by remember { mutableStateOf(value = false) }
            var launchVideoGallery by remember { mutableStateOf(value = false) }
            var launchSetting by remember { mutableStateOf(value = false) }
            var permissionRationalDialog by remember { mutableStateOf(value = false) }

            val permissionsManager = createPermissionsManager(object : PermissionCallback {
                override fun onPermissionStatus(
                    permissionType: PermissionType,
                    status: PermissionStatus
                ) {
                    when (status) {
                        PermissionStatus.GRANTED -> {
                            when (permissionType) {
                                PermissionType.CAMERA -> launchCamera = true
                                PermissionType.GALLERY -> {
                                    launchGallery = true
                                    launchVideoGallery = false
                                }
                            }
                        }

                        else -> {
                            permissionRationalDialog = true
                        }
                    }
                }
            })

            val cameraManager = rememberCameraManager {
                coroutineScope.launch {
                    val bitmap = withContext(Dispatchers.Default) {
                        it?.toImageBitmap()
                    }
                    imageBitmap = bitmap
                }
            }

            // Image gallery manager
            val imageGalleryManager = rememberGalleryManager(
                isSingleSelection = false,
                type = PickerType.IMAGE
            ) { sharedImages ->
                coroutineScope.launch {
                    withContext(Dispatchers.Default) {
                        sharedImages?.forEach { img ->
                            img.toImageBitmap()?.let { bitmap ->
                                imageList.add(bitmap)
                            }
                        }
                    }
                }
            }

            // Video gallery manager
            val videoGalleryManager = rememberGalleryManager(
                isSingleSelection = false,
                type = PickerType.VIDEO
            ) { sharedVideos ->
                coroutineScope.launch {
                    withContext(Dispatchers.Default) {
                        sharedVideos?.forEach { video ->
                            if (video.mimeType?.startsWith("video/") == true) {
                                videoList.add(
                                    SharedVideo(
                                        name = video.name ?: "Unknown Video",
                                        mimeType = video.mimeType!!,
                                        data = video.toByteArray()
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Mixed media gallery manager
            val mixedGalleryManager = rememberGalleryManager(
                isSingleSelection = false,
                type = PickerType.IMAGE_AND_VIDEO
            ) { sharedMedia ->
                coroutineScope.launch {
                    withContext(Dispatchers.Default) {
                        sharedMedia?.forEach { media ->
                            when {
                                media.mimeType?.startsWith("image/") == true -> {
                                    media.toImageBitmap()?.let { bitmap ->
                                        imageList.add(bitmap)
                                    }
                                }

                                media.mimeType?.startsWith("video/") == true -> {
                                    videoList.add(
                                        SharedVideo(
                                            name = media.name ?: "Unknown Video",
                                            mimeType = media.mimeType!!,
                                            data = media.toByteArray()
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Handle dialog and permission flows
            if (imageSourceOptionDialog) {
                MediaSourceOptionDialog(
                    onDismissRequest = {
                        imageSourceOptionDialog = false
                    },
                    onImageGalleryRequest = {
                        imageSourceOptionDialog = false
                        launchGallery = true
                        launchVideoGallery = false
                    },
                    onVideoGalleryRequest = {
                        imageSourceOptionDialog = false
                        launchVideoGallery = true
                        launchGallery = false
                    },
                    onMixedGalleryRequest = {
                        imageSourceOptionDialog = false
                        launchGallery = true
                        launchVideoGallery = true
                    },
                    onCameraRequest = {
                        imageSourceOptionDialog = false
                        launchCamera = true
                    }
                )
            }

            if (launchGallery && launchVideoGallery) {
                // Launch mixed media picker
                if (permissionsManager.isPermissionGranted(PermissionType.GALLERY)) {
                    mixedGalleryManager.launch()
                } else {
                    permissionsManager.askPermission(PermissionType.GALLERY)
                }
                launchGallery = false
                launchVideoGallery = false
            } else if (launchGallery) {
                // Launch image only picker
                if (permissionsManager.isPermissionGranted(PermissionType.GALLERY)) {
                    imageGalleryManager.launch()
                } else {
                    permissionsManager.askPermission(PermissionType.GALLERY)
                }
                launchGallery = false
            } else if (launchVideoGallery) {
                // Launch video only picker
                if (permissionsManager.isPermissionGranted(PermissionType.GALLERY)) {
                    videoGalleryManager.launch()
                } else {
                    permissionsManager.askPermission(PermissionType.GALLERY)
                }
                launchVideoGallery = false
            }

            if (launchCamera) {
                if (permissionsManager.isPermissionGranted(PermissionType.CAMERA)) {
                    cameraManager.launch()
                } else {
                    permissionsManager.askPermission(PermissionType.CAMERA)
                }
                launchCamera = false
            }

            if (launchSetting) {
                permissionsManager.launchSettings()
                launchSetting = false
            }

            if (permissionRationalDialog) {
                AlertMessageDialog(
                    title = "Permission Required",
                    message = "To access media files, please grant this permission. You can manage permissions in your device settings.",
                    positiveButtonText = "Settings",
                    negativeButtonText = "Cancel",
                    onPositiveClick = {
                        permissionRationalDialog = false
                        launchSetting = true
                    },
                    onNegativeClick = {
                        permissionRationalDialog = false
                    }
                )
            }

            Box(
                modifier = Modifier.fillMaxSize().padding(it).background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                if (imageBitmap != null || imageList.isNotEmpty() || videoList.isNotEmpty()) {
                    // Modern Media Display Screen
                    ModernMediaDisplayScreen(
                        imageList = imageList,
                        videoList = videoList,
                        singleImage = imageBitmap,
                        onVideoClick = { video ->
                            coroutineScope.launch {
                                openVideoInExternalPlayer(video)
                            }
                        },
                        onImageClick = {
                            imageSourceOptionDialog = true
                        },
                        onBackToSelection = {
                            // Clear all media and return to selection screen
                            imageBitmap = null
                            imageList.clear()
                            videoList.clear()
                        }
                    )
                } else {
                    // Modern Selection Screen
                    ModernSelectionScreen(
                        onImageGalleryClick = {
                            launchGallery = true
                            launchVideoGallery = false
                        },
                        onVideoGalleryClick = {
                            launchVideoGallery = true
                            launchGallery = false
                        },
                        onCameraClick = {
                            launchCamera = true
                        },
                        onMixedGalleryClick = {
                            imageSourceOptionDialog = true
                        }
                    )
                }
            }
        }
    }
}

// Data class to hold video information
data class SharedVideo(
    val name: String,
    val mimeType: String,
    val data: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as SharedVideo
        if (name != other.name) return false
        if (mimeType != other.mimeType) return false
        if (data != null) {
            if (other.data == null) return false
            if (!data.contentEquals(other.data)) return false
        } else if (other.data != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + (data?.contentHashCode() ?: 0)
        return result
    }
}

// Composable to display video information with modern UI
@Composable
fun VideoCard(
    video: SharedVideo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Video icon with modern styling
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_camera), // Replace with video icon when available
                    contentDescription = "Video",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Video details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Mime type with modern chip-like appearance
                Text(
                    text = video.mimeType.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // File size
                video.data?.let { data ->
                    Text(
                        text = formatFileSize(data.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Play indicator
            Icon(
                painter = painterResource(Res.drawable.ic_camera), // Use a play icon if available
                contentDescription = "Play video",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        }
    }
}

// Helper function to format file size - KMP compatible
fun formatFileSize(bytes: Int): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> {
            val kb = bytes / 1024.0
            "${(kb * 10).roundToInt() / 10.0} KB"
        }

        bytes < 1024 * 1024 * 1024 -> {
            val mb = bytes / (1024.0 * 1024.0)
            "${(mb * 10).roundToInt() / 10.0} MB"
        }

        else -> {
            val gb = bytes / (1024.0 * 1024.0 * 1024.0)
            "${(gb * 10).roundToInt() / 10.0} GB"
        }
    }
}

// Enhanced dialog for media source selection with modern UI
@Composable
fun MediaSourceOptionDialog(
    onDismissRequest: () -> Unit,
    onImageGalleryRequest: () -> Unit,
    onVideoGalleryRequest: () -> Unit,
    onMixedGalleryRequest: () -> Unit,
    onCameraRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                "Select Media Source",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Images Only Button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onImageGalleryRequest() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_images),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Images Only",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Videos Only Button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onVideoGalleryRequest() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_camera), // Use video icon when available
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Videos Only",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // Images & Videos Button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMixedGalleryRequest() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_images),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Images & Videos",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                // Camera Button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCameraRequest() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_camera),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Camera",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

// Modern Media Display Screen with contemporary design
@Composable
fun ModernMediaDisplayScreen(
    imageList: List<ImageBitmap?>,
    videoList: List<SharedVideo>,
    singleImage: ImageBitmap?,
    onVideoClick: (SharedVideo) -> Unit,
    onImageClick: () -> Unit,
    onBackToSelection: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Modern Header with Back Button
            ModernMediaHeader(
                totalImages = imageList.count { it != null } + if (singleImage != null) 1 else 0,
                totalVideos = videoList.size,
                onBackClick = onBackToSelection
            )

            // Media Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    top = 8.dp,
                    bottom = 24.dp
                )
            ) {
                // Images Section
                val allImages = buildList {
                    singleImage?.let { add(it) }
                    addAll(imageList.filterNotNull())
                }

                if (allImages.isNotEmpty()) {
                    item {
                        MediaSectionHeader(
                            title = "Images",
                            count = allImages.size,
                            icon = Res.drawable.ic_images
                        )
                    }

                    // Images Grid
                    items(allImages.chunked(2)) { rowImages ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowImages.forEach { bitmap ->
                                ModernImageCard(
                                    bitmap = bitmap,
                                    onClick = onImageClick,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Fill remaining space if odd number
                            if (rowImages.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Videos Section
                if (videoList.isNotEmpty()) {
                    item {
                        MediaSectionHeader(
                            title = "Videos",
                            count = videoList.size,
                            icon = Res.drawable.ic_camera // Replace with video icon when available
                        )
                    }

                    items(videoList) { video ->
                        ModernVideoCard(
                            video = video,
                            onClick = { onVideoClick(video) }
                        )
                    }
                }

                // Empty state shouldn't happen, but just in case
                if (allImages.isEmpty() && videoList.isEmpty()) {
                    item {
                        EmptyMediaState(onBackToSelection = onBackToSelection)
                    }
                }
            }
        }
    }
}

// Modern Header Component
@Composable
fun ModernMediaHeader(
    totalImages: Int,
    totalVideos: Int,
    onBackClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_person_circle), // Use back arrow icon when available
                    contentDescription = "Back to selection",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Content Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Selected Media",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "${totalImages + totalVideos} items • ${totalImages} images • ${totalVideos} videos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }

            // Action Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_images), // Use add/plus icon when available
                    contentDescription = "Add more media",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Section Header Component
@Composable
fun MediaSectionHeader(
    title: String,
    count: Int,
    icon: org.jetbrains.compose.resources.DrawableResource
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "($count)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

// Modern Image Card Component
@Composable
fun ModernImageCard(
    bitmap: ImageBitmap,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = "Selected Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Overlay with subtle gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.1f)
                            )
                        )
                    )
            )

            // Edit indicator
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.9f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_person_circle), // Use edit icon when available
                    contentDescription = "Edit",
                    modifier = Modifier.size(12.dp),
                    tint = Color.Black.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// Enhanced Video Card for display screen
@Composable
fun ModernVideoCard(
    video: SharedVideo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Video thumbnail placeholder with play button
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_camera), // Use play icon when available
                    contentDescription = "Play video",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Video details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mime type chip
                    Text(
                        text = video.mimeType.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )

                    // File size
                    video.data?.let { data ->
                        Text(
                            text = formatFileSize(data.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Play indicator with animation hint
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_camera), // Use play arrow icon when available
                    contentDescription = "Play video",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Empty State Component
@Composable
fun EmptyMediaState(
    onBackToSelection: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_images),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No media selected",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = "Go back to select some media",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onBackToSelection) {
            Text("Back to Selection")
        }
    }
}

// Modern Selection Screen with contemporary design
@Composable
fun ModernSelectionScreen(
    onImageGalleryClick: () -> Unit,
    onVideoGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    onMixedGalleryClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Media Picker",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Choose your media source",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            // Main Action Cards
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Row for Image and Video Gallery
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Image Gallery Card
                    ModernActionCard(
                        title = "Images",
                        subtitle = "Browse photos",
                        icon = Res.drawable.ic_images,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        onContainerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = onImageGalleryClick,
                        modifier = Modifier.weight(1f)
                    )

                    // Video Gallery Card
                    ModernActionCard(
                        title = "Videos",
                        subtitle = "Browse videos",
                        icon = Res.drawable.video, // Replace with video icon when available
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        onContainerColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = onVideoGalleryClick,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Camera Card (Full width)
                ModernActionCard(
                    title = "Camera",
                    subtitle = "Take a new photo",
                    icon = Res.drawable.ic_camera,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    onContainerColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    onClick = onCameraClick,
                    modifier = Modifier.fillMaxWidth(),
                    isLarge = true
                )

                // Mixed Gallery Card
                ModernActionCard(
                    title = "More Options",
                    subtitle = "Images & Videos together",
                    icon = Res.drawable.ic_person_circle,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    onContainerColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onMixedGalleryClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Footer Section
            Text(
                text = "Tap to select your preferred option",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

// Modern Action Card Component
@Composable
fun ModernActionCard(
    title: String,
    subtitle: String,
    icon: org.jetbrains.compose.resources.DrawableResource,
    containerColor: Color,
    onContainerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLarge: Boolean = false
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .then(
                if (isLarge) Modifier.height(120.dp) else Modifier.height(140.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        if (isLarge) {
            // Horizontal layout for large cards
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon Container
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = onContainerColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = title,
                        modifier = Modifier.size(32.dp),
                        tint = onContainerColor
                    )
                }

                // Text Content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = onContainerColor
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = onContainerColor.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            // Vertical layout for regular cards
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Icon Container
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = onContainerColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = title,
                        modifier = Modifier.size(28.dp),
                        tint = onContainerColor
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Text Content
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = onContainerColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainerColor.copy(alpha = 0.8f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}