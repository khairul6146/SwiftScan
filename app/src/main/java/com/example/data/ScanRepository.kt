package com.example.data

import kotlinx.coroutines.flow.Flow

class ScanRepository(private val scanDao: ScanDao) {
    val allScans: Flow<List<ScanResult>> = scanDao.getAllScans()
    val favoriteScans: Flow<List<ScanResult>> = scanDao.getFavoriteScans()

    suspend fun insert(scan: ScanResult) {
        scanDao.insertScan(scan)
    }

    suspend fun updateFavorite(id: Long, isFavorite: Boolean) {
        scanDao.updateFavorite(id, isFavorite)
    }

    suspend fun updateNote(id: Long, note: String?) {
        scanDao.updateNote(id, note)
    }

    suspend fun delete(scan: ScanResult) {
        scanDao.deleteScan(scan)
    }

    suspend fun deleteAll() {
        scanDao.deleteAllScans()
    }
}
