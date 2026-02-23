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
import java.text.DecimalFormat

/**
 * Adapter for displaying nearby places in a RecyclerView.
 * Each item shows place name, photo, address, and "Add to Trip" button.
 */
class NearbyPlaceAdapter(
    private val onAddToTripClick: (PlaceInfo) -> Unit
) : ListAdapter<PlaceInfo, NearbyPlaceAdapter.PlaceViewHolder>(PlaceDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val binding = ItemNearbyPlaceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlaceViewHolder(binding, onAddToTripClick)
    }
    
    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class PlaceViewHolder(
        private val binding: ItemNearbyPlaceBinding,
        private val onAddToTripClick: (PlaceInfo) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(place: PlaceInfo) {
            binding.apply {
                // Place name
                tvPlaceName.text = place.name
                
                // Address (use description/vicinity if available)
                tvAddress.text = place.description ?: "Location"
                
                // Distance (if available)
                if (place.distance != null) {
                    val distanceKm = place.distance / 1000.0
                    val distanceText = if (distanceKm < 1) {
                        "${DecimalFormat("#").format(place.distance)}m"
                    } else {
                        "${DecimalFormat("#.#").format(distanceKm)}km"
                    }
                    tvDistance.text = distanceText
                    tvDistance.visibility = View.VISIBLE
                } else {
                    tvDistance.visibility = View.GONE
                }
                
                // Place photo
                if (!place.imageUrl.isNullOrBlank()) {
                    ivPlacePhoto.load(place.imageUrl) {
                        placeholder(R.drawable.ic_placeholder_image)
                        error(R.drawable.ic_placeholder_image)
                        crossfade(true)
                    }
                    ivPlacePhoto.visibility = View.VISIBLE
                } else {
                    ivPlacePhoto.visibility = View.GONE
                }
                
                // Categories/Types (show first few)
                val categoriesText = place.categories.take(3).joinToString(", ")
                if (categoriesText.isNotBlank()) {
                    tvCategories.text = categoriesText
                    tvCategories.visibility = View.VISIBLE
                } else {
                    tvCategories.visibility = View.GONE
                }
                
                // Add to Trip button
                btnAddToTrip.setOnClickListener {
                    onAddToTripClick(place)
                }
            }
        }
    }
    
    private class PlaceDiffCallback : DiffUtil.ItemCallback<PlaceInfo>() {
        override fun areItemsTheSame(oldItem: PlaceInfo, newItem: PlaceInfo): Boolean {
            return oldItem.xid == newItem.xid
        }
        
        override fun areContentsTheSame(oldItem: PlaceInfo, newItem: PlaceInfo): Boolean {
            return oldItem == newItem
        }
    }
}
