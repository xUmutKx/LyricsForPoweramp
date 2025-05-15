package io.github.abhishekabhi789.lyricsforpoweramp.helpers


import com.google.gson.Gson
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object RequestHelper {
    private const val CONNECTION_TIMEOUT = 10_000L
    private const val READ_TIMEOUT = 30_000L
    val okHttpClient: OkHttpClient = OkHttpClient.Builder().run {
        connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
        readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
        build()
    }
    val gson: Gson = Gson()
}
