package io.github.abhishekabhi789.lyricsforpoweramp.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.core.os.BundleCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.abhishekabhi789.lyricsforpoweramp.activities.ui.theme.LyricsForPowerAmpTheme
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.ui.editor.EditorScreen
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.EditorViewmodel

class EditorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val lyrics: Lyrics? = intent.extras?.let {
            BundleCompat.getParcelableArrayList(it, KEY_LYRICS, Lyrics::class.java)?.firstOrNull()
        }
        val powerampId = intent.getLongExtra(KEY_REAL_ID, 0L)
        val filePath = intent.getStringExtra(KEY_FILE_PATH)
        val exitAfterFinish = intent.getBooleanExtra(KEY_EXIT_AFTER_FINISH, false)
        if (powerampId == 0L || filePath == null || lyrics == null) {
            Log.i(TAG, "onCreate: realId = $powerampId || path = $filePath || lyrics == $lyrics")
            Log.e(TAG, "onCreate: failed to get required parameters, returning")
            finish()
            return
        }
        setContent {
            val viewmodel: EditorViewmodel = viewModel()
            LaunchedEffect(Unit) {
                viewmodel.initialize(powerampId, filePath, lyrics)
            }
            LyricsForPowerAmpTheme {
                EditorScreen(
                    viewmodel = viewmodel,
                    onFinish = { if (exitAfterFinish == true) finishAffinity() else finish() }
                )
            }
        }
    }

    companion object {
        const val TAG = "EditorActivity"
        const val KEY_LYRICS = "lyrics"
        const val KEY_REAL_ID = "poweramp_id"
        const val KEY_FILE_PATH = "file_path"
        const val KEY_EXIT_AFTER_FINISH = "exit_after_editing_finished"
    }
}
