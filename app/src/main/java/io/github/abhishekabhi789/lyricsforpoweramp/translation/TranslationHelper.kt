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

    suspend fun getSupportedLanguages(translator: Translator, lyrics: String): List<String> {
        return when (translator) {
            Translator.GEMINI -> gemini.getSupportedLanguages(lyrics) ?: emptyList()
        }
    }

    suspend fun translate(lyrics: String, targetLanguage: String, translator: Translator): Result? {
        return when (translator) {
            Translator.GEMINI -> {
                return gemini.translateLyrics(lyrics, targetLanguage)
            }
        }
    }
}
