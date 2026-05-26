package com.example.triptip_yaron_and_alon.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.triptip_yaron_and_alon.databinding.ItemAvailablePostBinding
import com.example.triptip_yaron_and_alon.domain.model.Post
import com.example.triptip_yaron_and_alon.util.displayLocationName
import com.example.triptip_yaron_and_alon.util.loadPostImage

class AvailablePostsAdapter(
    private val onAddClick: (Post) -> Unit,
    private var excludedPostIds: Set<String> = emptySet()
) : ListAdapter<Post, AvailablePostsAdapter.PostViewHolder>(PostDiffCallback()) {
    
    fun updateExcludedIds(excludedIds: Set<String>) {
        excludedPostIds = excludedIds
        notifyDataSetChanged() // Refresh all items to update button visibility
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemAvailablePostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding, onAddClick)
    }
    
    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position), excludedPostIds.contains(getItem(position).id))
    }
    
    class PostViewHolder(
        private val binding: ItemAvailablePostBinding,
        private val onAddClick: (Post) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(post: Post, isExcluded: Boolean) {
            binding.apply {
                tvPostText.text = post.text
                
                ivPostImage.loadPostImage(post.imageUrl)
                
                val locationLabel = displayLocationName(post.location)
                if (locationLabel != null) {
                    tvLocation.text = "📍 $locationLabel"
                    tvLocation.visibility = View.VISIBLE
                } else {
                    tvLocation.visibility = View.GONE
                }
                
                // Add button
                if (isExcluded) {
                    btnAdd.visibility = View.GONE
                    tvAlreadyAdded.visibility = View.VISIBLE
                } else {
                    btnAdd.visibility = View.VISIBLE
                    tvAlreadyAdded.visibility = View.GONE
                    btnAdd.setOnClickListener {
                        onAddClick(post)
                    }
                }
            }
        }
    }
    
    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }
}

