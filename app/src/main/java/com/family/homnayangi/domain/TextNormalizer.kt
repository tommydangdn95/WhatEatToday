package com.family.homnayangi.domain

import java.text.Normalizer

/** Bỏ dấu tiếng Việt + chữ thường, để tìm kiếm không dấu ("ca kho" → "Cá kho riềng"). */
object TextNormalizer {
    private val marks = Regex("\\p{Mn}+")
    private val spaces = Regex("\\s+")

    fun noAccent(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(marks, "")
            .replace('đ', 'd').replace('Đ', 'D')
            .lowercase()
            .replace(spaces, " ")
            .trim()

    /** Mọi từ trong câu tìm đều có trong [haystackNoAccent]. */
    fun matches(haystackNoAccent: String, query: String): Boolean {
        val q = noAccent(query)
        if (q.isEmpty()) return true
        return q.split(' ').all { haystackNoAccent.contains(it) }
    }
}
