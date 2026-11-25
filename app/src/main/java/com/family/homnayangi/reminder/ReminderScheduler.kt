package com.family.homnayangi.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.family.homnayangi.data.settings.SettingsRepository
import com.family.homnayangi.domain.ReminderKind
import com.family.homnayangi.domain.ReminderPlanner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * F17 — đặt báo thức cho 3 mốc nhắc. Mỗi mốc một báo thức chính xác (`setExactAndAllowWhileIdle`);
 * báo xong thì [ReminderReceiver] tự đặt lần kế tiếp (ngày hôm sau).
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /** Đặt lại toàn bộ theo cài đặt hiện tại (mở app, đổi cài đặt, khởi động lại máy, đổi giờ). */
    suspend fun rescheduleAll() {
        val settings = settingsRepository.settings.first()
        for (kind in ReminderKind.entries) {
            if (settings.remindersEnabled) schedule(kind, settings.reminderTime(kind)) else cancel(kind)
        }
    }

    fun schedule(kind: ReminderKind, time: LocalTime, now: LocalDateTime = LocalDateTime.now()) {
        val at = ReminderPlanner.nextTrigger(time, now)
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pi = pendingIntent(kind)
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        } else {
            // Android 12+ khi chưa được cấp "Báo thức và lời nhắc": vẫn nhắc, có thể lệch vài phút.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        }
        Log.i(TAG, "Đặt nhắc $kind lúc $at")
    }

    fun cancel(kind: ReminderKind) = alarmManager.cancel(pendingIntent(kind))

    /** API 28–30 luôn được; API 31+ cần quyền "Báo thức và lời nhắc". */
    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun pendingIntent(kind: ReminderKind): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            kind.ordinal,
            Intent(context, ReminderReceiver::class.java).putExtra(ReminderReceiver.EXTRA_KIND, kind.name),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val TAG = "Reminder"
    }
}
