package io.github.abhishekabhi789.lyricsforpoweramp.airewrite.gemini.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeminiRequest(
    @SerialName("system_instruction")
    val systemInstruction: SystemInstruction? = null,
    @SerialName("contents")
    val userInstruction: UserInstruction

) {
    @Serializable
    data class SystemInstruction(
        @SerialName("parts")
        val parts: Content.Part
    )

    @Serializable
    data class UserInstruction(
        @SerialName("parts")
        val parts: List<Content.Part>
    )

    @Serializable
    data class Content(
        @SerialName("parts")
        val parts: List<Part>
    ) {
        @Serializable
        data class Part(
            @SerialName("text")
            val text: String
        )
    }

    companion object {
        fun getInstance(prompt: String, systemInstruction: String? = null): GeminiRequest {
            val systemContent = systemInstruction?.let {
                SystemInstruction(Content.Part(it))
            }
            val userContent = UserInstruction(listOf(Content.Part(prompt)))
            return GeminiRequest(
                systemInstruction = systemContent,
                userInstruction = userContent
            )
        }
    }
}
