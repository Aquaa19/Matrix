package com.aquaa.matrix.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * Data Access Object (DAO) for the MatrixHistoryEntry entity.
 * Defines methods for database operations related to calculation history.
 */
@Dao
interface MatrixHistoryDao {

    @Insert
    suspend fun insert(entry: MatrixHistoryEntry) // Suspend function for coroutine support

    @Query("SELECT * FROM matrix_history ORDER BY timestamp DESC")
    suspend fun getAllHistoryEntries(): List<MatrixHistoryEntry> // Suspend function for coroutine support

    @Query("DELETE FROM matrix_history")
    suspend fun deleteAll() // Optional: for clearing history
}