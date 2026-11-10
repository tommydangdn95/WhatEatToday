package com.family.homnayangi.domain

import com.family.homnayangi.domain.model.DishInfo
import com.family.homnayangi.domain.model.MealRecord
import com.family.homnayangi.domain.model.MealSlot
import com.family.homnayangi.domain.model.MealTarget
import com.family.homnayangi.domain.model.Region
import com.family.homnayangi.domain.model.Season
import com.family.homnayangi.domain.model.WeatherClass
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

/** Mọi thứ thuật toán cần biết về bữa đang gợi ý. */
data class SuggestionContext(
    val target: MealTarget,
    val weather: WeatherClass,
    /** Các bữa đã chốt. Người gọi bỏ bản ghi của chính bữa [target] (khi chọn lại). */
    val history: List<MealRecord> = emptyList(),
    /** id nguyên liệu đang có trong tủ lạnh. */
    val pantry: Set<String> = emptySet(),
    val repeatAvoidDays: Int = 4,
    val quickOnly: Boolean = false,
    val minDishes: Int = 4,
)

data class ScoredDish(
    val dish: DishInfo,
    val score: Double,
    /** Có ít nhất một nguyên liệu chính đang có trong tủ lạnh → nhãn "Có sẵn đồ". */
    val hasPantryIngredient: Boolean,
)

/** [dish] = null chỉ khi kho không còn món nào thuộc loại của slot (vd Mẹ ẩn hết món canh). */
data class SlotPick(val slot: MealSlot, val dish: ScoredDish?)

data class MealSuggestion(val picks: List<SlotPick>) {
    val dishIds: List<String> get() = picks.mapNotNull { it.dish?.dish?.id }
}

/**
 * Thuật toán gợi ý mâm cơm (SPEC mục 8). Thuần Kotlin, không phụ thuộc Android.
 * Truyền [random] = Random(seed) để test cho kết quả cố định.
 */
class SuggestionEngine(private val random: Random = Random.Default) {

    // ---------------------------------------------------------------- Chấm điểm (8.2)

    fun weatherScore(seasons: Set<Season>, weather: WeatherClass): Int = when (weather) {
        WeatherClass.NONG -> when {
            Season.NONG in seasons -> 30
            Season.QUANH_NAM in seasons -> 20
            else -> 0
        }
        WeatherClass.MAT_LANH -> when {
            Season.LANH in seasons -> 30
            Season.QUANH_NAM in seasons -> 20
            else -> 0
        }
        WeatherClass.BINH_THUONG -> if (Season.QUANH_NAM in seasons) 25 else 20
    }

    fun regionScore(region: Region): Int = when (region) {
        Region.BAC -> 20
        Region.CHUNG -> 10
        Region.TRUNG, Region.NAM -> 0
    }

    /** [daysSinceLastEaten] = null nếu chưa từng ăn. */
    fun recencyScore(daysSinceLastEaten: Long?): Int =
        if (daysSinceLastEaten == null) 20 else min(daysSinceLastEaten, 20L).toInt()

    fun pantryScore(dish: DishInfo, pantry: Set<String>): Double {
        if (dish.mainIngredients.isEmpty()) return 0.0
        val have = dish.mainIngredients.count { it.id in pantry }
        return 15.0 * have / dish.mainIngredients.size
    }

    fun score(dish: DishInfo, ctx: SuggestionContext, daysSinceLastEaten: Long?): ScoredDish {
        val total = weatherScore(dish.seasons, ctx.weather) +
            regionScore(dish.region) +
            recencyScore(daysSinceLastEaten) +
            (if (dish.isFavorite) 15 else 0) +
            pantryScore(dish, ctx.pantry)
        return ScoredDish(
            dish = dish,
            score = total,
            hasPantryIngredient = dish.mainIngredients.any { it.id in ctx.pantry },
        )
    }

    /** Khoảng cách (ngày) từ bữa đang gợi ý tới lần ăn gần nhất của từng món, tính cả bữa đã chốt trước cho tương lai. */
    fun daysSinceLastEaten(ctx: SuggestionContext): Map<String, Long> {
        val result = HashMap<String, Long>()
        for (record in ctx.history) {
            val distance = abs(ChronoUnit.DAYS.between(record.date, ctx.target.date))
            for (id in record.dishIds) {
                val old = result[id]
                if (old == null || distance < old) result[id] = distance
            }
        }
        return result
    }

    /** Toàn bộ món (trừ món ẩn) đã chấm điểm, xếp từ cao xuống thấp. */
    fun rank(dishes: List<DishInfo>, ctx: SuggestionContext): List<ScoredDish> {
        val lastEaten = daysSinceLastEaten(ctx)
        return dishes.asSequence()
            .filterNot { it.isHidden }
            .map { score(it, ctx, lastEaten[it.id]) }
            .sortedWith(compareByDescending<ScoredDish> { it.score }.thenBy { it.dish.id })
            .toList()
    }

    // ---------------------------------------------------------------- Chọn món (8.1, 8.3)

    /** Gợi ý cả mâm. */
    fun suggest(dishes: List<DishInfo>, ctx: SuggestionContext): MealSuggestion {
        val ranked = rank(dishes, ctx)
        val lastEaten = daysSinceLastEaten(ctx)
        val picks = ArrayList<SlotPick>()
        val usedIds = HashSet<String>()
        val usedProteins = HashSet<String>()
        for (slot in MealSlot.slotsFor(ctx.minDishes)) {
            val chosen = pickForSlot(slot, ranked, ctx, lastEaten, usedIds, usedProteins)
            if (chosen != null) {
                usedIds += chosen.dish.id
                usedProteins += chosen.dish.proteinIngredientIds
            }
            picks += SlotPick(slot, chosen)
        }
        return MealSuggestion(picks)
    }

    /**
     * F03 — đổi đúng món ở vị trí [index] bằng món khác cùng loại.
     * [rejected] = các món đã bị đổi trong phiên này; không gợi ý lại trừ khi hết món.
     */
    fun swap(
        dishes: List<DishInfo>,
        ctx: SuggestionContext,
        current: MealSuggestion,
        index: Int,
        rejected: Set<String> = emptySet(),
    ): MealSuggestion {
        val target = current.picks[index]
        val others = current.picks.filterIndexed { i, _ -> i != index }.mapNotNull { it.dish?.dish }
        val usedProteins = others.flatMapTo(HashSet()) { it.proteinIngredientIds }
        val ranked = rank(dishes, ctx)
        val lastEaten = daysSinceLastEaten(ctx)
        val othersIds = others.mapTo(HashSet()) { it.id }
        val currentId = target.dish?.dish?.id

        val replacement =
            pickForSlot(target.slot, ranked, ctx, lastEaten, othersIds + rejected + listOfNotNull(currentId), usedProteins)
                // Đã đổi hết các món cùng loại trong phiên → cho phép quay lại món đã bị đổi, chỉ trừ món đang hiện.
                ?: pickForSlot(target.slot, ranked, ctx, lastEaten, othersIds + listOfNotNull(currentId), usedProteins)
                ?: return current

        return MealSuggestion(current.picks.toMutableList().also { it[index] = SlotPick(target.slot, replacement) })
    }

    /**
     * Xếp một danh sách món có sẵn (mâm đã chốt, mâm chép từ Lịch sử) vào các slot.
     * Món không còn trong kho (đã xoá) bị bỏ qua. Nếu [fillMissing] = true, slot bắt buộc còn thiếu được gợi ý thêm.
     */
    fun arrange(
        dishes: List<DishInfo>,
        ctx: SuggestionContext,
        dishIds: List<String>,
        fillMissing: Boolean,
    ): MealSuggestion {
        val lastEaten = daysSinceLastEaten(ctx)
        val byId = dishes.associateBy { it.id }
        val scored = dishIds.distinct().mapNotNull { byId[it] }.map { score(it, ctx, lastEaten[it.id]) }

        val remaining = scored.toMutableList()
        fun take(slot: MealSlot): ScoredDish? =
            remaining.firstOrNull { it.dish.category in slot.categories }?.also { remaining.remove(it) }

        val required = listOf(MealSlot.MAN, MealSlot.CANH, MealSlot.RAU, MealSlot.MON_4)
        val picks = required.map { SlotPick(it, take(it)) }.toMutableList()
        remaining.forEach { picks += SlotPick(MealSlot.THEM, it) }
        while (picks.size < ctx.minDishes) picks += SlotPick(MealSlot.THEM, null)

        if (fillMissing && picks.any { it.dish == null }) {
            val ranked = rank(dishes, ctx)
            for (i in picks.indices) {
                if (picks[i].dish != null) continue
                val used = picks.mapNotNull { it.dish?.dish }
                val chosen = pickForSlot(
                    picks[i].slot, ranked, ctx, lastEaten,
                    used.mapTo(HashSet()) { it.id },
                    used.flatMapTo(HashSet()) { it.proteinIngredientIds },
                )
                picks[i] = SlotPick(picks[i].slot, chosen)
            }
        }
        return MealSuggestion(if (fillMissing) picks else picks.filter { it.dish != null })
    }

    private fun pickForSlot(
        slot: MealSlot,
        ranked: List<ScoredDish>,
        ctx: SuggestionContext,
        lastEaten: Map<String, Long>,
        excludeIds: Set<String>,
        usedProteins: Set<String>,
    ): ScoredDish? {
        val base = ranked.filter { it.dish.category in slot.categories && it.dish.id !in excludeIds }
        if (base.isEmpty()) return null

        fun recentlyEaten(d: DishInfo): Boolean = lastEaten[d.id]?.let { it <= ctx.repeatAvoidDays } ?: false
        fun quickOk(d: DishInfo): Boolean = d.cookTimeMin < QUICK_COOK_MIN
        fun noConflict(d: DishInfo): Boolean = d.proteinIngredientIds.none { it in usedProteins }

        // Nới dần điều kiện (8.1 bước 4): bỏ bộ lọc nhanh → bỏ tránh lặp → (cuối cùng) bỏ tránh trùng thịt/cá.
        val levels = buildList<(DishInfo) -> Boolean> {
            if (ctx.quickOnly) add { d -> quickOk(d) && !recentlyEaten(d) && noConflict(d) }
            add { d -> !recentlyEaten(d) && noConflict(d) }
            add { d -> noConflict(d) }
            add { _ -> true }
        }
        for (accept in levels) {
            val candidates = base.filter { accept(it.dish) }
            if (candidates.isNotEmpty()) return weightedPick(candidates)
        }
        return null
    }

    /** Lấy top [TOP_N] (đã xếp theo điểm), chọn ngẫu nhiên có trọng số ∝ điểm + 1. */
    private fun weightedPick(sortedCandidates: List<ScoredDish>): ScoredDish {
        val top = sortedCandidates.take(TOP_N)
        val total = top.sumOf { it.score + 1 }
        var r = random.nextDouble() * total
        for (c in top) {
            r -= c.score + 1
            if (r < 0) return c
        }
        return top.last()
    }

    companion object {
        const val TOP_N = 6
        const val QUICK_COOK_MIN = 30
    }
}
