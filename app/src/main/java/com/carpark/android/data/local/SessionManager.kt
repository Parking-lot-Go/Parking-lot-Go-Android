package com.carpark.android.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SessionManager {
    @Volatile
    private var appContext: Context? = null

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        refresh()
    }

    fun refresh() {
        val context = appContext ?: return
        val prefs = AuthPreferences(context)
        _isLoggedIn.value = prefs.isLoggedIn && !prefs.serverAccessToken.isNullOrBlank()
    }

    fun clearSession() {
        val context = appContext ?: return
        AuthPreferences(context).clear()
        _isLoggedIn.value = false
    }
}
