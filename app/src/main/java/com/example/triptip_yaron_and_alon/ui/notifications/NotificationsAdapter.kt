package com.example.triptip_yaron_and_alon.ui.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
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
            binding.tvMessage.text = notification.message
            binding.tvTime.text = formatTime(notification.createdAt)
            binding.root.setOnClickListener { onNotificationClick(notification) }

            // Icon by type
            binding.ivNotificationIcon.setImageResource(
                when (notification.type) {
                    NotificationsDataSource.TYPE_COMMENT -> R.drawable.ic_comment
                    else -> R.drawable.ic_heart          // LIKE and anything else
                }
            )

            // Unread dot
            binding.viewUnreadDot.visibility =
                if (notification.isRead) View.GONE else View.VISIBLE

            // Card background: soft orange tint for unread, white for read
            val bgColor = if (notification.isRead) {
                ContextCompat.getColor(binding.root.context, R.color.background_white)
            } else {
                ContextCompat.getColor(binding.root.context, R.color.orange_light)
            }
            binding.cardNotification.setCardBackgroundColor(bgColor)
        }

        private fun formatTime(timestamp: Long): String {
            val diff = System.currentTimeMillis() - timestamp
            return when {
                diff < 60_000 -> "Just now"
                diff < 3_600_000 -> "${diff / 60_000} min ago"
                diff < 86_400_000 -> "${diff / 3_600_000}h ago"
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
