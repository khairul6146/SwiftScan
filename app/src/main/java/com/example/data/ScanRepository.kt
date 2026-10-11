package com.example.data

import kotlinx.coroutines.flow.Flow

class ScanRepository(private val scanDao: ScanDao) {
    val allScans: Flow<List<ScanResult>> = scanDao.getAllScans()

    suspend fun insert(scan: ScanResult) {
        scanDao.insertScan(scan)
    }

    suspend fun delete(scan: ScanResult) {
        scanDao.deleteScan(scan)
    }

    suspend fun deleteAll() {
        scanDao.deleteAllScans()
    }
}
