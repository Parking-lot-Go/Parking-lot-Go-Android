package com.carpark.android.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carpark.android.data.model.ParkingLot
import com.carpark.android.ui.theme.Amber
import com.carpark.android.ui.theme.Gray100
import com.carpark.android.ui.theme.Gray200
import com.carpark.android.ui.theme.Gray400
import com.carpark.android.ui.theme.Gray500
import com.carpark.android.ui.theme.Gray600
import com.carpark.android.ui.theme.Gray700
import com.carpark.android.ui.theme.Gray800
import com.carpark.android.ui.theme.Gray900
import com.carpark.android.ui.theme.Primary
import com.carpark.android.util.NavigationHelper
import java.text.NumberFormat
import java.util.Locale

private val numberFmt = NumberFormat.getNumberInstance(Locale.KOREA)

private fun formatTime(t: String): String {
    if (t.isBlank() || t.length < 4) return t
    return "${t.substring(0, 2)}:${t.substring(2)}"
}

@Composable
fun DetailScreen(
    lot: ParkingLot?,
    open: Boolean,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onClose: () -> Unit,
) {
    AnimatedVisibility(
        visible = open && lot != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
    ) {
        if (lot == null) return@AnimatedVisibility
        val context = LocalContext.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .systemBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Gray800)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "back",
                        tint = Color.White,
                    )
                }
                Text(
                    text = "주차장 상세",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Text(
                    text = lot.parkingName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gray900,
                )
                Spacer(Modifier.size(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (lot.parkingTypeName.isNotBlank()) {
                        Text(text = lot.parkingTypeName, fontSize = 13.sp, color = Gray500)
                    }
                    if (lot.totalCapacity > 0) {
                        Text(text = "총 ${lot.totalCapacity}면", fontSize = 13.sp, color = Gray500)
                    }
                }

                Spacer(Modifier.size(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ActionButton(
                        icon = Icons.Default.Share,
                        label = "공유",
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, "${lot.parkingName}\n${lot.address}")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "공유"))
                        },
                    )
                    ActionButton(
                        icon = Icons.Default.Star,
                        label = if (isSaved) "저장됨" else "저장",
                        primary = isSaved,
                        onClick = onToggleSave,
                    )
                    ActionButton(
                        icon = Icons.Default.Navigation,
                        label = "길안내",
                        primary = true,
                        onClick = {
                            NavigationHelper.openNavigation(
                                context = context,
                                lat = lot.latDouble,
                                lng = lot.lngDouble,
                                name = lot.parkingName,
                            )
                        },
                    )
                }

                Spacer(Modifier.size(20.dp))
                HorizontalDivider(color = Gray200)
                Spacer(Modifier.size(16.dp))

                if (lot.address.isNotBlank()) {
                    InfoRow(icon = Icons.Default.Place, text = lot.address)
                    Spacer(Modifier.size(8.dp))
                }

                if (lot.phone.isNotBlank()) {
                    InfoRow(
                        icon = Icons.Default.Phone,
                        text = lot.phone,
                        textColor = Primary,
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${lot.phone}")))
                        },
                    )
                    Spacer(Modifier.size(8.dp))
                }

                Spacer(Modifier.size(12.dp))
                SectionTitle("주차 요금")
                DetailRow(
                    label = "기본요금",
                    value = if (lot.basicFee > 0) "${lot.basicTime}분 ${numberFmt.format(lot.basicFee)}원" else "정보 없음",
                )
                if (lot.additionalFee > 0) {
                    DetailRow(
                        label = "추가요금",
                        value = "${lot.additionalTime}분 ${numberFmt.format(lot.additionalFee)}원",
                    )
                }
                if (lot.dayMaxFee > 0) {
                    DetailRow(label = "일 최대", value = "${numberFmt.format(lot.dayMaxFee)}원")
                }
                if (lot.monthlyFee > 0) {
                    DetailRow(label = "월주차", value = "${numberFmt.format(lot.monthlyFee)}원")
                }

                Spacer(Modifier.size(12.dp))
                SectionTitle("운영 시간")
                TimeRow("평일", lot.weekdayStart, lot.weekdayEnd)
                TimeRow("토요일", lot.weekendStart, lot.weekendEnd)
                TimeRow("공휴일", lot.holidayStart, lot.holidayEnd)

                if (lot.updatedAt.isNotBlank()) {
                    Spacer(Modifier.size(16.dp))
                    Text(
                        text = "${lot.updatedAt.take(10).replace("-", ".")} 기준 정보입니다.",
                        fontSize = 12.sp,
                        color = Gray400,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
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
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(22.dp), tint = if (label == "저장됨") Amber else Color.Unspecified)
        }
        Spacer(Modifier.size(4.dp))
        Text(text = label, fontSize = 12.sp, color = Gray600)
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    text: String,
    textColor: Color = Gray700,
    onClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    ) {
        Icon(icon, contentDescription = null, tint = Gray400, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text(text = text, fontSize = 14.sp, color = textColor)
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = Gray900,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, fontSize = 14.sp, color = Gray500)
        Text(text = value, fontSize = 14.sp, color = Gray900)
    }
}

@Composable
private fun TimeRow(label: String, start: String, end: String) {
    val s = formatTime(start)
    val e = formatTime(end)
    val value = when {
        s.isBlank() && e.isBlank() -> "운영 정보 없음"
        s == "00:00" && e == "23:59" -> "00:00 ~ 24:00"
        else -> "$s ~ $e"
    }
    DetailRow(label = label, value = value)
}
