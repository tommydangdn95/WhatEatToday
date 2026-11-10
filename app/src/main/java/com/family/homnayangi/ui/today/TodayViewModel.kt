package com.family.homnayangi.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family.homnayangi.data.repo.DishRepository
import com.family.homnayangi.data.repo.MealRepository
import com.family.homnayangi.data.repo.PantryRepository
import com.family.homnayangi.data.settings.AppSettings
import com.family.homnayangi.data.settings.SettingsRepository
import com.family.homnayangi.domain.MealSlotResolver
import com.family.homnayangi.domain.SuggestionContext
import com.family.homnayangi.domain.SuggestionEngine
import com.family.homnayangi.domain.WeatherClassifier
import com.family.homnayangi.domain.model.DishInfo
import com.family.homnayangi.domain.model.MealRecord
import com.family.homnayangi.domain.MealSuggestion
import com.family.homnayangi.domain.model.MealTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class TodayUiState(
    val target: MealTarget,
    val loading: Boolean = true,
    val suggestion: MealSuggestion? = null,
) {
    val title: String get() = titleFor(target, LocalDate.now())

    companion object {
        /** "Trưa nay ăn gì?", "Tối nay ăn gì?", "Trưa mai ăn gì?", "Tối 12/11 ăn gì?" */
        fun titleFor(target: MealTarget, today: LocalDate): String {
            val meal = target.mealType.label
            val day = when (target.date) {
                today -> "nay"
                today.plusDays(1) -> "mai"
                today.minusDays(1) -> "qua"
                else -> "%02d/%02d".format(target.date.dayOfMonth, target.date.monthValue)
            }
            return "$meal $day ăn gì?"
        }
    }
}

/**
 * Bản đầu của màn hình Hôm nay: chỉ hiện mâm gợi ý + đổi món, để kiểm tra dữ liệu và thuật toán trên máy thật.
 * Chốt mâm, đổi bữa / đổi ngày, thời tiết thật sẽ làm ở bước tiếp theo (F01–F05 đầy đủ).
 */
@HiltViewModel
class TodayViewModel @Inject constructor(
    dishRepository: DishRepository,
    mealRepository: MealRepository,
    pantryRepository: PantryRepository,
    settingsRepository: SettingsRepository,
    private val engine: SuggestionEngine,
) : ViewModel() {

    private val _state = MutableStateFlow(TodayUiState(MealSlotResolver.resolve(LocalDateTime.now())))
    val state: StateFlow<TodayUiState> = _state.asStateFlow()

    private var dishes: List<DishInfo> = emptyList()
    private var context: SuggestionContext? = null
    private val rejected = HashSet<String>()

    init {
        viewModelScope.launch {
            combine(
                dishRepository.dishes,
                mealRepository.history,
                pantryRepository.ingredientIds,
                settingsRepository.settings,
            ) { d, h, p, s -> Inputs(d, h, p, s) }.collect { inputs ->
                dishes = inputs.dishes
                val target = _state.value.target
                context = SuggestionContext(
                    target = target,
                    weather = WeatherClassifier.byMonth(target.date),
                    history = inputs.history.filterNot { it.date == target.date && it.mealType == target.mealType },
                    pantry = inputs.pantry,
                    repeatAvoidDays = inputs.settings.repeatAvoidDays,
                    quickOnly = inputs.settings.quickFilterUnder30,
                    minDishes = inputs.settings.minDishes,
                )
                // Chỉ gợi ý mâm mới lần đầu; dữ liệu đổi sau đó không làm mâm đang xem tự nhảy.
                if (_state.value.suggestion == null && dishes.isNotEmpty()) reshuffle()
                else if (dishes.isEmpty()) _state.update { it.copy(loading = true) }
            }
        }
    }

    fun reshuffle() {
        val ctx = context ?: return
        rejected.clear()
        _state.update { it.copy(loading = false, suggestion = engine.suggest(dishes, ctx)) }
    }

    fun swap(index: Int) {
        val ctx = context ?: return
        val current = _state.value.suggestion ?: return
        current.picks[index].dish?.dish?.id?.let { rejected += it }
        _state.update { it.copy(suggestion = engine.swap(dishes, ctx, current, index, rejected)) }
    }

    private data class Inputs(
        val dishes: List<DishInfo>,
        val history: List<MealRecord>,
        val pantry: Set<String>,
        val settings: AppSettings,
    )
}
