package com.aquaa.matrix

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aquaa.matrix.databinding.ActivityMainBinding
import android.widget.ScrollView
import com.aquaa.matrix.data.AppDatabase
import com.aquaa.matrix.data.MatrixHistoryDao
import com.aquaa.matrix.data.MatrixHistoryEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.gson.Gson
import androidx.transition.TransitionManager // Import TransitionManager
import androidx.transition.AutoTransition // Import AutoTransition

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var database: AppDatabase
    private lateinit var matrixHistoryDao: MatrixHistoryDao

    private val matrixOperationOptions = mutableListOf(
        "Transpose of a Matrix",
        "Matrix Addition",
        "Matrix Subtraction",
        "Matrix Multiplication",
        "Determinant of a Matrix",
        "Adjoint of a Matrix",
        "Inverse of a Matrix (Adjoint Method)",
        "Inverse of a Matrix (Elementary Row Operations)",
        "Elementary Row Operations (Interactive)",
        "Row Echelon Form (REF)",
        "Normal Form (Rank Form)",
        "Rank of a Matrix",
        "Solve Linear Equations (REF Method)",
        "Solve Linear Equations (Inverse Method)"
    )

    private val eroTypeOptions = listOf(
        "Swap Two Rows",
        "Multiply a Row by a Scalar",
        "Add a Multiple of One Row to Another",
        "Replace Row with Linear Combination"
    )

    private var currentWorkingMatrix: List<List<Fraction>>? = null
    private var eroStepsCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(findViewById(R.id.toolbar_main))

        database = AppDatabase.getDatabase(this)
        matrixHistoryDao = database.matrixHistoryDao()

        matrixOperationOptions.sort()

        setupSpinners()
        setupListeners()
        resetInteractiveEroMode()

        // Animations on app launch
        binding.cardMatrixA.apply {
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            visibility = View.VISIBLE
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }

        binding.spinnerOperations.apply {
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            visibility = View.VISIBLE
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setStartDelay(150)
                .start()
        }

        binding.buttonPerformOperation.apply {
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            visibility = View.VISIBLE
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setStartDelay(300)
                .start()
        }

        binding.mainScrollView.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            val scrollView = v as ScrollView
            val totalHeight = scrollView.getChildAt(0).height
            val visibleHeight = scrollView.height
            val scrollRange = totalHeight - visibleHeight

            if (scrollRange <= 0) {
                binding.buttonGoUp.visibility = View.GONE
                binding.buttonGoUp.alpha = 0f
                return@setOnScrollChangeListener
            }

            val scrollThreshold = (scrollRange * 0.8).toInt()

            if (scrollY >= scrollThreshold && binding.buttonGoUp.visibility == View.GONE) {
                binding.buttonGoUp.alpha = 0f
                binding.buttonGoUp.visibility = View.VISIBLE
                binding.buttonGoUp.animate().alpha(1f).setDuration(300).start()
            } else if (scrollY < scrollThreshold && binding.buttonGoUp.visibility == View.VISIBLE) {
                binding.buttonGoUp.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction { binding.buttonGoUp.visibility = View.GONE }
                    .start()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_history -> {
                val intent = Intent(this, HistoryActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupSpinners() {
        val mainAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, matrixOperationOptions)
        mainAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerOperations.adapter = mainAdapter

        binding.spinnerOperations.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedOp = matrixOperationOptions[position]
                resetInteractiveEroMode()

                binding.cardResult.visibility = View.GONE
                binding.cardSteps.visibility = View.GONE

                // Animate Matrix B visibility
                val isMatrixBOperation = selectedOp == "Matrix Addition" || selectedOp == "Matrix Subtraction" || selectedOp == "Matrix Multiplication"

                if (isMatrixBOperation) {
                    if (binding.cardMatrixB.visibility != View.VISIBLE) { // Only animate if becoming visible
                        binding.cardMatrixB.apply {
                            alpha = 0f
                            scaleX = 0.8f
                            scaleY = 0.8f
                            visibility = View.VISIBLE
                            animate()
                                .alpha(1f)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(300) // Pop-in duration
                                .setInterpolator(AccelerateDecelerateInterpolator())
                                .start()
                        }
                    }
                } else {
                    if (binding.cardMatrixB.visibility == View.VISIBLE) { // Only animate if becoming gone
                        binding.cardMatrixB.animate()
                            .alpha(0f)
                            .scaleX(0.8f)
                            .scaleY(0.8f)
                            .setDuration(300) // Pop-out duration
                            .setInterpolator(AccelerateDecelerateInterpolator())
                            .withEndAction {
                                binding.cardMatrixB.visibility = View.GONE
                                binding.cardMatrixB.alpha = 1f // Reset alpha for next pop-in
                                binding.cardMatrixB.scaleX = 1f // Reset scale for next pop-in
                                binding.cardMatrixB.scaleY = 1f // Reset scale for next pop-in
                            }
                            .start()
                    }
                }

                // Animate Constants visibility (similar logic as before)
                val isLinearEquationOperation = selectedOp == "Solve Linear Equations (REF Method)" || selectedOp == "Solve Linear Equations (Inverse Method)"
                if (isLinearEquationOperation) {
                    binding.cardConstants.visibility = View.VISIBLE
                } else {
                    binding.cardConstants.visibility = View.GONE
                }


                when (selectedOp) {
                    "Matrix Addition", "Matrix Subtraction", "Matrix Multiplication" -> {
                        // Matrix B handled above
                        binding.cardConstants.visibility = View.GONE
                        binding.buttonPerformOperation.text = "Perform Operation"
                        binding.editTextMatrixA.isEnabled = true
                    }
                    "Solve Linear Equations (REF Method)", "Solve Linear Equations (Inverse Method)" -> {
                        binding.cardMatrixB.visibility = View.GONE // Ensure Matrix B is gone if it was visible
                        // Constants handled above
                        binding.buttonPerformOperation.text = "Perform Operation"
                        binding.editTextMatrixA.isEnabled = true
                    }
                    "Elementary Row Operations (Interactive)" -> {
                        binding.cardMatrixB.visibility = View.GONE // Ensure Matrix B is gone if it was visible
                        binding.cardConstants.visibility = View.GONE
                        binding.cardEroControls.visibility = View.GONE
                        binding.buttonPerformOperation.text = "Start Interactive ERO"
                        binding.editTextMatrixA.isEnabled = true
                    }
                    else -> {
                        binding.cardMatrixB.visibility = View.GONE // Ensure Matrix B is gone if it was visible
                        binding.cardConstants.visibility = View.GONE
                        binding.buttonPerformOperation.text = "Perform Operation"
                        binding.editTextMatrixA.isEnabled = true
                    }
                }
                binding.textViewResult.text = "No result yet."
                binding.textViewSteps.text = "Steps will appear here."
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val eroAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, eroTypeOptions)
        eroAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEroType.adapter = eroAdapter

        binding.spinnerEroType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (eroTypeOptions[position]) {
                    "Swap Two Rows" -> {
                        binding.editTextRow1.hint = "First Row (1-indexed)"
                        binding.editTextRow2.visibility = View.VISIBLE
                        binding.editTextRow2.hint = "Second Row (1-indexed)"
                        binding.editTextScalarK.visibility = View.GONE
                        binding.editTextSourceRow2.visibility = View.GONE
                        binding.editTextScalarK2.visibility = View.GONE
                    }
                    "Multiply a Row by a Scalar" -> {
                        binding.editTextRow1.hint = "Row to Multiply (1-indexed)"
                        binding.editTextRow2.visibility = View.GONE
                        binding.editTextScalarK.visibility = View.VISIBLE
                        binding.editTextScalarK.hint = "Scalar (k)"
                        binding.editTextSourceRow2.visibility = View.GONE
                        binding.editTextScalarK2.visibility = View.GONE
                    }
                    "Add a Multiple of One Row to Another" -> {
                        binding.editTextRow1.hint = "Row to Modify (1-indexed)"
                        binding.editTextRow2.visibility = View.VISIBLE
                        binding.editTextRow2.hint = "Row to Add (1-indexed)"
                        binding.editTextScalarK.visibility = View.VISIBLE
                        binding.editTextScalarK.hint = "Scalar (k) for Row to Add"
                        binding.editTextSourceRow2.visibility = View.GONE
                        binding.editTextScalarK2.visibility = View.GONE
                    }
                    "Replace Row with Linear Combination" -> {
                        binding.editTextRow1.hint = "Row to Modify (1-indexed)"
                        binding.editTextRow2.visibility = View.VISIBLE
                        binding.editTextRow2.hint = "First Source Row (1-indexed)"
                        binding.editTextScalarK.visibility = View.VISIBLE
                        binding.editTextScalarK.hint = "First Scalar (k1)"
                        binding.editTextSourceRow2.visibility = View.VISIBLE
                        binding.editTextScalarK2.visibility = View.VISIBLE
                        binding.editTextScalarK2.hint = "Second Scalar (k2)"
                    }
                }
                binding.editTextRow1.text.clear()
                binding.editTextRow2.text.clear()
                binding.editTextScalarK.text.clear()
                binding.editTextSourceRow2.text.clear()
                binding.editTextScalarK2.text.clear()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupListeners() {
        binding.buttonPerformOperation.setOnClickListener {
            val selectedOp = binding.spinnerOperations.selectedItem.toString()
            if (selectedOp == "Elementary Row Operations (Interactive)") {
                startEroInteractiveMode()
            } else {
                performSelectedOperation()
            }
        }

        binding.buttonApplyEroStep.setOnClickListener {
            applyEroStep()
        }

        binding.buttonResetEroSession.setOnClickListener {
            resetInteractiveEroMode()
            Toast.makeText(this, "ERO session reset. You can now edit Matrix A.", Toast.LENGTH_LONG).show()
        }

        binding.buttonGoUp.setOnClickListener {
            binding.mainScrollView.smoothScrollTo(0, binding.cardResult.top)
            binding.buttonGoUp.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction { binding.buttonGoUp.visibility = View.GONE }
                .start()
        }
    }

    private fun resetInteractiveEroMode() {
        currentWorkingMatrix = null
        eroStepsCount = 0
        binding.cardEroControls.visibility = View.GONE
        binding.buttonApplyEroStep.visibility = View.GONE
        binding.buttonPerformOperation.visibility = View.VISIBLE
        binding.editTextMatrixA.isEnabled = true
        binding.editTextMatrixB.text.clear()
        binding.editTextConstants.text.clear()
        binding.editTextRow1.text.clear()
        binding.editTextRow2.text.clear()
        binding.editTextScalarK.text.clear()
        binding.editTextSourceRow2.text.clear()
        binding.editTextScalarK2.text.clear()

        binding.editTextSourceRow2.visibility = View.GONE
        binding.editTextScalarK2.visibility = View.GONE

        binding.textViewResult.text = "No result yet."
        binding.textViewSteps.text = "Steps will appear here."

        binding.cardResult.visibility = View.GONE
        binding.cardSteps.visibility = View.GONE
        binding.buttonGoUp.visibility = View.GONE
        binding.buttonGoUp.alpha = 0f

        // Ensure Matrix B is gone and reset when mode is reset
        binding.cardMatrixB.visibility = View.GONE
        binding.cardMatrixB.alpha = 1f
        binding.cardMatrixB.scaleX = 1f
        binding.cardMatrixB.scaleY = 1f
    }

    private fun startEroInteractiveMode() {
        val matrixAInput = binding.editTextMatrixA.text.toString()
        val parsedMatrixA = MatrixOperations.parseMatrixInput(matrixAInput, "Matrix A")
        val matrixA = parsedMatrixA.first
        val errorA = parsedMatrixA.second

        if (matrixA == null) {
            binding.textViewResult.text = "Input Error!"
            binding.textViewSteps.text = errorA
            binding.cardResult.visibility = View.VISIBLE
            binding.cardSteps.visibility = View.VISIBLE
            return
        }

        currentWorkingMatrix = matrixA
        eroStepsCount = 0
        binding.textViewResult.text = MatrixOperations.formatMatrixForDisplay(currentWorkingMatrix!!, "Current Working Matrix:")
        binding.textViewSteps.text = "Interactive ERO mode started. Enter an operation below to apply it step-by-step.\n"

        binding.editTextMatrixA.isEnabled = false
        binding.cardMatrixB.visibility = View.GONE
        binding.cardConstants.visibility = View.GONE
        binding.buttonPerformOperation.visibility = View.GONE
        binding.cardEroControls.visibility = View.VISIBLE
        binding.buttonApplyEroStep.visibility = View.VISIBLE

        binding.spinnerEroType.setSelection(0)
        binding.editTextRow1.hint = "First Row (1-indexed)"
        binding.editTextRow2.visibility = View.VISIBLE
        binding.editTextRow2.hint = "Second Row (1-indexed)"
        binding.editTextScalarK.visibility = View.GONE
        binding.editTextSourceRow2.visibility = View.GONE
        binding.editTextScalarK2.visibility = View.GONE

        binding.cardResult.visibility = View.VISIBLE
        binding.cardSteps.visibility = View.VISIBLE
    }

    private fun applyEroStep() {
        val matrix = currentWorkingMatrix
        if (matrix == null) {
            Toast.makeText(this, "Please start Interactive ERO mode first.", Toast.LENGTH_SHORT).show()
            return
        }

        val rows = matrix.size
        val cols = matrix[0].size

        val selectedEroType = binding.spinnerEroType.selectedItem.toString()
        val steps = mutableListOf<String>()

        try {
            val updatedMatrix: List<List<Fraction>>? = when (selectedEroType) {
                "Swap Two Rows" -> {
                    val row1Str = binding.editTextRow1.text.toString()
                    val row2Str = binding.editTextRow2.text.toString()

                    if (row1Str.isBlank() || row2Str.isBlank()) {
                        Toast.makeText(this, "Please enter both row numbers for swap.", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val row1 = row1Str.toInt()
                    val row2 = row2Str.toInt()
                    if (row1 <= 0 || row1 > rows || row2 <= 0 || row2 > rows) {
                        Toast.makeText(this, "Invalid row numbers. Must be within matrix bounds (1 to $rows).", Toast.LENGTH_LONG).show()
                        return
                    }
                    MatrixOperations.performElementaryRowOperation(
                        matrix, "swap", row1, row2, steps = steps
                    )
                }
                "Multiply a Row by a Scalar" -> {
                    val rowStr = binding.editTextRow1.text.toString()
                    val scalarStr = binding.editTextScalarK.text.toString()

                    if (rowStr.isBlank()) {
                        Toast.makeText(this, "Please enter the row number to multiply.", Toast.LENGTH_SHORT).show()
                        return
                    }
                    if (scalarStr.isBlank()) {
                        Toast.makeText(this, "Please enter a scalar value.", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val row = rowStr.toInt()
                    if (row <= 0 || row > rows) {
                        Toast.makeText(this, "Invalid row number. Must be within matrix bounds (1 to $rows).", Toast.LENGTH_LONG).show()
                        return
                    }
                    val scalar = Fraction.parse(scalarStr)
                    if (scalar.isZero()) {
                        Toast.makeText(this, "Scalar cannot be zero for this operation.", Toast.LENGTH_SHORT).show()
                        return
                    }
                    MatrixOperations.performElementaryRowOperation(
                        matrix, "multiply", row, scalar = scalar, steps = steps
                    )
                }
                "Add a Multiple of One Row to Another" -> {
                    val targetRowStr = binding.editTextRow1.text.toString()
                    val scalarStr = binding.editTextScalarK.text.toString()
                    val sourceRowStr = binding.editTextRow2.text.toString()

                    if (targetRowStr.isBlank() || scalarStr.isBlank() || sourceRowStr.isBlank()) {
                        Toast.makeText(this, "Please enter the row to modify, the row to add, and the scalar.", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val targetRow = targetRowStr.toInt()
                    val sourceRow = sourceRowStr.toInt()
                    if (targetRow <= 0 || targetRow > rows || sourceRow <= 0 || sourceRow > rows) {
                        Toast.makeText(this, "Invalid row numbers. Must be within matrix bounds (1 to $rows).", Toast.LENGTH_LONG).show()
                        return
                    }
                    val scalar = Fraction.parse(scalarStr)

                    MatrixOperations.performElementaryRowOperation(
                        matrix, "add_multiple", targetRow, sourceRow, scalar, steps
                    )
                }
                "Replace Row with Linear Combination" -> {
                    val targetRowStr = binding.editTextRow1.text.toString()
                    val source1RowStr = binding.editTextRow2.text.toString()
                    val scalar1Str = binding.editTextScalarK.text.toString()
                    val source2RowStr = binding.editTextSourceRow2.text.toString()
                    val scalar2Str = binding.editTextScalarK2.text.toString()

                    if (targetRowStr.isBlank() || source1RowStr.isBlank() || scalar1Str.isBlank() ||
                        source2RowStr.isBlank() || scalar2Str.isBlank()) {
                        Toast.makeText(this, "Please fill all fields for linear combination.", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val targetRow = targetRowStr.toInt()
                    val source1Row = source1RowStr.toInt()
                    val scalar1 = Fraction.parse(scalar1Str)
                    val source2Row = source2RowStr.toInt()
                    val scalar2 = Fraction.parse(scalar2Str)

                    if (targetRow <= 0 || targetRow > rows ||
                        source1Row <= 0 || source1Row > rows ||
                        source2Row <= 0 || source2Row > rows) {
                        Toast.makeText(this, "Invalid row numbers. Must be within matrix bounds (1 to $rows).", Toast.LENGTH_LONG).show()
                        return
                    }

                    MatrixOperations.performLinearCombination(
                        matrix, targetRow, source1Row, scalar1, source2Row, scalar2, steps
                    )
                }
                else -> null
            }

            if (updatedMatrix != null) {
                currentWorkingMatrix = updatedMatrix
                eroStepsCount++
                binding.textViewResult.text = MatrixOperations.formatMatrixForDisplay(currentWorkingMatrix!!, "Current Working Matrix after Step $eroStepsCount:")
                binding.textViewSteps.append("\n--- ERO Step $eroStepsCount ---\n")
                binding.textViewSteps.append(steps.joinToString("\n"))
                binding.mainScrollView.post {
                    binding.mainScrollView.fullScroll(View.FOCUS_DOWN)
                }
                binding.editTextRow1.text.clear()
                binding.editTextRow2.text.clear()
                binding.editTextScalarK.text.clear()
                binding.editTextSourceRow2.text.clear()
                binding.editTextScalarK2.text.clear()
            } else {
                binding.textViewResult.text = "ERO Operation Failed."
                binding.textViewSteps.append("\nError during ERO step.")
                binding.textViewSteps.append(steps.joinToString("\n"))
                resetInteractiveEroMode()
                Toast.makeText(this, "Operation failed. Resetting ERO mode.", Toast.LENGTH_LONG).show()
            }

        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Invalid input. Please enter valid numbers or fractions for rows and scalar.", Toast.LENGTH_LONG).show()
            binding.textViewSteps.append("\nError: Invalid input format. ${e.message}")
        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, "Invalid input for scalar: ${e.message}", Toast.LENGTH_LONG).show()
            binding.textViewSteps.append("\nError: Invalid scalar input. ${e.message}")
        } catch (e: Exception) {
            Toast.makeText(this, "An unexpected error occurred: ${e.message}", Toast.LENGTH_LONG).show()
            binding.textViewSteps.append("\nUnexpected error during ERO: ${e.message}")
        }
    }


    private fun performSelectedOperation() {
        binding.textViewResult.text = "Calculating..."
        binding.textViewSteps.text = "Starting calculations...\n"
        val steps = mutableListOf<String>()

        val selectedOperation = binding.spinnerOperations.selectedItem.toString()
        val matrixAInput = binding.editTextMatrixA.text.toString()
        val matrixBInput = binding.editTextMatrixB.text.toString()
        val constantsInput = binding.editTextConstants.text.toString()

        val parsedMatrixA = MatrixOperations.parseMatrixInput(matrixAInput, "Matrix A")
        val matrixA = parsedMatrixA.first
        val errorA = parsedMatrixA.second

        if (matrixA == null) {
            binding.textViewResult.text = "Input Error!"
            binding.textViewSteps.text = errorA
            binding.cardResult.visibility = View.VISIBLE
            binding.cardSteps.visibility = View.VISIBLE
            return
        }

        var matrixB: List<List<Fraction>>? = null
        var constants: List<Fraction>? = null

        when (selectedOperation) {
            "Matrix Addition", "Matrix Subtraction", "Matrix Multiplication" -> {
                val parsedMatrixB = MatrixOperations.parseMatrixInput(matrixBInput, "Matrix B")
                matrixB = parsedMatrixB.first
                val errorB = parsedMatrixB.second
                if (matrixB == null) {
                    binding.textViewResult.text = "Input Error!"
                    binding.textViewSteps.text = errorB
                    binding.cardResult.visibility = View.VISIBLE
                    binding.cardSteps.visibility = View.VISIBLE
                    return
                }
            }
            "Solve Linear Equations (REF Method)", "Solve Linear Equations (Inverse Method)" -> {
                val parsedConstants = parseConstantsInput(constantsInput)
                constants = parsedConstants.first
                val errorConstants = parsedConstants.second
                if (constants == null) {
                    binding.textViewResult.text = "Input Error!"
                    binding.textViewSteps.text = errorConstants
                    binding.cardResult.visibility = View.VISIBLE
                    binding.cardSteps.visibility = View.VISIBLE
                    return
                }
                if (matrixA.size != constants.size) {
                    binding.textViewResult.text = "Input Error!"
                    binding.textViewSteps.text = "Error: Number of rows in Matrix A (${matrixA.size}) must match the number of constants (${constants.size})."
                    binding.cardResult.visibility = View.VISIBLE
                    binding.cardSteps.visibility = View.VISIBLE
                    return
                }
            }
            "Adjoint of a Matrix", "Inverse of a Matrix (Adjoint Method)", "Inverse of a Matrix (Elementary Row Operations)", "Determinant of a Matrix" -> {
                if (matrixA.size != matrixA[0].size) {
                    binding.textViewResult.text = "Input Error!"
                    binding.textViewSteps.text = "Error: This operation requires a square matrix. Matrix A is ${matrixA.size}x${matrixA[0].size}."
                    binding.cardResult.visibility = View.VISIBLE
                    binding.cardSteps.visibility = View.VISIBLE
                    return
                }
            }
            // "Elementary Row Operations (Interactive)" is handled by startEroInteractiveMode
        }

        var resultMatrix: List<List<Fraction>>? = null
        var resultText: String? = null

        when (selectedOperation) {
            "Transpose of a Matrix" -> {
                resultMatrix = MatrixOperations.transposeMatrix(matrixA, steps)
            }
            "Matrix Addition" -> {
                matrixB?.let { resultMatrix = MatrixOperations.addMatrices(matrixA, it, steps) }
            }
            "Matrix Subtraction" -> {
                matrixB?.let { resultMatrix = MatrixOperations.subtractMatrices(matrixA, it, steps) }
            }
            "Matrix Multiplication" -> {
                matrixB?.let { resultMatrix = MatrixOperations.multiplyMatrices(matrixA, it, steps) }
            }
            "Determinant of a Matrix" -> {
                val detVal = MatrixOperations.determinant(matrixA, steps)
                resultText = "Determinant of the matrix is: $detVal"
            }
            "Adjoint of a Matrix" -> {
                resultMatrix = MatrixOperations.adjointMatrix(matrixA, steps)
            }
            "Inverse of a Matrix (Adjoint Method)" -> {
                resultMatrix = MatrixOperations.inverseMatrixAdjointMethod(matrixA, steps)
            }
            "Inverse of a Matrix (Elementary Row Operations)" -> {
                resultMatrix = MatrixOperations.inverseMatrixElementaryOps(matrixA, steps)
            }
            "Row Echelon Form (REF)" -> {
                resultMatrix = MatrixOperations.getRowEchelonForm(matrixA, steps)
            }
            "Normal Form (Rank Form)" -> {
                resultMatrix = MatrixOperations.getNormalForm(matrixA, steps)
            }
            "Rank of a Matrix" -> {
                val rank = MatrixOperations.calculateRank(matrixA, steps)
                resultText = "Rank of the matrix is: $rank"
            }
            "Solve Linear Equations (REF Method)" -> {
                constants?.let { resultText = MatrixOperations.solveLinearEquationsRef(matrixA, it, steps) }
            }
            "Solve Linear Equations (Inverse Method)" -> {
                constants?.let { resultText = MatrixOperations.solveLinearEquationsInverse(matrixA, it, steps) }
            }
        }

        // Make result and steps cards visible and animate them
        binding.cardResult.visibility = View.VISIBLE
        binding.cardSteps.visibility = View.VISIBLE
        binding.cardResult.apply {
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
        binding.cardSteps.apply {
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setStartDelay(100)
                .start()
        }


        if (resultMatrix != null) {
            binding.textViewResult.text = MatrixOperations.formatMatrixForDisplay(resultMatrix, "Final Result:")
        } else if (resultText != null) {
            binding.textViewResult.text = resultText
        } else {
            if (binding.textViewSteps.text.contains("Error")) {
                binding.textViewResult.text = "Operation Failed."
            } else {
                binding.textViewResult.text = "Could not compute result."
            }
        }
        binding.textViewSteps.text = steps.joinToString("\n")
        binding.mainScrollView.post {
            binding.mainScrollView.smoothScrollTo(0, binding.cardResult.top)
        }

        // Save to history after successful calculation
        val currentResultJson: String = if (resultMatrix != null) {
            Gson().toJson(resultMatrix)
        } else {
            Gson().toJson(resultText ?: "Error/No Result")
        }

        val currentStepsJson: String = Gson().toJson(steps)

        val matrixAJson = Gson().toJson(matrixA)
        val matrixBJson = if (matrixB != null) Gson().toJson(matrixB) else null
        val constantsJson = if (constants != null) Gson().toJson(constants) else null


        val historyEntry = MatrixHistoryEntry(
            operationType = selectedOperation,
            matrixAJson = matrixAJson,
            matrixBJson = matrixBJson,
            constantsJson = constantsJson,
            resultJson = currentResultJson,
            stepsJson = currentStepsJson
        )

        CoroutineScope(Dispatchers.IO).launch {
            matrixHistoryDao.insert(historyEntry)
        }
    }

    private fun parseConstantsInput(input: String): Pair<List<Fraction>?, String?> {
        val elements = input.trim().split(" ").filter { it.isNotBlank() }
        if (elements.isEmpty()) {
            return Pair(null, "Error: No constants entered. Please enter space-separated numbers.")
        }
        val parsedConstants = mutableListOf<Fraction>()
        for ((i, elem) in elements.withIndex()) {
            try {
                parsedConstants.add(Fraction.parse(elem))
            } catch (e: NumberFormatException) {
                return Pair(null, "Error: Invalid number format in constant ${i + 1} ('$elem'). Please enter valid numbers or fractions (e.g., 1/2).")
            } catch (e: IllegalArgumentException) {
                return Pair(null, "Error: Invalid fraction format in constant ${i + 1} ('$elem'). ${e.message}")
            }
        }
        return Pair(parsedConstants, null)
    }
}