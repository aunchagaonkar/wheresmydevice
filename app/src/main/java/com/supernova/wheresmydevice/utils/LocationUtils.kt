package com.supernova.wheresmydevice.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationUtils(private val context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }
    
    fun getCurrentLocation(callback: (Location?) -> Unit) {
        // Check for location permissions
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "Location permission not granted")
            callback(null)
            return
        }
        
        // First try to get the last known location
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    callback(location)
                } else {
                    // If last known location is null, request a fresh location
                    requestFreshLocation(callback)
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "Error getting last location", it)
                requestFreshLocation(callback)
            }
    }
    
    private fun requestFreshLocation(callback: (Location?) -> Unit) {
        // Check for location permissions again
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            callback(null)
            return
        }
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(5000)
            .setMaxUpdateDelayMillis(15000)
            .build()
        
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let {
                    callback(it)
                    
                    // Remove location updates after receiving a location
                    fusedLocationClient.removeLocationUpdates(this)
                } ?: run {
                    callback(null)
                }
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            
            // Set a timeout to stop location updates if no location is received within 30 seconds
            Looper.myLooper()?.let { looper ->
                android.os.Handler(looper).postDelayed({
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                    callback(null)
                }, 30000)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting location updates", e)
            callback(null)
        }
    }
    
    companion object {
        private const val TAG = "LocationUtils"
    }
} 