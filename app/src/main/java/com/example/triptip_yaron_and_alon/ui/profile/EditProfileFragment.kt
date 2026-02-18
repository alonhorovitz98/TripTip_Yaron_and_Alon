package com.example.triptip_yaron_and_alon.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import coil.load
import java.io.File
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.databinding.FragmentEditProfileBinding
import com.example.triptip_yaron_and_alon.util.Result
import com.google.android.material.snackbar.Snackbar

class EditProfileFragment : Fragment() {
    
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: ProfileViewModel
    private var selectedImageUri: Uri? = null
    
    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                displayImagePreview(uri)
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        
        setupListeners()
        observeViewModel()
        
        // Load current profile
        viewModel.loadProfile()
    }
    
    private fun setupListeners() {
        binding.btnChangeImage.setOnClickListener {
            openImagePicker()
        }
        
        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }
    }
    
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }
    
    private fun displayImagePreview(uri: Uri) {
        binding.ivProfileImage.load(uri) {
            placeholder(R.drawable.ic_launcher_foreground)
            error(R.drawable.ic_launcher_foreground)
        }
    }
    
    private fun saveProfile() {
        val name = binding.etUsername.text.toString().trim()
        
        if (name.isBlank()) {
            Snackbar.make(binding.root, "Please enter a username", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        // Update profile with name and optional image
        viewModel.updateProfile(
            name = name,
            imageUri = selectedImageUri
        )
    }
    
    private fun observeViewModel() {
        // Observe user data
        viewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                // Pre-fill form
                binding.etUsername.setText(user.name)
                binding.etEmail.setText(user.email)
                
                // Profile image - Coil handles file errors gracefully
                if (user.profileImageUrl != null && selectedImageUri == null) {
                    try {
                        val imageFile = java.io.File(user.profileImageUrl)
                        binding.ivProfileImage.load(imageFile) {
                            placeholder(R.drawable.ic_launcher_foreground)
                            error(R.drawable.ic_launcher_foreground)
                            // Coil will handle missing files automatically
                        }
                    } catch (e: Exception) {
                        // If file path is invalid, Coil will show error placeholder
                    }
                }
            }
        }
        
        // Observe loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSaveProfile.isEnabled = !isLoading
            binding.btnChangeImage.isEnabled = !isLoading
        }
        
        // Observe update result
        viewModel.updateResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    Snackbar.make(binding.root, "Profile updated successfully!", Snackbar.LENGTH_SHORT).show()
                    // Navigate back to profile
                    findNavController().navigate(R.id.action_editProfileFragment_to_profileFragment)
                }
                is Result.Error -> {
                    val errorMessage = result.message ?: "An error occurred"
                    Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
        
        // Observe error
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.tvError.text = error
                binding.tvError.visibility = View.VISIBLE
            } else {
                binding.tvError.visibility = View.GONE
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
