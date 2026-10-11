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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContactPage
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
    PHONE("Phone"),
    CONTACT("vCard")
}

data class QrPalette(val name: String, val colorInt: Int, val composeColor: Color)

val PRESET_PALETTES = listOf(
    QrPalette("Black", android.graphics.Color.BLACK, Color(0xFF000000)),
    QrPalette("Laser Cyan", android.graphics.Color.parseColor("#00838F"), Color(0xFF00838F)),
    QrPalette("Indigo", android.graphics.Color.parseColor("#3730A3"), Color(0xFF3730A3)),
    QrPalette("Emerald", android.graphics.Color.parseColor("#065F46"), Color(0xFF065F46)),
    QrPalette("Crimson", android.graphics.Color.parseColor("#991B1B"), Color(0xFF991B1B))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    var selectedType by remember { mutableStateOf(QrType.TEXT) }
    var selectedPalette by remember { mutableStateOf(PRESET_PALETTES[0]) }

    var textInput by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }

    // vCard inputs
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }

    val rawContentToEncode = remember(
        selectedType, textInput, urlInput, wifiSsid, wifiPassword,
        phoneInput, contactName, contactPhone, contactEmail
    ) {
        when (selectedType) {
            QrType.TEXT -> textInput
            QrType.URL -> {
                if (urlInput.isNotBlank()) {
                    if (!urlInput.startsWith("http://", ignoreCase = true) &&
                        !urlInput.startsWith("https://", ignoreCase = true)
                    ) {
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
            QrType.CONTACT -> {
                if (contactName.isNotBlank() || contactPhone.isNotBlank()) {
                    "BEGIN:VCARD\nVERSION:3.0\nFN:$contactName\nTEL:$contactPhone\nEMAIL:$contactEmail\nEND:VCARD"
                } else ""
            }
        }
    }

    val qrBitmap by remember(rawContentToEncode, selectedPalette) {
        derivedStateOf {
            if (rawContentToEncode.isNotBlank()) {
                QrGenerator.generateQrCode(
                    text = rawContentToEncode,
                    size = 600,
                    foregroundColor = selectedPalette.colorInt
                )
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
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Type Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QrType.entries.forEach { type ->
                    val isSelected = selectedType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedType = type },
                        label = { Text(type.label, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            val icon = when (type) {
                                QrType.TEXT -> Icons.Default.TextFields
                                QrType.URL -> Icons.Default.Language
                                QrType.WIFI -> Icons.Default.Wifi
                                QrType.PHONE -> Icons.Default.Phone
                                QrType.CONTACT -> Icons.Default.ContactPage
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
                        placeholder = { Text("e.g. Note, message, or asset ID") },
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
                            label = { Text("Password (leave blank if open)") },
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
                QrType.CONTACT -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = contactName,
                            onValueChange = { contactName = it },
                            label = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth().testTag("input_contact_name"),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = contactPhone,
                            onValueChange = { contactPhone = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier.fillMaxWidth().testTag("input_contact_phone"),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = contactEmail,
                            onValueChange = { contactEmail = it },
                            label = { Text("Email Address") },
                            modifier = Modifier.fillMaxWidth().testTag("input_contact_email"),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )
                    }
                }
            }

            // Palette Theme Color Picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "QR Code Color:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PRESET_PALETTES.forEach { palette ->
                        val isSelected = selectedPalette == palette
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(palette.composeColor)
                                .clickable { selectedPalette = palette }
                                .then(
                                    if (isSelected) Modifier.border(2.dp, Color.White, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {}
                    }
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
                                imageVector = Icons.Default.TextFields,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "Preview will appear here",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            // Action Buttons: Save & Share
            if (qrBitmap != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val highRes = QrGenerator.generateQrCode(
                                text = rawContentToEncode,
                                size = 1024,
                                foregroundColor = selectedPalette.colorInt
                            ) ?: qrBitmap!!
                            saveQrCodeToGallery(context, highRes)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_qr_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Save",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save HD")
                    }

                    OutlinedButton(
                        onClick = {
                            shareQrCodeImage(context, qrBitmap!!)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_qr_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }
                }

                // Copy Payload
                TextButton(
                    onClick = {
                        ScanUtils.copyToClipboard(context, rawContentToEncode)
                    },
                    modifier = Modifier.testTag("copy_payload_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Raw Text")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun saveQrCodeToGallery(context: Context, bitmap: Bitmap) {
    try {
        val filename = "SwiftScan_QR_${System.currentTimeMillis()}.png"
        val fos: OutputStream? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SwiftScan")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let { context.contentResolver.openOutputStream(it) }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/SwiftScan"
            val file = File(imagesDir)
            if (!file.exists()) file.mkdirs()
            val image = File(file, filename)
            FileOutputStream(image)
        }

        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            Toast.makeText(context, "Saved HD QR Code to Gallery", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error saving QR Code", Toast.LENGTH_SHORT).show()
    }
}

private fun shareQrCodeImage(context: Context, bitmap: Bitmap) {
    try {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "swiftscan_share.png")
        val fileOutputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
        fileOutputStream.close()

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share QR Code"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to share QR image", Toast.LENGTH_SHORT).show()
    }
}
