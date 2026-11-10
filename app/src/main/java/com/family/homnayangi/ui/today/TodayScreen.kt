package com.family.homnayangi.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.family.homnayangi.domain.model.DishCategory
import com.family.homnayangi.domain.model.DishInfo
import com.family.homnayangi.domain.model.MealType
import com.family.homnayangi.share.ZaloSharer
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun TodayScreen(
    onOpenSettings: () -> Unit,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onAppForeground() }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is TodayEvent.Hidden -> scope.launch {
                    snackbar.currentSnackbarData?.dismiss()
                    // SPEC mục 2: Hoàn tác trong 5 giây.
                    val result = withTimeoutOrNull(5_000) {
                        snackbar.showSnackbar(
                            message = "Đã ẩn \"${event.dishName}\"",
                            actionLabel = "Hoàn tác",
                            duration = SnackbarDuration.Indefinite,
                        )
                    }
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoHide(event.dishId)
                }
                TodayEvent.Confirmed -> scope.launch {
                    snackbar.currentSnackbarData?.dismiss()
                    snackbar.showSnackbar("Đã chốt mâm. Bấm \"Gửi cho cả nhà\" để báo cả nhà nhé!")
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (!state.loading) {
                ActionBar(
                    state = state,
                    onConfirm = viewModel::confirm,
                    onKeep = viewModel::keepConfirmed,
                    onShare = { ZaloSharer.share(context, state.shareMessage()) },
                    onRechoose = viewModel::rechoose,
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Header(state, viewModel::shiftDay, viewModel::selectMealType, onOpenSettings) }

            if (state.loading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                return@LazyColumn
            }

            item { StatusRow(state, viewModel::setQuickFilter) }

            itemsIndexed(state.cards, key = { i, c -> "$i-${c.dish?.id}" }) { index, card ->
                DishCard(
                    card = card,
                    editable = state.mode == TodayMode.SUGGESTING,
                    onSwap = { viewModel.swap(index) },
                    onHide = { viewModel.hide(index) },
                    onFavorite = { card.dish?.let(viewModel::toggleFavorite) },
                )
            }

            if (state.mode == TodayMode.SUGGESTING) {
                item {
                    OutlinedButton(
                        onClick = viewModel::reshuffle,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Đổi cả mâm", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------- tiêu đề, chọn bữa / ngày

@Composable
private fun Header(
    state: TodayUiState,
    onShiftDay: (Long) -> Unit,
    onMealType: (MealType) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Filled.Settings, contentDescription = "Cài đặt", modifier = Modifier.size(30.dp))
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onShiftDay(-1) }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Ngày trước", modifier = Modifier.size(36.dp))
            }
            Text(
                text = state.dateLine,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onShiftDay(1) }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Ngày sau", modifier = Modifier.size(36.dp))
            }
        }

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            MealType.entries.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = state.target.mealType == type,
                    onClick = { onMealType(type) },
                    shape = SegmentedButtonDefaults.itemShape(index, MealType.entries.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.heightIn(min = 56.dp),
                ) {
                    Text("Bữa ${type.label.lowercase()}", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun StatusRow(state: TodayUiState, onQuickFilter: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(state.weatherLine, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        val banner = when {
            state.mode == TodayMode.CONFIRMED -> "Mẹ đã chốt mâm này"
            state.copiedFromHistory -> "Mâm chép từ Lịch sử — bấm \"Chọn mâm này\" để chốt"
            state.rechoosing -> "Đang chọn lại mâm đã chốt"
            else -> null
        }
        if (banner != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (state.mode == TodayMode.CONFIRMED) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(banner, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (state.mode == TodayMode.SUGGESTING) {
            FilterChip(
                selected = state.quickFilter,
                onClick = { onQuickFilter(!state.quickFilter) },
                label = { Text("Món nhanh < 30 phút", style = MaterialTheme.typography.labelMedium) },
                leadingIcon = if (state.quickFilter) {
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

// -------------------------------------------------------------------- thẻ món

@Composable
private fun DishCard(
    card: DishCardUi,
    editable: Boolean,
    onSwap: () -> Unit,
    onHide: () -> Unit,
    onFavorite: () -> Unit,
) {
    val dish = card.dish
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryBadge(dish?.category)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = dish?.name ?: "Chưa có món loại này",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (dish != null) DishTags(dish, card.hasPantryIngredient)
                }
                if (dish != null) FavoriteButton(dish, onFavorite)
            }
            if (editable && dish != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(
                        onClick = onSwap,
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Đổi món", style = MaterialTheme.typography.labelLarge)
                    }
                    TextButton(onClick = onHide, modifier = Modifier.heightIn(min = 56.dp)) {
                        Text(
                            "Nhà không ăn",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DishTags(dish: DishInfo, hasPantry: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Nhãn loại món đang nằm trên ô màu bên trái; khi có ảnh món thật sẽ hiện thêm nhãn loại ở đây.
        if (dish.imageUri != null) {
            Tag(dish.category.label, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (hasPantry) Tag("Có sẵn đồ", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun Tag(text: String, bg: Color, fg: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        modifier = Modifier.background(bg, MaterialTheme.shapes.small).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun FavoriteButton(dish: DishInfo, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(56.dp)) {
        Icon(
            imageVector = if (dish.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = if (dish.isFavorite) "Bỏ yêu thích ${dish.name}" else "Yêu thích ${dish.name}",
            tint = if (dish.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp),
        )
    }
}

/** Tạm thời thay cho ảnh món (ảnh theo loại món sẽ thêm sau). */
@Composable
fun CategoryBadge(category: DishCategory?, size: Int = 64) {
    val (bg, fg) = when (category) {
        DishCategory.MAN -> Color(0xFF8A3A00) to Color.White
        DishCategory.XAO -> Color(0xFF6D4C00) to Color.White
        DishCategory.RAU -> Color(0xFF1E5E20) to Color.White
        DishCategory.CANH -> Color(0xFF0B4F6C) to Color.White
        null -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier.size(size.dp).background(bg, MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center,
    ) {
        Text(category?.label ?: "?", color = fg, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
    }
}

// -------------------------------------------------------------------- nút chính dưới cùng

@Composable
private fun ActionBar(
    state: TodayUiState,
    onConfirm: () -> Unit,
    onKeep: () -> Unit,
    onShare: () -> Unit,
    onRechoose: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (state.mode) {
                TodayMode.SUGGESTING -> {
                    if (state.rechoosing) {
                        OutlinedButton(
                            onClick = onKeep,
                            modifier = Modifier.heightIn(min = 60.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                        ) {
                            Text("Giữ mâm cũ", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Button(
                        onClick = onConfirm,
                        enabled = state.canConfirm,
                        modifier = Modifier.weight(1f).heightIn(min = 60.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                    ) {
                        // Khi có thêm nút "Giữ mâm cũ" thì bỏ icon cho chữ không bị xuống dòng.
                        if (!state.rechoosing) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Chọn mâm này", style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                    }
                }
                TodayMode.CONFIRMED -> {
                    OutlinedButton(onClick = onRechoose, modifier = Modifier.heightIn(min = 60.dp)) {
                        Text("Chọn lại", style = MaterialTheme.typography.labelLarge)
                    }
                    Button(onClick = onShare, modifier = Modifier.weight(1f).heightIn(min = 60.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Gửi cho cả nhà", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
