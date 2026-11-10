package com.family.homnayangi.domain

import com.family.homnayangi.data.seed.RecipeParser
import com.family.homnayangi.data.seed.SeedParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.URLDecoder

class RecipeFileTest {
    private fun asset(name: String) =
        listOf("src/main/assets/$name", "app/src/main/assets/$name").map(::File).first { it.exists() }.readText(Charsets.UTF_8)

    @Test
    fun `moi mon trong seed deu co cong thuc`() {
        val dishes = SeedParser.parse(asset("seed_data.json")).dishes.map { it.id }.toSet()
        val recipes = RecipeParser.parse(asset("recipes.json"))
        val recipeIds = recipes.recipes.map { it.dishId }.toSet()
        assertEquals("món thiếu công thức", emptySet<String>(), dishes - recipeIds)
        assertEquals("công thức cho món không tồn tại", emptySet<String>(), recipeIds - dishes)
        assertEquals(7, recipes.servings)
    }

    @Test
    fun `cong thuc du noi dung va khong co so thu tu thua`() {
        val recipes = RecipeParser.parse(asset("recipes.json")).recipes
        for (r in recipes) {
            assertTrue("${r.dishId}: quá ít bước", r.steps.size >= 3)
            assertTrue("${r.dishId}: thiếu nguyên liệu", r.ingredients.size >= 2)
            // Màn hình tự đánh số, nên bước nấu không được tự ghi "1." ở đầu.
            assertEquals(r.dishId, r.steps, RecipeText.linesFromInput(RecipeText.inputFromLines(r.steps)))
            assertNull(r.videoUrl)
        }
    }

    @Test
    fun `bao loi cong thuc rong hoac trung`() {
        val empty = """{"version":1,"recipes":[{"dishId":"a","ingredients":["x"],"steps":[]}]}"""
        assertTrue(runCatching { RecipeParser.parse(empty) }.exceptionOrNull() is IllegalArgumentException)
        val dup = """{"version":1,"recipes":[
            {"dishId":"a","ingredients":["x"],"steps":["s"]},{"dishId":"a","ingredients":["x"],"steps":["s"]}]}"""
        assertTrue(runCatching { RecipeParser.parse(dup) }.exceptionOrNull() is IllegalArgumentException)
    }
}

class RecipeTextTest {
    @Test
    fun `tach dong khi Me sua`() {
        val input = """
            1. Rửa sạch thịt
            2) Thái miếng

            - Ướp gia vị
            • Rang cho cháy cạnh
            3 quả trứng
        """.trimIndent()
        assertEquals(
            listOf("Rửa sạch thịt", "Thái miếng", "Ướp gia vị", "Rang cho cháy cạnh", "3 quả trứng"),
            RecipeText.linesFromInput(input),
        )
    }

    @Test
    fun `link video`() {
        assertNull(RecipeText.normalizeVideoUrl("  "))
        assertNull(RecipeText.normalizeVideoUrl("không có"))
        assertEquals("https://youtu.be/abc", RecipeText.normalizeVideoUrl("youtu.be/abc"))
        assertEquals("https://www.youtube.com/watch?v=1", RecipeText.normalizeVideoUrl(" https://www.youtube.com/watch?v=1 "))
    }

    @Test
    fun `link tim kiem youtube theo ten mon`() {
        val url = RecipeText.youtubeSearchUrl("Cá kho riềng")
        assertTrue(url.startsWith("https://www.youtube.com/results?search_query="))
        assertEquals("cách nấu Cá kho riềng", URLDecoder.decode(url.substringAfter("="), "UTF-8"))
    }

    @Test
    fun `thoi gian nau`() {
        assertEquals("Khoảng 25 phút", RecipeText.cookTimeLabel(25))
        assertEquals("Khoảng 2 tiếng", RecipeText.cookTimeLabel(120))
        assertEquals("Khoảng 1 tiếng 30 phút", RecipeText.cookTimeLabel(90))
    }
}
