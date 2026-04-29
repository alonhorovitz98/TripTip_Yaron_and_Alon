package com.example.triptip_yaron_and_alon.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.triptip_yaron_and_alon.databinding.ItemTripBinding
import com.example.triptip_yaron_and_alon.domain.model.Trip
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class TripAdapter(
    private val onTripClick: (Trip) -> Unit,
    private val onTripLongClick: (Trip) -> Unit
) : ListAdapter<Trip, TripAdapter.TripViewHolder>(TripDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val binding = ItemTripBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TripViewHolder(binding, onTripClick, onTripLongClick)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TripViewHolder(
        private val binding: ItemTripBinding,
        private val onTripClick: (Trip) -> Unit,
        private val onTripLongClick: (Trip) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(trip: Trip) {
            binding.apply {
                tvTripTitle.text = trip.name
                tvTripDescription.visibility = View.GONE
                val fmt = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
                val s = trip.startDateMillis
                val e = trip.endDateMillis
                when {
                    s != null && e != null -> {
                        tvDates.text = "${fmt.format(Date(s))} – ${fmt.format(Date(e))}"
                        tvDates.visibility = View.VISIBLE
                    }
                    s != null -> {
                        tvDates.text = "From ${fmt.format(Date(s))}"
                        tvDates.visibility = View.VISIBLE
                    }
                    e != null -> {
                        tvDates.text = "Until ${fmt.format(Date(e))}"
                        tvDates.visibility = View.VISIBLE
                    }
                    else -> tvDates.visibility = View.GONE
                }
                val n = if (trip.days.isNotEmpty()) trip.days.size else trip.firestoreDayCount
                tvDaysCount.text = if (n == 1) "1 day" else "$n days"

                root.setOnClickListener {
                    onTripClick(trip)
                }

                root.setOnLongClickListener {
                    onTripLongClick(trip)
                    true
                }
            }
        }
    }

    class TripDiffCallback : DiffUtil.ItemCallback<Trip>() {
        override fun areItemsTheSame(oldItem: Trip, newItem: Trip): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Trip, newItem: Trip): Boolean {
            return oldItem == newItem
        }
    }
}
