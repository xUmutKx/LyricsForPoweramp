package io.github.abhishekabhi789.lyricsforpoweramp.model

import androidx.compose.ui.text.TextRange

data class EditorInputState(val lyrics: String = "", val selection: TextRange = TextRange.Zero)
