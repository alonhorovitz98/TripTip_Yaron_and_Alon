package com.example.triptip_yaron_and_alon

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.triptip_yaron_and_alon.ui.auth.AuthViewModel
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var authViewModel: AuthViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        
        setupNavigation()
        checkAutoLogin()
    }
    
    private fun checkAutoLogin() {
        // Observe logged in state
        authViewModel.isLoggedIn.observe(this) { isLoggedIn ->
            if (isLoggedIn) {
                val navHostFragment = supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val navController = navHostFragment.navController
                
                // Only navigate if we're still on login screen
                if (navController.currentDestination?.id == R.id.loginFragment) {
                    navController.navigate(R.id.action_loginFragment_to_feedFragment)
                }
            }
        }
    }

    
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        // Setup toolbar as action bar - must be done before setupActionBarWithNavController
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        requireNotNull(toolbar) { "Toolbar with id 'toolbar' not found in activity_main.xml" }
        setSupportActionBar(toolbar)
        
        // Create AppBarConfiguration with top-level destinations
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        
        // Setup action bar with navigation - this requires action bar to be set first
        setupActionBarWithNavController(navController, appBarConfiguration)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }
}