package com.family.homnayangi.domain.model

import java.time.LocalDate

/** Một slot trong mâm cơm (F02). */
enum class MealSlot(val label: String, val categories: Set<DishCategory>) {
    MAN("Món mặn", setOf(DishCategory.MAN)),
    RAU("Món rau", setOf(DishCategory.RAU)),
    CANH("Món canh", setOf(DishCategory.CANH)),
    MON_4("Món thứ 4", setOf(DishCategory.MAN, DishCategory.XAO)),
    THEM("Món thêm", setOf(DishCategory.MAN, DishCategory.XAO));

    companion object {
        /** Thứ tự chọn món theo SPEC 8.3: MAN → CANH → RAU → MON_4 → các slot thêm. */
        fun slotsFor(minDishes: Int): List<MealSlot> =
            listOf(MAN, CANH, RAU, MON_4) + List((minDishes - 4).coerceAtLeast(0)) { THEM }

        /** Thứ tự hiển thị trên màn hình: mặn, món thứ 4 / thêm, rau, canh. */
        val displayOrder = listOf(MAN, MON_4, THEM, RAU, CANH)
    }
}

data class IngredientRef(val id: String, val group: IngredientGroup)

/** Dữ liệu một món mà thuật toán gợi ý cần. */
data class DishInfo(
    val id: String,
    val name: String,
    val category: DishCategory,
    val seasons: Set<Season>,
    val region: Region,
    val cookTimeMin: Int,
    val mainIngredients: List<IngredientRef>,
    val imageUri: String? = null,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
) {
    val proteinIngredientIds: Set<String>
        get() = mainIngredients.filter { it.group.isProtein }.mapTo(HashSet()) { it.id }
}

/** Một bữa đã chốt (lấy từ MealLog). */
data class MealRecord(val date: LocalDate, val mealType: MealType, val dishIds: List<String>)

/** Bữa đang xem trên màn hình Hôm nay. */
data class MealTarget(val date: LocalDate, val mealType: MealType)
