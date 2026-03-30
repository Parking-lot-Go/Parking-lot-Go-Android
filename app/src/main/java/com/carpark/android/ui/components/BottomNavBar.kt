package com.carpark.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carpark.android.R
import com.carpark.android.data.model.TabId
import com.carpark.android.ui.theme.*

@Composable
fun BottomNavBar(
    activeTab: TabId,
    onTabChange: (TabId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val barBg = if (isDark) BarBgDark else BarBgLight
    val activeColor = if (isDark) BarActiveDark else BarActiveLight
    val inactiveColor = if (isDark) BarInactiveDark else BarInactiveLight

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(barBg)
            .navigationBarsPadding()
            .height(56.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavTabPainter(
            painter = painterResource(R.drawable.ic_around_me),
            label = "내 주변",
            active = activeTab == TabId.NEARBY,
            activeColor = activeColor,
            inactiveColor = inactiveColor,
            onClick = { onTabChange(TabId.NEARBY) },
        )
        NavTabPainter(
            painter = painterResource(R.drawable.ic_home),
            label = "홈",
            active = activeTab == TabId.HOME,
            activeColor = activeColor,
            inactiveColor = inactiveColor,
            onClick = { onTabChange(TabId.HOME) },
        )
        NavTabPainter(
            painter = painterResource(R.drawable.ic_bookmark),
            label = "저장",
            active = activeTab == TabId.SAVED,
            activeColor = activeColor,
            inactiveColor = inactiveColor,
            onClick = { onTabChange(TabId.SAVED) },
        )
        NavTabPainter(
            painter = painterResource(R.drawable.ic_my),
            label = "마이",
            active = activeTab == TabId.MY,
            activeColor = activeColor,
            inactiveColor = inactiveColor,
            onClick = { onTabChange(TabId.MY) },
        )
    }
}

@Composable
private fun NavTab(
    icon: ImageVector,
    label: String,
    active: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit,
) {
    val color = if (active) activeColor else inactiveColor

    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(text = label, fontSize = 11.sp, color = color)
    }
}

@Composable
private fun NavTabPainter(
    painter: androidx.compose.ui.graphics.painter.Painter,
    label: String,
    active: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit,
) {
    val color = if (active) activeColor else inactiveColor

    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(painter, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(text = label, fontSize = 11.sp, color = color)
    }
}
