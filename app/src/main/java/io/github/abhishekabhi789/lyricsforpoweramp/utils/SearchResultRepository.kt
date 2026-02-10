package io.github.abhishekabhi789.lyricsforpoweramp.utils

import io.github.abhishekabhi789.lyricsforpoweramp.model.InputState
import io.github.abhishekabhi789.lyricsforpoweramp.model.InputState.SearchMode
import io.github.abhishekabhi789.lyricsforpoweramp.model.SearchResultData
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor() {

    private val cache =
        mutableMapOf<String, SearchResultData>()

    fun saveResultData(inputState: InputState, resultData: SearchResultData): String {
        val key = generateKeyForInput(inputState)
        cache[key] = resultData
        return key
    }

    fun getResult(key: String): SearchResultData? {
        return cache[key]
    }

    fun getKeyForInputState(inputState: InputState): String? {
        val key = generateKeyForInput(inputState)
        return if (cache.containsKey(key)) key else null
    }

    /**get a unique key for the current input state combination*/
    private fun generateKeyForInput(inputState: InputState): String {
        val canonical = when (inputState.searchMode) {
            SearchMode.Coarse ->
                "coarse:${inputState.queryString}"

            SearchMode.Fine ->
                buildString {
                    append("fine:")
                    append("track=").append(inputState.queryTrack.trackName)
                    append("|artist=").append(inputState.queryTrack.artistName.orEmpty())
                    append("|album=").append(inputState.queryTrack.albumName.orEmpty())
                }
        }
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))

        return bytes.joinToString("") { "%02x".format(it) }
    }
}
