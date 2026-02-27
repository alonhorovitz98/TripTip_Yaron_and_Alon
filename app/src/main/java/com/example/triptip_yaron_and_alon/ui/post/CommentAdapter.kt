package com.example.triptip_yaron_and_alon.ui.post

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.databinding.ItemCommentBinding
import com.example.triptip_yaron_and_alon.domain.model.Comment
import java.io.File

class CommentAdapter : ListAdapter<Comment, CommentAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(comment: Comment) {
            binding.tvCommentAuthor.text = comment.userName.ifBlank { "User" }
            binding.tvCommentText.text = comment.text
            if (!comment.userAvatarUrl.isNullOrBlank()) {
                binding.ivCommentAvatar.load(comment.userAvatarUrl) {
                    placeholder(R.drawable.ic_profile_frame)
                    error(R.drawable.ic_profile_frame)
                }
            } else {
                binding.ivCommentAvatar.setImageResource(R.drawable.ic_profile_frame)
            }
            if (!comment.imageUrl.isNullOrBlank()) {
                binding.ivCommentImage.visibility = View.VISIBLE
                if (comment.imageUrl!!.startsWith("http", ignoreCase = true)) {
                    binding.ivCommentImage.load(comment.imageUrl) {
                        placeholder(R.drawable.ic_launcher_background)
                        error(R.drawable.ic_launcher_background)
                    }
                } else {
                    try {
                        binding.ivCommentImage.load(File(comment.imageUrl)) {
                            placeholder(R.drawable.ic_launcher_background)
                            error(R.drawable.ic_launcher_background)
                        }
                    } catch (e: Exception) {
                        binding.ivCommentImage.visibility = View.GONE
                    }
                }
            } else {
                binding.ivCommentImage.visibility = View.GONE
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(old: Comment, new: Comment) = old.id == new.id
        override fun areContentsTheSame(old: Comment, new: Comment) = old == new
    }
}
