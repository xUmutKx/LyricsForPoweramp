package io.github.abhishekabhi789.lyricsforpoweramp.translation

import android.content.Context
import com.google.gson.Gson
import io.github.abhishekabhi789.lyricsforpoweramp.model.Result
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import okhttp3.OkHttpClient

class TranslationHelper(context: Context, client: OkHttpClient, gson: Gson) {

    private val geminiApiKey = AppPreference.getTranslationApiKey(context, Translator.GEMINI)
    private val gemini = GeminiAiProvider(client, gson, geminiApiKey)

    fun getAvailableTranslators(): List<Translator> = Translator.entries

    suspend fun getSupportedLanguages(translator: Translator, lyrics: String): RequestState {
        val result = when (translator) {
            Translator.GEMINI -> gemini.getSupportedLanguages(lyrics)
        }
        return when (result) {
            is Result.Success -> {
                val list = result.response.split(",")
                    .map { it.trim().replaceFirstChar { c -> c.uppercaseChar() } }
                    .filter { it.isNotEmpty() && it.matches(Regex("^[A-Za-z ]+$")) }
                RequestState.Success(list)
            }

            Result.Cancelled -> RequestState.Idle
            is Result.Failure -> RequestState.Failure(result.error)
        }
    }

    suspend fun translate(
        lyrics: String,
        targetLanguage: String,
        translator: Translator
    ): RequestState {
        val result = when (translator) {
            Translator.GEMINI -> gemini.translateLyrics(lyrics, targetLanguage)
        }
        return when (result) {
            Result.Cancelled -> RequestState.Idle
            is Result.Failure -> RequestState.Failure(result.error)
            is Result.Success -> RequestState.Success(result.response)
        }
    }
}
