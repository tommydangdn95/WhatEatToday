package com.family.homnayangi.ui.recipe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family.homnayangi.data.db.Recipe
import com.family.homnayangi.data.repo.DishRepository
import com.family.homnayangi.data.repo.RecipeRepository
import com.family.homnayangi.data.settings.SettingsRepository
import com.family.homnayangi.domain.RecipeText
import com.family.homnayangi.domain.model.DishInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipeUiState(
    val loading: Boolean = true,
    val dish: DishInfo? = null,
    val recipe: Recipe? = null,
    val servings: Int = 7,
) {
    /** Link mở khi bấm "Xem video": link Mẹ dán, không có thì trang tìm kiếm YouTube. */
    val videoUrl: String?
        get() = recipe?.videoUrl ?: dish?.let { RecipeText.youtubeSearchUrl(it.name) }
}

@HiltViewModel
class RecipeViewModel @Inject constructor(
    savedState: SavedStateHandle,
    dishRepository: DishRepository,
    private val recipeRepository: RecipeRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val dishId: String = checkNotNull(savedState[ARG_DISH_ID])

    val state: StateFlow<RecipeUiState> = combine(
        dishRepository.dishes.map { list -> list.firstOrNull { it.id == dishId } },
        recipeRepository.observe(dishId),
        settingsRepository.settings.map { it.servings },
    ) { dish, recipe, servings ->
        RecipeUiState(loading = false, dish = dish, recipe = recipe, servings = servings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipeUiState())

    /** Lưu công thức Mẹ sửa. Trả về false nếu chưa có bước nấu nào. */
    fun save(ingredientsInput: String, stepsInput: String, videoInput: String): Boolean {
        val steps = RecipeText.linesFromInput(stepsInput)
        if (steps.isEmpty()) return false
        val ingredients = RecipeText.linesFromInput(ingredientsInput)
        viewModelScope.launch {
            recipeRepository.save(dishId, ingredients, steps, RecipeText.normalizeVideoUrl(videoInput))
        }
        return true
    }

    companion object {
        const val ARG_DISH_ID = "dishId"
        const val ROUTE = "recipe/{$ARG_DISH_ID}"
        fun route(dishId: String) = "recipe/$dishId"
    }
}
