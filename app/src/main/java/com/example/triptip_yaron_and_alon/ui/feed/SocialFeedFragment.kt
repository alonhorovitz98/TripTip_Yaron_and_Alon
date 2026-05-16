package com.example.triptip_yaron_and_alon.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.databinding.FragmentSocialFeedBinding
import com.example.triptip_yaron_and_alon.ui.notifications.NotificationsViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

/**
 * Social Feed Fragment with three tabs: Trending, Following, and Nearby
 */
class SocialFeedFragment : Fragment() {

    private var _binding: FragmentSocialFeedBinding? = null
    private val binding get() = _binding!!

    private lateinit var notificationsViewModel: NotificationsViewModel

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

        notificationsViewModel = ViewModelProvider(requireActivity())[NotificationsViewModel::class.java]
        notificationsViewModel.loadNotifications()

        setupViewPager()
        setupListeners()
        observeUnreadCount()
    }

    private fun observeUnreadCount() {
        notificationsViewModel.unreadCount.observe(viewLifecycleOwner) { count ->
            val badge = binding.tvNotificationBadge
            if (count > 0) {
                badge.visibility = View.VISIBLE
                badge.text = if (count > 9) "9+" else count.toString()
            } else {
                badge.visibility = View.GONE
            }
        }
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
        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.action_feedFragment_to_notificationsFragment)
        }

        binding.btnShowMap.setOnClickListener {
            findNavController().navigate(R.id.action_feedFragment_to_postsMapFragment)
        }

        binding.fabCreatePost.setOnClickListener {
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
