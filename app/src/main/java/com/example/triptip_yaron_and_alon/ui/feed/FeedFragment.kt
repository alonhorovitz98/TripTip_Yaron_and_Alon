package com.example.triptip_yaron_and_alon.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.databinding.FragmentFeedBinding
import com.example.triptip_yaron_and_alon.ui.adapter.PostAdapter
import com.google.android.material.snackbar.Snackbar

class FeedFragment : Fragment() {
    
    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: FeedViewModel
    private lateinit var postAdapter: PostAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this)[FeedViewModel::class.java]
        
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        
        // Explicitly load posts after view is ready
        viewModel.loadPosts()
    }
    
    private fun setupRecyclerView() {
        postAdapter = PostAdapter { post ->
            // Navigate to PostDetailsFragment
            val action = FeedFragmentDirections.actionFeedFragmentToPostDetailsFragment(post.id)
            findNavController().navigate(action)
        }
        
        binding.rvPosts.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(context)
            
            // Lazy loading
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    
                    // Load more when reaching the end
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                        && totalItemCount >= 10
                    ) {
                        viewModel.loadMorePosts()
                    }
                }
            })
        }
    }
    
    private fun setupListeners() {
        // Pull to refresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshPosts()
        }
        
        // FAB - Create new post
        binding.fabCreatePost.setOnClickListener {
            findNavController().navigate(R.id.action_feedFragment_to_createPostFragment)
        }
        
        // Filter buttons
        binding.btnTrending.setOnClickListener {
            // TODO: Implement filter logic
            setFilterSelected(binding.btnTrending)
        }
        
        binding.btnFollowing.setOnClickListener {
            // TODO: Implement filter logic
            setFilterSelected(binding.btnFollowing)
        }
        
        binding.btnNearby.setOnClickListener {
            // TODO: Implement filter logic
            setFilterSelected(binding.btnNearby)
        }
        
        binding.btnSoloTravel.setOnClickListener {
            // TODO: Implement filter logic
            setFilterSelected(binding.btnSoloTravel)
        }
        
        // Notifications and Messages (placeholder)
        binding.btnNotifications.setOnClickListener {
            // TODO: Navigate to notifications
            Snackbar.make(binding.root, "Notifications coming soon", Snackbar.LENGTH_SHORT).show()
        }
        
        binding.btnMessages.setOnClickListener {
            // TODO: Navigate to messages
            Snackbar.make(binding.root, "Messages coming soon", Snackbar.LENGTH_SHORT).show()
        }
    }
    
    private fun setFilterSelected(selectedButton: com.google.android.material.button.MaterialButton) {
        // Reset all buttons
        binding.btnTrending.apply {
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                if (this == selectedButton) resources.getColor(R.color.orange_primary, null)
                else android.graphics.Color.TRANSPARENT
            ))
            setTextColor(if (this == selectedButton) resources.getColor(R.color.text_white, null)
                else resources.getColor(R.color.orange_primary, null))
            strokeWidth = if (this == selectedButton) 0 else 1
        }
        
        binding.btnFollowing.apply {
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                if (this == selectedButton) resources.getColor(R.color.orange_primary, null)
                else android.graphics.Color.TRANSPARENT
            ))
            setTextColor(if (this == selectedButton) resources.getColor(R.color.text_white, null)
                else resources.getColor(R.color.orange_primary, null))
            strokeWidth = if (this == selectedButton) 0 else 1
        }
        
        binding.btnNearby.apply {
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                if (this == selectedButton) resources.getColor(R.color.orange_primary, null)
                else android.graphics.Color.TRANSPARENT
            ))
            setTextColor(if (this == selectedButton) resources.getColor(R.color.text_white, null)
                else resources.getColor(R.color.orange_primary, null))
            strokeWidth = if (this == selectedButton) 0 else 1
        }
        
        binding.btnSoloTravel.apply {
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                if (this == selectedButton) resources.getColor(R.color.orange_primary, null)
                else android.graphics.Color.TRANSPARENT
            ))
            setTextColor(if (this == selectedButton) resources.getColor(R.color.text_white, null)
                else resources.getColor(R.color.orange_primary, null))
            strokeWidth = if (this == selectedButton) 0 else 1
        }
    }
    
    private fun observeViewModel() {
        // Observe posts
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            if (posts.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvPosts.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.rvPosts.visibility = View.VISIBLE
                postAdapter.submitList(posts)
            }
        }
        
        // Observe loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading && (viewModel.posts.value == null || viewModel.posts.value!!.isEmpty())) {
                binding.progressBar.visibility = View.VISIBLE
            } else {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
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
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

