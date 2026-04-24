package io.github.abhishekabhi789.lyricsforpoweramp.airewrite.model

import com.google.gson.annotations.SerializedName

data class GeminiRequest(
    @SerializedName("system_instruction")
    val systemInstruction: SystemInstruction? = null,
    @SerializedName("contents")
    val userInstruction: UserInstruction

) {
    data class SystemInstruction(
        @SerializedName("parts")
        val parts: Content.Part
    )

    data class UserInstruction(
        @SerializedName("parts")
        val parts: List<Content.Part>
    )

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
