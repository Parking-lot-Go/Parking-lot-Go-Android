package com.carpark.android.ui.components

import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.location.Geocoder
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.carpark.android.R
import com.carpark.android.data.model.DataMode
import com.carpark.android.data.model.KakaoPlace
import com.carpark.android.data.model.MapBounds
import com.carpark.android.data.model.ParkingLot
import com.carpark.android.ui.theme.isAppInDarkTheme
import com.carpark.android.viewmodel.LatLngPoint
import com.kakao.vectormap.*
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.max

private const val TAG = "ParkingMapView"
private const val DONG_CLUSTER_MAX_ZOOM = 15
private const val SIGUNGU_CLUSTER_MAX_ZOOM = 14
private const val MAX_INDIVIDUAL_MARKERS = 200

private data class ClusterMarker(
    val labelId: String,
    val centerLat: Double,
    val centerLng: Double,
    val count: Int,
    val regionLabel: String,
)

private fun buildMarkerRenderKey(
    parkingLots: List<ParkingLot>,
    clusteredEntries: List<Pair<ClusterMarker?, List<ParkingLot>>>,
    selectedLotId: Int?,
    dataMode: DataMode,
    isDarkMode: Boolean,
    isNearbyMode: Boolean,
): Int {
    var h = dataMode.ordinal
    h = h * 31 + (if (isDarkMode) 1 else 0)
    h = h * 31 + (if (isNearbyMode) 1 else 0)
    h = h * 31 + (selectedLotId ?: -1)

    if (clusteredEntries.isNotEmpty()) {
        h = h * 31 + clusteredEntries.size
        for ((cluster, lots) in clusteredEntries) {
            if (cluster != null) {
                h = h * 31 + cluster.labelId.hashCode()
                h = h * 31 + cluster.count
            } else {
                val lot = lots.firstOrNull() ?: continue
                h = h * 31 + lot.id
                h = h * 31 + lot.availableCount
            }
        }
    } else {
        h = h * 31 + parkingLots.size
        for (lot in parkingLots) {
            h = h * 31 + lot.id
            h = h * 31 + lot.availableCount
        }
    }
    return h
}

private suspend fun resolveCenterRegion(
    context: Context,
    lat: Double,
    lng: Double,
): String? = withContext(Dispatchers.IO) {
    runCatching {
        if (!Geocoder.isPresent()) return@runCatching null
        val results = Geocoder(context, Locale.KOREA).getFromLocation(lat, lng, 1)
        val address = results?.firstOrNull() ?: return@runCatching null

        listOfNotNull(
            address.adminArea?.takeIf { it.isNotBlank() },
            address.locality?.takeIf { it.isNotBlank() },
            address.subAdminArea?.takeIf { it.isNotBlank() },
            address.subLocality?.takeIf { it.isNotBlank() },
        ).distinct().joinToString(" ").ifBlank { null }
    }.getOrNull()
}

private fun createUserLocationBitmap(context: Context, bearing: Float?): Bitmap {
    val dp = context.resources.displayMetrics.density
    val outerR = 14f * dp
    val ringR = 9.5f * dp
    val ringStroke = 2f * dp
    val whiteR = 7f * dp
    val blueR = 4.5f * dp
    val padding = 4f * dp
    val halfSize = outerR + padding
    val size = (halfSize * 2).toInt()

    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = halfSize
    val cy = halfSize
    val blueColor = android.graphics.Color.parseColor("#2D6FE8")

    // 방향 빔 (부채꼴, pgs_my_location_icon.svg 스타일)
    if (bearing != null) {
        canvas.save()
        canvas.rotate(bearing, cx, cy)
        val beamPath = Path().apply {
            moveTo(cx, cy - 2f * dp)
            lineTo(cx - 8f * dp, cy - outerR + 1f * dp)
            lineTo(cx + 8f * dp, cy - outerR + 1f * dp)
            close()
        }
        canvas.drawPath(beamPath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = blueColor
            alpha = 65
        })
        canvas.restore()
    }

    // 외부 글로우
    canvas.drawCircle(cx, cy, outerR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = blueColor
        alpha = 30
    })

    // 중간 글로우 링
    canvas.drawCircle(cx, cy, ringR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = ringStroke
        color = blueColor
        alpha = 46
    })

    // 흰 링
    canvas.drawCircle(cx, cy, whiteR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
    })

    // 파란 GPS 점
    canvas.drawCircle(cx, cy, blueR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = blueColor
    })

    return bitmap
}

private fun createParkingSignBitmap(
    context: Context,
    fillColor: Int,
    isSelected: Boolean = false,
    scale: Float = 1f,
): Bitmap {
    val density = context.resources.displayMetrics.density
    val sd = density * scale

    val inner = 11f * sd          // 내부 사각형 한 변
    val border = 1.8f * sd        // 흰 테두리
    val cr = 3.5f * sd            // 라운드
    val glow = if (isSelected) 3f * sd else 0f
    val pad = 3f * sd

    val total = inner + border * 2 + glow * 2 + pad * 2
    val size = total.toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = size / 2f
    val cy = size / 2f
    val half = inner / 2 + border

    // 그림자
    canvas.drawRoundRect(
        RectF(cx - half, cy - half + sd, cx + half, cy + half + sd),
        cr + sd, cr + sd,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(25, 0, 0, 0)
            maskFilter = BlurMaskFilter(2.5f * sd, BlurMaskFilter.Blur.NORMAL)
        },
    )

    // 선택 글로우
    if (isSelected) {
        canvas.drawRoundRect(
            RectF(cx - half - glow, cy - half - glow, cx + half + glow, cy + half + glow),
            cr + 2f * sd, cr + 2f * sd,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fillColor; alpha = 50 },
        )
    }

    // 흰 테두리 배경
    canvas.drawRoundRect(
        RectF(cx - half, cy - half, cx + half, cy + half),
        cr + sd, cr + sd,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE },
    )

    // 컬러 채움
    val ih = inner / 2
    canvas.drawRoundRect(
        RectF(cx - ih, cy - ih, cx + ih, cy + ih), cr, cr,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fillColor },
    )

    // P 텍스트
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 9f * sd; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
    }
    canvas.drawText("P", cx, cy - (p.descent() + p.ascent()) / 2, p)

    return bitmap
}

private fun createStaticMarkerBitmap(
    context: Context,
    isDarkMode: Boolean,
    isSelected: Boolean = false,
    scale: Float = 1f,
): Bitmap {
    val color = if (isDarkMode) Color.parseColor("#60A5FA") else Color.parseColor("#3B82F6")
    return createParkingSignBitmap(context, color, isSelected, scale)
}

private fun getMarkerColor(lot: ParkingLot, isRealtime: Boolean): Int {
    if (!isRealtime) return android.graphics.Color.parseColor("#9ca3af")
    if (lot.totalCapacity == 0) return android.graphics.Color.parseColor("#9ca3af")
    val ratio = lot.availableCount.toFloat() / lot.totalCapacity
    return when {
        ratio > 0.3f -> android.graphics.Color.parseColor("#22c55e")
        ratio > 0.1f -> android.graphics.Color.parseColor("#f59e0b")
        else -> android.graphics.Color.parseColor("#ef4444")
    }
}

private fun createMarkerBitmap(
    context: Context,
    color: Int,
    text: String?,
    isSelected: Boolean = false,
    scale: Float = 1f,
): Bitmap = createParkingSignBitmap(context, color, isSelected, scale)

private fun createLotLabelStyles(
    context: Context,
    lot: ParkingLot,
    dataMode: DataMode,
    isDarkMode: Boolean,
    isSelected: Boolean,
    scale: Float = 1f,
): LabelStyles {
    val isRealtime = dataMode == DataMode.REALTIME
    val isNotLinked = dataMode == DataMode.NOT_LINKED
    val bitmap = if (isNotLinked) {
        createStaticMarkerBitmap(
            context = context,
            isDarkMode = isDarkMode,
            isSelected = isSelected,
            scale = scale,
        )
    } else {
        createMarkerBitmap(
            context = context,
            color = getMarkerColor(lot, isRealtime),
            text = if (isRealtime) "${lot.availableCount}" else null,
            isSelected = isSelected,
            scale = scale,
        )
    }
    return LabelStyles.from(
        LabelStyle.from(bitmap).setAnchorPoint(0.5f, 0.5f)
    )
}

private enum class ClusterLevel { SIGUNGU, DONG }

private data class ParsedAddress(
    val sido: String?,
    val si: String?,
    val gun: String?,
    val gu: String?,
    val eupMyeonDong: String?,
) {
    val sidoNormalized: String?
        get() = sido?.let(::normalizeSido)

    val isMajorMetro: Boolean
        get() = sido != null && MAJOR_METRO_PREFIXES.any { sido.startsWith(it) }
}

private val MAJOR_METRO_PREFIXES = setOf(
    "서울", "부산", "대구", "인천", "광주", "대전", "울산",
)

private val SHORT_SIDO_NAMES = setOf(
    "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종",
    "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주",
)

private val addressParseCache = HashMap<Int, ParsedAddress>(512)

private fun normalizeSido(sido: String): String {
    return when {
        sido.endsWith("특별자치시") -> sido.removeSuffix("특별자치시") + "시"
        sido.endsWith("특별시") -> sido.removeSuffix("특별시") + "시"
        sido.endsWith("광역시") -> sido.removeSuffix("광역시") + "시"
        sido.endsWith("특별자치도") -> sido.removeSuffix("특별자치도") + "도"
        sido in setOf("서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종") -> "${sido}시"
        sido in setOf("경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남") -> "${sido}도"
        sido == "제주" -> "제주도"
        else -> sido
    }
}

private fun parseAddressString(source: String): ParsedAddress {
    val tokens = source
        .replace("(", " ").replace(")", " ")
        .replace(",", " ").replace("/", " ")
        .split(Regex("\\s+"))
        .filter { it.length > 1 }

    var sido: String? = null
    var si: String? = null
    var gun: String? = null
    var gu: String? = null
    var eupMyeonDong: String? = null

    for (token in tokens) {
        when {
            // 시도 (full form)
            sido == null && (
                token.endsWith("특별시") || token.endsWith("광역시") ||
                    token.endsWith("특별자치시") || token.endsWith("특별자치도") ||
                    (token.endsWith("도") && token.length >= 2 && !token.endsWith("동"))
                ) -> sido = token

            // 시도 (short form)
            sido == null && token in SHORT_SIDO_NAMES -> sido = token

            // 시 (under 도, not 시도-level)
            si == null && token.endsWith("시") &&
                !token.endsWith("특별시") && !token.endsWith("광역시") &&
                !token.endsWith("특별자치시") -> si = token

            // 군
            gun == null && token.endsWith("군") -> gun = token

            // 구
            gu == null && token.endsWith("구") -> gu = token

            // 동/읍/면/리
            eupMyeonDong == null && (
                token.endsWith("동") || token.endsWith("읍") ||
                    token.endsWith("면") || token.endsWith("리")
                ) -> eupMyeonDong = token
        }
    }

    return ParsedAddress(sido, si, gun, gu, eupMyeonDong)
}

private fun parseAddress(lot: ParkingLot): ParsedAddress {
    addressParseCache[lot.id]?.let { return it }
    val source = lot.address.ifBlank { "${lot.district} ${lot.parkingName}" }
    return parseAddressString(source).also { addressParseCache[lot.id] = it }
}


private fun getClusterKey(parsed: ParsedAddress, level: ClusterLevel): String? {
    return when (level) {
        ClusterLevel.SIGUNGU -> {
            val parts = listOfNotNull(parsed.sidoNormalized, parsed.si, parsed.gu, parsed.gun)
            parts.joinToString(" ").ifBlank { null }
        }
        ClusterLevel.DONG -> {
            val parent = listOfNotNull(parsed.sidoNormalized, parsed.si, parsed.gu, parsed.gun)
                .joinToString(" ")
            val dong = parsed.eupMyeonDong
            if (dong != null) {
                "$parent $dong".trim()
            } else {
                // 도로명주소 등 동 정보가 없으면 구/시/군으로 폴백
                parent.ifBlank { null }
            }
        }
    }
}

private fun getClusterLabel(parsed: ParsedAddress, level: ClusterLevel): String? {
    return when (level) {
        ClusterLevel.SIGUNGU -> parsed.gu ?: parsed.si ?: parsed.gun ?: parsed.sidoNormalized
        ClusterLevel.DONG -> parsed.eupMyeonDong ?: parsed.gu ?: parsed.si ?: parsed.gun
    }
}

private fun resolveClusterLevel(zoomLevel: Int, lotCount: Int): ClusterLevel? {
    // Force clustering when too many markers even at high zoom
    if (lotCount > MAX_INDIVIDUAL_MARKERS && zoomLevel > DONG_CLUSTER_MAX_ZOOM) {
        return ClusterLevel.DONG
    }
    return when {
        zoomLevel > DONG_CLUSTER_MAX_ZOOM -> null
        zoomLevel > SIGUNGU_CLUSTER_MAX_ZOOM -> ClusterLevel.DONG
        else -> ClusterLevel.SIGUNGU
    }
}

private fun createClusterMarkerBitmap(
    context: Context,
    regionLabel: String,
    count: Int,
    isDarkMode: Boolean,
): Bitmap {
    val density = context.resources.displayMetrics.density
    val label = "${regionLabel.take(5)} ${count}개"

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isDarkMode) android.graphics.Color.parseColor("#E5E7EB")
        else android.graphics.Color.parseColor("#1F2937")
        textSize = 10f * density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    val hPad = 10f * density
    val pillW = textPaint.measureText(label) + hPad * 2
    val pillH = 24f * density
    val borderW = 1.2f * density
    val cornerR = pillH / 2

    val width = pillW.toInt().coerceAtLeast(1)
    val height = pillH.toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())

    // 반투명 배경
    canvas.drawRoundRect(rect, cornerR, cornerR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isDarkMode) android.graphics.Color.parseColor("#1F2937")
        else android.graphics.Color.WHITE
        alpha = 230
    })

    // 테두리
    val inset = borderW / 2
    canvas.drawRoundRect(
        RectF(inset, inset, width - inset, height - inset),
        cornerR, cornerR,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = borderW
            color = if (isDarkMode) android.graphics.Color.parseColor("#60A5FA")
            else android.graphics.Color.parseColor("#93C5FD")
        },
    )

    // 텍스트
    val textY = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2
    canvas.drawText(label, width / 2f, textY, textPaint)

    return bitmap
}

private fun createClusterLabelStyles(
    context: Context,
    cluster: ClusterMarker,
    isDarkMode: Boolean,
): LabelStyles {
    val bitmap = createClusterMarkerBitmap(
        context = context,
        regionLabel = cluster.regionLabel,
        count = cluster.count,
        isDarkMode = isDarkMode,
    )
    return LabelStyles.from(
        LabelStyle.from(bitmap).setAnchorPoint(0.5f, 1f)
    )
}

private fun buildClusterMarkers(
    lots: List<ParkingLot>,
    zoomLevel: Int,
): List<Pair<ClusterMarker?, List<ParkingLot>>> {
    if (lots.isEmpty()) return emptyList()

    val clusterLevel = resolveClusterLevel(zoomLevel, lots.size) ?: return emptyList()

    val cellLat: Double
    val cellLng: Double
    when (clusterLevel) {
        ClusterLevel.DONG -> { cellLat = 0.015; cellLng = 0.02 }
        ClusterLevel.SIGUNGU -> { cellLat = 0.05; cellLng = 0.065 }
    }

    return lots
        .filter { it.latDouble != 0.0 && it.lngDouble != 0.0 }
        .groupBy { lot ->
            val parsed = parseAddress(lot)
            val key = getClusterKey(parsed, clusterLevel)
            if (key != null) {
                "admin:$key"
            } else {
                val row = kotlin.math.floor(lot.latDouble / cellLat).toInt()
                val col = kotlin.math.floor(lot.lngDouble / cellLng).toInt()
                "grid:$row:$col"
            }
        }
        .map { (key, groupedLots) ->
            if (groupedLots.size <= 1) {
                null to groupedLots
            } else {
                val centerLat = groupedLots.map { it.latDouble }.average()
                val centerLng = groupedLots.map { it.lngDouble }.average()

                val regionLabel = if (key.startsWith("admin:")) {
                    val parsed = parseAddress(groupedLots.first())
                    getClusterLabel(parsed, clusterLevel) ?: "이 지역"
                } else {
                    // grid 폴백: 라벨 투표 → 없으면 아무 주소 정보라도 추출
                    groupedLots
                        .mapNotNull { lot ->
                            val p = parseAddress(lot)
                            getClusterLabel(p, clusterLevel)
                                ?: p.gu ?: p.si ?: p.gun ?: p.sidoNormalized
                        }
                        .groupingBy { it }
                        .eachCount()
                        .maxByOrNull { it.value }
                        ?.key ?: "이 지역"
                }

                ClusterMarker(
                    labelId = "cluster_${key.hashCode()}",
                    centerLat = centerLat,
                    centerLng = centerLng,
                    count = groupedLots.size,
                    regionLabel = regionLabel,
                ) to groupedLots
            }
        }
}

private fun createSearchMarkerBitmap(context: Context): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (18 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = size / 2f
    val cy = size / 2f
    val radius = size / 2f - 2 * density

    // 흰색 테두리
    canvas.drawCircle(cx, cy, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
    })
    // 빨간 점
    canvas.drawCircle(cx, cy, radius - 1.5f * density, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#EF4444")
    })

    return bitmap
}

@Composable
fun ParkingMapView(
    parkingLots: List<ParkingLot>,
    selectedLot: ParkingLot?,
    dataMode: DataMode,
    isNearbyMode: Boolean,
    panTo: LatLngPoint?,
    userLocation: LatLngPoint?,
    userBearing: Float?,
    searchPlaces: List<KakaoPlace> = emptyList(),
    onPanToConsumed: () -> Unit,
    onBoundsChange: (MapBounds, String?, Int) -> Unit,
    onSelectLot: (ParkingLot) -> Unit,
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDarkMode = isAppInDarkTheme()
    val scope = rememberCoroutineScope()
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    val context = remember { mutableStateOf<Context?>(null) }
    val lotLabelsRef = remember { mutableStateOf<Map<Int, Label>>(emptyMap()) }
    val clusterLabelsRef = remember { mutableStateOf<Map<String, ClusterMarker>>(emptyMap()) }
    var currentZoomLevel by remember { mutableStateOf(13) }
    var lastMarkerRenderKey by remember { mutableStateOf<Int?>(null) }
    val latestParkingLots by rememberUpdatedState(parkingLots)
    val latestOnBoundsChange by rememberUpdatedState(onBoundsChange)
    val latestOnSelectLot by rememberUpdatedState(onSelectLot)
    val latestOnMapClick by rememberUpdatedState(onMapClick)

    // Update user location marker
    LaunchedEffect(kakaoMap, userLocation, userBearing) {
        val map = kakaoMap ?: return@LaunchedEffect
        val ctx = context.value ?: return@LaunchedEffect
        if (userLocation == null) return@LaunchedEffect
        val labelManager = map.labelManager ?: return@LaunchedEffect
        try {
            val layer = labelManager.getLayer("user_location_layer")
                ?: labelManager.addLayer(
                    LabelLayerOptions.from("user_location_layer").setZOrder(10)
                ) ?: return@LaunchedEffect

            val bitmap = createUserLocationBitmap(ctx, userBearing)
            val style = LabelStyle.from(bitmap).setAnchorPoint(0.5f, 0.5f)
            val styles = LabelStyles.from(style)

            val existing = layer.getLabel("user_location")
            if (existing != null) {
                existing.changeStyles(styles)
                existing.moveTo(LatLng.from(userLocation.lat, userLocation.lng))
            } else {
                val options = LabelOptions.from("user_location", LatLng.from(userLocation.lat, userLocation.lng))
                    .setStyles(styles)
                layer.addLabel(options)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user location marker", e)
        }
    }

    // Update markers when lots, zoom, or mode change
    LaunchedEffect(
        kakaoMap,
        parkingLots,
        dataMode,
        selectedLot?.id,
        isDarkMode,
        currentZoomLevel,
        isNearbyMode,
    ) {
        val map = kakaoMap ?: return@LaunchedEffect
        val ctx = context.value ?: return@LaunchedEffect
        val labelManager = map.labelManager ?: return@LaunchedEffect

        try {
            val layer = labelManager.layer ?: labelManager.addLayer(
                LabelLayerOptions.from("parking_markers").setZOrder(1)
            ) ?: return@LaunchedEffect

            val clusteredEntries = if (!isNearbyMode) {
                withContext(Dispatchers.Default) {
                    buildClusterMarkers(parkingLots, currentZoomLevel)
                }
            } else {
                emptyList()
            }

            val renderKey = buildMarkerRenderKey(
                parkingLots = parkingLots,
                clusteredEntries = clusteredEntries,
                selectedLotId = selectedLot?.id,
                dataMode = dataMode,
                isDarkMode = isDarkMode,
                isNearbyMode = isNearbyMode,
            )
            if (renderKey == lastMarkerRenderKey) return@LaunchedEffect

            layer.removeAll()

            val nextLotLabels = mutableMapOf<Int, Label>()
            val nextClusterLabels = mutableMapOf<String, ClusterMarker>()

            if (clusteredEntries.isNotEmpty()) {
                for ((cluster, groupedLots) in clusteredEntries) {
                    if (cluster == null) {
                        val lot = groupedLots.firstOrNull() ?: continue
                        val styles = createLotLabelStyles(
                            context = ctx,
                            lot = lot,
                            dataMode = dataMode,
                            isDarkMode = isDarkMode,
                            isSelected = selectedLot?.id == lot.id,
                        )
                        val label = layer.addLabel(
                            LabelOptions.from("lot_${lot.id}", LatLng.from(lot.latDouble, lot.lngDouble))
                                .setStyles(styles)
                                .setClickable(true)
                        )
                        if (label != null) {
                            nextLotLabels[lot.id] = label
                        }
                    } else {
                        val label = layer.addLabel(
                            LabelOptions.from(
                                cluster.labelId,
                                LatLng.from(cluster.centerLat, cluster.centerLng),
                            ).setStyles(createClusterLabelStyles(ctx, cluster, isDarkMode))
                                .setClickable(true)
                        )
                        if (label != null) {
                            nextClusterLabels[cluster.labelId] = cluster
                        }
                    }
                }
            } else {
                for (lot in parkingLots) {
                    val lat = lot.latDouble
                    val lng = lot.lngDouble
                    if (lat == 0.0 || lng == 0.0) continue

                    val label = layer.addLabel(
                        LabelOptions.from("lot_${lot.id}", LatLng.from(lat, lng))
                            .setStyles(
                                createLotLabelStyles(
                                    context = ctx,
                                    lot = lot,
                                    dataMode = dataMode,
                                    isDarkMode = isDarkMode,
                                    isSelected = selectedLot?.id == lot.id,
                                )
                            )
                            .setClickable(true)
                    )
                    if (label != null) {
                        nextLotLabels[lot.id] = label
                    }
                }
            }

            lotLabelsRef.value = nextLotLabels
            clusterLabelsRef.value = nextClusterLabels
            lastMarkerRenderKey = renderKey
        } catch (e: Exception) {
            Log.e(TAG, "Error updating markers", e)
        }
    }

    LaunchedEffect(kakaoMap, selectedLot?.id, dataMode, isDarkMode) {
        val map = kakaoMap ?: return@LaunchedEffect
        val ctx = context.value ?: return@LaunchedEffect
        val lot = selectedLot ?: return@LaunchedEffect
        val label = lotLabelsRef.value[lot.id] ?: return@LaunchedEffect
        if (parkingLots.none { it.id == lot.id }) return@LaunchedEffect

        val bounceSequence = listOf(1f, 1.2f, 0.9f, 1.1f, 0.95f, 1f)
        val frameDelayMillis = 65L

        try {
            for (scale in bounceSequence) {
                label.changeStyles(
                    createLotLabelStyles(
                        context = ctx,
                        lot = lot,
                        dataMode = dataMode,
                        isDarkMode = isDarkMode,
                        isSelected = true,
                        scale = scale,
                    )
                )
                kotlinx.coroutines.delay(frameDelayMillis)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error animating selected marker bounce", e)
        }
    }

    // 검색 결과 빨간 점 마커
    LaunchedEffect(kakaoMap, searchPlaces) {
        val map = kakaoMap ?: return@LaunchedEffect
        val ctx = context.value ?: return@LaunchedEffect
        val labelManager = map.labelManager ?: return@LaunchedEffect

        try {
            val layer = labelManager.getLayer("search_markers_layer")
                ?: labelManager.addLayer(
                    LabelLayerOptions.from("search_markers_layer").setZOrder(5)
                ) ?: return@LaunchedEffect

            layer.removeAll()

            if (searchPlaces.isEmpty()) return@LaunchedEffect

            val bitmap = createSearchMarkerBitmap(ctx)
            val style = LabelStyle.from(bitmap).setAnchorPoint(0.5f, 0.5f)
            val styles = LabelStyles.from(style)

            for (place in searchPlaces) {
                val lat = place.latitude
                val lng = place.longitude
                if (lat == 0.0 || lng == 0.0) continue

                val options = LabelOptions.from("search_${place.id}", LatLng.from(lat, lng))
                    .setStyles(styles)
                layer.addLabel(options)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating search markers", e)
        }
    }

    // Handle panTo
    LaunchedEffect(panTo) {
        val map = kakaoMap ?: return@LaunchedEffect
        if (panTo == null) return@LaunchedEffect

        map.moveCamera(
            CameraUpdateFactory.newCenterPosition(LatLng.from(panTo.lat, panTo.lng), 15),
            CameraAnimation.from(500)
        )
        onPanToConsumed()
    }

    // Pan to selected lot
    LaunchedEffect(selectedLot) {
        val map = kakaoMap ?: return@LaunchedEffect
        if (selectedLot == null) return@LaunchedEffect

        val lat = selectedLot.latDouble
        val lng = selectedLot.lngDouble
        if (lat == 0.0 || lng == 0.0) return@LaunchedEffect

        map.moveCamera(
            CameraUpdateFactory.newCenterPosition(LatLng.from(lat, lng), 15),
            CameraAnimation.from(500)
        )
    }

    AndroidView(
        factory = { ctx ->
            context.value = ctx
            MapView(ctx).apply {
                start(
                    object : MapLifeCycleCallback() {
                        override fun onMapDestroy() {
                            Log.d(TAG, "Map destroyed")
                        }
                        override fun onMapError(error: Exception) {
                            Log.e(TAG, "Map error", error)
                        }
                    },
                    object : KakaoMapReadyCallback() {
                        override fun onMapReady(map: KakaoMap) {
                            Log.d(TAG, "Map ready")
                            kakaoMap = map

                            // Initial camera position (Seoul)
                            map.moveCamera(
                                CameraUpdateFactory.newCenterPosition(
                                    LatLng.from(37.5665, 126.978)
                                )
                            )
                            map.moveCamera(CameraUpdateFactory.zoomTo(13))

                            // 축척 바 (좌측 하단)
                            map.scaleBar?.apply {
                                setAutoHide(false)
                                setPosition(MapGravity.BOTTOM or MapGravity.LEFT, 16f, 16f)
                                show()
                            }

                            // Camera idle listener → bounds change
                            map.setOnCameraMoveEndListener { _, position, _ ->
                                try {
                                    val centerLat = position.position.latitude
                                    val centerLng = position.position.longitude
                                    // Approximate bounds from zoom level
                                    val zoomLevel = position.zoomLevel
                                    val latSpan = 180.0 / Math.pow(2.0, zoomLevel.toDouble()) * 2
                                    val lngSpan = 360.0 / Math.pow(2.0, zoomLevel.toDouble()) * 2

                                    val bounds = MapBounds(
                                        swLat = centerLat - latSpan / 2,
                                        swLng = centerLng - lngSpan / 2,
                                        neLat = centerLat + latSpan / 2,
                                        neLng = centerLng + lngSpan / 2,
                                    )
                                    currentZoomLevel = zoomLevel
                                    scope.launch {
                                        val region = resolveCenterRegion(ctx, centerLat, centerLng)
                                        latestOnBoundsChange(bounds, region, zoomLevel)
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error computing bounds", e)
                                }
                            }

                            // Label click → select lot
                            map.setOnLabelClickListener { _, _, label ->
                                val labelId = label.labelId
                                if (labelId.startsWith("lot_")) {
                                    val lotId = labelId.removePrefix("lot_").toIntOrNull()
                                    if (lotId != null) {
                                        val lot = latestParkingLots.find { it.id == lotId }
                                        if (lot != null) latestOnSelectLot(lot)
                                    }
                                } else if (labelId.startsWith("cluster_")) {
                                    val cluster = clusterLabelsRef.value[labelId] ?: return@setOnLabelClickListener
                                    // 현재 레벨의 다음 단계 줌으로 이동
                                    // 구(14) → 동(15) → 개별마커(16)
                                    val nextZoom = when {
                                        currentZoomLevel <= SIGUNGU_CLUSTER_MAX_ZOOM -> DONG_CLUSTER_MAX_ZOOM   // → 동 레벨 (15)
                                        currentZoomLevel <= DONG_CLUSTER_MAX_ZOOM -> DONG_CLUSTER_MAX_ZOOM + 1  // → 개별 마커 (16)
                                        else -> (currentZoomLevel + 1).coerceAtMost(17)
                                    }
                                    map.moveCamera(
                                        CameraUpdateFactory.newCenterPosition(
                                            LatLng.from(cluster.centerLat, cluster.centerLng),
                                            nextZoom,
                                        ),
                                        CameraAnimation.from(350)
                                    )
                                }
                            }

                            // Map click → deselect
                            map.setOnMapClickListener { _, _, _, _ ->
                                latestOnMapClick()
                            }
                        }

                        override fun getPosition(): LatLng {
                            return LatLng.from(37.5665, 126.978)
                        }

                        override fun getZoomLevel(): Int {
                            return 13
                        }
                    },
                )
            }
        },
        modifier = modifier,
    )
}
