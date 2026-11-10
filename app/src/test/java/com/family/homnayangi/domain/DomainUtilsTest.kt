package com.family.homnayangi.domain

import com.family.homnayangi.data.seed.SeedParser
import com.family.homnayangi.domain.model.MealTarget
import com.family.homnayangi.domain.model.MealType
import com.family.homnayangi.domain.model.WeatherClass
import com.family.homnayangi.ui.today.TodayUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

class MealSlotResolverTest {
    private val day = LocalDate.of(2026, 11, 10)
    private fun at(h: Int, m: Int = 0) = MealSlotResolver.resolve(LocalDateTime.of(day, java.time.LocalTime.of(h, m)))

    @Test
    fun `khung gio theo F01`() {
        assertEquals(MealTarget(day, MealType.TRUA), at(0))
        assertEquals(MealTarget(day, MealType.TRUA), at(10, 59))
        assertEquals(MealTarget(day, MealType.TOI), at(11))
        assertEquals(MealTarget(day, MealType.TOI), at(19, 59))
        assertEquals(MealTarget(day.plusDays(1), MealType.TRUA), at(20))
        assertEquals(MealTarget(day.plusDays(1), MealType.TRUA), at(23, 59))
    }

    @Test
    fun `tieu de theo tieu chi chap nhan F01`() {
        assertEquals("Trưa mai ăn gì?", TodayUiState.titleFor(at(20, 30), day))
        assertEquals("Tối nay ăn gì?", TodayUiState.titleFor(at(12), day))
        assertEquals("Trưa nay ăn gì?", TodayUiState.titleFor(at(6), day))
    }
}

class TextNormalizerTest {
    @Test
    fun `tim kiem khong dau`() {
        val hay = TextNormalizer.noAccent("Cá kho riềng")
        assertEquals("ca kho rieng", hay)
        assertTrue(TextNormalizer.matches(hay, "ca kho"))
        assertTrue(TextNormalizer.matches(hay, "Cá  KHO"))
        assertTrue(TextNormalizer.matches(hay, "rieng ca"))
        assertFalse(TextNormalizer.matches(hay, "thit"))
        assertEquals("dau phu sot ca chua", TextNormalizer.noAccent("Đậu phụ sốt cà chua"))
    }
}

class WeatherClassifierTest {
    @Test
    fun `phan loai theo nguong mac dinh`() {
        fun c(t: Double, r: Double) = WeatherClassifier.classify(t, r, 32, 25, 10)
        assertEquals(WeatherClass.NONG, c(34.0, 0.0))
        assertEquals(WeatherClass.NONG, c(32.0, 20.0))      // nóng ưu tiên hơn mưa
        assertEquals(WeatherClass.MAT_LANH, c(25.0, 0.0))
        assertEquals(WeatherClass.MAT_LANH, c(29.0, 10.0))
        assertEquals(WeatherClass.BINH_THUONG, c(29.0, 2.0))
    }

    @Test
    fun `mac dinh theo thang`() {
        assertEquals(WeatherClass.NONG, WeatherClassifier.byMonth(LocalDate.of(2026, 6, 1)))
        assertEquals(WeatherClass.MAT_LANH, WeatherClassifier.byMonth(LocalDate.of(2026, 11, 10)))
        assertEquals(WeatherClass.MAT_LANH, WeatherClassifier.byMonth(LocalDate.of(2027, 2, 1)))
        assertEquals(WeatherClass.BINH_THUONG, WeatherClassifier.byMonth(LocalDate.of(2026, 3, 1)))
        assertEquals(WeatherClass.BINH_THUONG, WeatherClassifier.byMonth(LocalDate.of(2026, 10, 1)))
    }
}

class SeedParserTest {
    @Test
    fun `doc duoc seed that`() {
        val file = listOf("src/main/assets/seed_data.json", "app/src/main/assets/seed_data.json").map(::File).first { it.exists() }
        val seed = SeedParser.parse(file.readText(Charsets.UTF_8))
        assertEquals(80, seed.dishes.size)
        assertEquals(57, seed.ingredients.size)
        assertTrue(seed.links.isNotEmpty())
        val caKho = seed.dishes.first { it.id == "ca_kho_rieng" }
        assertEquals("ca kho rieng", caKho.nameNoAccent)
    }

    @Test
    fun `bo qua khoa la va bao loi nguyen lieu khong ton tai`() {
        val ok = """{"version":2,"note":"x","ingredients":[{"id":"a","name":"A","group":"THIT"}],
            "dishes":[{"id":"d","name":"D","category":"MAN","seasons":["NONG"],"region":"BAC","cookTimeMin":10,"mainIngredients":["a"],"extra":1}]}"""
        assertEquals(2, SeedParser.parse(ok).version)

        val bad = ok.replace("\"mainIngredients\":[\"a\"]", "\"mainIngredients\":[\"zzz\"]")
        val error = runCatching { SeedParser.parse(bad) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }
}
