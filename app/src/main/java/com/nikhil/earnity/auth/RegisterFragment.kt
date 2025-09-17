package com.nikhil.earnity.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.nikhil.earnity.MainActivity
import com.nikhil.earnity.R
import com.nikhil.earnity.firebase.FirebaseManager
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private lateinit var nameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText
    private lateinit var registerButton: Button
    private lateinit var alreadyAccountText: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        nameEditText = view.findViewById(R.id.et_name)
        emailEditText = view.findViewById(R.id.et_email)
        passwordEditText = view.findViewById(R.id.et_password)
        confirmPasswordEditText = view.findViewById(R.id.et_confirm_password)
        registerButton = view.findViewById(R.id.btn_register)
        alreadyAccountText = view.findViewById(R.id.tv_already_account)
        progressBar = view.findViewById(R.id.progress_bar)

        // Set up click listeners
        registerButton.setOnClickListener {
            registerUser()
        }

        alreadyAccountText.setOnClickListener {
            // Navigate back to LoginFragment
            parentFragmentManager.popBackStack()
        }
    }

    private fun registerUser() {
        val name = nameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val confirmPassword = confirmPasswordEditText.text.toString().trim()

        // Validate input
        if (name.isEmpty()) {
            nameEditText.error = "Name is required"
            nameEditText.requestFocus()
            return
        }

        if (email.isEmpty()) {
            emailEditText.error = "Email is required"
            emailEditText.requestFocus()
            return
        }

        if (password.isEmpty()) {
            passwordEditText.error = "Password is required"
            passwordEditText.requestFocus()
            return
        }

        if (password.length < 6) {
            passwordEditText.error = "Password must be at least 6 characters"
            passwordEditText.requestFocus()
            return
        }

        if (confirmPassword != password) {
            confirmPasswordEditText.error = "Passwords do not match"
            confirmPasswordEditText.requestFocus()
            return
        }

        // Show progress bar
        progressBar.visibility = View.VISIBLE

        // Attempt registration
        lifecycleScope.launch {
            val result = FirebaseManager.getInstance().signUp(email, password, name)

            // Hide progress bar
            progressBar.visibility = View.GONE

            result.fold(
                onSuccess = { user ->
                    // Registration successful, navigate to MainActivity
                    Toast.makeText(requireContext(), "Registration successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(requireActivity(), MainActivity::class.java))
                    requireActivity().finish()
                },
                onFailure = { exception ->
                    // Registration failed
                    Toast.makeText(requireContext(), "Registration failed: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}