package com.carpark.android.data.local

import android.content.Context
import android.content.SharedPreferences
import com.carpark.android.data.model.NavigationProvider
import com.carpark.android.data.model.ThemeMode
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class AppSettingsPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var preferredNavigation: NavigationProvider
        get() = NavigationProvider.entries.firstOrNull {
            it.name == prefs.getString(KEY_PREFERRED_NAVIGATION, NavigationProvider.NAVER.name)
        } ?: NavigationProvider.NAVER
        set(value) = prefs.edit().putString(KEY_PREFERRED_NAVIGATION, value.name).apply()

    var themeMode: ThemeMode
        get() = ThemeMode.entries.firstOrNull {
            it.name == prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        } ?: ThemeMode.SYSTEM
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value.serverValue).apply()

    var serviceNotificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_NOTIFICATIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_SERVICE_NOTIFICATIONS, value).apply()

    var parkingAlertsEnabled: Boolean
        get() = prefs.getBoolean(KEY_PARKING_ALERTS, true)
        set(value) = prefs.edit().putBoolean(KEY_PARKING_ALERTS, value).apply()

    val themeModeFlow: Flow<ThemeMode> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_THEME_MODE) {
                trySend(themeMode)
            }
        }

        trySend(themeMode)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.distinctUntilChanged()

    companion object {
        private const val PREF_NAME = "carpark"
        private const val KEY_PREFERRED_NAVIGATION = "preferredNav"
        private const val KEY_THEME_MODE = "themeMode"
        private const val KEY_SERVICE_NOTIFICATIONS = "serviceNotifications"
        private const val KEY_PARKING_ALERTS = "parkingAlerts"
    }
}
