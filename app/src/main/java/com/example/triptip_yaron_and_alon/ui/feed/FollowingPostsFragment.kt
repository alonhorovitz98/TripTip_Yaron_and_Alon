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
import com.example.triptip_yaron_and_alon.databinding.FragmentFollowingPostsBinding
import com.example.triptip_yaron_and_alon.ui.adapter.PostAdapter
import com.google.android.material.snackbar.Snackbar

/**
 * Following Posts Fragment - Shows posts from users the current user follows
 * For now, shows empty state message until following system is implemented
 */
class FollowingPostsFragment : Fragment() {
    
    private var _binding: FragmentFollowingPostsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: FeedViewModel
    private lateinit var postAdapter: PostAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFollowingPostsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[FeedViewModel::class.java]
        
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        
        // For now, show empty state until following system is implemented
        showEmptyState()
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
                viewModel.currentUserId.value?.let { uid ->
                    if (post.likedBy.contains(uid)) viewModel.unlikePost(post.id)
                    else viewModel.likePost(post.id)
                }
            },
            currentUserId = viewModel.currentUserId.value
        )
        
        binding.rvPosts.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }
    }
    
    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            // TODO: Implement following posts refresh
            binding.swipeRefresh.isRefreshing = false
        }
    }
    
    private fun observeViewModel() {
        viewModel.currentUserId.observe(viewLifecycleOwner) { id ->
            postAdapter.setCurrentUserId(id)
        }
        // TODO: Observe followed users' posts when following system is implemented
    }
    
    private fun showEmptyState() {
        binding.rvPosts.visibility = View.GONE
        binding.tvEmptyState.visibility = View.VISIBLE
        binding.tvEmptyState.text = "Follow users to see their posts here"
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
