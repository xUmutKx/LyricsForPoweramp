package io.github.abhishekabhi789.lyricsforpoweramp.viewmodels

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.PowerampApiHelper
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.RequestHelper
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.SendLyricsState
import io.github.abhishekabhi789.lyricsforpoweramp.model.EditorInputState
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import io.github.abhishekabhi789.lyricsforpoweramp.translation.RequestState
import io.github.abhishekabhi789.lyricsforpoweramp.translation.TranslationHelper
import io.github.abhishekabhi789.lyricsforpoweramp.translation.Translator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditorViewmodel(private val translationHelper: TranslationHelper) : ViewModel() {

    private val undoStack = ArrayDeque<EditorInputState>(50)
    private val redoStack = ArrayDeque<EditorInputState>(50)

    val canUndo: MutableStateFlow<Boolean> = MutableStateFlow(undoStack.isNotEmpty())
    val canRedo: MutableStateFlow<Boolean> = MutableStateFlow(redoStack.isNotEmpty())

    private val _inputState = MutableStateFlow(EditorInputState())
    val inputState: StateFlow<EditorInputState> = _inputState.asStateFlow()

    private var powerampId: Long = 0L
    private lateinit var lyrics: Lyrics
    lateinit var filePath: String

    private val _targetLanguage: MutableStateFlow<String?> = MutableStateFlow(null)
    val targetLanguage = _targetLanguage.asStateFlow()

    private val _chosenTranslator: MutableStateFlow<Translator> =
        MutableStateFlow(Translator.getDefault())
    val chosenTranslator = _chosenTranslator.asStateFlow()

    private val _supportedLanguageState: MutableStateFlow<RequestState> =
        MutableStateFlow(RequestState.Idle)
    val supportedLanguageState = _supportedLanguageState.asStateFlow()

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
        this.lyrics = lyrics
        this.filePath = filePath
        this.powerampId = powerampId
        val lyrics =
            (if (preferredLyricsType == LyricsType.SYNCED) lyrics.syncedLyrics else lyrics.plainLyrics)
                ?: ""
        this._inputState.value = EditorInputState(lyrics = lyrics)
    }

    fun sendLyricsToPoweramp(context: Context) {
        resetSendLyricsState()
        viewModelScope.launch {
            PowerampApiHelper.sendLyrics(
                context = context,
                filePath = filePath,
                powerampId = powerampId,
                lyrics = lyrics.copy(syncedLyrics = inputState.value.lyrics),
                lyricsType = LyricsType.SYNCED,
            ).collect { state -> _sendLyricsState.value = state }
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

    fun translate() {
        _translatorState.value = RequestState.Processing
        val lyricsContent = _inputState.value
        val targetLang = _targetLanguage.value
        val selectedTranslator = _chosenTranslator.value
        if (lyricsContent.lyrics.isBlank() || targetLang.isNullOrBlank()) {
            "lyrics blank ${lyricsContent.lyrics.isBlank()} || targetLang blank ${targetLang.isNullOrBlank()}".let {
                Log.w(TAG, "translate: $it")
            }
            return
        }
        viewModelScope.launch {
            val result = translationHelper.translate(
                lyrics = lyricsContent.lyrics,
                targetLanguage = targetLang,
                translator = selectedTranslator
            )
            Log.d(TAG, "translate: result- $result")
            _translatorState.value = result
            if (result is RequestState.Success<*>) {
                val lyrics = (result.response) as String
                val newState = _inputState.value.copy(lyrics = lyrics)
                updateInputState(newState)
            }
        }
    }

    init {
        viewModelScope.launch {
            setChosenTranslator(Translator.getDefault())
        }
    }

    companion object {
        private const val TAG = "EditorViewmodel"
        val FACTORY: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val context = (this[APPLICATION_KEY] as Application)
                EditorViewmodel(
                    TranslationHelper(context, RequestHelper.okHttpClient, RequestHelper.gson)
                )
            }
        }
    }
}
