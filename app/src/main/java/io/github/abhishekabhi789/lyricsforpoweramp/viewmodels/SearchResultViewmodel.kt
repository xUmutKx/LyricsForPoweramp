package io.github.abhishekabhi789.lyricsforpoweramp.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.LyricsSavingHelper
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.LyricsSavingState
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsSendData
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Collections
import javax.inject.Inject

@HiltViewModel
class SearchResultViewmodel @Inject constructor(private val lyricsSavingHelper: LyricsSavingHelper) :
    ViewModel() {

    private var _searchResults = MutableStateFlow<List<Lyrics>>(Collections.emptyList())
    private var pendingSend: LyricsSendData? = null

    /** Search results as [List]<[Lyrics]>*/
    val searchResults = _searchResults.asStateFlow()

    private val _lyricsSavingState = MutableStateFlow(LyricsSavingState())
    val lyricsSavingState = _lyricsSavingState.asStateFlow()

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
        lyrics: Lyrics,
        lyricsType: LyricsType,
        markInstrumental: Boolean = false,
    ) {
        pendingSend = LyricsSendData(lyrics, lyricsType, markInstrumental)
        clearResultState()
        viewModelScope.launch {
            powerampId?.let { realId ->
                lyricsSavingHelper.saveLyrics(
                    filePath = filePath,
                    powerampId = realId,
                    lyrics = lyrics,
                    lyricsType = lyricsType,
                    markInstrumental = markInstrumental
                ).collect { state -> _lyricsSavingState.value = state }
            } ?: Log.e(TAG, "sendLyricsToPoweramp: Poweramp realId is null")
        }
    }

    fun retrySend(context: Context) {
        val request = pendingSend
        if (request != null) {
            sendLyricsToPoweramp(
                lyrics = request.lyrics,
                lyricsType = request.type,
                markInstrumental = request.markInstrumental
            )
        } else {
            Log.w(TAG, "retrySend: Nothing to retry")
        }
    }

    fun clearResultState() {
        _lyricsSavingState.value = LyricsSavingState()
    }

    companion object {
        private const val TAG = "SearchResultViewmodel"
    }
}
