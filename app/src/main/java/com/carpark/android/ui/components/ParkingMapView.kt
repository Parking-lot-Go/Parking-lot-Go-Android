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

private const val TAG = "ParkingMapView"

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

private fun createStaticMarkerBitmap(
    context: Context,
    isDarkMode: Boolean,
    isSelected: Boolean = false,
    scale: Float = 1f,
): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = ((22 * density) * scale).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val drawable = ContextCompat.getDrawable(context, R.drawable.ic_parking)
        ?: return bitmap
    val wrapped = DrawableCompat.wrap(drawable.mutate())

    if (isSelected) {
        // Selection highlight circle
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#3B82F6")
            alpha = 50
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint)

        DrawableCompat.setTint(wrapped, Color.parseColor("#2563EB"))
    } else {
        val tintColor = if (isDarkMode) {
            Color.parseColor("#60A5FA") // blue-400
        } else {
            Color.parseColor("#3B82F6") // blue-500
        }
        DrawableCompat.setTint(wrapped, tintColor)
    }

    val padding = (2 * density).toInt()
    wrapped.setBounds(padding, padding, size - padding, size - padding)
    wrapped.draw(canvas)

    return bitmap
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
): Bitmap {
    val density = context.resources.displayMetrics.density
    val scaledDensity = density * scale
    val w = (if (text != null) 48 else 36).times(scaledDensity).toInt().coerceAtLeast(1)
    val h = (52 * scaledDensity).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val headRadius = (if (text != null) 20 else 15) * scaledDensity
    val cx = w / 2f
    val cy = headRadius + 2 * scaledDensity

    // Shadow
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.argb(40, 0, 0, 0)
        maskFilter = BlurMaskFilter(4 * scaledDensity, BlurMaskFilter.Blur.NORMAL)
    }
    canvas.drawCircle(cx, cy + 2 * scaledDensity, headRadius, shadowPaint)

    // Tail
    val tailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    val tailPath = Path().apply {
        moveTo(cx - 6 * scaledDensity, cy + headRadius * 0.5f)
        lineTo(cx, cy + headRadius + 10 * scaledDensity)
        lineTo(cx + 6 * scaledDensity, cy + headRadius * 0.5f)
        close()
    }
    canvas.drawPath(tailPath, tailPaint)

    // Head circle
    val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    canvas.drawCircle(cx, cy, headRadius, headPaint)

    // Border if selected
    if (isSelected) {
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.color = android.graphics.Color.WHITE
            strokeWidth = 3 * scaledDensity
        }
        canvas.drawCircle(cx, cy, headRadius - 1.5f * scaledDensity, borderPaint)
    }

    // P text
    val pPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        textSize = (if (text != null) 13 else 14) * scaledDensity
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    if (text != null) {
        // P on left, count on right
        pPaint.textSize = 12 * scaledDensity
        val pX = cx - 7 * scaledDensity
        canvas.drawText("P", pX, cy + 5 * scaledDensity, pPaint)

        val countPaint = Paint(pPaint).apply {
            textSize = 11 * scaledDensity
            typeface = Typeface.DEFAULT_BOLD
        }
        val countX = cx + 8 * scaledDensity
        canvas.drawText(text, countX, cy + 5 * scaledDensity, countPaint)
    } else {
        canvas.drawText("P", cx, cy + 5 * scaledDensity, pPaint)
    }

    return bitmap
}

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
    val anchor = if (isNotLinked) 0.5f else 1.0f
    return LabelStyles.from(
        LabelStyle.from(bitmap).setAnchorPoint(0.5f, anchor)
    )
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
    onBoundsChange: (MapBounds, String?) -> Unit,
    onSelectLot: (ParkingLot) -> Unit,
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDarkMode = isAppInDarkTheme()
    val scope = rememberCoroutineScope()
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    val context = remember { mutableStateOf<Context?>(null) }
    val lotLabelsRef = remember { mutableStateOf<Map<Int, Label>>(emptyMap()) }

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

    // Update markers when lots or mode change
    LaunchedEffect(kakaoMap, parkingLots, dataMode, selectedLot, isDarkMode) {
        val map = kakaoMap ?: return@LaunchedEffect
        val ctx = context.value ?: return@LaunchedEffect
        val labelManager = map.labelManager ?: return@LaunchedEffect

        try {
            val layer = labelManager.layer ?: labelManager.addLayer(
                LabelLayerOptions.from("parking_markers").setZOrder(1)
            ) ?: return@LaunchedEffect

            val existingLabels = lotLabelsRef.value.toMutableMap()
            val newLotIds = parkingLots.map { it.id }.toSet()

            // Remove labels for lots no longer visible
            existingLabels.keys.filter { it !in newLotIds }.forEach { id ->
                existingLabels[id]?.let { layer.remove(it) }
                existingLabels.remove(id)
            }

            // Add/update labels
            for (lot in parkingLots) {
                val lat = lot.latDouble
                val lng = lot.lngDouble
                if (lat == 0.0 || lng == 0.0) continue

                val isSelected = selectedLot?.id == lot.id
                val styles = createLotLabelStyles(
                    context = ctx,
                    lot = lot,
                    dataMode = dataMode,
                    isDarkMode = isDarkMode,
                    isSelected = isSelected,
                )
                val existing = existingLabels[lot.id]
                if (existing != null) {
                    existing.changeStyles(styles)
                } else {
                    val options = LabelOptions.from("lot_${lot.id}", LatLng.from(lat, lng))
                        .setStyles(styles)
                        .setClickable(true)
                    val label = layer.addLabel(options)
                    if (label != null) {
                        existingLabels[lot.id] = label
                    }
                }
            }

            lotLabelsRef.value = existingLabels
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
                                    scope.launch {
                                        val region = resolveCenterRegion(ctx, centerLat, centerLng)
                                        onBoundsChange(bounds, region)
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
                                        val lot = parkingLots.find { it.id == lotId }
                                        if (lot != null) onSelectLot(lot)
                                    }
                                }
                            }

                            // Map click → deselect
                            map.setOnMapClickListener { _, _, _, _ ->
                                onMapClick()
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
