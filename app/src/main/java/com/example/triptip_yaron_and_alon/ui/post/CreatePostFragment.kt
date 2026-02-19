package com.example.triptip_yaron_and_alon.ui.post

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.databinding.FragmentCreatePostBinding
import com.example.triptip_yaron_and_alon.domain.model.LocationSuggestion
import com.example.triptip_yaron_and_alon.util.Result
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CreatePostFragment : Fragment() {
    
    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: PostViewModel
    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null
    private lateinit var locationAdapter: ArrayAdapter<LocationSuggestion>
    
    // Image picker launcher (gallery)
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
        _binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this)[PostViewModel::class.java]
        
        setupLocationAutocomplete()
        setupListeners()
        observeViewModel()
    }
    
    private fun setupLocationAutocomplete() {
        // Create adapter for location suggestions with custom display
        locationAdapter = object : ArrayAdapter<LocationSuggestion>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf<LocationSuggestion>()
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val suggestion = getItem(position)
                if (suggestion != null) {
                    // Display the full location name with better formatting
                    val textView = view as? android.widget.TextView
                    textView?.text = suggestion.displayName
                    // Show city/country as hint if available
                    if (suggestion.city != null || suggestion.country != null) {
                        val subtitle = listOfNotNull(suggestion.city, suggestion.country).joinToString(", ")
                        textView?.hint = subtitle
                    }
                }
                return view
            }
        }
        binding.etLocation.setAdapter(locationAdapter)
        
        // Handle text changes to trigger search
        binding.etLocation.addTextChangedListener(object : TextWatcher {
            private var searchRunnable: Runnable? = null
            
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Cancel previous search
                binding.etLocation.removeCallbacks(searchRunnable)
                
                // Debounce search (wait 300ms after user stops typing - Google Places is fast!)
                searchRunnable = Runnable {
                    val query = s?.toString()?.trim()
                    if (!query.isNullOrBlank() && query.length >= 2) {
                        viewModel.searchLocationSuggestions(query)
                    } else {
                        locationAdapter.clear()
                        locationAdapter.notifyDataSetChanged()
                    }
                }
                binding.etLocation.postDelayed(searchRunnable, 300)
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Handle item selection
        binding.etLocation.setOnItemClickListener { _, _, position, _ ->
            val selected = locationAdapter.getItem(position)
            selected?.let {
                // If it's a Google Places suggestion without coordinates, fetch details first
                if (it.googlePlaceId != null && (it.latitude == 0.0 || it.longitude == 0.0)) {
                    // Fetch place details to get coordinates
                    viewModel.fetchPlaceDetails(it.googlePlaceId)
                    // Store the place_id with the display name for later use
                    val displayText = "${it.displayName}|${it.googlePlaceId}"
                    binding.etLocation.setText(displayText, false)
                } else {
                    // Already has coordinates or is Nominatim result, just use display name
                    binding.etLocation.setText(it.displayName, false)
                }
            }
        }
    }
    
    private fun setupListeners() {
        // Cancel button
        binding.btnCancel.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        
        // Publish button
        binding.btnPublish.setOnClickListener {
            createPost()
        }
        
        // Photo upload card
        binding.photoUploadCard.setOnClickListener {
            showImageSourceDialog()
        }
        
        // Upload placeholder (also clickable)
        binding.uploadPlaceholder.setOnClickListener {
            showImageSourceDialog()
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
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }
    
    private fun displayImagePreview(uri: Uri) {
        binding.ivImagePreview.visibility = View.VISIBLE
        binding.uploadPlaceholder.visibility = View.GONE
        binding.ivImagePreview.load(uri) {
            placeholder(R.drawable.ic_launcher_background)
            error(R.drawable.ic_launcher_background)
        }
    }
    
    private fun createPost() {
        val text = binding.etPostText.text.toString().trim()
        val location = binding.etLocation.text.toString().trim().takeIf { it.isNotEmpty() }
        
        if (text.isBlank()) {
            Snackbar.make(binding.root, "Please enter post text", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        viewModel.createPost(
            text = text,
            imageUri = selectedImageUri,
            location = location,
            latitude = null, // TODO: Add location picker in future
            longitude = null // TODO: Add location picker in future
        )
    }
    
    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnPublish.isEnabled = !isLoading
            binding.photoUploadCard.isEnabled = !isLoading
        }
        
        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    Snackbar.make(binding.root, "Post created successfully!", Snackbar.LENGTH_SHORT).show()
                    // Navigate back to feed
                    findNavController().navigate(R.id.action_createPostFragment_to_feedFragment)
                }
                is Result.Error -> {
                    val errorMessage = result.message ?: "An error occurred"
                    Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
            }
        }
        
        // Observe location suggestions
        viewModel.locationSuggestions.observe(viewLifecycleOwner) { suggestions ->
            locationAdapter.clear()
            locationAdapter.addAll(suggestions)
            locationAdapter.notifyDataSetChanged()
        }
        
        viewModel.locationSuggestionsLoading.observe(viewLifecycleOwner) { isLoading ->
            // Could show a loading indicator here if needed
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
