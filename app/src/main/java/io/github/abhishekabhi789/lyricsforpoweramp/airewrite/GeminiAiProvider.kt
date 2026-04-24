package io.github.abhishekabhi789.lyricsforpoweramp.airewrite

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.airewrite.model.GeminiRequest
import io.github.abhishekabhi789.lyricsforpoweramp.airewrite.model.GeminiResponse
import io.github.abhishekabhi789.lyricsforpoweramp.model.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.net.HttpURLConnection
import kotlin.coroutines.resume

class GeminiAiProvider(
    private val client: OkHttpClient,
    private val gson: Gson,
    private val apiKey: String
) : AiProviderRepository {

    override val nameResId: Int = R.string.ai_gemini_name

    override val instructions: String = """
            This prompt is generated programmatically from a lyrics app for Poweramp music player for android.
            Assume your role as a lyrics processing engine.
            You must transform lyrics based on user instructions (translate, censor, or style change).
            
            For synced lyrics(LRC), Poweramp supports dual-LRC format translation like below.
                [# Original Lyrics]
                [TIMESTAMP] Original Text
            
                [# Translated {LanguageName}]
                [TIMESTAMP] Translated Text
                
            For plain lyrics follow below format:
                Original Line 1
                Translated Line 1
                
                Original Line 2
                Translated Line 2
            
            STRICT FORMATTING RULES:
            - If the user prefers to keep original lyrics along with translation, use the PowerAmp dual-LRC format               
            - When writing with dual-LRC format, replace '{LanguageName}' in the header with the actual target language (e.g., [# Translated Romaji] or [# Translated Spanish]).
            - If the input is synchronized (LRC), you MUST preserve every [mm:ss.xx] timestamp exactly. Never merge lines or skip timestamps.
            - NO CONVERSATION. No intros, no markdown, no code blocks, and no explanations. Output raw text only.
            - Metadata: Leave tags like [ar:], [al:], [ti:] etc., exactly as they appear in the source.
            - Censorship: Do not translate explicit words; replace them with original-language asterisks (e.g., f***).
            - FAILURE PROTOCOL: If the prompt is irrelevant to lyrics, harmful, or impossible to fulfill, reply exactly: FAILED
            - If user asks to convert plain lyrics to synchronized lyrics, replay exactly: FAILED
             """

    override suspend fun rewriteLyrics(userPrompt: String, lyrics: String): Result {
        val prompt = """
            User instruction:
            $userPrompt
                        
            Lyrics to process:
            $lyrics

        """.trimIndent()
        return generateResponse(prompt)
    }

    override suspend fun generateResponse(prompt: String): Result {
        return try {
            val url = APIURL.toHttpUrl().newBuilder()
                .addQueryParameter("key", apiKey)
                .build()
            val request = Request.Builder().run {
                url(url)
                addHeader("Content-Type", "application/json")
                post(buildRequestBody(prompt))
                build()
            }
            withContext(Dispatchers.IO) {
                suspendCancellableCoroutine { continuation ->
                    val call = client.newCall(request)
                    continuation.invokeOnCancellation {
                        Log.i(TAG, "generateResponse: cancellation invoked")
                        call.cancel()
                    }
                    call.enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            if (call.isCanceled()) {
                                Log.i(TAG, "generateResponse - onFailure: cancelled")
                                continuation.resume(Result.Cancelled)
                            } else {
                                Log.e(TAG, "generateResponse - onFailure: failed", e)
                                continuation.resume(Result.Failure(e.message ?: "Unknown error"))
                            }
                        }

                        override fun onResponse(call: Call, response: Response) {
                            response.use {
                                when (response.code) {
                                    HttpURLConnection.HTTP_OK -> {
                                        val output = parseResponse(response.body.string())
                                        continuation.resume(output)
                                    }

                                    else -> {
                                        val errMsg =
                                            "Request Failed, HTTP ${response.code}: ${response.message}"
                                        Log.e(TAG, "onResponse: error on request $errMsg")
                                        continuation.resume(Result.Failure(errMsg))
                                    }
                                }
                            }
                        }
                    })
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "generateResponse: exception", e)
            Result.Failure("Exception ${e.message}")
        }
    }

    override fun buildRequestBody(prompt: String): RequestBody {
        val promptObj = GeminiRequest.getInstance(prompt = prompt, systemInstruction = instructions)
        return gson.toJson(promptObj).toRequestBody("application/json".toMediaType())
    }

    override fun parseResponse(response: String): Result {
        return try {
            val geminiResponse = gson.fromJson(response, GeminiResponse::class.java)
            val candidate = geminiResponse.candidates.firstOrNull()
            when (candidate?.finishReason) {
                "STOP" -> {
                    candidate.content.parts.first().text.let { Result.Success(it) }
                }

                "SAFETY" -> {
                    Log.e(TAG, "parseResponse: Content blocked by safety filters")
                    Result.Failure("Content blocked by safety filters")
                }

                "RECITATION" -> {
                    Log.e(TAG, "parseResponse: Copyright protection triggered")
                    Result.Failure("Copyright protection triggered")
                }

                else -> {
                    Log.e(TAG, "parseResponse: generation failed or no content")
                    Result.Failure("Failed to generate response")
                }
            }
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "parseResponse: JsonSyntaxException", e)
            Result.Failure("Failed to process response")

        } catch (e: Exception) {
            Log.e(TAG, "parseResponse: exception", e)
            Result.Failure("unknown error")
        }
    }

    companion object {
        private const val TAG = "GeminiAiProvider"
        private const val APIURL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
    }
}
