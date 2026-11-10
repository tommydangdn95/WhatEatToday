package com.family.homnayangi.domain

import java.net.URLEncoder

/** F10 — xử lý chữ của công thức nấu. Thuần Kotlin để test. */
object RecipeText {
    private val leadingNumber = Regex("""^\s*(\d+\s*[.)\-:]|[-•*+])\s*""")

    /**
     * Ô nhập "mỗi dòng một thứ" → danh sách. Bỏ dòng trống và số thứ tự / gạch đầu dòng Mẹ tự gõ
     * ("1. Rửa thịt", "- 500g thịt") vì màn hình đã tự đánh số.
     */
    fun linesFromInput(text: String): List<String> =
        text.lines()
            .map { it.replace(leadingNumber, "").trim() }
            .filter { it.isNotEmpty() }

    fun inputFromLines(lines: List<String>): String = lines.joinToString("\n")

    /** Link Mẹ dán: chỉ nhận http(s); bỏ trống → null (dùng trang tìm kiếm). */
    fun normalizeVideoUrl(text: String): String? {
        val t = text.trim()
        if (t.isEmpty()) return null
        return when {
            t.startsWith("https://") || t.startsWith("http://") -> t
            t.startsWith("www.") || t.startsWith("youtube.com") || t.startsWith("youtu.be") || t.startsWith("m.youtube.com") -> "https://$t"
            else -> null
        }
    }

    /** Trang tìm kiếm YouTube "cách nấu <tên món>" — không bịa link video cụ thể. */
    fun youtubeSearchUrl(dishName: String): String =
        "https://www.youtube.com/results?search_query=" + URLEncoder.encode("cách nấu $dishName", "UTF-8")

    fun cookTimeLabel(minutes: Int): String = when {
        minutes < 60 -> "Khoảng $minutes phút"
        minutes % 60 == 0 -> "Khoảng ${minutes / 60} tiếng"
        else -> "Khoảng ${minutes / 60} tiếng ${minutes % 60} phút"
    }
}
