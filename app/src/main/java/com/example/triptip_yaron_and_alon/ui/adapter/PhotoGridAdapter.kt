package com.example.triptip_yaron_and_alon.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.databinding.ItemPhotoCarouselBinding

/**
 * Adapter for displaying photos in a grid RecyclerView.
 */
class PhotoGridAdapter : ListAdapter<String, PhotoGridAdapter.PhotoViewHolder>(PhotoDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoCarouselBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class PhotoViewHolder(
        private val binding: ItemPhotoCarouselBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(photoUrl: String) {
            binding.ivPhoto.load(photoUrl) {
                placeholder(R.drawable.ic_placeholder_image)
                error(R.drawable.ic_placeholder_image)
                crossfade(true)
            }
        }
    }
    
    private class PhotoDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
        
        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}
