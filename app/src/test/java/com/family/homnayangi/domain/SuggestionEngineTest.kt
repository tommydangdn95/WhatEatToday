package com.family.homnayangi.domain

import com.family.homnayangi.data.seed.SeedParser
import com.family.homnayangi.domain.model.DishCategory
import com.family.homnayangi.domain.model.DishInfo
import com.family.homnayangi.domain.model.IngredientGroup
import com.family.homnayangi.domain.model.IngredientRef
import com.family.homnayangi.domain.model.MealRecord
import com.family.homnayangi.domain.model.MealSlot
import com.family.homnayangi.domain.model.MealTarget
import com.family.homnayangi.domain.model.MealType
import com.family.homnayangi.domain.model.Region
import com.family.homnayangi.domain.model.Season
import com.family.homnayangi.domain.model.WeatherClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import kotlin.random.Random

class SuggestionEngineTest {

    // ------------------------------------------------------------ dữ liệu thật từ seed_data.json

    private val seedDishes: List<DishInfo> by lazy {
        val file = listOf("src/main/assets/seed_data.json", "app/src/main/assets/seed_data.json")
            .map(::File).first { it.exists() }
        val seed = SeedParser.parse(file.readText(Charsets.UTF_8))
        val ingredients = seed.ingredients.associateBy { it.id }
        val linksByDish = seed.links.groupBy { it.dishId }
        seed.dishes.map { d ->
            DishInfo(
                id = d.id,
                name = d.name,
                category = d.category,
                seasons = d.seasons.toSet(),
                region = d.region,
                cookTimeMin = d.cookTimeMin,
                mainIngredients = linksByDish[d.id].orEmpty().map { IngredientRef(it.ingredientId, ingredients.getValue(it.ingredientId).group) },
            )
        }
    }

    private val today = LocalDate.of(2026, 11, 10)
    private val lunchToday = MealTarget(today, MealType.TRUA)
    private fun ctx(
        weather: WeatherClass = WeatherClass.BINH_THUONG,
        history: List<MealRecord> = emptyList(),
        pantry: Set<String> = emptySet(),
        quickOnly: Boolean = false,
        minDishes: Int = 4,
        repeatAvoidDays: Int = 4,
    ) = SuggestionContext(lunchToday, weather, history, pantry, repeatAvoidDays, quickOnly, minDishes)

    private val allContexts = listOf(
        ctx(WeatherClass.NONG), ctx(WeatherClass.MAT_LANH), ctx(WeatherClass.BINH_THUONG),
        ctx(quickOnly = true), ctx(minDishes = 6),
    )

    // ------------------------------------------------------------ 8.4 — test tối thiểu

    @Test
    fun `mam luon du 4 slot va dung loai`() {
        for (c in allContexts) repeat(200) { seed ->
            val meal = SuggestionEngine(Random(seed)).suggest(seedDishes, c)
            assertTrue(meal.picks.size >= 4)
            assertEquals(listOf(MealSlot.MAN, MealSlot.CANH, MealSlot.RAU, MealSlot.MON_4), meal.picks.take(4).map { it.slot })
            for (pick in meal.picks) {
                val dish = assertNotNullAndGet(pick.dish?.dish)
                assertTrue("${dish.id} không thuộc slot ${pick.slot}", dish.category in pick.slot.categories)
            }
            assertEquals("không được trùng món", meal.dishIds.size, meal.dishIds.toSet().size)
            assertEquals(c.minDishes, meal.dishIds.size)
        }
    }

    @Test
    fun `mon an khong bao gio xuat hien`() {
        val hiddenIds = seedDishes.filterIndexed { i, _ -> i % 3 == 0 }.map { it.id }.toSet()
        val dishes = seedDishes.map { if (it.id in hiddenIds) it.copy(isHidden = true) else it }
        repeat(300) { seed ->
            val meal = SuggestionEngine(Random(seed)).suggest(dishes, ctx())
            assertTrue(meal.dishIds.none { it in hiddenIds })
        }
    }

    @Test
    fun `an het mon canh thi slot canh de trong chu khong lay mon an`() {
        val dishes = seedDishes.map { if (it.category == DishCategory.CANH) it.copy(isHidden = true) else it }
        val meal = SuggestionEngine(Random(1)).suggest(dishes, ctx())
        assertEquals(null, meal.picks.first { it.slot == MealSlot.CANH }.dish)
    }

    @Test
    fun `mon an hom qua khong xuat hien khi repeatAvoidDays = 4`() {
        val yesterday = today.minusDays(1)
        // Hôm qua ăn khá nhiều món, nhưng mỗi loại vẫn còn món khác → không cần nới điều kiện.
        val eaten = seedDishes.groupBy { it.category }.values.flatMap { it.take(3) }.map { it.id }
        val history = listOf(
            MealRecord(yesterday, MealType.TRUA, eaten.take(eaten.size / 2)),
            MealRecord(yesterday, MealType.TOI, eaten.drop(eaten.size / 2)),
        )
        repeat(300) { seed ->
            val meal = SuggestionEngine(Random(seed)).suggest(seedDishes, ctx(history = history))
            assertTrue(meal.dishIds.none { it in eaten })
        }
    }

    @Test
    fun `mon an 5 ngay truoc duoc goi y lai`() {
        val engine = SuggestionEngine()
        val c = ctx(history = listOf(MealRecord(today.minusDays(5), MealType.TOI, listOf("x"))))
        assertEquals(5L, engine.daysSinceLastEaten(c)["x"])
        assertEquals(5, engine.recencyScore(5))
    }

    @Test
    fun `phai noi dieu kien tranh lap khi het mon thay vi de trong`() {
        // Ăn hết mọi món RAU hôm qua → slot RAU vẫn phải có món.
        val allRau = seedDishes.filter { it.category == DishCategory.RAU }.map { it.id }
        val c = ctx(history = listOf(MealRecord(today.minusDays(1), MealType.TOI, allRau)))
        repeat(50) { seed ->
            val meal = SuggestionEngine(Random(seed)).suggest(seedDishes, c)
            assertNotNull(meal.picks.first { it.slot == MealSlot.RAU }.dish)
        }
    }

    @Test
    fun `ngay nong mon chi co mua lanh khong bao gio dung dau`() {
        val engine = SuggestionEngine()
        // Các món giống hệt nhau, chỉ khác mùa → món chỉ LANH phải xếp cuối.
        fun dish(id: String, vararg seasons: Season) = DishInfo(
            id, id, DishCategory.CANH, seasons.toSet(), Region.BAC, 20,
            listOf(IngredientRef("rau_$id", IngredientGroup.RAU_CU)),
        )
        val dishes = listOf(dish("a_lanh", Season.LANH), dish("b_nong", Season.NONG), dish("c_quanh_nam", Season.QUANH_NAM))
        val ranked = engine.rank(dishes, ctx(WeatherClass.NONG))
        assertEquals("b_nong", ranked.first().dish.id)
        assertEquals("a_lanh", ranked.last().dish.id)

        // Với dữ liệu thật: món đứng đầu mỗi loại không phải món chỉ có mùa LANH.
        val real = engine.rank(seedDishes, ctx(WeatherClass.NONG))
        for (category in DishCategory.entries) {
            val top = real.first { it.dish.category == category }.dish
            assertFalse("${top.id} chỉ có mùa LANH mà đứng đầu ngày nóng", top.seasons == setOf(Season.LANH))
        }
    }

    @Test
    fun `khong co 2 mon trung nguyen lieu thit ca chinh`() {
        for (c in allContexts) repeat(300) { seed ->
            val meal = SuggestionEngine(Random(seed)).suggest(seedDishes, c)
            val proteins = meal.picks.mapNotNull { it.dish?.dish }.flatMap { it.proteinIngredientIds }
            assertEquals("trùng thịt/cá trong mâm: $proteins", proteins.size, proteins.toSet().size)
        }
    }

    // ------------------------------------------------------------ chấm điểm (8.2)

    @Test
    fun `diem thoi tiet theo bang`() {
        val e = SuggestionEngine()
        assertEquals(30, e.weatherScore(setOf(Season.NONG), WeatherClass.NONG))
        assertEquals(20, e.weatherScore(setOf(Season.QUANH_NAM), WeatherClass.NONG))
        assertEquals(0, e.weatherScore(setOf(Season.LANH), WeatherClass.NONG))
        assertEquals(30, e.weatherScore(setOf(Season.LANH), WeatherClass.MAT_LANH))
        assertEquals(20, e.weatherScore(setOf(Season.QUANH_NAM), WeatherClass.MAT_LANH))
        assertEquals(0, e.weatherScore(setOf(Season.NONG), WeatherClass.MAT_LANH))
        assertEquals(25, e.weatherScore(setOf(Season.QUANH_NAM), WeatherClass.BINH_THUONG))
        assertEquals(20, e.weatherScore(setOf(Season.NONG), WeatherClass.BINH_THUONG))
    }

    @Test
    fun `tong diem toi da la 100`() {
        val e = SuggestionEngine()
        val dish = DishInfo(
            "x", "x", DishCategory.MAN, setOf(Season.NONG), Region.BAC, 20,
            listOf(IngredientRef("thit", IngredientGroup.THIT), IngredientRef("hanh", IngredientGroup.RAU_CU)),
            isFavorite = true,
        )
        val full = e.score(dish, ctx(WeatherClass.NONG, pantry = setOf("thit", "hanh")), null)
        assertEquals(100.0, full.score, 0.001)
        assertTrue(full.hasPantryIngredient)

        val half = e.score(dish, ctx(WeatherClass.NONG, pantry = setOf("thit")), daysSinceLastEaten = 3)
        assertEquals(30 + 20 + 3 + 10 + 7.5, half.score, 0.001)
    }

    @Test
    fun `loc mon nhanh chi lay mon duoi 30 phut khi du mon`() {
        repeat(200) { seed ->
            val meal = SuggestionEngine(Random(seed)).suggest(seedDishes, ctx(quickOnly = true))
            assertTrue(meal.picks.all { it.dish!!.dish.cookTimeMin < 30 })
        }
    }

    @Test
    fun `cung seed cho cung ket qua`() {
        val a = SuggestionEngine(Random(42)).suggest(seedDishes, ctx())
        val b = SuggestionEngine(Random(42)).suggest(seedDishes, ctx())
        assertEquals(a, b)
    }

    // ------------------------------------------------------------ đổi món (F03)

    @Test
    fun `doi mon chi thay dung slot va giu loai`() {
        val engine = SuggestionEngine(Random(7))
        val c = ctx()
        var meal = engine.suggest(seedDishes, c)
        val rejected = HashSet<String>()
        repeat(4) { index ->
            val before = meal
            rejected += before.picks[index].dish!!.dish.id
            meal = engine.swap(seedDishes, c, before, index, rejected)
            assertNotEquals(before.picks[index].dish!!.dish.id, meal.picks[index].dish!!.dish.id)
            assertTrue(meal.picks[index].dish!!.dish.category in meal.picks[index].slot.categories)
            meal.picks.forEachIndexed { i, p -> if (i != index) assertEquals(before.picks[i], p) }
        }
    }

    @Test
    fun `doi mon khong lap mon vua bi doi trong phien`() {
        val engine = SuggestionEngine(Random(3))
        val c = ctx()
        var meal = engine.suggest(seedDishes, c)
        val rauIndex = meal.picks.indexOfFirst { it.slot == MealSlot.RAU }
        val totalRau = seedDishes.count { it.category == DishCategory.RAU }
        val seen = mutableListOf(meal.picks[rauIndex].dish!!.dish.id)
        val rejected = HashSet<String>()
        // Đổi (tổng số món rau - 1) lần → mỗi lần là một món mới, không lặp.
        repeat(totalRau - 1) {
            rejected += meal.picks[rauIndex].dish!!.dish.id
            meal = engine.swap(seedDishes, c, meal, rauIndex, rejected)
            seen += meal.picks[rauIndex].dish!!.dish.id
        }
        assertEquals(seen.size, seen.toSet().size)
        // Hết món mới: vẫn đổi được (quay lại món cũ) chứ không đứng yên.
        rejected += meal.picks[rauIndex].dish!!.dish.id
        val again = engine.swap(seedDishes, c, meal, rauIndex, rejected)
        assertNotEquals(meal.picks[rauIndex].dish!!.dish.id, again.picks[rauIndex].dish!!.dish.id)
    }

    @Test
    fun `doi mon khong tao trung thit ca voi cac mon con lai`() {
        val c = ctx()
        repeat(100) { seed ->
            val engine = SuggestionEngine(Random(seed))
            var meal = engine.suggest(seedDishes, c)
            repeat(10) { k -> meal = engine.swap(seedDishes, c, meal, k % meal.picks.size, emptySet()) }
            val proteins = meal.picks.mapNotNull { it.dish?.dish }.flatMap { it.proteinIngredientIds }
            assertEquals(proteins.size, proteins.toSet().size)
        }
    }

    // ------------------------------------------------------------ xếp mâm có sẵn (chốt / chép từ Lịch sử)

    @Test
    fun `xep mon da chot vao dung slot`() {
        val engine = SuggestionEngine(Random(1))
        val meal = engine.suggest(seedDishes, ctx())
        val shuffled = meal.dishIds.reversed()
        val arranged = engine.arrange(seedDishes, ctx(), shuffled, fillMissing = false)
        assertEquals(meal.picks.map { it.slot to it.dish!!.dish.id }, arranged.picks.map { it.slot to it.dish!!.dish.id })
    }

    @Test
    fun `mam chep thieu mon thi goi y them, bo mon da xoa`() {
        val man = seedDishes.first { it.category == DishCategory.MAN }.id
        val arranged = SuggestionEngine(Random(2)).arrange(seedDishes, ctx(), listOf(man, "mon_da_xoa"), fillMissing = true)
        assertEquals(4, arranged.dishIds.size)
        assertEquals(man, arranged.picks.first { it.slot == MealSlot.MAN }.dish!!.dish.id)
        assertTrue(arranged.picks.all { it.dish != null })
    }

    // ------------------------------------------------------------ món yêu thích (chiếm 10/100 điểm)

    @Test
    fun `trong so cac tieu chi cong lai bang 100, yeu thich chiem 10`() {
        with(SuggestionEngine) {
            assertEquals(100, WEATHER_MAX + REGION_MAX + RECENCY_MAX + FAVORITE + PANTRY_MAX)
            assertEquals(10, FAVORITE)
        }
        assertEquals(25, SuggestionEngine().recencyScore(null))
        assertEquals(25, SuggestionEngine().recencyScore(40))
    }

    @Test
    fun `mon yeu thich xep tren mon giong het nhung khong yeu thich`() {
        val base = seedDishes.first { it.category == DishCategory.CANH }
        val fav = base.copy(id = "z_fav", isFavorite = true)
        val plain = base.copy(id = "a_plain")
        val ranked = SuggestionEngine().rank(listOf(plain, fav), ctx())
        assertEquals("z_fav", ranked.first().dish.id)
        assertEquals(10.0, ranked[0].score - ranked[1].score, 0.001)
    }

    @Test
    fun `mon yeu thich duoc goi y thuong xuyen hon`() {
        val favIds = seedDishes.filter { it.category == DishCategory.CANH }.take(3).map { it.id }.toSet()
        val withFav = seedDishes.map { if (it.id in favIds) it.copy(isFavorite = true) else it }
        fun count(dishes: List<DishInfo>) = (0 until 2000).count { seed ->
            SuggestionEngine(Random(seed)).suggest(dishes, ctx()).dishIds.any { it in favIds }
        }
        val before = count(seedDishes)
        val after = count(withFav)
        assertTrue("có yêu thích: $after lần, không: $before lần", after > before)
    }

    @Test
    fun `mon yeu thich van tuan thu luat cu`() {
        val canh = seedDishes.filter { it.category == DishCategory.CANH }
        val eatenYesterday = canh[0].id
        val hidden = canh[1].id
        val dishes = seedDishes.map {
            when (it.id) {
                eatenYesterday -> it.copy(isFavorite = true)
                hidden -> it.copy(isFavorite = true, isHidden = true)
                else -> it
            }
        }
        val c = ctx(history = listOf(MealRecord(today.minusDays(1), MealType.TOI, listOf(eatenYesterday))), quickOnly = true)
        repeat(500) { seed ->
            val meal = SuggestionEngine(Random(seed)).suggest(dishes, c)
            assertFalse("món yêu thích bị ẩn vẫn được gợi ý", hidden in meal.dishIds)
            assertFalse("món yêu thích vừa ăn hôm qua vẫn được gợi ý", eatenYesterday in meal.dishIds)
            assertTrue("bộ lọc nhanh bị bỏ qua", meal.picks.all { it.dish!!.dish.cookTimeMin < 30 })
            val proteins = meal.picks.mapNotNull { it.dish?.dish }.flatMap { it.proteinIngredientIds }
            assertEquals(proteins.size, proteins.toSet().size)
            assertEquals(listOf(MealSlot.MAN, MealSlot.CANH, MealSlot.RAU, MealSlot.MON_4), meal.picks.map { it.slot })
        }
    }

    // ------------------------------------------------------------ Mẹ tự thêm món vào mâm

    @Test
    fun `them mon noi vao cuoi mam, khong them trung`() {
        val engine = SuggestionEngine(Random(4))
        val meal = engine.suggest(seedDishes, ctx())
        val extra = seedDishes.first { it.category == DishCategory.RAU && it.id !in meal.dishIds }.id
        val added = engine.addDish(seedDishes, ctx(), meal, extra)
        assertEquals(5, added.picks.size)
        assertEquals(meal.picks, added.picks.take(4))
        assertEquals(MealSlot.THEM, added.picks.last().slot)
        assertEquals(extra, added.picks.last().dish!!.dish.id)
        // Món đã có trong mâm / món không tồn tại → giữ nguyên.
        assertEquals(added, engine.addDish(seedDishes, ctx(), added, extra))
        assertEquals(added, engine.addDish(seedDishes, ctx(), added, "khong_co"))
    }

    @Test
    fun `doi mon them giu dung loai cua mon do`() {
        val c = ctx()
        repeat(50) { seed ->
            val engine = SuggestionEngine(Random(seed))
            val meal = engine.suggest(seedDishes, c)
            val rau = seedDishes.first { it.category == DishCategory.RAU && it.id !in meal.dishIds }.id
            val added = engine.addDish(seedDishes, c, meal, rau)
            val swapped = engine.swap(seedDishes, c, added, 4, setOf(rau))
            val dish = swapped.picks[4].dish!!.dish
            assertEquals(DishCategory.RAU, dish.category)
            assertNotEquals(rau, dish.id)
            assertFalse("trùng món đã có trong mâm", dish.id in meal.dishIds)
        }
    }

    @Test
    fun `bo mon nao cung duoc, ke ca 4 mon chinh`() {
        val engine = SuggestionEngine(Random(5))
        val meal = engine.suggest(seedDishes, ctx())
        val extra = seedDishes.first { it.id !in meal.dishIds }.id
        val added = engine.addDish(seedDishes, ctx(), meal, extra)
        assertEquals(meal, engine.remove(added, 4))
        // Bỏ món mặn → mâm còn 3 món, các món khác giữ nguyên thứ tự.
        val noMan = engine.remove(meal, 0)
        assertEquals(meal.picks.drop(1), noMan.picks)
        assertEquals(noMan, engine.remove(noMan, 99))
        // Mâm 3 món đã chốt, xếp lại không tự thêm món.
        val arranged = engine.arrange(seedDishes, ctx(), noMan.dishIds, fillMissing = false)
        assertEquals(noMan.dishIds.toSet(), arranged.dishIds.toSet())
    }

    @Test
    fun `mam da chot co mon them xep lai van giu du mon`() {
        val engine = SuggestionEngine(Random(6))
        val meal = engine.suggest(seedDishes, ctx())
        val extras = seedDishes.filter { it.id !in meal.dishIds && it.category == DishCategory.CANH }.take(2).map { it.id }
        val added = extras.fold(meal) { m, id -> engine.addDish(seedDishes, ctx(), m, id) }
        val arranged = engine.arrange(seedDishes, ctx(), added.dishIds, fillMissing = false)
        assertEquals(added.dishIds.toSet(), arranged.dishIds.toSet())
        assertEquals(6, arranged.picks.size)
    }

    private fun <T : Any> assertNotNullAndGet(value: T?): T {
        assertNotNull(value)
        return value!!
    }
}
