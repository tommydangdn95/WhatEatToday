package com.family.homnayangi.data.repo

import com.family.homnayangi.data.db.DishDao
import com.family.homnayangi.data.db.DishWithIngredients
import com.family.homnayangi.data.db.MealLog
import com.family.homnayangi.data.db.MealLogDao
import com.family.homnayangi.data.db.PantryDao
import com.family.homnayangi.data.db.Recipe
import com.family.homnayangi.data.db.RecipeDao
import com.family.homnayangi.domain.model.DishInfo
import com.family.homnayangi.domain.model.IngredientRef
import com.family.homnayangi.domain.model.MealRecord
import com.family.homnayangi.domain.model.MealTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

fun DishWithIngredients.toDishInfo() = DishInfo(
    id = dish.id,
    name = dish.name,
    category = dish.category,
    seasons = dish.seasons.toSet(),
    region = dish.region,
    cookTimeMin = dish.cookTimeMin,
    mainIngredients = ingredients.map { IngredientRef(it.id, it.group) },
    imageUri = dish.imageUri,
    isFavorite = dish.isFavorite,
    isHidden = dish.isHidden,
)

fun MealLog.toRecord() = MealRecord(date, mealType, dishIds)

@Singleton
class DishRepository @Inject constructor(private val dao: DishDao) {
    /** Mọi món (kể cả món ẩn — bộ lọc ẩn nằm ở SuggestionEngine / màn hình). */
    val dishes: Flow<List<DishInfo>> = dao.observeAllWithIngredients().map { list -> list.map { it.toDishInfo() } }

    suspend fun setFavorite(id: String, favorite: Boolean) = dao.setFavorite(id, favorite)
    suspend fun setHidden(id: String, hidden: Boolean) = dao.setHidden(id, hidden)
}

@Singleton
class MealRepository @Inject constructor(private val dao: MealLogDao) {
    val history: Flow<List<MealRecord>> = dao.observeAll().map { list -> list.map { it.toRecord() } }

    fun observe(target: MealTarget): Flow<MealRecord?> =
        dao.observe(target.date, target.mealType).map { it?.toRecord() }

    /** Chốt mâm (tạo mới hoặc ghi đè). Trả về id MealLog. */
    suspend fun confirm(target: MealTarget, dishIds: List<String>, now: Long = System.currentTimeMillis()): Long =
        dao.upsertByMeal(MealLog(date = target.date, mealType = target.mealType, dishIds = dishIds, confirmedAt = now))
}

@Singleton
class PantryRepository @Inject constructor(private val dao: PantryDao) {
    val ingredientIds: Flow<Set<String>> = dao.observeIds().map { it.toSet() }
}

@Singleton
class RecipeRepository @Inject constructor(private val dao: RecipeDao) {
    fun observe(dishId: String): Flow<Recipe?> = dao.observe(dishId)

    /** Mẹ sửa / thêm công thức → đánh dấu isEdited để bản cập nhật seed không ghi đè. */
    suspend fun save(dishId: String, ingredients: List<String>, steps: List<String>, videoUrl: String?) =
        dao.save(Recipe(dishId, ingredients, steps, videoUrl, isEdited = true))
}
