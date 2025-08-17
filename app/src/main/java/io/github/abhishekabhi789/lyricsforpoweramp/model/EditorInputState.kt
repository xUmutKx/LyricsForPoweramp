package io.github.abhishekabhi789.lyricsforpoweramp.model

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

data class EditorInputState(val lyrics: String = "", val selection: TextRange = TextRange.Zero) {
    companion object {
        fun fromTextFieldValue(textFieldValue: TextFieldValue): EditorInputState {
            return EditorInputState(
                lyrics = textFieldValue.text,
                selection = textFieldValue.selection
            )
        }
    }
}
