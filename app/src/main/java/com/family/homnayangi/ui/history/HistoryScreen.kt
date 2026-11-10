package com.family.homnayangi.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.family.homnayangi.domain.MenuText
import com.family.homnayangi.domain.model.MealType

@Composable
fun HistoryScreen(
    onCookAgain: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var opened by remember { mutableStateOf<HistoryMealUi?>(null) }

    when {
        state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Lịch sử",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() },
                )
            }
            if (state.isEmpty) {
                item {
                    Text(
                        "Chưa có bữa nào được chốt.\nChốt mâm ở tab Hôm nay thì bữa đó sẽ hiện ở đây.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            state.weeks.forEach { week ->
                item(key = week.title) {
                    Text(
                        week.title,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp).semantics { heading() },
                    )
                }
                items(week.days, key = { it.date.toEpochDay() }) { day ->
                    DayCard(day, onOpen = { opened = it })
                }
            }
        }
    }

    opened?.let { meal ->
        AlertDialog(
            onDismissRequest = { opened = null },
            title = {
                Text(
                    "${meal.record.mealType.label} ${MenuText.longDate(meal.record.date)}",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    meal.dishNames.forEach { Text("• $it", style = MaterialTheme.typography.bodyLarge) }
                    Text(
                        "\"Nấu lại\" sẽ chép mâm này sang bữa ${viewModel.currentTargetLabel().lowercase()}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cookAgain(meal.record)
                        opened = null
                        onCookAgain()
                    },
                    modifier = Modifier.heightIn(min = 56.dp),
                ) { Text("Nấu lại mâm này", style = MaterialTheme.typography.labelLarge) }
            },
            dismissButton = {
                TextButton(onClick = { opened = null }, modifier = Modifier.heightIn(min = 56.dp)) {
                    Text("Đóng", style = MaterialTheme.typography.labelLarge)
                }
            },
        )
    }
}

@Composable
private fun DayCard(day: HistoryDayUi, onOpen: (HistoryMealUi) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (day.isToday) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(
                day.label + if (day.isToday) " (hôm nay)" else "",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            MealRow(MealType.TRUA, day.lunch, onOpen)
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
            MealRow(MealType.TOI, day.dinner, onOpen)
        }
    }
}

@Composable
private fun MealRow(type: MealType, meal: HistoryMealUi?, onOpen: (HistoryMealUi) -> Unit) {
    val clickable = if (meal != null) Modifier.clickable(onClickLabel = "Xem lại mâm") { onOpen(meal) } else Modifier
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).then(clickable).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            type.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.width(60.dp),
        )
        Text(
            text = meal?.dishNames?.joinToString(", ") ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            color = if (meal == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f),
        )
    }
}
