package com.example.triptip_yaron_and_alon.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.triptip_yaron_and_alon.databinding.FragmentFollowingPostsBinding
import com.example.triptip_yaron_and_alon.ui.adapter.PostAdapter

/**
 * "My Posts" tab — shows posts created by the currently logged-in user.
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

        viewModel.loadMyPosts()
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(
            onPostClick = { post ->
                val bundle = android.os.Bundle().apply { putString("postId", post.id) }
                requireParentFragment().requireParentFragment()
                    .let { parentFragment ->
                        androidx.navigation.fragment.NavHostFragment.findNavController(this)
                            .navigate(
                                com.example.triptip_yaron_and_alon.R.id.action_feedFragment_to_postDetailsFragment,
                                bundle
                            )
                    }
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
            viewModel.loadMyPosts()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun observeViewModel() {
        viewModel.currentUserId.observe(viewLifecycleOwner) { id ->
            postAdapter.setCurrentUserId(id)
        }

        viewModel.myPosts.observe(viewLifecycleOwner) { posts ->
            postAdapter.submitList(posts)
            if (posts.isEmpty()) {
                binding.rvPosts.visibility = View.GONE
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.tvEmptyState.text = "You haven't posted anything yet"
            } else {
                binding.rvPosts.visibility = View.VISIBLE
                binding.tvEmptyState.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
