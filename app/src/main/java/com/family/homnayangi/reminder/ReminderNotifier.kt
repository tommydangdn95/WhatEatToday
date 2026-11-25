package com.family.homnayangi.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.family.homnayangi.MainActivity
import com.family.homnayangi.R
import com.family.homnayangi.data.repo.DishRepository
import com.family.homnayangi.data.repo.MealRepository
import com.family.homnayangi.data.repo.PantryRepository
import com.family.homnayangi.data.settings.SettingsRepository
import com.family.homnayangi.domain.ReminderKind
import com.family.homnayangi.domain.ReminderPlanner
import com.family.homnayangi.domain.SuggestionContext
import com.family.homnayangi.domain.SuggestionEngine
import com.family.homnayangi.domain.WeatherClassifier
import com.family.homnayangi.domain.model.MealTarget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** F17 — dựng và hiện thông báo nhắc. Bấm vào mở app đúng bữa, kèm mâm gợi ý trong thông báo. */
@Singleton
class ReminderNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dishRepository: DishRepository,
    private val mealRepository: MealRepository,
    private val pantryRepository: PantryRepository,
    private val settingsRepository: SettingsRepository,
    private val engine: SuggestionEngine,
) {
    /** Trả về false nếu thông báo của app đang bị tắt trong cài đặt máy. */
    suspend fun show(kind: ReminderKind, fireDate: LocalDate = LocalDate.now()): Boolean {
        ensureChannel()
        val target = ReminderPlanner.targetFor(kind, fireDate)
        val dishes = dishRepository.dishes.first()
        val byId = dishes.associateBy { it.id }
        val history = mealRepository.history.first()
        val record = history.firstOrNull { it.date == target.date && it.mealType == target.mealType }

        val suggestedIds: List<String>
        val content = if (record != null) {
            suggestedIds = emptyList()
            ReminderPlanner.content(kind, record.dishIds.mapNotNull { byId[it]?.name }, emptyList())
        } else {
            val settings = settingsRepository.settings.first()
            val ctx = SuggestionContext(
                target = target,
                weather = WeatherClassifier.byMonth(target.date),
                history = history,
                pantry = pantryRepository.ingredientIds.first(),
                repeatAvoidDays = settings.repeatAvoidDays,
                quickOnly = settings.quickFilterUnder30,
                minDishes = settings.minDishes,
            )
            suggestedIds = if (dishes.isEmpty()) emptyList() else engine.suggest(dishes, ctx).dishIds
            ReminderPlanner.content(kind, null, suggestedIds.mapNotNull { byId[it]?.name })
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(kind, target, suggestedIds))
            .build()

        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        try {
            manager.notify(kind.ordinal + NOTIFICATION_ID_BASE, notification)
        } catch (_: SecurityException) {
            return false // Android 13+ chưa cấp quyền thông báo.
        }
        return true
    }

    private fun openAppIntent(kind: ReminderKind, target: MealTarget, dishIds: List<String>): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_DATE, target.date.toEpochDay())
            .putExtra(EXTRA_MEAL_TYPE, target.mealType.name)
            .putExtra(EXTRA_DISH_IDS, dishIds.toTypedArray())
        return PendingIntent.getActivity(
            context, kind.ordinal, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(CHANNEL_ID, "Nhắc nấu ăn", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Nhắc Mẹ chọn món trưa, tối"
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "nhac_nau_an"
        private const val NOTIFICATION_ID_BASE = 100
        const val EXTRA_DATE = "date"
        const val EXTRA_MEAL_TYPE = "mealType"
        const val EXTRA_DISH_IDS = "dishIds"
    }
}
