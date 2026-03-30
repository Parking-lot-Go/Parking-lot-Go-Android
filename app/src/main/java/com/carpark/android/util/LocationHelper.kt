package com.carpark.android.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun getCurrentLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null

        val client = LocationServices.getFusedLocationProviderClient(context)

        // 1. lastLocation 먼저 시도 (즉시 반환)
        val last = suspendCancellableCoroutine { cont ->
            try {
                client.lastLocation
                    .addOnSuccessListener { location -> cont.resume(location) }
                    .addOnFailureListener { cont.resume(null) }
            } catch (e: SecurityException) {
                cont.resume(null)
            }
        }
        if (last != null) return last

        // 2. lastLocation 없으면 getCurrentLocation (3초 타임아웃)
        val cancellationToken = CancellationTokenSource()
        return withTimeoutOrNull(3000L) {
            suspendCancellableCoroutine { cont ->
                try {
                    client.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cancellationToken.token
                    ).addOnSuccessListener { location ->
                        cont.resume(location)
                    }.addOnFailureListener {
                        cont.resume(null)
                    }
                } catch (e: SecurityException) {
                    cont.resume(null)
                }

                cont.invokeOnCancellation {
                    cancellationToken.cancel()
                }
            }
        }
    }
}
