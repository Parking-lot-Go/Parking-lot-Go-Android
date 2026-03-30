package com.carpark.android.data.model

import com.google.gson.annotations.SerializedName

enum class DataMode {
    REALTIME, NON_REALTIME, NOT_LINKED
}

enum class TabId {
    NEARBY, HOME, SAVED, MY
}

enum class MyPageRoute {
    ROOT, NOTICE, CONTACT, REQUEST, SETTINGS, TERMS
}

enum class NavigationProvider {
    NAVER, KAKAO
}

data class ParkingLotSummary(
    val id: Int,
    @SerializedName("parkingName") val parkingName: String,
    val lat: Double,
    val lng: Double,
    val totalCapacity: Int,
    val availableCount: Int,
    val feeType: String? = null,
)

data class ParkingLot(
    val id: Int,
    val parkingCode: String = "",
    val parkingName: String = "",
    val address: String = "",
    val district: String = "",
    val parkingTypeName: String = "",
    val operationType: String = "",
    val phone: String = "",
    val totalCapacity: Int = 0,
    val currentCount: Int = 0,
    val availableCount: Int = 0,
    val updatedAt: String = "",
    val feeType: String = "",
    val basicFee: Int = 0,
    val basicTime: Int = 0,
    val additionalFee: Int = 0,
    val additionalTime: Int = 0,
    val monthlyFee: Int = 0,
    val dayMaxFee: Int = 0,
    val lat: String = "0",
    val lng: String = "0",
    val weekdayStart: String = "",
    val weekdayEnd: String = "",
    val weekendStart: String = "",
    val weekendEnd: String = "",
    val holidayStart: String = "",
    val holidayEnd: String = "",
) {
    val latDouble: Double get() = lat.toDoubleOrNull() ?: 0.0
    val lngDouble: Double get() = lng.toDoubleOrNull() ?: 0.0
}

data class NearbyParkingLot(
    val lot: ParkingLot,
    val distance: Int,
)

data class ParkingResponse(
    val content: List<ParkingLot>,
    val nextCursor: Int?,
    val hasNext: Boolean,
)

data class MapBounds(
    val swLat: Double,
    val swLng: Double,
    val neLat: Double,
    val neLng: Double,
)

data class FavoritesPageResponse(
    val content: List<ParkingLot>,
    val nextCursor: Long?,
    val hasNext: Boolean,
)
