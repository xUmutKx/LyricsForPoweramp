package io.github.abhishekabhi789.lyricsforpoweramp.utils

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.widget.Toast

fun Context.makeToast(messageRes: Int) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
    } else {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
        }
    }
}

/**@return document tree id of the uri*/
fun Uri.getTreeDocumentId(): String {
    return DocumentsContract.getTreeDocumentId(this)
}

/** Minimum query length before a free-text search is actually run. */
const val MIN_SEARCH_QUERY_LENGTH = 3

/**
 * Checks that [text] contains every word of [query] as a contiguous, in-order run
 * (like a phrase search), instead of each word matching anywhere independently.
 * Case- and extra-whitespace-insensitive.
 */
fun matchesAsPhrase(text: String, query: String): Boolean {
    val queryWords = LocalLyricsSearch.trLower(query.trim()).split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    if (queryWords.isEmpty()) return true
    val textWords = LocalLyricsSearch.trLower(text.trim()).split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    if (queryWords.size > textWords.size) return false
    for (start in 0..(textWords.size - queryWords.size)) {
        var matched = true
        for (i in queryWords.indices) {
            if (!textWords[start + i].contains(queryWords[i])) {
                matched = false
                break
            }
        }
        if (matched) return true
    }
    return false
}
