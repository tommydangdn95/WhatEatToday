package com.family.homnayangi.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family.homnayangi.data.repo.DishRepository
import com.family.homnayangi.data.repo.MealRepository
import com.family.homnayangi.data.repo.PantryRepository
import com.family.homnayangi.data.settings.AppSettings
import com.family.homnayangi.data.settings.SettingsRepository
import com.family.homnayangi.domain.MealSuggestion
import com.family.homnayangi.domain.MenuText
import com.family.homnayangi.domain.SuggestionContext
import com.family.homnayangi.domain.SuggestionEngine
import com.family.homnayangi.domain.WeatherClassifier
import com.family.homnayangi.domain.model.DishInfo
import com.family.homnayangi.domain.model.MealRecord
import com.family.homnayangi.domain.model.MealSlot
import com.family.homnayangi.domain.model.MealTarget
import com.family.homnayangi.domain.model.MealType
import com.family.homnayangi.ui.MealSelection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Một thẻ món trên màn hình. [dish] = null khi kho không còn món loại này. */
data class DishCardUi(
    val slot: MealSlot,
    val dish: DishInfo?,
    val hasPantryIngredient: Boolean,
)

enum class TodayMode {
    /** Chưa chốt: hiện mâm gợi ý, đổi món, "Chọn mâm này". */
    SUGGESTING,
    /** Đã chốt: hiện mâm đã chốt, "Gửi cho cả nhà", "Chọn lại". */
    CONFIRMED,
}

data class TodayUiState(
    val target: MealTarget,
    val today: LocalDate,
    val loading: Boolean = true,
    val mode: TodayMode = TodayMode.SUGGESTING,
    /** Đang chọn lại một bữa đã chốt (hiện nút "Giữ mâm cũ"). */
    val rechoosing: Boolean = false,
    /** Mâm vừa được chép từ Lịch sử. */
    val copiedFromHistory: Boolean = false,
    val cards: List<DishCardUi> = emptyList(),
    val quickFilter: Boolean = false,
    val weatherLine: String = "",
    /** Các món Mẹ có thể chọn thêm vào mâm: chưa ẩn, chưa có trong mâm. */
    val addable: List<DishInfo> = emptyList(),
) {
    val title: String get() = MenuText.question(target, today)
    val dateLine: String get() = MenuText.longDate(target.date)
    val canConfirm: Boolean get() = cards.any { it.dish != null }

    fun shareMessage(): String =
        MenuText.shareMessage(target, today, cards.mapNotNull { it.dish?.name })
}

sealed interface TodayEvent {
    data object Confirmed : TodayEvent
    /** Bỏ món khỏi mâm; [previous] = mâm trước khi bỏ, để Hoàn tác. */
    data class Removed(val target: MealTarget, val previous: MealSuggestion, val dishName: String) : TodayEvent
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    dishRepository: DishRepository,
    mealRepository: MealRepository,
    pantryRepository: PantryRepository,
    private val settingsRepository: SettingsRepository,
    private val selection: MealSelection,
    private val engine: SuggestionEngine,
) : ViewModel() {

    private val dishRepo = dishRepository
    private val mealRepo = mealRepository

    private data class Inputs(
        val dishes: List<DishInfo>,
        val history: List<MealRecord>,
        val pantry: Set<String>,
        val settings: AppSettings,
    ) {
        val dishById: Map<String, DishInfo> = dishes.associateBy { it.id }
        fun confirmed(target: MealTarget): MealRecord? =
            history.firstOrNull { it.date == target.date && it.mealType == target.mealType }
    }

    private val inputs = MutableStateFlow<Inputs?>(null)
    /** Mâm gợi ý (chưa chốt) của từng bữa, giữ lại khi Mẹ chuyển qua lại giữa các bữa. */
    private val suggestions = MutableStateFlow<Map<MealTarget, MealSuggestion>>(emptyMap())
    private val rechoosing = MutableStateFlow<Set<MealTarget>>(emptySet())
    private val copiedTargets = MutableStateFlow<Set<MealTarget>>(emptySet())
    /** Bộ lọc nhanh đổi ngay trên màn hình, không phải chờ DataStore ghi xong. */
    private val quickOverride = MutableStateFlow<Boolean?>(null)
    /** Món đã bị đổi trong phiên, theo từng bữa (F03: không lặp món vừa bị đổi). */
    private val rejected = HashMap<MealTarget, MutableSet<String>>()

    private val events = Channel<TodayEvent>(Channel.BUFFERED)
    val eventFlow: Flow<TodayEvent> = events.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(
                dishRepository.dishes,
                mealRepository.history,
                pantryRepository.ingredientIds,
                settingsRepository.settings,
            ) { d, h, p, s -> Inputs(d, h, p, s) }.collect { inputs.value = it }
        }
        // Bảo đảm bữa đang xem luôn có mâm (gợi ý mới, hoặc mâm chép từ Lịch sử).
        viewModelScope.launch {
            combine(inputs.filterNotNull(), selection.target, selection.copied, rechoosing) { i, t, _, _ -> i to t }
                .collect { (i, t) -> ensureMeal(i, t) }
        }
    }

    val state: StateFlow<TodayUiState> = combine(
        combine(inputs, selection.target, suggestions, rechoosing, copiedTargets) { a, b, c, d, e -> Quint(a, b, c, d, e) },
        quickOverride,
    ) { (i, target, sugg, re, copied), quick ->
        buildState(i, target, sugg, re, copied, quick)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        TodayUiState(selection.target.value, LocalDate.now()),
    )

    private data class Quint(
        val a: Inputs?, val b: MealTarget, val c: Map<MealTarget, MealSuggestion>,
        val d: Set<MealTarget>, val e: Set<MealTarget>,
    )

    // ---------------------------------------------------------------- dựng trạng thái

    private fun buildState(
        i: Inputs?,
        target: MealTarget,
        sugg: Map<MealTarget, MealSuggestion>,
        re: Set<MealTarget>,
        copied: Set<MealTarget>,
        quick: Boolean?,
    ): TodayUiState {
        val base = TodayUiState(
            target = target,
            today = LocalDate.now(),
            quickFilter = quick ?: i?.settings?.quickFilterUnder30 ?: false,
            weatherLine = MenuText.weatherLine(WeatherClassifier.byMonth(target.date), null),
        )
        if (i == null || i.dishes.isEmpty()) return base

        val record = i.confirmed(target)
        val showConfirmed = record != null && target !in re
        val meal = if (record != null && target !in re) {
            engine.arrange(i.dishes, contextFor(i, target), record.dishIds, fillMissing = false)
        } else {
            sugg[target] ?: return base
        }
        val cards = meal.picks.map { pick ->
            // Lấy bản mới nhất của món (vd vừa bấm tim) thay vì bản lúc gợi ý.
            val dish = pick.dish?.dish?.id?.let { i.dishById[it] }
            DishCardUi(pick.slot, dish, dish?.mainIngredients?.any { it.id in i.pantry } == true)
        }
        return base.copy(
            loading = false,
            mode = if (showConfirmed) TodayMode.CONFIRMED else TodayMode.SUGGESTING,
            rechoosing = record != null && target in re,
            copiedFromHistory = !showConfirmed && target in copied,
            cards = cards,
            addable = if (showConfirmed) emptyList() else {
                val inMeal = cards.mapNotNullTo(HashSet()) { it.dish?.id }
                i.dishes.filter { !it.isHidden && it.id !in inMeal }
            },
        )
    }

    private fun contextFor(i: Inputs, target: MealTarget) = SuggestionContext(
        target = target,
        weather = WeatherClassifier.byMonth(target.date),
        // Bỏ chính bữa này khỏi lịch sử, để "Chọn lại" không bị tránh lặp với chính nó.
        history = i.history.filterNot { it.date == target.date && it.mealType == target.mealType },
        pantry = i.pantry,
        repeatAvoidDays = i.settings.repeatAvoidDays,
        quickOnly = quickOverride.value ?: i.settings.quickFilterUnder30,
        minDishes = i.settings.minDishes,
    )

    private fun ensureMeal(i: Inputs, target: MealTarget) {
        if (i.dishes.isEmpty()) return
        val copied = selection.copied.value
        if (copied != null && copied.target == target) {
            selection.consumeCopied()
            val meal = engine.arrange(i.dishes, contextFor(i, target), copied.dishIds, fillMissing = true)
            suggestions.update { it + (target to meal) }
            if (copied.fromHistory) {
                copiedTargets.update { it + target }
                if (i.confirmed(target) != null) rechoosing.update { it + target }
            } else {
                // Mâm từ thông báo nhắc thay cho gợi ý cũ → coi như gợi ý mới.
                rejected.remove(target)
                copiedTargets.update { it - target }
            }
            return
        }
        val confirmed = i.confirmed(target) != null && target !in rechoosing.value
        if (!confirmed && target !in suggestions.value) {
            suggestions.update { it + (target to engine.suggest(i.dishes, contextFor(i, target))) }
        }
    }

    private inline fun withCurrent(block: (Inputs, MealTarget) -> Unit) {
        val i = inputs.value ?: return
        block(i, selection.target.value)
    }

    // ---------------------------------------------------------------- chọn bữa (F01)

    fun selectMealType(type: MealType) {
        val t = selection.target.value
        if (t.mealType != type) selection.select(t.copy(mealType = type))
    }

    fun shiftDay(days: Long) {
        val t = selection.target.value
        selection.select(t.copy(date = t.date.plusDays(days)))
    }

    fun onAppForeground() = selection.refreshDefault()

    // ---------------------------------------------------------------- đổi món (F03)

    fun swap(index: Int): Unit = withCurrent { i, target ->
        val current = suggestions.value[target] ?: return
        val set = rejected.getOrPut(target) { HashSet() }
        current.picks.getOrNull(index)?.dish?.dish?.id?.let { set += it }
        val next = engine.swap(i.dishes, contextFor(i, target), current, index, set)
        suggestions.update { it + (target to next) }
    }

    fun reshuffle(): Unit = withCurrent { i, target ->
        rejected.remove(target)
        copiedTargets.update { it - target }
        suggestions.update { it + (target to engine.suggest(i.dishes, contextFor(i, target))) }
    }

    /** F05 — chip "Món nhanh < 30 phút": gợi ý lại ngay theo bộ lọc mới. */
    fun setQuickFilter(enabled: Boolean) {
        quickOverride.value = enabled
        withCurrent { i, target ->
            if (i.confirmed(target) == null || target in rechoosing.value) {
                rejected.remove(target)
                suggestions.update { it + (target to engine.suggest(i.dishes, contextFor(i, target))) }
            }
        }
        // Các bữa khác chưa chốt sẽ gợi ý lại khi Mẹ chuyển sang.
        suggestions.update { m -> m.filterKeys { it == selection.target.value } }
        viewModelScope.launch { settingsRepository.setQuickFilter(enabled) }
    }

    // ---------------------------------------------------------------- Mẹ tự thêm / bỏ món

    fun addDish(dishId: String): Unit = withCurrent { i, target ->
        val current = suggestions.value[target] ?: return
        suggestions.update { it + (target to engine.addDish(i.dishes, contextFor(i, target), current, dishId)) }
    }

    fun remove(index: Int): Unit = withCurrent { _, target ->
        val current = suggestions.value[target] ?: return
        val name = current.picks.getOrNull(index)?.dish?.dish?.name ?: return
        val next = engine.remove(current, index)
        if (next == current) return
        suggestions.update { it + (target to next) }
        viewModelScope.launch { events.send(TodayEvent.Removed(target, current, name)) }
    }

    fun undoRemove(event: TodayEvent.Removed) {
        suggestions.update { it + (event.target to event.previous) }
    }

    // ---------------------------------------------------------------- chốt mâm

    fun confirm(): Unit = withCurrent { _, target ->
        val meal = suggestions.value[target] ?: return
        val ids = meal.dishIds
        if (ids.isEmpty()) return
        // Các bữa khác đang gợi ý trùng món vừa chốt → bỏ, để gợi ý lại theo F04.
        suggestions.update { m -> m.filter { (t, s) -> t == target || s.dishIds.none { it in ids } } }
        viewModelScope.launch {
            mealRepo.confirm(target, ids)
            rechoosing.update { it - target }
            copiedTargets.update { it - target }
            rejected.remove(target)
            events.send(TodayEvent.Confirmed)
        }
    }

    /** "Chọn lại": mở mâm đã chốt ra để đổi món; bắt đầu từ chính các món đã chốt. */
    fun rechoose(): Unit = withCurrent { i, target ->
        val record = i.confirmed(target) ?: return
        // Giữ đúng mâm Mẹ đã chốt (có thể ít hơn 4 món), không tự thêm.
        val meal = engine.arrange(i.dishes, contextFor(i, target), record.dishIds, fillMissing = false)
        suggestions.update { it + (target to meal) }
        rechoosing.update { it + target }
    }

    fun keepConfirmed(): Unit = withCurrent { _, target ->
        rechoosing.update { it - target }
        copiedTargets.update { it - target }
    }

    // ---------------------------------------------------------------- yêu thích (F09)

    fun toggleFavorite(dish: DishInfo) {
        viewModelScope.launch { dishRepo.setFavorite(dish.id, !dish.isFavorite) }
    }
}
