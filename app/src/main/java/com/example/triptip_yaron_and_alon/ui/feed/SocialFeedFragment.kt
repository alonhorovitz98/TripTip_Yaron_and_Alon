package com.example.triptip_yaron_and_alon.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.databinding.FragmentSocialFeedBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

/**
 * Social Feed Fragment with three tabs: Trending, Following, and Nearby
 */
class SocialFeedFragment : Fragment() {
    
    private var _binding: FragmentSocialFeedBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSocialFeedBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViewPager()
        setupListeners()
    }
    
    private fun setupViewPager() {
        val adapter = SocialFeedPagerAdapter(this)
        binding.viewPager.adapter = adapter
        
        // Connect TabLayout with ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Trending"
                1 -> "Following"
                2 -> "Nearby"
                else -> ""
            }
        }.attach()
        
        // Set default tab to Trending (index 0)
        binding.viewPager.setCurrentItem(0, false)
    }
    
    private fun setupListeners() {
        // Notifications button
        binding.btnNotifications.setOnClickListener {
            Snackbar.make(binding.root, "Notifications coming soon", Snackbar.LENGTH_SHORT).show()
        }
        
        // FAB - Add new post
        binding.fabCreatePost.setOnClickListener {
            // Navigate to create post
            findNavController().navigate(R.id.action_feedFragment_to_createPostFragment)
        }
    }
    
    /**
     * ViewPager2 Adapter for Social Feed tabs
     */
    private class SocialFeedPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        
        override fun getItemCount(): Int = 3
        
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> TrendingPostsFragment() // Trending tab
                1 -> FollowingPostsFragment() // Following tab
                2 -> NearbyPlacesFragment() // Nearby tab
                else -> TrendingPostsFragment()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
