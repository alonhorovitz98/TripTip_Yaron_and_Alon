package com.example.triptip_yaron_and_alon.ui.profile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import coil.load
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.databinding.FragmentEditProfileBinding
import com.example.triptip_yaron_and_alon.util.Result
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class EditProfileFragment : Fragment() {
    
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: ProfileViewModel
    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null
    
    // Image picker launcher — modern Photo Picker (Android 13+), auto-fallback on older devices
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            displayImagePreview(it)
        }
    }
    
    // Camera launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            selectedImageUri = cameraImageUri
            displayImagePreview(cameraImageUri!!)
        }
    }
    
    // Camera permission launcher
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Snackbar.make(
                binding.root,
                "Camera permission is required to take photos",
                Snackbar.LENGTH_LONG
            ).show()
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
            showImageSourceDialog()
        }
        
        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }
    }
    
    /**
     * Show bottom sheet dialog with options: Take Photo, Choose from Gallery, Cancel
     */
    private fun showImageSourceDialog() {
        val options = arrayOf(
            getString(R.string.take_photo),
            getString(R.string.choose_from_gallery),
            getString(R.string.cancel)
        )
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.select_photo_source))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera() // Take Photo
                    1 -> openImagePicker() // Choose from Gallery
                    2 -> {} // Cancel - do nothing
                }
            }
            .show()
    }
    
    /**
     * Open camera to take a photo.
     * Checks for camera permission first.
     */
    private fun openCamera() {
        // Check camera permission
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        
        // Create a file for the photo
        val photoFile = createImageFile()
        photoFile?.let { file ->
            // Get URI using FileProvider
            cameraImageUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            cameraLauncher.launch(cameraImageUri)
        } ?: run {
            Snackbar.make(
                binding.root,
                "Failed to create image file",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }
    
    /**
     * Create a temporary image file for camera capture.
     */
    private fun createImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "JPEG_${timeStamp}_"
            val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            File.createTempFile(imageFileName, ".jpg", storageDir)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Open gallery to pick an existing image.
     */
    private fun openImagePicker() {
        imagePickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
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
