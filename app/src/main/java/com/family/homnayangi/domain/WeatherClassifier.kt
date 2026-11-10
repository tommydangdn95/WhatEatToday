package com.family.homnayangi.domain

import com.family.homnayangi.domain.model.WeatherClass
import java.time.LocalDate

/** F19 — phân loại thời tiết của ngày. */
object WeatherClassifier {
    fun classify(maxTempC: Double, rainMm: Double, hotThresholdC: Int, coolThresholdC: Int, rainThresholdMm: Int): WeatherClass =
        when {
            maxTempC >= hotThresholdC -> WeatherClass.NONG
            maxTempC <= coolThresholdC || rainMm >= rainThresholdMm -> WeatherClass.MAT_LANH
            else -> WeatherClass.BINH_THUONG
        }

    /** Khi không có dữ liệu thời tiết (hoặc cache quá 3 ngày): tháng 4–8 nóng, tháng 11–2 mát lạnh. */
    fun byMonth(date: LocalDate): WeatherClass = when (date.monthValue) {
        in 4..8 -> WeatherClass.NONG
        11, 12, 1, 2 -> WeatherClass.MAT_LANH
        else -> WeatherClass.BINH_THUONG
    }
}
