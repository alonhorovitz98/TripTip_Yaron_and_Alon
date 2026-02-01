package com.example.triptip_yaron_and_alon.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.databinding.ItemMyPostBinding
import com.example.triptip_yaron_and_alon.domain.model.Post
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyPostsAdapter(
    private val onPostClick: (Post) -> Unit,
    private val onEditClick: (Post) -> Unit,
    private val onDeleteClick: (Post) -> Unit
) : ListAdapter<Post, MyPostsAdapter.MyPostViewHolder>(PostDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyPostViewHolder {
        val binding = ItemMyPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MyPostViewHolder(binding, onPostClick, onEditClick, onDeleteClick)
    }
    
    override fun onBindViewHolder(holder: MyPostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class MyPostViewHolder(
        private val binding: ItemMyPostBinding,
        private val onPostClick: (Post) -> Unit,
        private val onEditClick: (Post) -> Unit,
        private val onDeleteClick: (Post) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(post: Post) {
            binding.apply {
                // Post text
                tvPostText.text = post.text
                
                // Username
                tvUsername.text = "User ${post.userId.take(8)}"
                
                // Timestamp
                tvTimestamp.text = formatTimestamp(post.createdAt)
                
                // Post image
                if (post.imageUrl != null) {
                    ivPostImage.visibility = View.VISIBLE
                    ivPostImage.load(post.imageUrl) {
                        placeholder(R.drawable.ic_launcher_background)
                        error(R.drawable.ic_launcher_background)
                    }
                } else {
                    ivPostImage.visibility = View.GONE
                }
                
                // Location
                if (post.location != null) {
                    tvLocation.visibility = View.VISIBLE
                    tvLocation.text = post.location
                } else {
                    tvLocation.visibility = View.GONE
                }
                
                // User profile image (placeholder for now)
                ivUserProfile.load(R.drawable.ic_launcher_foreground)
                
                // Click listeners
                root.setOnClickListener {
                    onPostClick(post)
                }
                
                btnEdit.setOnClickListener {
                    onEditClick(post)
                }
                
                btnDelete.setOnClickListener {
                    onDeleteClick(post)
                }
            }
        }
        
        private fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            return when {
                diff < 60000 -> "Just now"
                diff < 3600000 -> "${diff / 60000} minutes ago"
                diff < 86400000 -> "${diff / 3600000} hours ago"
                diff < 604800000 -> "${diff / 86400000} days ago"
                else -> {
                    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    sdf.format(Date(timestamp))
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

