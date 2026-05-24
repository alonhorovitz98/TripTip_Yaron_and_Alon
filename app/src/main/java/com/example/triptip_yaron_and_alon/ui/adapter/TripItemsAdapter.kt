package com.example.triptip_yaron_and_alon.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.triptip_yaron_and_alon.databinding.ItemTripItemBinding
import com.example.triptip_yaron_and_alon.domain.model.DayItem
import com.example.triptip_yaron_and_alon.domain.model.DayItemType
import com.example.triptip_yaron_and_alon.util.loadPostImage

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
                            ivPostImage.loadPostImage(p.imageUrl, hideWhenEmpty = true)
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
