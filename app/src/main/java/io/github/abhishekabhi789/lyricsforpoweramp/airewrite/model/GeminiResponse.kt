package io.github.abhishekabhi789.lyricsforpoweramp.airewrite.model

import com.google.gson.annotations.SerializedName

data class GeminiResponse(
    @SerializedName("candidates")
    val candidates: List<Candidate>
) {
    data class Candidate(
        @SerializedName("content")
        val content: Content,
        @SerializedName("finishReason")
        val finishReason: String
    ) {
        data class Content(
            @SerializedName("parts")
            val parts: List<Part>,
            @SerializedName("role")
            val role: String
        ) {
            data class Part(
                @SerializedName("text")
                val text: String
            )
        }
    }
}
