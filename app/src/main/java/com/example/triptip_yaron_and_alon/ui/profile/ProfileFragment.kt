package com.example.triptip_yaron_and_alon.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.databinding.FragmentProfileBinding
import com.example.triptip_yaron_and_alon.util.loadProfileImage
import com.example.triptip_yaron_and_alon.util.Result
import com.google.android.material.snackbar.Snackbar

class ProfileFragment : Fragment() {
    
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: ProfileViewModel
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        
        setupListeners()
        observeViewModel()
        
        // Explicitly load profile after view is ready
        viewModel.loadProfile()
    }
    
    private fun setupListeners() {
        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }
        
        binding.btnMyPosts.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_myPostsFragment)
        }
        
        binding.btnLogout.setOnClickListener {
            viewModel.logout()
        }
    }
    
    private fun observeViewModel() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.tvUsername.text = user.name.ifBlank { user.email.substringBefore("@") }
                binding.tvEmail.text = user.email
                
                binding.ivProfileImage.loadProfileImage(user.profileImageUrl)
            }
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.logoutResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    // Navigate back to login
                    findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
                }
                is Result.Error -> {
                    val errorMessage = result.message ?: "An error occurred"
                    Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

