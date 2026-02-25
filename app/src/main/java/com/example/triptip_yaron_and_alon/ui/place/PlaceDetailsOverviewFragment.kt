package com.example.triptip_yaron_and_alon.ui.place

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.triptip_yaron_and_alon.databinding.FragmentPlaceDetailsOverviewBinding
import com.example.triptip_yaron_and_alon.data.remote.api.dto.PlaceDetailsResultDto

/**
 * Overview tab fragment for place details.
 * Shows opening hours, phone, website, and overview text.
 */
class PlaceDetailsOverviewFragment : Fragment() {
    
    private var _binding: FragmentPlaceDetailsOverviewBinding? = null
    private val binding get() = _binding!!
    
    private var placeDetails: PlaceDetailsResultDto? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaceDetailsOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupListeners()
        
        // Display data if already set
        placeDetails?.let { displayDetails(it) }
    }
    
    fun setPlaceDetails(details: PlaceDetailsResultDto) {
        placeDetails = details
        if (view != null) {
            displayDetails(details)
        }
    }
    
    private fun setupListeners() {
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
    }
    
    private fun displayDetails(details: PlaceDetailsResultDto) {
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
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
