package com.carpark.android.data.local

import android.content.Context

class AuthPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var userNickname: String?
        get() = prefs.getString(KEY_USER_NICKNAME, null)
        set(value) = prefs.edit().putString(KEY_USER_NICKNAME, value).apply()

    var userProfileImageUrl: String?
        get() = prefs.getString(KEY_PROFILE_IMAGE, null)
        set(value) = prefs.edit().putString(KEY_PROFILE_IMAGE, value).apply()

    var serverAccessToken: String?
        get() = prefs.getString(KEY_SERVER_ACCESS_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_SERVER_ACCESS_TOKEN, value).apply()

    var serverRefreshToken: String?
        get() = prefs.getString(KEY_SERVER_REFRESH_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_SERVER_REFRESH_TOKEN, value).apply()

    var loginType: String?
        get() = prefs.getString(KEY_LOGIN_TYPE, null)
        set(value) = prefs.edit().putString(KEY_LOGIN_TYPE, value).apply()

    var role: String?
        get() = prefs.getString(KEY_ROLE, null)
        set(value) = prefs.edit().putString(KEY_ROLE, value).apply()

    fun clear() = prefs.edit().clear().apply()

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NICKNAME = "user_nickname"
        private const val KEY_PROFILE_IMAGE = "profile_image"
        private const val KEY_SERVER_ACCESS_TOKEN = "server_access_token"
        private const val KEY_SERVER_REFRESH_TOKEN = "server_refresh_token"
        private const val KEY_LOGIN_TYPE = "login_type"
        private const val KEY_ROLE = "role"
    }
}
