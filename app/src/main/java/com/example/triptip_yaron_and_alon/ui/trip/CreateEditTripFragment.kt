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
import com.example.triptip_yaron_and_alon.databinding.FragmentCreateEditTripBinding
import com.example.triptip_yaron_and_alon.ui.adapter.TripDaysAdapter
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class CreateEditTripFragment : Fragment() {

    private var _binding: FragmentCreateEditTripBinding? = null
    private val binding get() = _binding!!

    private val args: CreateEditTripFragmentArgs by navArgs()
    private lateinit var viewModel: CreateEditTripViewModel
    private lateinit var daysAdapter: TripDaysAdapter

    // Display dates in UTC so they match the MaterialDatePicker selection (which is UTC midnight)
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private var startDateMillis: Long? = null
    private var endDateMillis: Long? = null

    // True only while viewing a brand-new trip before its first save
    private var isNewTrip = true
    // Populated only once when loading an existing trip
    private var hasPopulatedFields = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateEditTripBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[CreateEditTripViewModel::class.java]
        isNewTrip = args.tripId == "new"

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        if (isNewTrip) {
            viewModel.initNewTrip()
        } else {
            viewModel.loadTrip(args.tripId)
        }
    }

    // ─────────────────── Setup ───────────────────

    private fun setupRecyclerView() {
        daysAdapter = TripDaysAdapter(
            onDayClick = { day -> navigateToDayEditor(day.id) },
            onDayDateClick = { day -> navigateToDayEditor(day.id) }
        )
        binding.rvDays.apply {
            adapter = daysAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(false)
        }
    }

    private fun setupListeners() {
        // Date fields — register on BOTH the TextInputLayout container and the EditText
        // to ensure clicks are captured regardless of Material touch interception.
        binding.etStartDate.setOnClickListener { showStartDatePicker() }
        binding.tilStartDate.setOnClickListener { showStartDatePicker() }

        binding.etEndDate.setOnClickListener { showEndDatePicker() }
        binding.tilEndDate.setOnClickListener { showEndDatePicker() }

        // Save Trip
        binding.btnSaveTrip.setOnClickListener { onSaveTripClicked() }

        // Add Day — shows a date picker before creating the day
        binding.btnAddDay.setOnClickListener { showAddDayDatePicker() }
    }

    // ─────────────────── Trip date pickers ───────────────────

    private fun showStartDatePicker() {
        // Guard: prevent opening a second picker while one is already shown
        if (parentFragmentManager.findFragmentByTag("START_DATE_PICKER") != null) return

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select start date")
            .setSelection(startDateMillis ?: MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            startDateMillis = selection
            binding.etStartDate.setText(dateFormat.format(Date(selection)))
            binding.tilStartDate.error = null
            // Clear end date if it's now before the new start
            if (endDateMillis != null && endDateMillis!! <= selection) {
                endDateMillis = null
                binding.etEndDate.setText("")
            }
        }
        picker.show(parentFragmentManager, "START_DATE_PICKER")
    }

    private fun showEndDatePicker() {
        if (parentFragmentManager.findFragmentByTag("END_DATE_PICKER") != null) return

        val constraints = CalendarConstraints.Builder().apply {
            startDateMillis?.let { setValidator(DateValidatorPointForward.from(it)) }
        }.build()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select end date")
            .setSelection(endDateMillis ?: (startDateMillis ?: MaterialDatePicker.todayInUtcMilliseconds()))
            .setCalendarConstraints(constraints)
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            endDateMillis = selection
            binding.etEndDate.setText(dateFormat.format(Date(selection)))
            binding.tilEndDate.error = null
        }
        picker.show(parentFragmentManager, "END_DATE_PICKER")
    }

    // ─────────────────── Add Day with date picker ───────────────────

    private fun showAddDayDatePicker() {
        if (parentFragmentManager.findFragmentByTag("ADD_DAY_DATE_PICKER") != null) return

        val trip = viewModel.currentTrip.value ?: return
        val tripId = trip.id
        if (tripId.isBlank() || tripId == "new") {
            Snackbar.make(binding.root, "Save the trip first before adding days.", Snackbar.LENGTH_SHORT).show()
            return
        }

        val dayNumber = trip.days.size + 1

        // Pre-select a suggested date based on trip start date + day index
        val suggestedDate = trip.startDate?.let { start ->
            start + ((dayNumber - 1).toLong() * 24 * 60 * 60 * 1000L)
        } ?: MaterialDatePicker.todayInUtcMilliseconds()

        // Constrain picker to trip's date range
        val constraints = CalendarConstraints.Builder().apply {
            trip.startDate?.let { start ->
                setStart(start)
                setValidator(DateValidatorPointForward.from(start))
            }
            trip.endDate?.let { setEnd(it) }
        }.build()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Day $dayNumber — Pick a date")
            .setSelection(suggestedDate)
            .setCalendarConstraints(constraints)
            .build()

        picker.addOnPositiveButtonClickListener { dateMillis ->
            viewModel.addDay(tripId, dateMillis)
        }

        // Negative button = add without a date (user can set it later in DayEditorFragment)
        picker.addOnNegativeButtonClickListener {
            viewModel.addDay(tripId, null)
        }

        picker.show(parentFragmentManager, "ADD_DAY_DATE_PICKER")
    }

    // ─────────────────── Navigation ───────────────────

    private fun navigateToDayEditor(dayId: String) {
        val tripId = viewModel.currentTrip.value?.id ?: return
        if (tripId == "new" || tripId.isBlank()) {
            Snackbar.make(binding.root, "Save the trip first before editing days.", Snackbar.LENGTH_SHORT).show()
            return
        }
        val action = CreateEditTripFragmentDirections
            .actionCreateEditTripFragmentToDayEditorFragment(tripId, dayId)
        findNavController().navigate(action)
    }

    // ─────────────────── Save ───────────────────

    private fun onSaveTripClicked() {
        val title = binding.etTripName.text?.toString()?.trim() ?: ""
        val description = binding.etDescription.text?.toString()?.trim()

        val error = viewModel.validate(title, startDateMillis, endDateMillis)
        if (error != null) {
            when {
                error.contains("name", ignoreCase = true) -> binding.tilTripName.error = error
                error.contains("start", ignoreCase = true) -> {
                    binding.tilStartDate.error = error
                    Snackbar.make(binding.root, "Tap the Start Date field to select a date", Snackbar.LENGTH_LONG).show()
                }
                error.contains("end", ignoreCase = true) -> {
                    binding.tilEndDate.error = error
                    Snackbar.make(binding.root, "Tap the End Date field to select a date", Snackbar.LENGTH_LONG).show()
                }
                else -> Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
            }
            return
        }

        binding.tilTripName.error = null
        binding.tilStartDate.error = null
        binding.tilEndDate.error = null

        // Always use the live trip ID — args.tripId is "new" for new trips
        val currentTripId = viewModel.currentTrip.value?.id
        if (currentTripId.isNullOrBlank() || currentTripId == "new") {
            viewModel.createTrip(title, description, startDateMillis!!, endDateMillis!!)
        } else {
            viewModel.updateTrip(currentTripId, title, description, startDateMillis!!, endDateMillis!!)
        }
    }

    // ─────────────────── Observe ───────────────────

    private fun observeViewModel() {
        viewModel.currentTrip.observe(viewLifecycleOwner) { trip ->
            if (trip == null) return@observe

            // Populate fields only once when opening an EXISTING trip (not after creating a new one)
            if (args.tripId != "new" && !hasPopulatedFields) {
                binding.etTripName.setText(trip.title)
                binding.etDescription.setText(trip.description ?: "")
                trip.startDate?.let { ms ->
                    startDateMillis = ms
                    binding.etStartDate.setText(dateFormat.format(Date(ms)))
                }
                trip.endDate?.let { ms ->
                    endDateMillis = ms
                    binding.etEndDate.setText(dateFormat.format(Date(ms)))
                }
                hasPopulatedFields = true
            }

            val isSaved = trip.id.isNotBlank() && trip.id != "new"

            // "Add Day" is enabled only when the trip is saved to Firestore
            binding.btnAddDay.isEnabled = isSaved && viewModel.isLoading.value != true

            // Update the days list
            val sortedDays = trip.days.sortedBy { it.dayNumber }
            if (sortedDays.isEmpty()) {
                binding.tvNoDays.text = if (isSaved)
                    "No days yet. Tap \"+ Add Day\" to build your itinerary."
                else
                    "Save the trip first, then add days to your itinerary."
                binding.tvNoDays.visibility = View.VISIBLE
                binding.rvDays.visibility = View.GONE
            } else {
                binding.tvNoDays.visibility = View.GONE
                binding.rvDays.visibility = View.VISIBLE
                daysAdapter.submitList(sortedDays)
            }
        }

        viewModel.saveResult.observe(viewLifecycleOwner) { trip ->
            if (trip != null) {
                val msg = if (isNewTrip) "Trip created! Tap '+ Add Day' to start building your itinerary." else "Trip saved!"
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                isNewTrip = false
                viewModel.clearSaveResult()
            }
        }

        viewModel.dayAdded.observe(viewLifecycleOwner) { dayNumber ->
            if (dayNumber != null) {
                Snackbar.make(binding.root, "Day $dayNumber added! Tap it to add activities.", Snackbar.LENGTH_SHORT).show()
                viewModel.clearDayAdded()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnSaveTrip.isEnabled = !loading
            val isSaved = viewModel.currentTrip.value?.id?.let { it.isNotBlank() && it != "new" } == true
            binding.btnAddDay.isEnabled = !loading && isSaved
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
