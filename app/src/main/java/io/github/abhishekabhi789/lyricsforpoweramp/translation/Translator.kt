package io.github.abhishekabhi789.lyricsforpoweramp.translation

import io.github.abhishekabhi789.lyricsforpoweramp.R

enum class Translator(val nameRes: Int, val key: String) {
    GEMINI(R.string.ai_gemini_name, "ai_key_gemini")
    ;

    companion object {
        fun getDefault() = GEMINI
    }
}
