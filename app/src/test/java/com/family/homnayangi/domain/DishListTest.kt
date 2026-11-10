package com.family.homnayangi.domain

import com.family.homnayangi.domain.model.DishCategory
import com.family.homnayangi.domain.model.DishInfo
import com.family.homnayangi.domain.model.Region
import com.family.homnayangi.ui.dishes.DishFilter
import com.family.homnayangi.ui.dishes.DishListBuilder
import org.junit.Assert.assertEquals
import org.junit.Test

class DishListTest {
    private fun dish(id: String, name: String, cat: DishCategory, fav: Boolean = false, hidden: Boolean = false) =
        DishInfo(id, name, cat, emptySet(), Region.BAC, 20, emptyList(), isFavorite = fav, isHidden = hidden)

    private val dishes = listOf(
        dish("a", "Cá kho riềng", DishCategory.MAN, fav = true),
        dish("b", "Canh cua mồng tơi", DishCategory.CANH),
        dish("c", "Bò xào cần tỏi", DishCategory.XAO, fav = true, hidden = true),
        dish("d", "Rau muống luộc", DishCategory.RAU, fav = true),
        dish("e", "Cá rô phi rán", DishCategory.MAN),
    )

    @Test
    fun `loc yeu thich bo mon da an`() {
        val ids = DishListBuilder.build(dishes, DishFilter.FAVORITE, "").flatMap { it.dishes }.map { it.id }
        assertEquals(listOf("a", "d"), ids)
    }

    @Test
    fun `tat ca khong gom mon an, da an chi gom mon an`() {
        assertEquals(4, DishListBuilder.build(dishes, DishFilter.ALL, "").sumOf { it.dishes.size })
        assertEquals(listOf("c"), DishListBuilder.build(dishes, DishFilter.HIDDEN, "").flatMap { it.dishes }.map { it.id })
        assertEquals(mapOf(DishFilter.ALL to 4, DishFilter.FAVORITE to 2, DishFilter.HIDDEN to 1), DishListBuilder.counts(dishes))
    }

    @Test
    fun `nhom theo loai, sap xep ten, tim khong dau`() {
        val groups = DishListBuilder.build(dishes, DishFilter.ALL, "")
        assertEquals(listOf(DishCategory.MAN, DishCategory.RAU, DishCategory.CANH), groups.map { it.category })
        assertEquals(listOf("a", "e"), groups[0].dishes.map { it.id })
        val found = DishListBuilder.build(dishes, DishFilter.ALL, "ca kho").flatMap { it.dishes }.map { it.id }
        assertEquals(listOf("a"), found)
    }
}
