package com.family.homnayangi.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Khi đổi schema: tăng [version], thêm Migration và test migration (SPEC mục 10 — cài APK mới đè lên không mất dữ liệu).
 * Schema được export vào app/schemas/.
 */
@Database(
    entities = [
        Dish::class,
        Ingredient::class,
        DishIngredient::class,
        PantryItem::class,
        MealLog::class,
        ShoppingItem::class,
        WeatherCache::class,
        Recipe::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),   // thêm bảng Recipe (F10)
    ],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dishDao(): DishDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun pantryDao(): PantryDao
    abstract fun mealLogDao(): MealLogDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun weatherDao(): WeatherDao
    abstract fun recipeDao(): RecipeDao

    companion object {
        const val NAME = "homnayangi.db"
    }
}
