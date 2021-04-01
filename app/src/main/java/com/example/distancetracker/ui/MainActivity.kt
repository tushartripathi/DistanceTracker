package com.example.distancetracker.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.example.distancetracker.R
import com.example.distancetracker.util.Permission.hasLocationPermission

class MainActivity : AppCompatActivity() {

    private lateinit var  navController: NavController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navController = findNavController(R.id.fragment)

        if(hasLocationPermission(this))
        {
            navController.navigate(R.id.action_permissonFragment_to_mapsFragment)
        }
    }
}