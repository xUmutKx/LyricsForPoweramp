package io.github.abhishekabhi789.lyricsforpoweramp.airewrite.openrouter.model

import com.google.gson.annotations.SerializedName

data class OpenRouterResponse(
    @SerializedName("model")
    val model: String,
    @SerializedName("choices")
    val choices: List<Choice>
) {
    data class Choice(
        @SerializedName("finish_reason")
        val finishReason: String,
        @SerializedName("message")
        val message: Message
    ) {
        data class Message(
            @SerializedName("content")
            val content: String
        )
    }
}
