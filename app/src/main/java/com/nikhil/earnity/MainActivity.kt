package com.nikhil.earnity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.nikhil.earnity.fragments.*
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.nikhil.earnity.firebase.CloudinaryManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Cloudinary
        CloudinaryManager.initialize(applicationContext)

        // Initialize the Mobile Ads SDK
        MobileAds.initialize(this) {}

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        // Load default fragment (Home)
        loadFragment(EarnFragment())

        // Set up bottom navigation listener
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_ai -> loadFragment(SearchFragment())
                R.id.nav_portal -> loadFragment(NotificationsFragment())
                R.id.nav_earn -> loadFragment(EarnFragment())
                R.id.nav_skills ->  loadFragment(SettingsFragment())
                R.id.nav_profile -> loadFragment(ProfileFragment())
            }
            true
        }

        // Set default selected item as Home
        bottomNavigation.selectedItemId = R.id.nav_earn
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}