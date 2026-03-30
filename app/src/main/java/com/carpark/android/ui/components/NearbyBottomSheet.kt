package com.carpark.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.carpark.android.data.model.NearbyParkingLot
import com.carpark.android.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

private fun formatDistance(m: Int): String {
    return if (m >= 1000) "%.1fkm".format(m / 1000.0) else "${m}m"
}

private val numberFmt = NumberFormat.getNumberInstance(Locale.KOREA)

@Composable
fun NearbyBottomSheet(
    open: Boolean,
    expanded: Boolean,
    lots: List<NearbyParkingLot>,
    loading: Boolean,
    onClose: () -> Unit,
    onReSearch: () -> Unit,
    onSelectLot: (NearbyParkingLot) -> Unit,
    onExpandChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sortBy by remember { mutableStateOf("distance") }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val sorted = remember(lots, sortBy) {
        if (sortBy == "distance") lots.sortedBy { it.distance }
        else lots.sortedBy { it.lot.basicFee }
    }

    AnimatedVisibility(
        visible = open,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier,
    ) {
        BoxWithConstraints {
            val parentHeightPx = constraints.maxHeight.toFloat()
            val collapsedPx = parentHeightPx * 0.55f
            val expandedPx = parentHeightPx * 0.92f
            val closePx = collapsedPx * 0.5f

            val heightPx = remember { Animatable(collapsedPx) }

            LaunchedEffect(expanded) {
                heightPx.animateTo(
                    if (expanded) expandedPx else collapsedPx,
                    tween(300),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { heightPx.value.toDp() })
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(Color.White),
            ) {
                // 드래그 핸들
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onVerticalDrag = { change, amount ->
                                    change.consume()
                                    scope.launch {
                                        heightPx.snapTo(
                                            (heightPx.value - amount).coerceIn(0f, expandedPx)
                                        )
                                    }
                                },
                                onDragEnd = {
                                    scope.launch {
                                        val current = heightPx.value
                                        val mid = (collapsedPx + expandedPx) / 2
                                        when {
                                            current < closePx -> {
                                                heightPx.animateTo(0f, tween(200))
                                                onClose()
                                            }
                                            current > mid -> {
                                                onExpandChange(true)
                                                heightPx.animateTo(expandedPx, tween(200))
                                            }
                                            else -> {
                                                onExpandChange(false)
                                                heightPx.animateTo(collapsedPx, tween(200))
                                            }
                                        }
                                    }
                                },
                            )
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .background(Gray300, RoundedCornerShape(2.dp)),
                    )
                }

            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("내 주변 주차장", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Gray900)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onReSearch) {
                        Text("현위치 재검색", fontSize = 12.sp)
                    }
                    TextButton(onClick = onClose) {
                        Text("✕", fontSize = 16.sp, color = Gray500)
                    }
                }
            }

            // Sort buttons
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SortChip("거리순", sortBy == "distance") { sortBy = "distance" }
                SortChip("요금순", sortBy == "fee") { sortBy = "fee" }
            }

            // List
            if (loading) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.height(8.dp))
                        Text("주변 주차장을 찾는 중...", fontSize = 13.sp, color = Gray500)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    itemsIndexed(sorted, key = { _, it -> it.lot.id }) { index, nearby ->
                        NearbyItem(
                            rank = index + 1,
                            nearby = nearby,
                            onClick = { onSelectLot(nearby) },
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun NearbyItem(
    rank: Int,
    nearby: NearbyParkingLot,
    onClick: () -> Unit,
) {
    val lot = nearby.lot

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Rank
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(Gray100, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("$rank", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Primary)
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    lot.parkingName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Gray900,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    formatDistance(nearby.distance),
                    fontSize = 13.sp,
                    color = Primary,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (lot.feeType == "유료") {
                    Text(
                        "유료",
                        fontSize = 11.sp,
                        color = Amber,
                        modifier = Modifier
                            .background(Amber.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                if (lot.basicFee > 0) {
                    Text(
                        "${numberFmt.format(lot.basicFee)}원/${lot.basicTime}분",
                        fontSize = 12.sp,
                        color = Gray500,
                    )
                }
                if (lot.totalCapacity > 0) {
                    Text("총 ${lot.totalCapacity}면", fontSize = 12.sp, color = Gray400)
                }
            }
        }

        Spacer(Modifier.width(8.dp))
        Text("›", fontSize = 18.sp, color = Gray300)
    }
}

@Composable
private fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Primary else Gray100
    val textColor = if (selected) Color.White else Gray600

    Text(
        text = label,
        fontSize = 12.sp,
        color = textColor,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}
