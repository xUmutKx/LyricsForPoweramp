package io.github.abhishekabhi789.lyricsforpoweramp.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.core.os.BundleCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import io.github.abhishekabhi789.lyricsforpoweramp.ui.editor.EditorScreen
import io.github.abhishekabhi789.lyricsforpoweramp.ui.theme.LyricsForPowerAmpTheme
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
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
        val preferredLyricsType = intent.getStringExtra(KEY_PREFERRED_TYPE)?.let { name ->
            LyricsType.valueOf(name)
        } ?: AppPreference.getPreferredLyricsType(this)
        if (powerampId == 0L || filePath == null || lyrics == null) {
            Log.i(TAG, "onCreate: realId = $powerampId || path = $filePath || lyrics == $lyrics")
            Log.e(TAG, "onCreate: failed to get required parameters, returning")
            finish()
            return
        }
        setContent {
            val viewmodel: EditorViewmodel = viewModel(factory = EditorViewmodel.FACTORY)
            LaunchedEffect(Unit) {
                viewmodel.initialize(powerampId, filePath, lyrics, preferredLyricsType)
            }
            LyricsForPowerAmpTheme {
                EditorScreen(viewmodel = viewmodel, onFinish = { finish() })
            }
        }
    }

    companion object {
        const val TAG = "EditorActivity"
        const val KEY_LYRICS = "lyrics"
        const val KEY_REAL_ID = "poweramp_id"
        const val KEY_FILE_PATH = "file_path"
        const val KEY_PREFERRED_TYPE = "preferred_lyrics_type"
    }
}
