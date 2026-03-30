package com.carpark.android

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carpark.android.data.local.SessionManager
import com.carpark.android.ui.MainScreen
import com.carpark.android.ui.login.LoginScreen
import com.carpark.android.ui.theme.CarParkTheme
import com.carpark.android.viewmodel.AuthViewModel
import com.carpark.android.viewmodel.ParkingViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ParkingViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permissions result handled implicitly by LocationHelper */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this) {
            if (!viewModel.handleBackPress()) {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }

        setContent {
            CarParkTheme {
                val isLoggedIn by SessionManager.isLoggedIn.collectAsStateWithLifecycle()

                LaunchedEffect(isLoggedIn) {
                    if (isLoggedIn) {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            )
                        )
                    }
                }

                if (!isLoggedIn) {
                    LoginScreen(
                        viewModel = authViewModel,
                        onLoginSuccess = {
                            authViewModel.validateSession()
                        },
                    )
                } else {
                    MainScreen(
                        viewModel = viewModel,
                        onLogout = {
                            authViewModel.logout()
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        if (authViewModel.isLoggedIn) {
            authViewModel.validateSession()
        }
    }
}
