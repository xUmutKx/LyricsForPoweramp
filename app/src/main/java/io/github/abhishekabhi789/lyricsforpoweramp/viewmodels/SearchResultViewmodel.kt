package io.github.abhishekabhi789.lyricsforpoweramp.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.PowerampApiHelper
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.SendLyricsState
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsSendData
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Collections

class SearchResultViewmodel : ViewModel() {

    private var _searchResults = MutableStateFlow<List<Lyrics>>(Collections.emptyList())
    private var pendingSend: LyricsSendData? = null

    /** Search results as [List]<[Lyrics]>*/
    val searchResults = _searchResults.asStateFlow()

    private val _sendLyricsState = MutableStateFlow(SendLyricsState())
    val sendLyricsState = _sendLyricsState.asStateFlow()

    var powerampId: Long? = null

    var filePath = ""
        private set

    fun setSearchResults(list: List<Lyrics>) {
        _searchResults.update { list }
    }

    fun setPowerampId(realId: Long) {
        powerampId = realId
    }

    fun setFilePath(path: String) {
        filePath = path
    }

    /** Will send the chosen lyrics to PowerAmp. Should call when have realId
     * @return [Boolean] indicating request attempt result*/
    fun sendLyricsToPoweramp(
        context: Context,
        lyrics: Lyrics,
        lyricsType: LyricsType,
        markInstrumental: Boolean = false,
    ) {
        pendingSend = LyricsSendData(lyrics, lyricsType, markInstrumental)
        clearResultState()
        viewModelScope.launch {
            powerampId?.let { realId ->
                PowerampApiHelper.sendLyrics(
                    context = context,
                    filePath = filePath,
                    powerampId = realId,
                    lyrics = lyrics,
                    lyricsType = lyricsType,
                    markInstrumental = markInstrumental
                ).collect { state -> _sendLyricsState.value = state }
            } ?: Log.e(TAG, "sendLyricsToPoweramp: Poweramp realId is null")
        }
    }

    fun retrySend(context: Context) {
        val request = pendingSend
        if (request != null) {
            sendLyricsToPoweramp(
                context = context,
                lyrics = request.lyrics,
                lyricsType = request.type,
                markInstrumental = request.markInstrumental
            )
        } else {
            Log.w(TAG, "retrySend: Nothing to retry")
        }
    }

    fun clearResultState() {
        _sendLyricsState.value = SendLyricsState()
    }

    companion object {
        private const val TAG = "SearchResultViewmodel"
    }
}
