package com.example.triptip_yaron_and_alon.ui.trip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.triptip_yaron_and_alon.databinding.FragmentTripBuilderBinding
import com.example.triptip_yaron_and_alon.ui.adapter.TripDaysAdapter
import com.example.triptip_yaron_and_alon.util.Result
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TripBuilderFragment : Fragment() {
    
    private var _binding: FragmentTripBuilderBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: TripViewModel
    private val args: TripBuilderFragmentArgs by navArgs()
    
    private var isNewTrip = false
    private var currentTripId: String = "new" // Track the actual trip ID (updates after save)
    private var hasPopulatedFields = false // Track if we've populated fields for existing trip
    private lateinit var daysAdapter: TripDaysAdapter
    private var startDateMillis: Long? = null
    private var endDateMillis: Long? = null
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripBuilderBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this)[TripViewModel::class.java]
        
        // Validate trip ID from args
        val tripIdFromArgs = args.tripId
        android.util.Log.d("TripBuilder", "Trip ID from args: '$tripIdFromArgs', isEmpty: ${tripIdFromArgs.isEmpty()}, isBlank: ${tripIdFromArgs.isBlank()}")
        
        // If trip ID is empty/blank, treat as new trip
        isNewTrip = tripIdFromArgs == "new" || tripIdFromArgs.isBlank() || tripIdFromArgs.isEmpty()
        currentTripId = if (isNewTrip) "new" else tripIdFromArgs
        hasPopulatedFields = false // Reset flag
        
        setupRecyclerView()
        setupDatePickers()
        setupListeners()
        observeViewModel()
        
        if (!isNewTrip) {
            // Load existing trip
            android.util.Log.d("TripBuilder", "Loading trip with ID: '$currentTripId'")
            viewModel.loadTrip(currentTripId)
        } else {
            // Initialize empty trip for new trips (allows adding days before saving)
            android.util.Log.d("TripBuilder", "Initializing new trip")
            viewModel.initializeNewTrip()
        }
    }
    
    private fun setupRecyclerView() {
        daysAdapter = TripDaysAdapter(
            onDayClick = { day ->
                if (day.id.isBlank() || day.id.startsWith("temp_")) {
                    Snackbar.make(
                        binding.root,
                        "Save the trip first to edit this day.",
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    val action = TripBuilderFragmentDirections
                        .actionTripBuilderFragmentToTripDayEditorFragment(
                            tripId = currentTripId,
                            dayId = day.id
                        )
                    findNavController().navigate(action)
                }
            },
            onDayDateClick = { day ->
                if (isNewTrip || day.id.startsWith("temp_")) {
                    Snackbar.make(
                        binding.root,
                        "Save the trip first, then you can set a date for each day.",
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    showDayDatePicker(day)
                }
            }
        )
        
        binding.rvDays.apply {
            adapter = daysAdapter
            layoutManager = LinearLayoutManager(context)
            isNestedScrollingEnabled = false
        }
    }

    private fun showDayDatePicker(day: com.example.triptip_yaron_and_alon.domain.model.TripDay) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Date for Day ${day.dayNumber}")
            .setSelection(day.date ?: MaterialDatePicker.todayInUtcMilliseconds())
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            selection?.let {
                viewModel.updateDayDate(currentTripId, day.id, it)
            }
        }
        picker.show(parentFragmentManager, "DAY_DATE_${day.id}")
    }
    
    private fun setupDatePickers() {
        binding.etStartDate.setOnClickListener { showStartDatePicker() }
        binding.etEndDate.setOnClickListener { showEndDatePicker() }
    }

    private fun showStartDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select start date")
            .setSelection(startDateMillis ?: MaterialDatePicker.todayInUtcMilliseconds())
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            selection?.let {
                startDateMillis = it
                binding.etStartDate.setText(dateFormat.format(Date(it)))
            }
        }
        picker.show(parentFragmentManager, "START_DATE")
    }

    private fun showEndDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select end date")
            .setSelection(endDateMillis ?: startDateMillis ?: MaterialDatePicker.todayInUtcMilliseconds())
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            selection?.let {
                endDateMillis = it
                binding.etEndDate.setText(dateFormat.format(Date(it)))
            }
        }
        picker.show(parentFragmentManager, "END_DATE")
    }

    private fun setupListeners() {
        binding.btnSaveTrip.setOnClickListener {
            val title = binding.etTripTitle.text.toString().trim()
            val description = binding.etTripDescription.text.toString().trim()
            
            if (title.isEmpty()) {
                Snackbar.make(binding.root, "Please enter a trip title", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (isNewTrip) {
                viewModel.createTrip(
                    title = title,
                    description = description.ifEmpty { null },
                    startDate = startDateMillis,
                    endDate = endDateMillis
                )
            } else {
                viewModel.updateTrip(
                    tripId = currentTripId,
                    title = title,
                    description = description.ifEmpty { null },
                    startDate = startDateMillis,
                    endDate = endDateMillis
                )
            }
        }
        
        binding.btnAddDay.setOnClickListener {
            val currentTrip = viewModel.currentTrip.value
            android.util.Log.d("TripBuilder", "Add Day clicked - currentTrip: ${currentTrip != null}, currentTripId: $currentTripId, isNewTrip: $isNewTrip")
            
            if (currentTrip != null) {
                // Allow adding days even for new trips (they're stored locally)
                val dayNumber = currentTrip.days.size + 1
                android.util.Log.d("TripBuilder", "Adding day $dayNumber to trip ${currentTrip.id}")
                
                if (isNewTrip) {
                    // Add day to local trip (not saved yet)
                    android.util.Log.d("TripBuilder", "Adding day to local trip (new trip)")
                    viewModel.addDayToLocalTrip(dayNumber, "Day $dayNumber")
                } else {
                    // Use currentTripId as fallback if currentTrip.id is invalid
                    val tripIdToUse = if (currentTrip.id.isNotBlank() && currentTrip.id != "new") {
                        currentTrip.id
                    } else {
                        currentTripId
                    }
                    android.util.Log.d("TripBuilder", "Calling addDay with tripId: $tripIdToUse")
                    viewModel.addDay(tripIdToUse, dayNumber, "Day $dayNumber")
                }
            } else {
                android.util.Log.w("TripBuilder", "Cannot add day - currentTrip is null")
                Snackbar.make(binding.root, "Trip not loaded yet. Please wait...", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun observeViewModel() {
        viewModel.currentTrip.observe(viewLifecycleOwner) { trip ->
            if (trip != null) {
                // Update current trip ID to match loaded trip
                if (trip.id != "new" && trip.id.isNotBlank()) {
                    currentTripId = trip.id
                }
                
                // Always populate fields for existing trips (only once when first loaded)
                // For new trips, don't overwrite user input
                if (!isNewTrip && !hasPopulatedFields) {
                    android.util.Log.d("TripBuilder", "Populating fields for trip: ${trip.title}")
                    binding.etTripTitle.setText(trip.title)
                    binding.etTripDescription.setText(trip.description ?: "")
                    startDateMillis = trip.startDate
                    endDateMillis = trip.endDate
                    trip.startDate?.let { binding.etStartDate.setText(dateFormat.format(Date(it))) }
                    trip.endDate?.let { binding.etEndDate.setText(dateFormat.format(Date(it))) }
                    hasPopulatedFields = true
                }
                
                val sortedDays = trip.days.sortedBy { it.dayNumber }
                daysAdapter.submitList(sortedDays)
                binding.tvDaysEmpty.visibility =
                    if (sortedDays.isEmpty()) View.VISIBLE else View.GONE
                binding.rvDays.visibility =
                    if (sortedDays.isEmpty()) View.GONE else View.VISIBLE
                
                binding.btnAddDay.isEnabled = viewModel.isLoading.value != true
            } else {
                // Trip is null - might be loading or failed to load
                if (!isNewTrip && !hasPopulatedFields) {
                    // If we're trying to edit an existing trip but it's null,
                    // show an error message
                    android.util.Log.w("TripBuilder", "Trip is null for ID: ${args.tripId}")
                    Snackbar.make(
                        binding.root,
                        "Failed to load trip. Please try again.",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
                daysAdapter.submitList(emptyList())
                binding.tvDaysEmpty.visibility = View.GONE
                binding.rvDays.visibility = View.GONE
                binding.btnAddDay.isEnabled = false
            }
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSaveTrip.isEnabled = !isLoading
            // Avoid double "Add Day" while Firestore/Room work is in progress
            val trip = viewModel.currentTrip.value
            binding.btnAddDay.isEnabled = !isLoading && trip != null
        }
        
        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    Snackbar.make(binding.root, "Trip saved successfully!", Snackbar.LENGTH_SHORT).show()
                    // Update current trip ID (important for new trips)
                    currentTripId = result.data.id
                    // Update isNewTrip flag since trip is now saved
                    if (isNewTrip) {
                        isNewTrip = false
                    }
                    // Trip is already updated in currentTrip via ViewModel
                    // Stay on screen for both new and existing trips (user can continue editing)
                }
                is Result.Error -> {
                    val errorMessage = result.message ?: "An error occurred"
                    Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

