package com.example.mlkit_posedetection_jetpack.camera_usecase

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task

class LocationManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val TAG = "LocationManager"

    fun getCurrentLocation(onLocationReceived: (Double?, Double?) -> Unit) {
        // Check if location permissions are granted
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permissions not granted")
            onLocationReceived(null, null)
            return
        }

        try {
            val locationTask: Task<Location> = fusedLocationClient.lastLocation
            locationTask.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    Log.d(TAG, "Location obtained: lat=$latitude, lng=$longitude")
                    onLocationReceived(latitude, longitude)
                } else {
                    Log.w(TAG, "Location is null, trying to get from LocationManager")
                    getLocationFromLocationManager(onLocationReceived)
                }
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Failed to get location from FusedLocationProvider", exception)
                getLocationFromLocationManager(onLocationReceived)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when requesting location", e)
            onLocationReceived(null, null)
        }
    }

    private fun getLocationFromLocationManager(onLocationReceived: (Double?, Double?) -> Unit) {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            // Check if GPS provider is enabled
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                Log.w(TAG, "No location providers enabled")
                onLocationReceived(null, null)
                return
            }

            // Try GPS provider first
            val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (gpsLocation != null) {
                Log.d(TAG, "GPS location obtained: lat=${gpsLocation.latitude}, lng=${gpsLocation.longitude}")
                onLocationReceived(gpsLocation.latitude, gpsLocation.longitude)
                return
            }

            // Try network provider
            val networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (networkLocation != null) {
                Log.d(TAG, "Network location obtained: lat=${networkLocation.latitude}, lng=${networkLocation.longitude}")
                onLocationReceived(networkLocation.latitude, networkLocation.longitude)
                return
            }

            Log.w(TAG, "No location available from any provider")
            onLocationReceived(null, null)

        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when getting location from LocationManager", e)
            onLocationReceived(null, null)
        }
    }
}
