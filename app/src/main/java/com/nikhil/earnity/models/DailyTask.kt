package com.nikhil.earnity.models

data class DailyTask(
    val id: Int,
    val title: String,
    val description: String,
    val reward: Int,  // Reward in Rs
    val isAdInstallTask: Boolean,  // true = install app task, false = watch video task
    val isCompleted: Boolean = false,

    val isLocked: Boolean = false,
    val lockTimeRemaining: Long = 0 // Remaining lock time in milliseconds
)