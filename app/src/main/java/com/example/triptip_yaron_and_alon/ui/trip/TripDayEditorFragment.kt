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
        observeViewModel()
        
        // Load day and available posts
        viewModel.loadDay(args.tripId, args.dayId)
        viewModel.loadAvailablePosts()
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
        
        // Available posts adapter
        availablePostsAdapter = AvailablePostsAdapter(
            onAddClick = { post ->
                viewModel.addItemToDay(args.dayId, post.id)
            },
            excludedPostIds = emptySet() // Will be updated when day loads
        )
        
        binding.rvAvailablePosts.apply {
            adapter = availablePostsAdapter
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
                
                // Update excluded post IDs for available posts adapter
                val excludedIds = sortedItems.map { it.postId }.toSet()
                availablePostsAdapter = AvailablePostsAdapter(
                    onAddClick = { post ->
                        viewModel.addItemToDay(args.dayId, post.id)
                    },
                    excludedPostIds = excludedIds
                )
                binding.rvAvailablePosts.adapter = availablePostsAdapter
                
                // Update available posts list
                viewModel.availablePosts.value?.let { posts ->
                    availablePostsAdapter.submitList(posts)
                }
            }
        }
        
        // Observe available posts
        viewModel.availablePosts.observe(viewLifecycleOwner) { posts ->
            val excludedIds = viewModel.currentDay.value?.items?.map { it.postId }?.toSet() ?: emptySet()
            availablePostsAdapter = AvailablePostsAdapter(
                onAddClick = { post ->
                    viewModel.addItemToDay(args.dayId, post.id)
                },
                excludedPostIds = excludedIds
            )
            binding.rvAvailablePosts.adapter = availablePostsAdapter
            availablePostsAdapter.submitList(posts)
        }
        
        // Observe loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // Observe item operation result
        viewModel.itemOperationResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    Snackbar.make(binding.root, "Item updated successfully", Snackbar.LENGTH_SHORT).show()
                }
                is Result.Error -> {
                    Snackbar.make(binding.root, result.message ?: "An error occurred", Snackbar.LENGTH_LONG).show()
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
