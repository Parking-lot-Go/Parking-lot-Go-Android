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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import com.carpark.android.data.model.ParkingLot
import com.carpark.android.ui.theme.Gray100
import com.carpark.android.ui.theme.Gray300
import com.carpark.android.ui.theme.Gray500
import com.carpark.android.ui.theme.Gray700
import com.carpark.android.ui.theme.Gray900
import com.carpark.android.ui.theme.Primary

@Composable
fun SavedBottomSheet(
    open: Boolean,
    expanded: Boolean,
    lots: List<ParkingLot>,
    hasMore: Boolean = false,
    loading: Boolean = false,
    onSelectLot: (ParkingLot) -> Unit,
    onRemoveLot: (ParkingLot) -> Unit,
    onClose: () -> Unit,
    onExpandChange: (Boolean) -> Unit,
    onLoadMore: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    AnimatedVisibility(
        visible = open,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier,
    ) {
        BoxWithConstraints {
            val parentHeightPx = constraints.maxHeight.toFloat()
            val collapsedPx = parentHeightPx * 0.50f
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
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
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
                        .size(width = 36.dp, height = 4.dp)
                        .background(Gray300, RoundedCornerShape(2.dp)),
                )
            }

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
                TextButton(onClick = onClose) {
                    Text(text = "닫기", fontSize = 16.sp, color = Gray500)
                }
            }

            if (lots.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "저장된 주차장이 없습니다.", fontSize = 14.sp, color = Gray500)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 8.dp),
                ) {
                    items(lots, key = { it.id }) { lot ->
                        SavedLotRow(
                            lot = lot,
                            onClick = { onSelectLot(lot) },
                            onRemove = { onRemoveLot(lot) },
                        )
                    }
                    if (hasMore) {
                        item {
                            LaunchedEffect(Unit) { onLoadMore() }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
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

@Composable
private fun SavedLotRow(
    lot: ParkingLot,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = lot.parkingName,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "remove-saved-lot",
                        tint = Gray500,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            if (lot.address.isNotBlank()) {
                Text(
                    text = lot.address,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }

            if (lot.totalCapacity > 0) {
                Spacer(Modifier.size(6.dp))
                Text(
                    text = "총 ${lot.totalCapacity}면",
                    color = Gray500,
                    fontSize = 12.sp,
                )
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
}
