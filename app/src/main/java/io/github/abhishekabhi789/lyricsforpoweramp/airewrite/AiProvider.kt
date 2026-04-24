package io.github.abhishekabhi789.lyricsforpoweramp.airewrite

import io.github.abhishekabhi789.lyricsforpoweramp.R

enum class AiProvider(val nameRes: Int, val key: String) {
    GEMINI(R.string.ai_gemini_name, "ai_key_gemini")
    ;

    companion object {
        fun getDefault() = GEMINI
    }
}
