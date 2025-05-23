package io.github.abhishekabhi789.lyricsforpoweramp.translation

import io.github.abhishekabhi789.lyricsforpoweramp.model.Result
import okhttp3.RequestBody

interface TranslationProvider {
    val nameRes: Int
    suspend fun getSupportedLanguages(lyrics: String): Result
    suspend fun translateLyrics(lyrics: String, targetLanguage: String): Result
    suspend fun generateResponse(prompt: String): Result
    fun buildRequestBody(prompt: String): RequestBody
    fun parseResponse(response: String): String?
}
