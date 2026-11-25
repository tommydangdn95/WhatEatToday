package com.family.homnayangi.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.family.homnayangi.domain.ReminderKind
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/** Cài đặt của app (SPEC mục 5.3). Giờ nhắc lưu dạng số phút trong ngày. */
data class AppSettings(
    val servings: Int = 7,
    val minDishes: Int = 4,
    val repeatAvoidDays: Int = 4,
    val hotThresholdC: Int = 32,
    val coolThresholdC: Int = 25,
    val rainThresholdMm: Int = 10,
    val reminderLunchPrevDay: LocalTime = LocalTime.of(20, 0),
    val reminderDinner: LocalTime = LocalTime.of(13, 0),
    val reminderLunchMorning: LocalTime = LocalTime.of(6, 0),
    val remindersEnabled: Boolean = true,
    val quickFilterUnder30: Boolean = false,
) {
    fun reminderTime(kind: ReminderKind): LocalTime = when (kind) {
        ReminderKind.LUNCH_PREV_DAY -> reminderLunchPrevDay
        ReminderKind.DINNER -> reminderDinner
        ReminderKind.LUNCH_MORNING -> reminderLunchMorning
    }
}

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val servings = intPreferencesKey("servings")
        val minDishes = intPreferencesKey("minDishes")
        val repeatAvoidDays = intPreferencesKey("repeatAvoidDays")
        val hotThresholdC = intPreferencesKey("hotThresholdC")
        val coolThresholdC = intPreferencesKey("coolThresholdC")
        val rainThresholdMm = intPreferencesKey("rainThresholdMm")
        val reminderLunchPrevDay = intPreferencesKey("reminderLunchPrevDay")
        val reminderDinner = intPreferencesKey("reminderDinner")
        val reminderLunchMorning = intPreferencesKey("reminderLunchMorning")
        val remindersEnabled = booleanPreferencesKey("remindersEnabled")
        val quickFilterUnder30 = booleanPreferencesKey("quickFilterUnder30")
        val seedVersion = intPreferencesKey("seedVersion")
        val recipesVersion = intPreferencesKey("recipesVersion")
    }

    private val defaults = AppSettings()

    val settings: Flow<AppSettings> = dataStore.data.map { p ->
        AppSettings(
            servings = p[Keys.servings] ?: defaults.servings,
            minDishes = (p[Keys.minDishes] ?: defaults.minDishes).coerceAtLeast(4),
            repeatAvoidDays = p[Keys.repeatAvoidDays] ?: defaults.repeatAvoidDays,
            hotThresholdC = p[Keys.hotThresholdC] ?: defaults.hotThresholdC,
            coolThresholdC = p[Keys.coolThresholdC] ?: defaults.coolThresholdC,
            rainThresholdMm = p[Keys.rainThresholdMm] ?: defaults.rainThresholdMm,
            reminderLunchPrevDay = p[Keys.reminderLunchPrevDay]?.toTime() ?: defaults.reminderLunchPrevDay,
            reminderDinner = p[Keys.reminderDinner]?.toTime() ?: defaults.reminderDinner,
            reminderLunchMorning = p[Keys.reminderLunchMorning]?.toTime() ?: defaults.reminderLunchMorning,
            remindersEnabled = p[Keys.remindersEnabled] ?: defaults.remindersEnabled,
            quickFilterUnder30 = p[Keys.quickFilterUnder30] ?: defaults.quickFilterUnder30,
        )
    }

    suspend fun setQuickFilter(enabled: Boolean) = dataStore.edit { it[Keys.quickFilterUnder30] = enabled }
    suspend fun setMinDishes(value: Int) = dataStore.edit { it[Keys.minDishes] = value.coerceAtLeast(4) }
    suspend fun setRepeatAvoidDays(value: Int) = dataStore.edit { it[Keys.repeatAvoidDays] = value.coerceAtLeast(0) }
    suspend fun setRemindersEnabled(enabled: Boolean) = dataStore.edit { it[Keys.remindersEnabled] = enabled }
    suspend fun setServings(value: Int) = dataStore.edit { it[Keys.servings] = value.coerceIn(1, 30) }

    suspend fun setReminderTime(kind: ReminderKind, time: LocalTime) = dataStore.edit {
        val key = when (kind) {
            ReminderKind.LUNCH_PREV_DAY -> Keys.reminderLunchPrevDay
            ReminderKind.DINNER -> Keys.reminderDinner
            ReminderKind.LUNCH_MORNING -> Keys.reminderLunchMorning
        }
        it[key] = time.hour * 60 + time.minute
    }

    suspend fun seedVersion(): Int = dataStore.data.first()[Keys.seedVersion] ?: 0
    suspend fun setSeedVersion(version: Int) = dataStore.edit { it[Keys.seedVersion] = version }

    suspend fun recipesVersion(): Int = dataStore.data.first()[Keys.recipesVersion] ?: 0
    suspend fun setRecipesVersion(version: Int) = dataStore.edit { it[Keys.recipesVersion] = version }

    private fun Int.toTime(): LocalTime = LocalTime.of(this / 60, this % 60)
}
