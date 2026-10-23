package com.example.data.storage

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.example.data.model.DocumentAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max

object AttachmentStorageManager {

    private const val ATTACHMENTS_SUBDIR = "documents/attachments"
    private const val CAMERA_CACHE_SUBDIR = "camera"
    private const val MAX_IMAGE_DIMENSION = 1920 // Optimized for low-end devices and clear document legibility

    fun getAttachmentsDir(context: Context): File {
        val dir = File(context.filesDir, ATTACHMENTS_SUBDIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getCameraCacheDir(context: Context): File {
        val dir = File(context.cacheDir, CAMERA_CACHE_SUBDIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun createTempCameraImageUri(context: Context): Pair<Uri, File> {
        val cacheDir = getCameraCacheDir(context)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val tempFile = File(cacheDir, "SCAN_TEMP_${timestamp}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        return Pair(uri, tempFile)
    }

    suspend fun processAndSaveCameraScan(
        context: Context,
        tempFile: File,
        docName: String? = null
    ): DocumentAttachment? = withContext(Dispatchers.IO) {
        if (!tempFile.exists() || tempFile.length() == 0L) {
            return@withContext null
        }

        try {
            val dateStr = SimpleDateFormat("ddMMM_HHmm", Locale.getDefault()).format(Date())
            val cleanDocName = docName?.trim()?.take(20)?.replace(Regex("[^a-zA-Z0-9_]"), "_") ?: "Scan"
            val displayFileName = "${cleanDocName}_$dateStr.jpg"

            val targetFile = File(getAttachmentsDir(context), "scan_${UUID.randomUUID()}.jpg")

            // Correct EXIF orientation and downsample if oversized to prevent memory pressure
            val orientation = try {
                val exif = ExifInterface(tempFile.absolutePath)
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } catch (e: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(tempFile.absolutePath, options)

            var sampleSize = 1
            val maxDim = max(options.outWidth, options.outHeight)
            if (maxDim > MAX_IMAGE_DIMENSION) {
                sampleSize = (maxDim.toFloat() / MAX_IMAGE_DIMENSION).toInt().coerceAtLeast(1)
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // Low-memory footprint for fast rendering
            }

            var bitmap = BitmapFactory.decodeFile(tempFile.absolutePath, decodeOptions)
            if (bitmap != null) {
                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                }

                if (!matrix.isIdentity) {
                    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    if (rotated != bitmap) {
                        bitmap.recycle()
                        bitmap = rotated
                    }
                }

                FileOutputStream(targetFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                bitmap.recycle()
            } else {
                // Fallback direct copy
                tempFile.copyTo(targetFile, overwrite = true)
            }

            // Cleanup temp file
            tempFile.delete()

            DocumentAttachment(
                id = UUID.randomUUID().toString(),
                fileName = displayFileName,
                filePath = targetFile.absolutePath,
                mimeType = "image/jpeg",
                fileSizeBytes = targetFile.length(),
                addedAtMillis = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveContentUriToAttachments(
        context: Context,
        uri: Uri
    ): DocumentAttachment? = withContext(Dispatchers.IO) {
        try {
            var fileName = "Document_${System.currentTimeMillis()}"
            var fileSizeBytes = 0L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                    if (sizeIndex != -1) {
                        fileSizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            }

            var mimeType = context.contentResolver.getType(uri)
            if (mimeType.isNullOrBlank()) {
                val extension = MimeTypeMap.getFileExtensionFromUrl(fileName)
                if (!extension.isNullOrBlank()) {
                    mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
                }
            }
            if (mimeType.isNullOrBlank()) {
                mimeType = if (fileName.endsWith(".pdf", ignoreCase = true)) "application/pdf"
                else if (fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".jpeg", ignoreCase = true)) "image/jpeg"
                else if (fileName.endsWith(".png", ignoreCase = true)) "image/png"
                else "application/octet-stream"
            }

            val extension = if (fileName.contains(".")) {
                "." + fileName.substringAfterLast(".")
            } else {
                if (mimeType == "application/pdf") ".pdf" else ".bin"
            }

            val targetFile = File(getAttachmentsDir(context), "att_${UUID.randomUUID()}$extension")

            context.contentResolver.openInputStream(uri)?.use { input: InputStream ->
                FileOutputStream(targetFile).use { output: FileOutputStream ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            if (fileSizeBytes <= 0L) {
                fileSizeBytes = targetFile.length()
            }

            DocumentAttachment(
                id = UUID.randomUUID().toString(),
                fileName = fileName,
                filePath = targetFile.absolutePath,
                mimeType = mimeType,
                fileSizeBytes = fileSizeBytes,
                addedAtMillis = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteAttachmentFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    fun openAttachmentInSystemViewer(context: Context, attachment: DocumentAttachment): Result<Unit> {
        return try {
            val file = File(attachment.filePath)
            if (!file.exists()) {
                return Result.failure(Exception("File not found on device: ${attachment.fileName}"))
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, attachment.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: ActivityNotFoundException) {
            Result.failure(Exception("No application found to view this ${attachment.mimeType.substringAfter("/")} file. Please install a file viewer."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.getDefault(), "%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    fun isImage(attachment: DocumentAttachment): Boolean {
        return attachment.mimeType.startsWith("image/", ignoreCase = true) ||
                attachment.fileName.endsWith(".jpg", ignoreCase = true) ||
                attachment.fileName.endsWith(".jpeg", ignoreCase = true) ||
                attachment.fileName.endsWith(".png", ignoreCase = true) ||
                attachment.fileName.endsWith(".webp", ignoreCase = true)
    }

    fun isPdf(attachment: DocumentAttachment): Boolean {
        return attachment.mimeType.equals("application/pdf", ignoreCase = true) ||
                attachment.fileName.endsWith(".pdf", ignoreCase = true)
    }
}
