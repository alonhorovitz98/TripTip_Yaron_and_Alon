package com.example.triptip_yaron_and_alon.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
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
import com.example.triptip_yaron_and_alon.util.loadProfileImage
import com.example.triptip_yaron_and_alon.util.Result
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProfileViewModel
    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null

    // Prevents the user observer from clobbering user input every time the Firestore
    // listener re-emits. We only prefill the form on the first emission per view.
    private var hasPrefilledForm = false

    // Modern photo picker — no runtime permission needed, works reliably on emulators
    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            // Eagerly copy the picked content URI into a temp file we fully control.
            // This avoids URI-permission edge cases and ensures the preview + later
            // upload both work even if the picker URI is revoked.
            val localUri = copyUriToCache(uri) ?: uri
            selectedImageUri = localUri
            displayImagePreview(localUri)
        }
    }

    private fun copyUriToCache(uri: Uri): Uri? = try {
        val outFile = File(
            requireContext().cacheDir,
            "picked_${System.currentTimeMillis()}.jpg"
        )
        requireContext().contentResolver.openInputStream(uri)?.use { input ->
            outFile.outputStream().use { output -> input.copyTo(output) }
        }
        if (outFile.length() > 0) Uri.fromFile(outFile) else null
    } catch (_: Exception) {
        null
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            selectedImageUri = cameraImageUri
            displayImagePreview(cameraImageUri!!)
        }
    }

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

        viewModel = ViewModelProvider(requireActivity())[ProfileViewModel::class.java]
        hasPrefilledForm = false

        setupListeners()
        observeViewModel()

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
                    0 -> openCamera()
                    1 -> openImagePicker()
                    2 -> {}
                }
            }
            .show()
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        val photoFile = createImageFile()
        photoFile?.let { file ->
            cameraImageUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            cameraLauncher.launch(cameraImageUri)
        } ?: run {
            Snackbar.make(binding.root, "Failed to create image file", Snackbar.LENGTH_SHORT).show()
        }
    }

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

    private fun openImagePicker() {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun displayImagePreview(uri: Uri) {
        binding.ivProfileImage.load(uri) {
            placeholder(R.drawable.ic_profile_frame)
            error(R.drawable.ic_profile_frame)
            size(240)
            crossfade(true)
            transformations(coil.transform.CircleCropTransformation())
            memoryCachePolicy(coil.request.CachePolicy.DISABLED)
            diskCachePolicy(coil.request.CachePolicy.DISABLED)
        }
    }

    private fun saveProfile() {
        val name = binding.etUsername.text.toString().trim()
        if (name.isBlank()) {
            Snackbar.make(binding.root, "Please enter a username", Snackbar.LENGTH_SHORT).show()
            return
        }
        viewModel.updateProfile(name = name, imageUri = selectedImageUri)
    }

    private fun observeViewModel() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            // Only prefill the form ONCE — otherwise repeated Firestore emissions
            // clobber user input and make the screen feel frozen.
            if (user != null && !hasPrefilledForm) {
                binding.etUsername.setText(user.name)
                binding.etEmail.setText(user.email)
                hasPrefilledForm = true

                if (selectedImageUri == null) {
                    binding.ivProfileImage.loadProfileImage(user.profileImageUrl)
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            // Only disable Save while saving. Never disable inputs or Change Image —
            // that's what made the screen feel locked.
            binding.btnSaveProfile.isEnabled = !isLoading
        }

        viewModel.updateResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    Snackbar.make(binding.root, "Profile updated successfully!", Snackbar.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_editProfileFragment_to_profileFragment)
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
