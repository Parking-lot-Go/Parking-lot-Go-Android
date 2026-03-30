package com.carpark.android.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carpark.android.R
import com.carpark.android.data.model.DataMode
import com.carpark.android.ui.theme.BarActiveDark
import com.carpark.android.ui.theme.BarActiveLight
import com.carpark.android.ui.theme.BarBgDark
import com.carpark.android.ui.theme.BarHintDark
import com.carpark.android.ui.theme.BarHintLight
import com.carpark.android.ui.theme.BarInactiveDark
import com.carpark.android.ui.theme.BarInactiveLight
import com.carpark.android.ui.theme.BarInputBgDark
import com.carpark.android.ui.theme.BarInputBgLight
import com.carpark.android.ui.theme.BarInputTextDark
import com.carpark.android.ui.theme.BarInputTextLight
import com.carpark.android.ui.theme.Red

@Composable
fun HeaderBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onSearchFocusChange: (Boolean) -> Unit,
    centerRegion: String,
    dataMode: DataMode,
    onModeChange: (DataMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val isDark = isSystemInDarkTheme()
    val isRealtime = dataMode == DataMode.REALTIME

    val inputBg = if (isDark) BarInputBgDark else BarInputBgLight
    val inputText = if (isDark) BarInputTextDark else BarInputTextLight
    val hint = if (isDark) BarHintDark else BarHintLight
    val active = if (isDark) BarActiveDark else BarActiveLight
    val inactive = if (isDark) BarInactiveDark else BarInactiveLight
    val chipBg = if (isDark) BarInputBgDark else BarInputBgLight
    val toggleBg = if (isDark) BarBgDark else Color.White

    val infiniteTransition = rememberInfiniteTransition(label = "realtime_indicator")
    val blinkingAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "realtime_indicator_alpha",
    )

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
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { onSearchFocusChange(it.isFocused) },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 15.sp,
                    color = inputText,
                ),
                cursorBrush = SolidColor(active),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (searchQuery.isNotBlank()) {
                            focusManager.clearFocus()
                            onSearch(searchQuery.trim())
                        }
                    },
                ),
                decorationBox = { inner ->
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "주차장 검색",
                            color = hint,
                            fontSize = 15.sp,
                        )
                    }
                    inner()
                },
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

            Row(
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(20.dp))
                    .background(toggleBg, RoundedCornerShape(20.dp))
                    .padding(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (isRealtime) {
                                Red.copy(alpha = blinkingAlpha)
                            } else {
                                inactive.copy(alpha = 0.45f)
                            },
                            shape = CircleShape,
                        ),
                )
                Text(
                    text = "실시간",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isRealtime) active else inactive,
                )
                Switch(
                    checked = isRealtime,
                    onCheckedChange = { enabled ->
                        onModeChange(if (enabled) DataMode.REALTIME else DataMode.NOT_LINKED)
                    },
                )
            }
        }
    }
}
