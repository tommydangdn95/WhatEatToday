package com.family.homnayangi.data.db

import androidx.room.TypeConverter
import com.family.homnayangi.domain.model.Season
import kotlinx.serialization.json.Json
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun seasonsToString(value: List<Season>): String = value.joinToString(",") { it.name }

    @TypeConverter
    fun stringToSeasons(value: String): List<Season> =
        if (value.isBlank()) emptyList() else value.split(",").map { Season.valueOf(it) }

    @TypeConverter
    fun stringsToJson(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun jsonToStrings(value: String): List<String> = Json.decodeFromString(value)

    /** Lưu ngày dạng epochDay để so sánh / sắp xếp trong SQL. */
    @TypeConverter
    fun dateToLong(value: LocalDate): Long = value.toEpochDay()

    @TypeConverter
    fun longToDate(value: Long): LocalDate = LocalDate.ofEpochDay(value)
}
