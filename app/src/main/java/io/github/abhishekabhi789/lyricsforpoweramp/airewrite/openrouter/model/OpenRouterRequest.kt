package io.github.abhishekabhi789.lyricsforpoweramp.airewrite.openrouter.model

import com.google.gson.annotations.SerializedName

data class OpenRouterRequest(
    @SerializedName("model")
    val model: String,
    @SerializedName("messages")
    val messages: List<Message>
) {
    data class Message(
        @SerializedName("role")
        val role: String,
        @SerializedName("content")
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
