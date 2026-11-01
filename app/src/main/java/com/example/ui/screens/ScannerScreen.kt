package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.MainViewModel
import com.example.ui.components.LaserReticleHUD
import com.example.ui.components.ScanResultSheet
import com.example.ui.components.TurboActionPill
import com.example.ui.theme.*
import com.example.util.DpmImageProcessor
import com.example.util.OcrUtils
import com.example.util.PayloadCategory
import com.example.util.PayloadParser
import com.example.util.ScanUtils
import com.example.util.SoundUtils
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

enum class ScanMode(val label: String) {
    SINGLE("Single"),
    BATCH("Batch"),
    OCR("OCR")
}

@Composable
fun ScannerScreen(
    viewModel: MainViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToGenerate: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val lastScan by viewModel.lastScan.collectAsState()

    // Mode Settings: Single, Batch, or OCR
    var scanMode by remember { mutableStateOf(ScanMode.SINGLE) }
    val isBatchMode = scanMode == ScanMode.BATCH
    val isOcrMode = scanMode == ScanMode.OCR

    var isAutoCopyActive by remember { mutableStateOf(false) }
    var showDetailedSheet by remember { mutableStateOf(false) }

    var batchCount by remember { mutableIntStateOf(0) }
    var batchBannerText by remember { mutableStateOf<String?>(null) }
    val batchScannedSet = remember { mutableSetOf<String>() }
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

    // Shared OCR Text Recognizer instance
    val textRecognizer = remember {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    // Photo picker for scanning images from gallery
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                if (isOcrMode) {
                    val inputImage = InputImage.fromFilePath(context, uri)
                    textRecognizer.process(inputImage)
                        .addOnSuccessListener { visionText ->
                            val ocrResult = OcrUtils.parseOcrText(visionText.text)
                            val target = ocrResult.primarySerial ?: visionText.text
                            if (target.isNotBlank()) {
                                viewModel.onScanResult(
                                    content = target,
                                    format = OcrUtils.FORMAT_OCR_TEXT,
                                    type = Barcode.TYPE_TEXT
                                )
                                ScanUtils.triggerHapticFeedback(context)
                                if (soundEnabled) SoundUtils.playBeep()
                                if (isAutoCopyActive) {
                                    ScanUtils.copyToClipboard(context, target)
                                }
                            } else {
                                Toast.makeText(context, "No readable text found in image", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to analyze text", Toast.LENGTH_SHORT).show()
                        }
                } else {
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
                                        if (isAutoCopyActive) {
                                            ScanUtils.copyToClipboard(context, content)
                                        }
                                        if (isBatchMode) {
                                            batchScannedSet.add(content)
                                            batchCount++
                                            batchBannerText = "Scanned #$batchCount: $content"
                                            viewModel.clearLastScan()
                                        }
                                    }
                                }
                            } else {
                                Toast.makeText(context, "No barcode found in image", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to analyze image", Toast.LENGTH_SHORT).show()
                        }
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

    if (hasCameraPermission) {
        val cameraController = remember {
            LifecycleCameraController(context).apply {
                setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                isPinchToZoomEnabled = true
                isTapToFocusEnabled = true
            }
        }

        // Camera controller lifecycle binding happens ONCE per lens change
        LaunchedEffect(cameraController, isBackCamera) {
            try {
                cameraController.cameraSelector = if (isBackCamera) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }
                cameraController.bindToLifecycle(lifecycleOwner)
            } catch (_: Exception) {
                Toast.makeText(context, "Selected camera lens unavailable", Toast.LENGTH_SHORT).show()
            }
        }

        // Decoupled OCR vs Barcode Analyzer: No arbitrary delay bottleneck
        LaunchedEffect(cameraController, scanMode) {
            val detectorList = if (scanMode == ScanMode.OCR) {
                listOf(textRecognizer)
            } else {
                listOf(barcodeScanner)
            }

            @Suppress("DEPRECATION")
            cameraController.setImageAnalysisAnalyzer(
                analysisExecutor,
                MlKitAnalyzer(
                    detectorList,
                    CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
                    analysisExecutor
                ) { result ->
                    // In single-mode, pause if result is currently active on screen
                    if (scanMode == ScanMode.SINGLE && viewModel.lastScan.value != null) return@MlKitAnalyzer

                    if (scanMode == ScanMode.OCR) {
                        val visionText = result.getValue(textRecognizer)
                        if (visionText != null && visionText.text.isNotBlank()) {
                            val ocrResult = OcrUtils.parseOcrText(visionText.text)
                            val target = ocrResult.primarySerial ?: ocrResult.displaySummary
                            if (target.isNotBlank()) {
                                val accepted = viewModel.onScanResult(
                                    content = target,
                                    format = OcrUtils.FORMAT_OCR_TEXT,
                                    type = Barcode.TYPE_TEXT
                                )
                                if (accepted) {
                                    ScanUtils.triggerHapticFeedback(context)
                                    if (soundEnabled) SoundUtils.playBeep()
                                    if (isAutoCopyActive) {
                                        ScanUtils.copyToClipboard(context, target)
                                    }
                                }
                            }
                        }
                    } else {
                        val barcodes = result.getValue(barcodeScanner)
                        if (!barcodes.isNullOrEmpty()) {
                            val barcode = barcodes.first()
                            val content = barcode.rawValue
                            if (!content.isNullOrBlank()) {
                                if (isBatchMode && batchScannedSet.contains(content)) {
                                    batchBannerText = "Duplicate ignored: $content"
                                    return@MlKitAnalyzer
                                }

                                val accepted = viewModel.onScanResult(
                                    content = content,
                                    format = barcode.format,
                                    type = barcode.valueType
                                )
                                if (accepted) {
                                    ScanUtils.triggerHapticFeedback(context)
                                    if (soundEnabled) SoundUtils.playBeep()
                                    if (isAutoCopyActive) {
                                        ScanUtils.copyToClipboard(context, content)
                                    }

                                    if (isBatchMode) {
                                        batchScannedSet.add(content)
                                        batchCount++
                                        batchBannerText = "Scanned #$batchCount: $content"
                                        viewModel.clearLastScan()
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }

        val torchState by cameraController.torchState.observeAsState(initial = 0)
        val zoomState by cameraController.zoomState.observeAsState()
        val currentZoomRatio = zoomState?.zoomRatio ?: 1f

        val parsedPayload = remember(lastScan) {
            lastScan?.let { PayloadParser.parse(it) }
        }

        Box(modifier = Modifier.fillMaxSize().background(VoidCanvas)) {
            // Live Camera Viewport
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        this.controller = cameraController
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Precision Optical Reticle HUD (Replacing Slop Animated Overlay)
            LaserReticleHUD(
                isLocked = lastScan != null,
                lockElapsedMs = null,
                isTorchOn = torchState == 1 && isBackCamera,
                isAutoCopyActive = isAutoCopyActive,
                soundEnabled = soundEnabled,
                onToggleTorch = {
                    if (isBackCamera) {
                        try {
                            cameraController.enableTorch(torchState != 1)
                        } catch (_: Exception) {
                            Toast.makeText(context, "Flashlight unavailable", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Flashlight unavailable on front camera", Toast.LENGTH_SHORT).show()
                    }
                },
                onSwitchCamera = {
                    isBackCamera = !isBackCamera
                },
                onToggleAutoCopy = {
                    isAutoCopyActive = !isAutoCopyActive
                    Toast.makeText(context, if (isAutoCopyActive) "Auto-Copy ON" else "Auto-Copy OFF", Toast.LENGTH_SHORT).show()
                },
                onToggleSound = {
                    soundEnabled = !soundEnabled
                    Toast.makeText(context, if (soundEnabled) "Audio ON" else "Audio MUTED", Toast.LENGTH_SHORT).show()
                },
                onPickGallery = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )

            // Status Banner in Batch Mode
            AnimatedVisibility(
                visible = batchBannerText != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 70.dp, start = 16.dp, end = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = StatusEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = batchBannerText ?: "",
                            color = TextHighEmphasis,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Bottom Cluster: Reticle Controls, Turbo Action Pill & Navigation
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Non-Blocking Turbo Action Pill (Concentric 20dp card)
                if (!isBatchMode && parsedPayload != null) {
                    TurboActionPill(
                        payload = parsedPayload,
                        visible = lastScan != null,
                        onPrimaryAction = {
                            when (parsedPayload.category) {
                                PayloadCategory.URL -> {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(parsedPayload.cleanedContent))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        ScanUtils.copyToClipboard(context, parsedPayload.cleanedContent)
                                    }
                                }
                                PayloadCategory.WIFI -> {
                                    ScanUtils.openWifiSettings(context, parsedPayload.rawContent)
                                }
                                PayloadCategory.PRODUCT -> {
                                    try {
                                        val searchUri = Uri.parse("https://www.google.com/search?q=${Uri.encode(parsedPayload.cleanedContent)}")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, searchUri))
                                    } catch (_: Exception) {
                                        ScanUtils.copyToClipboard(context, parsedPayload.cleanedContent)
                                    }
                                }
                                PayloadCategory.CONTACT -> {
                                    try {
                                        val intent = Intent(Intent.ACTION_INSERT).apply {
                                            type = android.provider.ContactsContract.Contacts.CONTENT_TYPE
                                            putExtra(android.provider.ContactsContract.Intents.Insert.NAME, parsedPayload.contactName ?: "")
                                            putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, parsedPayload.contactPhone ?: "")
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        ScanUtils.copyToClipboard(context, parsedPayload.rawContent)
                                    }
                                }
                                PayloadCategory.TEXT -> {
                                    ScanUtils.copyToClipboard(context, parsedPayload.cleanedContent)
                                }
                            }
                        },
                        onSecondaryAction = {
                            when (parsedPayload.category) {
                                PayloadCategory.URL -> ScanUtils.copyToClipboard(context, parsedPayload.cleanedContent)
                                PayloadCategory.WIFI -> ScanUtils.copyToClipboard(context, parsedPayload.wifiPassword ?: parsedPayload.wifiSsid ?: parsedPayload.rawContent)
                                PayloadCategory.PRODUCT -> ScanUtils.copyToClipboard(context, parsedPayload.cleanedContent)
                                PayloadCategory.CONTACT -> {
                                    if (parsedPayload.contactPhone != null) {
                                        try {
                                            val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${parsedPayload.contactPhone}"))
                                            context.startActivity(dial)
                                        } catch (_: Exception) {
                                            ScanUtils.copyToClipboard(context, parsedPayload.contactPhone)
                                        }
                                    } else {
                                        ScanUtils.copyToClipboard(context, parsedPayload.rawContent)
                                    }
                                }
                                PayloadCategory.TEXT -> {
                                    try {
                                        val searchUri = Uri.parse("https://www.google.com/search?q=${Uri.encode(parsedPayload.cleanedContent)}")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, searchUri))
                                    } catch (_: Exception) {
                                        ScanUtils.copyToClipboard(context, parsedPayload.cleanedContent)
                                    }
                                }
                            }
                        },
                        onOpenDetails = {
                            showDetailedSheet = true
                        },
                        onDismiss = {
                            viewModel.clearLastScan()
                        }
                    )
                }

                // Controls row: Zoom Pill (1x, 2x, 3x) & Mode Selector Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Zoom selector pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SurfaceSlate,
                        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
                        modifier = Modifier.testTag("zoom_selector_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(1.0f, 2.0f, 3.0f).forEach { zoomLevel ->
                                val isSelected = (currentZoomRatio - zoomLevel).let { kotlin.math.abs(it) < 0.25f }
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) PrimaryCobalt else Color.Transparent)
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
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else TextMediumEmphasis
                                    )
                                }
                            }
                        }
                    }

                    // 3-Segment Mode Switcher Pill: Single | Batch | OCR
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SurfaceSlate,
                        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
                        modifier = Modifier.testTag("mode_toggle_surface")
                    ) {
                        Row(
                            modifier = Modifier.padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ScanMode.entries.forEach { mode ->
                                val isSelected = scanMode == mode
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) PrimaryCobalt else Color.Transparent)
                                        .clickable {
                                            if (mode != scanMode) {
                                                scanMode = mode
                                                if (mode != ScanMode.BATCH) {
                                                    batchCount = 0
                                                    batchScannedSet.clear()
                                                    batchBannerText = null
                                                }
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (mode == ScanMode.BATCH && batchCount > 0) "Batch ($batchCount)" else mode.label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else TextMediumEmphasis
                                    )
                                }
                            }
                        }
                    }
                }

                // Batch Mode Done Banner
                if (isBatchMode && batchCount > 0) {
                    Button(
                        onClick = onNavigateToHistory,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceSlate,
                            contentColor = TextHighEmphasis
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(48.dp)
                            .testTag("batch_done_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = PrimaryCobalt,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Batch Scans ($batchCount)", fontWeight = FontWeight.SemiBold)
                    }
                }

                // Main Navigation Buttons (History & Create QR)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // History button (48dp height)
                    Button(
                        onClick = onNavigateToHistory,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceSlate,
                            contentColor = TextHighEmphasis
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("history_nav_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = TextMediumEmphasis,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("History", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Create QR Code button (48dp height)
                    Button(
                        onClick = onNavigateToGenerate,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryCobalt,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("generate_nav_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create QR", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else {
        // Camera Permission Fallback
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VoidCanvas)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceSlate),
                border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = PrimaryCobalt
                    )
                    Text(
                        text = "Camera Permission Required",
                        color = TextHighEmphasis,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "SwiftScan uses the camera exclusively to scan QR codes and barcodes in real-time.",
                        color = TextMediumEmphasis,
                        fontSize = 14.sp
                    )
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("grant_permission_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryCobalt,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Grant Permission", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for deep details and note editing
    if (!isBatchMode && showDetailedSheet) {
        lastScan?.let { scan ->
            ScanResultSheet(
                scanResult = scan,
                onDismiss = {
                    showDetailedSheet = false
                },
                onToggleFavorite = {
                    viewModel.toggleFavorite(it)
                }
            )
        }
    }
}
