package com.carpark.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carpark.android.ui.theme.BarActiveDark
import com.carpark.android.ui.theme.BarActiveLight
import com.carpark.android.ui.theme.BarHintDark
import com.carpark.android.ui.theme.BarHintLight
import com.carpark.android.ui.theme.BarInputBgDark
import com.carpark.android.ui.theme.BarInputBgLight
import com.carpark.android.ui.theme.BarInputTextDark
import com.carpark.android.ui.theme.BarInputTextLight
import com.carpark.android.ui.theme.Gray100
import com.carpark.android.ui.theme.Gray400
import com.carpark.android.ui.theme.Gray500
import com.carpark.android.ui.theme.Gray900
import com.carpark.android.ui.theme.isAppInDarkTheme

@Composable
fun SearchOverlay(
    open: Boolean,
    searchQuery: String,
    recentSearches: List<String>,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClose: () -> Unit,
    onRecentSearchClick: (String) -> Unit,
    onDeleteRecentSearch: (String) -> Unit,
    onDeleteAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!open) return

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val isDark = isAppInDarkTheme()

    val background = if (isDark) Gray900 else Color.White
    val cardBackground = if (isDark) BarInputBgDark else BarInputBgLight
    val textColor = if (isDark) BarInputTextDark else BarInputTextLight
    val hintColor = if (isDark) BarHintDark else BarHintLight
    val activeColor = if (isDark) BarActiveDark else BarActiveLight

    LaunchedEffect(open) {
        if (open) focusRequester.requestFocus()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // 검색바
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(48.dp)
                    .background(cardBackground, RoundedCornerShape(24.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "back",
                    tint = activeColor,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            focusManager.clearFocus()
                            onClose()
                        },
                )
                Spacer(modifier = Modifier.width(12.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        color = textColor,
                        fontSize = 15.sp,
                    ),
                    cursorBrush = SolidColor(activeColor),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            val trimmed = searchQuery.trim()
                            if (trimmed.isNotEmpty()) {
                                focusManager.clearFocus()
                                onSearch(trimmed)
                            }
                        },
                    ),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "장소·주소 검색",
                                color = hintColor,
                                fontSize = 15.sp,
                            )
                        }
                        innerTextField()
                    },
                )
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "clear",
                        tint = hintColor,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onQueryChange("") },
                    )
                }
            }

            HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.06f) else Gray100)

            // 최근 검색 헤더
            if (recentSearches.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "최근",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeColor,
                    )
                    Text(
                        text = "전체 삭제",
                        fontSize = 13.sp,
                        color = Gray500,
                        modifier = Modifier.clickable(onClick = onDeleteAll),
                    )
                }
            }

            // 검색 기록 목록
            if (recentSearches.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 60.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = Gray400,
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "최근 검색 기록이 없습니다",
                            fontSize = 14.sp,
                            color = Gray500,
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(recentSearches, key = { it }) { query ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    focusManager.clearFocus()
                                    onRecentSearchClick(query)
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = Gray400,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = query,
                                modifier = Modifier.weight(1f),
                                color = activeColor,
                                fontSize = 15.sp,
                            )
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "delete",
                                tint = Gray400,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onDeleteRecentSearch(query) },
                            )
                        }
                    }
                }
            }
        }
    }
}
