package com.nikhil.earnity.firebase

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.HashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CloudinaryManager private constructor(context: Context) {

    companion object {
        @Volatile
        private var instance: CloudinaryManager? = null

        fun initialize(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        // Configure Cloudinary
                        val config = HashMap<String, String>()
                        config["cloud_name"] = "dyg3kmjfp"
                        config["api_key"] = "665733665142341"
                        config["api_secret"] = "YaOrft7ie4V-_9YbAS9uPvxeYtU"
                        config["secure"] = "true"

                        MediaManager.init(context, config)
                        instance = CloudinaryManager(context)
                    }
                }
            }
        }

        fun getInstance(): CloudinaryManager {
            return instance ?: throw IllegalStateException("CloudinaryManager must be initialized before use")
        }
    }

    suspend fun uploadImage(imageUri: Uri, folder: String): String {
        return suspendCancellableCoroutine { continuation ->
            val requestId = MediaManager.get().upload(imageUri)
                .option("folder", folder)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        // Upload started
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        // Upload progress
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        // Image uploaded successfully
                        val secureUrl = resultData["secure_url"] as String
                        continuation.resume(secureUrl)
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        // Error uploading image
                        continuation.resumeWithException(Exception(error.description))
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        // Upload rescheduled
                    }
                })
                .dispatch()

            continuation.invokeOnCancellation {
                MediaManager.get().cancelRequest(requestId)
            }
        }
    }
}