package com.example.triptip_yaron_and_alon.ui.trip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.triptip_yaron_and_alon.databinding.FragmentTripBuilderBinding
import com.example.triptip_yaron_and_alon.util.Result
import com.google.android.material.snackbar.Snackbar

class TripBuilderFragment : Fragment() {
    
    private var _binding: FragmentTripBuilderBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: TripViewModel
    private val args: TripBuilderFragmentArgs by navArgs()
    
    private var isNewTrip = false
    
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
        
        isNewTrip = args.tripId == "new"
        
        if (!isNewTrip) {
            viewModel.loadTrip(args.tripId)
        }
        
        setupListeners()
        observeViewModel()
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
                // TODO: Get current user ID from AuthRepository
                // For now, using placeholder - this should be fixed when implementing auth flow
                viewModel.createTrip(
                    title = title,
                    description = description.ifEmpty { null },
                    userId = "currentUserId" // TODO: Replace with actual user ID
                )
            } else {
                viewModel.updateTrip(
                    tripId = args.tripId,
                    title = title,
                    description = description.ifEmpty { null }
                )
            }
        }
        
        binding.btnAddDay.setOnClickListener {
            // Simple implementation - add a day with number based on current count
            val currentTrip = viewModel.currentTrip.value
            if (currentTrip != null) {
                val dayNumber = currentTrip.days.size + 1
                viewModel.addDay(currentTrip.id, dayNumber, "Day $dayNumber")
            } else {
                Snackbar.make(binding.root, "Please save the trip first", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun observeViewModel() {
        viewModel.currentTrip.observe(viewLifecycleOwner) { trip ->
            if (trip != null && !isNewTrip) {
                binding.etTripTitle.setText(trip.title)
                binding.etTripDescription.setText(trip.description)
                // TODO: Display days in RecyclerView
            }
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSaveTrip.isEnabled = !isLoading
        }
        
        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    Snackbar.make(binding.root, "Trip saved successfully!", Snackbar.LENGTH_SHORT).show()
                    if (isNewTrip) {
                        // Navigate back to trip list
                        findNavController().navigateUp()
                    }
                }
                is Result.Error -> {
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
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

