package com.carpark.android.data.model

data class KakaoKeywordSearchResponse(
    val meta: KakaoKeywordMeta,
    val documents: List<KakaoPlace>,
)

data class KakaoKeywordMeta(
    val total_count: Int,
    val pageable_count: Int,
    val is_end: Boolean,
)

data class KakaoPlace(
    val id: String,
    val place_name: String,
    val category_name: String,
    val category_group_code: String?,
    val category_group_name: String?,
    val phone: String?,
    val address_name: String,
    val road_address_name: String,
    val x: String,
    val y: String,
    val place_url: String,
    val distance: String?,
) {
    val latitude: Double
        get() = y.toDoubleOrNull() ?: 0.0

    val longitude: Double
        get() = x.toDoubleOrNull() ?: 0.0

    val displayAddress: String
        get() = road_address_name.ifBlank { address_name }
}
