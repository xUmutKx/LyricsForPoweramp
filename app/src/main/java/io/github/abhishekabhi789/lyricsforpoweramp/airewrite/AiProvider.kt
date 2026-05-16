package io.github.abhishekabhi789.lyricsforpoweramp.airewrite

import io.github.abhishekabhi789.lyricsforpoweramp.R

enum class AiProvider(val nameRes: Int, val key: String) {
    //never change any keys
    GEMINI(R.string.ai_gemini_name, "ai_key_gemini") {
        override val availableModels: List<String> = listOf(
            "gemini-2.5-flash",
            "gemini-2.5-pro",
            "gemini-2.0-flash",
            "gemini-2.5-flash-lite",
            "gemini-3-flash-preview",
            "gemini-3.1-flash-lite-preview"
        )
    },
    OPENROUTER(R.string.ai_open_router_name, "ai_openrouter") {
        override val availableModels: List<String> = listOf(
            "openrouter/free",
            "openai/gpt-oss-120b:free",
            "openai/gpt-oss-20b:free",
            "nvidia/nemotron-3-super-120b-a12b:free",
            "openrouter/owl-alpha"
        )
    }
    ;

    abstract val availableModels: List<String>
    val defaultModel: String get() = availableModels.first()

    companion object {
        fun getDefault() = GEMINI
    }
}
