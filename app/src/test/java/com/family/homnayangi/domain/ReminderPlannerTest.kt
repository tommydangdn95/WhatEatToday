package com.family.homnayangi.domain

import com.family.homnayangi.domain.model.MealTarget
import com.family.homnayangi.domain.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderPlannerTest {
    private val day = LocalDate.of(2026, 11, 10)

    @Test
    fun `gio bao ke tiep la hom nay neu chua qua, khong thi ngay mai`() {
        val t = LocalTime.of(20, 0)
        assertEquals(day.atTime(20, 0), ReminderPlanner.nextTrigger(t, day.atTime(19, 59)))
        // Đúng 20:00 (báo thức vừa chạy xong tự đặt lại) → ngày mai, không báo 2 lần.
        assertEquals(day.plusDays(1).atTime(20, 0), ReminderPlanner.nextTrigger(t, day.atTime(20, 0)))
        assertEquals(day.plusDays(1).atTime(6, 0), ReminderPlanner.nextTrigger(LocalTime.of(6, 0), LocalDateTime.of(day, LocalTime.of(23, 30))))
    }

    @Test
    fun `moi moc nhac dung bua theo F17`() {
        assertEquals(MealTarget(day.plusDays(1), MealType.TRUA), ReminderPlanner.targetFor(ReminderKind.LUNCH_PREV_DAY, day))
        assertEquals(MealTarget(day, MealType.TOI), ReminderPlanner.targetFor(ReminderKind.DINNER, day))
        assertEquals(MealTarget(day, MealType.TRUA), ReminderPlanner.targetFor(ReminderKind.LUNCH_MORNING, day))
    }

    @Test
    fun `cau chu thong bao`() {
        val goiY = listOf("Cá kho riềng", "Rau muống luộc")
        assertEquals(
            ReminderContent("Mẹ ơi, trưa mai nấu gì nhỉ?", "Gợi ý: Cá kho riềng, Rau muống luộc"),
            ReminderPlanner.content(ReminderKind.LUNCH_PREV_DAY, null, goiY),
        )
        assertEquals("Mẹ ơi, tối nay nấu gì nhỉ?", ReminderPlanner.content(ReminderKind.DINNER, null, goiY).title)
        assertEquals(
            ReminderContent("Trưa nay mẹ nấu", "Cá kho riềng, Rau muống luộc"),
            ReminderPlanner.content(ReminderKind.LUNCH_MORNING, goiY, emptyList()),
        )
        assertEquals("Bấm để chọn món", ReminderPlanner.content(ReminderKind.DINNER, null, emptyList()).body)
    }
}
