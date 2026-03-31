package com.carpark.android.data.model

data class KakaoLoginRequest(
    val accessToken: String,
)

data class TokenReissueRequest(
    val refreshToken: String,
)

data class ApiError(
    val code: String? = null,
    val message: String? = null,
)

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String = "",
    val error: ApiError? = null,
)

fun ApiResponse<*>.errorMessageOrDefault(defaultMessage: String): String {
    return error?.message?.takeIf { it.isNotBlank() }
        ?: message?.takeIf { it.isNotBlank() }
        ?: defaultMessage
}

data class KakaoLoginResponse(
    val user: AuthUser,
    val token: AuthTokenPair,
)

data class AuthUser(
    val userId: Long,
    val nickname: String?,
    val profileImageUrl: String?,
    val loginType: String,
    val role: String,
    val naviType: String? = null,
    val isRegister: Boolean? = null,
)

data class AuthTokenPair(
    val accessToken: String,
    val refreshToken: String,
)
