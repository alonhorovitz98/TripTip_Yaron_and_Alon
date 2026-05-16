package com.example.triptip_yaron_and_alon.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.triptip_yaron_and_alon.databinding.SkeletonPlaceCardBinding

/**
 * Adapter for displaying skeleton/shimmer loading place cards.
 */
class SkeletonPlaceAdapter(private val itemCount: Int = 5) : RecyclerView.Adapter<SkeletonPlaceAdapter.SkeletonViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkeletonViewHolder {
        val binding = SkeletonPlaceCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SkeletonViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: SkeletonViewHolder, position: Int) {
        // Skeleton items don't need binding - they're static
    }
    
    override fun getItemCount(): Int = itemCount
    
    class SkeletonViewHolder(
        binding: SkeletonPlaceCardBinding
    ) : RecyclerView.ViewHolder(binding.root)
}
