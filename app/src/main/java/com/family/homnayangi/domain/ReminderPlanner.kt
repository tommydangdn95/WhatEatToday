package com.family.homnayangi.domain

import com.family.homnayangi.domain.model.MealTarget
import com.family.homnayangi.domain.model.MealType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** 3 mốc nhắc của F17. */
enum class ReminderKind(val label: String) {
    /** 20:00 — nhắc chọn món trưa ngày mai. */
    LUNCH_PREV_DAY("Nhắc chọn món trưa mai"),
    /** 13:00 — nhắc chọn món tối nay. */
    DINNER("Nhắc chọn món tối nay"),
    /** 06:00 — nhắc lại món trưa hôm nay. */
    LUNCH_MORNING("Nhắc món trưa hôm nay"),
}

data class ReminderContent(val title: String, val body: String)

/** Thuần Kotlin: tính giờ báo thức kế tiếp, bữa cần nhắc và câu chữ thông báo (SPEC F17). */
object ReminderPlanner {

    /** Lần báo kế tiếp sau [now]: hôm nay lúc [time] nếu chưa qua, không thì ngày mai. */
    fun nextTrigger(time: LocalTime, now: LocalDateTime): LocalDateTime {
        val today = now.toLocalDate().atTime(time)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    /** Bữa mà thông báo [kind] báo vào ngày [fireDate] nói tới. */
    fun targetFor(kind: ReminderKind, fireDate: LocalDate): MealTarget = when (kind) {
        ReminderKind.LUNCH_PREV_DAY -> MealTarget(fireDate.plusDays(1), MealType.TRUA)
        ReminderKind.DINNER -> MealTarget(fireDate, MealType.TOI)
        ReminderKind.LUNCH_MORNING -> MealTarget(fireDate, MealType.TRUA)
    }

    /**
     * [confirmed] = tên các món đã chốt (null nếu bữa chưa chốt);
     * [suggested] = tên các món app gợi ý (dùng khi chưa chốt).
     */
    fun content(kind: ReminderKind, confirmed: List<String>?, suggested: List<String>): ReminderContent {
        val meal = when (kind) {
            ReminderKind.LUNCH_PREV_DAY -> "trưa mai"
            ReminderKind.DINNER -> "tối nay"
            ReminderKind.LUNCH_MORNING -> "trưa nay"
        }
        if (confirmed != null) {
            val heading = meal.replaceFirstChar { it.uppercase() }
            return ReminderContent("$heading mẹ nấu", confirmed.joinToString(", "))
        }
        val title = when (kind) {
            ReminderKind.LUNCH_PREV_DAY -> "Mẹ ơi, trưa mai nấu gì nhỉ?"
            ReminderKind.DINNER -> "Mẹ ơi, tối nay nấu gì nhỉ?"
            ReminderKind.LUNCH_MORNING -> "Mẹ ơi, trưa nay nấu gì nhỉ?"
        }
        val body = if (suggested.isEmpty()) "Bấm để chọn món" else "Gợi ý: " + suggested.joinToString(", ")
        return ReminderContent(title, body)
    }
}
