package com.example.triptip_yaron_and_alon.ui.post

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirebaseAuthDataSource
import com.example.triptip_yaron_and_alon.databinding.FragmentEditPostBinding
import com.example.triptip_yaron_and_alon.util.Result
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class EditPostFragment : Fragment() {
    
    private var _binding: FragmentEditPostBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: PostViewModel
    private val args: EditPostFragmentArgs by navArgs()
    private val authDataSource = FirebaseAuthDataSource()
    
    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null
    private var originalImageUrl: String? = null
    private var shouldRemoveImage = false
    
    // Image picker launcher — modern Photo Picker (Android 13+), auto-fallback on older devices
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            shouldRemoveImage = false
            displayImagePreview(it)
        }
    }
    
    // Camera launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            selectedImageUri = cameraImageUri
            shouldRemoveImage = false
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
        _binding = FragmentEditPostBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this)[PostViewModel::class.java]
        
        setupListeners()
        observeViewModel()
        
        // Load post data
        viewModel.loadPost(args.postId)
    }
    
    private fun setupListeners() {
        binding.btnChangeImage.setOnClickListener {
            showImageSourceDialog()
        }
        
        binding.btnRemoveImage.setOnClickListener {
            shouldRemoveImage = true
            selectedImageUri = null
            binding.ivImagePreview.visibility = View.GONE
            binding.btnChangeImage.visibility = View.GONE
            binding.btnRemoveImage.visibility = View.GONE
        }
        
        binding.btnSavePost.setOnClickListener {
            savePost()
        }
        
        binding.btnDeletePost.setOnClickListener {
            showDeleteDialog()
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
        binding.ivImagePreview.visibility = View.VISIBLE
        binding.btnChangeImage.visibility = View.VISIBLE
        binding.btnRemoveImage.visibility = View.VISIBLE
        binding.ivImagePreview.load(uri) {
            placeholder(R.drawable.ic_launcher_background)
            error(R.drawable.ic_launcher_background)
        }
    }
    
    private fun savePost() {
        val text = binding.etPostText.text.toString().trim()
        
        if (text.isBlank()) {
            Snackbar.make(binding.root, "Please enter post text", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        // Verify ownership
        val currentPost = viewModel.post.value
        if (currentPost == null) {
            Snackbar.make(binding.root, "Post not loaded yet", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            val currentUserId = authDataSource.getCurrentUser().firstOrNull()?.id
            if (currentUserId != currentPost.userId) {
                Snackbar.make(binding.root, "You can only edit your own posts", Snackbar.LENGTH_LONG).show()
                return@launch
            }
            
            // Determine image URI: use new selection, or null if removed, or keep original
            val imageUri = when {
                shouldRemoveImage -> null
                selectedImageUri != null -> selectedImageUri
                else -> null // Keep original image (don't pass URI)
            }
            
            viewModel.updatePost(
                postId = args.postId,
                text = text,
                imageUri = imageUri
            )
        }
        
    }
    
    private fun showDeleteDialog() {
        val currentPost = viewModel.post.value
        val postPreview = currentPost?.text?.take(50) ?: "this post"
        
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?\n\n\"$postPreview...\"")
            .setPositiveButton("Delete") { _, _ ->
                deletePost()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deletePost() {
        // Verify ownership
        val currentPost = viewModel.post.value
        if (currentPost == null) {
            Snackbar.make(binding.root, "Post not loaded yet", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            val currentUserId = authDataSource.getCurrentUser().firstOrNull()?.id
            if (currentUserId != currentPost.userId) {
                Snackbar.make(binding.root, "You can only delete your own posts", Snackbar.LENGTH_LONG).show()
                return@launch
            }
            
            viewModel.deletePost(args.postId)
        }
    }
    
    private fun observeViewModel() {
        // Observe post data
        viewModel.post.observe(viewLifecycleOwner) { post ->
            if (post != null) {
                // Pre-fill form
                binding.etPostText.setText(post.text)
                
                // Location (read-only)
                if (post.location != null) {
                    binding.etLocation.setText(post.location)
                    binding.tilLocation.visibility = View.VISIBLE
                } else {
                    binding.tilLocation.visibility = View.GONE
                }
                
                // Image preview - Coil handles file errors gracefully
                originalImageUrl = post.imageUrl
                if (post.imageUrl != null && !shouldRemoveImage && selectedImageUri == null) {
                    binding.ivImagePreview.visibility = View.VISIBLE
                    binding.btnChangeImage.visibility = View.VISIBLE
                    binding.btnRemoveImage.visibility = View.VISIBLE
                    try {
                        val imageFile = java.io.File(post.imageUrl)
                        binding.ivImagePreview.load(imageFile) {
                            placeholder(R.drawable.ic_launcher_background)
                            error(R.drawable.ic_launcher_background)
                            // Coil will handle missing files automatically
                        }
                    } catch (e: Exception) {
                        // If file path is invalid, hide image view
                        binding.ivImagePreview.visibility = View.GONE
                    }
                }
            }
        }
        
        // Observe loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSavePost.isEnabled = !isLoading
            binding.btnDeletePost.isEnabled = !isLoading
            binding.btnChangeImage.isEnabled = !isLoading
        }
        
        // Observe operation result (update)
        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    Snackbar.make(binding.root, "Post updated successfully!", Snackbar.LENGTH_SHORT).show()
                    // Navigate back to My Posts
                    findNavController().navigate(R.id.action_editPostFragment_to_myPostsFragment)
                }
                is Result.Error -> {
                    val errorMessage = result.message ?: "An error occurred"
                    Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
        
        // Observe delete result
        viewModel.deleteResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    Snackbar.make(binding.root, "Post deleted successfully", Snackbar.LENGTH_SHORT).show()
                    // Navigate back to My Posts
                    findNavController().navigate(R.id.action_editPostFragment_to_myPostsFragment)
                }
                is Result.Error -> {
                    val errorMessage = result.message ?: "Failed to delete post"
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
