package com.example.triptip_yaron_and_alon.ui.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.databinding.FragmentPostDetailsBinding
import com.example.triptip_yaron_and_alon.ui.adapter.NearbyPlacesAdapter
import com.example.triptip_yaron_and_alon.util.Result
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostDetailsFragment : Fragment() {
    
    private var _binding: FragmentPostDetailsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: PostViewModel
    private val args: PostDetailsFragmentArgs by navArgs()
    private lateinit var placesAdapter: NearbyPlacesAdapter
    
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
        
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        
        // Load post
        viewModel.loadPost(args.postId)
    }
    
    private fun setupRecyclerView() {
        placesAdapter = NearbyPlacesAdapter { place ->
            // Handle place click - could navigate to place details or show info
            Snackbar.make(binding.root, "Place: ${place.name}", Snackbar.LENGTH_SHORT).show()
        }
        
        binding.rvNearbyPlaces.apply {
            adapter = placesAdapter
            layoutManager = LinearLayoutManager(context)
        }
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
                
                // Load weather and places if coordinates are available
                if (post.latitude != null && post.longitude != null) {
                    viewModel.loadWeather(post.latitude, post.longitude)
                    viewModel.loadNearbyPlaces(post.latitude, post.longitude)
                }
            } else {
                binding.tvError.text = "Post not found"
                binding.tvError.visibility = View.VISIBLE
            }
        }
        
        // Observe weather
        viewModel.weather.observe(viewLifecycleOwner) { weather ->
            if (weather != null) {
                displayWeather(weather)
            }
        }
        
        viewModel.weatherLoading.observe(viewLifecycleOwner) { isLoading ->
            // Weather loading is handled in displayWeather
        }
        
        viewModel.weatherError.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                // Don't show error for weather - just keep it hidden
                binding.cardWeather.visibility = View.GONE
            }
        }
        
        // Observe nearby places
        viewModel.nearbyPlaces.observe(viewLifecycleOwner) { places ->
            if (places.isNotEmpty()) {
                displayPlaces(places)
            } else {
                binding.tvNearbyPlacesTitle.visibility = View.GONE
                binding.rvNearbyPlaces.visibility = View.GONE
            }
        }
        
        viewModel.placesLoading.observe(viewLifecycleOwner) { isLoading ->
            // Places loading is handled in displayPlaces
        }
        
        viewModel.placesError.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                // Don't show error for places - just keep them hidden
                binding.tvNearbyPlacesTitle.visibility = View.GONE
                binding.rvNearbyPlaces.visibility = View.GONE
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
        
        // Post image - Coil handles file errors gracefully
        if (post.imageUrl != null) {
            binding.ivPostImage.visibility = View.VISIBLE
            try {
                val imageFile = java.io.File(post.imageUrl)
                binding.ivPostImage.load(imageFile) {
                    placeholder(R.drawable.ic_launcher_background)
                    error(R.drawable.ic_launcher_background)
                    // Coil will handle missing files automatically
                }
            } catch (e: Exception) {
                // If file path is invalid, hide image view
                binding.ivPostImage.visibility = View.GONE
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
        
        // Weather and Places will be loaded if coordinates are available
        // They are observed separately in observeViewModel()
    }
    
    private fun displayWeather(weather: com.example.triptip_yaron_and_alon.domain.model.WeatherInfo) {
        binding.apply {
            cardWeather.visibility = View.VISIBLE
            
            // Weather description
            tvWeatherDescription.text = weather.description
            
            // Weather details
            val details = buildString {
                append("${weather.temperature}°C")
                append(" • ${weather.humidity}% humidity")
                append(" • ${weather.windSpeed} km/h wind")
            }
            tvWeatherDetails.text = details
            
            // Weather icon (using emoji or placeholder)
            // Note: Weather icon URL would need to be loaded with Coil if available
            ivWeatherIcon.setImageResource(R.drawable.ic_launcher_foreground)
        }
    }
    
    private fun displayPlaces(places: List<com.example.triptip_yaron_and_alon.domain.model.PlaceInfo>) {
        binding.apply {
            tvNearbyPlacesTitle.visibility = View.VISIBLE
            rvNearbyPlaces.visibility = View.VISIBLE
            placesAdapter.submitList(places)
        }
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
