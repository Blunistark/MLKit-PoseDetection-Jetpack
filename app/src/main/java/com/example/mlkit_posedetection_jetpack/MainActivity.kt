package com.example.mlkit_posedetection_jetpack

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mlkit_posedetection_jetpack.camera_usecase.CameraScreen
import com.example.mlkit_posedetection_jetpack.camera_usecase.CameraViewModel
import com.example.mlkit_posedetection_jetpack.ui.EnhancedCameraScreen
import com.example.mlkit_posedetection_jetpack.ui.theme.MLKitPoseDetectionJetpackTheme

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permissions required for camera, speech recognition, and location", Toast.LENGTH_LONG).show()
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request permissions
        checkAndRequestPermissions()
        
        setContent {
            MLKitPoseDetectionJetpackTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Enhanced camera screen with multiple modes
                    EnhancedCameraScreen()
                }
            }
        }
    }
    
    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}