package com.example.triptip_yaron_and_alon.ui.place

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.databinding.FragmentGooglePlaceDetailsBinding
import com.example.triptip_yaron_and_alon.ui.adapter.PhotoCarouselAdapter
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator

/**
 * Google Place Details Fragment - Shows full details of a place including photos, reviews, opening hours, etc.
 */
class GooglePlaceDetailsFragment : Fragment() {
    
    private var _binding: FragmentGooglePlaceDetailsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: GooglePlaceDetailsViewModel by viewModels()
    private val args: GooglePlaceDetailsFragmentArgs by navArgs()
    
    private lateinit var photoCarouselAdapter: PhotoCarouselAdapter
    private lateinit var tabsAdapter: PlaceDetailsTabsAdapter
    private var currentPlaceDetails: com.example.triptip_yaron_and_alon.data.remote.api.dto.PlaceDetailsResultDto? = null
    
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
        
        // Set image section to 40% of screen height and update scroll view padding
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val imageHeight = (screenHeight * 0.4f).toInt()
        binding.imageSection.layoutParams.height = imageHeight
        binding.imageSection.requestLayout()
        
        // Update NestedScrollView padding to match image height
        binding.nestedScrollView.setPadding(
            binding.nestedScrollView.paddingLeft,
            imageHeight,
            binding.nestedScrollView.paddingRight,
            binding.nestedScrollView.paddingBottom
        )
        
        setupPhotoCarousel()
        setupTabs()
        setupParallaxScrolling()
        setupListeners()
        observeViewModel()
        
        // Load place details using placeId from args
        viewModel.loadPlaceDetails(args.placeId)
    }
    
    private fun setupParallaxScrolling() {
        // Image stays fixed - no parallax scrolling to prevent content from going over image
        // The CoordinatorLayout behavior handles the positioning
    }
    
    private fun setupPhotoCarousel() {
        photoCarouselAdapter = PhotoCarouselAdapter()
        
        binding.viewPagerPhotos.apply {
            adapter = photoCarouselAdapter
            offscreenPageLimit = 1
        }
    }
    
    private fun setupTabs() {
        // Setup ViewPager2 adapter
        tabsAdapter = PlaceDetailsTabsAdapter(childFragmentManager, lifecycle)
        binding.viewPagerTabs.adapter = tabsAdapter
        
        // Connect TabLayout with ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPagerTabs) { tab, position ->
            tab.text = when (position) {
                0 -> "Overview"
                1 -> "Reviews"
                2 -> "Photos"
                else -> ""
            }
        }.attach()
        
        // Pass data to fragments when they're ready
        binding.viewPagerTabs.post {
            currentPlaceDetails?.let { details ->
                updateTabFragments(details)
            }
        }
    }
    
    private fun setupListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        
        // Add to Trip button
        binding.btnAddToTrip.setOnClickListener {
            // TODO: Navigate to trip selection or add directly to a trip
            Snackbar.make(binding.root, "Add to Trip feature coming soon", Snackbar.LENGTH_SHORT).show()
        }
        
        // Photo carousel page change listener for indicators
        binding.viewPagerPhotos.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updatePhotoIndicators(position)
            }
        })
    }
    
    private fun updatePhotoIndicators(currentPosition: Int) {
        val indicator = binding.photoIndicator
        val photoCount = photoCarouselAdapter.itemCount
        
        if (photoCount > 1) {
            indicator.visibility = View.VISIBLE
            indicator.removeAllViews()
            
            for (i in 0 until photoCount) {
                val dot = View(context).apply {
                    layoutParams = ViewGroup.MarginLayoutParams(
                        resources.getDimensionPixelSize(R.dimen.photo_indicator_size),
                        resources.getDimensionPixelSize(R.dimen.photo_indicator_size)
                    ).apply {
                        marginEnd = if (i < photoCount - 1) 8 else 0
                    }
                    background = if (i == currentPosition) {
                        resources.getDrawable(R.drawable.photo_indicator_selected, null)
                    } else {
                        resources.getDrawable(R.drawable.photo_indicator_unselected, null)
                    }
                }
                indicator.addView(dot)
            }
        } else {
            indicator.visibility = View.GONE
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
                binding.nestedScrollView.visibility = View.GONE
                binding.tvError.visibility = View.GONE
            } else {
                binding.progressBar.visibility = View.GONE
                binding.nestedScrollView.visibility = View.VISIBLE
            }
        }
        
        // Observe error
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.tvError.text = error
                binding.tvError.visibility = View.VISIBLE
                binding.nestedScrollView.visibility = View.GONE
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
        
        // Photos - Load all photos in carousel
        try {
            val apiKey = com.example.triptip_yaron_and_alon.BuildConfig.GOOGLE_PLACES_API_KEY
            if (!apiKey.isBlank() && !details.photos.isNullOrEmpty()) {
                val photoUrls = details.photos.map { photo ->
                    "https://maps.googleapis.com/maps/api/place/photo?maxwidth=800&photoreference=${photo.photoReference}&key=$apiKey"
                }
                photoCarouselAdapter.submitList(photoUrls)
                updatePhotoIndicators(0)
            } else {
                // Show placeholder
                photoCarouselAdapter.submitList(listOf())
            }
        } catch (e: Exception) {
            photoCarouselAdapter.submitList(listOf())
        }
        
        // Store details and pass to tab fragments
        currentPlaceDetails = details
        updateTabFragments(details)
    }
    
    private fun updateTabFragments(details: com.example.triptip_yaron_and_alon.data.remote.api.dto.PlaceDetailsResultDto) {
        android.util.Log.d("PlaceDetails", "Updating tab fragments. Reviews count: ${details.reviews?.size ?: 0}")
        
        // Update adapter which will update existing fragments and store for future fragments
        tabsAdapter.updatePlaceDetails(details)
        
        // Also update directly if fragments exist
        val overviewFragment = tabsAdapter.getFragment(0) as? PlaceDetailsOverviewFragment
        val reviewsFragment = tabsAdapter.getFragment(1) as? PlaceDetailsReviewsFragment
        val photosFragment = tabsAdapter.getFragment(2) as? PlaceDetailsPhotosFragment
        
        overviewFragment?.setPlaceDetails(details)
        reviewsFragment?.setPlaceDetails(details)
        photosFragment?.setPlaceDetails(details)
    }
    
    /**
     * ViewPager2 Adapter for place details tabs
     */
    private class PlaceDetailsTabsAdapter(
        fragmentManager: FragmentManager,
        lifecycle: Lifecycle
    ) : FragmentStateAdapter(fragmentManager, lifecycle) {
        
        private val fragments = mutableMapOf<Int, Fragment>()
        private var placeDetails: com.example.triptip_yaron_and_alon.data.remote.api.dto.PlaceDetailsResultDto? = null
        
        fun updatePlaceDetails(details: com.example.triptip_yaron_and_alon.data.remote.api.dto.PlaceDetailsResultDto) {
            placeDetails = details
            // Update existing fragments
            fragments.values.forEach { fragment ->
                when (fragment) {
                    is PlaceDetailsOverviewFragment -> fragment.setPlaceDetails(details)
                    is PlaceDetailsReviewsFragment -> fragment.setPlaceDetails(details)
                    is PlaceDetailsPhotosFragment -> fragment.setPlaceDetails(details)
                }
            }
        }
        
        override fun getItemCount(): Int = 3
        
        override fun createFragment(position: Int): Fragment {
            val fragment = when (position) {
                0 -> PlaceDetailsOverviewFragment()
                1 -> PlaceDetailsReviewsFragment()
                2 -> PlaceDetailsPhotosFragment()
                else -> PlaceDetailsOverviewFragment()
            }
            fragments[position] = fragment
            
            // Set data immediately if available
            placeDetails?.let { details ->
                when (fragment) {
                    is PlaceDetailsOverviewFragment -> fragment.setPlaceDetails(details)
                    is PlaceDetailsReviewsFragment -> fragment.setPlaceDetails(details)
                    is PlaceDetailsPhotosFragment -> fragment.setPlaceDetails(details)
                }
            }
            
            return fragment
        }
        
        fun getFragment(position: Int): Fragment? {
            return fragments[position]
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearDetails()
        _binding = null
    }
}
