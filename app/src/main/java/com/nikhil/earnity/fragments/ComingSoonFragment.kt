package com.nikhil.earnity.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.nikhil.earnity.R

// Common base class for fragments that will show "Coming Soon"
abstract class ComingSoonFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_coming_soon, container, false)
    }
}

// Specific fragment implementations
class SearchFragment : ComingSoonFragment()
class NotificationsFragment : ComingSoonFragment()
//class ProfileFragment : ComingSoonFragment()
class SettingsFragment : ComingSoonFragment()