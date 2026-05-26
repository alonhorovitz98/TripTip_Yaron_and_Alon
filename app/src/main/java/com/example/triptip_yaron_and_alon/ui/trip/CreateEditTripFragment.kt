package com.example.triptip_yaron_and_alon.ui.trip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.triptip_yaron_and_alon.databinding.FragmentCreateEditTripBinding
import com.example.triptip_yaron_and_alon.ui.adapter.TripDaysAdapter
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CreateEditTripFragment : Fragment() {

    private var _binding: FragmentCreateEditTripBinding? = null
    private val binding get() = _binding!!

    private val args: CreateEditTripFragmentArgs by navArgs()
    private lateinit var viewModel: CreateEditTripViewModel
    private lateinit var daysAdapter: TripDaysAdapter

    private var isNewTrip = true
    private var hasPopulatedName = false
    private var hasPopulatedDates = false

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private var tripStartMillis: Long? = null
    private var tripEndMillis: Long? = null

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

        binding.tilDescription.visibility = View.GONE

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        setFragmentResultListener(DayEditorFragment.REQUEST_DAY_EDITOR) { _, bundle ->
            val message = bundle.getString(DayEditorFragment.RESULT_MESSAGE) ?: return@setFragmentResultListener
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
        }

        if (isNewTrip) {
            viewModel.initNewTrip()
        } else {
            viewModel.loadTrip(args.tripId)
        }
    }

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
        binding.btnSaveTrip.setOnClickListener { onSaveTripClicked() }
        binding.btnAddDay.setOnClickListener { onAddDayClicked() }

        val openStart: (View) -> Unit = { showTripDatePicker(isStart = true) }
        binding.tilStartDate.setOnClickListener(openStart)
        binding.etStartDate.setOnClickListener(openStart)
        val openEnd: (View) -> Unit = { showTripDatePicker(isStart = false) }
        binding.tilEndDate.setOnClickListener(openEnd)
        binding.etEndDate.setOnClickListener(openEnd)
    }

    private fun showTripDatePicker(isStart: Boolean) {
        val current = if (isStart) tripStartMillis else tripEndMillis
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(if (isStart) "Trip start" else "Trip end")
            .setSelection(current ?: MaterialDatePicker.todayInUtcMilliseconds())
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            if (isStart) {
                tripStartMillis = selection
                binding.etStartDate.setText(dateFormat.format(Date(selection)))
            } else {
                tripEndMillis = selection
                binding.etEndDate.setText(dateFormat.format(Date(selection)))
            }
        }
        picker.show(parentFragmentManager, if (isStart) "TRIP_START_PICKER" else "TRIP_END_PICKER")
    }

    private fun onAddDayClicked() {
        val trip = viewModel.currentTrip.value ?: return
        val tripId = trip.id
        if (tripId.isBlank() || tripId == "new") {
            Snackbar.make(binding.root, "Save the trip first before adding days.", Snackbar.LENGTH_SHORT).show()
            return
        }
        viewModel.addDay(tripId, null)
    }

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

    private fun onSaveTripClicked() {
        val name = binding.etTripName.text?.toString()?.trim() ?: ""
        val err = viewModel.validateName(name)
        if (err != null) {
            binding.tilTripName.error = err
            return
        }
        binding.tilTripName.error = null

        val s = tripStartMillis
        val e = tripEndMillis
        if (s != null && e != null && e < s) {
            Snackbar.make(
                binding.root,
                "End date must be on or after the start date.",
                Snackbar.LENGTH_LONG
            ).show()
            return
        }

        val currentTripId = viewModel.currentTrip.value?.id
        if (currentTripId.isNullOrBlank() || currentTripId == "new") {
            viewModel.createTrip(name, tripStartMillis, tripEndMillis)
        } else {
            viewModel.updateTrip(currentTripId, name, tripStartMillis, tripEndMillis)
        }
    }

    private fun observeViewModel() {
        viewModel.currentTrip.observe(viewLifecycleOwner) { trip ->
            if (trip == null) return@observe

            if (args.tripId != "new" && !hasPopulatedName) {
                binding.etTripName.setText(trip.name)
                hasPopulatedName = true
            }

            if (args.tripId != "new" && !hasPopulatedDates) {
                tripStartMillis = trip.startDateMillis
                tripEndMillis = trip.endDateMillis
                tripStartMillis?.let { binding.etStartDate.setText(dateFormat.format(Date(it))) }
                tripEndMillis?.let { binding.etEndDate.setText(dateFormat.format(Date(it))) }
                hasPopulatedDates = true
            }

            val isSaved = trip.id.isNotBlank() && trip.id != "new"
            binding.btnAddDay.isEnabled = isSaved && viewModel.isLoading.value != true

            val sortedDays = trip.days.sortedBy { it.dayOrder }
            if (sortedDays.isEmpty()) {
                binding.tvNoDays.text = if (isSaved) {
                    if (trip.startDateMillis != null && trip.endDateMillis != null) {
                        "No days yet. Save again after setting dates, or tap \"+ Add Day\"."
                    } else {
                        "Set trip dates and save to auto-create days, or tap \"+ Add Day\"."
                    }
                } else {
                    "Save the trip first, then add days."
                }
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
                val hasDates = trip.startDateMillis != null && trip.endDateMillis != null
                val msg = when {
                    hasDates && trip.days.isNotEmpty() ->
                        "Trip saved with ${trip.days.size} day(s). Tap a day to add places and posts."
                    isNewTrip ->
                        "Trip saved! Set start and end dates to auto-create days, or tap '+ Add Day'."
                    else ->
                        "Trip saved."
                }
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                isNewTrip = false
                viewModel.clearSaveResult()
            }
        }

        viewModel.dayAdded.observe(viewLifecycleOwner) { n ->
            if (n != null) {
                Snackbar.make(
                    binding.root,
                    "Day $n added. Tap it to add items.",
                    Snackbar.LENGTH_SHORT
                ).show()
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
