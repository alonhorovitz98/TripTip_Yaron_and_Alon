package com.example.triptip_yaron_and_alon.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.domain.model.TripDay

/**
 * Adapter for displaying trip days in a RecyclerView.
 * Uses DiffUtil for efficient list updates.
 */
class TripDayAdapter(
    private val onDayClick: (TripDay) -> Unit
) : ListAdapter<TripDay, TripDayAdapter.TripDayViewHolder>(TripDayDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripDayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip_day, parent, false)
        return TripDayViewHolder(view, onDayClick)
    }

    override fun onBindViewHolder(holder: TripDayViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TripDayViewHolder(
        itemView: View,
        private val onDayClick: (TripDay) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val ivDayThumbnail: ImageView = itemView.findViewById(R.id.ivDayThumbnail)
        private val tvDayNumber: TextView = itemView.findViewById(R.id.tvDayNumber)
        private val tvCityName: TextView = itemView.findViewById(R.id.tvCityName)
        private val tvActivityCount: TextView = itemView.findViewById(R.id.tvActivityCount)

        fun bind(tripDay: TripDay) {
            // Set day number
            tvDayNumber.text = "Day ${tripDay.dayNumber}"
            
            // Set city/description
            tvCityName.text = tripDay.description.ifEmpty { "Untitled Day" }
            
            // Set activity count
            val count = tripDay.items.size
            tvActivityCount.text = itemView.context.getString(
                R.string.activity_count,
                count
            )
            
            // Load thumbnail (first item's image or placeholder)
            val firstItemImage = tripDay.items.firstOrNull()?.imageUrl
            ivDayThumbnail.load(firstItemImage) {
                crossfade(true)
                placeholder(R.drawable.ic_placeholder_image)
                error(R.drawable.ic_placeholder_image)
            }
            
            // Click listener
            itemView.setOnClickListener {
                onDayClick(tripDay)
            }
        }
    }

    class TripDayDiffCallback : DiffUtil.ItemCallback<TripDay>() {
        override fun areItemsTheSame(oldItem: TripDay, newItem: TripDay): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TripDay, newItem: TripDay): Boolean {
            return oldItem == newItem
        }
    }
}
