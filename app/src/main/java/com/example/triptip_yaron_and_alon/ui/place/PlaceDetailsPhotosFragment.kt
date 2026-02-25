package com.example.triptip_yaron_and_alon.ui.place

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.triptip_yaron_and_alon.databinding.FragmentPlaceDetailsPhotosBinding
import com.example.triptip_yaron_and_alon.data.remote.api.dto.PlaceDetailsResultDto
import com.example.triptip_yaron_and_alon.ui.adapter.PhotoGridAdapter

/**
 * Photos tab fragment for place details.
 * Shows all photos in a grid layout.
 */
class PlaceDetailsPhotosFragment : Fragment() {
    
    private var _binding: FragmentPlaceDetailsPhotosBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var photoAdapter: PhotoGridAdapter
    private var placeDetails: PlaceDetailsResultDto? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaceDetailsPhotosBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        
        // Display data if already set
        placeDetails?.let { displayPhotos(it) }
    }
    
    fun setPlaceDetails(details: PlaceDetailsResultDto) {
        placeDetails = details
        if (view != null) {
            displayPhotos(details)
        }
    }
    
    private fun setupRecyclerView() {
        photoAdapter = PhotoGridAdapter()
        
        binding.rvPhotos.apply {
            adapter = photoAdapter
            layoutManager = GridLayoutManager(context, 2).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int = 1
                }
            }
        }
    }
    
    private fun displayPhotos(details: PlaceDetailsResultDto) {
        val apiKey = com.example.triptip_yaron_and_alon.BuildConfig.GOOGLE_PLACES_API_KEY
        if (!apiKey.isBlank() && !details.photos.isNullOrEmpty()) {
            val photoUrls = details.photos.map { photo ->
                "https://maps.googleapis.com/maps/api/place/photo?maxwidth=800&photoreference=${photo.photoReference}&key=$apiKey"
            }
            photoAdapter.submitList(photoUrls)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
