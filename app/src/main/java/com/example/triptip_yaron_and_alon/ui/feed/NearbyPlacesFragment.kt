package com.example.triptip_yaron_and_alon.ui.feed

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.databinding.FragmentNearbyPlacesBinding
import com.example.triptip_yaron_and_alon.domain.model.PlaceInfo
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.snackbar.Snackbar

/**
 * Nearby Places Fragment - Shows Google Places API results (places near user)
 * Displays places in a card-based UI with photos, addresses, and "Add to Trip" buttons
 */
class NearbyPlacesFragment : Fragment() {
    
    private var _binding: FragmentNearbyPlacesBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: NearbyPlacesViewModel by viewModels()
    private lateinit var placeAdapter: com.example.triptip_yaron_and_alon.ui.adapter.NearbyPlaceAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNearbyPlacesBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        
        // Request location and load places
        requestLocationAndLoadPlaces()
    }
    
    private fun setupRecyclerView() {
        placeAdapter = com.example.triptip_yaron_and_alon.ui.adapter.NearbyPlaceAdapter(
            onAddToTripClick = { place ->
                onAddToTripClick(place)
            },
            onPlaceClick = { place ->
                // Navigate to place details screen
                // Use place.xid as placeId (it contains Google place_id)
                val action = com.example.triptip_yaron_and_alon.ui.feed.SocialFeedFragmentDirections
                    .actionFeedFragmentToGooglePlaceDetailsFragment(place.xid)
                findNavController().navigate(action)
            }
        )
        
        binding.rvPlaces.apply {
            adapter = placeAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            
            // Endless scrolling - load more when reaching the end
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    
                    // Load more when reaching near the end (within 3 items of the end)
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 3
                        && firstVisibleItemPosition >= 0
                        && totalItemCount >= 10
                        && viewModel.hasMorePages()
                        && viewModel.isLoadingMore.value != true
                    ) {
                        android.util.Log.d("NearbyPlaces", "Loading more places...")
                        viewModel.loadMorePlaces()
                    }
                }
            })
        }
    }
    
    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            requestLocationAndLoadPlaces()
        }
    }
    
    private fun observeViewModel() {
        // Observe places
        viewModel.places.observe(viewLifecycleOwner) { places ->
            if (places.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvPlaces.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.rvPlaces.visibility = View.VISIBLE
                placeAdapter.submitList(places)
            }
        }
        
        // Observe loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading && (viewModel.places.value == null || viewModel.places.value!!.isEmpty())) {
                binding.progressBar.visibility = View.VISIBLE
            } else {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
        
        // Observe loading more (for pagination)
        viewModel.isLoadingMore.observe(viewLifecycleOwner) { isLoadingMore ->
            // You can show a loading indicator at the bottom if needed
            // For now, we'll just let it load silently
        }
        
        // Observe error
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.tvError.text = error
                binding.tvError.visibility = View.VISIBLE
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
            } else {
                binding.tvError.visibility = View.GONE
            }
        }
    }
    
    private fun requestLocationAndLoadPlaces() {
        // Check location permissions
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request permissions
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
        
        // Try to get a fresh current location with high accuracy
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null // no cancellation token
        ).addOnSuccessListener { location: Location? ->
            if (location != null) {
                android.util.Log.d(
                    "NearbyPlaces",
                    "Current location: lat=${location.latitude}, lon=${location.longitude}"
                )
                viewModel.loadNearbyPlaces(location.latitude, location.longitude)
            } else {
                android.util.Log.d(
                    "NearbyPlaces",
                    "Current location unavailable, falling back to last known location"
                )
                // Fallback to last known location if current location is not available
                loadFromLastKnownLocation()
            }
        }.addOnFailureListener { e ->
            android.util.Log.e("NearbyPlaces", "Failed to get current location: ${e.message}")
            // Fallback to last known location on failure
            loadFromLastKnownLocation(errorMessage = e.message)
        }
    }

    private fun loadFromLastKnownLocation(errorMessage: String? = null) {
        fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation: Location? ->
            if (lastLocation != null) {
                android.util.Log.d(
                    "NearbyPlaces",
                    "Using last known location: lat=${lastLocation.latitude}, lon=${lastLocation.longitude}"
                )
                viewModel.loadNearbyPlaces(lastLocation.latitude, lastLocation.longitude)
            } else {
                binding.tvError.text = errorMessage?.let {
                    "Failed to get location: $it"
                } ?: "Location not available. Please enable location services and try again."
                binding.tvError.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }.addOnFailureListener { e ->
            binding.tvError.text = "Failed to get location: ${e.message}"
            binding.tvError.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
            binding.swipeRefresh.isRefreshing = false
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, load places
                requestLocationAndLoadPlaces()
            } else {
                // Permission denied
                binding.tvError.text = "Location permission is required to show nearby places."
                binding.tvError.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
    
    private fun onAddToTripClick(place: PlaceInfo) {
        // Navigate to trip list or show trip selection dialog
        // For now, show a snackbar
        Snackbar.make(
            binding.root,
            "Adding ${place.name} to trip...",
            Snackbar.LENGTH_SHORT
        ).show()
        
        // TODO: Implement navigation to trip selection or add directly to a trip
        // This will be integrated with TripViewModel in Phase 4
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
