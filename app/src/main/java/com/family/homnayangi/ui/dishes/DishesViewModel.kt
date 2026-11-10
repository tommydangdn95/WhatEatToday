package com.family.homnayangi.ui.dishes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family.homnayangi.data.repo.DishRepository
import com.family.homnayangi.domain.TextNormalizer
import com.family.homnayangi.domain.model.DishCategory
import com.family.homnayangi.domain.model.DishInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DishFilter(val label: String) {
    ALL("Tất cả"), FAVORITE("Yêu thích"), HIDDEN("Đã ẩn")
}

data class DishGroup(val category: DishCategory, val dishes: List<DishInfo>)

data class DishesUiState(
    val loading: Boolean = true,
    val query: String = "",
    val filter: DishFilter = DishFilter.ALL,
    val groups: List<DishGroup> = emptyList(),
    val counts: Map<DishFilter, Int> = emptyMap(),
) {
    val isEmpty: Boolean get() = groups.isEmpty()
}

object DishListBuilder {
    private val categoryOrder = listOf(DishCategory.MAN, DishCategory.XAO, DishCategory.RAU, DishCategory.CANH)

    fun matches(dish: DishInfo, filter: DishFilter): Boolean = when (filter) {
        DishFilter.ALL -> !dish.isHidden
        DishFilter.FAVORITE -> dish.isFavorite && !dish.isHidden
        DishFilter.HIDDEN -> dish.isHidden
    }

    /** F07/F09 — lọc theo chip + tìm không dấu, nhóm theo loại món, tên A→Z. */
    fun build(dishes: List<DishInfo>, filter: DishFilter, query: String): List<DishGroup> {
        val filtered = dishes.filter { matches(it, filter) && TextNormalizer.matches(TextNormalizer.noAccent(it.name), query) }
        return categoryOrder.mapNotNull { cat ->
            filtered.filter { it.category == cat }
                .sortedBy { TextNormalizer.noAccent(it.name) }
                .takeIf { it.isNotEmpty() }
                ?.let { DishGroup(cat, it) }
        }
    }

    fun counts(dishes: List<DishInfo>): Map<DishFilter, Int> =
        DishFilter.entries.associateWith { f -> dishes.count { matches(it, f) } }
}

sealed interface DishesEvent {
    data class Unfavorited(val dish: DishInfo) : DishesEvent
    data class Hidden(val dish: DishInfo) : DishesEvent
}

@HiltViewModel
class DishesViewModel @Inject constructor(
    private val dishRepository: DishRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(DishFilter.ALL)
    private val events = Channel<DishesEvent>(Channel.BUFFERED)
    val eventFlow: Flow<DishesEvent> = events.receiveAsFlow()

    val state: StateFlow<DishesUiState> =
        combine(dishRepository.dishes, query, filter) { dishes, q, f ->
            DishesUiState(
                loading = false,
                query = q,
                filter = f,
                groups = DishListBuilder.build(dishes, f, q),
                counts = DishListBuilder.counts(dishes),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DishesUiState())

    fun setQuery(text: String) { query.value = text }
    fun setFilter(f: DishFilter) { filter.value = f }

    fun toggleFavorite(dish: DishInfo) {
        viewModelScope.launch {
            dishRepository.setFavorite(dish.id, !dish.isFavorite)
            // Bỏ yêu thích = xoá khỏi danh sách đã lưu → có Hoàn tác (SPEC mục 2).
            if (dish.isFavorite) events.send(DishesEvent.Unfavorited(dish))
        }
    }

    fun hide(dish: DishInfo) {
        viewModelScope.launch {
            dishRepository.setHidden(dish.id, true)
            events.send(DishesEvent.Hidden(dish))
        }
    }

    fun unhide(dish: DishInfo) {
        viewModelScope.launch { dishRepository.setHidden(dish.id, false) }
    }

    fun undo(event: DishesEvent) {
        viewModelScope.launch {
            when (event) {
                is DishesEvent.Unfavorited -> dishRepository.setFavorite(event.dish.id, true)
                is DishesEvent.Hidden -> dishRepository.setHidden(event.dish.id, false)
            }
        }
    }
}
