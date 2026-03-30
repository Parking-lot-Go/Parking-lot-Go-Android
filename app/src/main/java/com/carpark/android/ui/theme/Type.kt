package com.carpark.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    headlineLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Gray900),
    headlineMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Gray900),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Gray800),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Gray800),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, color = Gray700),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, color = Gray600),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, color = Gray500),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Gray700),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Gray500),
    labelSmall = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Gray400),
)
