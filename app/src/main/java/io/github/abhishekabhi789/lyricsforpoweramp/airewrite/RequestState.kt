package io.github.abhishekabhi789.lyricsforpoweramp.airewrite

sealed interface RequestState {
    object Idle : RequestState
    object Processing : RequestState
    data class Success<T>(val response: T) : RequestState
    data class Failure(val errorMessage: String? = null) : RequestState
}
