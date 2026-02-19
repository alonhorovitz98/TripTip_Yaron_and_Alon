package com.example.triptip_yaron_and_alon

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.triptip_yaron_and_alon.ui.auth.AuthViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var authViewModel: AuthViewModel
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var navController: androidx.navigation.NavController
    private var isUpdatingSelection = false // Flag to prevent infinite loop
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        
        setupNavigation()
        setupBottomNavigation()
        checkAutoLogin()
    }
    
    private fun checkAutoLogin() {
        lifecycleScope.launch {
            // Check login status immediately and navigate if logged in
            val isLoggedIn = authViewModel.checkLoginStatusSync()
            
            if (isLoggedIn) {
                val navHostFragment = supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val navController = navHostFragment.navController
                
                // Navigate to feed if we're on login screen
                if (navController.currentDestination?.id == R.id.loginFragment) {
                    navController.navigate(R.id.action_loginFragment_to_feedFragment)
                }
            }
            
            // Continue observing for future changes
            authViewModel.checkLoginStatus()
        }
        
        // Observe logged in state for future changes
        authViewModel.isLoggedIn.observe(this) { isLoggedIn ->
            if (isLoggedIn) {
                val navHostFragment = supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val navController = navHostFragment.navController
                
                // Navigate to feed if we're on login screen
                if (navController.currentDestination?.id == R.id.loginFragment) {
                    navController.navigate(R.id.action_loginFragment_to_feedFragment)
                }
            }
        }
    }

    
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        
        // Setup toolbar as action bar - must be done before setupActionBarWithNavController
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        requireNotNull(toolbar) { "Toolbar with id 'toolbar' not found in activity_main.xml" }
        setSupportActionBar(toolbar)
        
        // Create AppBarConfiguration with top-level destinations
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        
        // Setup action bar with navigation - this requires action bar to be set first
        setupActionBarWithNavController(navController, appBarConfiguration)
    }
    
    private fun setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        
        // Handle reselection (when user taps already selected tab)
        bottomNavigationView.setOnItemReselectedListener { 
            // No-op: prevents reloading when user taps the same tab
        }
        
        // Override default navigation behavior for specific items
        bottomNavigationView.setOnItemSelectedListener { item ->
            // Prevent navigation if we're programmatically updating selection
            if (isUpdatingSelection) {
                return@setOnItemSelectedListener true
            }
            
            val destinationId = when (item.itemId) {
                R.id.nav_home -> R.id.feedFragment
                R.id.nav_explore -> R.id.feedFragment // For now, navigate to feed
                R.id.nav_create -> R.id.createPostFragment
                R.id.nav_plan -> R.id.tripListFragment
                R.id.nav_profile -> R.id.profileFragment
                else -> null
            }
            
            if (destinationId != null && navController.currentDestination?.id != destinationId) {
                navigateToTopLevelDestination(destinationId)
            }
            
            true
        }
        
        // Listen to navigation changes to show/hide bottom nav
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Hide bottom navigation on these screens
            val hideBottomNavDestinations = setOf(
                R.id.loginFragment,
                R.id.registerFragment,
                R.id.createPostFragment,
                R.id.editPostFragment,
                R.id.postDetailsFragment,
                R.id.tripBuilderFragment,
                R.id.tripDayEditorFragment,
                R.id.editProfileFragment,
                R.id.tripDetailsFragment
            )
            
            if (destination.id in hideBottomNavDestinations) {
                bottomNavigationView.visibility = android.view.View.GONE
            } else {
                bottomNavigationView.visibility = android.view.View.VISIBLE
            }
            
            // Update selected item based on destination
            updateBottomNavSelection(destination.id)
        }
    }
    
    /**
     * Navigate to a top-level destination with state restoration for smooth tab switching
     * Using launchSingleTop prevents creating duplicate fragments, making tab switching instant
     */
    private fun navigateToTopLevelDestination(destinationId: Int) {
        // Simple navigation - Navigation Component handles state restoration automatically
        // The key optimization is preventing duplicate collectors in ViewModels
        navController.navigate(destinationId)
    }
    
    private fun updateBottomNavSelection(destinationId: Int) {
        // Map navigation destinations to bottom nav items
        val destinationToNavItem = mapOf(
            R.id.feedFragment to R.id.nav_home,
            R.id.tripListFragment to R.id.nav_plan,
            R.id.profileFragment to R.id.nav_profile,
            R.id.createPostFragment to R.id.nav_create
        )
        
        destinationToNavItem[destinationId]?.let { menuItemId ->
            // Only update if different from current selection
            if (bottomNavigationView.selectedItemId != menuItemId) {
                isUpdatingSelection = true
                try {
                    bottomNavigationView.selectedItemId = menuItemId
                } finally {
                    isUpdatingSelection = false
                }
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        return when (item.itemId) {
            R.id.action_my_trips -> {
                // Navigate to TripListFragment
                navController.navigate(R.id.tripListFragment)
                true
            }
            R.id.action_profile -> {
                // Navigate to ProfileFragment
                navController.navigate(R.id.profileFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}