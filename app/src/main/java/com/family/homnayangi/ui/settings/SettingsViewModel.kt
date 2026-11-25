package com.family.homnayangi.ui.settings

import android.content.Context
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family.homnayangi.data.settings.AppSettings
import com.family.homnayangi.data.settings.SettingsRepository
import com.family.homnayangi.domain.MealSlotResolver
import com.family.homnayangi.domain.ReminderKind
import com.family.homnayangi.domain.model.MealType
import com.family.homnayangi.reminder.ReminderNotifier
import com.family.homnayangi.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

/** Trạng thái của máy ảnh hưởng tới nhắc nhở — đọc lại mỗi lần quay về màn hình. */
data class DeviceStatus(
    val notificationsEnabled: Boolean = true,
    val batteryOptimizationOff: Boolean = true,
    val exactAlarmAllowed: Boolean = true,
)

data class SettingsUiState(
    val loading: Boolean = true,
    val settings: AppSettings = AppSettings(),
    val device: DeviceStatus = DeviceStatus(),
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val scheduler: ReminderScheduler,
    private val notifier: ReminderNotifier,
) : ViewModel() {

    private val device = MutableStateFlow(readDevice())
    private val messages = Channel<String>(Channel.BUFFERED)
    val messageFlow: Flow<String> = messages.receiveAsFlow()

    val state: StateFlow<SettingsUiState> =
        combine(settingsRepository.settings, device) { s, d -> SettingsUiState(false, s, d) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun refreshDevice() { device.value = readDevice() }

    fun setRemindersEnabled(enabled: Boolean) = updateAndReschedule { settingsRepository.setRemindersEnabled(enabled) }

    fun setReminderTime(kind: ReminderKind, time: LocalTime) =
        updateAndReschedule { settingsRepository.setReminderTime(kind, time) }

    fun setServings(value: Int) { viewModelScope.launch { settingsRepository.setServings(value) } }
    fun setRepeatAvoidDays(value: Int) { viewModelScope.launch { settingsRepository.setRepeatAvoidDays(value.coerceIn(0, 14)) } }

    /** "Gửi thử thông báo": gửi đúng loại nhắc của bữa sắp tới, để Mẹ thấy thông báo thật trông thế nào. */
    fun sendTest() {
        viewModelScope.launch {
            val now = LocalDateTime.now()
            val next = MealSlotResolver.resolve(now)
            val kind = when {
                next.mealType == MealType.TOI -> ReminderKind.DINNER
                next.date.isAfter(now.toLocalDate()) -> ReminderKind.LUNCH_PREV_DAY
                else -> ReminderKind.LUNCH_MORNING
            }
            val shown = notifier.show(kind, LocalDate.now())
            messages.send(if (shown) "Đã gửi thông báo thử — kéo thanh thông báo xuống để xem" else "Thông báo của app đang bị tắt")
            refreshDevice()
        }
    }

    private fun updateAndReschedule(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            scheduler.rescheduleAll()
        }
    }

    private fun readDevice(): DeviceStatus {
        val power = context.getSystemService(PowerManager::class.java)
        return DeviceStatus(
            notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled(),
            batteryOptimizationOff = power.isIgnoringBatteryOptimizations(context.packageName),
            exactAlarmAllowed = scheduler.canScheduleExact(),
        )
    }
}
