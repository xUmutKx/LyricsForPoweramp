package io.github.abhishekabhi789.lyricsforpoweramp.utils

import io.github.abhishekabhi789.lyricsforpoweramp.model.LocalLyricsEntry
import io.github.abhishekabhi789.lyricsforpoweramp.model.LocalLyricsMatch
import io.github.abhishekabhi789.lyricsforpoweramp.model.LocalLyricsMatchLine

/**
 * Free text search over the indexed `.lrc` files.
 *
 * A multi word query is treated as one phrase: the words have to follow each other
 * in the lyrics, in the typed order. "kimseye söyleme" only matches lines where
 * "söyleme" comes right after "kimseye", not lines that happen to contain both words
 * far apart. A phrase is also allowed to continue on the next line, since lyrics are
 * wrapped at arbitrary points.
 */
object LocalLyricsSearch {

    /** Lines kept above and below every hit, to give the match some context. */
    const val CONTEXT_LINES = 1

    /** Upper bound on displayed lines so a very common word can't build a huge list. */
    private const val MAX_MATCHED_LINES = 400

    /**
     * Lowercase that keeps Turkish letters intact and never changes the string length,
     * so match offsets stay valid for highlighting the original text.
     */
    fun trLower(text: String): String = buildString(text.length) {
        for (char in text) {
            append(
                when (char) {
                    'I' -> 'ı'
                    'İ' -> 'i'
                    else -> char.lowercaseChar()
                }
            )
        }
    }

    /** Builds the phrase pattern: words in order, separated by any run of whitespace. */
    private fun phrasePattern(query: String): Regex? {
        val words = trLower(query).trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return null
        return words.joinToString(separator = "\\s+") { Regex.escape(it) }.toRegex()
    }

    fun search(entries: List<LocalLyricsEntry>, query: String): List<LocalLyricsMatch> {
        val trimmed = query.trim()
        if (trimmed.length < MIN_SEARCH_QUERY_LENGTH) return emptyList()
        val pattern = phrasePattern(trimmed) ?: return emptyList()

        val matches = mutableListOf<LocalLyricsMatch>()
        var shownLines = 0
        for (entry in entries) {
            val hits = findHits(entry, pattern)
            if (hits.isEmpty()) continue

            val lines = withContextLines(entry, hits)
            matches += LocalLyricsMatch(entry = entry, hits = hits.size, lines = lines)
            shownLines += lines.size
            if (shownLines >= MAX_MATCHED_LINES) break
        }
        return matches.sortedWith(
            compareByDescending<LocalLyricsMatch> { it.hits }.thenBy { trLower(it.entry.title) }
        )
    }

    /** Highlight ranges per line index. A phrase spanning two lines highlights both. */
    private fun findHits(entry: LocalLyricsEntry, pattern: Regex): Map<Int, IntRange> {
        val hits = linkedMapOf<Int, IntRange>()
        val lines = entry.lines
        for (index in lines.indices) {
            val current = trLower(lines[index].text)
            val inLine = pattern.find(current)
            if (inLine != null) {
                hits.putIfAbsent(index, inLine.range)
                continue
            }
            // the phrase may continue on the following line
            val next = lines.getOrNull(index + 1) ?: continue
            val joined = current + " " + trLower(next.text)
            val across = pattern.find(joined) ?: continue
            val breakAt = current.length
            if (across.range.first >= breakAt || across.range.last <= breakAt) continue
            hits.putIfAbsent(index, across.range.first..current.lastIndex)
            hits.putIfAbsent(index + 1, 0..(across.range.last - breakAt - 1))
        }
        return hits
    }

    private fun withContextLines(
        entry: LocalLyricsEntry,
        hits: Map<Int, IntRange>
    ): List<LocalLyricsMatchLine> {
        val wanted = sortedSetOf<Int>()
        for (index in hits.keys) {
            val from = (index - CONTEXT_LINES).coerceAtLeast(0)
            val to = (index + CONTEXT_LINES).coerceAtMost(entry.lines.lastIndex)
            for (i in from..to) wanted.add(i)
        }
        return wanted.map { index ->
            val range = hits[index]
            LocalLyricsMatchLine(
                line = entry.lines[index],
                isMatch = range != null,
                highlightStart = range?.first ?: -1,
                highlightLength = range?.let { it.last - it.first + 1 } ?: 0
            )
        }
    }
}
