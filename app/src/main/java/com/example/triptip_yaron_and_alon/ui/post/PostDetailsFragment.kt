package com.example.triptip_yaron_and_alon.ui.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.databinding.FragmentPostDetailsBinding
import com.example.triptip_yaron_and_alon.ui.adapter.NearbyPlaceAdapter
import com.example.triptip_yaron_and_alon.ui.post.CommentAdapter
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
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
    private lateinit var placesAdapter: NearbyPlaceAdapter
    private lateinit var commentAdapter: CommentAdapter
    
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
        setupCommentsList()
        setupListeners()
        observeViewModel()
        
        // Load post and comments
        viewModel.loadPost(args.postId)
        viewModel.loadComments(args.postId)
        
        binding.mapView.onCreate(savedInstanceState)
    }
    
    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }
    
    override fun onPause() {
        binding.mapView.onPause()
        super.onPause()
    }
    
    override fun onDestroyView() {
        binding.mapView.onDestroy()
        super.onDestroyView()
        _binding = null
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }
    
    private val commentImagePicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val text = binding.etComment.text?.toString()?.trim() ?: ""
            viewModel.addComment(args.postId, text, it)
            binding.etComment.text?.clear()
        }
    }
    
    private fun setupRecyclerView() {
        placesAdapter = NearbyPlaceAdapter(
            onAddToTripClick = { place ->
                // Navigate to TripDayEditorFragment with place info
                // For now, show a message - can be enhanced to navigate to trip builder
                Snackbar.make(
                    binding.root,
                    "Tap 'Add to Trip' to add this place to your trip",
                    Snackbar.LENGTH_LONG
                ).show()
            },
            onPlaceClick = { place ->
                // Navigate to place details if needed
                // For now, do nothing
            }
        )
        
        binding.rvNearbyPlaces.apply {
            adapter = placesAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        }
    }
    
    private fun setupCommentsList() {
        commentAdapter = CommentAdapter()
        binding.rvComments.apply {
            adapter = commentAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        }
    }
    
    private fun setupListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        
        // Share button (placeholder)
        binding.btnShare.setOnClickListener {
            // TODO: Implement share functionality
            Snackbar.make(binding.root, "Share functionality coming soon", Snackbar.LENGTH_SHORT).show()
        }
        
        // Like button (real like/unlike)
        binding.btnLike.setOnClickListener { viewModel.toggleLike(args.postId) }
        
        // Send comment
        binding.btnSendComment.setOnClickListener {
            val text = binding.etComment.text?.toString()?.trim() ?: ""
            viewModel.addComment(args.postId, text, null)
            binding.etComment.text?.clear()
        }
        
        // Add photo to comment (camera icon)
        binding.btnCommentPhoto.setOnClickListener {
            commentImagePicker.launch("image/*")
        }
        
        // Add to Trip button
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
                    // Use coordinates directly
                    viewModel.loadWeather(post.latitude, post.longitude)
                    viewModel.loadNearbyPlaces(post.latitude, post.longitude)
                } else if (post.location != null && post.location.isNotBlank()) {
                    // Geocode location name to get coordinates, then load weather/places
                    viewModel.loadWeatherForLocation(post.location)
                    viewModel.loadNearbyPlacesForLocation(post.location)
                }
            } else {
                Snackbar.make(binding.root, "Post not found", Snackbar.LENGTH_LONG).show()
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
                binding.weatherCard.visibility = View.GONE
            }
        }
        
        // Observe nearby places
        viewModel.nearbyPlaces.observe(viewLifecycleOwner) { places ->
            if (places.isNotEmpty()) {
                binding.tvNearbyPlacesLabel.visibility = View.VISIBLE
                binding.rvNearbyPlaces.visibility = View.VISIBLE
                placesAdapter.submitList(places)
            } else {
                binding.tvNearbyPlacesLabel.visibility = View.GONE
                binding.rvNearbyPlaces.visibility = View.GONE
            }
        }
        
        viewModel.placesLoading.observe(viewLifecycleOwner) { isLoading ->
            // Show loading state if needed
        }
        
        viewModel.placesError.observe(viewLifecycleOwner) { error ->
            // Don't show error for places - just keep it hidden
            if (error != null) {
                binding.tvNearbyPlacesLabel.visibility = View.GONE
                binding.rvNearbyPlaces.visibility = View.GONE
            }
        }
        
        viewModel.isLikedByCurrentUser.observe(viewLifecycleOwner) { liked ->
            binding.btnLike.setIconResource(
                if (liked) com.example.triptip_yaron_and_alon.R.drawable.ic_heart_filled
                else com.example.triptip_yaron_and_alon.R.drawable.ic_heart
            )
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Progress bar not in new layout - show loading via button state
            binding.btnAddToTrip.isEnabled = !isLoading
            binding.btnAddToTrip.text = if (isLoading) "Loading..." else "Add to Trip"
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
            }
        }
        
        viewModel.comments.observe(viewLifecycleOwner) { comments ->
            commentAdapter.submitList(comments)
        }
    }
    
    private fun displayPost(post: com.example.triptip_yaron_and_alon.domain.model.Post) {
        // Title (use post text or location as title)
        binding.tvTitle.text = post.text.take(50).ifEmpty { post.location ?: "Post" }
        
        // Tag chip (can be based on location or category)
        binding.chipTag.text = post.location?.uppercase() ?: "TRAVEL"
        
        // User info
        binding.tvUsername.text = post.userName.ifEmpty { "User ${post.userId.take(8)}" }
        
        // Location + time
        val locationTime = buildString {
            if (post.location != null) {
                append(post.location)
            }
            append(" • ${formatTimestamp(post.createdAt)}")
        }
        binding.tvLocationTime.text = locationTime
        
        // User profile image
        if (post.userImageUrl != null) {
            binding.ivUserProfile.load(post.userImageUrl) {
                placeholder(R.drawable.ic_profile_frame)
                error(R.drawable.ic_profile_frame)
            }
        } else {
            binding.ivUserProfile.setImageResource(R.drawable.ic_profile_frame)
        }
        
        // Post text (description)
        binding.tvPostText.text = post.text
        
        // Post image - Coil handles file errors gracefully (URL or file path)
        if (post.imageUrl != null) {
            binding.ivPostImage.visibility = View.VISIBLE
            if (post.imageUrl!!.startsWith("http", ignoreCase = true)) {
                binding.ivPostImage.load(post.imageUrl) {
                    placeholder(R.drawable.ic_launcher_background)
                    error(R.drawable.ic_launcher_background)
                }
            } else {
                try {
                    binding.ivPostImage.load(java.io.File(post.imageUrl)) {
                        placeholder(R.drawable.ic_launcher_background)
                        error(R.drawable.ic_launcher_background)
                    }
                } catch (e: Exception) {
                    binding.ivPostImage.visibility = View.GONE
                }
            }
        } else {
            binding.ivPostImage.visibility = View.GONE
        }
        
        // Price level (Google 0-4): only show when we have real data
        val level = post.priceLevel?.coerceIn(0, 4)
        if (level != null) {
            binding.pricingSection.visibility = View.VISIBLE
            binding.tvPrice.text = when (level) {
                0 -> "Free"
                1 -> "$"
                2 -> "$$"
                3 -> "$$$"
                4 -> "$$$$"
                else -> ""
            }
        } else {
            binding.pricingSection.visibility = View.GONE
        }
        
        // Map: show real Google map only when we have coordinates
        val lat = post.latitude
        val lng = post.longitude
        if (lat != null && lng != null) {
            binding.tvLocationLabel.visibility = View.VISIBLE
            binding.mapCard.visibility = View.VISIBLE
            binding.mapView.getMapAsync { googleMap ->
                googleMap.uiSettings.isMapToolbarEnabled = false
                val pos = LatLng(lat, lng)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 14f))
                googleMap.addMarker(
                    MarkerOptions().position(pos).title(post.location ?: "Location")
                )
            }
        } else {
            binding.tvLocationLabel.visibility = View.GONE
            binding.mapCard.visibility = View.GONE
        }
        
        // Weather and Places will be loaded if coordinates are available
        // They are observed separately in observeViewModel()
    }
    
    private fun displayWeather(weather: com.example.triptip_yaron_and_alon.domain.model.WeatherInfo) {
        binding.apply {
            weatherCard.visibility = View.VISIBLE
            
            // Temperature (large, bold)
            tvTemperature.text = "${weather.temperature}°C"
            
            // Condition with icon
            tvCondition.text = weather.description.replaceFirstChar { it.uppercase() }
            
            // Wind speed
            tvWind.text = "${weather.windSpeed}km/h"
            
            // Humidity
            tvHumidity.text = "${weather.humidity}%"
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
    
}
