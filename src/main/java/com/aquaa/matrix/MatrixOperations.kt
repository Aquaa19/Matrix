package com.aquaa.matrix

import kotlin.math.abs

/**
 * Contains core logic for matrix operations, now using custom Fraction class for precision.
 */
object MatrixOperations {

    // Epsilon for comparing Doubles to zero, only used in `Fraction`'s `toDouble` conversion logic.
    // For Fraction comparisons, we check if numerator is zero.
    private const val EPSILON_DOUBLE = 1e-9

    /**
     * Formats a matrix for display in a TextView, using Fraction.toString().
     */
    fun formatMatrixForDisplay(matrix: List<List<Fraction>>, message: String = "Matrix:"): String {
        if (matrix.isEmpty() || matrix[0].isEmpty()) {
            return "$message\n[Empty Matrix]\n"
        }

        val maxColumnWidths = mutableListOf<Int>()
        // Initialize maxColumnWidths with zeros based on the number of columns
        repeat(matrix[0].size) { maxColumnWidths.add(0) }

        // First pass to determine max width for each column
        for (row in matrix) {
            for (j in row.indices) {
                val formattedVal = row[j].toString()
                if (formattedVal.length > maxColumnWidths[j]) {
                    maxColumnWidths[j] = formattedVal.length
                }
            }
        }

        val sb = StringBuilder()
        sb.appendLine("\n$message")
        for (row in matrix) {
            val formattedRow = row.indices.joinToString(" ") { j ->
                val formattedVal = row[j].toString()
                formattedVal.padStart(maxColumnWidths[j])
            }
            sb.appendLine("[$formattedRow]")
        }
        sb.appendLine("-".repeat(maxColumnWidths.sum() + matrix[0].size * 2)) // Separator line
        return sb.toString()
    }

    /**
     * Parses a string input into a matrix (List of List of Fraction).
     * Handles newlines for rows and spaces for columns.
     * Now supports direct fraction input (e.g., "1/2").
     * Returns a Pair of (Matrix, String of errors), or null if parsing fails entirely.
     */
    fun parseMatrixInput(input: String, name: String): Pair<List<List<Fraction>>?, String?> {
        val rows = input.trim().split("\n").filter { it.isNotBlank() }
        if (rows.isEmpty()) {
            return Pair(null, "Error: No data entered for $name. Please enter matrix elements.")
        }

        val matrix = mutableListOf<MutableList<Fraction>>()
        var expectedCols = -1

        for ((i, rowStr) in rows.withIndex()) {
            val elements = rowStr.trim().split(" ").filter { it.isNotBlank() }
            if (elements.isEmpty()) {
                return Pair(null, "Error: Row ${i + 1} of $name is empty. Please ensure all rows have elements.")
            }

            if (expectedCols == -1) {
                expectedCols = elements.size
            } else if (elements.size != expectedCols) {
                return Pair(null, "Error: Row ${i + 1} of $name has ${elements.size} elements, but expected $expectedCols. Ensure all rows have the same number of columns.")
            }

            val parsedRow = mutableListOf<Fraction>()
            for (elem in elements) {
                try {
                    parsedRow.add(Fraction.parse(elem)) // Use the new Fraction.parse function
                } catch (e: NumberFormatException) {
                    return Pair(null, "Error: Invalid number format in $name, row ${i + 1} element '$elem'. Please enter valid numbers or fractions (e.g., 1/2).")
                } catch (e: IllegalArgumentException) {
                    return Pair(null, "Error: Invalid fraction format in $name, row ${i + 1} element '$elem'. ${e.message}")
                }
            }
            matrix.add(parsedRow)
        }
        return Pair(matrix, null)
    }

    /**
     * Calculates the transpose of a matrix with detailed step-by-step display.
     */
    fun transposeMatrix(matrix: List<List<Fraction>>, steps: MutableList<String>): List<List<Fraction>> {
        steps.add("--- Transpose Matrix Operation ---")
        steps.add(formatMatrixForDisplay(matrix, "Original Matrix:"))

        val rows = matrix.size
        val cols = matrix[0].size
        val transposed = List(cols) { MutableList(rows) { Fraction.ZERO } }

        steps.add("\nStep-by-step Transposition Process:")
        steps.add("The element at row i, column j in the original matrix (A[i][j])")
        steps.add("becomes the element at row j, column i in the transposed matrix (A_T[j][i]).")

        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val originalVal = matrix[i][j]
                transposed[j][i] = originalVal
                steps.add("  - Moving element A[${i + 1}][${j + 1}] = ${originalVal} to A_T[${j + 1}][${i + 1}]")
            }
        }

        steps.add("\nAll elements have been transposed.")
        return transposed
    }

    /**
     * Adds two matrices with detailed step-by-step display.
     */
    fun addMatrices(matrixA: List<List<Fraction>>, matrixB: List<List<Fraction>>, steps: MutableList<String>): List<List<Fraction>>? {
        steps.add("--- Matrix Addition Operation ---")
        val rowsA = matrixA.size
        val colsA = matrixA[0].size
        val rowsB = matrixB.size
        val colsB = matrixB[0].size

        if (rowsA != rowsB || colsA != colsB) {
            steps.add("Error: Matrices must have the same dimensions for addition.")
            steps.add("Matrix A dimensions: ${rowsA}x${colsA}")
            steps.add("Matrix B dimensions: ${rowsB}x${colsB}")
            return null
        }

        val result = List(rowsA) { MutableList(colsA) { Fraction.ZERO } }
        steps.add(formatMatrixForDisplay(matrixA, "Matrix A for Addition:"))
        steps.add(formatMatrixForDisplay(matrixB, "Matrix B for Addition:"))

        steps.add("\nStep-by-step Addition Process:")
        steps.add("Each element of the resulting matrix (C[i][j]) is the sum of the corresponding")
        steps.add("elements from Matrix A (A[i][j]) and Matrix B (B[i][j]). That is, C[i][j] = A[i][j] + B[i][j].")

        for (i in 0 until rowsA) {
            for (j in 0 until colsA) {
                val valA = matrixA[i][j]
                val valB = matrixB[i][j]
                val sumVal = valA + valB
                result[i][j] = sumVal
                steps.add("  - Calculating C[${i + 1}][${j + 1}]: ${valA} + ${valB} = ${sumVal}")
            }
        }

        steps.add("\nAll elements have been added.")
        return result
    }

    /**
     * Subtracts two matrices (A - B) with detailed step-by-step display.
     */
    fun subtractMatrices(matrixA: List<List<Fraction>>, matrixB: List<List<Fraction>>, steps: MutableList<String>): List<List<Fraction>>? {
        steps.add("--- Matrix Subtraction Operation ---")
        val rowsA = matrixA.size
        val colsA = matrixA[0].size
        val rowsB = matrixB.size
        val colsB = matrixB[0].size

        if (rowsA != rowsB || colsA != colsB) {
            steps.add("Error: Matrices must have the same dimensions for subtraction.")
            steps.add("Matrix A dimensions: ${rowsA}x${colsA}")
            steps.add("Matrix B dimensions: ${rowsB}x${colsB}")
            return null
        }

        val result = List(rowsA) { MutableList(colsA) { Fraction.ZERO } }
        steps.add(formatMatrixForDisplay(matrixA, "Matrix A for Subtraction:"))
        steps.add(formatMatrixForDisplay(matrixB, "Matrix B for Subtraction:"))

        steps.add("\nStep-by-step Subtraction Process:")
        steps.add("Each element of the resulting matrix (C[i][j]) is the difference between the corresponding")
        steps.add("elements from Matrix A (A[i][j]) and Matrix B (B[i][j]). That is, C[i][j] = A[i][j] - B[i][j].")

        for (i in 0 until rowsA) {
            for (j in 0 until colsA) {
                val valA = matrixA[i][j]
                val valB = matrixB[i][j]
                val diffVal = valA - valB
                result[i][j] = diffVal
                steps.add("  - Calculating C[${i + 1}][${j + 1}]: ${valA} - ${valB} = ${diffVal}")
            }
        }

        steps.add("\nAll elements have been subtracted.")
        return result
    }

    /**
     * Multiplies two matrices with detailed step-by-step display.
     */
    fun multiplyMatrices(matrixA: List<List<Fraction>>, matrixB: List<List<Fraction>>, steps: MutableList<String>): List<List<Fraction>>? {
        steps.add("--- Matrix Multiplication Operation ---")
        val rowsA = matrixA.size
        val colsA = matrixA[0].size
        val rowsB = matrixB.size
        val colsB = matrixB[0].size

        if (colsA != rowsB) {
            steps.add("Error: Number of columns in the first matrix must equal the number of rows in the second matrix for multiplication.")
            steps.add("Matrix A dimensions: ${rowsA}x${colsA}")
            steps.add("Matrix B dimensions: ${rowsB}x${colsB}")
            return null
        }

        val result = List(rowsA) { MutableList(colsB) { Fraction.ZERO } }
        steps.add(formatMatrixForDisplay(matrixA, "Matrix A for Multiplication:"))
        steps.add(formatMatrixForDisplay(matrixB, "Matrix B for Multiplication:"))

        steps.add("\nStep-by-step Multiplication Process:")
        steps.add("Each element C[i][j] of the resulting matrix is computed by taking the dot product")
        steps.add("of the i-th row of Matrix A and the j-th column of Matrix B.")
        steps.add("C[i][j] = Sum(A[i][k] * B[k][j]) for k from 1 to number of columns in A (or rows in B).")

        for (i in 0 until rowsA) {
            for (j in 0 until colsB) {
                var currentSum = Fraction.ZERO
                val calculationStr = mutableListOf<String>()
                for (k in 0 until colsA) { // This is equal to rowsB
                    val valA = matrixA[i][k]
                    val valB = matrixB[k][j]
                    val product = valA * valB
                    currentSum += product
                    calculationStr.add("(${valA} * ${valB})")
                }
                result[i][j] = currentSum
                steps.add("  - Calculating C[${i + 1}][${j + 1}]: ${calculationStr.joinToString(" + ")} = ${currentSum}")
            }
        }

        steps.add("\nAll elements have been calculated.")
        return result
    }

    /**
     * Returns the sub-matrix (minor) used for calculating determinants and cofactors.
     * This is a helper function.
     */
    private fun getCofactor(matrix: List<List<Fraction>>, p: Int, q: Int): List<List<Fraction>> {
        val minor = mutableListOf<MutableList<Fraction>>()
        for (i in matrix.indices) {
            if (i == p) continue
            val row = mutableListOf<Fraction>()
            for (j in matrix[i].indices) {
                if (j == q) continue
                row.add(matrix[i][j])
            }
            if (row.isNotEmpty()) { // Only add if row is not empty after removing column
                minor.add(row)
            }
        }
        return minor
    }

    /**
     * Calculates the determinant of a square matrix recursively, with detailed step-by-step display.
     * Adds a 'level' parameter to indent steps for nested determinant calculations.
     */
    fun determinant(matrix: List<List<Fraction>>, steps: MutableList<String>, level: Int = 0): Fraction {
        val n = matrix.size
        val indent = "  ".repeat(level)

        if (n == 1) {
            steps.add("${indent}  Determinant of a 1x1 matrix is simply the element: ${matrix[0][0]}")
            return matrix[0][0]
        }
        if (n == 2) {
            val det = matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0]
            steps.add("${indent}  Calculating determinant for 2x2 sub-matrix:")
            steps.add("${indent}  |${matrix[0][0].toString().padStart(5)} ${matrix[0][1].toString().padStart(5)}|")
            steps.add("${indent}  |${matrix[1][0].toString().padStart(5)} ${matrix[1][1].toString().padStart(5)}|")
            steps.add("${indent}  = (${matrix[0][0]} * ${matrix[1][1]}) - (${matrix[0][1]} * ${matrix[1][0]})")
            steps.add("${indent}  = ${det}")
            return det
        }

        var detVal = Fraction.ZERO
        steps.add("${indent}Expanding determinant along the first row:")
        for (c in 0 until n) {
            val cofactorMatrix = getCofactor(matrix, 0, c)

            steps.add("${indent}  For element A[1][${c + 1}] = ${matrix[0][c]}:")
            steps.add("${indent}    Creating minor matrix by removing row 1 and column ${c + 1}:")

            // Print minor matrix for clarity
            for (rMinor in cofactorMatrix) {
                val minorStr = rMinor.joinToString(" ") { it.toString().padStart(5) }
                steps.add("${indent}    [$minorStr]")
            }

            val cofDet = determinant(cofactorMatrix, steps, level + 1) // Recursive call

            val sign = if (c % 2 == 0) Fraction.ONE else -Fraction.ONE // (-1)^c
            val term = sign * matrix[0][c] * cofDet

            steps.add("${indent}    Cofactor C[1][${c + 1}] = ((-1)^${c}) * det(Minor) = ${sign} * ${cofDet} = ${sign * cofDet}")
            steps.add("${indent}    Term for A[1][${c + 1}] = ${matrix[0][c]} * ${sign * cofDet} = ${term}")
            detVal += term
        }
        steps.add("${indent}Summing all terms: ${detVal}")
        return detVal
    }

    /**
     * Calculates the adjoint of a square matrix with detailed step-by-step display.
     */
    fun adjointMatrix(matrix: List<List<Fraction>>, steps: MutableList<String>): List<List<Fraction>>? {
        steps.add("--- Adjoint Matrix Operation ---")
        val n = matrix.size
        if (matrix[0].size != n) {
            steps.add("Error: Adjoint is defined only for square matrices.")
            return null
        }

        val adj = List(n) { MutableList(n) { Fraction.ZERO } }
        steps.add(formatMatrixForDisplay(matrix, "Original Matrix for Adjoint:"))

        steps.add("\nStep-by-step Adjoint Calculation:")
        steps.add("The adjoint matrix is the transpose of the cofactor matrix.")
        steps.add("First, we calculate the cofactor C[i][j] for each element M[i][j] of the original matrix.")
        steps.add("C[i][j] = (-1)^(i+j) * det(M_ij), where M_ij is the minor matrix.")

        for (i in 0 until n) {
            for (j in 0 until n) {
                steps.add("\n  -- Calculating Cofactor for element A[${i + 1}][${j + 1}] = ${matrix[i][j]} --")
                val cofactorMatrix = getCofactor(matrix, i, j)
                steps.add(formatMatrixForDisplay(cofactorMatrix, "    Minor M[${i + 1}][${j + 1}] (sub-matrix after removing row ${i + 1} and col ${j + 1}):"))

                val cofDetSteps = mutableListOf<String>()
                val cofDet = determinant(cofactorMatrix, cofDetSteps, level = 1) // Nested determinant calculation
                steps.addAll(cofDetSteps.map { "    $it" }) // Indent nested steps

                val signFactor = if ((i + j) % 2 == 0) Fraction.ONE else -Fraction.ONE
                val cofactorVal = signFactor * cofDet
                adj[j][i] = cofactorVal // Assign to transposed position for adjoint

                steps.add("    The sign factor for C[${i + 1}][${j + 1}] is (-1)^(${i}+${j}) = ${signFactor}")
                steps.add("    Cofactor C[${i + 1}][${j + 1}] = ${signFactor} * ${cofDet} = ${cofactorVal}")
                steps.add("    This value becomes the element at Adj[${j + 1}][${i + 1}] (transposed position in adjoint matrix).")
            }
        }

        steps.add("\nAll cofactors calculated and transposed to form the adjoint matrix.")
        return adj
    }

    /**
     * Calculates the inverse of a square matrix using the adjoint method, with detailed step-by-step display.
     */
    fun inverseMatrixAdjointMethod(matrix: List<List<Fraction>>, steps: MutableList<String>): List<List<Fraction>>? {
        steps.add("--- Inverse Matrix Operation (Adjoint Method) ---")
        val n = matrix.size
        if (matrix[0].size != n) {
            steps.add("Error: Inverse is defined only for square matrices.")
            return null
        }

        steps.add(formatMatrixForDisplay(matrix, "Original Matrix for Inverse:"))

        steps.add("\nStep 1: Calculate the Determinant of the matrix.")
        val detSteps = mutableListOf<String>()
        val detVal = determinant(matrix, detSteps)
        steps.addAll(detSteps)

        steps.add("\nFinal Determinant (det(A)): ${detVal}")

        if (detVal.isZero()) {
            steps.add("Error: Determinant is zero (or very close to zero), so the inverse does not exist.")
            steps.add("A singular matrix does not have an inverse.")
            return null
        }

        steps.add("\nStep 2: Calculate the Adjoint of the matrix.")
        val adjMatrixVal = adjointMatrix(matrix, steps)
        if (adjMatrixVal == null) {
            steps.add("Error: Could not calculate adjoint matrix.")
            return null
        }
        steps.add("\nAdjoint Matrix (adj(A)):")
        steps.add(formatMatrixForDisplay(adjMatrixVal, ""))

        steps.add("\nStep 3: Calculate the Inverse using the formula: A^-1 = (1 / det(A)) * adj(A).")
        val inverse = List(n) { MutableList(n) { Fraction.ZERO } }
        val scalarOneOverDet = Fraction.ONE / detVal
        steps.add("  Scalar factor (1/det(A)) = ${Fraction.ONE} / ${detVal} = ${scalarOneOverDet}")

        for (i in 0 until n) {
            for (j in 0 until n) {
                val adjElement = adjMatrixVal[i][j]
                val invElement = adjElement * scalarOneOverDet
                inverse[i][j] = invElement
                steps.add("  - Inverse[${i + 1}][${j + 1}] = ${scalarOneOverDet} * ${adjElement} = ${invElement}")
            }
        }

        steps.add("\nInverse matrix calculation complete.")
        return inverse
    }

    /**
     * Performs elementary row operations on a matrix with detailed step-by-step display.
     * This function is adapted to be called with explicit operation parameters from the UI.
     * It returns the modified matrix.
     */
    fun performElementaryRowOperation(
        matrix: List<List<Fraction>>,
        operationType: String, // "swap", "multiply", "add_multiple"
        row1: Int, // 1-indexed
        row2: Int = -1, // 1-indexed, for swap/add_multiple
        scalar: Fraction = Fraction.ZERO, // For multiply/add_multiple
        steps: MutableList<String>
    ): List<List<Fraction>>? {
        val mat = matrix.map { it.toMutableList() }.toMutableList() // Create a deep copy
        val rows = mat.size
        val cols = mat[0].size

        steps.add("\n--- Performing Elementary Row Operation ---")
        steps.add(formatMatrixForDisplay(matrix, "Starting Matrix:"))

        when (operationType) {
            "swap" -> {
                val r1Idx = row1 - 1
                val r2Idx = row2 - 1
                if (!(r1Idx in 0 until rows && r2Idx in 0 until rows)) {
                    steps.add("Error: Invalid row numbers. Please ensure they are within matrix bounds.")
                    return null
                }
                steps.add("Performing: R${row1} <-> R${row2}")
                mat[r1Idx] = mat[r2Idx].also { mat[r2Idx] = mat[r1Idx] }
                steps.add(formatMatrixForDisplay(mat, "Matrix after R${row1} <-> R${row2}:"))
            }
            "multiply" -> {
                val rIdx = row1 - 1
                if (!(rIdx in 0 until rows)) {
                    steps.add("Error: Invalid row number. Please ensure it is within matrix bounds.")
                    return null
                }
                if (scalar.isZero()) {
                    steps.add("Error: Scalar cannot be zero for this operation (would zero out the row).")
                    return null
                }
                steps.add("Performing: ${scalar} * R${row1}")
                for (j in 0 until cols) {
                    mat[rIdx][j] *= scalar
                }
                steps.add(formatMatrixForDisplay(mat, "Matrix after ${scalar} * R${row1}:"))
            }
            "add_multiple" -> {
                val rTargetIdx = row1 - 1
                val rSourceIdx = row2 - 1
                if (!(rTargetIdx in 0 until rows && rSourceIdx in 0 until rows)) {
                    steps.add("Error: Invalid row numbers. Please ensure they are within matrix bounds.")
                    return null
                }
                if (rTargetIdx == rSourceIdx && !scalar.isZero()) {
                    steps.add("Warning: Adding a multiple of a row to itself can lead to loss of information if k = -1 and not intended. Proceeding anyway.")
                }
                steps.add("Performing: R${row1} = R${row1} + (${scalar}) * R${row2}")
                for (j in 0 until cols) {
                    mat[rTargetIdx][j] += scalar * mat[rSourceIdx][j]
                }
                steps.add(formatMatrixForDisplay(mat, "Matrix after R${row1} = R${row1} + (${scalar}) * R${row2}:"))
            }
            else -> {
                steps.add("Error: Invalid elementary row operation type.")
                return null
            }
        }
        return mat
    }

    /**
     * Performs a linear combination of two source rows to replace a target row: R_target = k1*R_i + k2*R_j.
     * This is a new, more general elementary row operation.
     */
    fun performLinearCombination(
        matrix: List<List<Fraction>>,
        targetRow: Int, // 1-indexed
        source1Row: Int, // 1-indexed
        scalar1: Fraction,
        source2Row: Int, // 1-indexed
        scalar2: Fraction,
        steps: MutableList<String>
    ): List<List<Fraction>>? {
        val mat = matrix.map { it.toMutableList() }.toMutableList() // Create a deep copy
        val rows = mat.size
        val cols = mat[0].size

        val targetIdx = targetRow - 1
        val source1Idx = source1Row - 1
        val source2Idx = source2Row - 1

        if (!(targetIdx in 0 until rows && source1Idx in 0 until rows && source2Idx in 0 until rows)) {
            steps.add("Error: Invalid row numbers. Please ensure they are within matrix bounds.")
            return null
        }
        if (scalar1.isZero() && scalar2.isZero()) {
            steps.add("Warning: Both scalars are zero. This will make the target row all zeros.")
        }

        steps.add("\n--- Performing Linear Combination of Rows ---")
        steps.add(formatMatrixForDisplay(matrix, "Starting Matrix:"))
        steps.add("Performing: R${targetRow} = (${scalar1}) * R${source1Row} + (${scalar2}) * R${source2Row}")

        for (j in 0 until cols) {
            val val1 = mat[source1Idx][j] * scalar1
            val val2 = mat[source2Idx][j] * scalar2
            mat[targetIdx][j] = val1 + val2
        }

        steps.add(formatMatrixForDisplay(mat, "Matrix after R${targetRow} = (${scalar1}) * R${source1Row} + (${scalar2}) * R${source2Row}:"))
        return mat
    }


    /**
     * Converts a matrix to row echelon form (REF) with very detailed step-by-step display.
     */
    fun getRowEchelonForm(matrix: List<List<Fraction>>, steps: MutableList<String>): List<List<Fraction>> {
        steps.add("--- Converting Matrix to Row Echelon Form (REF) ---")
        val mat = matrix.map { it.toMutableList() }.toMutableList() // Create a deep copy
        val rows = mat.size
        val cols = mat[0].size
        var lead = 0 // Current leading column index

        steps.add(formatMatrixForDisplay(mat, "Original Matrix:"))
        steps.add("\nGoal: Transform the matrix into Row Echelon Form (REF).")
        steps.add("Properties of REF:")
        steps.add("  1. All non-zero rows are above any rows of all zeros.")
        steps.add("  2. The leading entry (pivot) of each non-zero row is in a column to the right of the leading entry of the row above it.")
        steps.add("  3. All entries in a column below a leading entry are zeros.")

        for (r in 0 until rows) { // Iterate through each row
            if (lead >= cols) { // If we've processed all columns, stop
                break
            }

            steps.add("\n--- Processing Row ${r + 1} (Current pivot column: ${lead + 1}) ---")

            // Step 1: Find a pivot (non-zero element) in the current leading column
            var i = r
            while (i < rows && mat[i][lead].isZero()) { // Search for a non-zero element in the current column from row 'r' downwards
                i++
            }

            if (i < rows) { // A non-zero pivot was found
                if (i != r) {
                    steps.add("  Step 1a: Pivot not at R${r + 1}. Swapping R${r + 1} and R${i + 1} to bring a non-zero pivot to position (${r + 1},${lead + 1}).")
                    mat[r] = mat[i].also { mat[i] = mat[r] }
                    steps.add(formatMatrixForDisplay(mat, "  Matrix after R${r + 1} <-> R${i + 1}:"))
                }

                // Step 2: Normalize the pivot row (make the pivot element 1)
                val pivotValue = mat[r][lead]
                if (pivotValue != Fraction.ONE) { // Only normalize if pivot is not already 1
                    if (pivotValue.isZero()) { // Should not happen if previous swap was successful, but as a safeguard
                        steps.add("  Warning: Pivot at (${r + 1},${lead + 1}) is still zero after swap. Skipping normalization.")
                        lead++ // Move to next column
                        continue
                    }

                    val scalarForNormalization = Fraction.ONE / pivotValue
                    steps.add("  Step 1b: Normalizing R${r + 1}. Multiplying R${r + 1} by ${Fraction.ONE} / ${pivotValue} = ${scalarForNormalization} to make pivot 1.")

                    // Perform the operation on the current row
                    for (colIdx in 0 until cols) {
                        mat[r][colIdx] *= scalarForNormalization
                    }
                    steps.add(formatMatrixForDisplay(mat, "  Matrix after R${r + 1} = ${scalarForNormalization} * R${r + 1}:"))
                }

                // Step 3: Eliminate elements below the pivot
                steps.add("  Step 2: Eliminating elements below the pivot (${mat[r][lead]}) in column ${lead + 1}.")
                for (rowIdx in 0 until rows) {
                    if (rowIdx != r) { // Don't operate on the pivot row itself
                        val elementToEliminate = mat[rowIdx][lead]
                        if (!elementToEliminate.isZero()) { // Only if the element is non-zero
                            val scalarForElimination = -elementToEliminate / mat[r][lead] // Should be -elementToEliminate if pivot is 1
                            steps.add("    Performing R${rowIdx + 1} = R${rowIdx + 1} + (${scalarForElimination}) * R${r + 1} to make element at (${rowIdx + 1},${lead + 1}) zero.")

                            // Perform the row operation
                            for (colIdx in 0 until cols) {
                                mat[rowIdx][colIdx] += scalarForElimination * mat[r][colIdx]
                            }
                            steps.add(formatMatrixForDisplay(mat, "    Matrix after eliminating element in R${rowIdx + 1}:"))
                        }
                    }
                }
                lead++ // Move to the next column for the next pivot
            } else { // If no non-zero pivot found in the current column (all zeros below 'r')
                steps.add("  Column ${lead + 1} contains all zeros from row ${r + 1} downwards. Moving to the next column.")
                lead++ // Move to the next column
            }
        }

        steps.add("\nMatrix conversion to Row Echelon Form complete.")
        return mat
    }

    /**
     * Converts a matrix to its Normal Form (also known as Rank Form or Canonical Form).
     * This involves converting to Row Reduced Echelon Form (RREF) and then
     * performing column operations to clear non-pivot columns, resulting in an identity block.
     */
    fun getNormalForm(matrix: List<List<Fraction>>, steps: MutableList<String>): List<List<Fraction>> {
        steps.add("--- Converting Matrix to Normal Form (Rank Form) ---")
        steps.add(formatMatrixForDisplay(matrix, "Original Matrix:"))

        steps.add("\nPhase 1: Convert to Row Echelon Form (REF).")
        val refMatrix = getRowEchelonForm(matrix, steps) // This will add its own steps
        steps.add(formatMatrixForDisplay(refMatrix, "Matrix in Row Echelon Form:"))

        steps.add("\nPhase 2: Convert from REF to Row Reduced Echelon Form (RREF).")
        steps.add("Goal: Make all pivots 1 (already done in REF step) and make all other entries in pivot columns zero.")
        val rrefMat = refMatrix.map { it.toMutableList() }.toMutableList() // Work on a copy of REF
        val rows = rrefMat.size
        val cols = rrefMat[0].size

        // Identify pivots to clear elements above them
        val pivotPositions = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (rrefMat[r][c] == Fraction.ONE && (r == rows - 1 || rrefMat.subList(r + 1, rows).all { it[c].isZero() })) {
                    // This check confirms it's a leading 1 and there are zeros below it
                    pivotPositions.add(Pair(r, c))
                    break // Move to the next row
                }
            }
        }

        for (r in pivotPositions.indices.reversed()) { // Iterate backwards from the last pivot
            val (pivotRow, pivotCol) = pivotPositions[r]
            steps.add("\n  -- Clearing elements above pivot at (${pivotRow + 1},${pivotCol + 1}) = ${rrefMat[pivotRow][pivotCol]} --")
            for (i in 0 until pivotRow) { // Iterate through rows above the current pivot row
                val elementToEliminate = rrefMat[i][pivotCol]
                if (!elementToEliminate.isZero()) { // Only if the element is non-zero
                    val scalarForElimination = -elementToEliminate / rrefMat[pivotRow][pivotCol] // Should be -elementToEliminate as pivot is 1
                    steps.add("    Performing R${i + 1} = R${i + 1} + (${scalarForElimination}) * R${pivotRow + 1} to make element at (${i + 1},${pivotCol + 1}) zero.")
                    for (colIdx in 0 until cols) {
                        rrefMat[i][colIdx] += scalarForElimination * rrefMat[pivotRow][colIdx]
                    }
                    steps.add(formatMatrixForDisplay(rrefMat, "    Matrix after eliminating element in R${i + 1}:"))
                }
            }
        }

        steps.add("\nMatrix in Row Reduced Echelon Form (RREF):")
        steps.add(formatMatrixForDisplay(rrefMat, ""))

        steps.add("\nPhase 3: Perform Column Operations to achieve Normal Form.")
        steps.add("Goal: Move pivot columns to form an identity matrix block in the top-left corner.")

        // Calculate rank from RREF (number of non-zero rows)
        var rank = 0
        for (r in 0 until rows) {
            if (rrefMat[r].any { !it.isZero() }) {
                rank++
            }
        }

        steps.add("\nConceptual Normal Form (an identity block of size ${rank}x${rank} followed by zeros):")
        val displayNormalForm = List(rows) { MutableList(cols) { Fraction.ZERO } }
        for (i in 0 until minOf(rank, rows)) {
            if (i < cols) {
                displayNormalForm[i][i] = Fraction.ONE
            }
        }

        steps.add(formatMatrixForDisplay(displayNormalForm, ""))
        steps.add("In a rigorous Normal Form transformation, column operations would physically rearrange the columns")
        steps.add("to achieve this identity block and move all non-pivot columns to the right.")

        return rrefMat // Returning the RREF for practical purposes
    }

    /**
     * Calculates the rank of a matrix by converting it to row echelon form.
     * Provides detailed explanation of rank definition.
     */
    fun calculateRank(matrix: List<List<Fraction>>, steps: MutableList<String>): Int {
        steps.add("--- Calculating Rank of a Matrix ---")
        steps.add(formatMatrixForDisplay(matrix, "Original Matrix:"))

        steps.add("\nDefinition of Rank:")
        steps.add("The rank of a matrix is the maximum number of linearly independent row vectors (or column vectors).")
        steps.add("Alternatively, it is the number of non-zero rows in its Row Echelon Form (REF) or Row Reduced Echelon Form (RREF).")

        steps.add("\nStep 1: Convert the matrix to Row Echelon Form (REF).")
        val refMatrix = getRowEchelonForm(matrix, steps) // This will add its own steps

        steps.add("\nMatrix in Row Echelon Form (after conversion):")
        steps.add(formatMatrixForDisplay(refMatrix, ""))

        var rank = 0
        for (r in refMatrix.indices) {
            // Check if the row contains any non-zero elements
            if (refMatrix[r].any { !it.isZero() }) {
                rank++
            }
        }

        steps.add("\nStep 2: Count the number of non-zero rows in the REF.")
        steps.add("  Number of non-zero rows = $rank")
        steps.add("The rank of the matrix is: $rank")
        return rank
    }

    /**
     * Solves a system of linear equations using the augmented matrix and Row Echelon Form.
     * Provides detailed steps for consistency check and back-substitution.
     */
    fun solveLinearEquationsRef(matrix: List<List<Fraction>>, constants: List<Fraction>, steps: MutableList<String>): String {
        steps.add("--- Solving System of Linear Equations (Row Echelon Form Method) ---")
        val rows = matrix.size
        val cols = matrix[0].size // Number of variables

        steps.add("The system of equations can be represented as AX = B, where:")
        steps.add(formatMatrixForDisplay(matrix, "Coefficient Matrix (A):"))
        steps.add("\nConstant Vector (B):")
        for (const in constants) {
            steps.add("[${const}]")
        }
        steps.add("-".repeat(15))

        // Create the augmented matrix [A|B]
        val augmentedMatrix = matrix.mapIndexed { i, row ->
            row.toMutableList().apply { add(constants[i]) }
        }.toMutableList()
        steps.add("\nStep 1: Form the Augmented Matrix [A|B].")
        steps.add(formatMatrixForDisplay(augmentedMatrix, "Augmented Matrix [A|B]:"))

        steps.add("\nStep 2: Convert the Augmented Matrix to Row Echelon Form (REF).")
        val refAugmentedMatrix = getRowEchelonForm(augmentedMatrix, steps)
        steps.add("\nAugmented Matrix in Row Echelon Form:")
        steps.add(formatMatrixForDisplay(refAugmentedMatrix, ""))

        steps.add("\nStep 3: Analyze the Ranks for Consistency and Type of Solution.")
        // Calculate rank of coefficient matrix A and augmented matrix [A|B]
        var rankA = 0
        for (r in 0 until rows) {
            // Check if row in A part (excluding last column) is non-zero
            if (refAugmentedMatrix[r].subList(0, cols).any { !it.isZero() }) {
                rankA++
            }
        }

        var rankAugmented = 0
        for (r in 0 until rows) {
            // Check if entire row in augmented matrix is non-zero
            if (refAugmentedMatrix[r].any { !it.isZero() }) {
                rankAugmented++
            }
        }

        steps.add("  Rank of Coefficient Matrix (rank(A)): $rankA")
        steps.add("  Rank of Augmented Matrix (rank(A|b)): $rankAugmented")
        val numVariables = cols
        steps.add("  Number of variables (n): $numVariables")

        if (rankA < rankAugmented) {
            steps.add("\nConclusion: The system is INCONSISTENT.")
            steps.add("  This occurs when rank(A) < rank(A|b), implying a contradiction (e.g., 0 = non-zero value).")
            steps.add("  Therefore, there is NO SOLUTION to this system of equations.")
            return "No Solution"
        } else if (rankA == rankAugmented) {
            if (rankA == numVariables) {
                steps.add("\nConclusion: The system has a UNIQUE SOLUTION.")
                steps.add("  This occurs when rank(A) = rank(A|b) = number of variables.")
                steps.add("Step 4: Perform Back-Substitution to find the unique solution.")
                val solutions = MutableList(numVariables) { Fraction.ZERO } // Initialize solutions

                // Convert to RREF explicitly for back-substitution clarity
                val tempRREFMatrix = refAugmentedMatrix.map { it.toMutableList() }.toMutableList()
                for (r in rankA - 1 downTo 0) { // Iterate backwards from the last pivot row
                    var pivotCol = -1
                    for (c in 0 until numVariables) {
                        if (tempRREFMatrix[r][c] == Fraction.ONE) { // Find the leading 1 (pivot)
                            pivotCol = c
                            break
                        }
                    }

                    if (pivotCol != -1) { // If it's a pivot row
                        // Clear elements above the current pivot
                        for (iAbove in 0 until r) { // Iterate through rows above current row
                            val elementAbovePivot = tempRREFMatrix[iAbove][pivotCol]
                            if (!elementAbovePivot.isZero()) {
                                val scalar = -elementAbovePivot / tempRREFMatrix[r][pivotCol]
                                for (k in 0 until numVariables + 1) { // Apply to all columns including constants
                                    tempRREFMatrix[iAbove][k] += scalar * tempRREFMatrix[r][k]
                                }
                            }
                        }
                    }
                }

                steps.add(formatMatrixForDisplay(tempRREFMatrix, "\nAugmented Matrix in Row Reduced Echelon Form (for back-substitution):"))

                // Perform back-substitution
                steps.add("\n  Performing back-substitution:")
                for (i in numVariables - 1 downTo 0) {
                    // Find the pivot for the current row
                    var pivotCol = -1
                    for (j in 0 until numVariables) {
                        if (!tempRREFMatrix[i][j].isZero()) { // Find first non-zero element in the row
                            pivotCol = j
                            break
                        }
                    }

                    if (pivotCol != -1) { // If it's a pivot row
                        // Value from the constant column
                        var value = tempRREFMatrix[i][numVariables]

                        // Subtract terms involving already found solutions
                        var sumOfKnownTerms = Fraction.ZERO
                        for (k in pivotCol + 1 until numVariables) {
                            val termVal = tempRREFMatrix[i][k] * solutions[k]
                            sumOfKnownTerms += termVal
                            steps.add("    x${pivotCol + 1} calculation: Subtracting ${tempRREFMatrix[i][k]} * x${k + 1} (${solutions[k]})")
                        }

                        solutions[pivotCol] = (value - sumOfKnownTerms) / tempRREFMatrix[i][pivotCol]
                        steps.add("    x${pivotCol + 1} = (${value} - ${sumOfKnownTerms}) / ${tempRREFMatrix[i][pivotCol]} = ${solutions[pivotCol]}")
                    }
                }

                val solutionStr = StringBuilder("\nSolution Found (X):\n")
                for (i in solutions.indices) {
                    solutionStr.appendLine("  x${i + 1} = ${solutions[i]}")
                }
                steps.add(solutionStr.toString())
                return solutionStr.toString()
            } else {
                steps.add("\nConclusion: The system has INFINITE SOLUTIONS.")
                steps.add("  This occurs when rank(A) = rank(A|b) < number of variables.")
                steps.add("  Number of free variables = ${numVariables - rankA}.")
                steps.add("  To express the general solution, leading variables are expressed in terms of free variables.")
                steps.add("  This involves parameterizing the free variables and then solving for leading variables.")
                steps.add("  Due to the complexity of a general parametric solution, this calculator will not explicitly generate the parametric form,")
                steps.add("  but it confirms the existence of infinite solutions.")
                return "Infinite Solutions"
            }
        } else {
            steps.add("An unexpected error occurred during rank analysis. Please check the input matrix.")
            return "Error during rank analysis."
        }
    }


    /**
     * Solves a system of linear equations using the Matrix Inversion Method (X = A^-1 * B).
     * Provides detailed steps.
     */
    fun solveLinearEquationsInverse(matrix: List<List<Fraction>>, constants: List<Fraction>, steps: MutableList<String>): String? {
        steps.add("--- Solving System of Linear Equations (Matrix Inversion Method) ---")
        val rows = matrix.size
        val cols = matrix[0].size

        if (rows != cols) {
            steps.add("Error: The matrix inversion method requires the coefficient matrix to be square (number of equations = number of variables).")
            return null
        }

        steps.add("The system is in the form AX = B, where:")
        steps.add(formatMatrixForDisplay(matrix, "Coefficient Matrix (A):"))
        steps.add("\nConstant Vector (B):")
        for (const in constants) {
            steps.add("[${const}]")
        }
        steps.add("-".repeat(15))

        steps.add("\nStep 1: Calculate the Inverse of the Coefficient Matrix (A^-1).")
        val inverseA = inverseMatrixAdjointMethod(matrix, steps) // This function is already detailed

        if (inverseA == null) {
            steps.add("\nCannot solve using the inverse method because the inverse of A does not exist.")
            steps.add("This usually happens if the determinant of A is zero (A is singular).")
            return null
        }

        steps.add("\nStep 2: Multiply the Inverse Matrix by the Constant Vector (X = A^-1 * B).")
        steps.add("  This is a matrix multiplication where A^-1 is (${rows} x ${cols}) and B is (${rows} x 1), resulting in X (${rows} x 1).")

        // Convert constants list to a column matrix for multiplication
        val bColumnMatrix = constants.map { listOf(it) }

        val solutionsMatrixProduct = multiplyMatrices(inverseA, bColumnMatrix, steps)

        return if (solutionsMatrixProduct != null) {
            val solutionStr = StringBuilder("\nSolution (X) = A^-1 * B:\n")
            for (i in solutionsMatrixProduct.indices) {
                val sol = solutionsMatrixProduct[i][0]
                solutionStr.appendLine("  x${i + 1} = ${sol}")
            }
            steps.add(solutionStr.toString())
            solutionStr.toString()
        } else {
            steps.add("Error: Failed to multiply inverse matrix by constant vector.")
            null
        }
    }


    /**
     * Creates an n x n identity matrix.
     */
    private fun createIdentityMatrix(n: Int): List<List<Fraction>> {
        val identity = List(n) { MutableList(n) { Fraction.ZERO } }
        for (i in 0 until n) {
            identity[i][i] = Fraction.ONE
        }
        return identity
    }

    /**
     * Calculates the inverse of a square matrix using elementary row operations.
     * Augments [A|I] and reduces A to I, converting I to A^-1.
     */
    fun inverseMatrixElementaryOps(matrix: List<List<Fraction>>, steps: MutableList<String>): List<List<Fraction>>? {
        steps.add("--- Inverse Matrix Operation (Elementary Row Operations Method) ---")
        val rows = matrix.size
        val cols = matrix[0].size

        if (rows != cols) {
            steps.add("Error: Inverse by elementary row operations is defined only for square matrices.")
            return null
        }

        // Create the augmented matrix [A|I]
        val identityMatrix = createIdentityMatrix(rows)
        val augmentedMatrix = mutableListOf<MutableList<Fraction>>()
        for (r in 0 until rows) {
            augmentedMatrix.add((matrix[r].toMutableList()).apply { addAll(identityMatrix[r]) })
        }

        steps.add(formatMatrixForDisplay(matrix, "Original Matrix (A):"))
        steps.add(formatMatrixForDisplay(identityMatrix, "Identity Matrix (I):"))
        steps.add(formatMatrixForDisplay(augmentedMatrix, "Augmented Matrix [A|I]:"))

        steps.add("\nGoal: Apply elementary row operations to transform [A|I] into [I|A^-1].")
        steps.add("This means reducing the left half (original matrix A) to an identity matrix.")

        val mat = augmentedMatrix.map { it.toMutableList() }.toMutableList() // Work on a deep copy
        val n = rows // n for the square matrix

        for (r in 0 until n) { // Iterate through each row to create leading 1s and zeros below
            steps.add("\n--- Processing for Pivot in Row ${r + 1}, Column ${r + 1} ---")

            // Step 1: Find a pivot (non-zero element) for the current column 'r'
            var pivotRow = r
            while (pivotRow < n && mat[pivotRow][r].isZero()) {
                pivotRow++
            }

            if (pivotRow == n) {
                steps.add("Error: A pivot could not be found in column ${r + 1}. The matrix is singular, and its inverse does not exist.")
                steps.add("This usually happens if a row or column becomes all zeros during the process.")
                return null
            }

            if (pivotRow != r) {
                steps.add("  Step 1a: Pivot not at R${r + 1}. Swapping R${r + 1} and R${pivotRow + 1} to bring a non-zero pivot to position (${r + 1},${r + 1}).")
                mat[r] = mat[pivotRow].also { mat[pivotRow] = mat[r] }
                steps.add(formatMatrixForDisplay(mat, "  Matrix after R${r + 1} <-> R${pivotRow + 1}:"))
            }

            // Step 2: Normalize the pivot row (make the pivot element 1)
            val pivotValue = mat[r][r]
            if (pivotValue != Fraction.ONE) { // Only normalize if pivot is not already 1
                val scalarForNormalization = Fraction.ONE / pivotValue
                steps.add("  Step 1b: Normalizing R${r + 1}. Multiplying R${r + 1} by ${Fraction.ONE} / ${pivotValue} = ${scalarForNormalization} to make pivot 1.")
                for (cIdx in 0 until 2 * n) { // Apply to entire augmented row
                    mat[r][cIdx] *= scalarForNormalization
                }
                steps.add(formatMatrixForDisplay(mat, "  Matrix after ${scalarForNormalization} * R${r + 1}:"))
            }

            // Step 3: Eliminate elements above and below the pivot in the current column
            steps.add("  Step 2: Eliminating elements above and below the pivot (${mat[r][r]}) in column ${r + 1}.")
            for (rowIdx in 0 until n) {
                if (rowIdx != r) { // Don't operate on the pivot row itself
                    val elementToEliminate = mat[rowIdx][r]
                    if (!elementToEliminate.isZero()) { // Only if the element is non-zero
                        val scalarForElimination = -elementToEliminate / mat[r][r] // Should be -elementToEliminate as pivot is 1
                        steps.add("    Performing R${rowIdx + 1} = R${rowIdx + 1} + (${scalarForElimination}) * R${r + 1} to make element at (${rowIdx + 1},${r + 1}) zero.")
                        for (colIdx in 0 until 2 * n) { // Apply to entire augmented row
                            mat[rowIdx][colIdx] += scalarForElimination * mat[r][colIdx]
                        }
                        steps.add(formatMatrixForDisplay(mat, "    Matrix after eliminating element in R${rowIdx + 1}:"))
                    }
                }
            }
        }

        // After loop, the left side of 'mat' should be the identity matrix if inverse exists.
        // Check if the left side is indeed an identity matrix
        var isIdentity = true
        for (r in 0 until n) {
            for (c in 0 until n) {
                val expectedVal = if (r == c) Fraction.ONE else Fraction.ZERO
                if (mat[r][c] != expectedVal) {
                    isIdentity = false
                    break
                }
            }
            if (!isIdentity) {
                break
            }
        }

        if (!isIdentity) {
            steps.add("\nError: The original matrix could not be reduced to an identity matrix.")
            steps.add("This indicates that the matrix is singular and its inverse does not exist.")
            return null
        }

        steps.add("\nTransformation complete. The left side is now the Identity Matrix.")
        steps.add("The right side is the Inverse Matrix (A^-1).")

        // Extract the inverse matrix (the right half of the augmented matrix)
        val inverseMatrixResult = mutableListOf<List<Fraction>>()
        for (r in 0 until n) {
            inverseMatrixResult.add(mat[r].subList(n, 2 * n))
        }

        return inverseMatrixResult
    }
}
