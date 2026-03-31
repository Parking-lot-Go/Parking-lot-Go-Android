package com.carpark.android.data.api

import android.content.Context
import com.carpark.android.data.local.AuthPreferences
import com.carpark.android.data.local.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class AuthTokenAuthenticator(
    context: Context,
) : Authenticator {
    private val appContext = context.applicationContext
    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val path = response.request.url.encodedPath
        if (path.endsWith("/auth/login/kakao") || path.endsWith("/auth/reissue")) {
            return null
        }
        if (responseCount(response) >= 2) {
            return null
        }

        val prefs = AuthPreferences(appContext)
        val refreshToken = prefs.serverRefreshToken?.takeIf { it.isNotBlank() } ?: run {
            SessionManager.clearSession()
            return null
        }

        synchronized(refreshLock) {
            val requestAccessToken = response.request.header("Authorization")
                ?.removePrefix("Bearer ")
                ?.trim()
            val latestAccessToken = prefs.serverAccessToken?.takeIf { it.isNotBlank() }

            if (!latestAccessToken.isNullOrBlank() && latestAccessToken != requestAccessToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $latestAccessToken")
                    .build()
            }

            val newTokens = runBlocking {
                RetrofitClient.refreshTokens(refreshToken)
            } ?: run {
                SessionManager.clearSession()
                return null
            }

            prefs.serverAccessToken = newTokens.accessToken
            prefs.serverRefreshToken = newTokens.refreshToken
            SessionManager.refresh()

            return response.request.newBuilder()
                .header("Authorization", "Bearer ${newTokens.accessToken}")
                .build()
        }
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var priorResponse = response.priorResponse
        while (priorResponse != null) {
            result++
            priorResponse = priorResponse.priorResponse
        }
        return result
    }
}
