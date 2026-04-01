package com.carpark.android.data.model

import com.google.gson.annotations.SerializedName

enum class DataMode {
    REALTIME, NON_REALTIME, NOT_LINKED
}

enum class TabId {
    NEARBY, HOME, SAVED, MY
}

enum class MyPageRoute {
    ROOT, NOTICE, TICKETS, CONTACT, REQUEST, SETTINGS, NOTIFICATIONS, TERMS
}

enum class NavigationProvider {
    NAVER, KAKAO
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    val serverValue: String
        get() = name
}

data class ParkingLotSummary(
    val id: Int = 0,
    @SerializedName("parkingName") val parkingName: String? = null,
    val address: String? = null,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val totalCapacity: Int = 0,
    val availableCount: Int = 0,
    val feeType: String? = null,
)

data class ParkingLot(
    val id: Int,
    val parkingCode: String = "",
    val parkingName: String = "",
    val address: String = "",
    val district: String = "",
    val parkingTypeName: String = "",
    val operationType: String? = null,
    val phone: String? = null,
    val totalCapacity: Int = 0,
    val currentCount: Int = 0,
    val availableCount: Int = 0,
    val updatedAt: String? = null,
    val feeType: String = "",
    val basicFee: Int = 0,
    val basicTime: Int = 0,
    val additionalFee: Int = 0,
    val additionalTime: Int = 0,
    val monthlyFee: Int = 0,
    val dayMaxFee: Int = 0,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val weekdayStart: String? = null,
    val weekdayEnd: String? = null,
    val weekendStart: String? = null,
    val weekendEnd: String? = null,
    val holidayStart: String? = null,
    val holidayEnd: String? = null,
) {
    val latDouble: Double get() = lat
    val lngDouble: Double get() = lng
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
