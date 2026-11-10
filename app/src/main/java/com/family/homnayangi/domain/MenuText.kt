package com.family.homnayangi.domain

import com.family.homnayangi.domain.model.MealTarget
import com.family.homnayangi.domain.model.WeatherClass
import java.time.DayOfWeek
import java.time.LocalDate

/** Các câu chữ hiển thị / chia sẻ, tách riêng để test. */
object MenuText {

    /** "T3", "CN"… */
    fun weekday(date: LocalDate): String =
        if (date.dayOfWeek == DayOfWeek.SUNDAY) "CN" else "T${date.dayOfWeek.value + 1}"

    fun dayMonth(date: LocalDate): String = "%02d/%02d".format(date.dayOfMonth, date.monthValue)

    /** "nay", "mai", "qua" hoặc "T5 12/11". */
    fun relativeDay(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "nay"
        today.plusDays(1) -> "mai"
        today.minusDays(1) -> "qua"
        else -> "${weekday(date)} ${dayMonth(date)}"
    }

    /** "Trưa nay ăn gì?", "Trưa mai ăn gì?", "Tối T5 12/11 ăn gì?" */
    fun question(target: MealTarget, today: LocalDate): String =
        "${target.mealType.label} ${relativeDay(target.date, today)} ăn gì?"

    /** "Trưa nay mẹ nấu", "Tối mai mẹ nấu", "Trưa T5 12/11 mẹ nấu". */
    fun shareHeading(target: MealTarget, today: LocalDate): String =
        "${target.mealType.label} ${relativeDay(target.date, today)} mẹ nấu"

    /** Dòng ngày nhỏ dưới tiêu đề: "Thứ Ba, 10/11". */
    fun longDate(date: LocalDate): String {
        val name = when (date.dayOfWeek) {
            DayOfWeek.SUNDAY -> "Chủ nhật"
            else -> "Thứ " + listOf("Hai", "Ba", "Tư", "Năm", "Sáu", "Bảy")[date.dayOfWeek.value - 1]
        }
        return "$name, ${dayMonth(date)}"
    }

    /** Mẫu chữ gửi Zalo theo F25. */
    fun shareMessage(target: MealTarget, today: LocalDate, dishNames: List<String>): String = buildString {
        append(shareHeading(target, today))
        append(" (").append(weekday(target.date)).append(", ").append(dayMonth(target.date)).append("):")
        dishNames.forEach { append("\n- ").append(it) }
    }

    /** F19 — dòng nhỏ trên màn hình Hôm nay. [maxTempC] = null khi chưa có dữ liệu thời tiết. */
    fun weatherLine(weather: WeatherClass, maxTempC: Int?): String {
        val temp = maxTempC?.let { " ${it}°C" } ?: ""
        return when (weather) {
            WeatherClass.NONG -> "Trời nóng$temp — ưu tiên canh mát"
            WeatherClass.MAT_LANH -> "Trời mát lạnh$temp — ưu tiên món nóng, món kho"
            WeatherClass.BINH_THUONG -> "Trời dễ chịu$temp"
        }
    }
}
