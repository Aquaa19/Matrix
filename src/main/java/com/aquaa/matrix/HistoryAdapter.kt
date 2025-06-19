package com.aquaa.matrix

import android.animation.Animator // Import Animator
import android.animation.AnimatorListenerAdapter // Import AnimatorListenerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.RotateAnimation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.matrix.data.MatrixHistoryEntry
import com.aquaa.matrix.databinding.ItemHistoryEntryBinding
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.aquaa.matrix.MatrixOperations // For formatMatrixForDisplay
import androidx.transition.TransitionManager // Import TransitionManager
import android.view.animation.AccelerateDecelerateInterpolator // Import AccelerateDecelerateInterpolator
import androidx.transition.AutoTransition // NEW: Import AutoTransition

/**
 * RecyclerView Adapter for displaying MatrixHistoryEntry items.
 * Uses ListAdapter for efficient item updates.
 */
class HistoryAdapter : ListAdapter<MatrixHistoryEntry, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    // Set to keep track of expanded item IDs
    private val expandedItemIds: MutableSet<Long> = mutableSetOf()

    // Inner ViewHolder class to hold references to the views for each item
    inner class HistoryViewHolder(private val binding: ItemHistoryEntryBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            // Set click listener on the entire item to toggle expand/collapse
            binding.historyItemRootLayout.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position)
                    // Create a custom transition with desired duration
                    val transition = AutoTransition().setDuration(300)
                    TransitionManager.beginDelayedTransition(binding.historyItemRootLayout.parent as ViewGroup, transition)
                    toggleExpandedState(item.id, position)
                }
            }
        }

        // Bind data from a MatrixHistoryEntry to the views in the item layout
        fun bind(entry: MatrixHistoryEntry) {
            val isExpanded = expandedItemIds.contains(entry.id)

            // Collapsed View (Always Visible Summary Part)
            binding.textViewOperationType.text = "Operation: ${entry.operationType}"
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            binding.textViewTimestamp.text = "Time: ${dateFormat.format(entry.timestamp)}"

            // Display Matrix A for collapsed view (truncated) OR hide if expanded
            val matrixAType = object : TypeToken<List<List<Fraction>>>() {}.type
            val matrixA = Gson().fromJson<List<List<Fraction>>>(entry.matrixAJson, matrixAType)

            if (!isExpanded) {
                binding.textViewMatrixACollapsed.visibility = View.VISIBLE
                binding.textViewMatrixACollapsed.text = MatrixOperations.formatMatrixForDisplay(matrixA ?: emptyList(), "Matrix A:")
            } else {
                binding.textViewMatrixACollapsed.visibility = View.GONE
            }

            // Expanded View (Visibility toggled)
            binding.expandedDetailsLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE

            // Rotate arrow based on state
            val fromDegrees = if (isExpanded) 0f else 180f
            val toDegrees = if (isExpanded) 180f else 0f
            val rotateAnimation = RotateAnimation(
                fromDegrees,
                toDegrees,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 200
                fillAfter = true
            }
            binding.imageViewExpandArrow.startAnimation(rotateAnimation)


            if (isExpanded) {
                // Populate expanded details only when expanded

                // Display full Matrix A in expanded view
                binding.textViewMatrixAExpanded.text = MatrixOperations.formatMatrixForDisplay(matrixA ?: emptyList(), "Matrix A (Full):")


                // Display Matrix B if present
                if (!entry.matrixBJson.isNullOrEmpty()) {
                    binding.textViewMatrixB.visibility = View.VISIBLE
                    val matrixBType = object : TypeToken<List<List<Fraction>>>() {}.type
                    val matrixB = Gson().fromJson<List<List<Fraction>>>(entry.matrixBJson, matrixBType)
                    binding.textViewMatrixB.text = MatrixOperations.formatMatrixForDisplay(matrixB ?: emptyList(), "Matrix B:")
                } else {
                    binding.textViewMatrixB.visibility = View.GONE
                }

                // Display Constants if present
                if (!entry.constantsJson.isNullOrEmpty()) {
                    binding.textViewConstants.visibility = View.VISIBLE
                    val constantsType = object : TypeToken<List<Fraction>>() {}.type
                    val constants = Gson().fromJson<List<Fraction>>(entry.constantsJson, constantsType)
                    binding.textViewConstants.text = "Constants:\n${constants?.joinToString(" ")}"
                } else {
                    binding.textViewConstants.visibility = View.GONE
                }

                // Display Result (Expanded view)
                try {
                    val resultMatrixType = object : TypeToken<List<List<Fraction>>>() {}.type
                    val resultMatrix = Gson().fromJson<List<List<Fraction>>>(entry.resultJson, resultMatrixType)
                    if (resultMatrix != null && resultMatrix.isNotEmpty() && resultMatrix[0].isNotEmpty()) {
                        binding.textViewResultExpanded.text = MatrixOperations.formatMatrixForDisplay(resultMatrix, "Result:")
                    } else {
                        binding.textViewResultExpanded.text = "Result:\n${entry.resultJson.replace("\"", "")}"
                    }
                } catch (e: Exception) {
                    binding.textViewResultExpanded.text = "Result:\n${entry.resultJson.replace("\"", "")}"
                }

                // Display Steps
                val stepsListType = object : TypeToken<List<String>>() {}.type
                val stepsList = Gson().fromJson<List<String>>(entry.stepsJson, stepsListType)
                binding.textViewSteps.text = "Steps:\n${stepsList?.joinToString("\n")}"
            }
        }
    }

    // Called when RecyclerView needs a new ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.root.alpha = 0f // Set initial alpha to 0 to prevent flash
        return HistoryViewHolder(binding)
    }

    // Called by RecyclerView to display the data at the specified position
    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // Apply entrance animation when view is attached to window
    override fun onViewAttachedToWindow(holder: HistoryViewHolder) {
        super.onViewAttachedToWindow(holder)
        val position = holder.bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION) {
            holder.itemView.post {
                val translationX = if (position % 2 == 0) -holder.itemView.width.toFloat() else holder.itemView.width.toFloat()
                holder.itemView.translationX = translationX
                // holder.itemView.alpha = 0f; // This is set in onCreateViewHolder
                holder.itemView.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(500) // Increased duration for smoother slide-in
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .setStartDelay((position * 50).toLong())
                    .start()
            }
        }
    }

    // NEW: Graceful exit animation when view is detached for recycling
    override fun onViewDetachedFromWindow(holder: HistoryViewHolder) {
        super.onViewDetachedFromWindow(holder)
        val position = holder.bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION) {
            // Determine the direction to slide out based on its original entrance
            val exitTranslationX = if (position % 2 == 0) -holder.itemView.width.toFloat() else holder.itemView.width.toFloat()

            holder.itemView.animate()
                .translationX(exitTranslationX) // Slide back in the direction it came from
                .alpha(0f)                       // Fade out
                .setDuration(300)                // Duration for exit animation
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        // Reset properties after animation ends to ensure clean recycling state
                        holder.itemView.translationX = 0f // Reset for next use
                        holder.itemView.alpha = 1f        // Reset alpha to 1 for next use, will be set to 0 in onCreateViewHolder
                        holder.itemView.clearAnimation() // Clear any ongoing animation
                    }
                })
                .start()
        }
    }

    // Function to toggle expanded state and notify adapter
    private fun toggleExpandedState(itemId: Long, position: Int) {
        if (expandedItemIds.contains(itemId)) {
            expandedItemIds.remove(itemId)
        } else {
            expandedItemIds.add(itemId)
        }
        notifyItemChanged(position) // Notify that this item's data has changed
    }


    /**
     * DiffUtil.ItemCallback to efficiently calculate differences between two lists.
     */
    private class HistoryDiffCallback : DiffUtil.ItemCallback<MatrixHistoryEntry>() {
        override fun areItemsTheSame(oldItem: MatrixHistoryEntry, newItem: MatrixHistoryEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MatrixHistoryEntry, newItem: MatrixHistoryEntry): Boolean {
            return oldItem == newItem
        }
    }
}