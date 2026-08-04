package com.icinema.pages.widgets

/**
 * Adaptive episode range sizing for long series (anime 1000+ eps).
 *
 * ≤24: single page (no range bar)
 * 25–100: 20/page
 * 101–500: 50/page
 * >500: 100/page
 */
fun episodeRangeSize(totalEpisodes: Int): Int {
    val total = totalEpisodes.coerceAtLeast(0)
    return when {
        total <= 24 -> total.coerceAtLeast(1)
        total <= 100 -> 20
        total <= 500 -> 50
        else -> 100
    }
}

fun episodeRangeCount(totalEpisodes: Int, rangeSize: Int = episodeRangeSize(totalEpisodes)): Int {
    if (totalEpisodes <= 0) return 0
    val size = rangeSize.coerceAtLeast(1)
    return (totalEpisodes + size - 1) / size
}

fun episodeRangeIndex(
    episodeIndex: Int,
    totalEpisodes: Int,
    rangeSize: Int = episodeRangeSize(totalEpisodes)
): Int {
    if (totalEpisodes <= 0) return 0
    val size = rangeSize.coerceAtLeast(1)
    val maxRange = (episodeRangeCount(totalEpisodes, size) - 1).coerceAtLeast(0)
    return (episodeIndex.coerceAtLeast(0) / size).coerceIn(0, maxRange)
}

fun episodeRangeBounds(
    rangeIndex: Int,
    totalEpisodes: Int,
    rangeSize: Int = episodeRangeSize(totalEpisodes)
): IntRange {
    if (totalEpisodes <= 0) return IntRange.EMPTY
    val size = rangeSize.coerceAtLeast(1)
    val start = rangeIndex.coerceAtLeast(0) * size
    if (start >= totalEpisodes) return IntRange.EMPTY
    val endExclusive = minOf(start + size, totalEpisodes)
    return start until endExclusive
}

private val SHORT_EPISODE_TITLE = Regex(
    pattern = """^\s*(?:第\s*)?(\d{1,4})\s*(?:集|话|回|期|章|部)?\s*$""" +
        """|^\s*(?:EP|E|SP|P)\s*[-._]?\s*(\d{1,4})\s*$""",
    option = RegexOption.IGNORE_CASE
)

fun isDenseEpisodeLayout(titles: List<String>, sampleLimit: Int = 30): Boolean {
    if (titles.isEmpty()) return false
    val sample = titles.take(sampleLimit)
    val shortCount = sample.count { isShortEpisodeTitle(it) }
    return shortCount * 5 >= sample.size * 4 // ≥80%
}

fun isShortEpisodeTitle(title: String): Boolean {
    val trimmed = title.trim()
    if (trimmed.isEmpty()) return true
    return SHORT_EPISODE_TITLE.containsMatchIn(trimmed)
}

/** Prefer compact numeric label for dense grid; fall back to 1-based index. */
fun denseEpisodeLabel(title: String, zeroBasedIndex: Int): String {
    val trimmed = title.trim()
    if (trimmed.isEmpty()) return (zeroBasedIndex + 1).toString()
    val match = SHORT_EPISODE_TITLE.find(trimmed) ?: return (zeroBasedIndex + 1).toString()
    val number = match.groupValues.drop(1).firstOrNull { it.isNotBlank() }
    return number ?: (zeroBasedIndex + 1).toString()
}
