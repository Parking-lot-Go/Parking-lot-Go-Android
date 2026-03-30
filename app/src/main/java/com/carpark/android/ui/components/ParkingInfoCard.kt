package com.carpark.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.painterResource
import com.carpark.android.R
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carpark.android.data.model.DataMode
import com.carpark.android.data.model.ParkingLot
import com.carpark.android.ui.theme.Amber
import com.carpark.android.ui.theme.Gray100
import com.carpark.android.ui.theme.Gray300
import com.carpark.android.ui.theme.Gray400
import com.carpark.android.ui.theme.Gray500
import com.carpark.android.ui.theme.Gray600
import com.carpark.android.ui.theme.Gray700
import com.carpark.android.ui.theme.Gray900
import com.carpark.android.ui.theme.Green
import com.carpark.android.ui.theme.Red
import com.carpark.android.util.NavigationHelper
import java.text.NumberFormat
import java.util.Locale

private val numberFmt = NumberFormat.getNumberInstance(Locale.KOREA)

private fun formatTime(t: String): String {
    if (t.isBlank() || t.length < 4) return t
    return "${t.substring(0, 2)}:${t.substring(2)}"
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
    val isRealtime = dataMode == DataMode.REALTIME
    val statusColor = if (isRealtime) getStatusColor(lot) else Gray400

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = lot.parkingName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Gray900,
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
                    if (lot.parkingTypeName.isNotBlank()) {
                        Text(text = lot.parkingTypeName, fontSize = 12.sp, color = Gray500)
                    }
                }

                IconButton(onClick = onToggleSave, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painter = painterResource(
                            if (isSaved) R.drawable.ic_star_filled else R.drawable.ic_star_outline
                        ),
                        contentDescription = "save-lot",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "close-card",
                        tint = Gray400,
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
                    Text(text = "주차 가능", fontSize = 12.sp, color = Gray500)
                    Text(
                        text = "${lot.availableCount} / ${lot.totalCapacity}면",
                        fontSize = 13.sp,
                        color = Gray700,
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
                    Text(text = "총 주차면", fontSize = 12.sp, color = Gray500)
                    Text(
                        text = "${lot.totalCapacity}면",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Gray700,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (lot.address.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = Gray400, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text(text = lot.address, fontSize = 12.sp, color = Gray600, maxLines = 1)
                }
                Spacer(Modifier.height(4.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = Gray400, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(4.dp))
                Text(
                    text = "평일 ${formatTime(lot.weekdayStart)} ~ ${formatTime(lot.weekdayEnd)}",
                    fontSize = 12.sp,
                    color = Gray600,
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
                ) {
                    Icon(
                        painter = painterResource(
                            if (isSaved) R.drawable.ic_star_filled else R.drawable.ic_star_outline
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Unspecified,
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(text = if (isSaved) "저장됨" else "저장", fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = {
                        NavigationHelper.openNavigation(
                            context = context,
                            lat = lot.latDouble,
                            lng = lot.lngDouble,
                            name = lot.parkingName,
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(painter = painterResource(R.drawable.track_order), contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Unspecified)
                    Spacer(Modifier.size(4.dp))
                    Text(text = "길안내", fontSize = 13.sp)
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
