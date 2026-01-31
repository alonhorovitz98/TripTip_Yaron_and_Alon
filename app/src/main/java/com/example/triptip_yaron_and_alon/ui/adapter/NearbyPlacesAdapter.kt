package com.example.triptip_yaron_and_alon.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.databinding.ItemNearbyPlaceBinding
import com.example.triptip_yaron_and_alon.domain.model.PlaceInfo

class NearbyPlacesAdapter(
    private val onPlaceClick: (PlaceInfo) -> Unit
) : ListAdapter<PlaceInfo, NearbyPlacesAdapter.PlaceViewHolder>(PlaceDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val binding = ItemNearbyPlaceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlaceViewHolder(binding, onPlaceClick)
    }
    
    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class PlaceViewHolder(
        private val binding: ItemNearbyPlaceBinding,
        private val onPlaceClick: (PlaceInfo) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(place: PlaceInfo) {
            binding.apply {
                // Place name
                tvPlaceName.text = place.name
                
                // Description
                if (place.description != null && place.description.isNotBlank()) {
                    tvPlaceDescription.text = place.description
                    tvPlaceDescription.visibility = View.VISIBLE
                } else {
                    tvPlaceDescription.visibility = View.GONE
                }
                
                // Categories
                if (place.categories.isNotEmpty()) {
                    tvPlaceCategory.text = place.categories.joinToString(", ")
                    tvPlaceCategory.visibility = View.VISIBLE
                } else {
                    tvPlaceCategory.visibility = View.GONE
                }
                
                // Distance (if available)
                if (place.distance != null) {
                    val distanceKm = place.distance / 1000.0
                    tvPlaceDistance.text = if (distanceKm < 1) {
                        "${(place.distance).toInt()}m"
                    } else {
                        String.format("%.1f km", distanceKm)
                    }
                    tvPlaceDistance.visibility = View.VISIBLE
                } else {
                    tvPlaceDistance.visibility = View.GONE
                }
                
                // Place image
                if (place.imageUrl != null) {
                    ivPlaceImage.visibility = View.VISIBLE
                    ivPlaceImage.load(place.imageUrl) {
                        placeholder(R.drawable.ic_launcher_background)
                        error(R.drawable.ic_launcher_background)
                    }
                } else {
                    ivPlaceImage.visibility = View.GONE
                }
                
                // Click listener
                root.setOnClickListener {
                    onPlaceClick(place)
                }
            }
        }
    }
    
    class PlaceDiffCallback : DiffUtil.ItemCallback<PlaceInfo>() {
        override fun areItemsTheSame(oldItem: PlaceInfo, newItem: PlaceInfo): Boolean {
            return oldItem.xid == newItem.xid
        }
        
        override fun areContentsTheSame(oldItem: PlaceInfo, newItem: PlaceInfo): Boolean {
            return oldItem == newItem
        }
    }
}

