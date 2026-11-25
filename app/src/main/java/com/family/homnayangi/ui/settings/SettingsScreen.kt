package com.family.homnayangi.ui.settings

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.family.homnayangi.domain.ReminderKind
import java.time.LocalTime

/** Cài đặt: nhắc nhở (F17), hướng dẫn máy Vivo, số người, tránh lặp món. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var editingKind by rememberSaveable { mutableStateOf<ReminderKind?>(null) }

    // Quay lại từ cài đặt của máy (bật thông báo, tắt tối ưu pin) → đọc lại trạng thái.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshDevice() }
    LaunchedEffect(Unit) { viewModel.messageFlow.collect { snackbar.showSnackbar(it) } }

    Scaffold(contentWindowInsets = WindowInsets(0), snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                IconButton(onClick = onBack, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", modifier = Modifier.size(30.dp))
                }
                Text(
                    "Cài đặt",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() },
                )
            }

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val s = state.settings
                val d = state.device

                // ------------------------------------------------ Nhắc nhở
                Section("Nhắc nhở") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) {
                        Text("Nhắc Mẹ chọn món", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Switch(checked = s.remindersEnabled, onCheckedChange = viewModel::setRemindersEnabled)
                    }
                    if (s.remindersEnabled) {
                        ReminderKind.entries.forEach { kind ->
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                            ) {
                                Text(kind.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                OutlinedButton(
                                    onClick = { editingKind = kind },
                                    modifier = Modifier.heightIn(min = 56.dp).widthIn(min = 104.dp),
                                ) {
                                    Text(formatTime(s.reminderTime(kind)), style = MaterialTheme.typography.titleSmall)
                                }
                            }
                        }
                        if (!d.notificationsEnabled) {
                            Warning(
                                "Thông báo của app đang bị tắt nên sẽ không nhắc được.",
                                action = "Bật thông báo",
                                onAction = { openNotificationSettings(context) },
                            )
                        }
                        if (!d.exactAlarmAllowed) {
                            Warning(
                                "Máy chưa cho app đặt báo thức đúng giờ — nhắc có thể trễ vài phút.",
                                action = "Cho phép",
                                onAction = { openExactAlarmSettings(context) },
                            )
                        }
                        FilledTonalButton(
                            onClick = viewModel::sendTest,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        ) {
                            Icon(Icons.Filled.Notifications, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Gửi thử thông báo", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                // ------------------------------------------------ Vivo
                if (s.remindersEnabled) {
                    Section("Để nhắc đúng giờ trên máy Vivo") {
                        Text(
                            "Máy Vivo hay tự tắt app chạy nền, làm mất thông báo nhắc. Làm 2 bước dưới đây một lần là xong:",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Step(
                            number = 1,
                            text = "Tắt tối ưu pin cho app.",
                            done = d.batteryOptimizationOff,
                            doneText = "Đã tắt tối ưu pin",
                            action = "Tắt tối ưu pin",
                            onAction = { openBatteryOptimization(context) },
                        )
                        Step(
                            number = 2,
                            text = "Bật \"Tự khởi động\" cho app \"Hôm nay ăn gì\".\n" +
                                "Nếu nút không mở đúng chỗ: vào Cài đặt → Pin → Quản lý điện năng nền → \"Hôm nay ăn gì\" → Cho phép; " +
                                "hoặc i Manager → Quản lý ứng dụng → Tự khởi động.",
                            done = false,
                            doneText = "",
                            action = "Mở Tự khởi động",
                            onAction = { openAutoStart(context) },
                        )
                    }
                }

                // ------------------------------------------------ Mâm cơm
                Section("Mâm cơm") {
                    Stepper(
                        label = "Số người ăn",
                        value = s.servings,
                        valueText = "${s.servings} người",
                        range = 1..30,
                        onChange = viewModel::setServings,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Stepper(
                        label = "Không gợi ý lại món đã ăn trong",
                        value = s.repeatAvoidDays,
                        valueText = "${s.repeatAvoidDays} ngày",
                        range = 0..14,
                        onChange = viewModel::setRepeatAvoidDays,
                    )
                }

                Text(
                    "Hôm nay ăn gì · phiên bản ${appVersion(context)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                )
            }
        }
    }

    editingKind?.let { kind ->
        TimeDialog(
            title = kind.label,
            initial = state.settings.reminderTime(kind),
            onDismiss = { editingKind = null },
            onConfirm = { time ->
                viewModel.setReminderTime(kind, time)
                editingKind = null
            },
        )
    }
}

// ---------------------------------------------------------------- khối giao diện

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.semantics { heading() },
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
        }
    }
}

@Composable
private fun Warning(text: String, action: String, onAction: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Filled.Warning, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedButton(onClick = onAction, modifier = Modifier.heightIn(min = 56.dp)) {
                Text(action, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun Step(number: Int, text: String, done: Boolean, doneText: String, action: String, onAction: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$number. $text", style = MaterialTheme.typography.bodyMedium)
        if (done) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 56.dp)) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(doneText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            }
        } else {
            OutlinedButton(onClick = onAction, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(action, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun Stepper(label: String, value: Int, valueText: String, range: IntRange, onChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { onChange(value - 1) },
                enabled = value > range.first,
                modifier = Modifier.size(width = 72.dp, height = 56.dp),
            ) { Text("−", style = MaterialTheme.typography.titleLarge) }
            Text(
                valueText,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                onClick = { onChange(value + 1) },
                enabled = value < range.last,
                modifier = Modifier.size(width = 72.dp, height = 56.dp),
            ) { Text("+", style = MaterialTheme.typography.titleLarge) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(title: String, initial: LocalTime, onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    val picker = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = { TimePicker(state = picker) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(picker.hour, picker.minute)) }, modifier = Modifier.heightIn(min = 56.dp)) {
                Text("Lưu", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 56.dp)) {
                Text("Huỷ", style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}

// ---------------------------------------------------------------- mở cài đặt của máy

private fun formatTime(t: LocalTime) = "%02d:%02d".format(t.hour, t.minute)

private fun appVersion(context: Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
} catch (_: Exception) {
    "?"
}

private fun Context.tryStart(vararg intents: Intent): Boolean {
    for (intent in intents) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return true
        } catch (_: ActivityNotFoundException) {
        } catch (_: SecurityException) {
        }
    }
    return false
}

private fun appDetails(context: Context) =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

private fun openNotificationSettings(context: Context) {
    context.tryStart(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
        appDetails(context),
    )
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.tryStart(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
            appDetails(context),
        )
    }
}

@android.annotation.SuppressLint("BatteryLife") // App nội bộ gia đình, không lên Play: được phép hỏi thẳng.
private fun openBatteryOptimization(context: Context) {
    context.tryStart(
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")),
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        appDetails(context),
    )
}

/** Màn "Tự khởi động" của Vivo (tên lớp khác nhau theo bản Funtouch OS); không có thì mở trang thông tin app. */
private fun openAutoStart(context: Context) {
    context.tryStart(
        Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
        Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")),
        Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
        appDetails(context),
    )
}
