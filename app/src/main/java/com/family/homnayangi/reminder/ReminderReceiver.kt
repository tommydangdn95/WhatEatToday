package com.family.homnayangi.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.family.homnayangi.data.settings.SettingsRepository
import com.family.homnayangi.di.ApplicationScope
import com.family.homnayangi.domain.ReminderKind
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Báo thức tới giờ: hiện thông báo rồi đặt lần nhắc ngày hôm sau. */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var notifier: ReminderNotifier
    @Inject lateinit var scheduler: ReminderScheduler
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(EXTRA_KIND)
            ?.let { name -> ReminderKind.entries.firstOrNull { it.name == name } } ?: return
        val pending = goAsync()
        appScope.launch {
            try {
                val settings = settingsRepository.settings.first()
                if (settings.remindersEnabled) {
                    notifier.show(kind)
                    scheduler.schedule(kind, settings.reminderTime(kind))
                }
            } catch (e: Exception) {
                Log.e(ReminderScheduler.TAG, "Lỗi khi nhắc $kind", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_KIND = "kind"
    }
}

/** Khởi động lại máy, đổi giờ / múi giờ, cập nhật app → báo thức bị xoá, phải đặt lại. */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: ReminderScheduler
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        appScope.launch {
            try {
                scheduler.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
