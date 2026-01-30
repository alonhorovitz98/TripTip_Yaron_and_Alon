package com.example.triptip_yaron_and_alon.ui.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.databinding.FragmentPostDetailsBinding
import com.example.triptip_yaron_and_alon.util.Result
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostDetailsFragment : Fragment() {
    
    private var _binding: FragmentPostDetailsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: PostViewModel
    private val args: PostDetailsFragmentArgs by navArgs()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this)[PostViewModel::class.java]
        
        setupListeners()
        observeViewModel()
        
        // Load post
        viewModel.loadPost(args.postId)
    }
    
    private fun setupListeners() {
        binding.btnAddToTrip.setOnClickListener {
            // Navigate to TripBuilderFragment with postId
            val action = PostDetailsFragmentDirections
                .actionPostDetailsFragmentToTripBuilderFragment(
                    tripId = "new",
                    postId = args.postId
                )
            findNavController().navigate(action)
        }
    }
    
    private fun observeViewModel() {
        viewModel.post.observe(viewLifecycleOwner) { post ->
            if (post != null) {
                displayPost(post)
            } else {
                binding.tvError.text = "Post not found"
                binding.tvError.visibility = View.VISIBLE
            }
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnAddToTrip.isEnabled = !isLoading
        }
        
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
    
    private fun displayPost(post: com.example.triptip_yaron_and_alon.domain.model.Post) {
        // User info
        binding.tvUsername.text = post.userName.ifEmpty { "User ${post.userId.take(8)}" }
        binding.tvTimestamp.text = formatTimestamp(post.createdAt)
        
        // User profile image
        if (post.userImageUrl != null) {
            binding.ivUserProfile.load(post.userImageUrl) {
                placeholder(R.drawable.ic_launcher_foreground)
                error(R.drawable.ic_launcher_foreground)
            }
        } else {
            binding.ivUserProfile.setImageResource(R.drawable.ic_launcher_foreground)
        }
        
        // Post text
        binding.tvPostText.text = post.text
        
        // Post image
        if (post.imageUrl != null) {
            binding.ivPostImage.visibility = View.VISIBLE
            binding.ivPostImage.load(post.imageUrl) {
                placeholder(R.drawable.ic_launcher_background)
                error(R.drawable.ic_launcher_background)
            }
        } else {
            binding.ivPostImage.visibility = View.GONE
        }
        
        // Location
        if (post.location != null) {
            binding.tvLocation.text = "📍 ${post.location}"
            binding.tvLocation.visibility = View.VISIBLE
        } else {
            binding.tvLocation.visibility = View.GONE
        }
        
        // Weather and Places will be added in Step 12.3
        // For now, keep them hidden
        binding.cardWeather.visibility = View.GONE
        binding.tvNearbyPlacesTitle.visibility = View.GONE
        binding.rvNearbyPlaces.visibility = View.GONE
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000} minutes ago"
            diff < 86400000 -> "${diff / 3600000} hours ago"
            diff < 604800000 -> "${diff / 86400000} days ago"
            else -> {
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
