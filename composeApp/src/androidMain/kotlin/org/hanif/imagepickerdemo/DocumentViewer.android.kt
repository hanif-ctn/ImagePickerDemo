// shared/androidMain/DocumentViewer.android.kt
package org.hanif.imagepickerdemo

import PdfViewer1
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun DocumentViewer(document: SharedDocument, modifier: Modifier, onBack: () -> Unit) {
    var isControlsVisible by remember { mutableStateOf(true) }
    var topAppBarHeight by remember { mutableStateOf(0.dp) }
    val contentPadding by animateDpAsState(
        targetValue = topAppBarHeight,
        label = "contentPaddingAnimation"
    )
    val localDensity = LocalDensity.current

    // Auto-hide controls after 3 seconds
    LaunchedEffect(isControlsVisible) {
        if (isControlsVisible) {
            delay(3000)
            isControlsVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black)
            .pointerInput(Unit) {
                detectTapGestures {
                    isControlsVisible = !isControlsVisible
                }
            }
    ) {
        when {
            document.mimeType?.contains("pdf") == true ->
//                PdfViewer(
//                    document,
//                    Modifier.fillMaxSize().run {
//                        if (isControlsVisible) Modifier.padding(top = contentPadding) else Modifier
//                    }
//                )
                PdfViewer1(document, contentPaddingValues = if (isControlsVisible) PaddingValues(top = topAppBarHeight) else PaddingValues(), modifier = modifier)

            document.mimeType?.startsWith("image/") == true ->
                ImageViewer(document, Modifier.fillMaxSize())

            document.mimeType?.startsWith("video/") == true ->
                InAppVideoPlayer(
                    SharedVideo(document.name, document.mimeType, document.data),
                    Modifier.fillMaxSize()
                )

            document.mimeType?.startsWith("text/") == true ||
                    document.mimeType in listOf("application/json", "application/xml") ->
                TextViewer(document, Modifier.fillMaxSize())

            else -> UnsupportedFileViewer(document, Modifier.fillMaxSize())
        }

        // Top bar with document info
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            TopAppBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { layoutCoordinates ->
                        with(localDensity) {
                            topAppBarHeight = layoutCoordinates.size.height.toDp()
                        }
                    },
                title = {
                    Text(
                        text = document.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f)
                ),
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Handle share */ }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share",
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun PdfViewer(document: SharedDocument, modifier: Modifier) {
    val context = LocalContext.current
    val file = remember(document) {
        File(context.cacheDir, "${document.name}.pdf").apply {
            writeBytes(document.data ?: byteArrayOf())
        }
    }

    var renderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var currentPage by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()

    // Global zoom state (applies to all pages)
    var globalScale by remember { mutableFloatStateOf(1f) }
    var globalOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            try {
                renderer = PdfRenderer(
                    ParcelFileDescriptor.open(
                        file,
                        ParcelFileDescriptor.MODE_READ_ONLY
                    )
                )
                isLoading = false
            } catch (_: Exception) {
                isLoading = false
            }
        }
    }

    // Reset offset when scale becomes 1
    LaunchedEffect(globalScale) {
        if (globalScale == 1f) {
            globalOffset = Offset.Zero
        }
    }

    Box(modifier = modifier) {
        if (isLoading) {
            LoadingIndicator()
        } else {
            renderer?.let { pdfRenderer ->
                // Modifier that handles global pinch & double-tap zooming with horizontal panning
                val zoomContainerModifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        // double-tap to toggle between 1x and 2x
                        detectTapGestures(
                            onDoubleTap = {
                                globalScale = if (globalScale > 1f) 1f else 2f
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        // pinch-to-zoom and pan handling
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (globalScale * zoom).coerceIn(1f, 5f)

                            // Handle panning - only allow horizontal panning when zoomed
                            if (newScale > 1f) {
                                // Calculate max horizontal offset based on scale
                                val maxHorizontalOffset = (size.width * (newScale - 1f)) / 2f

                                val newOffsetX = (globalOffset.x + pan.x).coerceIn(
                                    -maxHorizontalOffset,
                                    maxHorizontalOffset
                                )

                                globalOffset = Offset(newOffsetX, 0f)
                            } else {
                                globalOffset = Offset.Zero
                            }

                            globalScale = newScale
                        }
                    }
                    .graphicsLayer(
                        scaleX = globalScale,
                        scaleY = globalScale,
                        translationX = globalOffset.x,
                        translationY = globalOffset.y,
                        transformOrigin = TransformOrigin.Center
                    )

                Box(modifier = zoomContainerModifier) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = (16.dp * globalScale).coerceAtLeast(16.dp),
                            bottom = (16.dp * globalScale).coerceAtLeast(16.dp),
                            start = 16.dp,
                            end = 16.dp
                        ),
                        //verticalArrangement = Arrangement.spacedBy((16.dp * globalScale).coerceAtLeast(16.dp))
                    ) {
                        items((0 until pdfRenderer.pageCount).toList()) { index ->
                            PdfPageItem(pdfRenderer, index)
                        }
                    }
                }

                // Page indicator (still shown over scaled content)
                PageIndicator(
                    currentPage = currentPage + 1,
                    totalPages = pdfRenderer.pageCount,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            } ?: ErrorMessage("Failed to load PDF")
        }
    }
}

@Composable
private fun PdfPageItem(renderer: PdfRenderer, pageIndex: Int) {
    val pageBitmap = remember(pageIndex) {
        try {
            renderer.openPage(pageIndex).use { page ->
                val bitmap = createBitmap(page.width * 2, page.height * 2) // High resolution
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }
        } catch (_: Exception) {
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
    ) {
        pageBitmap?.let { bitmap ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1}",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
            }
        } ?: Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Page ${pageIndex + 1} - Failed to load")
        }
    }
}

@Composable
private fun ImageViewer(document: SharedDocument, modifier: Modifier) {
    val bitmap = remember(document) {
        document.data?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = modifier) {
        bitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = document.name,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 5f)
                            offset = if (scale > 1f) {
                                offset + pan
                            } else {
                                Offset.Zero
                            }
                        }
                    },
                contentScale = ContentScale.Fit
            )
        } ?: ErrorMessage("Invalid image format")
    }
}

@Composable
private fun TextViewer(document: SharedDocument, modifier: Modifier) {
    val text = remember(document) { document.data?.decodeToString() ?: "" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(20.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = text,
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 24.sp,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UnsupportedFileViewer(document: SharedDocument, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Unsupported File Format",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            document.mimeType ?: "Unknown",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { /* Handle with external app */ },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open with external app")
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Loading document...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PageIndicator(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(16.dp)
            .clip(CircleShape),
        color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f)
    ) {
        Text(
            text = "$currentPage / $totalPages",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = androidx.compose.ui.graphics.Color.White,
            style = MaterialTheme.typography.bodySmall
        )
    }
}