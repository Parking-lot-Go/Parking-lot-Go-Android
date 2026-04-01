package com.carpark.android.data.repository

import android.content.Context
import com.carpark.android.data.api.RetrofitClient
import com.carpark.android.data.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.*

class ParkingRepository {

    private val api = RetrofitClient.api
    private val kakaoLocalApi = RetrofitClient.kakaoLocalApi
    private val gson = Gson()

    private var staticLotsCache: List<ParkingLotSummary>? = null

    fun loadStaticLots(context: Context): List<ParkingLotSummary> {
        staticLotsCache?.let { return it }
        val json = context.assets.open("parkingLots_static.json").bufferedReader().use { it.readText() }
        val lots: List<ParkingLotSummary> = gson.fromJson(json, object : TypeToken<List<ParkingLotSummary>>() {}.type)
        staticLotsCache = lots
        return lots
    }

    fun getStaticLotsInBounds(context: Context, bounds: MapBounds): List<ParkingLot> {
        val summaries = loadStaticLots(context)
        return summaries
            .filter { it.lat != 0.0 && it.lng != 0.0 }
            .filter {
                it.lat in bounds.swLat..bounds.neLat && it.lng in bounds.swLng..bounds.neLng
            }
            .map { s ->
                ParkingLot(
                    id = s.id,
                    parkingName = s.parkingName ?: "",
                    address = s.address ?: "",
                    lat = s.lat,
                    lng = s.lng,
                    totalCapacity = s.totalCapacity,
                    availableCount = s.availableCount,
                    feeType = s.feeType ?: "",
                )
            }
    }

    suspend fun fetchParkingLots(
        mode: DataMode,
        bounds: MapBounds? = null,
        district: String? = null,
    ): List<ParkingLot> {
        val response = api.fetchParkingLots(
            type = mode.name,
            swLat = bounds?.swLat,
            swLng = bounds?.swLng,
            neLat = bounds?.neLat,
            neLng = bounds?.neLng,
            district = district,
        )
        return response.content
    }

    suspend fun fetchNearbyLots(lat: Double, lng: Double): List<NearbyParkingLot> {
        val json = api.fetchNearbyLots(lat = lat, lng = lng)

        val lots: List<ParkingLot> = if (json.isJsonArray) {
            gson.fromJson(json, object : TypeToken<List<ParkingLot>>() {}.type)
        } else {
            val obj = json.asJsonObject
            if (obj.has("content")) {
                gson.fromJson(obj.getAsJsonArray("content"), object : TypeToken<List<ParkingLot>>() {}.type)
            } else {
                emptyList()
            }
        }

        return lots.map { lot ->
            val straightDistance = haversine(lat, lng, lot.latDouble, lot.lngDouble)
            NearbyParkingLot(
                lot = lot,
                distance = adjustNearbyDistance(straightDistance),
            )
        }.sortedBy { it.distance }
    }

    suspend fun fetchParkingDetail(id: Int): ParkingLot {
        val response = api.fetchParkingDetail(id)
        if (!response.success || response.data == null) {
            throw IllegalStateException(response.errorMessageOrDefault("주차장 상세 정보를 불러오지 못했습니다"))
        }
        return response.data
    }

    suspend fun searchPlaces(
        query: String,
        bounds: MapBounds? = null,
    ): List<KakaoPlace> {
        return kakaoLocalApi.searchKeyword(
            query = query,
            size = 10,
        ).documents
    }

    // --- 즐겨찾기 ---

    suspend fun fetchFavorites(cursor: Long? = null, size: Int = 10): FavoritesPageResponse {
        val response = api.fetchFavorites(cursor = cursor, size = size)
        if (!response.success || response.data == null) {
            throw IllegalStateException(response.message.ifBlank { "즐겨찾기를 불러올 수 없습니다" })
        }
        return response.data
    }

    /** @return true = 등록됨, false = 해제됨 */
    suspend fun toggleFavorite(parkingId: Int): Boolean {
        val response = api.toggleFavorite(parkingId)
        if (!response.success) {
            throw IllegalStateException(response.message.ifBlank { "즐겨찾기 변경 실패" })
        }
        return response.data ?: false
    }

    suspend fun createSupportTicket(request: CreateSupportTicketRequest): SupportTicket {
        val response = api.createSupportTicket(request)
        if (!response.success || response.data == null) {
            throw IllegalStateException(response.errorMessageOrDefault("문의를 접수하지 못했습니다"))
        }
        return response.data
    }

    suspend fun fetchSupportTickets(
        type: SupportTicketType? = null,
        status: SupportTicketStatus? = null,
        mine: Boolean? = null,
    ): List<SupportTicket> {
        val response = api.fetchSupportTickets(
            type = type,
            status = status,
            mine = mine,
        )
        if (!response.success || response.data == null) {
            throw IllegalStateException(response.errorMessageOrDefault("문의 목록을 불러오지 못했습니다"))
        }
        return response.data
    }

    suspend fun updateSupportTicket(
        id: Long,
        request: UpdateSupportTicketRequest,
    ): SupportTicket {
        val response = api.updateSupportTicket(id = id, request = request)
        if (!response.success || response.data == null) {
            throw IllegalStateException(response.errorMessageOrDefault("문의 수정에 실패했습니다"))
        }
        return response.data
    }

    companion object {
        private fun adjustNearbyDistance(distanceMeters: Int): Int {
            return when {
                distanceMeters <= 300 -> distanceMeters + 80
                distanceMeters < 1000 -> (distanceMeters * 1.25).roundToInt()
                else -> (distanceMeters * 1.15).roundToInt()
            }
        }

        fun haversine(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Int {
            val r = 6371000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLng = Math.toRadians(lng2 - lng1)
            val a = sin(dLat / 2).pow(2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
            return (r * 2 * atan2(sqrt(a), sqrt(1 - a))).roundToInt()
        }

        private fun estimateRadiusMeters(bounds: MapBounds): Int {
            val centerLat = (bounds.swLat + bounds.neLat) / 2.0
            val centerLng = (bounds.swLng + bounds.neLng) / 2.0
            val cornerDistance = haversine(centerLat, centerLng, bounds.neLat, bounds.neLng)
            return cornerDistance.coerceIn(0, 20_000)
        }
    }
}
