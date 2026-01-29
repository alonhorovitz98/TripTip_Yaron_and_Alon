package com.example.triptip_yaron_and_alon.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.triptip_yaron_and_alon.databinding.ItemTripBinding
import com.example.triptip_yaron_and_alon.domain.model.Trip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TripAdapter(
    private val onTripClick: (Trip) -> Unit,
    private val onTripLongClick: (Trip) -> Unit
) : ListAdapter<Trip, TripAdapter.TripViewHolder>(TripDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val binding = ItemTripBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TripViewHolder(binding, onTripClick, onTripLongClick)
    }
    
    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class TripViewHolder(
        private val binding: ItemTripBinding,
        private val onTripClick: (Trip) -> Unit,
        private val onTripLongClick: (Trip) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(trip: Trip) {
            binding.apply {
                tvTripTitle.text = trip.title
                tvTripDescription.text = trip.description ?: "No description"
                tvDaysCount.text = "${trip.days.size} days"
                
                // Format dates
                if (trip.startDate != null && trip.endDate != null) {
                    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                    tvDates.text = "${dateFormat.format(Date(trip.startDate))} - ${dateFormat.format(Date(trip.endDate))}"
                } else {
                    tvDates.text = "No dates set"
                }
                
                root.setOnClickListener {
                    onTripClick(trip)
                }
                
                root.setOnLongClickListener {
                    onTripLongClick(trip)
                    true
                }
            }
        }
    }
    
    class TripDiffCallback : DiffUtil.ItemCallback<Trip>() {
        override fun areItemsTheSame(oldItem: Trip, newItem: Trip): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Trip, newItem: Trip): Boolean {
            return oldItem == newItem
        }
    }
}
