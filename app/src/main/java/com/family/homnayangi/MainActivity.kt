package com.family.homnayangi

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.content.ContextCompat
import com.family.homnayangi.domain.model.MealTarget
import com.family.homnayangi.domain.model.MealType
import com.family.homnayangi.reminder.ReminderNotifier
import com.family.homnayangi.ui.AppRoot
import com.family.homnayangi.ui.MealSelection
import com.family.homnayangi.ui.theme.HomNayAnGiTheme
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var selection: MealSelection

    /** Tăng mỗi lần mở từ thông báo nhắc → AppRoot chuyển về tab Hôm nay. */
    private val openTodaySignal = mutableIntStateOf(0)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* Mẹ từ chối thì Cài đặt sẽ nhắc. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // App chỉ dùng nền sáng → icon thanh trạng thái / điều hướng luôn màu tối cho dễ nhìn.
        val paper = Color.parseColor("#FFFBF5")
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(paper, paper),
            navigationBarStyle = SystemBarStyle.light(paper, paper),
        )
        if (savedInstanceState == null) handleReminderIntent(intent)
        askNotificationPermission()
        setContent {
            HomNayAnGiTheme {
                AppRoot(openTodaySignal = openTodaySignal.intValue)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleReminderIntent(intent)
    }

    /** F17 — bấm vào thông báo: mở tab Hôm nay, đúng bữa, đúng mâm đã gợi ý trong thông báo. */
    private fun handleReminderIntent(intent: Intent?) {
        intent ?: return
        if (!intent.hasExtra(ReminderNotifier.EXTRA_DATE)) return
        val date = LocalDate.ofEpochDay(intent.getLongExtra(ReminderNotifier.EXTRA_DATE, 0))
        val mealType = intent.getStringExtra(ReminderNotifier.EXTRA_MEAL_TYPE)
            ?.let { name -> MealType.entries.firstOrNull { it.name == name } } ?: return
        val dishIds = intent.getStringArrayExtra(ReminderNotifier.EXTRA_DISH_IDS)?.toList().orEmpty()
        selection.openFromReminder(MealTarget(date, mealType), dishIds)
        openTodaySignal.intValue++
        // Đã xử lý → xoay màn hình / mở lại không áp dụng lần nữa.
        intent.removeExtra(ReminderNotifier.EXTRA_DATE)
    }

    /** Android 13+ phải xin quyền thông báo; máy Mẹ (Android 10) không cần. */
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
