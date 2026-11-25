package com.family.homnayangi.domain

import com.family.homnayangi.data.seed.SeedParser
import com.family.homnayangi.domain.model.DishCategory
import com.family.homnayangi.domain.model.DishInfo
import com.family.homnayangi.domain.model.Region
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DishPhotoTest {
    private fun dish(id: String, imageUri: String? = null) =
        DishInfo(id, id, DishCategory.MAN, emptySet(), Region.BAC, 30, emptyList(), imageUri = imageUri)

    @Test
    fun `uu tien anh Me chup, roi anh co san, khong co thi null`() {
        val bundled = setOf("gia_cay")
        assertEquals("content://anh/me", DishPhoto.source(dish("gia_cay", "content://anh/me"), bundled))
        assertEquals("file:///android_asset/dish_images/gia_cay.webp", DishPhoto.source(dish("gia_cay"), bundled))
        assertEquals("file:///android_asset/dish_images/gia_cay.webp", DishPhoto.source(dish("gia_cay", " "), bundled))
        assertNull(DishPhoto.source(dish("mon_moi"), bundled))
    }

    @Test
    fun `duong dan assets`() {
        assertEquals("dish_images/gia_cay.webp", DishPhoto.assetPath(DishPhoto.assetUri("gia_cay")))
        assertNull(DishPhoto.assetPath("content://abc"))
        assertEquals(setOf("a", "b"), DishPhoto.idsFromAssetFiles(listOf("a.webp", "b.webp", "ghi_chu.txt")))
    }

    @Test
    fun `moi mon trong seed deu co anh trong assets`() {
        val root = listOf("src/main/assets", "app/src/main/assets").map(::File).first { it.exists() }
        val ids = DishPhoto.idsFromAssetFiles(File(root, DishPhoto.ASSET_DIR).list().orEmpty().asIterable())
        val seedIds = SeedParser.parse(File(root, "seed_data.json").readText(Charsets.UTF_8)).dishes.map { it.id }
        assertEquals(80, seedIds.size)
        val missing = seedIds.filterNot { it in ids }
        assertTrue("Món chưa có ảnh: $missing", missing.isEmpty())
    }
}
