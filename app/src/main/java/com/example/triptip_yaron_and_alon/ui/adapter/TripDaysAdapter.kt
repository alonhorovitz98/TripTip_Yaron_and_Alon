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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TripDaysAdapter(
    private val onDayClick: (TripDay) -> Unit,
    private val onDayDateClick: ((TripDay) -> Unit)? = null
) : ListAdapter<TripDay, TripDaysAdapter.DayViewHolder>(DayDiffCallback()) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemTripDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DayViewHolder(binding, onDayClick, onDayDateClick, dateFormat)
    }
    
    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class DayViewHolder(
        private val binding: ItemTripDayBinding,
        private val onDayClick: (TripDay) -> Unit,
        private val onDayDateClick: ((TripDay) -> Unit)?,
        private val dateFormat: SimpleDateFormat
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(day: TripDay) {
            binding.apply {
                tvDayNumber.text = "Day ${day.dayNumber}"
                tvDayDate.text = day.date?.let { dateFormat.format(Date(it)) } ?: "Tap to set date"
                tvDayDate.setOnClickListener {
                    onDayDateClick?.invoke(day)
                }
                
                // Set activity count
                val count = day.items.size
                tvActivityCount.text = "${count} ${if (count == 1) "item" else "items"}"
                
                // Set city name (use first item's location or default)
                val cityName = day.items.firstOrNull()?.post?.location ?: "Untitled Day"
                tvCityName.text = cityName
                
                // Load thumbnail (first item's image)
                val firstItemImage = day.items.firstOrNull()?.post?.imageUrl
                if (!firstItemImage.isNullOrBlank()) {
                    ivDayThumbnail.visibility = View.VISIBLE
                    ivDayThumbnail.load(firstItemImage) {
                        placeholder(R.drawable.ic_placeholder_image)
                        error(R.drawable.ic_placeholder_image)
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

