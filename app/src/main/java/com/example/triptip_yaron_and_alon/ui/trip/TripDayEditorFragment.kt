package com.example.triptip_yaron_and_alon.ui.trip

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.triptip_yaron_and_alon.databinding.FragmentTripDayEditorBinding
import com.example.triptip_yaron_and_alon.domain.model.Post
import com.example.triptip_yaron_and_alon.domain.model.TripItem
import com.example.triptip_yaron_and_alon.ui.adapter.AvailablePostsAdapter
import com.example.triptip_yaron_and_alon.ui.adapter.TripItemsAdapter
import com.example.triptip_yaron_and_alon.util.Result
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class TripDayEditorFragment : Fragment() {
    
    private var _binding: FragmentTripDayEditorBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: TripViewModel
    private val args: TripDayEditorFragmentArgs by navArgs()
    
    private lateinit var itemsAdapter: TripItemsAdapter
    private lateinit var availablePostsAdapter: AvailablePostsAdapter
    private lateinit var nearbyPlacesAdapter: com.example.triptip_yaron_and_alon.ui.adapter.NearbyPlaceAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripDayEditorBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this)[TripViewModel::class.java]
        
        setupRecyclerViews()
        setupListeners()
        observeViewModel()
        
        // Load day and available posts
        viewModel.loadDay(args.tripId, args.dayId)
        viewModel.loadAvailablePosts()
        
        // Load nearby places based on day's location
        // We'll need to get location from the day's items or trip
        // For now, we'll load places when a day with location is available
    }
    
    private fun setupListeners() {
        binding.btnDone.setOnClickListener {
            // Navigate back - changes are already saved automatically
            findNavController().popBackStack()
        }
    }
    
    private fun setupRecyclerViews() {
        // Items adapter
        itemsAdapter = TripItemsAdapter(
            onNotesChanged = { item, notes ->
                viewModel.updateItemNotes(args.dayId, item.id, notes)
            },
            onDelete = { item ->
                showDeleteItemDialog(item)
            },
            onMoveUp = { item ->
                moveItemUp(item)
            },
            onMoveDown = { item ->
                moveItemDown(item)
            }
        )
        
        binding.rvItems.apply {
            adapter = itemsAdapter
            layoutManager = LinearLayoutManager(context)
        }
        
        // Available posts adapter - create once and update excluded IDs
        availablePostsAdapter = AvailablePostsAdapter(
            onAddClick = { post ->
                android.util.Log.d("TripDayEditor", "Add post clicked: ${post.id}")
                viewModel.addItemToDay(args.dayId, post.id)
            },
            excludedPostIds = emptySet() // Will be updated when day loads
        )
        
        binding.rvAvailablePosts.apply {
            adapter = availablePostsAdapter
            layoutManager = LinearLayoutManager(context)
        }
        
        // Nearby places adapter
        nearbyPlacesAdapter = com.example.triptip_yaron_and_alon.ui.adapter.NearbyPlaceAdapter(
            onAddToTripClick = { place ->
                android.util.Log.d("TripDayEditor", "Add place clicked: ${place.xid}")
                viewModel.addPlaceToDay(args.dayId, place)
            },
            onPlaceClick = { place ->
                // Navigate to place details if needed
                // For now, do nothing
            }
        )
        
        binding.rvNearbyPlaces.apply {
            adapter = nearbyPlacesAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }
    
    private fun observeViewModel() {
        // Observe current day
        viewModel.currentDay.observe(viewLifecycleOwner) { day ->
            if (day != null) {
                binding.tvDayTitle.text = "Day ${day.dayNumber}"
                
                // Update items list
                val sortedItems = day.items.sortedBy { it.order }
                itemsAdapter.submitList(sortedItems)
                
                // Show/hide empty state
                if (sortedItems.isEmpty()) {
                    binding.tvEmptyItems.visibility = View.VISIBLE
                    binding.rvItems.visibility = View.GONE
                } else {
                    binding.tvEmptyItems.visibility = View.GONE
                    binding.rvItems.visibility = View.VISIBLE
                }
                
                // Update excluded post IDs for available posts adapter (don't recreate adapter)
                val excludedIds = sortedItems.mapNotNull { it.postId }.toSet()
                availablePostsAdapter.updateExcludedIds(excludedIds)
                
                // Update available posts list - filter out already added posts
                viewModel.availablePosts.value?.let { posts ->
                    val filteredPosts = posts.filter { it.id !in excludedIds }
                    availablePostsAdapter.submitList(filteredPosts)
                }
                
                // Load nearby places if we have location from items
                // Try to get location from first item with coordinates
                val itemWithLocation = sortedItems.firstOrNull { item ->
                    item.post?.latitude != null && item.post?.longitude != null
                }
                
                if (itemWithLocation != null) {
                    val lat = itemWithLocation.post?.latitude ?: return@observe
                    val lon = itemWithLocation.post?.longitude ?: return@observe
                    viewModel.loadNearbyPlaces(lat, lon)
                } else {
                    // Try to get location from post location name
                    val itemWithLocationName = sortedItems.firstOrNull { item ->
                        item.post?.location != null && item.post?.location?.isNotBlank() == true
                    }
                    
                    if (itemWithLocationName != null) {
                        val locationName = itemWithLocationName.post?.location ?: return@observe
                        viewModel.loadNearbyPlacesForLocation(locationName)
                    }
                }
            }
        }
        
        // Observe available posts
        viewModel.availablePosts.observe(viewLifecycleOwner) { posts ->
            // Filter out already added posts
            val excludedIds = viewModel.currentDay.value?.items?.mapNotNull { it.postId }?.toSet() ?: emptySet()
            availablePostsAdapter.updateExcludedIds(excludedIds)
            val filteredPosts = posts.filter { it.id !in excludedIds }
            availablePostsAdapter.submitList(filteredPosts)
        }
        
        // Observe nearby places
        viewModel.nearbyPlaces.observe(viewLifecycleOwner) { places ->
            val excludedPlaceIds = viewModel.currentDay.value?.items?.mapNotNull { it.placeId }?.toSet() ?: emptySet()
            val filteredPlaces = places.filter { it.xid !in excludedPlaceIds }
            nearbyPlacesAdapter.submitList(filteredPlaces)
            
            // Show/hide section based on whether there are places
            if (filteredPlaces.isNotEmpty()) {
                binding.tvNearbyPlacesTitle.visibility = View.VISIBLE
                binding.rvNearbyPlaces.visibility = View.VISIBLE
            } else {
                binding.tvNearbyPlacesTitle.visibility = View.GONE
                binding.rvNearbyPlaces.visibility = View.GONE
            }
        }
        
        viewModel.placesLoading.observe(viewLifecycleOwner) { isLoading ->
            // Show loading state if needed
        }
        
        viewModel.placesError.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.tvNearbyPlacesTitle.visibility = View.GONE
                binding.rvNearbyPlaces.visibility = View.GONE
            }
        }
        
        // Observe loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // Observe item operation result
        viewModel.itemOperationResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    Snackbar.make(binding.root, "Item added successfully", Snackbar.LENGTH_SHORT).show()
                }
                is Result.Error -> {
                    Snackbar.make(binding.root, result.message ?: "Failed to add item", Snackbar.LENGTH_LONG).show()
                }
                else -> {}
            }
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
    
    private fun showDeleteItemDialog(item: TripItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to remove this post from the day?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.removeItemFromDay(args.dayId, item.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun moveItemUp(item: TripItem) {
        val currentDay = viewModel.currentDay.value ?: return
        val items = currentDay.items.sortedBy { it.order }.toMutableList()
        val index = items.indexOfFirst { it.id == item.id }
        
        if (index > 0) {
            // Swap with previous item
            val temp = items[index]
            items[index] = items[index - 1].copy(order = index)
            items[index - 1] = temp.copy(order = index - 1)
            
            viewModel.reorderItems(args.dayId, items)
        }
    }
    
    private fun moveItemDown(item: TripItem) {
        val currentDay = viewModel.currentDay.value ?: return
        val items = currentDay.items.sortedBy { it.order }.toMutableList()
        val index = items.indexOfFirst { it.id == item.id }
        
        if (index < items.size - 1) {
            // Swap with next item
            val temp = items[index]
            items[index] = items[index + 1].copy(order = index)
            items[index + 1] = temp.copy(order = index + 1)
            
            viewModel.reorderItems(args.dayId, items)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
