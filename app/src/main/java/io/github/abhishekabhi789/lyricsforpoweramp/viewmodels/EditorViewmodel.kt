package io.github.abhishekabhi789.lyricsforpoweramp.viewmodels

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxmpz.poweramp.player.PowerampAPI
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.PlaybackHelper
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.PowerampApiHelper
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.RequestHelper
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.SendLyricsState
import io.github.abhishekabhi789.lyricsforpoweramp.model.EditorInputState
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import io.github.abhishekabhi789.lyricsforpoweramp.model.Timestamp
import io.github.abhishekabhi789.lyricsforpoweramp.translation.RequestState
import io.github.abhishekabhi789.lyricsforpoweramp.translation.TranslationHelper
import io.github.abhishekabhi789.lyricsforpoweramp.translation.Translator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditorViewmodel(
    private val playbackHelper: PlaybackHelper,
    private val translationHelper: TranslationHelper
) : ViewModel() {
    private var isInitialized = false
    private val undoStack = ArrayDeque<EditorInputState>(50)
    private val redoStack = ArrayDeque<EditorInputState>(50)

    val canUndo: MutableStateFlow<Boolean> = MutableStateFlow(undoStack.isNotEmpty())
    val canRedo: MutableStateFlow<Boolean> = MutableStateFlow(redoStack.isNotEmpty())

    private val _inputState = MutableStateFlow(EditorInputState())
    val inputState: StateFlow<EditorInputState> = _inputState.asStateFlow()

    private var powerampId = PowerampAPI.NO_ID

    private lateinit var lyrics: Lyrics

    private val _filepath = MutableStateFlow("")
    val filePath = _filepath.asStateFlow()

    private val _targetLanguage: MutableStateFlow<String?> = MutableStateFlow(null)
    val targetLanguage = _targetLanguage.asStateFlow()

    private val _chosenTranslator: MutableStateFlow<Translator> =
        MutableStateFlow(Translator.getDefault())
    val chosenTranslator = _chosenTranslator.asStateFlow()

    private val _supportedLanguageState: MutableStateFlow<RequestState> =
        MutableStateFlow(RequestState.Idle)
    val supportedLanguageState = _supportedLanguageState.asStateFlow()

    private val _replaceOriginalWithTranslation = MutableStateFlow(false)
    val replaceOriginalWithTranslation = _replaceOriginalWithTranslation.asStateFlow()

    private val _translatorState: MutableStateFlow<RequestState> =
        MutableStateFlow(RequestState.Idle)
    val translatorState = _translatorState.asStateFlow()

    val translators = translationHelper.getAvailableTranslators()

    private val _sendLyricsState = MutableStateFlow(SendLyricsState())
    val sendLyricsState = _sendLyricsState.asStateFlow()

    fun undo() {
        if (canUndo.value) {
            val popped = undoStack.removeLast()
            redoStack.add(inputState.value)
            _inputState.value = popped
            updateStackState()
        }
    }

    fun redo() {
        if (canRedo.value) {
            undoStack.add(inputState.value)
            _inputState.value = redoStack.removeLast()
            updateStackState()
        }
    }

    fun updateInputState(newState: EditorInputState) {
        undoStack.add(this.inputState.value)
        redoStack.clear()
        updateStackState()
        _inputState.value = newState
    }

    fun initialize(
        powerampId: Long, filePath: String, lyrics: Lyrics, preferredLyricsType: LyricsType
    ) {
        if (isInitialized) {
            Log.w(TAG, "initialize: viewmodel already initialized")
            return
        }
        this.lyrics = lyrics
        _filepath.value = filePath
        this.powerampId = powerampId
        val lyrics =
            (if (preferredLyricsType == LyricsType.SYNCED) lyrics.syncedLyrics else lyrics.plainLyrics)
                ?: ""
        this._inputState.value = EditorInputState(lyrics = lyrics)
        Log.i(TAG, "initialize: viewmodel initialized")
        isInitialized = true
    }

    fun sendLyricsToPoweramp(context: Context) {
        _inputState.value.lyrics.takeIf { it.isNotBlank() }?.let { lyricsContent ->
            viewModelScope.launch {
                resetSendLyricsState()
                PowerampApiHelper.sendLyrics(
                    context = context,
                    filePath = _filepath.value,
                    powerampId = powerampId,
                    lyrics = lyrics.copy(syncedLyrics = lyricsContent),
                    lyricsType = LyricsType.SYNCED,
                ).collect { state -> _sendLyricsState.value = state }
            }
        }
    }

    fun resetSendLyricsState() {
        _sendLyricsState.value = SendLyricsState()
    }

    fun updateStackState() {
        canUndo.value = undoStack.isNotEmpty()
        canRedo.value = redoStack.isNotEmpty()
    }

    fun setChosenTranslator(translator: Translator) {
        _chosenTranslator.value = translator
        fetchSupportedLanguages()
    }

    fun fetchSupportedLanguages() {
        _supportedLanguageState.value = RequestState.Processing
        viewModelScope.launch {
            val result = translationHelper.getSupportedLanguages(
                translator = _chosenTranslator.value, lyrics = _inputState.value.lyrics
            )
            Log.d(TAG, "fetchSupportedLanguages: result- $result")
            _supportedLanguageState.value = result
        }
    }

    fun setTargetLanguage(lang: String) {
        viewModelScope.launch {
            _targetLanguage.value = lang
            _translatorState.value = RequestState.Idle
        }
    }

    fun setReplaceLyrics(replaceLyrics: Boolean) {
        _replaceOriginalWithTranslation.value = replaceLyrics
    }

    fun translate() {
        _translatorState.value = RequestState.Processing
        val lyricsContent = _inputState.value.lyrics
        val originalLyrics = getOriginalLyrics(_inputState.value.lyrics)
        val targetLang = _targetLanguage.value
        val selectedTranslator = _chosenTranslator.value
        if (originalLyrics.isBlank() || targetLang.isNullOrBlank()) {
            "lyrics blank ${originalLyrics.isBlank()} || targetLang blank ${targetLang.isNullOrBlank()}".let {
                Log.w(TAG, "translate: $it")
            }
            return
        }
        viewModelScope.launch {
            val result = translationHelper.translate(
                lyrics = originalLyrics,
                targetLanguage = targetLang,
                translator = selectedTranslator
            )
            Log.d(TAG, "translate: result- $result")
            _translatorState.value = result
            if (result is RequestState.Success<*>) {
                val translatedLyrics = (result.response) as String
                val newLyrics = if (_replaceOriginalWithTranslation.value) translatedLyrics else {
                    mergeLyricsWithTranslation(lyricsContent, translatedLyrics, targetLang)
                }
                val newState = _inputState.value.copy(lyrics = newLyrics)
                updateInputState(newState)
            }
        }
    }

    private fun mergeLyricsWithTranslation(
        lyricsContent: String,
        translatedLyrics: String,
        language: String
    ): String {
        val hasOriginalTag = lyricsContent.contains(
            Regex("^\\[#?.*Original.*]", RegexOption.IGNORE_CASE)
        )
        return if (hasOriginalTag) {
            buildString {
                appendLine(lyricsContent.trimEnd())
                appendLine()
                appendLine("[# Translated $language]")
                appendLine(translatedLyrics.trim())
            }
        } else {
            val firstTimestampIndex = Regex("\\[\\d{2}:\\d{2}\\.\\d{2}]")
                .find(lyricsContent)?.range?.first ?: 0
            val prefix = lyricsContent.substring(0, firstTimestampIndex)
            val lyricsWithoutPrefix = lyricsContent.substring(firstTimestampIndex)

            buildString {
                if (prefix.isNotBlank()) appendLine(prefix.trimEnd())
                appendLine("[# Original Lyrics]")
                appendLine(lyricsWithoutPrefix.trim())
                appendLine()
                appendLine("[# Translated $language]")
                appendLine(translatedLyrics.trim())
            }
        }
    }

    private fun getOriginalLyrics(lyrics: String): String {
        val matchResult = Regex("^\\[#?.*Original.*]", RegexOption.IGNORE_CASE).find(lyrics)
        return matchResult?.let { result ->
            lyrics.substringAfter(result.value).substringBefore("[# Translated")
        } ?: lyrics
    }

    val playerInitialized = playbackHelper.playerInitialized
    val trackDuration = playbackHelper.trackDurationInSeconds
    val playbackPosition = playbackHelper.playbackSeconds
    val isPlaying = playbackHelper.isPlaying

    fun seekTo(seconds: Int) {
        playbackHelper.seekTo((seconds.toLong().times(1000)))
    }

    fun togglePlayback(play: Boolean) {
        playbackHelper.togglePlayback(play)
    }

    fun setTrackUri(uri: Uri) {
        Log.d(TAG, "setTrackUri: newUri $uri ")
        playbackHelper.setTrackUri(uri)
    }

    fun getCurrentTimestamp(): Timestamp = playbackHelper.getCurrentTimestamp()

    init {
        viewModelScope.launch {
            _chosenTranslator.value = Translator.getDefault()
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared: stopping playback")
        playbackHelper.destroy()
    }

    companion object {
        private const val TAG = "EditorViewmodel"
        val FACTORY: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val context = (this[APPLICATION_KEY] as Application)
                EditorViewmodel(
                    PlaybackHelper(context),
                    TranslationHelper(context, RequestHelper.okHttpClient, RequestHelper.gson)
                )
            }
        }
    }
}
