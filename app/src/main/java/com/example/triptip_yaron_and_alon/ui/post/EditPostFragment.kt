package com.example.triptip_yaron_and_alon.ui.post

import android.app.Activity
import android.app.AlertDialog
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
import androidx.navigation.fragment.navArgs
import coil.load
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirebaseAuthDataSource
import com.example.triptip_yaron_and_alon.databinding.FragmentEditPostBinding
import com.example.triptip_yaron_and_alon.util.Result
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class EditPostFragment : Fragment() {
    
    private var _binding: FragmentEditPostBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: PostViewModel
    private val args: EditPostFragmentArgs by navArgs()
    private val authDataSource = FirebaseAuthDataSource()
    
    private var selectedImageUri: Uri? = null
    private var originalImageUrl: String? = null
    private var shouldRemoveImage = false
    
    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                shouldRemoveImage = false
                displayImagePreview(uri)
            }
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
            openImagePicker()
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
    
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
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
                
                // Image preview
                originalImageUrl = post.imageUrl
                if (post.imageUrl != null && !shouldRemoveImage && selectedImageUri == null) {
                    binding.ivImagePreview.visibility = View.VISIBLE
                    binding.btnChangeImage.visibility = View.VISIBLE
                    binding.btnRemoveImage.visibility = View.VISIBLE
                    binding.ivImagePreview.load(post.imageUrl) {
                        placeholder(R.drawable.ic_launcher_background)
                        error(R.drawable.ic_launcher_background)
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
