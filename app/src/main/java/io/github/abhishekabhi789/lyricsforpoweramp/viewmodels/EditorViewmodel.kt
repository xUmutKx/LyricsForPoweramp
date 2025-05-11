package io.github.abhishekabhi789.lyricsforpoweramp.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.PowerampApiHelper
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.StorageHelper
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class EditorViewmodel : ViewModel() {

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

    companion object {
        private const val TAG = "EditorViewmodel"
    }
}
