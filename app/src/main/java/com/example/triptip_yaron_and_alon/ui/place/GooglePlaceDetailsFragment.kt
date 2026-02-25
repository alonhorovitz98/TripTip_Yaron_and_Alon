package com.example.triptip_yaron_and_alon.ui.place

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.databinding.FragmentGooglePlaceDetailsBinding
import com.example.triptip_yaron_and_alon.ui.adapter.ReviewAdapter
import com.google.android.material.snackbar.Snackbar

/**
 * Google Place Details Fragment - Shows full details of a place including photos, reviews, opening hours, etc.
 */
class GooglePlaceDetailsFragment : Fragment() {
    
    private var _binding: FragmentGooglePlaceDetailsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: GooglePlaceDetailsViewModel by viewModels()
    private val args: GooglePlaceDetailsFragmentArgs by navArgs()
    
    private lateinit var reviewAdapter: ReviewAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGooglePlaceDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        
        // Load place details using placeId from args
        viewModel.loadPlaceDetails(args.placeId)
    }
    
    private fun setupRecyclerView() {
        reviewAdapter = ReviewAdapter()
        
        binding.rvReviews.apply {
            adapter = reviewAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }
    
    private fun setupListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        
        // Phone click
        binding.tvPhone.setOnClickListener {
            val phone = binding.tvPhone.text.toString()
            if (phone.isNotBlank()) {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                startActivity(intent)
            }
        }
        
        // Website click
        binding.tvWebsite.setOnClickListener {
            val website = binding.tvWebsite.text.toString()
            if (website.isNotBlank()) {
                val url = if (!website.startsWith("http")) "https://$website" else website
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
        }
        
        // Add to Trip button
        binding.btnAddToTrip.setOnClickListener {
            // TODO: Navigate to trip selection or add directly to a trip
            Snackbar.make(binding.root, "Add to Trip feature coming soon", Snackbar.LENGTH_SHORT).show()
        }
    }
    
    private fun observeViewModel() {
        // Observe place details
        viewModel.placeDetails.observe(viewLifecycleOwner) { details ->
            if (details != null) {
                displayPlaceDetails(details)
            }
        }
        
        // Observe loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                binding.progressBar.visibility = View.VISIBLE
                binding.contentSection.visibility = View.GONE
                binding.tvError.visibility = View.GONE
            } else {
                binding.progressBar.visibility = View.GONE
                binding.contentSection.visibility = View.VISIBLE
            }
        }
        
        // Observe error
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.tvError.text = error
                binding.tvError.visibility = View.VISIBLE
                binding.contentSection.visibility = View.GONE
                binding.progressBar.visibility = View.GONE
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
            } else {
                binding.tvError.visibility = View.GONE
            }
        }
    }
    
    private fun displayPlaceDetails(details: com.example.triptip_yaron_and_alon.data.remote.api.dto.PlaceDetailsResultDto) {
        // Place name
        binding.tvPlaceName.text = details.name
        
        // Rating
        if (details.rating != null) {
            binding.ratingLayout.visibility = View.VISIBLE
            binding.tvRating.text = String.format("%.1f", details.rating)
            
            if (details.userRatingsTotal != null && details.userRatingsTotal > 0) {
                binding.tvReviewsCount.text = "(${details.userRatingsTotal} reviews)"
                binding.tvReviewsCount.visibility = View.VISIBLE
            } else {
                binding.tvReviewsCount.visibility = View.GONE
            }
        } else {
            binding.ratingLayout.visibility = View.GONE
        }
        
        // Address
        val address = details.formattedAddress ?: details.vicinity
        if (!address.isNullOrBlank()) {
            binding.tvAddress.text = address
            binding.tvAddress.visibility = View.VISIBLE
        } else {
            binding.tvAddress.visibility = View.GONE
        }
        
        // Categories
        val categories = details.types?.take(3)?.joinToString(", ") ?: ""
        if (categories.isNotBlank()) {
            binding.tvCategories.text = categories
            binding.tvCategories.visibility = View.VISIBLE
        } else {
            binding.tvCategories.visibility = View.GONE
        }
        
        // Opening hours
        if (details.openingHours != null) {
            binding.openingHoursLayout.visibility = View.VISIBLE
            val openNow = details.openingHours.openNow
            val hoursText = if (openNow == true) {
                "Open now"
            } else if (openNow == false) {
                "Closed now"
            } else {
                "Hours not available"
            }
            
            // Add weekday text if available
            val weekdayText = details.openingHours.weekdayText?.firstOrNull()
            if (weekdayText != null) {
                binding.tvOpeningHours.text = "$hoursText • $weekdayText"
            } else {
                binding.tvOpeningHours.text = hoursText
            }
        } else {
            binding.openingHoursLayout.visibility = View.GONE
        }
        
        // Phone
        val phone = details.formattedPhoneNumber ?: details.internationalPhoneNumber
        if (!phone.isNullOrBlank()) {
            binding.phoneLayout.visibility = View.VISIBLE
            binding.tvPhone.text = phone
        } else {
            binding.phoneLayout.visibility = View.GONE
        }
        
        // Website
        if (!details.website.isNullOrBlank()) {
            binding.websiteLayout.visibility = View.VISIBLE
            binding.tvWebsite.text = details.website
        } else {
            binding.websiteLayout.visibility = View.GONE
        }
        
        // Overview / Editorial Summary
        val overview = details.editorialSummary?.overview
        if (!overview.isNullOrBlank()) {
            binding.tvOverview.text = overview
            binding.tvOverview.visibility = View.VISIBLE
        } else {
            binding.tvOverview.visibility = View.GONE
        }
        
        // Photos - Load first photo
        try {
            val apiKey = com.example.triptip_yaron_and_alon.BuildConfig.GOOGLE_PLACES_API_KEY
            if (!apiKey.isBlank() && !details.photos.isNullOrEmpty()) {
                val firstPhoto = details.photos.first()
                val photoUrl = "https://maps.googleapis.com/maps/api/place/photo?maxwidth=800&photoreference=${firstPhoto.photoReference}&key=$apiKey"
                
                binding.ivPlacePhoto.load(photoUrl) {
                    placeholder(R.drawable.ic_placeholder_image)
                    error(R.drawable.ic_placeholder_image)
                    crossfade(true)
                }
            } else {
                binding.ivPlacePhoto.setImageResource(R.drawable.ic_placeholder_image)
            }
        } catch (e: Exception) {
            // Fallback if BuildConfig is not available
            binding.ivPlacePhoto.setImageResource(R.drawable.ic_placeholder_image)
        }
        
        // Reviews
        if (!details.reviews.isNullOrEmpty()) {
            binding.tvReviewsTitle.visibility = View.VISIBLE
            binding.rvReviews.visibility = View.VISIBLE
            reviewAdapter.submitList(details.reviews.take(5)) // Show first 5 reviews
        } else {
            binding.tvReviewsTitle.visibility = View.GONE
            binding.rvReviews.visibility = View.GONE
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearDetails()
        _binding = null
    }
}
