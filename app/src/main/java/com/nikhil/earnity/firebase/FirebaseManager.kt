package com.nikhil.earnity.firebase

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.nikhil.earnity.firebase.CloudinaryManager
import com.nikhil.earnity.models.User
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseManager private constructor() {
    private val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    companion object {
        @Volatile
        private var instance: FirebaseManager? = null

        fun getInstance(): FirebaseManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseManager().also { instance = it }
            }
        }
    }

    // Auth state
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isLoggedIn: Boolean
        get() = currentUser != null

    // Auth operations
    suspend fun signUp(email: String, password: String, name: String): Result<User> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                val user = User(
                    uid = firebaseUser.uid,
                    name = name,
                    email = email,
                    walletBalance = 0,  // Initialize wallet balance to 0
                )

                // Save user to Firestore
                firestore.collection("users")
                    .document(firebaseUser.uid)
                    .set(user)
                    .await()

                Result.success(user)
            } else {
                Result.failure(Exception("Failed to create user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                // Get user from Firestore
                val documentSnapshot = firestore.collection("users")
                    .document(firebaseUser.uid)
                    .get()
                    .await()

                val user = documentSnapshot.toObject(User::class.java)
                    ?: User(uid = firebaseUser.uid, email = email)

                Result.success(user)
            } else {
                Result.failure(Exception("Failed to login"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }

    // User data operations
    suspend fun getCurrentUserData(): Result<User> {
        val uid = currentUser?.uid ?: return Result.failure(Exception("User not logged in"))

        return try {
            val documentSnapshot = firestore.collection("users")
                .document(uid)
                .get()
                .await()

            val user = documentSnapshot.toObject(User::class.java)

            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("User data not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(updates: Map<String, Any>): Result<User> {
        val uid = currentUser?.uid ?: return Result.failure(Exception("User not logged in"))

        return try {
            firestore.collection("users")
                .document(uid)
                .update(updates)
                .await()

            getCurrentUserData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadProfileImage(imageUri: Uri): Result<String> {
        val uid = currentUser?.uid ?: return Result.failure(Exception("User not logged in"))

        return try {
            // Using Cloudinary instead of Firebase Storage
            val cloudinaryManager = CloudinaryManager.getInstance()
            val folderPath = "profile_images/$uid"

            // Upload to Cloudinary
            val imageUrl = cloudinaryManager.uploadImage(imageUri, folderPath)

            // Update user profile with new image URL in Firestore
            updateUserProfile(mapOf("profileImageUrl" to imageUrl))

            Result.success(imageUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Wallet operations
    suspend fun updateWalletBalance(amount: Int, taskId: Int? = null): Result<User> {
        val uid = currentUser?.uid ?: return Result.failure(Exception("User not logged in"))

        return try {
            // First get current wallet balance
            val userResult = getCurrentUserData()

            userResult.fold(
                onSuccess = { user ->
                    val newBalance = user.walletBalance + amount

                    // Updates to send to Firestore
                    val updates = mutableMapOf<String, Any>()
                    updates["walletBalance"] = newBalance

                    // If taskId is provided, record it as a completed task
                    if (taskId != null) {
                        val completedTaskKey = "completedTasks.$taskId"
                        updates[completedTaskKey] = System.currentTimeMillis()
                    }

                    // Update user in Firestore
                    firestore.collection("users")
                        .document(uid)
                        .update(updates)
                        .await()

                    getCurrentUserData()
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetWalletBalance(): Result<User> {
        return updateUserProfile(mapOf("walletBalance" to 0))
    }

    suspend fun submitWithdrawalRequest(method: String, paymentId: String): Result<String> {
        val uid = currentUser?.uid ?: return Result.failure(Exception("User not logged in"))

        return try {
            // Get current user data to get wallet balance
            val userResult = getCurrentUserData()

            userResult.fold(
                onSuccess = { user ->
                    val balance = user.walletBalance

                    if (balance < 100) {
                        return Result.failure(Exception("Minimum withdrawal amount is ₹100"))
                    }

                    // Create withdrawal request
                    val withdrawalId = UUID.randomUUID().toString()
                    val withdrawalData = hashMapOf(
                        "userId" to uid,
                        "amount" to balance,
                        "method" to method,
                        "paymentId" to paymentId,
                        "status" to "pending",
                        "timestamp" to System.currentTimeMillis(),
                        "withdrawalId" to withdrawalId
                    )

                    // Add withdrawal request to Firestore
                    firestore.collection("withdrawals")
                        .document(withdrawalId)
                        .set(withdrawalData)
                        .await()

                    // Reset wallet balance
                    resetWalletBalance()

                    Result.success(withdrawalId)
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}