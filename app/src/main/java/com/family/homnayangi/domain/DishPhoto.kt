package com.family.homnayangi.domain

import com.family.homnayangi.domain.model.DishInfo

/**
 * Chọn ảnh cho một món: ảnh Mẹ tự chụp (F08, [DishInfo.imageUri]) → ảnh có sẵn trong app
 * (`assets/dish_images/<mã_món>.webp`, tạo bằng tools/optimize_dish_images.py) → không có (hiện ô màu theo loại).
 */
object DishPhoto {
    const val ASSET_DIR = "dish_images"
    private const val ASSET_PREFIX = "file:///android_asset/"

    fun assetUri(dishId: String): String = "$ASSET_PREFIX$ASSET_DIR/$dishId.webp"

    /** [bundledIds] = mã các món có ảnh trong assets. */
    fun source(dish: DishInfo, bundledIds: Set<String>): String? =
        dish.imageUri?.takeIf { it.isNotBlank() }
            ?: if (dish.id in bundledIds) assetUri(dish.id) else null

    /** "file:///android_asset/dish_images/x.webp" → "dish_images/x.webp"; null nếu không phải ảnh trong assets. */
    fun assetPath(uri: String): String? = uri.takeIf { it.startsWith(ASSET_PREFIX) }?.removePrefix(ASSET_PREFIX)

    /** Tên file trong assets → mã món ("canh_cua.webp" → "canh_cua"). */
    fun idsFromAssetFiles(files: Iterable<String>): Set<String> =
        files.filter { it.endsWith(".webp") }.mapTo(HashSet()) { it.removeSuffix(".webp") }
}
