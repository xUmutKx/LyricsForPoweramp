package io.github.abhishekabhi789.lyricsforpoweramp.airewrite

import io.github.abhishekabhi789.lyricsforpoweramp.model.Result
import okhttp3.RequestBody

interface AiProviderRepository {
    val nameResId: Int
    val instructions: String
    suspend fun rewriteLyrics(userPrompt: String, lyrics: String, model: String): Result
    suspend fun generateResponse(prompt: String, model: String): Result
    fun buildRequestBody(prompt: String): RequestBody
    fun parseResponse(response: String): Result
}
