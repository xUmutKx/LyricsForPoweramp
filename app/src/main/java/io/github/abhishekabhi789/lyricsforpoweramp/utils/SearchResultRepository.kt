package io.github.abhishekabhi789.lyricsforpoweramp.utils

import io.github.abhishekabhi789.lyricsforpoweramp.model.SearchResultData
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor() {

    private val cache =
        mutableMapOf<String, SearchResultData>()

    fun saveResultData(resultData: SearchResultData): String {
        val key = generateNewKey()
        cache[key] = resultData
        return key
    }

    fun getResult(key: String): SearchResultData? {
        return cache[key]
    }

    fun clearResult(key: String): Boolean {
        return cache.remove(key) != null
    }

    private fun generateNewKey() = UUID.randomUUID().toString()
}
