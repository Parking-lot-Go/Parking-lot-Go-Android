package com.carpark.android

import android.app.Application
import com.carpark.android.data.api.RetrofitClient
import com.carpark.android.data.local.SessionManager
import com.kakao.sdk.common.KakaoSdk
import com.kakao.vectormap.KakaoMapSdk

class CarParkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KakaoMapSdk.init(this, BuildConfig.KAKAO_APP_KEY)
        KakaoSdk.init(this, BuildConfig.KAKAO_APP_KEY)
        RetrofitClient.init(this)
        SessionManager.init(this)
    }
}
