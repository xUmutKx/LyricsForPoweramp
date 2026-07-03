package io.github.abhishekabhi789.lyricsforpoweramp.airewrite.openrouter.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenRouterRequest(
    @SerialName("model")
    val model: String,
    @SerialName("messages")
    val messages: List<Message>
) {
    @Serializable
    data class Message(
        @SerialName("role")
        val role: String,
        @SerialName("content")
        val content: String
    )

    companion object {
        fun getInstance(
            model: String,
            prompt: String,
            systemInstruction: String? = null
        ): OpenRouterRequest {
            val systemMessage = systemInstruction?.let { instruction ->
                Message(role = "system", content = instruction)
            }
            val userMessage = Message(role = "user", content = prompt)
            return OpenRouterRequest(
                model = model,
                messages = listOfNotNull(systemMessage, userMessage)
            )
        }
    }
}
