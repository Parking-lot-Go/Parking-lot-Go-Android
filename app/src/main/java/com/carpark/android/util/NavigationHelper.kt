package com.carpark.android.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object NavigationHelper {

    fun openNavigation(context: Context, lat: Double, lng: Double, name: String) {
        val provider = context.getSharedPreferences("carpark", Context.MODE_PRIVATE)
            .getString("preferredNav", "NAVER")
            ?.lowercase() ?: "naver"
        val encoded = Uri.encode(name)

        val uri = if (provider == "naver") {
            "nmap://route/car?dlat=$lat&dlng=$lng&dname=$encoded&appname=com.carpark.android"
        } else {
            "kakaonavi://route?ep=$lat,$lng&by=CAR&destinationName=$encoded"
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Fallback to web
            val webUri = if (provider == "naver") {
                "https://map.naver.com/index.nhn?menu=route&pathType=0&elng=$lng&elat=$lat&etext=$encoded"
            } else {
                "https://map.kakao.com/link/to/$encoded,$lat,$lng"
            }
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUri)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
}
