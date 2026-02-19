package com.example.triptip_yaron_and_alon.ui.trip

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.databinding.FragmentTripListBinding
import com.example.triptip_yaron_and_alon.ui.adapter.TripAdapter
import com.google.android.material.snackbar.Snackbar

class TripListFragment : Fragment() {
    
    private var _binding: FragmentTripListBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: TripViewModel
    private lateinit var tripAdapter: TripAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripListBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this)[TripViewModel::class.java]
        
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        
        // ViewModel will automatically get the current user ID from Firebase Auth
        viewModel.loadUserTrips()
    }
    
    private fun setupRecyclerView() {
        tripAdapter = TripAdapter(
            onTripClick = { trip ->
                val action = TripListFragmentDirections
                    .actionTripListFragmentToTripBuilderFragment(trip.id)
                findNavController().navigate(action)
            },
            onTripLongClick = { trip ->
                showDeleteDialog(trip.id, trip.title)
            }
        )
        
        binding.rvTrips.apply {
            adapter = tripAdapter
            layoutManager = LinearLayoutManager(context)
            
            // Performance optimizations for smooth scrolling
            setHasFixedSize(true) // RecyclerView knows item sizes won't change
            setItemViewCacheSize(10) // Cache more views off-screen
        }
    }
    
    private fun setupListeners() {
        binding.fabCreateTrip.setOnClickListener {
            val action = TripListFragmentDirections
                .actionTripListFragmentToTripBuilderFragment("new")
            findNavController().navigate(action)
        }
    }
    
    private fun observeViewModel() {
        viewModel.userTrips.observe(viewLifecycleOwner) { trips ->
            if (trips.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvTrips.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.rvTrips.visibility = View.VISIBLE
                tripAdapter.submitList(trips)
            }
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
            }
        }
    }
    
    private fun showDeleteDialog(tripId: String, tripTitle: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Trip")
            .setMessage("Are you sure you want to delete \"$tripTitle\"?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteTrip(tripId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

