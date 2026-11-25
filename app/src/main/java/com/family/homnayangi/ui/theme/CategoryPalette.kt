package com.family.homnayangi.ui.theme

import com.family.homnayangi.domain.model.DishCategory

/**
 * Màu ô theo loại món — dùng chung cho thẻ món trên màn hình và ảnh thực đơn gửi Zalo.
 * Chữ trắng trên các màu này đều đạt tương phản WCAG AA.
 */
object CategoryPalette {
    fun argb(category: DishCategory): Int = when (category) {
        DishCategory.MAN -> 0xFF8A3A00.toInt()
        DishCategory.XAO -> 0xFF6D4C00.toInt()
        DishCategory.RAU -> 0xFF1E5E20.toInt()
        DishCategory.CANH -> 0xFF0B4F6C.toInt()
    }
}
