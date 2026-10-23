package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.DocumentAttachment
import com.example.data.storage.AttachmentStorageManager
import com.example.ui.scanner.DocumentScannerScreen
import com.example.ui.theme.GlassBorderDark
import com.example.ui.theme.bouncyClickable
import com.example.ui.theme.glassCardClickable
import com.example.ui.theme.GlassSurfaceDark
import com.example.ui.theme.StatusExpired
import com.example.ui.theme.TextMutedDark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AttachmentSection(
    attachments: List<DocumentAttachment>,
    onAttachmentsChange: (List<DocumentAttachment>) -> Unit,
    documentName: String = "",
    isEditable: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isProcessing by remember { mutableStateOf(false) }
    var viewingAttachment by remember { mutableStateOf<DocumentAttachment?>(null) }
    var replacingIndex by remember { mutableIntStateOf(-1) }
    var showScanner by remember { mutableStateOf(false) }

    // Function to trigger camera scan
    val launchCameraScan = { targetIndex: Int ->
        replacingIndex = targetIndex
        showScanner = true
    }

    // Gallery Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            isProcessing = true
            scope.launch {
                val newItems = mutableListOf<DocumentAttachment>()
                for (uri in uris) {
                    val att = AttachmentStorageManager.saveContentUriToAttachments(context, uri)
                    if (att != null) {
                        newItems.add(att)
                    }
                }
                if (replacingIndex in attachments.indices && newItems.isNotEmpty()) {
                    val old = attachments[replacingIndex]
                    AttachmentStorageManager.deleteAttachmentFile(old.filePath)
                    val updated = attachments.toMutableList().apply {
                        this[replacingIndex] = newItems.first()
                        if (newItems.size > 1) {
                            addAll(newItems.drop(1))
                        }
                    }
                    onAttachmentsChange(updated)
                } else if (newItems.isNotEmpty()) {
                    onAttachmentsChange(attachments + newItems)
                }
                replacingIndex = -1
                isProcessing = false
            }
        } else {
            replacingIndex = -1
        }
    }

    // Document/File Picker Launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            isProcessing = true
            scope.launch {
                val newItems = mutableListOf<DocumentAttachment>()
                for (uri in uris) {
                    val att = AttachmentStorageManager.saveContentUriToAttachments(context, uri)
                    if (att != null) {
                        newItems.add(att)
                    }
                }
                if (replacingIndex in attachments.indices && newItems.isNotEmpty()) {
                    val old = attachments[replacingIndex]
                    AttachmentStorageManager.deleteAttachmentFile(old.filePath)
                    val updated = attachments.toMutableList().apply {
                        this[replacingIndex] = newItems.first()
                        if (newItems.size > 1) {
                            addAll(newItems.drop(1))
                        }
                    }
                    onAttachmentsChange(updated)
                } else if (newItems.isNotEmpty()) {
                    onAttachmentsChange(attachments + newItems)
                }
                replacingIndex = -1
                isProcessing = false
            }
        } else {
            replacingIndex = -1
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AttachFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ATTACHMENTS (${attachments.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = TextMutedDark
                )
            }
            if (isProcessing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Saving...",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextMutedDark
                    )
                }
            }
        }

        // Action Buttons Row if editable
        if (isEditable) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Scan via Camera
                AttachmentActionCard(
                    icon = Icons.Outlined.DocumentScanner,
                    label = "Scan Camera",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { launchCameraScan(-1) },
                    modifier = Modifier.weight(1f)
                )

                // Gallery Image
                AttachmentActionCard(
                    icon = Icons.Outlined.Image,
                    label = "Gallery",
                    color = Color(0xFF64B5F6),
                    onClick = {
                        replacingIndex = -1
                        galleryLauncher.launch("image/*")
                    },
                    modifier = Modifier.weight(1f)
                )

                // File/PDF Picker
                AttachmentActionCard(
                    icon = Icons.Outlined.PictureAsPdf,
                    label = "PDF / File",
                    color = Color(0xFFFF8A65),
                    onClick = {
                        replacingIndex = -1
                        filePickerLauncher.launch(arrayOf("application/pdf", "image/*", "*/*"))
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // List of Attachments
        if (attachments.isEmpty()) {
            if (isEditable) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassSurfaceDark)
                        .border(1.dp, GlassBorderDark, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No files attached yet. Scan or pick files above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedDark
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                attachments.forEachIndexed { index, attachment ->
                    AttachmentItemCard(
                        attachment = attachment,
                        isEditable = isEditable,
                        onClick = {
                            if (AttachmentStorageManager.isImage(attachment)) {
                                viewingAttachment = attachment
                            } else {
                                val result = AttachmentStorageManager.openAttachmentInSystemViewer(context, attachment)
                                if (result.isFailure) {
                                    Toast.makeText(
                                        context,
                                        result.exceptionOrNull()?.localizedMessage ?: "Cannot open file",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        onDelete = if (isEditable) {
                            {
                                AttachmentStorageManager.deleteAttachmentFile(attachment.filePath)
                                val updated = attachments.toMutableList().apply { removeAt(index) }
                                onAttachmentsChange(updated)
                            }
                        } else null,
                        onReplaceScan = if (isEditable) { { launchCameraScan(index) } } else null,
                        onReplaceGallery = if (isEditable) {
                            {
                                replacingIndex = index
                                galleryLauncher.launch("image/*")
                            }
                        } else null,
                        onReplaceFile = if (isEditable) {
                            {
                                replacingIndex = index
                                filePickerLauncher.launch(arrayOf("application/pdf", "image/*", "*/*"))
                            }
                        } else null
                    )
                }
            }
        }
    }

    // Full-screen Image Viewer Dialog
    viewingAttachment?.let { attachment ->
        AttachmentViewerDialog(
            attachment = attachment,
            onDismiss = { viewingAttachment = null }
        )
    }

    // Full-screen Document Scanner Dialog
    if (showScanner) {
        Dialog(
            onDismissRequest = {
                showScanner = false
                replacingIndex = -1
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                DocumentScannerScreen(
                    documentName = documentName.ifBlank { "Scan" },
                    onFinishScan = { newScannedAttachments ->
                        if (newScannedAttachments.isNotEmpty()) {
                            if (replacingIndex in attachments.indices) {
                                val old = attachments[replacingIndex]
                                AttachmentStorageManager.deleteAttachmentFile(old.filePath)
                                val updated = attachments.toMutableList().apply {
                                    this[replacingIndex] = newScannedAttachments.first()
                                    if (newScannedAttachments.size > 1) {
                                        addAll(newScannedAttachments.drop(1))
                                    }
                                }
                                onAttachmentsChange(updated)
                            } else {
                                onAttachmentsChange(attachments + newScannedAttachments)
                            }
                        }
                        replacingIndex = -1
                        showScanner = false
                    },
                    onCancel = {
                        replacingIndex = -1
                        showScanner = false
                    }
                )
            }
        }
    }
}

@Composable
fun AttachmentActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
            .bouncyClickable(pressedScale = 0.95f) { onClick() }
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                ),
                color = color
            )
        }
    }
}

@Composable
fun AttachmentItemCard(
    attachment: DocumentAttachment,
    isEditable: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onReplaceScan: (() -> Unit)? = null,
    onReplaceGallery: (() -> Unit)? = null,
    onReplaceFile: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showReplaceMenu by remember { mutableStateOf(false) }
    val isImage = AttachmentStorageManager.isImage(attachment)
    val isPdf = AttachmentStorageManager.isPdf(attachment)
    val formattedSize = remember(attachment.fileSizeBytes) {
        AttachmentStorageManager.formatFileSize(attachment.fileSizeBytes)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GlassSurfaceDark)
            .border(1.dp, GlassBorderDark, RoundedCornerShape(14.dp))
            .glassCardClickable { onClick() }
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Thumbnail / Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isPdf) Color(0xFFFF5722).copy(alpha = 0.12f)
                            else if (isImage) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else Color.White.copy(alpha = 0.08f)
                        )
                        .border(
                            1.dp,
                            if (isPdf) Color(0xFFFF5722).copy(alpha = 0.3f)
                            else if (isImage) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            else GlassBorderDark,
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isImage) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(File(attachment.filePath))
                                .crossfade(true)
                                .build(),
                            contentDescription = attachment.fileName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (isPdf) {
                        Icon(
                            imageVector = Icons.Outlined.PictureAsPdf,
                            contentDescription = "PDF",
                            tint = Color(0xFFFF5722),
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.InsertDriveFile,
                            contentDescription = "File",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name & Metadata
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = attachment.fileName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Tag Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isPdf) Color(0xFFFF5722).copy(alpha = 0.18f)
                                    else if (isImage) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    else Color.White.copy(alpha = 0.1f)
                                )
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isPdf) "PDF" else if (isImage) "IMAGE" else "FILE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                ),
                                color = if (isPdf) Color(0xFFFF8A65)
                                else if (isImage) MaterialTheme.colorScheme.primary
                                else TextMutedDark
                            )
                        }

                        Text(
                            text = formattedSize,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextMutedDark
                        )
                    }
                }
            }

            // Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                // View button
                IconButton(
                    onClick = onClick,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Visibility,
                        contentDescription = "View Attachment",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (isEditable) {
                    // Replace button
                    Box {
                        IconButton(
                            onClick = { showReplaceMenu = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SwapHoriz,
                                contentDescription = "Replace File",
                                tint = TextMutedDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showReplaceMenu,
                            onDismissRequest = { showReplaceMenu = false }
                        ) {
                            if (onReplaceScan != null) {
                                DropdownMenuItem(
                                    text = { Text("Replace with Scan (Camera)") },
                                    leadingIcon = { Icon(Icons.Outlined.DocumentScanner, contentDescription = null) },
                                    onClick = {
                                        showReplaceMenu = false
                                        onReplaceScan()
                                    }
                                )
                            }
                            if (onReplaceGallery != null) {
                                DropdownMenuItem(
                                    text = { Text("Replace from Gallery") },
                                    leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
                                    onClick = {
                                        showReplaceMenu = false
                                        onReplaceGallery()
                                    }
                                )
                            }
                            if (onReplaceFile != null) {
                                DropdownMenuItem(
                                    text = { Text("Replace with PDF/File") },
                                    leadingIcon = { Icon(Icons.Outlined.PictureAsPdf, contentDescription = null) },
                                    onClick = {
                                        showReplaceMenu = false
                                        onReplaceFile()
                                    }
                                )
                            }
                        }
                    }

                    // Delete button
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Delete Attachment",
                                tint = StatusExpired,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttachmentViewerDialog(
    attachment: DocumentAttachment,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = attachment.fileName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = AttachmentStorageManager.formatFileSize(attachment.fileSizeBytes),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                IconButton(
                    onClick = {
                        val result = AttachmentStorageManager.openAttachmentInSystemViewer(context, attachment)
                        if (result.isFailure) {
                            Toast.makeText(
                                context,
                                result.exceptionOrNull()?.localizedMessage ?: "Unable to open in external viewer",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.OpenInNew,
                        contentDescription = "Open in External App",
                        tint = Color.White
                    )
                }
            }

            // Image with interactive Zoom & Pan Gestures
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 80.dp)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                val maxOffsetX = (size.width * (scale - 1)) / 2
                                val maxOffsetY = (size.height * (scale - 1)) / 2
                                offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                                offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(attachment.filePath))
                        .crossfade(true)
                        .build(),
                    contentDescription = attachment.fileName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                )
            }

            // Reset Zoom / Quick Action Bar at bottom
            if (scale > 1.05f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                ) {
                    Button(
                        onClick = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Zoom")
                    }
                }
            }
        }
    }
}
