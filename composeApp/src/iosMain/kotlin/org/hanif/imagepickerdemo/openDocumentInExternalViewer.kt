// iosMain
package org.hanif.imagepickerdemo

import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.*
import platform.UIKit.*
import platform.QuickLook.*
import platform.darwin.NSInteger
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
actual suspend fun openDocumentInExternalViewer(document: SharedDocument): Boolean {
    return withContext(Dispatchers.Main) {
        try {
            println("=== iOS Document Viewer Debug ===")
            val documentData = document.data

            if (documentData == null) {
                println("ERROR: Document data is null")
                return@withContext false
            }

            println("Document name: '${document.name}'")
            println("Document MIME type: '${document.mimeType}'")
            println("Document size: ${documentData.size} bytes")

            // Create NSData
            val nsData = documentData.usePinned { pinned ->
                NSData.dataWithBytes(pinned.addressOf(0), documentData.size.toULong())
            }
            println("NSData created successfully")

            // Create file URL with proper extension
            val tempDirectory = NSFileManager.defaultManager.temporaryDirectory
            val fileName = getFileNameWithExtension(document.name, document.mimeType)
            val sanitizedFileName = fileName.replace(Regex("[/\\\\:*?\"<>|]"), "_")

            val tempFileURL = tempDirectory.URLByAppendingPathComponent(sanitizedFileName)
            if (tempFileURL == null) {
                println("ERROR: Could not create temp file URL")
                return@withContext false
            }

            println("Temp file URL: ${tempFileURL.absoluteString}")

            // Write file
            memScoped {
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                val writeSuccess = nsData.writeToURL(
                    url = tempFileURL,
                    options = NSDataWritingAtomic,
                    error = errorPtr.ptr
                )

                if (!writeSuccess) {
                    val error = errorPtr.value
                    println("ERROR: Failed to write file: ${error?.localizedDescription}")
                    return@withContext false
                }
            }

            // Verify file exists
            val fileExists = NSFileManager.defaultManager.fileExistsAtPath(tempFileURL.path!!)
            println("File exists after write: $fileExists")

            // Get root view controller
            val app = UIApplication.sharedApplication
            val keyWindow = app.keyWindow
            val rootViewController = keyWindow?.rootViewController

            if (rootViewController == null) {
                println("ERROR: Could not get root view controller")
                return@withContext false
            }

            // Create custom preview item wrapper
            val previewItem = DocumentPreviewItem(tempFileURL, document.name)

            // Create and present QuickLook controller
            val quickLookController = QLPreviewController()
            val dataSource = DocumentPreviewDataSource(previewItem)
            quickLookController.dataSource = dataSource
            quickLookController.currentPreviewItemIndex = 0

            println("Presenting QuickLook controller...")
            rootViewController.presentViewController(
                quickLookController,
                animated = true
            ) {
                println("QuickLook controller presented successfully")
            }

            true
        } catch (e: Exception) {
            println("EXCEPTION in iOS document viewer: ${e.message}")
            e.printStackTrace()
            false
        } finally {
            println("=== iOS Document Viewer End ===")
        }
    }
}

// Custom QLPreviewItem implementation
class DocumentPreviewItem(
    private val fileURL: NSURL,
    private val displayName: String?
) : NSObject(), QLPreviewItemProtocol {

    override fun previewItemURL(): NSURL {
        return fileURL
    }

    override fun previewItemTitle(): String? {
        return displayName ?: fileURL.lastPathComponent
    }
}

// Updated QuickLook data source
class DocumentPreviewDataSource(
    private val previewItem: QLPreviewItemProtocol
) : NSObject(), QLPreviewControllerDataSourceProtocol {

    override fun numberOfPreviewItemsInPreviewController(controller: QLPreviewController): NSInteger {
        return 1
    }

    override fun previewController(
        controller: QLPreviewController,
        previewItemAtIndex: NSInteger
    ): QLPreviewItemProtocol {
        return previewItem
    }
}

// Helper function to ensure proper file extension
private fun getFileNameWithExtension(fileName: String, mimeType: String?): String {
    // Check if filename already has an extension
    val hasExtension = fileName.contains(".") && fileName.substringAfterLast(".").length <= 4

    if (hasExtension) {
        return fileName
    }

    // Add extension based on MIME type
    val extension = when (mimeType) {
        "application/pdf" -> ".pdf"
        "application/msword" -> ".doc"
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx"
        "application/vnd.ms-excel" -> ".xls"
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> ".xlsx"
        "application/vnd.ms-powerpoint" -> ".ppt"
        "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> ".pptx"
        "text/plain" -> ".txt"
        "text/html" -> ".html"
        "application/json" -> ".json"
        "application/rtf" -> ".rtf"
        else -> {
            // Try to guess from filename or default
            when {
                fileName.lowercase().contains("doc") -> ".doc"
                fileName.lowercase().contains("pdf") -> ".pdf"
                fileName.lowercase().contains("xls") -> ".xls"
                fileName.lowercase().contains("ppt") -> ".ppt"
                else -> ".txt" // Default fallback
            }
        }
    }

    return "$fileName$extension"
}
