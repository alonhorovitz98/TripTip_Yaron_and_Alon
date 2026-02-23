package com.example.triptip_yaron_and_alon.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.triptip_yaron_and_alon.databinding.FragmentNearbyPlacesBinding

/**
 * Nearby Places Fragment - Shows Google Places API results (places near user)
 * This will be fully implemented in Phase 3
 */
class NearbyPlacesFragment : Fragment() {
    
    private var _binding: FragmentNearbyPlacesBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNearbyPlacesBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Show placeholder message - will be implemented in Phase 3
        binding.tvPlaceholder.text = "Nearby places will be shown here\n(Phase 3 implementation)"
        binding.tvPlaceholder.visibility = View.VISIBLE
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
