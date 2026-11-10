package com.family.homnayangi

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.family.homnayangi.data.db.AppDatabase
import com.family.homnayangi.data.db.Recipe
import com.family.homnayangi.domain.model.MealType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * SPEC mục 10: cài APK mới đè lên không được mất dữ liệu.
 * Mỗi lần tăng version DB, thêm một test nâng cấp từ version cũ ở đây.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migrate1To2_giuNguyenMonVaBuaDaChot() {
        // Dữ liệu ở version 1: một món Mẹ đã thích + một bữa đã chốt.
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                """INSERT INTO Dish (id, name, nameNoAccent, category, seasons, region, cookTimeMin, imageUri, isFavorite, isHidden, isCustom, note)
                   VALUES ('ca_kho_rieng', 'Cá kho riềng', 'ca kho rieng', 'MAN', 'LANH', 'BAC', 90, NULL, 1, 0, 0, NULL)"""
            )
            execSQL(
                """INSERT INTO MealLog (id, date, mealType, dishIds, confirmedAt)
                   VALUES (1, ${LocalDate.of(2026, 11, 10).toEpochDay()}, 'TOI', '["ca_kho_rieng"]', 0)"""
            )
            close()
        }

        // Chạy migration (AutoMigration) và kiểm tra schema khớp với 2.json.
        helper.runMigrationsAndValidate(dbName, 2, true).close()

        // Mở bằng Room thật để chắc dữ liệu cũ đọc được và bảng mới dùng được.
        val db = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            dbName,
        ).build()
        runBlocking {
            val dishes = db.dishDao().getAllWithIngredients()
            assertEquals(1, dishes.size)
            assertEquals(true, dishes[0].dish.isFavorite)

            val meal = db.mealLogDao().get(LocalDate.of(2026, 11, 10), MealType.TOI)
            assertEquals(listOf("ca_kho_rieng"), meal?.dishIds)

            db.recipeDao().insertIgnore(listOf(Recipe("ca_kho_rieng", listOf("1 con cá"), listOf("Kho"))))
            // IGNORE không ghi đè công thức đã có.
            db.recipeDao().insertIgnore(listOf(Recipe("ca_kho_rieng", listOf("khác"), listOf("khác"))))
            assertEquals(listOf("Kho"), db.recipeDao().observe("ca_kho_rieng").first()?.steps)
        }
        db.close()
    }
}
