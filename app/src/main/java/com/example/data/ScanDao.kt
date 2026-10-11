package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanResult>>

    @Query("SELECT * FROM scan_history WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteScans(): Flow<List<ScanResult>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanResult)

    @Query("UPDATE scan_history SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE scan_history SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String?)

    @Delete
    suspend fun deleteScan(scan: ScanResult)

    @Query("DELETE FROM scan_history")
    suspend fun deleteAllScans()
}
