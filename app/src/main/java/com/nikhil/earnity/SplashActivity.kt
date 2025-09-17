package com.nikhil.earnity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.nikhil.earnity.auth.AuthActivity
import com.nikhil.earnity.firebase.FirebaseManager

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Delay for showing splash screen (2 seconds)
        Handler(Looper.getMainLooper()).postDelayed({
            // Check if user is logged in
            if (FirebaseManager.getInstance().isLoggedIn) {
                // User is logged in, go to MainActivity
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                // User is not logged in, go to AuthActivity
                startActivity(Intent(this, AuthActivity::class.java))
            }
            finish()
        }, 2000)
    }
}