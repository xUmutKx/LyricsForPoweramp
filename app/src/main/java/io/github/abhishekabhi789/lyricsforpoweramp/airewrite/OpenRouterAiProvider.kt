package io.github.abhishekabhi789.lyricsforpoweramp.airewrite

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.airewrite.openrouter.model.OpenRouterRequest
import io.github.abhishekabhi789.lyricsforpoweramp.airewrite.openrouter.model.OpenRouterResponse
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
import okio.IOException
import java.net.HttpURLConnection
import kotlin.coroutines.resume

class OpenRouterAiProvider(
    private val client: OkHttpClient,
    private val gson: Gson,
    private val apiKey: String,
) : AiProviderRepository {

    override val nameResId: Int = R.string.ai_open_router_name

    override val instructions: String = """
            This prompt is generated programmatically from a lyrics app for Poweramp music player for android.
            Assume your role as a lyrics processing engine.
            You must transform lyrics based on user instructions (translate, censor, or style change).
            
            For synced lyrics(LRC), Poweramp supports dual-LRC format translation like below.
                [# Original Lyrics]
                [TIMESTAMP 1] Original Line 1
                [TIMESTAMP 2] Original Line 2
            
                [# Translated {LanguageName}]
                [TIMESTAMP 1] Translated Line 1
                [TIMESTAMP 2] Translated Line 2
                
            For plain lyrics follow below format:
                Original Line 1
                Translated Line 1
                
                Original Line 2
                Translated Line 2
            
            STRICT FORMATTING RULES:
            - If user asks to keep original lyrics, use the PowerAmp dual-LRC format, else replace original lyrics.
            - When writing with dual-LRC format, replace '{LanguageName}' in the header with the actual target language (e.g., [# Translated Romaji] or [# Translated Spanish]).
            - If the input is synchronized (LRC), you MUST preserve every [mm:ss.xx] timestamp exactly. Never merge lines or skip timestamps.
            - NO CONVERSATION. No intros, no markdown, no code blocks, and no explanations. Output raw text only.
            - Metadata: Leave tags like [ar:], [al:], [ti:] etc., exactly as they appear in the source.
            - Censorship: Do not translate explicit words; replace them with original-language asterisks (e.g., f***).
            - FAILURE PROTOCOL: If the prompt is irrelevant to lyrics, harmful, or impossible to fulfill, reply exactly: FAILED
            - If user asks to convert plain lyrics to synchronized lyrics, replay exactly: FAILED
             """

    override suspend fun rewriteLyrics(userPrompt: String, lyrics: String, model: String): Result {
        val prompt = """
            User instruction:
            $userPrompt
                        
            Lyrics to process:
            $lyrics

        """.trimIndent()
        return generateResponse(prompt, model)
    }

    override suspend fun generateResponse(
        prompt: String,
        model: String
    ): Result {
        return try {
            val url = BASE_URL.toHttpUrl()
            val request = Request.Builder().run {
                url(url)
                addHeader("Content-Type", "application/json")
                addHeader("Authorization", "Bearer $apiKey")
                post(buildRequestBody(prompt, model))
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
                                    //https://openrouter.ai/docs/api/reference/errors-and-debugging#error-codes
                                    400 -> {
                                        continuation.resume(Result.Failure("Bad Request"))
                                    }

                                    401 -> {
                                        continuation.resume(Result.Failure("Invalid credentials"))
                                    }

                                    402 -> {
                                        continuation.resume(Result.Failure("Insufficient API credits"))
                                    }

                                    403 -> {
                                        continuation.resume(Result.Failure("Forbidden"))
                                    }

                                    408 -> {
                                        continuation.resume(Result.Failure("Timed out"))
                                    }

                                    429 -> {
                                        continuation.resume(Result.Failure("Limit reached. Try changing the model or provider."))
                                    }

                                    502 -> {
                                        continuation.resume(Result.Failure("Service error"))
                                    }

                                    503 -> {
                                        continuation.resume(Result.Failure("Service Unavailable"))
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

    override fun buildRequestBody(prompt: String, model: String?): RequestBody {
        val promptObj = OpenRouterRequest.getInstance(
            model = model ?: throw IllegalArgumentException("OpenRouter requires a model name"),
            prompt = prompt,
            systemInstruction = instructions
        )
        return gson.toJson(promptObj).toRequestBody("application/json".toMediaType())
    }

    override fun parseResponse(response: String): Result {
        return try {
            val openRouterResponse = gson.fromJson(response, OpenRouterResponse::class.java)
            val choice = openRouterResponse.choices.first()
            when (choice.finishReason.lowercase()) {
                "stop" -> {
                    Result.Success(choice.message.content)
                }

                "length" -> {
                    Log.e(TAG, "parseResponse: Context window or max_tokens limit reached")
                    Result.Failure("Response was cut off due to length limits")
                }

                "content_filter" -> {
                    Log.e(TAG, "parseResponse: Content blocked by safety guardrails")
                    Result.Failure("Content blocked by safety filters")
                }

                "tool_calls" -> {
                    Log.e(TAG, "parseResponse: Model requested a tool execution")
                    Result.Failure("Function calling is not supported in this handler")
                }

                "error" -> {
                    Log.e(TAG, "parseResponse: Downstream provider execution failed")
                    Result.Failure("Provider error during generation")
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
        private const val TAG = "OpenRouterAiProvider"
        private const val BASE_URL = "https://openrouter.ai/api/v1/chat/completions"
    }
}
