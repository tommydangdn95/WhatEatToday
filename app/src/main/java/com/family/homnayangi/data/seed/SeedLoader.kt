package com.family.homnayangi.data.seed

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.family.homnayangi.data.db.AppDatabase
import com.family.homnayangi.data.db.Dish
import com.family.homnayangi.data.db.DishIngredient
import com.family.homnayangi.data.db.Ingredient
import com.family.homnayangi.data.settings.SettingsRepository
import com.family.homnayangi.domain.TextNormalizer
import com.family.homnayangi.domain.model.DishCategory
import com.family.homnayangi.domain.model.IngredientGroup
import com.family.homnayangi.domain.model.Region
import com.family.homnayangi.domain.model.Season
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class SeedFile(
    val version: Int,
    val ingredients: List<SeedIngredient>,
    val dishes: List<SeedDish>,
)

@Serializable
data class SeedIngredient(
    val id: String,
    val name: String,
    val group: IngredientGroup,
    val aliases: List<String> = emptyList(),
)

@Serializable
data class SeedDish(
    val id: String,
    val name: String,
    val category: DishCategory,
    val seasons: List<Season>,
    val region: Region,
    val cookTimeMin: Int,
    val mainIngredients: List<String>,
)

/** Kết quả đã chuyển sang entity Room, sẵn sàng insert. */
data class SeedEntities(
    val version: Int,
    val ingredients: List<Ingredient>,
    val dishes: List<Dish>,
    val links: List<DishIngredient>,
)

/** Phần thuần Kotlin: đọc chuỗi JSON → entity. Tách riêng để unit test được. */
object SeedParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): SeedEntities {
        val file = json.decodeFromString<SeedFile>(text)
        val ingredientIds = file.ingredients.mapTo(HashSet()) { it.id }
        val missing = file.dishes.flatMap { d -> d.mainIngredients.filter { it !in ingredientIds }.map { "${d.id}→$it" } }
        require(missing.isEmpty()) { "seed_data.json: nguyên liệu không tồn tại: $missing" }

        return SeedEntities(
            version = file.version,
            ingredients = file.ingredients.map {
                Ingredient(
                    id = it.id,
                    name = it.name,
                    nameNoAccent = TextNormalizer.noAccent((listOf(it.name) + it.aliases).joinToString(" ")),
                    group = it.group,
                    aliases = it.aliases,
                )
            },
            dishes = file.dishes.map {
                Dish(
                    id = it.id,
                    name = it.name,
                    nameNoAccent = TextNormalizer.noAccent(it.name),
                    category = it.category,
                    seasons = it.seasons,
                    region = it.region,
                    cookTimeMin = it.cookTimeMin,
                )
            },
            links = file.dishes.flatMap { d -> d.mainIngredients.map { DishIngredient(d.id, it, isMain = true) } },
        )
    }
}

/**
 * Nạp `assets/seed_data.json` vào Room lần chạy đầu, và mỗi khi `version` trong file tăng.
 * Chỉ THÊM món / nguyên liệu mới; không ghi đè bản ghi đã có (Mẹ có thể đã sửa, thích, ẩn).
 */
@Singleton
class SeedLoader @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsRepository,
) {
    suspend fun ensureSeeded() {
        try {
            val stored = settings.seedVersion()
            val text = context.assets.open(ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() }
            val seed = SeedParser.parse(text)
            if (seed.version <= stored && db.dishDao().count() > 0) return

            db.withTransaction {
                val dao = db.dishDao()
                dao.insertIngredientsIgnore(seed.ingredients)
                dao.insertDishesIgnore(seed.dishes)
                dao.insertDishIngredientsIgnore(seed.links)
            }
            settings.setSeedVersion(seed.version)
            Log.i(TAG, "Seed v${seed.version}: ${seed.dishes.size} món, ${seed.ingredients.size} nguyên liệu")
        } catch (e: Exception) {
            // Không crash app: các màn hình vẫn chạy với DB trống (SPEC mục 10).
            Log.e(TAG, "Không nạp được dữ liệu ban đầu", e)
        }
    }

    companion object {
        const val ASSET = "seed_data.json"
        private const val TAG = "SeedLoader"
    }
}
