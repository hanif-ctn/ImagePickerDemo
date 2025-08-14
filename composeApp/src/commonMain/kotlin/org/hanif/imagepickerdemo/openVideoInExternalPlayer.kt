package org.hanif.imagepickerdemo

expect suspend fun openVideoInExternalPlayer(video: SharedVideo): Boolean

expect suspend fun openDocumentInExternalViewer(document: SharedDocument): Boolean
