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
import com.example.triptip_yaron_and_alon.databinding.FragmentTrendingPostsBinding
import com.example.triptip_yaron_and_alon.ui.adapter.PostAdapter
import com.example.triptip_yaron_and_alon.util.showError

/**
 * Feed tab — all users' posts from the shared TripTip cloud (Instagram-style home feed).
 */
class TrendingPostsFragment : Fragment() {
    
    private var _binding: FragmentTrendingPostsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: FeedViewModel
    private lateinit var postAdapter: PostAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrendingPostsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[FeedViewModel::class.java]
        
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        
        // Load posts
        viewModel.loadPosts()
    }
    
    private fun setupRecyclerView() {
        postAdapter = PostAdapter(
            onPostClick = { post ->
                val navController = findNavController()
                val bundle = android.os.Bundle().apply { putString("postId", post.id) }
                navController.navigate(
                    com.example.triptip_yaron_and_alon.R.id.action_feedFragment_to_postDetailsFragment,
                    bundle
                )
            },
            onLikeClick = { post ->
                val uid = viewModel.currentUserId.value
                if (uid != null) {
                    if (post.likedBy.contains(uid)) viewModel.unlikePost(post.id)
                    else viewModel.likePost(post.id)
                }
            },
            currentUserId = null
        )
        
        binding.rvPosts.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(context)
            
            // Performance optimizations
            setHasFixedSize(true)
            setItemViewCacheSize(20)
            recycledViewPool.setMaxRecycledViews(0, 10)
            
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
    }
    
    private fun observeViewModel() {
        // Observe posts and current user (for like state)
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
        viewModel.currentUserId.observe(viewLifecycleOwner) { id ->
            postAdapter.setCurrentUserId(id)
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
                showError(error)
            } else {
                binding.tvError.visibility = View.GONE
            }
        }

        viewModel.syncError.observe(viewLifecycleOwner) { syncError ->
            if (!syncError.isNullOrBlank()) {
                binding.tvSyncWarning.text = syncError
                binding.tvSyncWarning.visibility = View.VISIBLE
            } else {
                binding.tvSyncWarning.visibility = View.GONE
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
