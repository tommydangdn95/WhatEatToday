package com.family.homnayangi.ui

import com.family.homnayangi.domain.MealSlotResolver
import com.family.homnayangi.domain.model.MealTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** Một mâm được chép từ Lịch sử ("Nấu lại mâm này") để Mẹ xem rồi chốt. */
data class CopiedMeal(val target: MealTarget, val dishIds: List<String>)

/**
 * Bữa đang chọn trên màn hình Hôm nay, dùng chung giữa các tab
 * (Lịch sử chép mâm sang bữa này; thông báo nhắc giờ mở đúng bữa).
 */
@Singleton
class MealSelection @Inject constructor() {
    private val _target = MutableStateFlow(MealSlotResolver.resolve(LocalDateTime.now()))
    val target: StateFlow<MealTarget> = _target.asStateFlow()

    private val _copied = MutableStateFlow<CopiedMeal?>(null)
    val copied: StateFlow<CopiedMeal?> = _copied.asStateFlow()

    /** Lần cuối Mẹ tự đổi bữa / ngày. */
    private var lastManualChange = 0L

    fun select(target: MealTarget, manual: Boolean = true) {
        _target.value = target
        if (manual) lastManualChange = System.currentTimeMillis()
    }

    /**
     * Mở lại app: nếu Mẹ không tự đổi bữa trong 30 phút gần đây thì quay về bữa sắp tới theo giờ (F01).
     * Tránh trường hợp để app chạy nền qua đêm, sáng mở ra vẫn thấy bữa của hôm qua.
     */
    fun refreshDefault(now: LocalDateTime = LocalDateTime.now()) {
        if (System.currentTimeMillis() - lastManualChange > 30 * 60 * 1000L) {
            _target.value = MealSlotResolver.resolve(now)
        }
    }

    fun copyToCurrent(dishIds: List<String>) {
        lastManualChange = System.currentTimeMillis()
        _copied.value = CopiedMeal(_target.value, dishIds)
    }

    fun consumeCopied(): CopiedMeal? = _copied.value.also { _copied.value = null }
}
