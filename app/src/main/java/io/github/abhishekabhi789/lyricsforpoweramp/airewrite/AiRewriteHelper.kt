package io.github.abhishekabhi789.lyricsforpoweramp.airewrite

import android.util.Log
import com.google.gson.Gson
import io.github.abhishekabhi789.lyricsforpoweramp.model.Result
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Provider

class AiRewriteHelper @Inject constructor(
    private val appPreference: AppPreference,
    okHttpClientProvider: Provider<OkHttpClient>,
    private val gson: Gson,
) {
    private val client by lazy { okHttpClientProvider.get() }
    private var gemini: GeminiAiProvider? = null

    suspend fun refreshProviders() {
        appPreference.aiProviders.firstOrNull()?.let { providers ->
            providers.forEach { (provider, key) ->
                if (!key.isNullOrBlank()) {
                    when (provider) {
                        AiProvider.GEMINI -> gemini = GeminiAiProvider(client, gson, key)
                    }
                }
            }
        }
    }


    suspend fun transform(
        prompt: String,
        lyrics: String,
        aiProvider: AiProvider,
    ): RequestState {
        if (!aiProvider.isConfigured()) {
            return RequestState.Failure("No API key provided")
        }
        val chosenModel = appPreference.getPreference(AppPreference.getKeyForModel(aiProvider))
        val model = chosenModel ?: aiProvider.defaultModel
        val result = when (aiProvider) {
            AiProvider.GEMINI -> gemini!!.rewriteLyrics(prompt, lyrics, model)
        }
        return when (result) {
            Result.Cancelled -> RequestState.Idle
            is Result.Failure -> RequestState.Failure(result.error)
            is Result.Success -> {
                val response = result.response
                if (response.trim().uppercase() == "FAILED") {
                    Log.e(TAG, "transform: ${aiProvider.name} response is FAILED")
                    return RequestState.Failure("FAILED")
                }
                RequestState.Success(result.response)
            }
        }
    }

    private fun AiProvider.isConfigured(): Boolean {
        return when (this) {
            AiProvider.GEMINI -> gemini != null
        }
    }

    companion object {
        private const val TAG = "AiRewriteHelper"
    }
}
