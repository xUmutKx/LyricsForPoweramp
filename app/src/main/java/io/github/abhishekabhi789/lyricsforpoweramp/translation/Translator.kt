package io.github.abhishekabhi789.lyricsforpoweramp.translation

import io.github.abhishekabhi789.lyricsforpoweramp.R

enum class Translator(val nameRes: Int) {
    GEMINI(R.string.ai_gemini_name)
    ;

    companion object {
        fun getDefault() = GEMINI
    }
}
