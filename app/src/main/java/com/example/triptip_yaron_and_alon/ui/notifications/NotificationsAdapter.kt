package com.example.triptip_yaron_and_alon.ui.notifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
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
            binding.tvMessage.text = notification.message
            binding.tvTime.text = formatTime(notification.createdAt)
            binding.root.setOnClickListener { onNotificationClick(notification) }
        }

        private fun formatTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            return when {
                diff < 60_000 -> "Just now"
                diff < 3600_000 -> "${diff / 60_000} min ago"
                diff < 86400_000 -> "${diff / 3600_000} hours ago"
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
