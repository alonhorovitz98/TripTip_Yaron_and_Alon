package com.example.triptip_yaron_and_alon.ui.post

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.databinding.FragmentCreatePostBinding
import com.example.triptip_yaron_and_alon.domain.model.LocationSuggestion
import com.example.triptip_yaron_and_alon.util.Result
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreatePostFragment : Fragment() {
    
    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: PostViewModel
    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null
    private lateinit var locationSuggestionsAdapter: LocationSuggestionsAdapter
    private val currentLocationSuggestions = mutableListOf<LocationSuggestion>()
    private lateinit var fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null
    private var selectedGooglePlaceId: String? = null
    private var isProgrammaticLocationUpdate: Boolean = false
    
    // Image picker launcher — uses the modern Photo Picker (Android 13+) with
    // automatic fallback to the system file picker on older devices.
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            displayImagePreview(it)
        }
    }
    
    // Location permission launcher
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        
        if (granted) {
            autoFillCurrentLocation()
        } else {
            Snackbar.make(
                binding.root,
                "Location permission denied. You can still type location manually.",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }
    
    // Camera launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            selectedImageUri = cameraImageUri
            displayImagePreview(cameraImageUri!!)
        } else {
            Snackbar.make(
                binding.root,
                "Camera canceled or failed. Try gallery instead.",
                Snackbar.LENGTH_SHORT
            ).show()
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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        locationSuggestionsAdapter = LocationSuggestionsAdapter { selected ->
            onLocationSuggestionSelected(selected)
        }
        binding.rvLocationSuggestions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = locationSuggestionsAdapter
        }
        
        setupLocationAutocomplete()
        setupListeners()
        observeViewModel()
        
        // Ensure screen opens at the top so the Add photos card is visible
        binding.scrollView.post {
            binding.scrollView.scrollTo(0, 0)
        }
        // Prevent location field from stealing focus on open and jumping the layout.
        binding.etLocation.clearFocus()
        
        // Auto-fill location from current GPS on open (if empty)
        if (binding.etLocation.text.isNullOrBlank()) {
            requestLocationPermissionAndAutofill()
        }
    }
    
    private fun setupLocationAutocomplete() {
        binding.etLocation.addTextChangedListener(object : TextWatcher {
            private var searchRunnable: Runnable? = null

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isProgrammaticLocationUpdate) return

                selectedLatitude = null
                selectedLongitude = null
                selectedGooglePlaceId = null

                // Cancel the previous pending search before scheduling a new one
                searchRunnable?.let { binding.etLocation.removeCallbacks(it) }

                searchRunnable = Runnable {
                    val query = s?.toString()?.trim()
                    if (!query.isNullOrBlank()) {
                        viewModel.searchLocationSuggestions(query)
                    } else {
                        currentLocationSuggestions.clear()
                        locationSuggestionsAdapter.submit(emptyList())
                        binding.rvLocationSuggestions.visibility = View.GONE
                    }
                }
                binding.etLocation.postDelayed(searchRunnable, 350)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etLocation.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.etLocation.selectAll()
                val query = binding.etLocation.text?.toString()?.trim()
                if (!query.isNullOrBlank()) {
                    viewModel.searchLocationSuggestions(query)
                }
            } else {
                // Delay hiding so that a tap on a suggestion item registers before the list disappears
                binding.root.postDelayed({
                    binding.rvLocationSuggestions.visibility = View.GONE
                }, 200)
            }
        }

        binding.etLocation.setOnClickListener {
            binding.etLocation.selectAll()
            val query = binding.etLocation.text?.toString()?.trim()
            if (!query.isNullOrBlank()) {
                viewModel.searchLocationSuggestions(query)
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
            Log.d("CreatePostFragment", "Photo card tapped")
            openImagePicker()
        }
        
        // Full photo area should always open source picker
        binding.photoUploadContainer.setOnClickListener {
            Log.d("CreatePostFragment", "Photo container tapped")
            openImagePicker()
        }
        
        // Upload placeholder (also clickable)
        binding.uploadPlaceholder.setOnClickListener {
            Log.d("CreatePostFragment", "Upload placeholder tapped")
            openImagePicker()
        }
        
        // If preview is visible, tapping it should still allow changing media
        binding.ivImagePreview.setOnClickListener {
            Log.d("CreatePostFragment", "Image preview tapped")
            openImagePicker()
        }

        binding.btnTakePhoto.setOnClickListener {
            Log.d("CreatePostFragment", "Take photo button tapped")
            openCamera()
        }

        binding.btnChooseGallery.setOnClickListener {
            Log.d("CreatePostFragment", "Choose gallery button tapped")
            openImagePicker()
        }
        
        // Explicit "Use current location" action (always refreshes from GPS).
        binding.btnUseCurrentLocation.setOnClickListener {
            requestLocationPermissionAndAutofill()
        }
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
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(requireContext().packageManager) == null) {
            Snackbar.make(
                binding.root,
                "No camera app available on this device",
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        val photoFile = createImageFile()
        photoFile?.let { file ->
            try {
                cameraImageUri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file
                )
                cameraLauncher.launch(cameraImageUri)
            } catch (e: Exception) {
                Log.e("CreatePostFragment", "Camera launch failed", e)
                Snackbar.make(
                    binding.root,
                    "Failed to open camera: ${e.localizedMessage ?: "unknown error"}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
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
    
    private fun requestLocationPermissionAndAutofill() {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        if (hasFineLocation || hasCoarseLocation) {
            autoFillCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    
    private fun autoFillCurrentLocation() {
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location: Location? ->
            if (location == null) {
                Snackbar.make(
                    binding.root,
                    "Could not get current location",
                    Snackbar.LENGTH_SHORT
                ).show()
                return@addOnSuccessListener
            }
            
            selectedLatitude = location.latitude
            selectedLongitude = location.longitude
            selectedGooglePlaceId = null
            
            // Resolve coordinates to human-readable place name.
            // scope tied to view lifecycle — avoids NPE if fragment view is destroyed while geolocation/geocoder runs
            viewLifecycleOwner.lifecycleScope.launch {
                val locationText = withContext(Dispatchers.IO) {
                    try {
                        val geocoder = Geocoder(requireContext(), Locale.getDefault())
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        val address = addresses?.firstOrNull()
                        val city = address?.locality ?: address?.subAdminArea
                        val country = address?.countryName
                        listOfNotNull(city, country).joinToString(", ").ifBlank { "Current location" }
                    } catch (e: Exception) {
                        "Current location"
                    }
                }
                val b = _binding ?: return@launch
                isProgrammaticLocationUpdate = true
                b.etLocation.setText(locationText)
                isProgrammaticLocationUpdate = false
                b.etLocation.clearFocus()
                b.scrollView.post {
                    _binding?.scrollView?.scrollTo(0, 0)
                }
            }
        }.addOnFailureListener {
            Snackbar.make(
                binding.root,
                "Could not get current location",
                Snackbar.LENGTH_SHORT
            ).show()
        }
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
        val locationText = binding.etLocation.text.toString().trim().takeIf { it.isNotEmpty() }
        val location = if (!locationText.isNullOrBlank() && !selectedGooglePlaceId.isNullOrBlank()) {
            "${locationText}|${selectedGooglePlaceId}"
        } else {
            locationText
        }

        viewModel.createPost(
            text = text,
            imageUri = selectedImageUri,
            location = location,
            latitude = selectedLatitude,
            longitude = selectedLongitude
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
        
        viewModel.locationSuggestions.observe(viewLifecycleOwner) { suggestions ->
            currentLocationSuggestions.clear()
            currentLocationSuggestions.addAll(suggestions)
            locationSuggestionsAdapter.submit(suggestions)
            // Show when there are results; hide only when the list is empty.
            // The focus-loss handler (with its 200 ms delay) takes care of hiding on dismiss.
            binding.rvLocationSuggestions.visibility =
                if (suggestions.isNotEmpty()) View.VISIBLE else View.GONE
        }
        
        viewModel.locationSuggestionsLoading.observe(viewLifecycleOwner) { isLoading ->
            // Could show a loading indicator here if needed
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onLocationSuggestionSelected(selected: LocationSuggestion) {
        if (selected.googlePlaceId != null && (selected.latitude == 0.0 || selected.longitude == 0.0)) {
            viewModel.fetchPlaceDetails(selected.googlePlaceId)
            selectedGooglePlaceId = selected.googlePlaceId
            selectedLatitude = null
            selectedLongitude = null
        } else {
            selectedGooglePlaceId = selected.googlePlaceId
            selectedLatitude = selected.latitude.takeIf { lat -> lat != 0.0 }
            selectedLongitude = selected.longitude.takeIf { lon -> lon != 0.0 }
        }

        isProgrammaticLocationUpdate = true
        binding.etLocation.setText(selected.displayName)
        binding.etLocation.setSelection(binding.etLocation.text?.length ?: 0)
        isProgrammaticLocationUpdate = false
        binding.rvLocationSuggestions.visibility = View.GONE
        binding.etLocation.clearFocus()
    }

    private inner class LocationSuggestionsAdapter(
        private val onSuggestionClick: (LocationSuggestion) -> Unit
    ) : RecyclerView.Adapter<LocationSuggestionsAdapter.SuggestionViewHolder>() {

        private val items = mutableListOf<LocationSuggestion>()

        fun submit(newItems: List<LocationSuggestion>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
            val view = layoutInflater.inflate(android.R.layout.simple_list_item_1, parent, false)
            return SuggestionViewHolder(view)
        }

        override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class SuggestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val textView: TextView = itemView.findViewById(android.R.id.text1)

            fun bind(item: LocationSuggestion) {
                textView.text = item.displayName
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                itemView.setOnClickListener { onSuggestionClick(item) }
            }
        }
    }
}
