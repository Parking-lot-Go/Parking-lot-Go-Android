package com.carpark.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
import com.carpark.android.ui.theme.Gray200
import com.carpark.android.ui.theme.Gray50
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
    val itemBackground = if (isDark) Gray900.copy(alpha = 0.6f) else Color.White
    val itemBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Gray200

    LaunchedEffect(open) {
        if (open) {
            focusRequester.requestFocus()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .shadow(8.dp, RoundedCornerShape(24.dp))
                    .background(cardBackground, RoundedCornerShape(24.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.Icon(
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
                                text = "주차장 검색",
                                color = hintColor,
                                fontSize = 15.sp,
                            )
                        }
                        innerTextField()
                    },
                )
                if (searchQuery.isNotEmpty()) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "clear-search-query",
                        tint = hintColor,
                        modifier = Modifier.clickable { onQueryChange("") },
                    )
                } else {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = hintColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "최근 검색",
                        color = activeColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "최신 10개까지 저장돼요",
                        color = hintColor,
                        fontSize = 12.sp,
                    )
                }

                if (recentSearches.isNotEmpty()) {
                    TextButton(onClick = onDeleteAll) {
                        Text("전체 삭제")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (recentSearches.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(cardBackground, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                tint = hintColor,
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "최근 검색어가 아직 없어요",
                            color = activeColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "검색하면 최근 기록이 여기에 쌓여요",
                            color = hintColor,
                            fontSize = 13.sp,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(recentSearches, key = { it }) { query ->
                        SearchHistoryRow(
                            query = query,
                            backgroundColor = itemBackground,
                            borderColor = itemBorder,
                            textColor = activeColor,
                            hintColor = hintColor,
                            onClick = {
                                focusManager.clearFocus()
                                onRecentSearchClick(query)
                            },
                            onDelete = { onDeleteRecentSearch(query) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHistoryRow(
    query: String,
    backgroundColor: Color,
    borderColor: Color,
    textColor: Color,
    hintColor: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(18.dp))
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Gray50, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                tint = Gray500,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = query,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick),
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
        )

        Spacer(modifier = Modifier.width(8.dp))

        androidx.compose.material3.Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "delete-recent-search",
            tint = hintColor,
            modifier = Modifier
                .size(20.dp)
                .clickable(onClick = onDelete),
        )
    }
}
