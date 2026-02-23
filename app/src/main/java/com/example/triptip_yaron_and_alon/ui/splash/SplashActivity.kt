package com.example.triptip_yaron_and_alon.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.triptip_yaron_and_alon.MainActivity
import com.example.triptip_yaron_and_alon.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        lifecycleScope.launch {
            delay(1000) // 1 second splash screen
            checkAuthAndNavigate()
        }
    }
    
    private fun checkAuthAndNavigate() {
        val user = FirebaseAuth.getInstance().currentUser
        
        // Navigate to MainActivity (which will handle routing based on auth state)
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
