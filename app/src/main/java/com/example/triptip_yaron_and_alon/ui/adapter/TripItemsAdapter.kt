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
import com.example.triptip_yaron_and_alon.domain.model.DayItem
import com.example.triptip_yaron_and_alon.domain.model.DayItemType
import java.io.File

class TripItemsAdapter(
    private val onDelete: (DayItem) -> Unit
) : ListAdapter<DayItem, TripItemsAdapter.ItemViewHolder>(ItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemTripItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ItemViewHolder(binding, onDelete)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ItemViewHolder(
        private val binding: ItemTripItemBinding,
        private val onDelete: (DayItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DayItem) {
            binding.apply {
                tilNotes.visibility = View.GONE
                llReorder.visibility = View.GONE

                when (item.type) {
                    DayItemType.POST -> {
                        val p = item.post
                        if (p != null) {
                            tvPostText.text = p.text
                            val imageUrl = p.imageUrl
                            if (imageUrl != null) {
                                ivPostImage.visibility = View.VISIBLE
                                if (imageUrl.startsWith("http", ignoreCase = true)) {
                                    ivPostImage.load(imageUrl) {
                                        placeholder(R.drawable.ic_launcher_background)
                                        error(R.drawable.ic_launcher_background)
                                    }
                                } else {
                                    try {
                                        ivPostImage.load(File(imageUrl)) {
                                            placeholder(R.drawable.ic_launcher_background)
                                            error(R.drawable.ic_launcher_background)
                                        }
                                    } catch (_: Exception) {
                                        ivPostImage.visibility = View.GONE
                                    }
                                }
                            } else {
                                ivPostImage.visibility = View.GONE
                            }
                        } else {
                            tvPostText.text = "Post (loading…)"
                            ivPostImage.visibility = View.GONE
                        }
                    }
                    DayItemType.PLACE -> {
                        tvPostText.text = item.value
                        ivPostImage.visibility = View.GONE
                    }
                    else -> {
                        tvPostText.text = item.value
                        ivPostImage.visibility = View.GONE
                    }
                }

                btnDelete.setOnClickListener { onDelete(item) }
            }
        }
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<DayItem>() {
        override fun areItemsTheSame(oldItem: DayItem, newItem: DayItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DayItem, newItem: DayItem): Boolean {
            return oldItem == newItem
        }
    }
}
