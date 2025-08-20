package org.hanif.imagepickerdemo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun DocumentViewer(
    document: SharedDocument,
    modifier: Modifier = Modifier,
    onBack : () -> Unit
)

@Composable
fun DocumentScreen(document: SharedDocument, onBack : () -> Unit) {
    DocumentViewer(document = document, modifier = Modifier.fillMaxSize(), onBack =onBack)
}