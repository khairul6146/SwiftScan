package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ScanResult
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.LaserCyan
import com.example.ui.theme.SafeEmerald
import com.example.util.OcrUtils
import com.example.util.ScanUtils
import com.example.util.SecurityUtils
import com.google.mlkit.vision.barcode.common.Barcode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultSheet(
    scanResult: ScanResult,
    onDismiss: () -> Unit,
    onToggleFavorite: ((ScanResult) -> Unit)? = null
) {
    val context = LocalContext.current
    val formatName = ScanUtils.getFormatName(scanResult.format)
    val typeName = ScanUtils.getTypeName(scanResult.type, scanResult.content)

    val isOcr = scanResult.format == OcrUtils.FORMAT_OCR_TEXT
    val ocrResult = remember(scanResult.content) {
        if (isOcr) OcrUtils.parseOcrText(scanResult.content) else null
    }

    val isUrl = !isOcr && (scanResult.type == Barcode.TYPE_URL ||
            scanResult.content.startsWith("http://", ignoreCase = true) ||
            scanResult.content.startsWith("https://", ignoreCase = true))
    val isWifi = !isOcr && (scanResult.type == Barcode.TYPE_WIFI ||
            scanResult.content.startsWith("wifi:", ignoreCase = true))
    val isPhone = !isOcr && (scanResult.type == Barcode.TYPE_PHONE || scanResult.content.startsWith("tel:"))
    val isEmail = !isOcr && (scanResult.type == Barcode.TYPE_EMAIL || scanResult.content.startsWith("mailto:"))

    // Security Analysis for URLs
    val securityAnalysis = remember(scanResult.content) {
        if (isUrl) SecurityUtils.analyzeUrl(scanResult.content) else null
    }

    var stripTrackers by remember { mutableStateOf(securityAnalysis?.hasTrackers == true) }

    val icon: ImageVector = when {
        isOcr -> Icons.Default.Pin
        isUrl -> Icons.Default.Language
        isWifi -> Icons.Default.Wifi
        isPhone -> Icons.Default.Call
        isEmail -> Icons.Default.Email
        else -> Icons.Default.TextFields
    }

    val finalTargetUrl = remember(scanResult.content, stripTrackers, securityAnalysis) {
        if (isUrl && securityAnalysis != null) {
            if (stripTrackers) securityAnalysis.cleanedUrl else securityAnalysis.originalUrl
        } else {
            scanResult.content
        }
    }

    val primaryActionLabel = when {
        isOcr -> if (ocrResult?.primarySerial != null) "Copy Serial (${ocrResult.primarySerial})" else "Copy OCR Text"
        isUrl -> if (stripTrackers) "Open Cleaned Link" else "Open in Browser"
        isWifi -> "Connect to Wi-Fi"
        isPhone -> "Call Number"
        isEmail -> "Send Email"
        else -> "Copy Text"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Icon, Type Badge & Favorite Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = typeName,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isOcr) "Alphanumeric Serial" else typeName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onToggleFavorite != null) {
                        IconButton(
                            onClick = { onToggleFavorite(scanResult) }
                        ) {
                            Icon(
                                imageVector = if (scanResult.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Toggle Favorite",
                                tint = if (scanResult.isFavorite) AlertCrimson else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = if (isOcr) "OCR" else "0ms Local",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // OCR SERIAL SPECIFIC CARD
            if (isOcr && ocrResult != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Extracted Alphanumeric Serial",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = CircleShape,
                                color = SafeEmerald
                            ) {
                                Text(
                                    text = "OCR ACTIVE",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        if (ocrResult.primarySerial != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = ocrResult.primarySerial,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // SECURITY SHIELD CARD (For URLs)
            if (isUrl && securityAnalysis != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (securityAnalysis.riskLevel) {
                            SecurityUtils.RiskLevel.SAFE -> SafeEmerald.copy(alpha = 0.12f)
                            SecurityUtils.RiskLevel.CAUTION -> CautionAmber.copy(alpha = 0.14f)
                            SecurityUtils.RiskLevel.DANGEROUS -> AlertCrimson.copy(alpha = 0.14f)
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (securityAnalysis.riskLevel == SecurityUtils.RiskLevel.SAFE)
                                        Icons.Default.Security else Icons.Default.Warning,
                                    contentDescription = "Security Status",
                                    tint = when (securityAnalysis.riskLevel) {
                                        SecurityUtils.RiskLevel.SAFE -> SafeEmerald
                                        SecurityUtils.RiskLevel.CAUTION -> CautionAmber
                                        SecurityUtils.RiskLevel.DANGEROUS -> AlertCrimson
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Target Domain: ${securityAnalysis.rootDomain}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = when (securityAnalysis.riskLevel) {
                                    SecurityUtils.RiskLevel.SAFE -> SafeEmerald
                                    SecurityUtils.RiskLevel.CAUTION -> CautionAmber
                                    SecurityUtils.RiskLevel.DANGEROUS -> AlertCrimson
                                }
                            ) {
                                Text(
                                    text = securityAnalysis.riskLevel.name,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = securityAnalysis.riskReason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Privacy tracker scrub switch
                        if (securityAnalysis.hasTrackers) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Strip tracking tags (utm, fbclid)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Switch(
                                    checked = stripTrackers,
                                    onCheckedChange = { stripTrackers = it },
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // WI-FI SPECIFIC CARD
            if (isWifi) {
                val wifiConfig = remember(scanResult.content) { ScanUtils.parseWifiString(scanResult.content) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = LaserCyan.copy(alpha = 0.12f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Network SSID",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = wifiConfig.ssid.ifBlank { "Hidden / Unspecified" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Password: ${if (wifiConfig.password.isNotBlank()) wifiConfig.password else "(Open Network - No Password)"}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Raw Scanned Content Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scan_result_content_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                SelectionContainer {
                    Text(
                        text = if (isOcr && ocrResult?.primarySerial != null) ocrResult.fullText else finalTargetUrl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Primary Action Button
            Button(
                onClick = {
                    if (isOcr) {
                        val target = ocrResult?.primarySerial ?: scanResult.content
                        ScanUtils.copyToClipboard(context, target, "OCR Serial")
                    } else if (isWifi) {
                        ScanUtils.openWifiSettings(context, scanResult.content)
                    } else {
                        ScanUtils.openAction(context, finalTargetUrl, scanResult.type)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("primary_action_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = if (isUrl) Icons.AutoMirrored.Filled.ArrowForward
                    else if (isWifi) Icons.Default.Wifi
                    else Icons.Default.ContentCopy,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = primaryActionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Actions Row: Search, Copy and Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isOcr && ocrResult?.primarySerial != null) {
                    OutlinedButton(
                        onClick = {
                            val searchUrl = "https://www.google.com/search?q=" + Uri.encode(ocrResult.primarySerial)
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Serial",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Search")
                    }
                }

                OutlinedButton(
                    onClick = {
                        val textToCopy = if (isOcr && ocrResult?.primarySerial != null) ocrResult.primarySerial else finalTargetUrl
                        ScanUtils.copyToClipboard(context, textToCopy)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("copy_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy")
                }

                OutlinedButton(
                    onClick = {
                        val textToShare = if (isOcr && ocrResult?.primarySerial != null) ocrResult.primarySerial else finalTargetUrl
                        ScanUtils.shareText(context, textToShare)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("share_button"),
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

            Spacer(modifier = Modifier.height(10.dp))

            // Dismiss / Scan Next button
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("scan_again_button")
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scan Next Code")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
