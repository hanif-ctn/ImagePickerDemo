package org.hanif.imagepickerdemo

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun InAppVideoPlayer(
    video: SharedVideo,
    modifier: Modifier = Modifier
)