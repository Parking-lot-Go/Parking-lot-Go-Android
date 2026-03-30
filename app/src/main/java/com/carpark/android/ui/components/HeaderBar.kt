package com.carpark.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.onFocusChanged
import com.carpark.android.R
import com.carpark.android.data.model.DataMode
import com.carpark.android.ui.theme.*

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

    val inputBg = if (isDark) BarInputBgDark else BarInputBgLight
    val inputText = if (isDark) BarInputTextDark else BarInputTextLight
    val hint = if (isDark) BarHintDark else BarHintLight
    val active = if (isDark) BarActiveDark else BarActiveLight
    val inactive = if (isDark) BarInactiveDark else BarInactiveLight
    val chipBg = if (isDark) BarInputBgDark else BarInputBgLight
    val chipSelectedBg = if (isDark) Color(0xFF4A4A4A) else Color.White

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // Pill search bar
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
                textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, color = inputText),
                cursorBrush = SolidColor(active),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (searchQuery.isNotBlank()) {
                            focusManager.clearFocus()
                            onSearch(searchQuery.trim())
                        }
                    }
                ),
                decorationBox = { inner ->
                    if (searchQuery.isEmpty()) {
                        Text("주차장 검색", color = hint, fontSize = 15.sp)
                    }
                    inner()
                },
            )
            if (searchQuery.isNotEmpty()) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "초기화",
                    tint = hint,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onSearchChange("") },
                )
            } else {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = hint,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Mode toggle + region
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(20.dp))
                    .background(chipBg, RoundedCornerShape(20.dp))
                    .padding(3.dp),
            ) {
                ModeChip(
                    label = "비실시간",
                    selected = dataMode == DataMode.NOT_LINKED,
                    onClick = { onModeChange(DataMode.NOT_LINKED) },
                    selectedBg = chipSelectedBg,
                    activeColor = active,
                    inactiveColor = inactive,
                )
                ModeChip(
                    label = "실시간",
                    selected = dataMode == DataMode.REALTIME,
                    onClick = { onModeChange(DataMode.REALTIME) },
                    showDot = true,
                    selectedBg = chipSelectedBg,
                    activeColor = active,
                    inactiveColor = inactive,
                )
            }

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
                        Icons.Default.LocationOn,
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
            }
        }
    }
}

@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedBg: Color,
    activeColor: Color,
    inactiveColor: Color,
    showDot: Boolean = false,
) {
    val bgColor = if (selected) selectedBg else Color.Transparent
    val textColor = if (selected) activeColor else inactiveColor

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showDot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(if (selected) Red else inactiveColor, CircleShape),
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textColor)
    }
}
