package io.github.abhishekabhi789.lyricsforpoweramp.airewrite

import io.github.abhishekabhi789.lyricsforpoweramp.R

enum class AiProvider(val nameRes: Int, val key: String) {
    GEMINI(R.string.ai_gemini_name, "ai_key_gemini") {
        override val availableModels: List<String> = listOf(
            "gemini-2.5-pro",
            "gemini-2.5-flash",
            "gemini-2.0-flash",
            "gemini-2.5-flash-lite",
            "gemini-3-flash-preview",
            "gemini-3.1-flash-lite-preview"
        )
        override val defaultModel: String = "gemini-2.5-pro"
    };

    abstract val availableModels: List<String>
    abstract val defaultModel: String

    companion object {
        fun getDefault() = GEMINI
    }
}
