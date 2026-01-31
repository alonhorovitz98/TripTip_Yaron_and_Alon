package com.example.triptip_yaron_and_alon.ui.post

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
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirebaseAuthDataSource
import com.example.triptip_yaron_and_alon.databinding.FragmentMyPostsBinding
import com.example.triptip_yaron_and_alon.ui.adapter.MyPostsAdapter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class MyPostsFragment : Fragment() {
    
    private var _binding: FragmentMyPostsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: PostViewModel
    private lateinit var postAdapter: MyPostsAdapter
    private val authDataSource = FirebaseAuthDataSource()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyPostsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this)[PostViewModel::class.java]
        
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        
        // Load current user's posts
        loadUserPosts()
    }
    
    private fun loadUserPosts() {
        lifecycleScope.launch {
            val userId = authDataSource.getCurrentUser().firstOrNull()?.id
            if (userId != null) {
                viewModel.loadUserPosts(userId)
            } else {
                binding.tvError.text = "User not authenticated"
                binding.tvError.visibility = View.VISIBLE
            }
        }
    }
    
    private fun setupRecyclerView() {
        postAdapter = MyPostsAdapter(
            onPostClick = { post ->
                // Navigate to post details
                val action = MyPostsFragmentDirections
                    .actionMyPostsFragmentToPostDetailsFragment(post.id)
                findNavController().navigate(action)
            },
            onEditClick = { post ->
                // Navigate to edit post
                val action = MyPostsFragmentDirections
                    .actionMyPostsFragmentToEditPostFragment(post.id)
                findNavController().navigate(action)
            },
            onDeleteClick = { post ->
                // Show delete confirmation dialog
                showDeleteDialog(post.id, post.text.take(50))
            }
        )
        
        binding.rvPosts.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }
    
    private fun setupListeners() {
        // Pull to refresh
        binding.swipeRefresh.setOnRefreshListener {
            loadUserPosts()
        }
    }
    
    private fun observeViewModel() {
        // Observe user posts
        viewModel.userPosts.observe(viewLifecycleOwner) { posts ->
            if (posts.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvPosts.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.rvPosts.visibility = View.VISIBLE
                postAdapter.submitList(posts)
            }
            binding.swipeRefresh.isRefreshing = false
        }
        
        // Observe loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading && (viewModel.userPosts.value == null || viewModel.userPosts.value!!.isEmpty())) {
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
        
        // Observe delete result
        viewModel.deleteResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is com.example.triptip_yaron_and_alon.util.Result.Success -> {
                    Snackbar.make(binding.root, "Post deleted successfully", Snackbar.LENGTH_SHORT).show()
                    // Reload posts
                    loadUserPosts()
                }
                is com.example.triptip_yaron_and_alon.util.Result.Error -> {
                    val errorMessage = result.message ?: "Failed to delete post"
                    Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }
    
    private fun showDeleteDialog(postId: String, postPreview: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?\n\n\"$postPreview...\"")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deletePost(postId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
