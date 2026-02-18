package com.example.triptip_yaron_and_alon.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.databinding.ItemTripItemBinding
import com.example.triptip_yaron_and_alon.domain.model.TripItem
import java.io.File

class TripItemsAdapter(
    private val onNotesChanged: (TripItem, String?) -> Unit,
    private val onDelete: (TripItem) -> Unit,
    private val onMoveUp: (TripItem) -> Unit,
    private val onMoveDown: (TripItem) -> Unit
) : ListAdapter<TripItem, TripItemsAdapter.ItemViewHolder>(ItemDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemTripItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ItemViewHolder(binding, onNotesChanged, onDelete, onMoveUp, onMoveDown)
    }
    
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position), position, itemCount)
    }
    
    class ItemViewHolder(
        private val binding: ItemTripItemBinding,
        private val onNotesChanged: (TripItem, String?) -> Unit,
        private val onDelete: (TripItem) -> Unit,
        private val onMoveUp: (TripItem) -> Unit,
        private val onMoveDown: (TripItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: TripItem, position: Int, totalItems: Int) {
            binding.apply {
                // Post text
                tvPostText.text = item.post?.text ?: "Loading post..."
                
                // Post image - Coil handles file errors gracefully
                val imageUrl = item.post?.imageUrl
                if (imageUrl != null) {
                    ivPostImage.visibility = View.VISIBLE
                    try {
                        val imageFile = File(imageUrl)
                        ivPostImage.load(imageFile) {
                            placeholder(R.drawable.ic_launcher_background)
                            error(R.drawable.ic_launcher_background)
                            // Coil will handle missing files automatically
                        }
                    } catch (e: Exception) {
                        // If file path is invalid, hide image view
                        ivPostImage.visibility = View.GONE
                    }
                } else {
                    ivPostImage.visibility = View.GONE
                }
                
                // Notes
                etNotes.setText(item.notes)
                etNotes.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        onNotesChanged(item, etNotes.text.toString().trim().takeIf { it.isNotBlank() })
                    }
                }
                
                // Delete button
                btnDelete.setOnClickListener {
                    onDelete(item)
                }
                
                // Reorder buttons
                btnMoveUp.visibility = if (position > 0) View.VISIBLE else View.GONE
                btnMoveUp.setOnClickListener {
                    onMoveUp(item)
                }
                
                btnMoveDown.visibility = if (position < totalItems - 1) View.VISIBLE else View.GONE
                btnMoveDown.setOnClickListener {
                    onMoveDown(item)
                }
            }
        }
    }
    
    class ItemDiffCallback : DiffUtil.ItemCallback<TripItem>() {
        override fun areItemsTheSame(oldItem: TripItem, newItem: TripItem): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: TripItem, newItem: TripItem): Boolean {
            return oldItem == newItem
        }
    }
}

