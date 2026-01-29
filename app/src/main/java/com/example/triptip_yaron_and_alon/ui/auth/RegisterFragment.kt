package com.example.triptip_yaron_and_alon.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.triptip_yaron_and_alon.R
import com.example.triptip_yaron_and_alon.databinding.FragmentRegisterBinding
import com.example.triptip_yaron_and_alon.util.Result
import com.google.android.material.snackbar.Snackbar

class RegisterFragment : Fragment() {
    
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: AuthViewModel
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        
        setupListeners()
        observeViewModel()
    }
    
    private fun setupListeners() {
        // Clear error when user types
        binding.etName.addTextChangedListener {
            viewModel.clearError()
        }
        
        binding.etEmail.addTextChangedListener {
            viewModel.clearError()
        }
        
        binding.etPassword.addTextChangedListener {
            viewModel.clearError()
        }
        
        binding.etConfirmPassword.addTextChangedListener {
            viewModel.clearError()
        }
        
        // Register button
        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()
            
            // Validate password confirmation
            if (password != confirmPassword) {
                binding.tvError.text = "Passwords do not match"
                binding.tvError.visibility = View.VISIBLE
                Snackbar.make(binding.root, "Passwords do not match", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }
            
            viewModel.register(email, password, name)
        }
        
        // Login button
        binding.btnLogin.setOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun observeViewModel() {
        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                binding.progressBar.visibility = View.VISIBLE
                binding.btnRegister.isEnabled = false
                binding.btnRegister.text = ""
            } else {
                binding.progressBar.visibility = View.GONE
                binding.btnRegister.isEnabled = true
                binding.btnRegister.text = "Register"
            }
        }
        
        // Observe error
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.tvError.text = error
                binding.tvError.visibility = View.VISIBLE
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
            } else {
                binding.tvError.visibility = View.GONE
            }
        }
        
        // Observe register result
        viewModel.registerResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    // Navigate to feed
                    findNavController().navigate(R.id.action_registerFragment_to_feedFragment)
                }
                is Result.Error -> {
                    // Error is handled by error LiveData
                }
                is Result.Loading -> {
                    // Loading is handled by isLoading LiveData
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

