package com.carpark.android.data.api

import android.content.Context
import com.carpark.android.BuildConfig
import com.carpark.android.data.local.AuthPreferences
import com.carpark.android.data.model.AuthTokenPair
import com.carpark.android.data.model.TokenReissueRequest
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val okHttpClient: OkHttpClient by lazy {
        val context = requireNotNull(appContext) { "RetrofitClient.init(context) must be called first" }
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .authenticator(AuthTokenAuthenticator(context))
            .addInterceptor { chain ->
                val token = appContext?.let { AuthPreferences(it).serverAccessToken }
                val request = chain.request().newBuilder().apply {
                    if (!token.isNullOrBlank()) {
                        addHeader("Authorization", "Bearer $token")
                    }
                }.build()
                chain.proceed(request)
            }
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        }
                    )
                }
            }
            .build()
    }

    private val refreshClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        }
                    )
                }
            }
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL.trimEnd('/') + "/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
            .build()
    }

    private val kakaoLocalClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder().apply {
                    if (BuildConfig.KAKAO_REST_API_KEY.isNotBlank()) {
                        addHeader("Authorization", "KakaoAK ${BuildConfig.KAKAO_REST_API_KEY}")
                    }
                }.build()
                chain.proceed(request)
            }
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                        }
                    )
                }
            }
            .build()
    }

    private val kakaoLocalRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://dapi.kakao.com/")
            .client(kakaoLocalClient)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
            .build()
    }

    private val refreshRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL.trimEnd('/') + "/")
            .client(refreshClient)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
            .build()
    }

    val api: ParkingApi by lazy {
        retrofit.create(ParkingApi::class.java)
    }

    val kakaoLocalApi: KakaoLocalApi by lazy {
        kakaoLocalRetrofit.create(KakaoLocalApi::class.java)
    }

    private val refreshApi: ParkingApi by lazy {
        refreshRetrofit.create(ParkingApi::class.java)
    }

    suspend fun refreshTokens(refreshToken: String): AuthTokenPair? {
        val response = runCatching {
            refreshApi.reissueTokens(TokenReissueRequest(refreshToken = refreshToken))
        }.getOrNull() ?: return null

        if (!response.success || response.data == null) {
            return null
        }

        return response.data
    }
}
