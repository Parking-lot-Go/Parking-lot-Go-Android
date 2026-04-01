package com.carpark.android.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.painterResource
import com.carpark.android.R
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carpark.android.data.model.DataMode
import com.carpark.android.data.model.ParkingLot
import com.carpark.android.ui.theme.Amber
import com.carpark.android.ui.theme.Gray100
import com.carpark.android.ui.theme.Gray200
import com.carpark.android.ui.theme.Gray300
import com.carpark.android.ui.theme.Gray400
import com.carpark.android.ui.theme.Gray500
import com.carpark.android.ui.theme.Gray600
import com.carpark.android.ui.theme.Gray700
import com.carpark.android.ui.theme.Gray900
import com.carpark.android.ui.theme.Green
import com.carpark.android.ui.theme.Primary
import com.carpark.android.ui.theme.Red
import com.carpark.android.ui.theme.isAppInDarkTheme
import com.carpark.android.util.NavigationHelper
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

private val numberFmt = NumberFormat.getNumberInstance(Locale.KOREA)

private fun safeText(value: String?): String = value.orEmpty()

private fun formatTime(t: String?): String {
    val value = safeText(t)
    if (value.isBlank() || value.length < 4) return value
    return "${value.substring(0, 2)}:${value.substring(2)}"
}

private fun getStatusColor(lot: ParkingLot): Color {
    if (lot.totalCapacity == 0) return Gray400
    val ratio = lot.availableCount.toFloat() / lot.totalCapacity
    return when {
        ratio > 0.3f -> Green
        ratio > 0.1f -> Amber
        else -> Red
    }
}

private fun getStatusLabel(lot: ParkingLot): String {
    if (lot.totalCapacity == 0) return "정보없음"
    val ratio = lot.availableCount.toFloat() / lot.totalCapacity
    return when {
        ratio > 0.3f -> "여유"
        ratio > 0.1f -> "보통"
        else -> "혼잡"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NearbyParkingInfoPager(
    lots: List<ParkingLot>,
    selectedIndex: Int,
    dataMode: DataMode,
    isSaved: (Int) -> Boolean,
    onToggleSave: (ParkingLot) -> Unit,
    onClose: () -> Unit,
    onShowDetail: (ParkingLot) -> Unit,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (lots.isEmpty()) return

    val safeIndex = selectedIndex.coerceIn(0, lots.lastIndex)
    val pagerState = rememberPagerState(
        initialPage = safeIndex,
        pageCount = { lots.size },
    )

    LaunchedEffect(safeIndex, lots.size) {
        if (pagerState.currentPage != safeIndex) {
            pagerState.scrollToPage(safeIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage, safeIndex, lots.size) {
        if (pagerState.currentPage != safeIndex && pagerState.currentPage in lots.indices) {
            onPageSelected(pagerState.currentPage)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) { page ->
            val lot = lots[page]
            ParkingInfoCard(
                lot = lot,
                dataMode = dataMode,
                isSaved = isSaved(lot.id),
                onToggleSave = { onToggleSave(lot) },
                onClose = onClose,
                onShowDetail = { onShowDetail(lot) },
            )
        }

        if (lots.size > 1) {
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(lots.size) { index ->
                    val isActive = index == pagerState.currentPage
                    Spacer(
                        modifier = Modifier
                            .size(width = if (isActive) 16.dp else 6.dp, height = 6.dp)
                            .background(
                                color = if (isActive) Gray700 else Gray300,
                                shape = RoundedCornerShape(999.dp),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
fun ParkingInfoCard(
    lot: ParkingLot,
    dataMode: DataMode,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onClose: () -> Unit,
    onShowDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val detailDragThreshold = remember { 72f }
    val isRealtime = dataMode == DataMode.REALTIME
    val parkingName = safeText(lot.parkingName).ifBlank { "이름 없는 주차장" }
    val parkingTypeName = safeText(lot.parkingTypeName)
    val address = safeText(lot.address)
    val weekdayStart = safeText(lot.weekdayStart)
    val weekdayEnd = safeText(lot.weekdayEnd)
    val isDark = isAppInDarkTheme()
    val cardContainerColor = Color.White
    val statusColor = if (isRealtime) getStatusColor(lot) else Gray400
    val titleColor = Gray900
    val primaryBodyColor = Gray700
    val secondaryBodyColor = Gray600
    val tertiaryBodyColor = Gray500
    val iconColor = Gray400
    val actionButtonContentColor = Gray700
    val actionButtonBorderColor = Gray100
    val actionButtonContainerColor = Color.White
    val saveIconTint = if (isSaved) Amber else actionButtonContentColor
    val titleStyle = TextStyle(
        shadow = if (isDark && cardContainerColor != Color.White) {
            Shadow(
                color = Color.Black.copy(alpha = 0.35f),
                offset = Offset(0f, 1.5f),
                blurRadius = 6f,
            )
        } else {
            Shadow(color = Color.Transparent)
        }
    )

    androidx.compose.material3.Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .pointerInput(lot.id) {
                var totalDragY = 0f
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        totalDragY += dragAmount
                    },
                    onDragEnd = {
                        if (totalDragY <= -detailDragThreshold) {
                            onShowDetail()
                        }
                        totalDragY = 0f
                    },
                    onDragCancel = {
                        totalDragY = 0f
                    },
                )
            },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 4.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.12f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Gray300),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "위로 드래그하면 상세 보기",
                        fontSize = 11.sp,
                        color = Gray500,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = parkingName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = titleColor,
                            style = titleStyle,
                        )
                        if (isRealtime) {
                            Spacer(Modifier.size(8.dp))
                            Text(
                                text = getStatusLabel(lot),
                                fontSize = 11.sp,
                                color = Color.White,
                                modifier = Modifier
                                    .background(statusColor, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                    if (parkingTypeName.isNotBlank()) {
                        Text(text = parkingTypeName, fontSize = 12.sp, color = tertiaryBodyColor)
                    }
                }

                IconButton(onClick = onToggleSave, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painter = painterResource(
                            if (isSaved) R.drawable.ic_star_filled else R.drawable.ic_star_outline
                        ),
                        contentDescription = "save-lot",
                        tint = saveIconTint,
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "close-card",
                        tint = iconColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (isRealtime && lot.totalCapacity > 0) {
                val usedRatio = ((lot.totalCapacity - lot.availableCount).toFloat() / lot.totalCapacity)
                    .coerceIn(0f, 1f)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = "주차 가능", fontSize = 12.sp, color = tertiaryBodyColor)
                    Text(
                        text = "${lot.availableCount} / ${lot.totalCapacity}면",
                        fontSize = 13.sp,
                        color = primaryBodyColor,
                    )
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { usedRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = statusColor,
                    trackColor = Gray100,
                )
            } else if (lot.totalCapacity > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = "총 주차면", fontSize = 12.sp, color = tertiaryBodyColor)
                    Text(
                        text = "${lot.totalCapacity}면",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = primaryBodyColor,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (address.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text(text = address, fontSize = 12.sp, color = secondaryBodyColor, maxLines = 1)
                }
                Spacer(Modifier.height(4.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(4.dp))
                Text(
                    text = "평일 ${formatTime(weekdayStart)} ~ ${formatTime(weekdayEnd)}",
                    fontSize = 12.sp,
                    color = secondaryBodyColor,
                )
            }

            Spacer(Modifier.height(4.dp))

            val feeText = if (lot.feeType == "유료" && lot.basicFee > 0) {
                "기본 ${numberFmt.format(lot.basicFee)}원 / ${lot.basicTime}분"
            } else if (lot.feeType == "유료") {
                "유료"
            } else {
                "무료"
            }
            Text(
                text = feeText,
                fontSize = 12.sp,
                color = if (lot.feeType == "유료") Amber else Green,
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onToggleSave,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, actionButtonBorderColor),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = actionButtonContainerColor,
                        contentColor = actionButtonContentColor,
                    ),
                ) {
                    Icon(
                        painter = painterResource(
                            if (isSaved) R.drawable.ic_star_filled else R.drawable.ic_star_outline
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = saveIconTint,
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = if (isSaved) "저장됨" else "저장",
                        fontSize = 13.sp,
                        color = actionButtonContentColor,
                    )
                }
                OutlinedButton(
                    onClick = {
                        NavigationHelper.openNavigation(
                            context = context,
                            lat = lot.latDouble,
                            lng = lot.lngDouble,
                            name = parkingName,
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, actionButtonBorderColor),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = actionButtonContainerColor,
                        contentColor = actionButtonContentColor,
                    ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.track_order),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = actionButtonContentColor,
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = "길안내",
                        fontSize = 13.sp,
                        color = actionButtonContentColor,
                    )
                }
                Button(
                    onClick = onShowDetail,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(text = "상세보기", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun ExpandableParkingCard(
    lot: ParkingLot,
    dataMode: DataMode,
    expanded: Boolean,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onClose: () -> Unit,
    onExpandChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val context = LocalContext.current
        val density = LocalDensity.current
        val scope = rememberCoroutineScope()

        val parentHeightPx = maxHeight.value * density.density
        val collapsedPx = 300f * density.density
        val expandedPx = parentHeightPx
        val closePx = collapsedPx * 0.4f
        val heightPx = remember { Animatable(collapsedPx) }
        val expansion = ((heightPx.value - collapsedPx) / (expandedPx - collapsedPx)).coerceIn(0f, 1f)

        LaunchedEffect(expanded) {
            heightPx.animateTo(
                targetValue = if (expanded) expandedPx else collapsedPx,
                animationSpec = tween(300),
            )
        }

        LaunchedEffect(lot.id) {
            heightPx.snapTo(collapsedPx)
            onExpandChange(false)
        }

        val isRealtime = dataMode == DataMode.REALTIME
        val parkingName = safeText(lot.parkingName).ifBlank { "이름 없는 주차장" }
        val parkingTypeName = safeText(lot.parkingTypeName)
        val address = safeText(lot.address)
        val phone = safeText(lot.phone)
        val updatedAt = safeText(lot.updatedAt)
        val statusColor = if (isRealtime) getStatusColor(lot) else Gray400

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { heightPx.value.toDp() })
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(Color.White),
        ) {
            // Handle bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(lot.id) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, amount ->
                                change.consume()
                                scope.launch {
                                    heightPx.snapTo(
                                        (heightPx.value - amount).coerceIn(0f, expandedPx),
                                    )
                                }
                            },
                            onDragEnd = {
                                scope.launch {
                                    val current = heightPx.value
                                    val midpoint = (collapsedPx + expandedPx) / 2f
                                    when {
                                        current < closePx -> {
                                            heightPx.animateTo(0f, tween(200))
                                            onClose()
                                        }
                                        current > midpoint -> {
                                            onExpandChange(true)
                                            heightPx.animateTo(expandedPx, tween(300))
                                        }
                                        else -> {
                                            onExpandChange(false)
                                            heightPx.animateTo(collapsedPx, tween(300))
                                        }
                                    }
                                }
                            },
                        )
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .width(42.dp)
                            .height(4.dp)
                            .background(Gray300, RoundedCornerShape(999.dp)),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (expanded) "아래로 드래그하면 요약 보기" else "위로 드래그하면 상세 보기",
                        fontSize = 11.sp,
                        color = Gray500,
                    )
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(if (expanded) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
            ) {
                // ── Summary ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(parkingName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Gray900)
                            if (isRealtime) {
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    text = getStatusLabel(lot),
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    modifier = Modifier
                                        .background(statusColor, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                        if (parkingTypeName.isNotBlank()) {
                            Text(parkingTypeName, fontSize = 12.sp, color = Gray500)
                        }
                    }
                    IconButton(onClick = onToggleSave, modifier = Modifier.size(32.dp)) {
                        Icon(
                            painter = painterResource(
                                if (isSaved) R.drawable.ic_star_filled else R.drawable.ic_star_outline,
                            ),
                            contentDescription = "save",
                            tint = if (isSaved) Amber else Gray700,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "close", tint = Gray400, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (isRealtime && lot.totalCapacity > 0) {
                    val usedRatio = ((lot.totalCapacity - lot.availableCount).toFloat() / lot.totalCapacity).coerceIn(0f, 1f)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("주차 가능", fontSize = 12.sp, color = Gray500)
                        Text("${lot.availableCount} / ${lot.totalCapacity}면", fontSize = 13.sp, color = Gray700)
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { usedRatio },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = statusColor,
                        trackColor = Gray100,
                    )
                } else if (lot.totalCapacity > 0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("총 주차면", fontSize = 12.sp, color = Gray500)
                        Text("${lot.totalCapacity}면", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Gray700)
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (address.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, null, tint = Gray400, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(address, fontSize = 12.sp, color = Gray600, maxLines = if (expanded) Int.MAX_VALUE else 1)
                    }
                    Spacer(Modifier.height(4.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = Gray400, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("평일 ${formatTime(lot.weekdayStart)} ~ ${formatTime(lot.weekdayEnd)}", fontSize = 12.sp, color = Gray600)
                }
                Spacer(Modifier.height(4.dp))

                val feeText = if (lot.feeType == "유료" && lot.basicFee > 0) {
                    "기본 ${numberFmt.format(lot.basicFee)}원 / ${lot.basicTime}분"
                } else if (lot.feeType == "유료") "유료" else "무료"
                Text(feeText, fontSize = 12.sp, color = if (lot.feeType == "유료") Amber else Green)

                // ── Action buttons (collapsed only) ──
                if (!expanded) {
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onToggleSave,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Gray100),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Gray700),
                        ) {
                            Icon(
                                painter = painterResource(if (isSaved) R.drawable.ic_star_filled else R.drawable.ic_star_outline),
                                null, modifier = Modifier.size(16.dp), tint = if (isSaved) Amber else Gray700,
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(if (isSaved) "저장됨" else "저장", fontSize = 13.sp, color = Gray700)
                        }
                        OutlinedButton(
                            onClick = {
                                NavigationHelper.openNavigation(context, lot.latDouble, lot.lngDouble, parkingName)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Gray100),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Gray700),
                        ) {
                            Icon(painterResource(R.drawable.track_order), null, modifier = Modifier.size(16.dp), tint = Gray700)
                            Spacer(Modifier.size(4.dp))
                            Text("길안내", fontSize = 13.sp, color = Gray700)
                        }
                        Button(
                            onClick = { onExpandChange(true) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("상세보기", fontSize = 13.sp)
                        }
                    }
                }

                // ── Detail content (expanded) ──
                if (expansion > 0.1f) {
                    Column(modifier = Modifier.graphicsLayer { alpha = expansion }) {
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = Gray200)
                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            ExpandableActionButton(
                                icon = Icons.Default.Share,
                                label = "공유",
                                onClick = {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        putExtra(Intent.EXTRA_TEXT, "$parkingName\n$address")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "공유"))
                                },
                            )
                            ExpandableActionButton(
                                icon = Icons.Default.Star,
                                label = if (isSaved) "저장됨" else "저장",
                                primary = isSaved,
                                onClick = onToggleSave,
                            )
                            ExpandableActionButton(
                                icon = Icons.Default.Navigation,
                                label = "길안내",
                                primary = true,
                                onClick = {
                                    NavigationHelper.openNavigation(context, lot.latDouble, lot.lngDouble, parkingName)
                                },
                            )
                        }

                        Spacer(Modifier.height(20.dp))
                        HorizontalDivider(color = Gray200)
                        Spacer(Modifier.height(16.dp))

                        if (phone.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                                },
                            ) {
                                Icon(Icons.Default.Phone, null, tint = Gray400, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(phone, fontSize = 14.sp, color = Primary)
                            }
                            Spacer(Modifier.height(12.dp))
                        }

                        Text("주차 요금", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Gray900, modifier = Modifier.padding(bottom = 8.dp))
                        ExpandableDetailRow("기본 요금", if (lot.basicFee > 0) "${lot.basicTime}분 ${numberFmt.format(lot.basicFee)}원" else "정보 없음")
                        if (lot.additionalFee > 0) {
                            ExpandableDetailRow("추가 요금", "${lot.additionalTime}분 ${numberFmt.format(lot.additionalFee)}원")
                        }
                        if (lot.dayMaxFee > 0) {
                            ExpandableDetailRow("일 최대", "${numberFmt.format(lot.dayMaxFee)}원")
                        }
                        if (lot.monthlyFee > 0) {
                            ExpandableDetailRow("월 정기권", "${numberFmt.format(lot.monthlyFee)}원")
                        }

                        Spacer(Modifier.height(12.dp))
                        Text("운영 시간", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Gray900, modifier = Modifier.padding(bottom = 8.dp))
                        ExpandableTimeRow("평일", lot.weekdayStart, lot.weekdayEnd)
                        ExpandableTimeRow("주말", lot.weekendStart, lot.weekendEnd)
                        ExpandableTimeRow("공휴일", lot.holidayStart, lot.holidayEnd)

                        if (updatedAt.isNotBlank()) {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "${updatedAt.take(10).replace("-", ".")} 기준 정보입니다.",
                                fontSize = 12.sp,
                                color = Gray400,
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandableActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    primary: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp),
    ) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (primary) Primary else Gray100,
                contentColor = if (primary) Color.White else Gray700,
            ),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = if (label == "저장됨") Amber else Color.Unspecified,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(text = label, fontSize = 12.sp, color = Gray600)
    }
}

@Composable
private fun ExpandableDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 14.sp, color = Gray500)
        Text(value, fontSize = 14.sp, color = Gray900)
    }
}

@Composable
private fun ExpandableTimeRow(label: String, start: String?, end: String?) {
    val s = formatTime(start)
    val e = formatTime(end)
    val value = when {
        s.isBlank() && e.isBlank() -> "운영 정보 없음"
        s == "00:00" && e == "23:59" -> "00:00 ~ 24:00"
        else -> "$s ~ $e"
    }
    ExpandableDetailRow(label, value)
}
