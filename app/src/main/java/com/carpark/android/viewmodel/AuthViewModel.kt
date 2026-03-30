package com.carpark.android.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.carpark.android.data.api.RetrofitClient
import com.carpark.android.data.local.AuthPreferences
import com.carpark.android.data.local.SessionManager
import com.carpark.android.data.model.KakaoLoginRequest
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.HttpException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authPrefs = AuthPreferences(application)
    private val appSettings = com.carpark.android.data.local.AppSettingsPreferences(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    val isLoggedIn: Boolean
        get() = authPrefs.isLoggedIn && !authPrefs.serverAccessToken.isNullOrBlank()

    fun loginWithKakao(context: Context) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val kakaoToken = performKakaoLogin(context)
                loginToServerWithKakao(kakaoToken.accessToken)
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Kakao login failed")
            }
        }
    }

    fun handleGoogleLoginResult(idToken: String, displayName: String?, photoUrl: String?) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authPrefs.isLoggedIn = true
            authPrefs.userId = idToken
            authPrefs.userNickname = displayName
            authPrefs.userProfileImageUrl = photoUrl
            _authState.value = AuthState.Success
        }
    }

    fun setError(message: String) {
        _authState.value = AuthState.Error(message)
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun logout() {
        UserApiClient.instance.logout { _ ->
            SessionManager.clearSession()
        }
    }

    fun validateSession() {
        if (authPrefs.serverAccessToken.isNullOrBlank()) {
            SessionManager.clearSession()
            return
        }

        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.fetchMyProfile()
                if (!response.success || response.data == null) {
                    SessionManager.clearSession()
                } else {
                    SessionManager.refresh()
                }
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    SessionManager.clearSession()
                }
            } catch (_: Exception) {
                // Ignore transient errors; do not force logout on network instability.
            }
        }
    }

    private suspend fun loginToServerWithKakao(kakaoAccessToken: String) {
        val response = RetrofitClient.api.loginWithKakao(
            KakaoLoginRequest(accessToken = kakaoAccessToken)
        )

        if (!response.success || response.data == null) {
            throw IllegalStateException(response.message.ifBlank { "Server login failed" })
        }

        val user = response.data.user
        val token = response.data.token

        authPrefs.isLoggedIn = true
        authPrefs.userId = user.userId.toString()
        authPrefs.userNickname = user.nickname
        authPrefs.userProfileImageUrl = user.profileImageUrl
        authPrefs.serverAccessToken = token.accessToken
        authPrefs.serverRefreshToken = token.refreshToken
        authPrefs.loginType = user.loginType
        authPrefs.role = user.role

        // 서버 naviType을 로컬 설정에 동기화
        val naviType = user.naviType ?: "KAKAO"
        appSettings.preferredNavigation = com.carpark.android.data.model.NavigationProvider.entries
            .firstOrNull { it.name == naviType }
            ?: com.carpark.android.data.model.NavigationProvider.KAKAO

        SessionManager.refresh()
    }

    private suspend fun performKakaoLogin(context: Context): OAuthToken =
        suspendCancellableCoroutine { cont ->
            val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
                if (error != null) {
                    cont.resumeWithException(error)
                } else {
                    cont.resume(token!!)
                }
            }

            if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
                UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                    if (error != null) {
                        if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                            cont.resumeWithException(error)
                            return@loginWithKakaoTalk
                        }

                        UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
                    } else {
                        callback(token, null)
                    }
                }
            } else {
                UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
            }
        }
}
