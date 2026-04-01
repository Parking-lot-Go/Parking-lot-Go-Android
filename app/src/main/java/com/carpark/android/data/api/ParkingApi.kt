package com.carpark.android.data.api

import com.carpark.android.data.model.ApiResponse
import com.carpark.android.data.model.AuthUser
import com.carpark.android.data.model.CreateSupportTicketRequest
import com.carpark.android.data.model.FavoritesPageResponse
import com.carpark.android.data.model.KakaoLoginRequest
import com.carpark.android.data.model.KakaoLoginResponse
import com.carpark.android.data.model.ParkingLot
import com.carpark.android.data.model.ParkingResponse
import com.carpark.android.data.model.SupportTicket
import com.carpark.android.data.model.SupportTicketStatus
import com.carpark.android.data.model.SupportTicketType
import com.carpark.android.data.model.TokenReissueRequest
import com.carpark.android.data.model.UpdateSupportTicketRequest
import com.google.gson.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ParkingApi {
    @POST("auth/login/kakao")
    suspend fun loginWithKakao(
        @Body request: KakaoLoginRequest,
    ): ApiResponse<KakaoLoginResponse>

    @POST("auth/reissue")
    suspend fun reissueTokens(
        @Body request: TokenReissueRequest,
    ): ApiResponse<com.carpark.android.data.model.AuthTokenPair>

    @GET("users/me")
    suspend fun fetchMyProfile(): ApiResponse<AuthUser>

    @PATCH("users/navi")
    suspend fun updateNaviType(
        @Query("naviType") naviType: String,
    ): ApiResponse<AuthUser>

    @GET("parking")
    suspend fun fetchParkingLots(
        @Query("type") type: String,
        @Query("swLat") swLat: Double? = null,
        @Query("swLng") swLng: Double? = null,
        @Query("neLat") neLat: Double? = null,
        @Query("neLng") neLng: Double? = null,
        @Query("district") district: String? = null,
    ): ParkingResponse

    @GET("search")
    suspend fun fetchNearbyLots(
        @Query("type") type: String = "near",
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
    ): JsonElement

    @GET("parking/{id}")
    suspend fun fetchParkingDetail(
        @Path("id") id: Int,
    ): ApiResponse<ParkingLot>

    // --- 즐겨찾기 ---

    @GET("favorites")
    suspend fun fetchFavorites(
        @Query("cursor") cursor: Long? = null,
        @Query("size") size: Int? = null,
    ): ApiResponse<FavoritesPageResponse>

    @POST("favorites/{parkingId}")
    suspend fun toggleFavorite(
        @Path("parkingId") parkingId: Int,
    ): ApiResponse<Boolean>

    @POST("support-tickets")
    suspend fun createSupportTicket(
        @Body request: CreateSupportTicketRequest,
    ): ApiResponse<SupportTicket>

    @GET("support-tickets")
    suspend fun fetchSupportTickets(
        @Query("type") type: SupportTicketType? = null,
        @Query("status") status: SupportTicketStatus? = null,
        @Query("mine") mine: Boolean? = null,
    ): ApiResponse<List<SupportTicket>>

    @PATCH("support-tickets/{id}")
    suspend fun updateSupportTicket(
        @Path("id") id: Long,
        @Body request: UpdateSupportTicketRequest,
    ): ApiResponse<SupportTicket>
}
