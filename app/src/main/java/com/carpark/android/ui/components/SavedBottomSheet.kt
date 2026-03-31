package com.carpark.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carpark.android.R
import com.carpark.android.data.model.ParkingLot
import com.carpark.android.ui.theme.Gray100
import com.carpark.android.ui.theme.Gray300
import com.carpark.android.ui.theme.Gray400
import com.carpark.android.ui.theme.Gray500
import com.carpark.android.ui.theme.Gray600
import com.carpark.android.ui.theme.Gray900
import com.carpark.android.ui.theme.Primary
import com.carpark.android.viewmodel.LatLngPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.launch

private enum class SavedSortOption(val label: String) {
    REGISTERED("등록순"),
    NAME("이름순"),
}

@Composable
fun SavedBottomSheet(
    open: Boolean,
    expanded: Boolean,
    lots: List<ParkingLot>,
    hasMore: Boolean = false,
    loading: Boolean = false,
    userLocation: LatLngPoint? = null,
    onSelectLot: (ParkingLot) -> Unit,
    onRemoveLot: (ParkingLot) -> Unit,
    onClose: () -> Unit,
    onExpandChange: (Boolean) -> Unit,
    onLoadMore: () -> Unit = {},
    onAddFavorite: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var sortOption by remember(open) { mutableStateOf(SavedSortOption.REGISTERED) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var editMode by remember(open) { mutableStateOf(false) }

    val displayedLots = remember(lots, sortOption, userLocation) {
        when (sortOption) {
            SavedSortOption.REGISTERED -> lots
            SavedSortOption.NAME -> lots.sortedBy { it.parkingName.lowercase() }
        }
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
                    targetValue = if (expanded) expandedPx else collapsedPx,
                    animationSpec = tween(300),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { heightPx.value.toDp() })
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(MaterialTheme.colorScheme.surface),
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
                                                heightPx.animateTo(0f, tween(180))
                                                onClose()
                                            }
                                            current > midpoint -> {
                                                onExpandChange(true)
                                                heightPx.animateTo(expandedPx, tween(220))
                                            }
                                            else -> {
                                                onExpandChange(false)
                                                heightPx.animateTo(collapsedPx, tween(220))
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

                // 헤더: 타이틀 + 닫기
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "저장한 주차장",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onAddFavorite) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("추가", fontSize = 12.sp)
                        }
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "close-saved-sheet",
                                tint = Gray500,
                            )
                        }
                    }
                }

                // 필터 행: 개수 + 정렬 + 편집
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "전체 ${lots.size}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Gray600,
                        )

                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Gray100)
                                    .clickable { sortMenuExpanded = true }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = sortOption.label,
                                    fontSize = 12.sp,
                                    color = Gray600,
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Gray500,
                                    modifier = Modifier.size(16.dp),
                                )
                            }

                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false },
                            ) {
                                SavedSortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label, fontSize = 13.sp) },
                                        onClick = {
                                            sortOption = option
                                            sortMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = if (editMode) "완료" else "편집",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { editMode = !editMode }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }

                HorizontalDivider(color = Gray100)

                // 본문
                when {
                    loading && displayedLots.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "불러오는 중",
                                    fontSize = 13.sp,
                                    color = Gray500,
                                )
                            }
                        }
                    }

                    displayedLots.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.BookmarkBorder,
                                    contentDescription = null,
                                    tint = Gray400,
                                    modifier = Modifier.size(36.dp),
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "저장한 주차장이 없습니다",
                                    fontSize = 14.sp,
                                    color = Gray500,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "자주 가는 주차장을 저장해보세요",
                                    fontSize = 12.sp,
                                    color = Gray400,
                                )
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        ) {
                            items(displayedLots, key = { it.id }) { lot ->
                                SavedLotRow(
                                    lot = lot,
                                    userLocation = userLocation,
                                    editMode = editMode,
                                    onClick = { onSelectLot(lot) },
                                    onRemove = { onRemoveLot(lot) },
                                )
                            }

                            if (hasMore) {
                                item {
                                    LaunchedEffect(displayedLots.size) { onLoadMore() }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedLotRow(
    lot: ParkingLot,
    userLocation: LatLngPoint?,
    editMode: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val distanceLabel = remember(lot, userLocation) { formatDistanceLabel(lot, userLocation) }
    val addressLine = lot.address.ifBlank { lot.district.ifBlank { "주소 정보 없음" } }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // P 마커 아이콘
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_bookmark),
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(16.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = lot.parkingName.ifBlank { "이름 없는 주차장" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (distanceLabel.isNotBlank()) {
                    Text(
                        text = distanceLabel,
                        fontSize = 13.sp,
                        color = Primary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = addressLine,
                    fontSize = 12.sp,
                    color = Gray500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (lot.parkingTypeName.isNotBlank()) {
                    Text(
                        text = "· ${lot.parkingTypeName}",
                        fontSize = 12.sp,
                        color = Gray400,
                        maxLines = 1,
                    )
                }
            }
        }

        if (editMode) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "삭제",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFEF4444),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onRemove)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}

private fun formatDistanceLabel(
    lot: ParkingLot,
    userLocation: LatLngPoint?,
): String {
    if (userLocation == null || lot.latDouble == 0.0 || lot.lngDouble == 0.0) {
        return ""
    }

    val meters = haversineDistanceMeters(
        lat1 = userLocation.lat,
        lng1 = userLocation.lng,
        lat2 = lot.latDouble,
        lng2 = lot.lngDouble,
    )

    return if (meters >= 1000) {
        "${((meters / 100.0).roundToInt() / 10.0)}km"
    } else {
        "${meters}m"
    }
}

private fun haversineDistanceMeters(
    lat1: Double,
    lng1: Double,
    lat2: Double,
    lng2: Double,
): Int {
    val earthRadius = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
    return (earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))).roundToInt()
}
