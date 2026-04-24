package io.github.abhishekabhi789.lyricsforpoweramp.di

import android.content.Context
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.abhishekabhi789.lyricsforpoweramp.airewrite.AiRewriteHelper
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.LyricsSavingHelper
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.NotificationHelper
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.PlaybackHelper
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.PowerampApiHelper
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.StorageHelper
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.TaglibHelper
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModules {

    private const val CONNECTION_TIMEOUT = 10_000L
    private const val READ_TIMEOUT = 30_000L

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().run {
        connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
        readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
        build()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    fun provideTaglibHelper(@ApplicationContext context: Context) = TaglibHelper(context)

    @Provides
    fun provideLyricsSavingHelper(
        @ApplicationContext context: Context,
        appPreference: AppPreference,
        powerampApiHelper: PowerampApiHelper,
        taglibHelper: TaglibHelper
    ) =
        LyricsSavingHelper(context, appPreference, powerampApiHelper, StorageHelper, taglibHelper)

    @Provides
    fun providePlaybackHelper(@ApplicationContext context: Context) = PlaybackHelper(context)

    @Provides
    fun provideAiRewriteHelper(
        appPreference: AppPreference,
        okHttpClientProvider: Provider<OkHttpClient>,
        gson: Gson
    ) = AiRewriteHelper(appPreference, okHttpClientProvider, gson)

    @Provides
    fun provideNotificationHelper(
        @ApplicationContext context: Context,
        appPreference: AppPreference,
        @ApplicationScope scope: CoroutineScope
    ) =
        NotificationHelper(context, appPreference, scope)

}
