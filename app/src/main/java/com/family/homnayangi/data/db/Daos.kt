package com.family.homnayangi.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.family.homnayangi.domain.model.MealType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DishDao {
    /** IGNORE: khi nạp lại seed, không ghi đè món Mẹ đã sửa / thích / ẩn. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDishesIgnore(dishes: List<Dish>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIngredientsIgnore(ingredients: List<Ingredient>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDishIngredientsIgnore(links: List<DishIngredient>)

    @Transaction
    @Query("SELECT * FROM Dish ORDER BY name")
    fun observeAllWithIngredients(): Flow<List<DishWithIngredients>>

    @Transaction
    @Query("SELECT * FROM Dish")
    suspend fun getAllWithIngredients(): List<DishWithIngredients>

    @Query("SELECT COUNT(*) FROM Dish")
    suspend fun count(): Int

    @Query("UPDATE Dish SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("UPDATE Dish SET isHidden = :hidden WHERE id = :id")
    suspend fun setHidden(id: String, hidden: Boolean)

    @Update
    suspend fun update(dish: Dish)
}

@Dao
interface IngredientDao {
    @Query("SELECT * FROM Ingredient ORDER BY name")
    fun observeAll(): Flow<List<Ingredient>>
}

@Dao
interface PantryDao {
    @Query("SELECT ingredientId FROM PantryItem")
    fun observeIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PantryItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PantryItem>)

    @Query("DELETE FROM PantryItem WHERE ingredientId = :ingredientId")
    suspend fun delete(ingredientId: String)

    @Query("SELECT * FROM PantryItem")
    suspend fun getAll(): List<PantryItem>

    @Query("DELETE FROM PantryItem")
    suspend fun clear()
}

@Dao
interface MealLogDao {
    @Query("SELECT * FROM MealLog ORDER BY date DESC, mealType DESC")
    fun observeAll(): Flow<List<MealLog>>

    @Query("SELECT * FROM MealLog WHERE date = :date AND mealType = :mealType LIMIT 1")
    fun observe(date: LocalDate, mealType: MealType): Flow<MealLog?>

    @Query("SELECT * FROM MealLog WHERE date = :date AND mealType = :mealType LIMIT 1")
    suspend fun get(date: LocalDate, mealType: MealType): MealLog?

    @Insert
    suspend fun insert(log: MealLog): Long

    @Update
    suspend fun update(log: MealLog)

    @Delete
    suspend fun delete(log: MealLog)

    /** Chốt mâm: tạo mới, hoặc ghi đè bữa đã chốt (giữ nguyên id). Trả về id của MealLog. */
    @Transaction
    suspend fun upsertByMeal(log: MealLog): Long {
        val existing = get(log.date, log.mealType)
        return if (existing == null) {
            insert(log.copy(id = 0))
        } else {
            update(log.copy(id = existing.id))
            existing.id
        }
    }
}

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM ShoppingItem ORDER BY isBought, id")
    fun observeAll(): Flow<List<ShoppingItem>>
}

@Dao
interface WeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<WeatherCache>)

    @Query("SELECT * FROM WeatherCache WHERE date = :date LIMIT 1")
    suspend fun get(date: LocalDate): WeatherCache?
}
