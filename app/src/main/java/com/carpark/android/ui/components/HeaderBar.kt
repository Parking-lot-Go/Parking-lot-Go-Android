package com.carpark.android.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carpark.android.R
import com.carpark.android.data.model.DataMode
import com.carpark.android.ui.theme.BarActiveDark
import com.carpark.android.ui.theme.BarActiveLight
import com.carpark.android.ui.theme.BarHintDark
import com.carpark.android.ui.theme.BarHintLight
import com.carpark.android.ui.theme.BarInactiveDark
import com.carpark.android.ui.theme.BarInactiveLight
import com.carpark.android.ui.theme.BarInputBgDark
import com.carpark.android.ui.theme.BarInputBgLight
import com.carpark.android.ui.theme.BarInputTextDark
import com.carpark.android.ui.theme.BarInputTextLight
import com.carpark.android.ui.theme.Gray100
import com.carpark.android.ui.theme.Gray200
import com.carpark.android.ui.theme.Gray300
import com.carpark.android.ui.theme.Gray500
import com.carpark.android.ui.theme.Gray600
import com.carpark.android.ui.theme.Gray800
import com.carpark.android.ui.theme.Primary
import com.carpark.android.ui.theme.Red
import com.carpark.android.ui.theme.isAppInDarkTheme

@Composable
fun HeaderBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    centerRegion: String,
    dataMode: DataMode,
    onModeChange: (DataMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isAppInDarkTheme()
    val isRealtime = dataMode == DataMode.REALTIME

    val inputBg = if (isDark) BarInputBgDark else BarInputBgLight
    val inputText = if (isDark) BarInputTextDark else BarInputTextLight
    val hint = if (isDark) BarHintDark else BarHintLight
    val active = if (isDark) BarActiveDark else BarActiveLight
    val inactive = if (isDark) BarInactiveDark else BarInactiveLight
    val chipBg = if (isDark) BarInputBgDark else BarInputBgLight

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .shadow(8.dp, RoundedCornerShape(24.dp))
                .background(inputBg, RoundedCornerShape(24.dp))
                .clickable(onClick = onSearchClick)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_search_icon),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = searchQuery.ifEmpty { "주차장 검색" },
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                color = if (searchQuery.isEmpty()) hint else inputText,
                maxLines = 1,
            )
            if (searchQuery.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "검색어 지우기",
                    tint = hint,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onSearchChange("") },
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = hint,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (centerRegion.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .shadow(4.dp, RoundedCornerShape(20.dp))
                        .background(chipBg, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = inactive,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = centerRegion,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = active,
                        maxLines = 1,
                    )
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.width(8.dp))

            RealtimeToggleChip(
                checked = isRealtime,
                isDark = isDark,
                activeColor = active,
                inactiveColor = inactive,
                onToggle = { enabled ->
                    onModeChange(if (enabled) DataMode.REALTIME else DataMode.NOT_LINKED)
                },
            )
        }
    }
}

@Composable
private fun RealtimeToggleChip(
    checked: Boolean,
    isDark: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onToggle: (Boolean) -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "realtime_indicator")
    val indicatorAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "realtime_indicator_alpha",
    )

    val chipBackground = when {
        checked && isDark -> Primary.copy(alpha = 0.18f)
        checked -> Primary.copy(alpha = 0.10f)
        isDark -> Gray800
        else -> Gray100
    }
    val chipBorderColor = when {
        checked && isDark -> Primary.copy(alpha = 0.42f)
        checked -> Primary.copy(alpha = 0.18f)
        isDark -> Color.White.copy(alpha = 0.10f)
        else -> Gray200
    }
    val sliderBackground = when {
        checked -> Primary
        isDark -> Gray600
        else -> Gray200
    }
    val thumbColor = if (checked) Color.White else if (isDark) Gray300 else Color.White
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 14.dp else 2.dp,
        animationSpec = tween(180),
        label = "realtime_thumb_offset",
    )

    Row(
        modifier = Modifier
            .shadow(if (isDark) 0.dp else 4.dp, RoundedCornerShape(18.dp))
            .background(chipBackground, RoundedCornerShape(18.dp))
            .border(1.dp, chipBorderColor, RoundedCornerShape(18.dp))
            .clickable { onToggle(!checked) }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(
                    color = if (checked) {
                        Red.copy(alpha = indicatorAlpha)
                    } else {
                        Gray500.copy(alpha = if (isDark) 0.75f else 0.55f)
                    },
                    shape = CircleShape,
                ),
        )
        Text(
            text = "실시간",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (checked) activeColor else inactiveColor,
        )
        Box(
            modifier = Modifier
                .size(width = 30.dp, height = 18.dp)
                .background(sliderBackground, RoundedCornerShape(999.dp)),
        ) {
            Box(
                modifier = Modifier
                    .padding(start = thumbOffset, top = 2.dp)
                    .size(14.dp)
                    .background(thumbColor, CircleShape),
            )
        }
    }
}
