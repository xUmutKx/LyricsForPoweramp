package io.github.abhishekabhi789.lyricsforpoweramp.airewrite.gemini.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeminiResponse(
    @SerialName("candidates")
    val candidates: List<Candidate>
) {
    @Serializable
    data class Candidate(
        @SerialName("content")
        val content: Content,
        @SerialName("finishReason")
        val finishReason: String
    ) {
        @Serializable
        data class Content(
            @SerialName("parts")
            val parts: List<Part>,
            @SerialName("role")
            val role: String
        ) {
            @Serializable
            data class Part(
                @SerialName("text")
                val text: String
            )
        }
    }
}
