package com.family.homnayangi.ui.dishes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.family.homnayangi.domain.RecipeText
import com.family.homnayangi.domain.model.DishInfo
import com.family.homnayangi.ui.common.DishImage
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Tab "Món nhà" (F07, F09): danh sách món, tìm không dấu, Yêu thích, Đã ẩn. */
@Composable
fun DishesScreen(
    onOpenRecipe: (dishId: String) -> Unit,
    viewModel: DishesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                val message = when (event) {
                    is DishesEvent.Unfavorited -> "Đã bỏ yêu thích \"${event.dish.name}\""
                    is DishesEvent.Hidden -> "Đã ẩn \"${event.dish.name}\""
                }
                val result = withTimeoutOrNull(5_000) {
                    snackbar.showSnackbar(message, actionLabel = "Hoàn tác", duration = SnackbarDuration.Indefinite)
                }
                if (result == SnackbarResult.ActionPerformed) viewModel.undo(event)
            }
        }
    }

    Scaffold(contentWindowInsets = WindowInsets(0), snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "Món của nhà",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp).semantics { heading() },
                )
            }
            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    placeholder = { Text("Tìm món, vd: ca kho", style = MaterialTheme.typography.bodyLarge) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = if (state.query.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.setQuery("") }, modifier = Modifier.size(56.dp)) {
                                Icon(Icons.Filled.Clear, contentDescription = "Xoá chữ tìm")
                            }
                        }
                    } else null,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    DishFilter.entries.forEach { f ->
                        FilterChip(
                            selected = state.filter == f,
                            onClick = { viewModel.setFilter(f) },
                            label = { Text("${f.label} (${state.counts[f] ?: 0})", style = MaterialTheme.typography.labelMedium) },
                            leadingIcon = if (state.filter == f) {
                                { Icon(Icons.Filled.Check, contentDescription = null) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            ),
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }
            }

            when {
                state.loading -> item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                state.isEmpty -> item { EmptyText(state) }
                else -> state.groups.forEach { group ->
                    item(key = "h-${group.category}") {
                        Text(
                            "${group.category.label} (${group.dishes.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 8.dp).semantics { heading() },
                        )
                    }
                    items(group.dishes, key = { it.id }) { dish ->
                        DishRow(
                            dish = dish,
                            hiddenMode = state.filter == DishFilter.HIDDEN,
                            onOpen = { onOpenRecipe(dish.id) },
                            onFavorite = { viewModel.toggleFavorite(dish) },
                            onHide = { viewModel.hide(dish) },
                            onUnhide = { viewModel.unhide(dish) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyText(state: DishesUiState) {
    val text = when {
        state.query.isNotBlank() -> "Không tìm thấy món nào khớp \"${state.query}\"."
        state.filter == DishFilter.FAVORITE ->
            "Chưa có món yêu thích.\nMẹ bấm ♡ ở món nhà mình thích để lưu lại — app sẽ gợi ý các món này thường hơn."
        state.filter == DishFilter.HIDDEN -> "Chưa ẩn món nào."
        else -> "Chưa có món nào."
    }
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 16.dp),
    )
}

@Composable
private fun DishRow(
    dish: DishInfo,
    hiddenMode: Boolean,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    onHide: () -> Unit,
    onUnhide: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f).heightIn(min = 56.dp).clickable(onClickLabel = "Xem cách nấu", onClick = onOpen),
            ) {
                DishImage(dish, MaterialTheme.shapes.medium, Modifier.size(width = 88.dp, height = 66.dp))
                Column(Modifier.weight(1f)) {
                    Text(dish.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        RecipeText.cookTimeLabel(dish.cookTimeMin),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (hiddenMode) {
                OutlinedButton(onClick = onUnhide, modifier = Modifier.heightIn(min = 56.dp).padding(end = 6.dp)) {
                    Text("Bỏ ẩn", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                TextButton(onClick = onHide, modifier = Modifier.heightIn(min = 56.dp)) {
                    Text("Ẩn", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onFavorite, modifier = Modifier.size(56.dp)) {
                    Icon(
                        imageVector = if (dish.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (dish.isFavorite) "Bỏ yêu thích ${dish.name}" else "Yêu thích ${dish.name}",
                        tint = if (dish.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }
}
