package dev.pschmitt.syncwich.ui.common

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/** Returns every case-insensitive, non-overlapping match of [query] in [text]. */
fun searchMatchRanges(text: String, query: String): List<IntRange> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return emptyList()
    val ranges = buildList {
        var start = 0
        while (start <= text.length - normalizedQuery.length) {
            val match = text.indexOf(normalizedQuery, startIndex = start, ignoreCase = true)
            if (match < 0) break
            add(match until match + normalizedQuery.length)
            start = match + normalizedQuery.length
        }
    }
    return ranges
}

fun highlightedSearchText(
    text: String,
    query: String,
    highlightStyle: SpanStyle,
): AnnotatedString {
    val matches = searchMatchRanges(text, query)
    if (matches.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        var cursor = 0
        matches.forEach { range ->
            append(text.substring(cursor, range.first))
            withStyle(highlightStyle) { append(text.substring(range)) }
            cursor = range.last + 1
        }
        append(text.substring(cursor))
    }
}
