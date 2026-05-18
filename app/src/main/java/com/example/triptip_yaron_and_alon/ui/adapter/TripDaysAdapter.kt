package com.example.triptip_yaron_and_alon.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.databinding.ItemTripDayBinding
import com.example.triptip_yaron_and_alon.domain.model.DayItemType
import com.example.triptip_yaron_and_alon.domain.model.TripDay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TripDaysAdapter(
    private val onDayClick: (TripDay) -> Unit,
    private val onDayDateClick: ((TripDay) -> Unit)? = null
) : ListAdapter<TripDay, TripDaysAdapter.DayViewHolder>(DayDiffCallback()) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemTripDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DayViewHolder(binding, onDayClick, onDayDateClick, dateFormat)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DayViewHolder(
        private val binding: ItemTripDayBinding,
        private val onDayClick: (TripDay) -> Unit,
        private val onDayDateClick: ((TripDay) -> Unit)?,
        private val dateFormat: SimpleDateFormat
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(day: TripDay) {
            binding.apply {
                tvDayNumber.text = "Day ${day.dayOrder}"
                tvDayDate.text = day.dateMillis?.let { dateFormat.format(Date(it)) } ?: "No date"
                tvDayDate.setOnClickListener {
                    onDayDateClick?.invoke(day)
                }

                val count = day.items.size
                tvActivityCount.text = "$count ${if (count == 1) "item" else "items"}"

                val first = day.items.firstOrNull()
                tvCityName.text = when {
                    first == null -> "No items yet"
                    first.type == DayItemType.POST -> first.post?.text?.take(40)
                        ?: first.post?.location
                        ?: "Post"
                    first.type == DayItemType.PLACE -> first.value
                    else -> "Item"
                }

                val firstItemImage = day.items.firstOrNull { it.type == DayItemType.POST }?.post?.imageUrl
                if (!firstItemImage.isNullOrBlank()) {
                    ivDayThumbnail.visibility = View.VISIBLE
                    ivDayThumbnail.load(firstItemImage) {
                        placeholder(R.drawable.ic_placeholder_image)
                        error(R.drawable.ic_placeholder_image)
                    }
                } else {
                    ivDayThumbnail.visibility = View.GONE
                }

                root.setOnClickListener {
                    onDayClick(day)
                }
            }
        }
    }

    class DayDiffCallback : DiffUtil.ItemCallback<TripDay>() {
        override fun areItemsTheSame(oldItem: TripDay, newItem: TripDay): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TripDay, newItem: TripDay): Boolean {
            return oldItem == newItem
        }
    }
}
