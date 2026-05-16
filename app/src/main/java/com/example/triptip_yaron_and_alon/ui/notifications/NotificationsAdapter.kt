package com.example.triptip_yaron_and_alon.ui.notifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.data.remote.firebase.NotificationsDataSource
import com.example.triptip_yaron_and_alon.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationsAdapter(
    private val onNotificationClick: (NotificationsDataSource.NotificationDoc) -> Unit
) : ListAdapter<NotificationsDataSource.NotificationDoc, NotificationsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onNotificationClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemNotificationBinding,
        private val onNotificationClick: (NotificationsDataSource.NotificationDoc) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: NotificationsDataSource.NotificationDoc) {
            binding.tvMessage.text = notification.message.ifBlank {
                when (notification.type) {
                    NotificationsDataSource.TYPE_COMMENT ->
                        "${notification.actorUserName.ifBlank { "Someone" }} commented on your post"
                    else ->
                        "${notification.actorUserName.ifBlank { "Someone" }} liked your post"
                }
            }
            binding.tvTime.text = formatTime(notification.createdAt)

            val iconRes = when (notification.type) {
                NotificationsDataSource.TYPE_COMMENT -> R.drawable.ic_comment
                else -> R.drawable.ic_heart
            }
            binding.ivTypeIcon.setImageResource(iconRes)

            binding.unreadDot.visibility = if (notification.isRead) android.view.View.GONE else android.view.View.VISIBLE
            // Subtle background tint when unread
            val bgColor = if (notification.isRead) {
                androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.background_white)
            } else {
                // very light orange tint — fallback to white if color missing
                try {
                    androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.orange_light)
                } catch (_: Exception) {
                    androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.background_white)
                }
            }
            binding.cardRoot.setCardBackgroundColor(bgColor)

            binding.root.setOnClickListener { onNotificationClick(notification) }
        }

        private fun formatTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            return when {
                diff < 60_000 -> "Just now"
                diff < 3600_000 -> "${diff / 60_000} min ago"
                diff < 86400_000 -> {
                    val hours = diff / 3600_000
                    if (hours == 1L) "1 hour ago" else "$hours hours ago"
                }
                diff < 7 * 86400_000L -> {
                    val days = diff / 86400_000L
                    if (days == 1L) "Yesterday" else "$days days ago"
                }
                else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<NotificationsDataSource.NotificationDoc>() {
        override fun areItemsTheSame(
            old: NotificationsDataSource.NotificationDoc,
            new: NotificationsDataSource.NotificationDoc
        ) = old.id == new.id

        override fun areContentsTheSame(
            old: NotificationsDataSource.NotificationDoc,
            new: NotificationsDataSource.NotificationDoc
        ) = old == new
    }
}
