package io.github.abhishekabhi789.lyricsforpoweramp.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.LyricsSavingHelper
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.LyricsSavingState
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.StorageHelper
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.TaglibHelper
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsSendData
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import io.github.abhishekabhi789.lyricsforpoweramp.model.SearchResultData
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import io.github.abhishekabhi789.lyricsforpoweramp.utils.SearchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchResultViewmodel @Inject constructor(
    appPreference: AppPreference,
    private val lyricsSavingHelper: LyricsSavingHelper,
    private val taglibHelper: TaglibHelper,
    private val searchRepository: SearchRepository
) :
    ViewModel() {
    val appTheme = appPreference.appTheme
    private val _searchResultData = MutableStateFlow<SearchResultData?>(null)
    val searchResultData = _searchResultData.asStateFlow()

    private var pendingSend: LyricsSendData? = null

    private val _lyricsSavingState = MutableStateFlow(LyricsSavingState())
    val lyricsSavingState = _lyricsSavingState.asStateFlow()

    val preferredLyricsType = appPreference.preferredLyricsType
        .stateIn(viewModelScope, SharingStarted.Lazily, LyricsType.SYNCED)

    fun setSearchResultDataKey(key: String): Boolean {
        val data = searchRepository.getResult(key)
        if (data == null) {
            Log.e(TAG, "setSearchResultDataKey: no data found for key $key")
            return false
        }
        _searchResultData.value = data
        searchRepository.clearResult(key)
        return true
    }

    private var _tagLibSession = MutableStateFlow<TaglibHelper.TagLibSession?>(null)
    val tagLibSession = _tagLibSession.asStateFlow()
    fun prepareTaglibSession(
        path: String?,
        onError: (error: StorageHelper.Result) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (path == null) {
                Log.d(TAG, "prepareTaglibSession: closing taglib session")
                _tagLibSession.value?.closeSafely()
                _tagLibSession.value = null
                Log.d(TAG, "prepareTaglibSession: session closed")
            } else {
                Log.d(TAG, "prepareTaglibSession: preparing taglib session")
                _tagLibSession.value = taglibHelper.getTaglibSession(path, onError)
            }
        }
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
        val data = _searchResultData.value
        if (data == null) {
            Log.w(TAG, "sendLyricsToPoweramp: searchResultData is null, aborting")
            return
        }
        viewModelScope.launch {
            data.powerampId?.let { realId ->
                lyricsSavingHelper.saveLyrics(
                    filePath = data.filepath,
                    powerampId = realId,
                    lyrics = lyrics,
                    lyricsType = lyricsType,
                    markInstrumental = markInstrumental
                ).collect { state -> _lyricsSavingState.value = state }
            } ?: Log.e(TAG, "sendLyricsToPoweramp: Poweramp realId is null")
        }
    }

    fun retrySend() {
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
