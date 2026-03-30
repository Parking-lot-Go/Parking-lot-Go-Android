package com.carpark.android.data.api

import com.carpark.android.data.model.KakaoKeywordSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface KakaoLocalApi {

    @GET("v2/local/search/keyword.json")
    suspend fun searchKeyword(
        @Query("query") query: String,
        @Query("x") longitude: Double? = null,
        @Query("y") latitude: Double? = null,
        @Query("radius") radius: Int? = null,
        @Query("sort") sort: String? = null,
        @Query("size") size: Int = 15,
    ): KakaoKeywordSearchResponse
}
