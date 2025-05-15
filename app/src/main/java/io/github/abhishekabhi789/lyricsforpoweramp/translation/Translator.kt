package io.github.abhishekabhi789.lyricsforpoweramp.translation

import androidx.annotation.StringRes
import io.github.abhishekabhi789.lyricsforpoweramp.R

enum class Translator(@StringRes val nameRes: Int) {
    GEMINI(R.string.ai_gemini_name)
    ;

    companion object {
        fun getDefault() = GEMINI
    }
}
