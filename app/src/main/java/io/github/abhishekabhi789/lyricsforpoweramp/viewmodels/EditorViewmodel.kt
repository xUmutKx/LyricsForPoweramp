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
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.StorageHelper
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import io.github.abhishekabhi789.lyricsforpoweramp.model.Result
import io.github.abhishekabhi789.lyricsforpoweramp.translation.TranslationHelper
import io.github.abhishekabhi789.lyricsforpoweramp.translation.Translator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class EditorViewmodel(private val translationHelper: TranslationHelper) : ViewModel() {

    private val undoStack = ArrayDeque<String>(50)
    private val redoStack = ArrayDeque<String>(50)

    val canUndo: MutableStateFlow<Boolean> = MutableStateFlow(undoStack.isNotEmpty())
    val canRedo: MutableStateFlow<Boolean> = MutableStateFlow(redoStack.isNotEmpty())

    private val _lyricsContent = MutableStateFlow("")
    val lyricsContent: StateFlow<String> = _lyricsContent

    private var powerampId: Long = 0L
    private lateinit var lyrics: Lyrics
    lateinit var filePath: String

    private val _sendToPowerampState = MutableStateFlow<Boolean?>(null)
    val sendToPowerampState = _sendToPowerampState.asStateFlow()

    private val _saveToStorageState = MutableStateFlow<StorageHelper.Result?>(null)
    val saveToStorageState = _saveToStorageState.asStateFlow()

    private val _supportedLanguages = MutableStateFlow<List<String>?>(null)
    val supportedLanguages = _supportedLanguages.asStateFlow()

    private val _targetLanguage: MutableStateFlow<String?> = MutableStateFlow(null)
    val targetLanguage = _targetLanguage.asStateFlow()

    private val _chosenTranslator: MutableStateFlow<Translator> =
        MutableStateFlow(Translator.getDefault())
    val chosenTranslator = _chosenTranslator.asStateFlow()

    private val _translatorRunning = MutableStateFlow(false)
    val translatorRunning = _translatorRunning.asStateFlow()

    val translators = translationHelper.getAvailableTranslators()

    fun undo() {
        if (canUndo.value) {
            val popped = undoStack.removeLast()
            redoStack.add(lyricsContent.value)
            _lyricsContent.value = popped
            updateStackState()
        }
    }

    fun redo() {
        if (canRedo.value) {
            undoStack.add(lyricsContent.value)
            _lyricsContent.value = redoStack.removeLast()
            updateStackState()
        }
    }

    fun setLyrics(lyrics: String) {
        undoStack.add(this.lyricsContent.value)
        redoStack.clear()
        updateStackState()
        _lyricsContent.value = lyrics
    }

    fun initialize(powerampId: Long, filePath: String, lyrics: Lyrics) {
        this.lyrics = lyrics
        this.filePath = filePath
        this.powerampId = powerampId
        _lyricsContent.value = (lyrics.syncedLyrics ?: lyrics.plainLyrics ?: "")
    }

    fun sendLyricsToPoweramp(context: Context, onComplete: () -> Unit) {
        viewModelScope.launch {
            val (sentToPoweramp, writingResult) =
                PowerampApiHelper.sendLyricResponse(
                    context = context,
                    filePath = filePath,
                    powerampId = powerampId,
                    lyrics = lyrics.copy(syncedLyrics = lyricsContent.value),
                    lyricsType = LyricsType.SYNCED,
                )
            "sentToPoweramp $sentToPoweramp result $writingResult".let {
                Log.d(TAG, "sendLyricsToPoweramp: $it")
            }
            _sendToPowerampState.update { sentToPoweramp }
            _saveToStorageState.update { writingResult }
            if (sentToPoweramp && writingResult == StorageHelper.Result.SUCCESS) {
                delay(3.seconds)
                onComplete()
            }
        }
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
        viewModelScope.launch {
            _supportedLanguages.value =
                translationHelper.getSupportedLanguages(
                    _chosenTranslator.value,
                    _lyricsContent.value
                )
        }
    }

    fun setTargetLanguage(lang: String) {
        _targetLanguage.value = lang
    }

    fun translate() {
        _translatorRunning.value = true
        val lyricsContent = _lyricsContent.value
        val targetLang = _targetLanguage.value
        val selectedTranslator = _chosenTranslator.value
        if (lyricsContent.isBlank() || targetLang.isNullOrBlank()) {
            return
        }
        viewModelScope.launch {
            translationHelper.translate(
                lyrics = lyricsContent,
                targetLanguage = targetLang,
                translator = selectedTranslator
            )?.let { translationResponse ->
                when (translationResponse) {
                    Result.Cancelled -> {
                        Log.i(TAG, "translate: cancelled")
                    }

                    is Result.Failure -> {
                        Log.w(TAG, "translate: failed ${translationResponse.error}")
                    }

                    is Result.Success -> {
                        _lyricsContent.value = translationResponse.response
                    }
                }
                _translatorRunning.value = false
            }
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
