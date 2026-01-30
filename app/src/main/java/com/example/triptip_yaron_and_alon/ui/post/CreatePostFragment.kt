package com.example.triptip_yaron_and_alon.ui.post

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
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.databinding.FragmentCreatePostBinding
import com.example.triptip_yaron_and_alon.util.Result
import com.google.android.material.snackbar.Snackbar

class CreatePostFragment : Fragment() {
    
    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: PostViewModel
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
        _binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this)[PostViewModel::class.java]
        
        setupListeners()
        observeViewModel()
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
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
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
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
