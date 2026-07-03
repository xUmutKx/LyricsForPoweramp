package io.github.abhishekabhi789.lyricsforpoweramp.airewrite.openrouter.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenRouterResponse(
    @SerialName("model")
    val model: String,
    @SerialName("choices")
    val choices: List<Choice>
) {
    @Serializable
    data class Choice(
        @SerialName("finish_reason")
        val finishReason: String,
        @SerialName("message")
        val message: Message
    ) {
        @Serializable
        data class Message(
            @SerialName("content")
            val content: String
        )
    }
}
