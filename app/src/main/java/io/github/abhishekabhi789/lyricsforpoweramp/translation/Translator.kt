package io.github.abhishekabhi789.lyricsforpoweramp.translation

import android.content.Context
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference

enum class Translator(val nameRes: Int) {
    GEMINI(R.string.ai_gemini_name)
    ;

    fun isConfigured(context: Context): Boolean {
        return AppPreference.getTranslationApiKey(context, this).isNotBlank()
    }

    companion object {
        fun getDefault() = GEMINI
    }
}
