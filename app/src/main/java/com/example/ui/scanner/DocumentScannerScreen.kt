package com.example.ui.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.PointF
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.DocumentAttachment
import com.example.data.storage.AttachmentStorageManager
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

private enum class ScannerState {
    CAMERA_CAPTURE,
    CORNER_ADJUSTMENT,
    CONFIRMATION_REVIEW
}

data class ScannedPage(
    val rawBitmap: Bitmap,
    val croppedBitmap: Bitmap,
    val corners: DocumentScanProcessor.QuadCorners,
    val rotationDegrees: Float = 0f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScannerScreen(
    documentName: String = "Document",
    onFinishScan: (List<DocumentAttachment>) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var scannerState by remember { mutableStateOf(ScannerState.CAMERA_CAPTURE) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }

    val scannedPages = remember { mutableStateListOf<ScannedPage>() }

    var currentRawBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentCorners by remember { mutableStateOf<DocumentScanProcessor.QuadCorners?>(null) }
    var currentCroppedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var isProcessing by remember { mutableStateOf(false) }
    var processingMessage by remember { mutableStateOf("Processing...") }

    // Camera references
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var liveDetectionState by remember { mutableStateOf(DocumentScanProcessor.LiveDetectionState.EMPTY) }

    // Permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Camera permission needed for scanning documents", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Gallery Picker Fallback
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessing = true
            processingMessage = "Detecting document edges..."
            scope.launch {
                try {
                    val bitmap = loadGalleryBitmap(context, uri)
                    if (bitmap != null) {
                        currentRawBitmap = bitmap
                        val detectedCorners = DocumentScanProcessor.detectDocumentCorners(bitmap)
                        currentCorners = detectedCorners
                        val cropped = DocumentScanProcessor.cropAndStraighten(bitmap, detectedCorners)
                        currentCroppedBitmap = cropped
                        scannerState = ScannerState.CORNER_ADJUSTMENT
                    } else {
                        Toast.makeText(context, "Failed to load image from gallery", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Throwable) {
                    Toast.makeText(context, "Failed to load image from gallery", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    // Back handling
    BackHandler {
        when (scannerState) {
            ScannerState.CAMERA_CAPTURE -> onCancel()
            ScannerState.CORNER_ADJUSTMENT -> scannerState = ScannerState.CAMERA_CAPTURE
            ScannerState.CONFIRMATION_REVIEW -> scannerState = ScannerState.CORNER_ADJUSTMENT
        }
    }

    // Save & Complete Scan function (Natural image, 100% offline, NO filters)
    val saveAndCompleteScan: (List<ScannedPage>) -> Unit = { pagesToSave ->
        val finalPages = if (pagesToSave.isNotEmpty()) {
            pagesToSave
        } else if (currentCroppedBitmap != null && currentRawBitmap != null && currentCorners != null) {
            listOf(
                ScannedPage(
                    rawBitmap = currentRawBitmap!!,
                    croppedBitmap = currentCroppedBitmap!!,
                    corners = currentCorners!!
                )
            )
        } else {
            emptyList()
        }

        if (finalPages.isEmpty()) {
            onCancel()
        } else {
            isProcessing = true
            processingMessage = "Saving natural scan..."
            scope.launch {
                val results = mutableListOf<DocumentAttachment>()
                val attachmentsDir = AttachmentStorageManager.getAttachmentsDir(context)
                val dateStr = SimpleDateFormat("ddMMM_HHmm", Locale.getDefault()).format(Date())
                val cleanDocName = documentName.trim().take(20).replace(Regex("[^a-zA-Z0-9_]"), "_").ifBlank { "Scan" }

                for ((idx, page) in finalPages.withIndex()) {
                    val pageSuffix = if (finalPages.size > 1) "_p${idx + 1}" else ""
                    val fileName = "${cleanDocName}${pageSuffix}_$dateStr.jpg"
                    val targetFile = File(attachmentsDir, "scan_${UUID.randomUUID()}.jpg")

                    val success = DocumentScanProcessor.saveBitmapToFile(page.croppedBitmap, targetFile)
                    if (success) {
                        results.add(
                            DocumentAttachment(
                                id = UUID.randomUUID().toString(),
                                fileName = fileName,
                                filePath = targetFile.absolutePath,
                                mimeType = "image/jpeg",
                                fileSizeBytes = targetFile.length(),
                                addedAtMillis = System.currentTimeMillis()
                            )
                        )
                    }
                }

                isProcessing = false
                if (results.isNotEmpty()) {
                    onFinishScan(results)
                } else {
                    onCancel()
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        when (scannerState) {
            ScannerState.CAMERA_CAPTURE -> {
                if (hasCameraPermission) {
                    CameraCaptureView(
                        flashMode = flashMode,
                        liveDetectionState = liveDetectionState,
                        onLiveDetectionUpdated = { liveDetectionState = it },
                        onFlashToggle = {
                            flashMode = when (flashMode) {
                                ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                                ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                                else -> ImageCapture.FLASH_MODE_OFF
                            }
                            imageCapture?.flashMode = flashMode
                        },
                        onCapture = {
                            val ic = imageCapture ?: return@CameraCaptureView
                            val captureTime = System.currentTimeMillis()
                            val frozenQuad = liveDetectionState.normalizedQuad?.let { quad ->
                                DocumentScanProcessor.NormalizedQuad(
                                    topLeft = PointF(quad.topLeft.x, quad.topLeft.y),
                                    topRight = PointF(quad.topRight.x, quad.topRight.y),
                                    bottomRight = PointF(quad.bottomRight.x, quad.bottomRight.y),
                                    bottomLeft = PointF(quad.bottomLeft.x, quad.bottomLeft.y)
                                )
                            }
                            val frozenLiveState = DocumentScanProcessor.LiveDetectionState(
                                isDetected = liveDetectionState.isDetected,
                                normalizedQuad = frozenQuad,
                                confidence = liveDetectionState.confidence,
                                timestamp = liveDetectionState.timestamp,
                                isStable = liveDetectionState.isStable
                            )
                            isProcessing = true
                            processingMessage = "Processing document scan..."

                            val tempFile = File(context.cacheDir, "temp_scan_${System.currentTimeMillis()}.jpg")
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()

                            ic.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        scope.launch {
                                            try {
                                                val exif = ExifInterface(tempFile.absolutePath)
                                                val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

                                                val options = BitmapFactory.Options().apply {
                                                    inJustDecodeBounds = true
                                                }
                                                BitmapFactory.decodeFile(tempFile.absolutePath, options)

                                                var sample = 1
                                                val maxDim = max(options.outWidth, options.outHeight)
                                                if (maxDim > 1920) {
                                                    sample = (maxDim / 1920f).toInt().coerceAtLeast(1)
                                                }

                                                val decodeOptions = BitmapFactory.Options().apply {
                                                    inSampleSize = sample
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

                                                    currentRawBitmap = bitmap
                                                    val isRecentAndValid = frozenLiveState.isDetected &&
                                                            frozenLiveState.normalizedQuad != null &&
                                                            (captureTime - frozenLiveState.timestamp < 3500L)

                                                    val detectedCorners = if (isRecentAndValid) {
                                                        frozenLiveState.normalizedQuad!!.toQuadCorners(
                                                            bitmap.width.toFloat(),
                                                            bitmap.height.toFloat()
                                                        )
                                                    } else {
                                                        // Safe fallback: static detector on high-res bitmap
                                                        DocumentScanProcessor.detectDocumentCorners(bitmap)
                                                    }

                                                    currentCorners = detectedCorners
                                                    val cropped = DocumentScanProcessor.cropAndStraighten(bitmap, detectedCorners)
                                                    currentCroppedBitmap = cropped

                                                    scannerState = ScannerState.CORNER_ADJUSTMENT
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Error capturing photo: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                            } finally {
                                                tempFile.delete()
                                                isProcessing = false
                                            }
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        isProcessing = false
                                        Toast.makeText(context, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        },
                        onPickGallery = { galleryLauncher.launch("image/*") },
                        onClose = onCancel,
                        onBindCamera = { capture, control ->
                            imageCapture = capture
                            cameraControl = control
                        },
                        scannedPagesCount = scannedPages.size,
                        onDone = { saveAndCompleteScan(scannedPages) }
                    )
                } else {
                    // Camera permission rationale UI
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoCamera,
                            contentDescription = null,
                            tint = KaamKaajGold,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Camera Permission Required",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "To scan physical documents, IDs, and bills offline, KaamKaaj needs access to your camera.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = KaamKaajGold)
                        ) {
                            Text("GRANT CAMERA PERMISSION", color = Color(0xFF050505), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = onCancel) {
                            Text("Cancel", color = Color(0xFF94A3B8))
                        }
                    }
                }
            }

            ScannerState.CORNER_ADJUSTMENT -> {
                val raw = currentRawBitmap
                val corners = currentCorners
                if (raw != null && corners != null) {
                    CornerAdjustmentView(
                        bitmap = raw,
                        initialCorners = corners,
                        pageNumber = scannedPages.size + 1,
                        onCornersConfirmed = { updatedCorners ->
                            currentCorners = updatedCorners
                            isProcessing = true
                            processingMessage = "Straightening document..."
                            scope.launch {
                                try {
                                    val cropped = DocumentScanProcessor.cropAndStraighten(raw, updatedCorners)
                                    currentCroppedBitmap = cropped
                                    scannerState = ScannerState.CONFIRMATION_REVIEW
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error cropping: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isProcessing = false
                                }
                            }
                        },
                        onAutoDetect = {
                            isProcessing = true
                            processingMessage = "Auto-detecting edges..."
                            scope.launch {
                                val detected = DocumentScanProcessor.detectDocumentCorners(raw)
                                currentCorners = detected
                                isProcessing = false
                            }
                        },
                        onReset = {
                            currentCorners = DocumentScanProcessor.QuadCorners.defaultForSize(raw.width.toFloat(), raw.height.toFloat(), 0.02f)
                        },
                        onRetake = {
                            scannerState = ScannerState.CAMERA_CAPTURE
                        }
                    )
                }
            }

            ScannerState.CONFIRMATION_REVIEW -> {
                val cropped = currentCroppedBitmap
                if (cropped != null) {
                    ConfirmationReviewView(
                        previewBitmap = cropped,
                        pageNumber = scannedPages.size + 1,
                        onRotate = {
                            isProcessing = true
                            scope.launch {
                                val rotated = DocumentScanProcessor.rotateBitmap(cropped, 90f)
                                currentCroppedBitmap = rotated
                                isProcessing = false
                            }
                        },
                        onAdjustCrop = {
                            scannerState = ScannerState.CORNER_ADJUSTMENT
                        },
                        onRetake = {
                            scannerState = ScannerState.CAMERA_CAPTURE
                        },
                        onAddNextPage = {
                            scannedPages.add(
                                ScannedPage(
                                    rawBitmap = currentRawBitmap!!,
                                    croppedBitmap = cropped,
                                    corners = currentCorners!!
                                )
                            )
                            currentRawBitmap = null
                            currentCroppedBitmap = null
                            currentCorners = null
                            scannerState = ScannerState.CAMERA_CAPTURE
                        },
                        onDone = {
                            val allPages = scannedPages.toMutableList().apply {
                                add(
                                    ScannedPage(
                                        rawBitmap = currentRawBitmap!!,
                                        croppedBitmap = cropped,
                                        corners = currentCorners!!
                                    )
                                )
                            }
                            saveAndCompleteScan(allPages)
                        }
                    )
                }
            }
        }

        // Processing Overlay
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = KaamKaajGold,
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 3.5.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = processingMessage,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraCaptureView(
    flashMode: Int,
    liveDetectionState: DocumentScanProcessor.LiveDetectionState,
    onLiveDetectionUpdated: (DocumentScanProcessor.LiveDetectionState) -> Unit,
    onFlashToggle: () -> Unit,
    onCapture: () -> Unit,
    onPickGallery: () -> Unit,
    onClose: () -> Unit,
    onBindCamera: (ImageCapture, CameraControl) -> Unit,
    scannedPagesCount: Int,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val tracker = remember { DocumentTemporalTracker() }
    val analysisExecutor = remember { java.util.concurrent.Executors.newSingleThreadExecutor() }
    DisposableEffect(context) {
        onDispose {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
            } catch (_: Exception) {}
            tracker.reset()
            analysisExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Live Camera Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                    val imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setFlashMode(flashMode)
                        .build()

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build()

                    imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        try {
                            val result = DocumentScanProcessor.processAnalysisFrame(imageProxy, tracker)
                            onLiveDetectionUpdated(result)
                        } catch (e: Exception) {
                            onLiveDetectionUpdated(DocumentScanProcessor.LiveDetectionState.EMPTY)
                        } finally {
                            imageProxy.close()
                        }
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture,
                            imageAnalysis
                        )
                        onBindCamera(imageCapture, camera.cameraControl)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Real-Time Detected Document Live Overlay & Adaptive Viewfinder
        val liveQuad = liveDetectionState.normalizedQuad
        val isDocDetected = liveDetectionState.isDetected && liveQuad != null

        // Subtle breathing animation for active document lock (lightweight float state without canvas relayout)
        val guideOpacity by animateFloatAsState(
            targetValue = if (isDocDetected) 0.20f else 0.85f,
            animationSpec = tween(250),
            label = "guide_opacity"
        )

        if (isDocDetected && liveQuad != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val streamAspect = 3f / 4f
                val viewAspect = w / h

                val (scaledW, scaledH, offsetX, offsetY) = if (viewAspect < streamAspect) {
                    val sH = h
                    val sW = h * streamAspect
                    val offX = (w - sW) / 2f
                    val offY = 0f
                    listOf(sW, sH, offX, offY)
                } else {
                    val sW = w
                    val sH = w / streamAspect
                    val offX = 0f
                    val offY = (h - sH) / 2f
                    listOf(sW, sH, offX, offY)
                }

                val tl = Offset(offsetX + liveQuad.topLeft.x * scaledW, offsetY + liveQuad.topLeft.y * scaledH)
                val tr = Offset(offsetX + liveQuad.topRight.x * scaledW, offsetY + liveQuad.topRight.y * scaledH)
                val br = Offset(offsetX + liveQuad.bottomRight.x * scaledW, offsetY + liveQuad.bottomRight.y * scaledH)
                val bl = Offset(offsetX + liveQuad.bottomLeft.x * scaledW, offsetY + liveQuad.bottomLeft.y * scaledH)

                val quadPath = Path().apply {
                    moveTo(tl.x, tl.y)
                    lineTo(tr.x, tr.y)
                    lineTo(br.x, br.y)
                    lineTo(bl.x, bl.y)
                    close()
                }

                // 1. Semi-transparent premium highlight fill
                drawPath(
                    path = quadPath,
                    color = KaamKaajGold.copy(alpha = 0.16f)
                )

                // 2. High-contrast dual-tone boundary outline
                // Subtle dark outline for high contrast on bright white documents
                drawPath(
                    path = quadPath,
                    color = Color(0x40000000),
                    style = Stroke(width = 4.dp.toPx())
                )
                // Crisp luminous signature gold boundary stroke
                drawPath(
                    path = quadPath,
                    color = KaamKaajGold.copy(alpha = 0.90f),
                    style = Stroke(width = 2.dp.toPx())
                )

                // 3. Modern precision L-bracket reticles & accent nodes at 4 corners
                fun drawCornerReticle(corner: Offset, toEdge1: Offset, toEdge2: Offset) {
                    val d1 = hypot(toEdge1.x - corner.x, toEdge1.y - corner.y).coerceAtLeast(1f)
                    val d2 = hypot(toEdge2.x - corner.x, toEdge2.y - corner.y).coerceAtLeast(1f)
                    val armLength = min(22.dp.toPx(), 0.25f * min(d1, d2))

                    val u1 = Offset((toEdge1.x - corner.x) / d1 * armLength, (toEdge1.y - corner.y) / d1 * armLength)
                    val u2 = Offset((toEdge2.x - corner.x) / d2 * armLength, (toEdge2.y - corner.y) / d2 * armLength)

                    // Shadow / contrast underlay for bracket arms
                    val shadowWidth = 5.dp.toPx()
                    drawLine(Color(0x66000000), corner, corner + u1, strokeWidth = shadowWidth, cap = StrokeCap.Round)
                    drawLine(Color(0x66000000), corner, corner + u2, strokeWidth = shadowWidth, cap = StrokeCap.Round)

                    // Crisp Accent bracket arms
                    val bracketWidth = 3.2.dp.toPx()
                    drawLine(KaamKaajGold, corner, corner + u1, strokeWidth = bracketWidth, cap = StrokeCap.Round)
                    drawLine(KaamKaajGold, corner, corner + u2, strokeWidth = bracketWidth, cap = StrokeCap.Round)

                    // Modern Precision Target Node at corner apex
                    drawCircle(color = Color(0x80000000), radius = 5.dp.toPx(), center = corner)
                    drawCircle(color = Color.White, radius = 3.5.dp.toPx(), center = corner)
                    drawCircle(color = KaamKaajGold, radius = 2.dp.toPx(), center = corner)
                }

                drawCornerReticle(tl, tr, bl)
                drawCornerReticle(tr, tl, br)
                drawCornerReticle(br, tr, bl)
                drawCornerReticle(bl, br, tl)
            }
        }

        // Viewfinder Guide Corner Overlays (Smoothly dimmed when document is actively tracked)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val guideMarginX = w * 0.08f
            val guideMarginY = h * 0.14f
            val cornerLength = 36.dp.toPx()
            val strokeWidth = 3.5.dp.toPx()

            val left = guideMarginX
            val right = w - guideMarginX
            val top = guideMarginY
            val bottom = h - guideMarginY - 80.dp.toPx()

            val guideColor = KaamKaajGold.copy(alpha = guideOpacity)

            // Top-Left Corner
            drawLine(guideColor, Offset(left, top), Offset(left + cornerLength, top), strokeWidth, cap = StrokeCap.Round)
            drawLine(guideColor, Offset(left, top), Offset(left, top + cornerLength), strokeWidth, cap = StrokeCap.Round)

            // Top-Right Corner
            drawLine(guideColor, Offset(right, top), Offset(right - cornerLength, top), strokeWidth, cap = StrokeCap.Round)
            drawLine(guideColor, Offset(right, top), Offset(right, top + cornerLength), strokeWidth, cap = StrokeCap.Round)

            // Bottom-Left Corner
            drawLine(guideColor, Offset(left, bottom), Offset(left + cornerLength, bottom), strokeWidth, cap = StrokeCap.Round)
            drawLine(guideColor, Offset(left, bottom), Offset(left, bottom - cornerLength), strokeWidth, cap = StrokeCap.Round)

            // Bottom-Right Corner
            drawLine(guideColor, Offset(right, bottom), Offset(right - cornerLength, bottom), strokeWidth, cap = StrokeCap.Round)
            drawLine(guideColor, Offset(right, bottom), Offset(right, bottom - cornerLength), strokeWidth, cap = StrokeCap.Round)
        }

        // Top Control Bar (Safely inset from status bar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = Color.White)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(100.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "AUTO EDGE SCANNER",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = KaamKaajGold
                )
            }

            IconButton(
                onClick = onFlashToggle,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                val flashIcon = when (flashMode) {
                    ImageCapture.FLASH_MODE_ON -> Icons.Filled.FlashOn
                    ImageCapture.FLASH_MODE_AUTO -> Icons.Filled.FlashAuto
                    else -> Icons.Filled.FlashOff
                }
                Icon(flashIcon, contentDescription = "Flash", tint = Color.White)
            }
        }

        // Bottom Controls Bar (Safely elevated above system navigation/gesture area with generous clear gap)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color(0xD9000000))
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Align document & tap shutter for auto-edge crop",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = Color(0xFFCBD5E1),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery import button (Guaranteed fully visible)
                IconButton(
                    onClick = onPickGallery,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = "Import from Gallery", tint = Color.White)
                }

                // Center Shutter Button (Existing gold circle design preserved)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                        .border(3.dp, KaamKaajGold, CircleShape)
                        .clickable { onCapture() }
                        .padding(5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }

                // Done / Multi-page save button
                if (scannedPagesCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(KaamKaajGold)
                            .clickable { onDone() }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Save ($scannedPagesCount)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF050505)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            // Visible safe buffer space above gesture bar (elevated ~64-70dp)
            Spacer(modifier = Modifier.height(76.dp))
        }
    }
}

@Composable
private fun CornerAdjustmentView(
    bitmap: Bitmap,
    initialCorners: DocumentScanProcessor.QuadCorners,
    pageNumber: Int,
    onCornersConfirmed: (DocumentScanProcessor.QuadCorners) -> Unit,
    onAutoDetect: () -> Unit,
    onReset: () -> Unit,
    onRetake: () -> Unit
) {
    var displaySize by remember { mutableStateOf(IntSize.Zero) }

    // Normalized corner positions [0..1]
    var tlNorm by remember(initialCorners) {
        mutableStateOf(Offset(initialCorners.topLeft.x / bitmap.width, initialCorners.topLeft.y / bitmap.height))
    }
    var trNorm by remember(initialCorners) {
        mutableStateOf(Offset(initialCorners.topRight.x / bitmap.width, initialCorners.topRight.y / bitmap.height))
    }
    var brNorm by remember(initialCorners) {
        mutableStateOf(Offset(initialCorners.bottomRight.x / bitmap.width, initialCorners.bottomRight.y / bitmap.height))
    }
    var blNorm by remember(initialCorners) {
        mutableStateOf(Offset(initialCorners.bottomLeft.x / bitmap.width, initialCorners.bottomLeft.y / bitmap.height))
    }

    var activeCorner by remember { mutableIntStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C12))
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Top Toolbar (Safely below status bar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onRetake,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retake", tint = Color.White)
            }

            Text(
                text = "ADJUST CROP CORNERS",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                ),
                color = KaamKaajGold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = onAutoDetect,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Auto Detect", color = KaamKaajGold, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                TextButton(
                    onClick = onReset,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Full", color = Color(0xFF94A3B8), fontSize = 13.sp)
                }
            }
        }

        // Center Quadrilateral Adjustment Canvas (Takes remaining flexible space without pushing bottom controls)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF14141E))
                .onGloballyPositioned { displaySize = it.size },
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Document Scan",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            // Interactive quadrilateral canvas
            if (displaySize.width > 0 && displaySize.height > 0) {
                val imageAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
                val containerAspect = displaySize.width.toFloat() / displaySize.height.toFloat()

                val renderW: Float
                val renderH: Float
                val offsetX: Float
                val offsetY: Float

                if (containerAspect > imageAspect) {
                    renderH = displaySize.height.toFloat()
                    renderW = renderH * imageAspect
                    offsetX = (displaySize.width - renderW) / 2f
                    offsetY = 0f
                } else {
                    renderW = displaySize.width.toFloat()
                    renderH = renderW / imageAspect
                    offsetX = 0f
                    offsetY = (displaySize.height - renderH) / 2f
                }

                val pTL = Offset(offsetX + tlNorm.x * renderW, offsetY + tlNorm.y * renderH)
                val pTR = Offset(offsetX + trNorm.x * renderW, offsetY + trNorm.y * renderH)
                val pBR = Offset(offsetX + brNorm.x * renderW, offsetY + brNorm.y * renderH)
                val pBL = Offset(offsetX + blNorm.x * renderW, offsetY + blNorm.y * renderH)

                val cornersList = listOf(pTL, pTR, pBR, pBL)
                val currentCornersState by rememberUpdatedState(cornersList)
                val currentRenderW by rememberUpdatedState(renderW)
                val currentRenderH by rememberUpdatedState(renderH)

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { startOffset ->
                                    val threshold = 52.dp.toPx()
                                    val pts = currentCornersState
                                    var closestIndex = -1
                                    var minDistance = Float.MAX_VALUE
                                    for (i in pts.indices) {
                                        val dist = hypot(startOffset.x - pts[i].x, startOffset.y - pts[i].y)
                                        if (dist < threshold && dist < minDistance) {
                                            minDistance = dist
                                            closestIndex = i
                                        }
                                    }
                                    activeCorner = closestIndex
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (activeCorner != -1 && renderW > 0 && renderH > 0) {
                                        val deltaNormX = dragAmount.x / renderW
                                        val deltaNormY = dragAmount.y / renderH

                                        when (activeCorner) {
                                            0 -> tlNorm = Offset((tlNorm.x + deltaNormX).coerceIn(0f, 1f), (tlNorm.y + deltaNormY).coerceIn(0f, 1f))
                                            1 -> trNorm = Offset((trNorm.x + deltaNormX).coerceIn(0f, 1f), (trNorm.y + deltaNormY).coerceIn(0f, 1f))
                                            2 -> brNorm = Offset((brNorm.x + deltaNormX).coerceIn(0f, 1f), (brNorm.y + deltaNormY).coerceIn(0f, 1f))
                                            3 -> blNorm = Offset((blNorm.x + deltaNormX).coerceIn(0f, 1f), (blNorm.y + deltaNormY).coerceIn(0f, 1f))
                                        }
                                    }
                                },
                                onDragEnd = { activeCorner = -1 },
                                onDragCancel = { activeCorner = -1 }
                            )
                        }
                ) {
                    val path = Path().apply {
                        moveTo(pTL.x, pTL.y)
                        lineTo(pTR.x, pTR.y)
                        lineTo(pBR.x, pBR.y)
                        lineTo(pBL.x, pBL.y)
                        close()
                    }

                    // Shaded area & bounding outline
                    drawPath(path, Color(0xFFF59E0B).copy(alpha = 0.15f))
                    drawPath(path, Color(0xFFF59E0B), style = Stroke(width = 2.5.dp.toPx()))

                    // Grid lines inside quad for alignment aid
                    val midTop = Offset((pTL.x + pTR.x) / 2f, (pTL.y + pTR.y) / 2f)
                    val midBottom = Offset((pBL.x + pBR.x) / 2f, (pBL.y + pBR.y) / 2f)
                    val midLeft = Offset((pTL.x + pBL.x) / 2f, (pTL.y + pBL.y) / 2f)
                    val midRight = Offset((pTR.x + pBR.x) / 2f, (pTR.y + pBR.y) / 2f)

                    drawLine(Color(0xFFF59E0B).copy(alpha = 0.35f), midTop, midBottom, strokeWidth = 1.dp.toPx())
                    drawLine(Color(0xFFF59E0B).copy(alpha = 0.35f), midLeft, midRight, strokeWidth = 1.dp.toPx())

                    // Draw 4 corner handles
                    val cornersList = listOf(pTL, pTR, pBR, pBL)
                    cornersList.forEachIndexed { index, cornerPos ->
                        val isHandleActive = activeCorner == index
                        val handleRadius = if (isHandleActive) 16.dp.toPx() else 12.dp.toPx()

                        drawCircle(
                            color = Color.Black.copy(alpha = 0.4f),
                            radius = handleRadius + 3.dp.toPx(),
                            center = cornerPos
                        )
                        drawCircle(
                            color = if (isHandleActive) Color.White else Color(0xFFF59E0B),
                            radius = handleRadius,
                            center = cornerPos
                        )
                        drawCircle(
                            color = if (isHandleActive) Color(0xFFF59E0B) else Color.White,
                            radius = handleRadius * 0.45f,
                            center = cornerPos
                        )
                    }
                }
            }
        }

        // Bottom Action Bar (Safely elevated with WindowInsets + visible buffer gap)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF12121A))
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Drag 4 pins to adjust edges",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color(0xFF94A3B8)
                )

                Button(
                    onClick = {
                        val finalCorners = DocumentScanProcessor.QuadCorners(
                            topLeft = PointF(tlNorm.x * bitmap.width, tlNorm.y * bitmap.height),
                            topRight = PointF(trNorm.x * bitmap.width, trNorm.y * bitmap.height),
                            bottomRight = PointF(brNorm.x * bitmap.width, brNorm.y * bitmap.height),
                            bottomLeft = PointF(blNorm.x * bitmap.width, blNorm.y * bitmap.height)
                        )
                        onCornersConfirmed(finalCorners)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KaamKaajGold),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF050505), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("CONFIRM CROP", color = Color(0xFF050505), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Visible safe buffer space above gesture bar (elevated ~64-70dp)
            Spacer(modifier = Modifier.height(76.dp))
        }
    }
}

@Composable
private fun ConfirmationReviewView(
    previewBitmap: Bitmap,
    pageNumber: Int,
    onRotate: () -> Unit,
    onAdjustCrop: () -> Unit,
    onRetake: () -> Unit,
    onAddNextPage: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C12))
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onAdjustCrop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to crop", tint = Color.White)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(0xFF14141E))
                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f), RoundedCornerShape(100.dp))
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "PAGE $pageNumber • NATURAL SCAN",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = KaamKaajGold
                )
            }

            // Clearly separated top-right action buttons with independent touch hitboxes
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onRotate,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Icon(Icons.Outlined.RotateRight, contentDescription = "Rotate 90°", tint = Color.White)
                }
                IconButton(
                    onClick = onRetake,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x26EF4444))
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Discard", tint = Color(0xFFEF4444))
                }
            }
        }

        // Center Preview Area (Natural photo, no filters)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF14141E))
                .border(1.dp, Color(0xFF262635), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = previewBitmap.asImageBitmap(),
                contentDescription = "Straightened Document Preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Bottom Controls Container (Elevated with WindowInsets + visible buffer gap)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF12121A))
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onAddNextPage,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF334155))
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ ADD PAGE", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onDone,
                    modifier = Modifier.weight(1.3f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KaamKaajGold),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF050505), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ATTACH SCAN", color = Color(0xFF050505), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Safe buffer space above gesture/navigation bar (~76dp elevation)
            Spacer(modifier = Modifier.height(76.dp))
        }
    }
}

/**
 * Safely decodes, downsamples, and handles EXIF orientation for gallery images.
 * Bounds uncompressed bitmap memory usage to prevent OutOfMemoryError crashes on large photos,
 * while preserving high resolution (max 2048px dimension) for document edge detection and cropping.
 */
private suspend fun loadGalleryBitmap(
    context: Context,
    uri: Uri,
    targetMaxDim: Int = 2048
): Bitmap? = withContext(Dispatchers.IO) {
    try {
        // 1. Measure image dimensions without allocating memory for pixels
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        if (options.outWidth <= 0 || options.outHeight <= 0) {
            return@withContext null
        }

        // 2. Calculate optimal power-of-2 inSampleSize
        var sampleSize = 1
        val maxOriginalDim = max(options.outWidth, options.outHeight)
        while (maxOriginalDim / (sampleSize * 2) >= targetMaxDim) {
            sampleSize *= 2
        }

        // 3. Read EXIF orientation safely from content URI stream
        var orientation = ExifInterface.ORIENTATION_NORMAL
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        } catch (ignored: Throwable) {
            // Ignore EXIF parsing errors
        }

        // 4. Decode downsampled bitmap into memory
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        var bitmap: Bitmap? = null
        context.contentResolver.openInputStream(uri)?.use { stream ->
            bitmap = BitmapFactory.decodeStream(stream, null, decodeOptions)
        }

        if (bitmap == null) return@withContext null

        // 5. Apply EXIF orientation rotation if needed
        if (orientation != ExifInterface.ORIENTATION_NORMAL) {
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            }
            val rotated = Bitmap.createBitmap(bitmap!!, 0, 0, bitmap!!.width, bitmap!!.height, matrix, true)
            if (rotated != bitmap) {
                bitmap!!.recycle()
                bitmap = rotated
            }
        }

        // 6. Ensure image max dimension does not exceed targetMaxDim
        val currentMax = max(bitmap!!.width, bitmap!!.height)
        if (currentMax > targetMaxDim) {
            val scale = targetMaxDim.toFloat() / currentMax
            val newW = (bitmap!!.width * scale).toInt().coerceAtLeast(100)
            val newH = (bitmap!!.height * scale).toInt().coerceAtLeast(100)
            val scaled = Bitmap.createScaledBitmap(bitmap!!, newW, newH, true)
            if (scaled != bitmap) {
                bitmap!!.recycle()
                bitmap = scaled
            }
        }

        bitmap
    } catch (e: Throwable) {
        e.printStackTrace()
        System.gc()
        null
    }
}
