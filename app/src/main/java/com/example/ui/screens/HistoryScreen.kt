package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TextFields
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ScanResult
import com.example.ui.MainViewModel
import com.example.ui.components.ScanResultSheet
import com.example.ui.theme.*
import com.example.util.OcrUtils
import com.example.util.ScanUtils
import com.google.mlkit.vision.barcode.common.Barcode
import java.text.SimpleDateFormat
import java.util.*

enum class HistoryFilter(val label: String) {
    ALL("All"),
    FAVORITES("Favorites"),
    URLS("Links"),
    WIFI("Wi-Fi"),
    TEXT("Text")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val history by viewModel.history.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }
    var selectedScan by remember { mutableStateOf<ScanResult?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    // Filtered list based on search and selected filter chip
    val filteredHistory = remember(history, searchQuery, selectedFilter) {
        history.filter { scan ->
            val matchesSearch = searchQuery.isBlank() ||
                    scan.content.contains(searchQuery, ignoreCase = true) ||
                    ScanUtils.getTypeName(scan.type, scan.content).contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                HistoryFilter.ALL -> true
                HistoryFilter.FAVORITES -> scan.isFavorite
                HistoryFilter.URLS -> scan.type == Barcode.TYPE_URL || scan.content.startsWith("http://") || scan.content.startsWith("https://")
                HistoryFilter.WIFI -> scan.type == Barcode.TYPE_WIFI || scan.content.startsWith("WIFI:")
                HistoryFilter.TEXT -> scan.type == Barcode.TYPE_TEXT || (!scan.content.startsWith("http") && !scan.content.startsWith("WIFI:"))
            }

            matchesSearch && matchesFilter
        }
    }

    // Chronological grouping: TODAY, YESTERDAY, EARLIER
    val groupedHistory = remember(filteredHistory) {
        filteredHistory.groupBy { scan ->
            val scanCal = Calendar.getInstance().apply { timeInMillis = scan.timestamp }
            val nowCal = Calendar.getInstance()

            when {
                nowCal.get(Calendar.YEAR) == scanCal.get(Calendar.YEAR) &&
                        nowCal.get(Calendar.DAY_OF_YEAR) == scanCal.get(Calendar.DAY_OF_YEAR) -> "TODAY"

                nowCal.get(Calendar.YEAR) == scanCal.get(Calendar.YEAR) &&
                        nowCal.get(Calendar.DAY_OF_YEAR) - scanCal.get(Calendar.DAY_OF_YEAR) == 1 -> "YESTERDAY"

                else -> "EARLIER"
            }
        }
    }

    Scaffold(
        containerColor = VoidCanvas,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Scan Vault",
                            color = TextHighEmphasis,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = (-0.4).sp
                        )
                        if (history.isNotEmpty()) {
                            Text(
                                text = "${filteredHistory.size} of ${history.size} items",
                                color = TextMediumEmphasis,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(onClick = onNavigateBack)
                            .testTag("history_back_button"),
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
                actions = {
                    if (history.isNotEmpty()) {
                        // Export Dialog Launcher (48dp Touch Target)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { showExportDialog = true }
                                .testTag("export_history_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = "Export History",
                                tint = PrimaryCobalt,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Clear All History Button (48dp Touch Target)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { showClearDialog = true }
                                .testTag("clear_history_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear All History",
                                tint = StatusCrimson,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VoidCanvas
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input Field & Filter Chips
            if (history.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("history_search_field"),
                    placeholder = { Text("Search scanned codes...", color = TextLowEmphasis) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextMediumEmphasis, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextMediumEmphasis, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryCobalt,
                        unfocusedBorderColor = HairlineBorder,
                        focusedContainerColor = SurfaceSlate,
                        unfocusedContainerColor = SurfaceSlate,
                        focusedTextColor = TextHighEmphasis,
                        unfocusedTextColor = TextHighEmphasis
                    )
                )

                // Filter Category Chips (Concentric 8dp radius)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HistoryFilter.entries.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .background(
                                    color = if (isSelected) PrimaryCobalt else SurfaceSlate,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) PrimaryCobalt else HairlineBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedFilter = filter },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filter.label,
                                color = if (isSelected) Color.White else TextMediumEmphasis,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // List of Scans or Empty Placeholder
            if (filteredHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(SurfaceSlate, CircleShape)
                                .border(1.dp, HairlineBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = TextLowEmphasis
                            )
                        }
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No Matching Scans" else "Scan Vault Empty",
                            color = TextHighEmphasis,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Try a different search term or reset filter" else "Codes scanned in the viewfinder will be safely stored here with zero cloud telemetry.",
                            color = TextMediumEmphasis,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("TODAY", "YESTERDAY", "EARLIER").forEach { groupKey ->
                        val groupItems = groupedHistory[groupKey]
                        if (!groupItems.isNullOrEmpty()) {
                            item(key = "header_$groupKey") {
                                Text(
                                    text = groupKey,
                                    color = TextMediumEmphasis,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 4.dp)
                                )
                            }

                            items(groupItems, key = { it.id }) { scan ->
                                HistoryCard(
                                    scan = scan,
                                    onToggleFavorite = { viewModel.toggleFavorite(scan) },
                                    onDelete = { viewModel.deleteScan(scan) },
                                    onQuickCopy = {
                                        ScanUtils.copyToClipboard(context, scan.content)
                                    },
                                    onClick = { selectedScan = scan }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail sheet when tapping an item
    selectedScan?.let { scan ->
        ScanResultSheet(
            scanResult = scan,
            onDismiss = { selectedScan = null },
            onToggleFavorite = { viewModel.toggleFavorite(it) }
        )
    }

    // Export Format Selection Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            containerColor = SurfaceElevated,
            title = {
                Text(
                    text = "Export Scan Vault",
                    color = TextHighEmphasis,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Select export format for ${filteredHistory.size} scan records:",
                    color = TextMediumEmphasis
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val csv = ScanUtils.exportHistoryAsCsv(filteredHistory)
                        ScanUtils.shareText(context, csv, "Share Scanned History (CSV)")
                        showExportDialog = false
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryCobalt,
                        contentColor = Color.White
                    )
                ) {
                    Text("Export CSV", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        val json = ScanUtils.exportHistoryAsJson(filteredHistory)
                        ScanUtils.shareText(context, json, "Share Scanned History (JSON)")
                        showExportDialog = false
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextHighEmphasis
                    )
                ) {
                    Text("Export JSON")
                }
            }
        )
    }

    // Confirmation dialog for clearing all history
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = SurfaceElevated,
            title = {
                Text(
                    text = "Clear All Records?",
                    color = TextHighEmphasis,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will permanently delete all ${history.size} saved scan records from your device.",
                    color = TextMediumEmphasis
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearHistory()
                        showClearDialog = false
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusCrimson,
                        contentColor = Color.White
                    )
                ) {
                    Text("Delete All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = TextMediumEmphasis)
                }
            }
        )
    }
}

@Composable
fun HistoryCard(
    scan: ScanResult,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onQuickCopy: () -> Unit,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = remember(scan.timestamp) { dateFormat.format(Date(scan.timestamp)) }
    val typeName = remember(scan.type, scan.content) { ScanUtils.getTypeName(scan.type, scan.content) }

    val isUrl = scan.type == Barcode.TYPE_URL || scan.content.startsWith("http://") || scan.content.startsWith("https://")
    val isWifi = scan.type == Barcode.TYPE_WIFI || scan.content.startsWith("WIFI:")
    val isOcr = scan.format == OcrUtils.FORMAT_OCR_TEXT

    val icon: ImageVector = when {
        isOcr -> Icons.Default.Pin
        isUrl -> Icons.Default.Language
        isWifi -> Icons.Default.Wifi
        else -> Icons.Default.TextFields
    }

    // Solid SurfaceSlate card with 1dp HairlineBorder, concentric 14dp radius
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceSlate, RoundedCornerShape(14.dp))
            .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("history_item_${scan.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon glyph in SurfaceElevated container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(SurfaceElevated, RoundedCornerShape(10.dp))
                    .border(1.dp, HairlineBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryCobalt,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text metadata
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = typeName,
                        color = PrimaryCobalt,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "•",
                        color = TextLowEmphasis,
                        fontSize = 10.sp
                    )
                    Text(
                        text = timeString,
                        color = TextMediumEmphasis,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = scan.content,
                    color = TextHighEmphasis,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // 1-Tap Quick-Copy Button (48dp Touch Target)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onQuickCopy)
                    .testTag("copy_item_${scan.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Quick Copy",
                    tint = TextMediumEmphasis,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Favorite Star Toggle (48dp Touch Target)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onToggleFavorite),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (scan.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Toggle Favorite",
                    tint = if (scan.isFavorite) StatusAmber else TextLowEmphasis,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Delete Item Button (48dp Touch Target)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onDelete)
                    .testTag("delete_item_${scan.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Item",
                    tint = TextLowEmphasis,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
