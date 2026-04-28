package com.example.triptip_yaron_and_alon.ui.trip

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.triptip_yaron_and_alon.databinding.FragmentDayEditorBinding
import com.example.triptip_yaron_and_alon.databinding.ItemPlaceSuggestionBinding
import com.example.triptip_yaron_and_alon.domain.model.LocationSuggestion
import com.example.triptip_yaron_and_alon.ui.adapter.AvailablePostsAdapter
import com.example.triptip_yaron_and_alon.ui.adapter.TripItemsAdapter
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DayEditorFragment : Fragment() {

    private var _binding: FragmentDayEditorBinding? = null
    private val binding get() = _binding!!

    private val args: DayEditorFragmentArgs by navArgs()
    private lateinit var viewModel: DayEditorViewModel

    private lateinit var postsAdapter: AvailablePostsAdapter
    private lateinit var itemsAdapter: TripItemsAdapter
    private lateinit var suggestionsAdapter: PlaceSuggestionAdapter

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private var selectedDateMillis: Long? = null

    // Debounce search
    private var searchRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDayEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[DayEditorViewModel::class.java]

        setupRecyclerViews()
        setupTabs()
        setupListeners()
        observeViewModel()

        viewModel.loadDay(args.tripId, args.dayId)
        viewModel.loadAvailablePosts()
    }

    // ─────────────────── Setup ───────────────────

    private fun setupRecyclerViews() {
        // Available posts
        postsAdapter = AvailablePostsAdapter(
            onAddClick = { post ->
                viewModel.addPostToDay(args.tripId, args.dayId, post)
            }
        )
        binding.rvAvailablePosts.apply {
            adapter = postsAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(false)
        }

        // Place search suggestions
        suggestionsAdapter = PlaceSuggestionAdapter { suggestion ->
            // Hide suggestions, show loading, then add the place
            binding.rvSearchSuggestions.visibility = View.GONE
            binding.etSearchPlace.setText("")
            viewModel.clearPlaceSuggestions()
            val placeId = suggestion.googlePlaceId
            if (placeId != null) {
                viewModel.addGooglePlaceToDay(args.tripId, args.dayId, placeId)
            } else {
                Snackbar.make(binding.root, "Place not available via Google API", Snackbar.LENGTH_SHORT).show()
            }
        }
        binding.rvSearchSuggestions.apply {
            adapter = suggestionsAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(false)
        }

        // Items on this day
        itemsAdapter = TripItemsAdapter(
            onNotesChanged = { item, notes ->
                // Notes are updated directly via updateTrip; handled inline
            },
            onDelete = { item ->
                viewModel.removeItemFromDay(args.tripId, args.dayId, item.id)
            },
            onMoveUp = { item ->
                val currentItems = viewModel.currentDay.value?.items ?: return@TripItemsAdapter
                val index = currentItems.indexOfFirst { it.id == item.id }
                if (index > 0) {
                    val reordered = currentItems.toMutableList().also {
                        val tmp = it[index]; it[index] = it[index - 1]; it[index - 1] = tmp
                    }.mapIndexed { i, it -> it.copy(order = i) }
                    viewModel.reorderItems(args.dayId, reordered)
                }
            },
            onMoveDown = { item ->
                val currentItems = viewModel.currentDay.value?.items ?: return@TripItemsAdapter
                val index = currentItems.indexOfFirst { it.id == item.id }
                if (index < currentItems.size - 1) {
                    val reordered = currentItems.toMutableList().also {
                        val tmp = it[index]; it[index] = it[index + 1]; it[index + 1] = tmp
                    }.mapIndexed { i, it -> it.copy(order = i) }
                    viewModel.reorderItems(args.dayId, reordered)
                }
            }
        )
        binding.rvDayItems.apply {
            adapter = itemsAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(false)
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        binding.panelPosts.visibility = View.VISIBLE
                        binding.panelSearch.visibility = View.GONE
                    }
                    1 -> {
                        binding.panelPosts.visibility = View.GONE
                        binding.panelSearch.visibility = View.VISIBLE
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupListeners() {
        // Date picker
        binding.btnPickDate.setOnClickListener { showDatePicker() }

        // Place search with 400ms debounce
        binding.etSearchPlace.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchRunnable?.let { binding.root.handler.removeCallbacks(it) }
                val query = s?.toString()?.trim() ?: ""
                if (query.length < 2) {
                    viewModel.clearPlaceSuggestions()
                    return
                }
                searchRunnable = Runnable { viewModel.searchPlaces(query) }
                binding.root.handler.postDelayed(searchRunnable!!, 400)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Save Day
        binding.btnSaveDay.setOnClickListener {
            val description = binding.etDayDescription.text?.toString()
            viewModel.saveDay(args.tripId, args.dayId, selectedDateMillis, description)
        }
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Set day date")
            .setSelection(selectedDateMillis ?: MaterialDatePicker.todayInUtcMilliseconds())
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            selectedDateMillis = selection
            val formatted = dateFormat.format(Date(selection))
            binding.tvSelectedDate.text = "Date: $formatted"
            binding.tvSelectedDate.visibility = View.VISIBLE
            binding.btnPickDate.text = formatted
        }
        picker.show(parentFragmentManager, "DAY_DATE_PICKER")
    }

    // ─────────────────── Observe ───────────────────

    private fun observeViewModel() {
        viewModel.currentDay.observe(viewLifecycleOwner) { day ->
            if (day == null) return@observe
            binding.tvDayTitle.text = "Day ${day.dayNumber}"

            // Show existing date if any
            day.date?.let { ms ->
                if (selectedDateMillis == null) {
                    selectedDateMillis = ms
                    val formatted = dateFormat.format(Date(ms))
                    binding.tvSelectedDate.text = "Date: $formatted"
                    binding.tvSelectedDate.visibility = View.VISIBLE
                    binding.btnPickDate.text = formatted
                }
            }

            // Populate description only once (don't overwrite user edits)
            if (binding.etDayDescription.text.isNullOrEmpty() && !day.description.isNullOrBlank()) {
                binding.etDayDescription.setText(day.description)
            }

            // Update items RecyclerView
            val items = day.items
            if (items.isEmpty()) {
                binding.tvNoItems.visibility = View.VISIBLE
                binding.rvDayItems.visibility = View.GONE
            } else {
                binding.tvNoItems.visibility = View.GONE
                binding.rvDayItems.visibility = View.VISIBLE
                itemsAdapter.submitList(items)
            }

            // Update excluded post IDs so already-added posts show "Added"
            val addedPostIds = items.mapNotNull { it.postId }.toSet()
            postsAdapter.updateExcludedIds(addedPostIds)
        }

        viewModel.availablePosts.observe(viewLifecycleOwner) { posts ->
            if (posts.isEmpty()) {
                binding.tvNoPostsHint.visibility = View.VISIBLE
            } else {
                binding.tvNoPostsHint.visibility = View.GONE
                postsAdapter.submitList(posts)
            }
        }

        viewModel.placeSuggestions.observe(viewLifecycleOwner) { suggestions ->
            if (suggestions.isEmpty()) {
                binding.rvSearchSuggestions.visibility = View.GONE
            } else {
                binding.rvSearchSuggestions.visibility = View.VISIBLE
                suggestionsAdapter.submitList(suggestions)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnSaveDay.isEnabled = !loading
        }

        viewModel.isPlaceLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressPlaceSearch.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.itemAdded.observe(viewLifecycleOwner) { added ->
            if (added == true) {
                Snackbar.make(binding.root, "Item added to day!", Snackbar.LENGTH_SHORT).show()
                viewModel.clearItemAdded()
            }
        }

        viewModel.daySaved.observe(viewLifecycleOwner) { saved ->
            if (saved == true) {
                Snackbar.make(binding.root, "Day saved!", Snackbar.LENGTH_SHORT).show()
                viewModel.clearDaySaved()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    override fun onDestroyView() {
        searchRunnable?.let { binding.root.handler.removeCallbacks(it) }
        super.onDestroyView()
        _binding = null
    }

    // ─────────────────── Inner: Place suggestion adapter ───────────────────

    private class PlaceSuggestionAdapter(
        private val onClick: (LocationSuggestion) -> Unit
    ) : ListAdapter<LocationSuggestion, PlaceSuggestionAdapter.SuggestionViewHolder>(SuggestionDiffCallback()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
            val binding = ItemPlaceSuggestionBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return SuggestionViewHolder(binding, onClick)
        }

        override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        class SuggestionViewHolder(
            private val binding: ItemPlaceSuggestionBinding,
            private val onClick: (LocationSuggestion) -> Unit
        ) : RecyclerView.ViewHolder(binding.root) {
            fun bind(suggestion: LocationSuggestion) {
                binding.tvSuggestionName.text = suggestion.displayName
                val detail = listOfNotNull(suggestion.city, suggestion.country).joinToString(", ")
                if (detail.isNotBlank()) {
                    binding.tvSuggestionDescription.text = detail
                    binding.tvSuggestionDescription.visibility = View.VISIBLE
                } else {
                    binding.tvSuggestionDescription.visibility = View.GONE
                }
                binding.root.setOnClickListener { onClick(suggestion) }
            }
        }

        class SuggestionDiffCallback : DiffUtil.ItemCallback<LocationSuggestion>() {
            override fun areItemsTheSame(oldItem: LocationSuggestion, newItem: LocationSuggestion) =
                oldItem.googlePlaceId == newItem.googlePlaceId && oldItem.displayName == newItem.displayName
            override fun areContentsTheSame(oldItem: LocationSuggestion, newItem: LocationSuggestion) =
                oldItem == newItem
        }
    }
}
