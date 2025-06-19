package com.aquaa.matrix

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.aquaa.matrix.data.AppDatabase
import com.aquaa.matrix.data.MatrixHistoryEntry
import com.aquaa.matrix.databinding.ActivityHistoryBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var historyAdapter: HistoryAdapter // We will create this in the next step

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enable the back button

        // Initialize RecyclerView
        historyAdapter = HistoryAdapter()
        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = historyAdapter
        }

        loadHistory()
    }

    private fun loadHistory() {
        CoroutineScope(Dispatchers.IO).launch {
            val historyEntries = AppDatabase.getDatabase(this@HistoryActivity).matrixHistoryDao().getAllHistoryEntries()

            withContext(Dispatchers.Main) {
                if (historyEntries.isEmpty()) {
                    binding.recyclerViewHistory.visibility = View.GONE
                    binding.textViewNoHistory.visibility = View.VISIBLE
                } else {
                    binding.recyclerViewHistory.visibility = View.VISIBLE
                    binding.textViewNoHistory.visibility = View.GONE
                    historyAdapter.submitList(historyEntries) // Update adapter with data
                }
            }
        }
    }

    // Handle back button click on the toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}