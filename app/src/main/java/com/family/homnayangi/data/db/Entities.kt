package com.family.homnayangi.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.family.homnayangi.domain.model.DishCategory
import com.family.homnayangi.domain.model.IngredientGroup
import com.family.homnayangi.domain.model.MealType
import com.family.homnayangi.domain.model.Region
import com.family.homnayangi.domain.model.Season
import com.family.homnayangi.domain.model.WeatherClass
import java.time.LocalDate

@Entity
data class Dish(
    /** slug, vd "ca_kho_rieng"; món Mẹ thêm: "custom_<uuid>". */
    @PrimaryKey val id: String,
    val name: String,
    val nameNoAccent: String,
    val category: DishCategory,
    val seasons: List<Season>,
    val region: Region,
    val cookTimeMin: Int,
    /** Ảnh asset hoặc ảnh Mẹ chụp; null → dùng ảnh theo loại món. */
    val imageUri: String? = null,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val isCustom: Boolean = false,
    val note: String? = null,
)

@Entity
data class Ingredient(
    @PrimaryKey val id: String,
    val name: String,
    val nameNoAccent: String,
    val group: IngredientGroup,
    /** "ba rọi", "thịt xay"… */
    val aliases: List<String>,
)

@Entity(
    primaryKeys = ["dishId", "ingredientId"],
    indices = [Index("ingredientId")],
)
data class DishIngredient(
    val dishId: String,
    val ingredientId: String,
    val isMain: Boolean = true,
)

/** "Tủ lạnh có gì" — chỉ tên, KHÔNG lượng. */
@Entity
data class PantryItem(
    @PrimaryKey val ingredientId: String,
    val addedAt: Long,
)

/** Mỗi (date, mealType) chỉ có một bản ghi: chốt lại thì ghi đè. */
@Entity(indices = [Index(value = ["date", "mealType"], unique = true)])
data class MealLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val mealType: MealType,
    /** ≥ 4 món, theo thứ tự các slot. */
    val dishIds: List<String>,
    val confirmedAt: Long,
)

@Entity
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** null nếu Mẹ gõ tay. */
    val ingredientId: String?,
    val freeText: String?,
    val isBought: Boolean = false,
    val createdFromMealLogId: Long?,
)

@Entity
data class WeatherCache(
    @PrimaryKey val date: LocalDate,
    val maxTempC: Double,
    val rainMm: Double,
    val classification: WeatherClass,
    val fetchedAt: Long,
)

/** Món kèm nguyên liệu chính. */
data class DishWithIngredients(
    @Embedded val dish: Dish,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(DishIngredient::class, parentColumn = "dishId", entityColumn = "ingredientId"),
    )
    val ingredients: List<Ingredient>,
)
