package com.nikhil.earnity.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nikhil.earnity.R
import com.nikhil.earnity.adapters.DailyTaskAdapter
import com.nikhil.earnity.models.DailyTask
import com.nikhil.earnity.firebase.FirebaseManager
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions
import kotlinx.coroutines.launch
import java.util.Random

class EarnFragment : Fragment() {

    private lateinit var tvWalletBalance: TextView
    private lateinit var btnWallet: Button
    private lateinit var rvDailyTasks: RecyclerView
    private lateinit var adViewContainer: FrameLayout

    private val dailyTasks = mutableListOf<DailyTask>()
    private var walletBalance = 0
    private var rewardedAd: RewardedAd? = null
    private val firebaseManager = FirebaseManager.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_earn, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize AdMob
        MobileAds.initialize(requireContext()) {}

        // Initialize views
        tvWalletBalance = view.findViewById(R.id.tvWalletBalance)
        btnWallet = view.findViewById(R.id.btnWallet)
        rvDailyTasks = view.findViewById(R.id.rvDailyTasks)
        adViewContainer = view.findViewById(R.id.adViewContainer)

        // Set up wallet button click listener
        btnWallet.setOnClickListener {
            openProfileFragment()
        }

        // Load wallet balance from Firebase
        loadWalletBalanceFromFirebase()

        // Setup tasks
        setupDailyTasks()

        // Setup RecyclerView
        setupRecyclerView()

        // Load banner ad
        loadBannerAd()

        // Preload rewarded ad
        loadRewardedAd()
    }

    private fun openProfileFragment() {
        // Navigate to ProfileFragment
        val profileFragment = ProfileFragment()

        // Replace the current fragment with ProfileFragment
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, profileFragment)
            .addToBackStack(null) // Add to back stack so user can navigate back
            .commit()
    }

    private fun setupDailyTasks() {
        // Clear existing tasks
        dailyTasks.clear()

        // Add 8 watch video tasks (2 Rs reward)
        for (i in 1..8) {
            dailyTasks.add(
                DailyTask(
                    id = i,
                    title = "Watch Video #$i",
                    description = "Watch a short video to earn rewards",
                    reward = 2,
                    isAdInstallTask = false
                )
            )
        }

        // Add 2 install app tasks (5 Rs reward)
        for (i in 9..10) {
            dailyTasks.add(
                DailyTask(
                    id = i,
                    title = "Install App #${i-8}",
                    description = "Watch ad, click and install app to earn rewards",
                    reward = 5,
                    isAdInstallTask = true
                )
            )
        }

        // Shuffle tasks to mix them
        dailyTasks.shuffle(Random())
    }

    private fun setupRecyclerView() {
        rvDailyTasks.layoutManager = LinearLayoutManager(requireContext())
        rvDailyTasks.adapter = DailyTaskAdapter(dailyTasks) { task ->
            // Handle task click
            if (task.isCompleted) {
                Toast.makeText(requireContext(), "Task already completed", Toast.LENGTH_SHORT).show()
                return@DailyTaskAdapter
            }

            if (task.isAdInstallTask) {
                showInstallAppAd(task)
            } else {
                showRewardedVideoAd(task)
            }
        }
    }

    private fun loadBannerAd() {
        val adView = AdView(requireContext())
        adView.adUnitId = "ca-app-pub-3940256099942544/6300978111" // Test Banner Ad ID
        adView.setAdSize(AdSize.BANNER)

        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                adViewContainer.removeAllViews()
                adViewContainer.addView(adView)
            }
        }
    }

    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(requireContext(), "ca-app-pub-3940256099942544/5224354917", adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rewardedAd = null
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad

                    // Set server-side verification options
                    val options = ServerSideVerificationOptions.Builder()
                        .setCustomData("user-id-${firebaseManager.currentUser?.uid ?: "unknown"}")
                        .build()
                    rewardedAd?.setServerSideVerificationOptions(options)
                }
            })
    }

    private fun showRewardedVideoAd(task: DailyTask) {
        if (rewardedAd != null) {
            rewardedAd?.show(requireActivity(), OnUserEarnedRewardListener { rewardItem ->
                // Add reward to wallet and mark task as completed
                addToWallet(task.reward, task.id)
                Toast.makeText(requireContext(), "You earned ₹${task.reward}", Toast.LENGTH_SHORT).show()

                // Preload next rewarded ad
                loadRewardedAd()
            })
        } else {
            Toast.makeText(requireContext(), "Ad not ready yet, please try again later", Toast.LENGTH_SHORT).show()
            loadRewardedAd()
        }
    }

    private fun showInstallAppAd(task: DailyTask) {
        // In a real app, this would use a different ad type like app install ads
        // For this example, we'll use rewarded ads but give a higher reward
        showRewardedVideoAd(task)
    }

    private fun markTaskCompleted(taskId: Int) {
        // Find the task and mark it as completed locally only (no Firestore tracking)
        val index = dailyTasks.indexOfFirst { it.id == taskId }
        if (index != -1) {
            dailyTasks[index] = dailyTasks[index].copy(isCompleted = true)
            rvDailyTasks.adapter?.notifyItemChanged(index)
        }
    }

    private fun addToWallet(amount: Int, taskId: Int) {
        // Update local wallet balance
        walletBalance += amount
        tvWalletBalance.text = "₹$walletBalance"

        // Mark task as completed in UI
        markTaskCompleted(taskId)

        // Update wallet balance in Firebase (without tracking completed tasks)
        lifecycleScope.launch {
            val uid = firebaseManager.currentUser?.uid
            if (uid != null) {
                try {
                    // Get current user data first
                    val userResult = firebaseManager.getCurrentUserData()

                    userResult.fold(
                        onSuccess = { user ->
                            // Calculate new wallet balance
                            val newBalance = user.walletBalance + amount

                            // Create updates map - only update wallet balance
                            val updates = mutableMapOf<String, Any>()
                            updates["walletBalance"] = newBalance

                            // Update user in Firestore
                            lifecycleScope.launch {
                                val updateResult = firebaseManager.updateUserProfile(updates)

                                updateResult.fold(
                                    onSuccess = {
                                        // Balance updated successfully
                                    },
                                    onFailure = { exception ->
                                        Toast.makeText(requireContext(), "Failed to update balance: ${exception.message}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        },
                        onFailure = { exception ->
                            Toast.makeText(requireContext(), "Failed to load user data: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error updating wallet: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadWalletBalanceFromFirebase() {
        lifecycleScope.launch {
            try {
                val result = firebaseManager.getCurrentUserData()

                result.fold(
                    onSuccess = { user ->
                        // Update local wallet balance
                        walletBalance = user.walletBalance
                        tvWalletBalance.text = "₹$walletBalance"
                    },
                    onFailure = { exception ->
                        Toast.makeText(requireContext(), "Failed to load wallet balance: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading wallet balance: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}