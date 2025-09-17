package com.nikhil.earnity.fragments

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import com.nikhil.earnity.R
import com.nikhil.earnity.auth.AuthActivity
import com.nikhil.earnity.firebase.FirebaseManager
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileFragment : Fragment() {

    private lateinit var profileImageView: CircleImageView
    private lateinit var editProfileImageView: ImageView
    private lateinit var profileNameTextView: TextView
    private lateinit var profileEmailTextView: TextView
    private lateinit var joinedDateTextView: TextView
    private lateinit var walletBalanceTextView: TextView
    private lateinit var totalEarningsTextView: TextView
    private lateinit var editProfileButton: Button
    private lateinit var withdrawButton: Button
    private lateinit var changePasswordLayout: LinearLayout
    private lateinit var logoutLayout: LinearLayout
    private lateinit var progressBar: ProgressBar

    private var selectedImageUri: Uri? = null
    private val firebaseManager = FirebaseManager.getInstance()

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            selectedImageUri = data?.data

            selectedImageUri?.let { uri ->
                Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.default_profile)
                    .into(profileImageView)

                // Upload the image to Cloudinary
                uploadProfileImage(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        profileImageView = view.findViewById(R.id.iv_profile_image)
        editProfileImageView = view.findViewById(R.id.iv_edit_profile_image)
        profileNameTextView = view.findViewById(R.id.tv_profile_name)
        profileEmailTextView = view.findViewById(R.id.tv_profile_email)
        joinedDateTextView = view.findViewById(R.id.tv_joined_date)
        walletBalanceTextView = view.findViewById(R.id.tv_wallet_balance)
        totalEarningsTextView = view.findViewById(R.id.tv_total_earnings)
        editProfileButton = view.findViewById(R.id.btn_edit_profile)
        withdrawButton = view.findViewById(R.id.btn_withdraw)
        changePasswordLayout = view.findViewById(R.id.ll_change_password)
        logoutLayout = view.findViewById(R.id.ll_logout)
        progressBar = view.findViewById(R.id.progress_bar)

        // Set up click listeners
        editProfileImageView.setOnClickListener {
            openImagePicker()
        }

        editProfileButton.setOnClickListener {
            showEditProfileDialog()
        }

        withdrawButton.setOnClickListener {
            showWithdrawDialog()
        }

        changePasswordLayout.setOnClickListener {
            // TODO: Implement change password functionality
            Toast.makeText(requireContext(), "Change password functionality coming soon", Toast.LENGTH_SHORT).show()
        }

        logoutLayout.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        // Load user data
        loadUserData()
    }

    private fun loadUserData() {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = firebaseManager.getCurrentUserData()

            progressBar.visibility = View.GONE

            result.fold(
                onSuccess = { user ->
                    // Display user data
                    profileNameTextView.text = user.name
                    profileEmailTextView.text = user.email

                    // Format the joined date
                    val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    val joinedDate = Date(user.joinedDate)
                    joinedDateTextView.text = "Joined: ${dateFormat.format(joinedDate)}"

                    // Display wallet balance
                    walletBalanceTextView.text = "₹${user.walletBalance}"

                    // Set total earnings to the wallet balance (since we're not tracking tasks anymore)
                    totalEarningsTextView.text = "₹${user.walletBalance}"

                    // Load profile image (from Cloudinary URL)
                    if (user.profileImageUrl.isNotEmpty()) {
                        Glide.with(this@ProfileFragment)
                            .load(user.profileImageUrl)
                            .placeholder(R.drawable.default_profile)
                            .into(profileImageView)
                    }
                },
                onFailure = { exception ->
                    Toast.makeText(requireContext(), "Failed to load user data: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun uploadProfileImage(imageUri: Uri) {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = firebaseManager.uploadProfileImage(imageUri)

            progressBar.visibility = View.GONE

            result.fold(
                onSuccess = { downloadUrl ->
                    Toast.makeText(requireContext(), "Profile image updated", Toast.LENGTH_SHORT).show()
                },
                onFailure = { exception ->
                    Toast.makeText(requireContext(), "Failed to upload image: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun showEditProfileDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_edit_profile)

        val nameEditText = dialog.findViewById<TextInputEditText>(R.id.et_name)
        val phoneEditText = dialog.findViewById<TextInputEditText>(R.id.et_phone)
        val cancelButton = dialog.findViewById<Button>(R.id.btn_cancel)
        val saveButton = dialog.findViewById<Button>(R.id.btn_save)

        // Get current user data
        lifecycleScope.launch {
            val result = firebaseManager.getCurrentUserData()

            result.fold(
                onSuccess = { user ->
                    nameEditText.setText(user.name)
                    phoneEditText.setText(user.phone)
                },
                onFailure = { exception ->
                    Toast.makeText(requireContext(), "Failed to load user data: ${exception.message}", Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                }
            )
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val phone = phoneEditText.text.toString().trim()

            if (name.isEmpty()) {
                nameEditText.error = "Name is required"
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            dialog.dismiss()

            // Update user profile
            lifecycleScope.launch {
                val updates = mutableMapOf<String, Any>()
                updates["name"] = name

                if (phone.isNotEmpty()) {
                    updates["phone"] = phone
                }

                val result = firebaseManager.updateUserProfile(updates)

                progressBar.visibility = View.GONE

                result.fold(
                    onSuccess = { user ->
                        // Update UI with new data
                        profileNameTextView.text = user.name
                        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { exception ->
                        Toast.makeText(requireContext(), "Failed to update profile: ${exception.message}", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }

        dialog.show()
    }

    private fun showWithdrawDialog() {
        lifecycleScope.launch {
            val userResult = firebaseManager.getCurrentUserData()

            userResult.fold(
                onSuccess = { user ->
                    val balance = user.walletBalance

                    if (balance < 100) {
                        Toast.makeText(requireContext(), "Minimum withdrawal amount is ₹100", Toast.LENGTH_SHORT).show()
                        return@fold
                    }

                    // Show withdrawal options dialog
                    val dialog = Dialog(requireContext())
                    dialog.setContentView(R.layout.dialog_withdraw)

                    val tvAvailableBalance = dialog.findViewById<TextView>(R.id.tv_available_balance)
                    val btnPaytm = dialog.findViewById<Button>(R.id.btn_paytm)
                    val btnUPI = dialog.findViewById<Button>(R.id.btn_upi)
                    val btnCancel = dialog.findViewById<Button>(R.id.btn_cancel)

                    tvAvailableBalance.text = "₹$balance"

                    btnPaytm.setOnClickListener {
                        dialog.dismiss()
                        showPaymentDetailsDialog("Paytm")
                    }

                    btnUPI.setOnClickListener {
                        dialog.dismiss()
                        showPaymentDetailsDialog("UPI")
                    }

                    btnCancel.setOnClickListener {
                        dialog.dismiss()
                    }

                    dialog.show()
                },
                onFailure = { exception ->
                    Toast.makeText(requireContext(), "Failed to load wallet balance: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun showPaymentDetailsDialog(method: String) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_payment_details)

        val etPaymentId = dialog.findViewById<TextInputEditText>(R.id.et_payment_id)
        val btnSubmit = dialog.findViewById<Button>(R.id.btn_submit)
        val btnCancel = dialog.findViewById<Button>(R.id.btn_cancel)
        val tvPaymentMethod = dialog.findViewById<TextView>(R.id.tv_payment_method)

        tvPaymentMethod.text = "Enter your $method details"

        if (method == "Paytm") {
            etPaymentId.hint = "Paytm Number"
        } else {
            etPaymentId.hint = "UPI ID"
        }

        btnSubmit.setOnClickListener {
            val paymentId = etPaymentId.text.toString().trim()

            if (paymentId.isEmpty()) {
                etPaymentId.error = "This field is required"
                return@setOnClickListener
            }

            // Process withdrawal request
            progressBar.visibility = View.VISIBLE
            dialog.dismiss()

            lifecycleScope.launch {
                // In a real app, you would send this to a backend to process
                // For now, we'll just reduce the wallet balance to simulate the withdrawal

                val userResult = firebaseManager.getCurrentUserData()

                userResult.fold(
                    onSuccess = { user ->
                        // Create a withdrawal request in Firestore
                        val withdrawalRef = firebaseManager.firestore.collection("withdrawals").document()

                        val withdrawalData = hashMapOf(
                            "userId" to user.uid,
                            "amount" to user.walletBalance,
                            "method" to method,
                            "paymentId" to paymentId,
                            "status" to "pending",
                            "timestamp" to System.currentTimeMillis()
                        )

                        withdrawalRef.set(withdrawalData)
                            .addOnSuccessListener {
                                // Reset wallet balance
                                lifecycleScope.launch {
                                    val updateResult = firebaseManager.updateUserProfile(
                                        mapOf("walletBalance" to 0)
                                    )

                                    progressBar.visibility = View.GONE

                                    updateResult.fold(
                                        onSuccess = {
                                            AlertDialog.Builder(requireContext())
                                                .setTitle("Withdrawal Request Submitted")
                                                .setMessage("Your withdrawal request has been submitted and is being processed. You will receive your payment within 24-48 hours.")
                                                .setPositiveButton("OK", null)
                                                .show()

                                            // Reload user data to update the UI
                                            loadUserData()
                                        },
                                        onFailure = { exception ->
                                            Toast.makeText(requireContext(), "Failed to update wallet balance: ${exception.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                            .addOnFailureListener { e ->
                                progressBar.visibility = View.GONE
                                Toast.makeText(requireContext(), "Failed to submit withdrawal request: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    },
                    onFailure = { exception ->
                        progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Failed to process withdrawal: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { dialog, which ->
                // Logout user
                firebaseManager.logout()

                // Navigate to AuthActivity
                startActivity(Intent(requireContext(), AuthActivity::class.java))
                requireActivity().finish()
            }
            .setNegativeButton("No", null)
            .show()
    }
}