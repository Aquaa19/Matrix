package com.aquaa.matrix.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * The Room database for the Matrix Calculator app.
 * Defines the database configuration, including entities and DAOs.
 */
@Database(entities = [MatrixHistoryEntry::class], version = 1, exportSchema = false)
@TypeConverters(MatrixHistoryEntry.Converters::class) // Link the TypeConverters here as well
abstract class AppDatabase : RoomDatabase() {

    // Abstract getter for the DAO
    abstract fun matrixHistoryDao(): MatrixHistoryDao

    companion object {
        // Singleton instance of the database
        @Volatile // Ensures that changes to INSTANCE are immediately visible to other threads
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the singleton instance of the AppDatabase.
         * If the instance is null, it builds a new one.
         */
        fun getDatabase(context: Context): AppDatabase {
            // If INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) { // Synchronized block to prevent multiple threads from creating the database concurrently
                val instance = Room.databaseBuilder(
                    context.applicationContext, // Use application context to prevent memory leaks
                    AppDatabase::class.java,
                    "matrix_calculator_database" // Database name
                )
                    .fallbackToDestructiveMigration() // Destroys and recreates database on version change (for development)
                    .build()
                INSTANCE = instance
                // Return instance
                instance
            }
        }
    }
}