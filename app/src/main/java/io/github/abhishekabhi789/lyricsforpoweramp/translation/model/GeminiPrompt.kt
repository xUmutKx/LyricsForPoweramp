package io.github.abhishekabhi789.lyricsforpoweramp.translation.model

import com.google.gson.annotations.SerializedName

data class GeminiPrompt(
    @SerializedName("contents")
    val contents: List<Content>
) {
    data class Content(
        @SerializedName("parts")
        val parts: List<Part>
    ) {
        data class Part(
            @SerializedName("text")
            val text: String
        )
    }

    companion object {
        fun getInstance(prompt: String): GeminiPrompt {
            return GeminiPrompt(listOf(Content(listOf(Content.Part(prompt)))))
        }
    }
}
