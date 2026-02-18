package com.example.triptip_yaron_and_alon.ui.post

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.databinding.FragmentCreatePostBinding
import com.example.triptip_yaron_and_alon.domain.model.LocationSuggestion
import com.example.triptip_yaron_and_alon.util.Result
import com.google.android.material.snackbar.Snackbar

class CreatePostFragment : Fragment() {
    
    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: PostViewModel
    private var selectedImageUri: Uri? = null
    private lateinit var locationAdapter: ArrayAdapter<LocationSuggestion>
    
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
                    // Display the full location name
                    (view as? android.widget.TextView)?.text = suggestion.displayName
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
                
                // Debounce search (wait 500ms after user stops typing)
                searchRunnable = Runnable {
                    val query = s?.toString()?.trim()
                    if (!query.isNullOrBlank() && query.length >= 2) {
                        viewModel.searchLocationSuggestions(query)
                    } else {
                        locationAdapter.clear()
                        locationAdapter.notifyDataSetChanged()
                    }
                }
                binding.etLocation.postDelayed(searchRunnable, 500)
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Handle item selection
        binding.etLocation.setOnItemClickListener { _, _, position, _ ->
            val selected = locationAdapter.getItem(position)
            selected?.let {
                // Set the full display name
                binding.etLocation.setText(it.displayName, false)
            }
        }
    }
    
    private fun setupListeners() {
        binding.btnAddImage.setOnClickListener {
            openImagePicker()
        }
        
        binding.btnCreatePost.setOnClickListener {
            createPost()
        }
    }
    
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }
    
    private fun displayImagePreview(uri: Uri) {
        binding.ivImagePreview.visibility = View.VISIBLE
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
            binding.btnCreatePost.isEnabled = !isLoading
            binding.btnAddImage.isEnabled = !isLoading
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
                binding.tvError.text = error
                binding.tvError.visibility = View.VISIBLE
            } else {
                binding.tvError.visibility = View.GONE
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
