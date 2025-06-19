package com.aquaa.matrix.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.aquaa.matrix.Fraction // Assuming Fraction.kt is in com.aquaa.matrix package
import com.google.gson.Gson // For converting complex objects to String for storage
import com.google.gson.reflect.TypeToken

/**
 * Represents a single entry in the matrix calculation history.
 * Each entry stores details about the operation performed, input matrices, and the result.
 */
@Entity(tableName = "matrix_history")
@TypeConverters(MatrixHistoryEntry.Converters::class) // Link to our TypeConverter
data class MatrixHistoryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L, // Unique ID for each history entry

    val timestamp: Long = System.currentTimeMillis(), // When the operation was performed

    val operationType: String, // e.g., "Matrix Addition", "Determinant of a Matrix"

    // Storing matrices as JSON strings since Room can't directly store List<List<Fraction>>
    val matrixAJson: String,
    val matrixBJson: String?, // Nullable for single-matrix operations
    val constantsJson: String?, // Nullable for non-equation operations
    val resultJson: String, // Can be matrix or a single value (e.g., determinant, rank)
    val stepsJson: String // Steps as a JSON string
) {
    // TypeConverters to tell Room how to store and retrieve complex types
    object Converters {
        private val gson = Gson()

        @TypeConverter
        fun fromMatrixList(matrix: List<List<Fraction>>?): String? {
            return if (matrix == null) null else gson.toJson(matrix)
        }

        @TypeConverter
        fun toMatrixList(matrixJson: String?): List<List<Fraction>>? {
            if (matrixJson == null) return null
            val type = object : TypeToken<List<List<Fraction>>>() {}.type
            return gson.fromJson(matrixJson, type)
        }

        @TypeConverter
        fun fromFractionList(list: List<Fraction>?): String? {
            return if (list == null) null else gson.toJson(list)
        }

        @TypeConverter
        fun toFractionList(listJson: String?): List<Fraction>? {
            if (listJson == null) return null
            val type = object : TypeToken<List<Fraction>>() {}.type
            return gson.fromJson(listJson, type)
        }

        @TypeConverter
        fun fromStringList(list: List<String>?): String? {
            return if (list == null) null else gson.toJson(list)
        }

        @TypeConverter
        fun toStringList(listJson: String?): List<String>? {
            if (listJson == null) return null
            val type = object : TypeToken<List<String>>() {}.type
            return gson.fromJson(listJson, type)
        }
    }
}