package io.github.abhishekabhi789.lyricsforpoweramp.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.abhishekabhi789.lyricsforpoweramp.activities.ui.theme.LyricsForPowerAmpTheme
import io.github.abhishekabhi789.lyricsforpoweramp.ui.editor.EditorScreen
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.EditorViewmodel

class EditorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewmodel: EditorViewmodel = viewModel()
            LyricsForPowerAmpTheme {
                EditorScreen(viewmodel = viewmodel) { finish() }
            }
        }
    }
}
