package com.nikhil.earnity.models

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val profileImageUrl: String = "",
    val joinedDate: Long = System.currentTimeMillis(),
    val walletBalance: Int = 0 // Wallet balance field only (removed completedTasks)
)