package com.family.homnayangi.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import com.family.homnayangi.domain.DishPhoto
import com.family.homnayangi.domain.MenuText
import com.family.homnayangi.domain.model.DishInfo
import com.family.homnayangi.domain.model.MealTarget
import com.family.homnayangi.ui.common.BundledDishPhotos
import com.family.homnayangi.ui.theme.CategoryPalette
import java.io.File
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min

/**
 * F25 — vẽ ảnh thực đơn PNG 1080×1350 để gửi Zalo:
 * tiêu đề "Tối nay mẹ nấu" + ngày, từng món (ảnh món nếu có, không thì ô màu theo loại).
 * Gọi ở luồng nền (Dispatchers.IO / Default).
 */
object MenuImageRenderer {
    const val WIDTH = 1080
    const val HEIGHT = 1350

    private const val PAPER = 0xFFFFFBF5.toInt()
    private const val GREEN = 0xFF1E5E20.toInt()
    private const val GREEN_LIGHT = 0xFFDCEFD8.toInt()
    private const val INK = 0xFF1F1B16.toInt()
    private const val CARD = 0xFFFFFFFF.toInt()
    private const val CARD_BORDER = 0xFFE8DED2.toInt()

    private const val MARGIN = 56f
    private const val HEADER_H = 300f
    private const val BOTTOM_PAD = 56f

    /** Vẽ ảnh, lưu vào `cacheDir/share/menu_<ngày>_<bữa>.png`, trả về Uri FileProvider để chia sẻ. */
    fun renderToUri(
        context: Context,
        target: MealTarget,
        today: LocalDate,
        dishes: List<DishInfo>,
    ): Uri {
        val bitmap = render(context, target, today, dishes)
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        // Chỉ giữ ảnh mới nhất, không để cache phình ra.
        dir.listFiles()?.filter { it.name.startsWith("menu_") }?.forEach { it.delete() }
        val file = File(dir, "menu_${target.date}_${target.mealType.name.lowercase()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun render(
        context: Context,
        target: MealTarget,
        today: LocalDate,
        dishes: List<DishInfo>,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(PAPER)

        drawHeader(c, MenuText.shareHeading(target, today), MenuText.longDate(target.date))
        drawDishes(c, context, dishes)
        return bmp
    }

    // ------------------------------------------------------------------ phần đầu

    private fun drawHeader(c: Canvas, title: String, date: String) {
        val band = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GREEN }
        c.drawRect(0f, 0f, WIDTH.toFloat(), HEADER_H, band)

        val titlePaint = textPaint(96f, CARD, bold = true)
        val titleLayout = layout(fitText(title, titlePaint, WIDTH - 2 * MARGIN, minSize = 64f), titlePaint, WIDTH - 2 * MARGIN, maxLines = 1)
        val datePaint = textPaint(54f, GREEN_LIGHT, bold = false)
        val dateLayout = layout(date, datePaint, WIDTH - 2 * MARGIN, maxLines = 1)

        val gap = 18f
        val top = (HEADER_H - titleLayout.height - gap - dateLayout.height) / 2
        drawLayout(c, titleLayout, MARGIN, top)
        drawLayout(c, dateLayout, MARGIN, top + titleLayout.height + gap)
    }

    // ------------------------------------------------------------------ các món

    private fun drawDishes(c: Canvas, context: Context, dishes: List<DishInfo>) {
        if (dishes.isEmpty()) return
        val areaTop = HEADER_H + 40f
        val areaBottom = HEIGHT - BOTTOM_PAD
        val spacing = 24f
        val rowH = min(220f, (areaBottom - areaTop - spacing * (dishes.size - 1)) / dishes.size)
        // Căn giữa theo chiều dọc khi ít món.
        val used = rowH * dishes.size + spacing * (dishes.size - 1)
        var y = areaTop + max(0f, (areaBottom - areaTop - used) / 2)

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CARD }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CARD_BORDER; style = Paint.Style.STROKE; strokeWidth = 3f
        }
        val namePaint = textPaint(if (rowH >= 170f) 64f else 54f, INK, bold = true)

        for (dish in dishes) {
            val card = RectF(MARGIN, y, WIDTH - MARGIN, y + rowH)
            c.drawRoundRect(card, 28f, 28f, cardPaint)
            c.drawRoundRect(card, 28f, 28f, borderPaint)

            val pad = 18f
            val thumbSize = rowH - 2 * pad
            val thumb = RectF(card.left + pad, card.top + pad, card.left + pad + thumbSize, card.top + pad + thumbSize)
            drawThumb(c, context, dish, thumb)

            val textLeft = thumb.right + 32f
            val textWidth = card.right - 28f - textLeft
            // Loại món đã ghi trong ô màu → chỉ cần tên món.
            val nameLayout = layout(dish.name, namePaint, textWidth, maxLines = 2)
            drawLayout(c, nameLayout, textLeft, card.centerY() - nameLayout.height / 2f)

            y += rowH + spacing
        }
    }

    /** Ảnh món (Mẹ chụp hoặc ảnh có sẵn trong app); không có thì ô màu theo loại món. */
    private fun drawThumb(c: Canvas, context: Context, dish: DishInfo, rect: RectF) {
        val source = DishPhoto.source(dish, BundledDishPhotos.ids(context))
        val photo = source?.let { loadPhoto(context, it, rect.width().toInt()) }
        if (photo != null) {
            val save = c.save()
            val path = android.graphics.Path().apply { addRoundRect(rect, 22f, 22f, android.graphics.Path.Direction.CW) }
            c.clipPath(path)
            // Cắt giữa cho vừa ô vuông.
            val side = min(photo.width, photo.height)
            val src = Rect((photo.width - side) / 2, (photo.height - side) / 2, (photo.width + side) / 2, (photo.height + side) / 2)
            c.drawBitmap(photo, src, rect, Paint(Paint.FILTER_BITMAP_FLAG))
            c.restoreToCount(save)
            photo.recycle()
            return
        }
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CategoryPalette.argb(dish.category) }
        c.drawRoundRect(rect, 22f, 22f, bg)
        val labelPaint = textPaint(rect.width() * 0.26f, CARD, bold = true).apply { textAlign = Paint.Align.CENTER }
        val baseline = rect.centerY() - (labelPaint.descent() + labelPaint.ascent()) / 2
        c.drawText(dish.category.label, rect.centerX(), baseline, labelPaint)
    }

    private fun loadPhoto(context: Context, uriString: String, targetPx: Int): Bitmap? = try {
        val assetPath = DishPhoto.assetPath(uriString)
        val open: () -> java.io.InputStream? = when {
            assetPath != null -> { { context.assets.open(assetPath) } }
            uriString.startsWith("/") -> { { context.contentResolver.openInputStream(Uri.fromFile(File(uriString))) } }
            else -> { { context.contentResolver.openInputStream(Uri.parse(uriString)) } }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open()?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (min(bounds.outWidth, bounds.outHeight) / (sample * 2) >= targetPx) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        open()?.use { BitmapFactory.decodeStream(it, null, opts) }
    } catch (_: Exception) {
        null // Ảnh đã bị xoá / không đọc được → dùng ô màu.
    }

    // ------------------------------------------------------------------ tiện ích chữ

    private fun textPaint(size: Float, color: Int, bold: Boolean) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    /** Thu nhỏ cỡ chữ cho tiêu đề dài ("Trưa T5 12/11 mẹ nấu") vừa một dòng. */
    private fun fitText(text: String, paint: TextPaint, width: Float, minSize: Float): String {
        while (paint.measureText(text) > width && paint.textSize > minSize) paint.textSize -= 4f
        return text
    }

    private fun layout(text: String, paint: TextPaint, width: Float, maxLines: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setIncludePad(false)
            .build()

    private fun drawLayout(c: Canvas, layout: StaticLayout, x: Float, y: Float) {
        val save = c.save()
        c.translate(x, y)
        layout.draw(c)
        c.restoreToCount(save)
    }
}
