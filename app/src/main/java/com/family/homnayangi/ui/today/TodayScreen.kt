package com.family.homnayangi.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.family.homnayangi.domain.SlotPick
import com.family.homnayangi.domain.model.DishCategory

@Composable
fun TodayScreen(viewModel: TodayViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Text(
            text = state.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        val suggestion = state.suggestion
        if (state.loading || suggestion == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(suggestion.picks) { index, pick ->
                DishCard(pick = pick, onSwap = { viewModel.swap(index) })
            }
            item {
                OutlinedButton(
                    onClick = viewModel::reshuffle,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Đổi cả mâm", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun DishCard(pick: SlotPick, onSwap: () -> Unit) {
    val scored = pick.dish
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryBadge(scored?.dish?.category)
            Column(Modifier.weight(1f)) {
                Text(pick.slot.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = scored?.dish?.name ?: "Chưa có món loại này",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (scored?.hasPantryIngredient == true) {
                    Text("Có sẵn đồ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (scored != null) {
                FilledTonalButton(onClick = onSwap, modifier = Modifier.heightIn(min = 56.dp)) {
                    Text("Đổi", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** Tạm thời thay cho ảnh món (ảnh theo loại món sẽ thêm sau). */
@Composable
private fun CategoryBadge(category: DishCategory?) {
    val (bg, fg) = when (category) {
        DishCategory.MAN -> Color(0xFF8A3A00) to Color.White
        DishCategory.XAO -> Color(0xFF6D4C00) to Color.White
        DishCategory.RAU -> Color(0xFF1E5E20) to Color.White
        DishCategory.CANH -> Color(0xFF0B4F6C) to Color.White
        null -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier.size(64.dp).background(bg, MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center,
    ) {
        Text(category?.label ?: "?", color = fg, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
    }
}
