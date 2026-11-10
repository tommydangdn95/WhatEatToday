package com.family.homnayangi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// Bảng màu sáng, tương phản cao (chữ trên nền đều đạt WCAG AA, phần lớn đạt AAA).
// Chỉ dùng giao diện sáng: dễ đọc nhất với Mẹ, và không phụ thuộc chế độ tối của máy.
private val Green = Color(0xFF1E5E20)        // trên trắng ≈ 8.3:1
private val GreenContainer = Color(0xFFD6EED3)
private val OnGreenContainer = Color(0xFF0B2E0C)
private val Clay = Color(0xFF8A3A00)         // trên trắng ≈ 7.9:1
private val ClayContainer = Color(0xFFFFDCC7)
private val OnClayContainer = Color(0xFF3A1600)
private val Berry = Color(0xFFA0153E)        // nút tim, cảnh báo
private val Paper = Color(0xFFFFFBF5)
private val Ink = Color(0xFF1A1A1A)
private val InkSoft = Color(0xFF45464F)      // chữ phụ trên nền Paper ≈ 9:1
private val Line = Color(0xFF75777F)

private val ColorScheme = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = GreenContainer,
    onPrimaryContainer = OnGreenContainer,
    secondary = Clay,
    onSecondary = Color.White,
    secondaryContainer = ClayContainer,
    onSecondaryContainer = OnClayContainer,
    tertiary = Berry,
    onTertiary = Color.White,
    error = Color(0xFFB3261E),
    onError = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEDE6DC),
    onSurfaceVariant = InkSoft,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBF4EA),
    surfaceContainer = Color(0xFFF5EEE4),
    surfaceContainerHigh = Color(0xFFEFE8DE),
    surfaceContainerHighest = Color(0xFFE9E2D8),
    outline = Line,
    outlineVariant = Color(0xFFC6C6CC),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
)

@Composable
fun HomNayAnGiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
