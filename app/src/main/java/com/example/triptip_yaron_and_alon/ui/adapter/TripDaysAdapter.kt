package com.example.triptip_yaron_and_alon.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.databinding.ItemTripDayBinding
import com.example.triptip_yaron_and_alon.domain.model.TripDay
import java.io.File

class TripDaysAdapter(
    private val onDayClick: (TripDay) -> Unit
) : ListAdapter<TripDay, TripDaysAdapter.DayViewHolder>(DayDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemTripDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DayViewHolder(binding, onDayClick)
    }
    
    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class DayViewHolder(
        private val binding: ItemTripDayBinding,
        private val onDayClick: (TripDay) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(day: TripDay) {
            binding.apply {
                tvDayNumber.text = "Day ${day.dayNumber}"
                
                // Set activity count
                val count = day.items.size
                tvActivityCount.text = "${count} ${if (count == 1) "item" else "items"}"
                
                // Set city name (from day description)
                tvCityName.text = day.description.ifEmpty { "Untitled Day" }
                
                // Load thumbnail (first item's image)
                val firstItemImage = day.items.firstOrNull()?.post?.imageUrl
                if (firstItemImage != null) {
                    ivDayThumbnail.visibility = View.VISIBLE
                    try {
                        val imageFile = File(firstItemImage)
                        ivDayThumbnail.load(imageFile) {
                            placeholder(R.drawable.ic_placeholder_image)
                            error(R.drawable.ic_placeholder_image)
                        }
                    } catch (e: Exception) {
                        ivDayThumbnail.visibility = View.GONE
                    }
                } else {
                    ivDayThumbnail.visibility = View.GONE
                }
                
                root.setOnClickListener {
                    onDayClick(day)
                }
            }
        }
    }
    
    class DayDiffCallback : DiffUtil.ItemCallback<TripDay>() {
        override fun areItemsTheSame(oldItem: TripDay, newItem: TripDay): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: TripDay, newItem: TripDay): Boolean {
            return oldItem == newItem
        }
    }
}

