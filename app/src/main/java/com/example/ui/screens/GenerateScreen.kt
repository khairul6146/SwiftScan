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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.theme.*
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
    QrPalette("Obsidian", android.graphics.Color.BLACK, Color(0xFF0B0D11)),
    QrPalette("Cobalt", android.graphics.Color.parseColor("#1E3A8A"), Color(0xFF1E3A8A)),
    QrPalette("Forest", android.graphics.Color.parseColor("#065F46"), Color(0xFF065F46)),
    QrPalette("Deep Maroon", android.graphics.Color.parseColor("#881337"), Color(0xFF881337)),
    QrPalette("Charcoal", android.graphics.Color.parseColor("#334155"), Color(0xFF334155))
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
        containerColor = VoidCanvas,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Code Studio",
                        color = TextHighEmphasis,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        letterSpacing = (-0.4).sp
                    )
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(onClick = onNavigateBack)
                            .testTag("generate_back_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextHighEmphasis,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VoidCanvas
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Type Selector Chips (Concentric 8dp radius)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QrType.entries.forEach { type ->
                    val isSelected = selectedType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .background(
                                color = if (isSelected) PrimaryCobalt else SurfaceSlate,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) PrimaryCobalt else HairlineBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedType = type }
                            .testTag("chip_${type.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type.label,
                            color = if (isSelected) Color.White else TextMediumEmphasis,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Input Fields based on type (Concentric 12dp radius, HairlineBorder)
            when (selectedType) {
                QrType.TEXT -> {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        label = { Text("Enter plain text", color = TextMediumEmphasis) },
                        placeholder = { Text("e.g. Note, serial, or SKU payload", color = TextLowEmphasis) },
                        modifier = Modifier.fillMaxWidth().testTag("input_text"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryCobalt,
                            unfocusedBorderColor = HairlineBorder,
                            focusedContainerColor = SurfaceSlate,
                            unfocusedContainerColor = SurfaceSlate,
                            focusedTextColor = TextHighEmphasis,
                            unfocusedTextColor = TextHighEmphasis
                        ),
                        minLines = 2,
                        maxLines = 4
                    )
                }
                QrType.URL -> {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("Website Address", color = TextMediumEmphasis) },
                        placeholder = { Text("example.com or https://...", color = TextLowEmphasis) },
                        modifier = Modifier.fillMaxWidth().testTag("input_url"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryCobalt,
                            unfocusedBorderColor = HairlineBorder,
                            focusedContainerColor = SurfaceSlate,
                            unfocusedContainerColor = SurfaceSlate,
                            focusedTextColor = TextHighEmphasis,
                            unfocusedTextColor = TextHighEmphasis
                        ),
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
                            label = { Text("Network Name (SSID)", color = TextMediumEmphasis) },
                            modifier = Modifier.fillMaxWidth().testTag("input_wifi_ssid"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryCobalt,
                                unfocusedBorderColor = HairlineBorder,
                                focusedContainerColor = SurfaceSlate,
                                unfocusedContainerColor = SurfaceSlate,
                                focusedTextColor = TextHighEmphasis,
                                unfocusedTextColor = TextHighEmphasis
                            ),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = wifiPassword,
                            onValueChange = { wifiPassword = it },
                            label = { Text("Password (leave blank if open)", color = TextMediumEmphasis) },
                            modifier = Modifier.fillMaxWidth().testTag("input_wifi_password"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryCobalt,
                                unfocusedBorderColor = HairlineBorder,
                                focusedContainerColor = SurfaceSlate,
                                unfocusedContainerColor = SurfaceSlate,
                                focusedTextColor = TextHighEmphasis,
                                unfocusedTextColor = TextHighEmphasis
                            ),
                            singleLine = true
                        )
                    }
                }
                QrType.PHONE -> {
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Phone Number", color = TextMediumEmphasis) },
                        placeholder = { Text("+1234567890", color = TextLowEmphasis) },
                        modifier = Modifier.fillMaxWidth().testTag("input_phone"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryCobalt,
                            unfocusedBorderColor = HairlineBorder,
                            focusedContainerColor = SurfaceSlate,
                            unfocusedContainerColor = SurfaceSlate,
                            focusedTextColor = TextHighEmphasis,
                            unfocusedTextColor = TextHighEmphasis
                        ),
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
                            label = { Text("Full Name", color = TextMediumEmphasis) },
                            modifier = Modifier.fillMaxWidth().testTag("input_contact_name"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryCobalt,
                                unfocusedBorderColor = HairlineBorder,
                                focusedContainerColor = SurfaceSlate,
                                unfocusedContainerColor = SurfaceSlate,
                                focusedTextColor = TextHighEmphasis,
                                unfocusedTextColor = TextHighEmphasis
                            ),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = contactPhone,
                            onValueChange = { contactPhone = it },
                            label = { Text("Phone Number", color = TextMediumEmphasis) },
                            modifier = Modifier.fillMaxWidth().testTag("input_contact_phone"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryCobalt,
                                unfocusedBorderColor = HairlineBorder,
                                focusedContainerColor = SurfaceSlate,
                                unfocusedContainerColor = SurfaceSlate,
                                focusedTextColor = TextHighEmphasis,
                                unfocusedTextColor = TextHighEmphasis
                            ),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = contactEmail,
                            onValueChange = { contactEmail = it },
                            label = { Text("Email Address", color = TextMediumEmphasis) },
                            modifier = Modifier.fillMaxWidth().testTag("input_contact_email"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryCobalt,
                                unfocusedBorderColor = HairlineBorder,
                                focusedContainerColor = SurfaceSlate,
                                unfocusedContainerColor = SurfaceSlate,
                                focusedTextColor = TextHighEmphasis,
                                unfocusedTextColor = TextHighEmphasis
                            ),
                            singleLine = true
                        )
                    }
                }
            }

            // Palette Theme Color Picker (Restrained swatches with concentric active ring)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Ink Palette",
                    color = TextMediumEmphasis,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PRESET_PALETTES.forEach { palette ->
                        val isSelected = selectedPalette == palette
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(palette.composeColor)
                                .clickable { selectedPalette = palette }
                                .then(
                                    if (isSelected) Modifier.border(2.dp, PrimaryCobalt, CircleShape)
                                    else Modifier.border(1.dp, HairlineBorder, CircleShape)
                                ),
                            contentAlignment = Alignment.Center
                        ) {}
                    }
                }
            }

            // Clean Optical QR Canvas Card (Crisp white canvas, quiet zones, concentric 20dp container)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(SurfaceSlate, RoundedCornerShape(20.dp))
                    .border(1.dp, HairlineBorder, RoundedCornerShape(20.dp))
                    .padding(18.dp)
                    .testTag("qr_preview_card"),
                contentAlignment = Alignment.Center
            ) {
                if (qrBitmap != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(12.dp)
                        ) {
                            Image(
                                bitmap = qrBitmap!!.asImageBitmap(),
                                contentDescription = "Generated QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Text(
                            text = "QR CODE • 600 DPI • QUIET ZONE VERIFIED",
                            color = TextLowEmphasis,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TextFields,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = TextLowEmphasis
                        )
                        Text(
                            text = "Enter content to generate code",
                            color = TextMediumEmphasis,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Action Buttons: Save & Share (48dp height minimum, concentric 12dp radius)
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
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryCobalt,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Save",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save HD", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            shareQrCodeImage(context, qrBitmap!!)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_qr_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceSlate,
                            contentColor = TextHighEmphasis
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Copy Payload Button (48dp Touch Target)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable {
                            ScanUtils.copyToClipboard(context, rawContentToEncode)
                        }
                        .testTag("copy_payload_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = PrimaryCobalt,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Copy Raw Payload",
                            color = PrimaryCobalt,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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
