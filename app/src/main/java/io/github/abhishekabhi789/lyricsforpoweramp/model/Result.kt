package io.github.abhishekabhi789.lyricsforpoweramp.model

sealed interface Result {
    data class Success(val response: String) : Result
    data class Failure(val error: String?) : Result
    object Cancelled : Result
}
