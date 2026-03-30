package com.carpark.android.data.local

import android.content.Context
import com.carpark.android.data.model.NavigationProvider

class AppSettingsPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var preferredNavigation: NavigationProvider
        get() = NavigationProvider.entries.firstOrNull {
            it.name == prefs.getString(KEY_PREFERRED_NAVIGATION, NavigationProvider.NAVER.name)
        } ?: NavigationProvider.NAVER
        set(value) = prefs.edit().putString(KEY_PREFERRED_NAVIGATION, value.name).apply()

    companion object {
        private const val PREF_NAME = "carpark"
        private const val KEY_PREFERRED_NAVIGATION = "preferredNav"
    }
}
