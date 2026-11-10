package com.family.homnayangi.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Chữ to theo SPEC mục 2: nội dung ≥ 18sp, tiêu đề ≥ 24sp.
// Dùng sp nên vẫn phóng to theo cỡ chữ hệ thống của máy.
private fun style(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, letterSpacing = 0.sp)

val Typography = Typography(
    displayLarge = style(44, 52, FontWeight.Bold),
    displayMedium = style(38, 46, FontWeight.Bold),
    displaySmall = style(34, 42, FontWeight.Bold),
    headlineLarge = style(32, 40, FontWeight.Bold),
    headlineMedium = style(28, 36, FontWeight.Bold),
    headlineSmall = style(26, 34, FontWeight.Bold),
    titleLarge = style(24, 32, FontWeight.Bold),     // tiêu đề màn hình
    titleMedium = style(22, 30, FontWeight.Bold),    // tên món trên thẻ (≥ 20sp)
    titleSmall = style(20, 28, FontWeight.SemiBold),
    bodyLarge = style(20, 30),
    bodyMedium = style(18, 28),                       // nội dung tối thiểu
    bodySmall = style(18, 26),
    labelLarge = style(20, 28, FontWeight.SemiBold),  // chữ trên nút
    labelMedium = style(18, 26, FontWeight.SemiBold), // nhãn chip, thanh điều hướng
    labelSmall = style(18, 24, FontWeight.Medium),
)
