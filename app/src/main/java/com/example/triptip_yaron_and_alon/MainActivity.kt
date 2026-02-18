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
import androidx.navigation.ui.setupWithNavController
import com.example.triptip_yaron_and_alon.ui.auth.AuthViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var authViewModel: AuthViewModel
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var navController: androidx.navigation.NavController
    
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
        
        // Setup bottom navigation with NavController
        bottomNavigationView.setupWithNavController(navController)
        
        // Override default navigation behavior for specific items
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    navController.navigate(R.id.feedFragment)
                    true
                }
                R.id.nav_explore -> {
                    // For now, navigate to feed (explore fragment will be created later)
                    navController.navigate(R.id.feedFragment)
                    true
                }
                R.id.nav_create -> {
                    navController.navigate(R.id.createPostFragment)
                    true
                }
                R.id.nav_plan -> {
                    navController.navigate(R.id.tripListFragment)
                    true
                }
                R.id.nav_profile -> {
                    navController.navigate(R.id.profileFragment)
                    true
                }
                else -> false
            }
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
    
    private fun updateBottomNavSelection(destinationId: Int) {
        // Map navigation destinations to bottom nav items
        val destinationToNavItem = mapOf(
            R.id.feedFragment to R.id.nav_home,
            R.id.tripListFragment to R.id.nav_plan,
            R.id.profileFragment to R.id.nav_profile,
            R.id.createPostFragment to R.id.nav_create
        )
        
        destinationToNavItem[destinationId]?.let { menuItemId ->
            bottomNavigationView.selectedItemId = menuItemId
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