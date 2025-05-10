package io.github.abhishekabhi789.lyricsforpoweramp.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class EditorViewmodel : ViewModel() {

    private val undoStack = ArrayDeque<String>(50)
    private val redoStack = ArrayDeque<String>(50)

    val canUndo: MutableStateFlow<Boolean> = MutableStateFlow(undoStack.isNotEmpty())
    val canRedo: MutableStateFlow<Boolean> = MutableStateFlow(redoStack.isNotEmpty())

    private val _lyrics = MutableStateFlow("")
    val lyrics: StateFlow<String> = _lyrics

    fun undo() {
        if (canUndo.value) {
            val popped = undoStack.removeLast()
            redoStack.add(lyrics.value)
            _lyrics.value = popped
            canUndo.value = undoStack.isNotEmpty()
            canRedo.value = redoStack.isNotEmpty()
        }
    }

    fun redo() {
        if (canRedo.value) {
            undoStack.add(lyrics.value)
            _lyrics.value = redoStack.removeLast()
            canUndo.value = undoStack.isNotEmpty()
            canRedo.value = redoStack.isNotEmpty()
        }
    }

    fun setLyrics(lyrics: String) {
        undoStack.add(this.lyrics.value)
        redoStack.clear()
        canUndo.value = undoStack.isNotEmpty()
        canRedo.value = redoStack.isNotEmpty()
        _lyrics.value = lyrics
    }
}
