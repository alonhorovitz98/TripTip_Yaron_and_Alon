package com.example.triptip_yaron_and_alon.ui.place

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.triptip_yaron_and_alon.databinding.FragmentPlaceDetailsReviewsBinding
import com.example.triptip_yaron_and_alon.data.remote.api.dto.PlaceDetailsResultDto
import com.example.triptip_yaron_and_alon.ui.adapter.ReviewAdapter

/**
 * Reviews tab fragment for place details.
 * Shows user reviews in a RecyclerView.
 */
class PlaceDetailsReviewsFragment : Fragment() {
    
    private var _binding: FragmentPlaceDetailsReviewsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var reviewAdapter: ReviewAdapter
    private var placeDetails: PlaceDetailsResultDto? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaceDetailsReviewsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        
        // Display data if already set
        placeDetails?.let { displayReviews(it) }
    }
    
    fun setPlaceDetails(details: PlaceDetailsResultDto) {
        placeDetails = details
        if (view != null) {
            displayReviews(details)
        }
    }
    
    private fun setupRecyclerView() {
        reviewAdapter = ReviewAdapter()
        
        binding.rvReviews.apply {
            adapter = reviewAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }
    
    private fun displayReviews(details: PlaceDetailsResultDto) {
        if (!details.reviews.isNullOrEmpty()) {
            reviewAdapter.submitList(details.reviews)
        } else {
            // Show empty state if needed
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
