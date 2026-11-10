package com.family.homnayangi.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family.homnayangi.data.repo.DishRepository
import com.family.homnayangi.data.repo.MealRepository
import com.family.homnayangi.domain.MenuText
import com.family.homnayangi.domain.model.DishInfo
import com.family.homnayangi.domain.model.MealRecord
import com.family.homnayangi.domain.model.MealType
import com.family.homnayangi.ui.MealSelection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class HistoryMealUi(val record: MealRecord, val dishNames: List<String>)

data class HistoryDayUi(
    val date: LocalDate,
    val label: String,
    val isToday: Boolean,
    val lunch: HistoryMealUi?,
    val dinner: HistoryMealUi?,
)

data class HistoryWeekUi(val title: String, val days: List<HistoryDayUi>)

data class HistoryUiState(
    val loading: Boolean = true,
    val weeks: List<HistoryWeekUi> = emptyList(),
    val isEmpty: Boolean = false,
)

object HistoryBuilder {
    private const val MAX_WEEKS = 52

    /** Chia lịch sử theo tuần (Thứ Hai → Chủ nhật), tuần mới nhất lên đầu. */
    fun build(history: List<MealRecord>, dishes: Map<String, DishInfo>, today: LocalDate): List<HistoryWeekUi> {
        val monday = { d: LocalDate -> d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
        val byKey = history.associateBy { it.date to it.mealType }
        val newest = maxOf(monday(today), history.maxOfOrNull { monday(it.date) } ?: monday(today))
        val oldest = history.minOfOrNull { monday(it.date) } ?: monday(today)
        val weekCount = (ChronoUnit.WEEKS.between(oldest, newest) + 1).coerceIn(1, MAX_WEEKS.toLong())

        fun meal(date: LocalDate, type: MealType) = byKey[date to type]?.let { r ->
            HistoryMealUi(r, r.dishIds.map { dishes[it]?.name ?: "(món đã xoá)" })
        }

        return (0 until weekCount).map { w ->
            val start = newest.minusWeeks(w)
            val end = start.plusDays(6)
            HistoryWeekUi(
                title = run {
                    val range = "${MenuText.dayMonth(start)} – ${MenuText.dayMonth(end)}"
                    when (start) {
                        monday(today) -> "Tuần này ($range)"
                        monday(today).minusWeeks(1) -> "Tuần trước ($range)"
                        monday(today).plusWeeks(1) -> "Tuần sau ($range)"
                        else -> "Tuần $range"
                    }
                },
                days = (0L..6L).map { start.plusDays(it) }.map { d ->
                    HistoryDayUi(d, MenuText.longDate(d), d == today, meal(d, MealType.TRUA), meal(d, MealType.TOI))
                },
            )
        }
    }
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    mealRepository: MealRepository,
    dishRepository: DishRepository,
    private val selection: MealSelection,
) : ViewModel() {

    val state: StateFlow<HistoryUiState> =
        combine(mealRepository.history, dishRepository.dishes.map { list -> list.associateBy { it.id } }) { h, d ->
            HistoryUiState(loading = false, weeks = HistoryBuilder.build(h, d, LocalDate.now()), isEmpty = h.isEmpty())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    /** Bữa đang chọn bên màn hình Hôm nay, vd "Tối nay". */
    fun currentTargetLabel(): String {
        val t = selection.target.value
        return "${t.mealType.label} ${MenuText.relativeDay(t.date, LocalDate.now())}"
    }

    /** "Nấu lại mâm này": chép sang bữa đang chọn bên Hôm nay. */
    fun cookAgain(record: MealRecord) = selection.copyToCurrent(record.dishIds)
}
