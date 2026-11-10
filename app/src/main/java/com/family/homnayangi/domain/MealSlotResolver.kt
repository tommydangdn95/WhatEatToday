package com.family.homnayangi.domain

import com.family.homnayangi.domain.model.MealTarget
import com.family.homnayangi.domain.model.MealType
import java.time.LocalDateTime

/**
 * F01 — xác định bữa mặc định theo giờ hiện tại:
 * 00:00–10:59 → trưa hôm nay · 11:00–19:59 → tối hôm nay · 20:00–23:59 → trưa ngày mai.
 */
object MealSlotResolver {
    fun resolve(now: LocalDateTime): MealTarget {
        val today = now.toLocalDate()
        return when (now.hour) {
            in 0..10 -> MealTarget(today, MealType.TRUA)
            in 11..19 -> MealTarget(today, MealType.TOI)
            else -> MealTarget(today.plusDays(1), MealType.TRUA)
        }
    }
}
