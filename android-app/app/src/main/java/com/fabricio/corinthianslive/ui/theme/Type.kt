package com.fabricio.corinthianslive.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val BrandFont = FontFamily.SansSerif

private fun brandStyle(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    letterSpacing: Double = 0.0
) = TextStyle(
    fontFamily = BrandFont,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp
)

val Typography = Typography(
    displaySmall = brandStyle(40, 44, FontWeight.Black, -1.2),
    headlineLarge = brandStyle(34, 38, FontWeight.Black, -0.8),
    headlineMedium = brandStyle(29, 34, FontWeight.Black, -0.5),
    headlineSmall = brandStyle(24, 29, FontWeight.ExtraBold, -0.25),
    titleLarge = brandStyle(21, 27, FontWeight.ExtraBold, -0.15),
    titleMedium = brandStyle(17, 22, FontWeight.Bold),
    titleSmall = brandStyle(15, 20, FontWeight.Bold),
    bodyLarge = brandStyle(17, 25, FontWeight.Normal),
    bodyMedium = brandStyle(15, 22, FontWeight.Normal),
    bodySmall = brandStyle(13, 18, FontWeight.Normal),
    labelLarge = brandStyle(14, 19, FontWeight.Bold, 0.1),
    labelMedium = brandStyle(12, 17, FontWeight.Bold, 0.2),
    labelSmall = brandStyle(11, 15, FontWeight.Bold, 0.45)
)
