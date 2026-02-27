package io.github.abhishekabhi789.lyricsforpoweramp.viewmodels

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxmpz.poweramp.player.PowerampAPI
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.LrclibApiHelper
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.PowerampApiHelper
import io.github.abhishekabhi789.lyricsforpoweramp.model.InputState
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.SearchResultData
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import io.github.abhishekabhi789.lyricsforpoweramp.utils.SearchRepository
import io.github.abhishekabhi789.lyricsforpoweramp.workers.LyricsRequestWorker.Companion.MANUAL_SEARCH_ACTION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val lrclibApiHelper: LrclibApiHelper,
    private val appPreference: AppPreference,
    private val powerampApiHelper: PowerampApiHelper,
    private val searchRepository: SearchRepository
) : ViewModel() {

    val appTheme = appPreference.appTheme

    val firstTimeInfo = appPreference.firstTimeInfoShown
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun setFirstTimeInfoShown(value: Boolean) {
        viewModelScope.launch {
            appPreference.setPreference(AppPreference.FIRST_TIME_INFO_SHOWN, value)
        }
    }

    val showNotification = appPreference.notifyOnRequestFailure
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun setShowNotification(enabled: Boolean) {
        viewModelScope.launch {
            appPreference.setPreference(AppPreference.SHOW_LYRICS_REQUEST_NOTIFICATION, enabled)
        }
    }

    private val _inputState = MutableStateFlow(InputState())

    /** Carries inputs from PowerAmp or user, which is an instance of [InputState] */
    val inputState = _inputState.asStateFlow()

    private var _isInputValid = MutableStateFlow(true)

    /** Stores if input is valid for a search operation */
    val isInputValid = _isInputValid.asStateFlow()

    private val _searchErrorFlow = MutableSharedFlow<LrclibApiHelper.Error>()

    /** Carries errors related search job*/
    val searchErrorFlow: SharedFlow<LrclibApiHelper.Error> = _searchErrorFlow

    private var _searchResultKey = MutableSharedFlow<String>()

    /** key of search result stored in [SearchRepository]>*/
    val searchResultKeyFlow: SharedFlow<String> = _searchResultKey

    private var _isSearching = MutableStateFlow(false)

    /** Status about search */
    val isSearching = _isSearching.asStateFlow()

    /* Holds the current search job, inorder to cancel it if needed.*/
    private var searchJob: Job? = Job()

    fun updateLaunchIntent(intent: Intent) {
        when (intent.action) {
            PowerampAPI.Lyrics.ACTION_LYRICS_LINK, MANUAL_SEARCH_ACTION -> {
                viewModelScope.launch {
                    powerampApiHelper.makeTrack(intent)?.let { track ->
                        updateInputState(
                            InputState(
                                queryString = track.trackName,
                                queryTrack = track,
                                searchMode = if (track.artistName.isNullOrEmpty() && track.albumName.isNullOrEmpty())
                                    InputState.SearchMode.Coarse else InputState.SearchMode.Fine
                            )
                        )
                    }
                }
            }
        }
    }

    /** Updates [inputState]*/
    fun updateInputState(newState: InputState) {
        _inputState.value = newState
    }

    /** Ensures user inputs are suffice to perform search */
    private fun isValidInput(): Boolean {
        return when (_inputState.value.searchMode) {
            InputState.SearchMode.Coarse -> _inputState.value.queryString.isNotBlank()
            InputState.SearchMode.Fine -> inputState.value.queryTrack.trackName.isNotBlank()
        }
    }

    /** Abort search*/
    fun abortSearch() {
        searchJob?.cancel()
        Log.i(TAG, "abortSearch: aborting lyrics search")
    }

    /** Performs search for the [inputState]*/
    fun performSearch() {
        val isInputValid = isValidInput()
        if (!isInputValid) {
            Log.e(TAG, "performSearch: invalid input ${_inputState.value}")
            updateInputValidStatus(false)
            return
        }
        val inputState = _inputState.value
        searchRepository.getKeyForInputState(inputState)?.let {
            Log.d(TAG, "performSearch: saved result found for input")
            viewModelScope.launch { _searchResultKey.emit(it) }
            return
        }

        emitSearchStatus(true)
        searchJob?.cancel()
        searchJob = null

        searchJob = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    searchJob?.ensureActive()
                    val result = when (inputState.searchMode) {
                        InputState.SearchMode.Coarse -> {
                            lrclibApiHelper.searchLyricsForQuery(inputState.queryString)
                        }

                        InputState.SearchMode.Fine -> {
                            lrclibApiHelper.searchLyricsForTrack(inputState.queryTrack)
                        }
                    }
                    when (result) {
                        is LrclibApiHelper.Result.Success -> emitSearchResult(result.data)
                        is LrclibApiHelper.Result.Failure -> {
                            if (searchJob?.isCancelled == false) {
                                // don't send cancellation error from here
                                emitSearchError(result.error)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "performSearch: exception occurred", e)
                }
            }
        }
        searchJob?.invokeOnCompletion {
            if (searchJob?.isCancelled == true) {
                emitSearchError(LrclibApiHelper.Error.CANCELLED)
            }
            emitSearchStatus(false)
            Log.d(TAG, "performSearch: search job ended")
            searchJob = null
        }
    }

    private fun emitSearchStatus(isSearching: Boolean) {
        _isSearching.value = isSearching
    }

    private fun emitSearchError(error: LrclibApiHelper.Error) {
        viewModelScope.launch { _searchErrorFlow.emit(error) }
    }

    private fun emitSearchResult(result: List<Lyrics>) {
        val inputState = _inputState.value
        viewModelScope.launch {
            if (searchJob?.isCancelled == false) {
                val resultData = SearchResultData(
                    powerampId = inputState.queryTrack.realId,
                    filepath = inputState.queryTrack.filePath,
                    results = result,
                    trackDuration = inputState.queryTrack.duration
                )
                val key = searchRepository.saveResultData(inputState, resultData)
                _searchResultKey.emit(key)
            }
        }
    }

    private fun updateInputValidStatus(isInputValid: Boolean) {
        Log.d(TAG, "updateInputValidStatus: updating validity $isInputValid")
        _isInputValid.value = isInputValid
    }

    /** Call this when after updating the mandatory fields to clear the error*/
    fun clearInvalidInputError() {
        updateInputValidStatus(true)
    }

    companion object {
        private const val TAG = "MainActivityViewModel"
    }
}
