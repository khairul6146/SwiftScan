package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val format: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val type: Int // Barcode.TYPE_URL, Barcode.TYPE_TEXT, etc.
)
