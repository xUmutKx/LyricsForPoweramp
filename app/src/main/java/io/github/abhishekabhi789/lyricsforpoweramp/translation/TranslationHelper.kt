package io.github.abhishekabhi789.lyricsforpoweramp.translation

import com.google.gson.Gson
import io.github.abhishekabhi789.lyricsforpoweramp.model.Result
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Provider

class TranslationHelper @Inject constructor(
    private val appPreference: AppPreference,
    okHttpClientProvider: Provider<OkHttpClient>,
    private val gson: Gson,
) {
    private val client by lazy { okHttpClientProvider.get() }
    private var gemini: GeminiAiProvider? = null

    suspend fun refreshProviders() {
        appPreference.translators.firstOrNull()?.let { translators ->
            translators.forEach { (translator, key) ->
                if (!key.isNullOrBlank()) {
                    when (translator) {
                        Translator.GEMINI -> gemini = GeminiAiProvider(client, gson, key)
                    }
                }
            }
        }
    }

    suspend fun getSupportedLanguages(translator: Translator, lyrics: String): RequestState {
        if (!translator.isConfigured()) {
            return RequestState.Failure("No API Key")
        }
        val result = when (translator) {
            Translator.GEMINI -> gemini!!.getSupportedLanguages(lyrics)
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
        if (!translator.isConfigured()) {
            return RequestState.Failure("No API Key")
        }
        val result = when (translator) {
            Translator.GEMINI -> gemini!!.translateLyrics(lyrics, targetLanguage)
        }
        return when (result) {
            Result.Cancelled -> RequestState.Idle
            is Result.Failure -> RequestState.Failure(result.error)
            is Result.Success -> RequestState.Success(result.response)
        }
    }

    private fun Translator.isConfigured(): Boolean {
        return when (this) {
            Translator.GEMINI -> gemini != null
        }
    }
}
