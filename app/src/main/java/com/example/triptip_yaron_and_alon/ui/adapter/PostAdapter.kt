package com.example.triptip_yaron_and_alon.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
    private val onPostClick: (Post) -> Unit,
    private val onLikeClick: ((Post) -> Unit)? = null,
    currentUserId: String? = null
) : ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {
    
    private var _currentUserId: String? = currentUserId
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }
    
    fun setCurrentUserId(id: String?) {
        _currentUserId = id
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding, onPostClick, onLikeClick) { _currentUserId }
    }
    
    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class PostViewHolder(
        private val binding: ItemPostBinding,
        private val onPostClick: (Post) -> Unit,
        private val onLikeClick: ((Post) -> Unit)?,
        private val currentUserIdProvider: () -> String?
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(post: Post) {
            val currentUserId = currentUserIdProvider()
            val liked = currentUserId != null && post.likedBy.contains(currentUserId)
            binding.apply {
                // Post text
                tvPostText.text = post.text
                
                // Username and profile image from post (set at create from current user)
                tvUsername.text = post.userName.ifBlank { "User ${post.userId.take(8)}" }
                
                // Timestamp
                tvTimestamp.text = formatTimestamp(post.createdAt)
                
                // Post image (URL or local file path)
                if (!post.imageUrl.isNullOrBlank()) {
                    ivPostImage.visibility = View.VISIBLE
                    val isUrl = post.imageUrl.startsWith("http", ignoreCase = true)
                    if (isUrl) {
                        ivPostImage.load(post.imageUrl) {
                            placeholder(R.drawable.ic_launcher_background)
                            error(R.drawable.ic_launcher_background)
                        }
                    } else {
                        try {
                            ivPostImage.load(File(post.imageUrl)) {
                                placeholder(R.drawable.ic_launcher_background)
                                error(R.drawable.ic_launcher_background)
                            }
                        } catch (e: Exception) {
                            ivPostImage.visibility = View.GONE
                        }
                    }
                } else {
                    ivPostImage.visibility = View.GONE
                }
                
                // Location tag overlay (on image)
                if (post.location != null && post.imageUrl != null) {
                    locationTag.visibility = View.VISIBLE
                    tvLocation.text = post.location
                } else {
                    locationTag.visibility = View.GONE
                }
                
                // User profile image (from post or placeholder)
                if (!post.userImageUrl.isNullOrBlank()) {
                    ivUserProfile.load(post.userImageUrl) {
                        placeholder(R.drawable.ic_profile_frame)
                        error(R.drawable.ic_profile_frame)
                    }
                } else {
                    ivUserProfile.load(R.drawable.ic_profile_frame) {
                        placeholder(R.drawable.ic_profile_frame)
                        error(R.drawable.ic_profile_frame)
                    }
                }
                
                // Like count (real data from DB)
                tvLikeCount.text = when (val n = post.likes) {
                    0 -> ""
                    1 -> "1"
                    in 2..999 -> n.toString()
                    else -> "${n / 1000}.${(n % 1000) / 100}k"
                }
                tvCommentCount.text = when (val n = post.commentCount) {
                    0 -> "0"
                    in 1..999 -> n.toString()
                    else -> "${n / 1000}k"
                }
                
                // Like button state (filled when current user liked)
                btnLike.setImageResource(
                    if (liked) R.drawable.ic_heart_filled else R.drawable.ic_heart
                )
                btnLike.setColorFilter(
                    ContextCompat.getColor(binding.root.context, R.color.orange_primary)
                )
                // Engagement: like (wired in Fragment via onLikeClick)
                btnLike.setOnClickListener { onLikeClick?.invoke(post) }
                
                btnComment.setOnClickListener {
                    // Navigate to post details (which has comments)
                    onPostClick(post)
                }
                
                btnShare.setOnClickListener {
                    // TODO: Implement share functionality
                }
                
                btnBookmark.setOnClickListener {
                    // TODO: Implement bookmark functionality
                }
                
                btnMenu.setOnClickListener {
                    // TODO: Show post menu (edit/delete if owner)
                }
                
                // Click listener on card
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
