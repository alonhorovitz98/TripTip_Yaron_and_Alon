package com.example.triptip_yaron_and_alon.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.databinding.ItemAvailablePostBinding
import com.example.triptip_yaron_and_alon.domain.model.Post
import java.io.File

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
                
                // Post image: Firebase/HTTPS URLs and local file paths
                if (!post.imageUrl.isNullOrBlank()) {
                    ivPostImage.visibility = View.VISIBLE
                    val url = post.imageUrl!!
                    if (url.startsWith("http", ignoreCase = true) || url.startsWith("https", ignoreCase = true)) {
                        ivPostImage.load(url) {
                            placeholder(R.drawable.ic_launcher_background)
                            error(R.drawable.ic_launcher_background)
                        }
                    } else {
                        try {
                            val imageFile = File(url)
                            ivPostImage.load(imageFile) {
                                placeholder(R.drawable.ic_launcher_background)
                                error(R.drawable.ic_launcher_background)
                            }
                        } catch (_: Exception) {
                            ivPostImage.visibility = View.GONE
                        }
                    }
                } else {
                    ivPostImage.visibility = View.GONE
                }
                
                if (post.location != null) {
                    tvLocation.text = "📍 ${post.location}"
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

