package com.example.triptip_yaron_and_alon.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.databinding.ItemReviewBinding
import com.example.triptip_yaron_and_alon.data.remote.api.dto.ReviewDto

/**
 * Adapter for displaying place reviews in a RecyclerView.
 */
class ReviewAdapter : ListAdapter<ReviewDto, ReviewAdapter.ReviewViewHolder>(ReviewDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReviewViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ReviewViewHolder(
        private val binding: ItemReviewBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(review: ReviewDto) {
            binding.apply {
                // Author name
                tvAuthorName.text = review.authorName
                
                // Rating (show stars)
                val rating = review.rating
                tvRating.text = "⭐".repeat(rating) + "☆".repeat(5 - rating)
                
                // Review text
                if (!review.text.isNullOrBlank()) {
                    tvReviewText.text = review.text
                    tvReviewText.visibility = View.VISIBLE
                } else {
                    tvReviewText.visibility = View.GONE
                }
                
                // Time
                tvTime.text = review.relativeTimeDescription
                
                // Profile photo
                if (!review.profilePhotoUrl.isNullOrBlank()) {
                    ivProfilePhoto.load(review.profilePhotoUrl) {
                        placeholder(R.drawable.ic_profile)
                        error(R.drawable.ic_profile)
                        crossfade(true)
                    }
                    ivProfilePhoto.visibility = View.VISIBLE
                } else {
                    ivProfilePhoto.visibility = View.GONE
                }
            }
        }
    }
    
    private class ReviewDiffCallback : DiffUtil.ItemCallback<ReviewDto>() {
        override fun areItemsTheSame(oldItem: ReviewDto, newItem: ReviewDto): Boolean {
            // Use author name + time as unique identifier
            return oldItem.authorName == newItem.authorName && oldItem.time == newItem.time
        }
        
        override fun areContentsTheSame(oldItem: ReviewDto, newItem: ReviewDto): Boolean {
            return oldItem == newItem
        }
    }
}
