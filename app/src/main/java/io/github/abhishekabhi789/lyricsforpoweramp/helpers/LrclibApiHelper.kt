package io.github.abhishekabhi789.lyricsforpoweramp.helpers

import android.util.Log
import com.google.gson.Gson
import io.github.abhishekabhi789.lyricsforpoweramp.BuildConfig
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.Track
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.resume


/**Helper to interacts with LRCLIB*/
class LrclibApiHelper @Inject constructor(
    private val gson: Gson,
    private val okHttpClientProvider: Provider<OkHttpClient>,
    private val appPreference: AppPreference
) {
    private val client by lazy { okHttpClientProvider.get() }

    private sealed class ApiResponse {
        data class Success(val data: String) : ApiResponse()
        data class Failure(val error: Error) : ApiResponse() {
            fun errorAsResult(): Result = Result.Failure(error)
        }
    }

    sealed class Result {
        data class Success(val data: List<Lyrics>) : Result()
        data class Failure(val error: Error) : Result()
    }


    private suspend fun makeApiRequest(requestUrl: HttpUrl): ApiResponse {
        val request = Request.Builder().apply {
            url(requestUrl)
            headers(requestHeader)
            get()
        }.build()

        return try {
            suspendCancellableCoroutine { continuation ->
                val call = client.newCall(request)
                continuation.invokeOnCancellation { call.cancel() }
                call.enqueue(object : okhttp3.Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        val error = when {
                            call.isCanceled() -> Error.CANCELLED
                            e is java.net.SocketTimeoutException -> Error.TIMEOUT
                            else -> Error.NETWORK_ERROR
                        }
                        continuation.resume(ApiResponse.Failure(error))
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use { response ->
                            when (response.code) {
                                HttpURLConnection.HTTP_OK -> {
                                    val data = runCatching { response.body.string() }.getOrNull()
                                    if (data != null) {
                                        continuation.resume(ApiResponse.Success(data))
                                    } else {
                                        continuation.resume(ApiResponse.Failure(Error.EMPTY_RESPONSE))
                                    }
                                }

                                HttpURLConnection.HTTP_NOT_FOUND -> {
                                    val errorMsg = response.message
                                    Log.i(TAG, "makeApiRequest: no result $errorMsg")
                                    continuation.resume(ApiResponse.Failure(Error.NO_RESULTS))
                                }

                                else -> {
                                    val errorMsg =
                                        "Request Failed, HTTP ${response.code}: ${response.message}"
                                    Log.e(TAG, "makeApiRequest: $errorMsg")
                                    continuation.resume(ApiResponse.Failure(Error.NETWORK_ERROR))
                                }
                            }
                        }
                    }
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected Exception during network request: ${e.message}", e)
            ApiResponse.Failure(Error.EXCEPTION)
        }
    }


    suspend fun getLyricsForTrack(track: Track): Result {
        val requestParams = buildMap {
            put("track_name", track.trackName)
            track.artistName?.let { put("artist_name", it) }
            track.albumName?.let { put("album_name", it) }
            track.duration?.takeIf { it > 0 }?.let { put("duration", it.toString()) }
        }

        val requestUrl = runCatching { makeRequestUrl(Method.GET, requestParams) }.getOrElse {
            return Result.Failure(Error.URL_ERROR)
        }
        return when (val response = makeApiRequest(requestUrl)) {
            is ApiResponse.Failure -> {
                Log.e(TAG, "getLyricsForTracks: error ${response.error}")
                response.errorAsResult()
            }

            is ApiResponse.Success -> {
                runCatching {
                    val lyrics = gson.fromJson(response.data, Lyrics::class.java)
                        ?: return Result.Failure(Error.EMPTY_RESPONSE)
                    //get method returns a single lyrics result, so putting it in a list to fit in success
                    Result.Success(listOf(lyrics))
                }.getOrElse { Result.Failure(Error.PROCESSING_ERROR) }
            }
        }
    }

    suspend fun searchLyricsForQuery(query: String): Result {
        val requestParams = mapOf("q" to query)
        return searchLyrics(requestParams)
    }

    suspend fun searchLyricsForTrack(track: Track): Result {
        val requestParam = buildMap {
            put("track_name", track.trackName)
            track.artistName?.let { put("artist_name", it) }
            track.albumName?.let { put("album_name", it) }
        }
        return searchLyrics(requestParam)
    }

    /** Performs search for the given input.
     * @see <a href="https://lrclib.net/docs#:~:text=s%20example%20response.-,Search%20for%20lyrics%20records,-GET">
     *     LRCLIB#Search for lyrics records</a>*/
    private suspend fun searchLyrics(requestParams: Map<String, String>): Result {

        val requestUrl = runCatching { makeRequestUrl(Method.SEARCH, requestParams) }.getOrElse {
            return Result.Failure(Error.URL_ERROR)
        }

        return when (val response = makeApiRequest(requestUrl)) {
            is ApiResponse.Success -> {
                val lyricsList = runCatching {
                    gson.fromJson(response.data, Array<Lyrics>::class.java)
                }.getOrElse {
                    Log.e(TAG, "parseSearchResponse: failed to parse response", it)
                    return Result.Failure(Error.PROCESSING_ERROR)
                }

                if (!lyricsList.isNullOrEmpty()) {
                    val validLyrics = lyricsList.filter {
                        it.plainLyrics != null || it.syncedLyrics != null || it.instrumental == true
                    }
                    Log.d(TAG, "searchLyricsForTrack: found ${validLyrics.size} results")
                    Result.Success(validLyrics)
                } else {
                    Log.e(TAG, "searchLyricsForTrack: no result found")
                    val error =
                        if (lyricsList == null) Error.PROCESSING_ERROR else Error.NO_RESULTS
                    Result.Failure(error)
                }
            }

            is ApiResponse.Failure -> response.errorAsResult()
        }
    }

    private fun makeRequestUrl(method: Method, params: Map<String, String>): HttpUrl {
        val selectedApiUrl = appPreference.selectedLrcLibInstanceUrl.value
        val apiUrl = selectedApiUrl.toHttpUrlOrNull()
            ?: throw MalformedURLException("Invalid URL: $selectedApiUrl")
        return apiUrl.newBuilder().apply {
            addPathSegment(method.path)
            params.forEach { (key, value) ->
                addQueryParameter(key, value)
            }
        }.build()
    }

    enum class Error(val errMsgResId: Int) {
        CANCELLED(R.string.error_canceled),
        EMPTY_RESPONSE(R.string.error_empty_response),
        NETWORK_ERROR(R.string.error_network),
        TIMEOUT(R.string.error_timeout),
        EXCEPTION(R.string.error_exception),
        URL_ERROR(R.string.error_preparing_url),
        NO_RESULTS(R.string.error_no_results),
        PROCESSING_ERROR(R.string.error_processing_error)
    }

    enum class Method(val path: String) {
        GET("get"), SEARCH("search")
    }

    companion object {
        private const val TAG = "LrclibApiHelper"
        private val userAgent = buildString {
            append(BuildConfig.APPLICATION_ID)
            append(" ")
            append(BuildConfig.VERSION_NAME)
            append(" ")
            append(BuildConfig.GITHUB_REPO_URL)
        }
        private val requestHeader = Headers.Builder()
            .add("Accept", "application/json")
            .add("User-Agent", userAgent)
            .build()
    }
}
