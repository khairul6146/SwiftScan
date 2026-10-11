package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.MainViewModel
import com.example.ui.components.ScanResultSheet
import com.example.util.ScanUtils
import com.example.util.SoundUtils
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(
    viewModel: MainViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToGenerate: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val lastScan by viewModel.lastScan.collectAsState()

    // Mode Settings
    var isBatchMode by remember { mutableStateOf(false) }
    var batchCount by remember { mutableIntStateOf(0) }
    var batchBannerText by remember { mutableStateOf<String?>(null) }
    var soundEnabled by remember { mutableStateOf(true) }

    // Lens switch: Back (default) vs Front camera
    var isBackCamera by remember { mutableStateOf(true) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Auto-clear batch banner notification after 2.5 seconds
    LaunchedEffect(batchBannerText) {
        if (batchBannerText != null) {
            delay(2500)
            batchBannerText = null
        }
    }

    // Shared Barcode Scanner instance
    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
        BarcodeScanning.getClient(options)
    }

    // Photo picker for scanning images from gallery
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                barcodeScanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        if (!barcodes.isNullOrEmpty()) {
                            val barcode = barcodes.first()
                            val content = barcode.rawValue
                            if (!content.isNullOrBlank()) {
                                val accepted = viewModel.onScanResult(
                                    content = content,
                                    format = barcode.format,
                                    type = barcode.valueType
                                )
                                if (accepted) {
                                    ScanUtils.triggerHapticFeedback(context)
                                    if (soundEnabled) SoundUtils.playBeep()
                                    if (isBatchMode) {
                                        batchCount++
                                        batchBannerText = "Scanned: $content"
                                        viewModel.clearLastScan()
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(context, "No QR code or barcode found in image", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to analyze image", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open image file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Dedicated single-thread executor for background image analysis
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
        }
    }

    var lastAnalysisTime = remember { 0L }

    if (hasCameraPermission) {
        val cameraController = remember {
            LifecycleCameraController(context).apply {
                setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                isPinchToZoomEnabled = true
                isTapToFocusEnabled = true
            }
        }

        // Handle camera lens flip (Front / Back)
        LaunchedEffect(isBackCamera) {
            try {
                cameraController.cameraSelector = if (isBackCamera) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Selected camera lens unavailable", Toast.LENGTH_SHORT).show()
            }
        }

        // Configure analyzer with throttling and pause when single-mode result sheet is open
        LaunchedEffect(cameraController, lastScan, isBatchMode) {
            cameraController.setImageAnalysisAnalyzer(
                analysisExecutor,
                MlKitAnalyzer(
                    listOf(barcodeScanner),
                    CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
                    analysisExecutor
                ) { result ->
                    // In single-mode, pause if result sheet is currently open
                    if (!isBatchMode && lastScan != null) return@MlKitAnalyzer

                    val now = System.currentTimeMillis()
                    // Throttle frame processing to every 220ms
                    if (now - lastAnalysisTime < 220L) return@MlKitAnalyzer
                    lastAnalysisTime = now

                    val barcodes = result.getValue(barcodeScanner)
                    if (!barcodes.isNullOrEmpty()) {
                        val barcode = barcodes.first()
                        val content = barcode.rawValue
                        if (!content.isNullOrBlank()) {
                            val accepted = viewModel.onScanResult(
                                content = content,
                                format = barcode.format,
                                type = barcode.valueType
                            )
                            if (accepted) {
                                ScanUtils.triggerHapticFeedback(context)
                                if (soundEnabled) SoundUtils.playBeep()

                                if (isBatchMode) {
                                    batchCount++
                                    batchBannerText = "Scanned #$batchCount: $content"
                                    viewModel.clearLastScan()
                                }
                            }
                        }
                    }
                }
            )
            cameraController.bindToLifecycle(lifecycleOwner)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Live Camera Preview
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        this.controller = cameraController
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Animated Laser Viewfinder Overlay
            ScannerOverlay(isScanningActive = isBatchMode || lastScan == null)

            // TOP BAR: Quick Action Controls
            val torchState by cameraController.torchState.observeAsState(initial = 0)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header icons: Torch, Gallery, Sound, Lens Flip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left cluster: Flashlight & Sound Toggle
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Flashlight toggle (only active on back camera)
                        IconButton(
                            onClick = {
                                if (isBackCamera) {
                                    try {
                                        cameraController.enableTorch(torchState != 1)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Flashlight unavailable", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Flashlight unavailable on front camera", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = isBackCamera,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .testTag("flash_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (torchState == 1 && isBackCamera) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Toggle Flashlight",
                                tint = if (torchState == 1 && isBackCamera) Color(0xFFFFD54F) else if (isBackCamera) Color.White else Color.Gray
                            )
                        }

                        // Sound Beep toggle
                        IconButton(
                            onClick = {
                                soundEnabled = !soundEnabled
                                Toast.makeText(context, if (soundEnabled) "Beep sound ON" else "Beep sound MUTED", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .testTag("sound_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = "Toggle Audio Feedback",
                                tint = if (soundEnabled) Color(0xFF81C784) else Color.White
                            )
                        }
                    }

                    // Mode Toggle Pill (Single Scan vs Batch Scan)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.55f),
                        modifier = Modifier.testTag("mode_toggle_surface")
                    ) {
                        Row(
                            modifier = Modifier.padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (!isBatchMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable {
                                        isBatchMode = false
                                        batchBannerText = null
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Single",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isBatchMode) MaterialTheme.colorScheme.onPrimary else Color.White
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isBatchMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable {
                                        isBatchMode = true
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (batchCount > 0) "Batch ($batchCount)" else "Batch",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBatchMode) MaterialTheme.colorScheme.onPrimary else Color.White
                                )
                            }
                        }
                    }

                    // Right cluster: Camera Lens Switch & Gallery Photo Picker
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Switch Camera Lens (Front/Back)
                        IconButton(
                            onClick = {
                                isBackCamera = !isBackCamera
                                val target = if (isBackCamera) "Back Lens" else "Front Lens"
                                Toast.makeText(context, "Switched to $target", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .testTag("camera_switch_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera Lens",
                                tint = Color.White
                            )
                        }

                        // Pick Image from Gallery
                        IconButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .testTag("gallery_picker_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Scan Image from Gallery",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Batch Mode Scanned Toast / Notification Banner
                AnimatedVisibility(
                    visible = batchBannerText != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shadowElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = batchBannerText ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // BOTTOM CONTROLS CLUSTER: Zoom Options + Bottom Navigation Bar
            val zoomState by cameraController.zoomState.observeAsState()
            val currentZoomRatio = zoomState?.zoomRatio ?: 1f

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Quick Lens Zoom Options Pill (1x, 2x, 3x)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.testTag("zoom_selector_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(1.0f, 2.0f, 3.0f).forEach { zoomLevel ->
                            val isSelected = (currentZoomRatio - zoomLevel).let { kotlin.math.abs(it) < 0.25f }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable {
                                        try {
                                            cameraController.setZoomRatio(zoomLevel)
                                        } catch (_: Exception) {}
                                    }
                                    .testTag("zoom_button_${zoomLevel.toInt()}x"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${zoomLevel.toInt()}x",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White
                                )
                            }
                        }
                    }
                }

                // Batch Mode Done Banner (if batch count > 0)
                if (isBatchMode && batchCount > 0) {
                    FilledTonalButton(
                        onClick = onNavigateToHistory,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.testTag("batch_done_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Batch Scans ($batchCount)")
                    }
                }

                // Main Navigation Buttons (History & Create QR)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // History button
                    FilledTonalButton(
                        onClick = onNavigateToHistory,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        ),
                        modifier = Modifier.testTag("history_nav_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("History")
                    }

                    // Create QR Code button
                    Button(
                        onClick = onNavigateToGenerate,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("generate_nav_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create QR")
                    }
                }
            }
        }
    } else {
        // Camera Permission Request Fallback
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Camera Permission Required",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "SwiftScan uses the camera exclusively to scan QR codes and barcodes in real-time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.fillMaxWidth().testTag("grant_permission_button")
                    ) {
                        Text("Grant Permission")
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet showing scan details and actions in Single Scan mode
    if (!isBatchMode) {
        lastScan?.let { scan ->
            ScanResultSheet(
                scanResult = scan,
                onDismiss = {
                    viewModel.clearLastScan()
                }
            )
        }
    }
}

@Composable
fun ScannerOverlay(isScanningActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "LaserAnimation")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserProgress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val boxSize = width * 0.72f
        val left = (width - boxSize) / 2
        val top = (height - boxSize) / 2.35f

        // Darkened background framing mask
        drawRect(
            color = Color.Black.copy(alpha = 0.52f)
        )

        // Clear center transparent viewfinder
        drawRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(boxSize, boxSize),
            blendMode = BlendMode.Clear
        )

        // Accent Corners
        val cornerLength = 36.dp.toPx()
        val cornerThickness = 4.dp.toPx()
        val accentColor = Color(0xFF64B5F6)

        // Top Left
        drawLine(accentColor, Offset(left, top), Offset(left + cornerLength, top), cornerThickness)
        drawLine(accentColor, Offset(left, top), Offset(left, top + cornerLength), cornerThickness)

        // Top Right
        drawLine(accentColor, Offset(left + boxSize, top), Offset(left + boxSize - cornerLength, top), cornerThickness)
        drawLine(accentColor, Offset(left + boxSize, top), Offset(left + boxSize, top + cornerLength), cornerThickness)

        // Bottom Left
        drawLine(accentColor, Offset(left, top + boxSize), Offset(left + cornerLength, top + boxSize), cornerThickness)
        drawLine(accentColor, Offset(left, top + boxSize), Offset(left, top + boxSize - cornerLength), cornerThickness)

        // Bottom Right
        drawLine(accentColor, Offset(left + boxSize, top + boxSize), Offset(left + boxSize - cornerLength, top + boxSize), cornerThickness)
        drawLine(accentColor, Offset(left + boxSize, top + boxSize), Offset(left + boxSize, top + boxSize - cornerLength), cornerThickness)

        // Animated high-tech laser line
        if (isScanningActive) {
            val laserY = top + (boxSize * laserProgress)
            drawLine(
                color = Color(0xFF00E5FF).copy(alpha = 0.9f),
                start = Offset(left + 8.dp.toPx(), laserY),
                end = Offset(left + boxSize - 8.dp.toPx(), laserY),
                strokeWidth = 2.5.dp.toPx()
            )
        }
    }
}
