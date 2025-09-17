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

class LoginFragment : Fragment() {

    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var loginButton: Button
    private lateinit var forgotPasswordText: TextView
    private lateinit var noAccountText: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        emailEditText = view.findViewById(R.id.et_email)
        passwordEditText = view.findViewById(R.id.et_password)
        loginButton = view.findViewById(R.id.btn_login)
        forgotPasswordText = view.findViewById(R.id.tv_forgot_password)
        noAccountText = view.findViewById(R.id.tv_no_account)
        progressBar = view.findViewById(R.id.progress_bar)

        // Set up click listeners
        loginButton.setOnClickListener {
            loginUser()
        }

        forgotPasswordText.setOnClickListener {
            // TODO: Implement forgot password functionality
            Toast.makeText(requireContext(), "Forgot password functionality coming soon", Toast.LENGTH_SHORT).show()
        }

        noAccountText.setOnClickListener {
            // Navigate to RegisterFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.auth_fragment_container, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun loginUser() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        // Validate input
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

        // Show progress bar
        progressBar.visibility = View.VISIBLE

        // Attempt login
        lifecycleScope.launch {
            val result = FirebaseManager.getInstance().login(email, password)

            // Hide progress bar
            progressBar.visibility = View.GONE

            result.fold(
                onSuccess = { user ->
                    // Login successful, navigate to MainActivity
                    Toast.makeText(requireContext(), "Login successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(requireActivity(), MainActivity::class.java))
                    requireActivity().finish()
                },
                onFailure = { exception ->
                    // Login failed
                    Toast.makeText(requireContext(), "Login failed: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}