package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ScanRepository
import com.example.data.ScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ScanRepository
    val history: StateFlow<List<ScanResult>>
    val favorites: StateFlow<List<ScanResult>>

    private val _lastScan = MutableStateFlow<ScanResult?>(null)
    val lastScan: StateFlow<ScanResult?> = _lastScan.asStateFlow()

    private var lastScannedContent: String? = null
    private var lastScannedTimestamp: Long = 0L

    init {
        val scanDao = AppDatabase.getDatabase(application).scanDao()
        repository = ScanRepository(scanDao)
        history = repository.allScans.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        favorites = repository.favoriteScans.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    /**
     * Attempts to register a scan result.
     * Throttles identical scans and ignores scans while a result dialog is already open.
     * Returns true if the scan was accepted, false if throttled.
     */
    fun onScanResult(content: String, format: Int, type: Int, ignoreThrottle: Boolean = false): Boolean {
        // If a scan is currently being displayed, do not accept new scans
        if (_lastScan.value != null && !ignoreThrottle) {
            return false
        }

        val now = System.currentTimeMillis()
        if (!ignoreThrottle && content == lastScannedContent && (now - lastScannedTimestamp) < 2000L) {
            return false
        }

        lastScannedContent = content
        lastScannedTimestamp = now

        val scan = ScanResult(content = content, format = format, type = type)
        _lastScan.value = scan
        viewModelScope.launch {
            repository.insert(scan)
        }
        return true
    }

    fun toggleFavorite(scan: ScanResult) {
        viewModelScope.launch {
            val updated = !scan.isFavorite
            repository.updateFavorite(scan.id, updated)
            if (_lastScan.value?.id == scan.id) {
                _lastScan.value = _lastScan.value?.copy(isFavorite = updated)
            }
        }
    }

    fun updateNote(scan: ScanResult, note: String?) {
        viewModelScope.launch {
            repository.updateNote(scan.id, note)
            if (_lastScan.value?.id == scan.id) {
                _lastScan.value = _lastScan.value?.copy(note = note)
            }
        }
    }

    fun deleteScan(scan: ScanResult) {
        viewModelScope.launch {
            repository.delete(scan)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }

    fun clearLastScan() {
        _lastScan.value = null
    }
}
