package com.family.homnayangi.ui.common

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import coil3.compose.AsyncImage
import com.family.homnayangi.domain.DishPhoto
import com.family.homnayangi.domain.model.DishInfo
import com.family.homnayangi.ui.theme.CategoryPalette

/** Danh sách món có ảnh trong assets — đọc một lần (assets.list rất nhanh, ~80 file). */
object BundledDishPhotos {
    @Volatile private var cached: Set<String>? = null

    fun ids(context: Context): Set<String> = cached ?: synchronized(this) {
        cached ?: DishPhoto.idsFromAssetFiles(context.assets.list(DishPhoto.ASSET_DIR).orEmpty().asIterable())
            .also { cached = it }
    }
}

/** Nguồn ảnh của món (null = chưa có ảnh). */
@Composable
fun rememberDishPhoto(dish: DishInfo?): String? {
    val context = LocalContext.current
    return remember(dish?.id, dish?.imageUri) {
        dish?.let { DishPhoto.source(it, BundledDishPhotos.ids(context)) }
    }
}

/**
 * Ảnh món, cắt vừa khung ([modifier] quyết định kích thước, nên giữ tỉ lệ 4:3).
 * Chưa có ảnh / đang tải / lỗi → ô màu theo loại món kèm chữ "Mặn", "Canh"…
 * Ảnh không có contentDescription vì tên món luôn hiện ngay bên cạnh.
 */
@Composable
fun DishImage(dish: DishInfo?, shape: Shape, modifier: Modifier = Modifier) {
    val photo = rememberDishPhoto(dish)
    val bg = dish?.let { Color(CategoryPalette.argb(it.category)) } ?: MaterialTheme.colorScheme.surfaceVariant
    Box(modifier.clip(shape).background(bg), contentAlignment = Alignment.Center) {
        // Chữ loại món nằm dưới ảnh: hiện khi chưa có ảnh hoặc ảnh chưa tải xong.
        Text(
            dish?.category?.label ?: "?",
            color = if (dish != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
        )
        if (photo != null) {
            AsyncImage(
                model = photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
