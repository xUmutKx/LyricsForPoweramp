package io.github.abhishekabhi789.lyricsforpoweramp.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.abhishekabhi789.lyricsforpoweramp.utils.appDataStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PreferenceModule {
    @Provides
    @Singleton
    fun provideAppDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.appDataStore
}
