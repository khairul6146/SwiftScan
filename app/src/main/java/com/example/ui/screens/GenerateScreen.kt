package com.example.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.util.QrGenerator
import com.example.util.ScanUtils
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

enum class QrType(val label: String) {
    TEXT("Text"),
    URL("Website"),
    WIFI("Wi-Fi"),
    PHONE("Phone")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    var selectedType by remember { mutableStateOf(QrType.TEXT) }
    var textInput by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }

    val rawContentToEncode = remember(selectedType, textInput, urlInput, wifiSsid, wifiPassword, phoneInput) {
        when (selectedType) {
            QrType.TEXT -> textInput
            QrType.URL -> {
                if (urlInput.isNotBlank()) {
                    if (!urlInput.startsWith("http://") && !urlInput.startsWith("https://")) {
                        "https://$urlInput"
                    } else urlInput
                } else ""
            }
            QrType.WIFI -> {
                if (wifiSsid.isNotBlank()) {
                    "WIFI:S:$wifiSsid;T:WPA;P:$wifiPassword;;"
                } else ""
            }
            QrType.PHONE -> {
                if (phoneInput.isNotBlank()) "tel:$phoneInput" else ""
            }
        }
    }

    val qrBitmap by remember(rawContentToEncode) {
        derivedStateOf {
            if (rawContentToEncode.isNotBlank()) {
                QrGenerator.generateQrCode(rawContentToEncode, 600)
            } else null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generate QR Code", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("generate_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Type Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QrType.entries.forEach { type ->
                    val isSelected = selectedType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedType = type },
                        label = { Text(type.label) },
                        leadingIcon = {
                            val icon = when (type) {
                                QrType.TEXT -> Icons.Default.TextFields
                                QrType.URL -> Icons.Default.Language
                                QrType.WIFI -> Icons.Default.Wifi
                                QrType.PHONE -> Icons.Default.Phone
                            }
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("chip_${type.name.lowercase()}")
                    )
                }
            }

            // Input Fields based on type
            when (selectedType) {
                QrType.TEXT -> {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        label = { Text("Enter plain text") },
                        placeholder = { Text("e.g. Note, message, or ID") },
                        modifier = Modifier.fillMaxWidth().testTag("input_text"),
                        shape = RoundedCornerShape(14.dp),
                        minLines = 2,
                        maxLines = 4
                    )
                }
                QrType.URL -> {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("Website Address") },
                        placeholder = { Text("example.com or https://...") },
                        modifier = Modifier.fillMaxWidth().testTag("input_url"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                }
                QrType.WIFI -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = wifiSsid,
                            onValueChange = { wifiSsid = it },
                            label = { Text("Network Name (SSID)") },
                            modifier = Modifier.fillMaxWidth().testTag("input_wifi_ssid"),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = wifiPassword,
                            onValueChange = { wifiPassword = it },
                            label = { Text("Password (optional)") },
                            modifier = Modifier.fillMaxWidth().testTag("input_wifi_password"),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )
                    }
                }
                QrType.PHONE -> {
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("+1234567890") },
                        modifier = Modifier.fillMaxWidth().testTag("input_phone"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                }
            }

            // QR Code Preview Area
            Card(
                modifier = Modifier
                    .size(240.dp)
                    .testTag("qr_preview_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(8.dp)
                        ) {
                            Image(
                                bitmap = qrBitmap!!.asImageBitmap(),
                                contentDescription = "Generated QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "Enter details above to preview QR code",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Action Buttons
            if (qrBitmap != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Copy text button
                    OutlinedButton(
                        onClick = {
                            ScanUtils.copyToClipboard(context, rawContentToEncode)
                        },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("copy_generated_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy")
                    }

                    // Share button
                    Button(
                        onClick = {
                            shareGeneratedQr(context, qrBitmap!!, rawContentToEncode)
                        },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("share_generated_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share")
                    }

                    // Save to Gallery button
                    FilledTonalButton(
                        onClick = {
                            saveBitmapToGallery(context, qrBitmap!!)
                        },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("save_generated_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Saves generated QR code bitmap to Android MediaStore Pictures directory.
 */
private fun saveBitmapToGallery(context: Context, bitmap: Bitmap) {
    try {
        val filename = "QRCode_${System.currentTimeMillis()}.png"
        var outputStream: OutputStream? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SwiftScan")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                outputStream = context.contentResolver.openOutputStream(uri)
            }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
            val image = File(imagesDir, filename)
            outputStream = FileOutputStream(image)
        }

        outputStream?.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            Toast.makeText(context, "QR code saved to Pictures!", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(context, "Failed to save QR code", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Could not save image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Shares the generated QR code image file via Android system share sheet.
 */
private fun shareGeneratedQr(context: Context, bitmap: Bitmap, content: String) {
    try {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "qr_code.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_TEXT, content)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share QR Code"))
    } catch (_: Exception) {
        // Fallback to text sharing if FileProvider fails
        ScanUtils.shareText(context, content)
    }
}
