package com.example.triptip_yaron_and_alon.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.databinding.ItemPostBinding
import com.example.triptip_yaron_and_alon.domain.model.Post
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostAdapter(
    private val onPostClick: (Post) -> Unit
) : ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding, onPostClick)
    }
    
    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class PostViewHolder(
        private val binding: ItemPostBinding,
        private val onPostClick: (Post) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(post: Post) {
            binding.apply {
                // Post text
                tvPostText.text = post.text
                
                // Username (you'll need to fetch from userId later)
                tvUsername.text = "User ${post.userId.take(8)}"
                
                // Timestamp
                tvTimestamp.text = formatTimestamp(post.createdAt)
                
                // Post image - Coil handles file errors gracefully
                if (post.imageUrl != null) {
                    ivPostImage.visibility = View.VISIBLE
                    try {
                        val imageFile = File(post.imageUrl)
                        ivPostImage.load(imageFile) {
                            placeholder(R.drawable.ic_launcher_background)
                            error(R.drawable.ic_launcher_background)
                            // Coil will handle missing files automatically
                        }
                    } catch (e: Exception) {
                        // If file path is invalid, hide image view
                        ivPostImage.visibility = View.GONE
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
                
                // Click listener
                root.setOnClickListener {
                    onPostClick(post)
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
