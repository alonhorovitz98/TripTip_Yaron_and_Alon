package com.example.triptip_yaron_and_alon.ui.trip

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.ui.adapter.TripDayAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment displaying detailed information about a trip.
 * Shows trip header, info, and list of days with activities.
 */
class TripDetailsFragment : Fragment() {

    private val args: TripDetailsFragmentArgs by navArgs()
    private lateinit var viewModel: TripViewModel
    private lateinit var tripDayAdapter: TripDayAdapter

    // Views
    private lateinit var ivTripHeaderImage: ImageView
    private lateinit var btnBack: ImageButton
    private lateinit var btnMore: ImageButton
    private lateinit var tvTripTitle: TextView
    private lateinit var tvTripDates: TextView
    private lateinit var tvTripDescription: TextView
    private lateinit var rvTripDays: RecyclerView
    private lateinit var btnShareTrip: MaterialButton
    private lateinit var btnEditTrip: MaterialButton
    private lateinit var fabAddDay: FloatingActionButton
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_trip_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        initViews(view)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[TripViewModel::class.java]

        // Setup RecyclerView
        setupRecyclerView()

        // Setup click listeners
        setupClickListeners()

        // Observe ViewModel
        observeViewModel()

        // Load trip data
        viewModel.loadTrip(args.tripId)
    }

    private fun initViews(view: View) {
        ivTripHeaderImage = view.findViewById(R.id.ivTripHeaderImage)
        btnBack = view.findViewById(R.id.btnBack)
        btnMore = view.findViewById(R.id.btnMore)
        tvTripTitle = view.findViewById(R.id.tvTripTitle)
        tvTripDates = view.findViewById(R.id.tvTripDates)
        tvTripDescription = view.findViewById(R.id.tvTripDescription)
        rvTripDays = view.findViewById(R.id.rvTripDays)
        btnShareTrip = view.findViewById(R.id.btnShareTrip)
        btnEditTrip = view.findViewById(R.id.btnEditTrip)
        fabAddDay = view.findViewById(R.id.fabAddDay)
        progressBar = view.findViewById(R.id.progressBar)
    }

    private fun setupRecyclerView() {
        tripDayAdapter = TripDayAdapter { tripDay ->
            // TODO: Navigate to day editor when navigation action is added
            Toast.makeText(requireContext(), "Edit day: ${tripDay.dayNumber}", Toast.LENGTH_SHORT).show()
        }

        rvTripDays.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = tripDayAdapter
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        btnMore.setOnClickListener {
            showMoreOptionsMenu()
        }

        btnShareTrip.setOnClickListener {
            shareTrip()
        }

        btnEditTrip.setOnClickListener {
            // TODO: Navigate to trip builder when navigation action is added
            Toast.makeText(requireContext(), "Edit trip: ${viewModel.currentTrip.value?.title}", Toast.LENGTH_SHORT).show()
        }

        fabAddDay.setOnClickListener {
            val currentTrip = viewModel.currentTrip.value
            if (currentTrip != null) {
                val nextDayNumber = (currentTrip.days.maxOfOrNull { it.dayNumber } ?: 0) + 1
                viewModel.addDay(args.tripId, nextDayNumber, "Day $nextDayNumber")
            }
        }
    }

    private fun observeViewModel() {
        viewModel.currentTrip.observe(viewLifecycleOwner) { trip ->
            trip?.let {
                updateUI(it)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        viewModel.deleteResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is com.example.triptip_yaron_and_alon.util.Result.Success -> {
                    Toast.makeText(requireContext(), "Trip deleted", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                is com.example.triptip_yaron_and_alon.util.Result.Error -> {
                    // Error already handled by error observer
                }
                else -> {}
            }
        }
    }

    private fun updateUI(trip: com.example.triptip_yaron_and_alon.domain.model.Trip) {
        // Set title
        tvTripTitle.text = trip.title

        // Set dates
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val dates = when {
            trip.startDate != null && trip.endDate != null -> {
                "${dateFormat.format(Date(trip.startDate))}-${dateFormat.format(Date(trip.endDate))}"
            }
            trip.startDate != null -> dateFormat.format(Date(trip.startDate))
            else -> "No dates set"
        }
        tvTripDates.text = dates

        // Set description
        tvTripDescription.text = trip.description ?: "No description"
        tvTripDescription.visibility = if (trip.description.isNullOrEmpty()) View.GONE else View.VISIBLE

        // Load header image (use first day's first item image, or placeholder)
        val headerImage = trip.days.firstOrNull()?.items?.firstOrNull()?.post?.imageUrl
        ivTripHeaderImage.load(headerImage) {
            crossfade(true)
            placeholder(R.drawable.ic_placeholder_image)
            error(R.drawable.ic_placeholder_image)
        }

        // Update days list
        tripDayAdapter.submitList(trip.days.sortedBy { it.dayNumber })
    }

    private fun shareTrip() {
        val trip = viewModel.currentTrip.value ?: return

        val shareText = buildString {
            append("Check out my trip: ${trip.title}\n\n")
            if (trip.description != null) {
                append("${trip.description}\n\n")
            }
            append("Days:\n")
            trip.days.sortedBy { it.dayNumber }.forEach { day ->
                val location = day.items.firstOrNull()?.post?.location ?: "No location"
                append("Day ${day.dayNumber}: $location (${day.items.size} activities)\n")
            }
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Trip: ${trip.title}")
        }

        startActivity(Intent.createChooser(shareIntent, "Share trip via"))
    }

    private fun showMoreOptionsMenu() {
        AlertDialog.Builder(requireContext())
            .setTitle("Trip Options")
            .setItems(arrayOf("Delete Trip")) { _, which ->
                when (which) {
                    0 -> confirmDeleteTrip()
                }
            }
            .show()
    }

    private fun confirmDeleteTrip() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Trip")
            .setMessage("Are you sure you want to delete this trip? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteTrip(args.tripId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
