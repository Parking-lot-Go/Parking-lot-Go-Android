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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val TAG = "ParkingMapView"
// 실제 뷰포트 반경(m) 기반 클러스터 임계값
private const val INDIVIDUAL_MAX_RADIUS = 2000   // ≤2000m → 개별 마커, >2000m → 그리드 클러스터

private const val CLUSTER_GRID_SIZE = 3
private const val CLUSTER_GRID_CELL_COUNT = CLUSTER_GRID_SIZE * CLUSTER_GRID_SIZE

private data class ClusterMarker(
    val labelId: String,
    val centerLat: Double,
    val centerLng: Double,
    val count: Int,
)

private fun approximateVisibleRadiusMeters(bounds: MapBounds): Int {
    val centerLat = (bounds.swLat + bounds.neLat) / 2.0
    val centerLng = (bounds.swLng + bounds.neLng) / 2.0
    val latRadius = haversineMeters(centerLat, centerLng, bounds.neLat, centerLng)
    val lngRadius = haversineMeters(centerLat, centerLng, centerLat, bounds.neLng)
    return max(latRadius, lngRadius)
}

private fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Int {
    val r = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
    return (r * 2 * atan2(sqrt(a), sqrt(1 - a))).toInt()
}

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

private fun createClusterBadgeBitmap(
    context: Context,
    count: Int,
    isDarkMode: Boolean,
): Bitmap {
    val density = context.resources.displayMetrics.density
    val countText = if (count >= 1000) "999+" else count.toString()

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isDarkMode) android.graphics.Color.WHITE
            else android.graphics.Color.parseColor("#1E40AF")
        textSize = 13f * density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    val textWidth = textPaint.measureText(countText)
    val textHeight = -textPaint.ascent() + textPaint.descent()

    val padH = 11f * density
    val padV = 6f * density
    val strokeW = 1.5f * density
    val pad = 4f * density

    val innerW = textWidth + padH * 2
    val innerH = textHeight + padV * 2
    val totalW = (innerW + strokeW * 2 + pad * 2).toInt()
        .coerceAtLeast((innerH + strokeW * 2 + pad * 2).toInt())
    val totalH = (innerH + strokeW * 2 + pad * 2).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cx = totalW / 2f
    val cy = totalH / 2f
    val halfW = innerW / 2
    val halfH = innerH / 2
    val radius = 10f * density

    // 반투명 배경
    val fillColor = if (isDarkMode) android.graphics.Color.argb(180, 30, 58, 138)
        else android.graphics.Color.argb(170, 255, 255, 255)
    val strokeColor = if (isDarkMode) android.graphics.Color.argb(200, 96, 165, 250)
        else android.graphics.Color.argb(220, 59, 130, 246)

    // 그림자
    canvas.drawRoundRect(
        RectF(cx - halfW + 0.5f * density, cy - halfH + density,
            cx + halfW + 0.5f * density, cy + halfH + density),
        radius, radius,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(30, 0, 0, 0)
            maskFilter = BlurMaskFilter(2f * density, BlurMaskFilter.Blur.NORMAL)
        },
    )

    // 반투명 채우기
    canvas.drawRoundRect(
        RectF(cx - halfW, cy - halfH, cx + halfW, cy + halfH),
        radius, radius,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fillColor },
    )

    // 테두리
    canvas.drawRoundRect(
        RectF(cx - halfW, cy - halfH, cx + halfW, cy + halfH),
        radius, radius,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            color = strokeColor
        },
    )

    // 숫자 텍스트
    val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2
    canvas.drawText(countText, cx, textY, textPaint)

    return bitmap
}

private fun createClusterLabelStyles(
    context: Context,
    cluster: ClusterMarker,
    isDarkMode: Boolean,
): LabelStyles {
    val bitmap = createClusterBadgeBitmap(
        context = context,
        count = cluster.count,
        isDarkMode = isDarkMode,
    )
    return LabelStyles.from(
        LabelStyle.from(bitmap).setAnchorPoint(0.5f, 0.5f)
    )
}

private fun buildClusterMarkers(
    lots: List<ParkingLot>,
    visibleRadiusMeters: Int,
    bounds: MapBounds?,
): List<Pair<ClusterMarker?, List<ParkingLot>>> {
    if (lots.isEmpty() || bounds == null) return emptyList()

    if (visibleRadiusMeters <= INDIVIDUAL_MAX_RADIUS) {
        Log.d(TAG, "Clustering OFF: radius=${visibleRadiusMeters}m ≤ ${INDIVIDUAL_MAX_RADIUS}m → individual markers")
        return emptyList()
    }
    Log.d(TAG, "Clustering ON (grid): radius=${visibleRadiusMeters}m, lots=${lots.size}")

    val latStep = (bounds.neLat - bounds.swLat) / CLUSTER_GRID_SIZE
    val lngStep = (bounds.neLng - bounds.swLng) / CLUSTER_GRID_SIZE
    if (latStep <= 0 || lngStep <= 0) return emptyList()

    val gridCells = Array(CLUSTER_GRID_CELL_COUNT) { mutableListOf<ParkingLot>() }

    for (lot in lots) {
        if (lot.latDouble == 0.0 || lot.lngDouble == 0.0) continue
        val row = ((bounds.neLat - lot.latDouble) / latStep).toInt()
            .coerceIn(0, CLUSTER_GRID_SIZE - 1)
        val col = ((lot.lngDouble - bounds.swLng) / lngStep).toInt()
            .coerceIn(0, CLUSTER_GRID_SIZE - 1)
        gridCells[row * CLUSTER_GRID_SIZE + col].add(lot)
    }

    return gridCells.mapIndexedNotNull { index, cellLots ->
        if (cellLots.isEmpty()) return@mapIndexedNotNull null
        val row = index / CLUSTER_GRID_SIZE
        val col = index % CLUSTER_GRID_SIZE
        // 실제 주차장들의 중심점에 배치 (자연스러운 위치)
        val centerLat = cellLots.map { it.latDouble }.average()
        val centerLng = cellLots.map { it.lngDouble }.average()

        ClusterMarker(
            labelId = "cluster_${row}_${col}",
            centerLat = centerLat,
            centerLng = centerLng,
            count = cellLots.size,
        ) to cellLots
    }
}

private fun createSearchMarkerBitmap(context: Context): Bitmap {
    val dp = context.resources.displayMetrics.density
    val pinColor = android.graphics.Color.parseColor("#2563EB")

    // 핀 헤드 + 꼬리
    val headR = 8f * dp
    val border = 2f * dp
    val tailH = 6f * dp
    val pad = 3f * dp
    val w = ((headR + border) * 2 + pad * 2).toInt().coerceAtLeast(1)
    val h = ((headR + border) * 2 + tailH + pad * 2).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = w / 2f
    val cy = headR + border + pad

    // 그림자
    canvas.drawCircle(cx, cy + dp, headR + border, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(25, 0, 0, 0)
        maskFilter = BlurMaskFilter(2.5f * dp, BlurMaskFilter.Blur.NORMAL)
    })

    // 꼬리
    val tailPath = Path().apply {
        moveTo(cx - 4f * dp, cy + headR * 0.4f)
        lineTo(cx, cy + headR + tailH)
        lineTo(cx + 4f * dp, cy + headR * 0.4f)
        close()
    }
    canvas.drawPath(tailPath, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pinColor })

    // 흰 테두리 원
    canvas.drawCircle(cx, cy, headR + border, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
    })

    // 파란 원
    canvas.drawCircle(cx, cy, headR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = pinColor
    })

    // 흰 내부 점 (위치 표시)
    canvas.drawCircle(cx, cy, 2.5f * dp, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
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
    var currentVisibleBounds by remember { mutableStateOf<MapBounds?>(null) }
    var currentVisibleRadiusMeters by remember { mutableStateOf(Int.MAX_VALUE) }
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
        currentVisibleRadiusMeters,
        isNearbyMode,
    ) {
        val map = kakaoMap ?: return@LaunchedEffect
        val ctx = context.value ?: return@LaunchedEffect
        val labelManager = map.labelManager ?: return@LaunchedEffect

        try {
            val layer = labelManager.getLayer("parking_markers")
                ?: labelManager.addLayer(
                    LabelLayerOptions.from("parking_markers").setZOrder(1)
                ) ?: return@LaunchedEffect

            val clusteredEntries = if (!isNearbyMode) {
                withContext(Dispatchers.Default) {
                    buildClusterMarkers(parkingLots, currentVisibleRadiusMeters, currentVisibleBounds)
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
            val style = LabelStyle.from(bitmap).setAnchorPoint(0.5f, 1.0f)
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
                                    currentVisibleBounds = bounds
                                    currentVisibleRadiusMeters = approximateVisibleRadiusMeters(bounds)
                                    val isClustering = currentVisibleRadiusMeters > INDIVIDUAL_MAX_RADIUS
                                    Log.d(TAG, "zoom=$zoomLevel, radius=${currentVisibleRadiusMeters}m, clustering=$isClustering")
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
                                    // 한 단계 줌인 → 반경 ~절반 → 구→동→개별 순
                                    val nextZoom = (currentZoomLevel + 1).coerceAtMost(17)
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
