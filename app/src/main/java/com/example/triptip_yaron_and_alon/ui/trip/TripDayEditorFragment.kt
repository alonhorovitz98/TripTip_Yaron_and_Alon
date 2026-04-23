package com.example.triptip_yaron_and_alon.ui.trip

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.databinding.FragmentTripDayEditorBinding
import com.example.triptip_yaron_and_alon.domain.model.LocationSuggestion
import com.example.triptip_yaron_and_alon.domain.model.Post
import com.example.triptip_yaron_and_alon.domain.model.TripItem
import com.example.triptip_yaron_and_alon.ui.adapter.AvailablePostsAdapter
import com.example.triptip_yaron_and_alon.ui.adapter.TripItemsAdapter
import com.example.triptip_yaron_and_alon.util.Result
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import android.widget.TextView

class TripDayEditorFragment : Fragment() {
    
    private var _binding: FragmentTripDayEditorBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: TripViewModel
    private val args: TripDayEditorFragmentArgs by navArgs()
    
    private lateinit var itemsAdapter: TripItemsAdapter
    private lateinit var availablePostsAdapter: AvailablePostsAdapter
    private lateinit var nearbyPlacesAdapter: com.example.triptip_yaron_and_alon.ui.adapter.NearbyPlaceAdapter
    private lateinit var placeSearchAdapter: PlaceSearchSuggestionsAdapter
    private var searchRunnable: Runnable? = null

    // Track last location used for nearby places to avoid duplicate API calls
    private var lastNearbyLat: Double? = null
    private var lastNearbyLon: Double? = null
    private var lastNearbyLocationName: String? = null
    
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
        setupPlaceSearch()
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
                viewModel.addItemToDay(args.tripId, args.dayId, post.id)
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
                viewModel.addPlaceToDay(args.tripId, args.dayId, place)
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

        placeSearchAdapter = PlaceSearchSuggestionsAdapter { suggestion ->
            val placeId = suggestion.googlePlaceId
            if (!placeId.isNullOrBlank()) {
                viewModel.addGooglePlaceToDay(args.tripId, args.dayId, placeId)
                binding.etSearchPlace.text?.clear()
                binding.rvPlaceSearchSuggestions.visibility = View.GONE
            } else {
                Snackbar.make(binding.root, "This place cannot be added", Snackbar.LENGTH_SHORT).show()
            }
        }
        binding.rvPlaceSearchSuggestions.apply {
            adapter = placeSearchAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupPlaceSearch() {
        binding.etSearchPlace.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.etSearchPlace.removeCallbacks(searchRunnable)
                searchRunnable = Runnable {
                    val query = s?.toString()?.trim()
                    if (!query.isNullOrBlank() && query.length >= 2) {
                        viewModel.searchPlaceSuggestions(query)
                    } else {
                        viewModel.searchPlaceSuggestions("")
                        placeSearchAdapter.submit(emptyList())
                        binding.rvPlaceSearchSuggestions.visibility = View.GONE
                    }
                }
                binding.etSearchPlace.postDelayed(searchRunnable!!, 300)
            }
            override fun afterTextChanged(editable: Editable?) {}
        })
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
                
                // Load nearby places only when the location actually changes
                val itemWithLocation = sortedItems.firstOrNull { item ->
                    item.post?.latitude != null && item.post?.longitude != null
                }

                if (itemWithLocation != null) {
                    val lat = itemWithLocation.post?.latitude ?: return@observe
                    val lon = itemWithLocation.post?.longitude ?: return@observe
                    if (lat != lastNearbyLat || lon != lastNearbyLon) {
                        lastNearbyLat = lat
                        lastNearbyLon = lon
                        lastNearbyLocationName = null
                        viewModel.loadNearbyPlaces(lat, lon)
                    }
                } else {
                    val itemWithLocationName = sortedItems.firstOrNull { item ->
                        item.post?.location?.isNotBlank() == true
                    }
                    if (itemWithLocationName != null) {
                        val locationName = itemWithLocationName.post?.location ?: return@observe
                        if (locationName != lastNearbyLocationName) {
                            lastNearbyLocationName = locationName
                            lastNearbyLat = null
                            lastNearbyLon = null
                            viewModel.loadNearbyPlacesForLocation(locationName)
                        }
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
                    val message = when (viewModel.lastItemOperation.value) {
                        TripViewModel.ItemOperation.ADD -> "Item added"
                        TripViewModel.ItemOperation.REMOVE -> "Item removed"
                        TripViewModel.ItemOperation.REORDER, TripViewModel.ItemOperation.UPDATE_NOTES, null -> null
                    }
                    message?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show() }
                }
                is Result.Error -> {
                    val fallback = when (viewModel.lastItemOperation.value) {
                        TripViewModel.ItemOperation.REMOVE -> "Failed to remove item"
                        TripViewModel.ItemOperation.REORDER -> "Failed to reorder items"
                        TripViewModel.ItemOperation.UPDATE_NOTES -> "Failed to save notes"
                        else -> "Failed to add item"
                    }
                    Snackbar.make(binding.root, result.message ?: fallback, Snackbar.LENGTH_LONG).show()
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

        viewModel.placeSearchSuggestions.observe(viewLifecycleOwner) { suggestions ->
            val withGoogleId = suggestions.filter { !it.googlePlaceId.isNullOrBlank() }
            placeSearchAdapter.submit(withGoogleId)
            binding.rvPlaceSearchSuggestions.visibility = if (withGoogleId.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private inner class PlaceSearchSuggestionsAdapter(
        private val onSuggestionClick: (LocationSuggestion) -> Unit
    ) : RecyclerView.Adapter<PlaceSearchSuggestionsAdapter.VH>() {
        private val items = mutableListOf<LocationSuggestion>()
        fun submit(newItems: List<LocationSuggestion>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = layoutInflater.inflate(android.R.layout.simple_list_item_1, parent, false)
            return VH(view)
        }
        override fun onBindViewHolder(holder: VH, position: Int) { holder.bind(items[position]) }
        override fun getItemCount(): Int = items.size
        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val textView: TextView = itemView.findViewById(android.R.id.text1)
            fun bind(item: LocationSuggestion) {
                textView.text = item.displayName
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                itemView.setOnClickListener { onSuggestionClick(item) }
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
        binding.etSearchPlace.removeCallbacks(searchRunnable)
        searchRunnable = null
        lastNearbyLat = null
        lastNearbyLon = null
        lastNearbyLocationName = null
        super.onDestroyView()
        _binding = null
    }
}
