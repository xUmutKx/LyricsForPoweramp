package io.github.abhishekabhi789.lyricsforpoweramp.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PreferenceModules {

    @Provides
    @Singleton
    @Named(AppPreference.OTHER_PREF)
    fun provideAppPreference(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(AppPreference.OTHER_PREF, Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    @Named(AppPreference.UI_PREF_NAME)
    fun provideUiPreference(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(AppPreference.UI_PREF_NAME, Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    @Named(AppPreference.FILTER_PREF_NAME)
    fun provideFilterPreference(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(AppPreference.FILTER_PREF_NAME, Context.MODE_PRIVATE)
    }
}
