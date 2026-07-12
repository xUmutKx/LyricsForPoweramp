package io.github.abhishekabhi789.lyricsforpoweramp.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.os.BundleCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maxmpz.poweramp.player.PowerampAPI
import dagger.hilt.android.AndroidEntryPoint
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import io.github.abhishekabhi789.lyricsforpoweramp.model.Track
import io.github.abhishekabhi789.lyricsforpoweramp.ui.editor.EditorScreen
import io.github.abhishekabhi789.lyricsforpoweramp.ui.theme.LyricsForPowerAmpTheme
import io.github.abhishekabhi789.lyricsforpoweramp.utils.makeToast
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.EditorViewmodel

@AndroidEntryPoint
class EditorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val lyrics: Lyrics? = intent.extras?.let {
            BundleCompat.getParcelableArrayList(it, Track.KEY_LYRICS, Lyrics::class.java)
                ?.firstOrNull()
        }
        val powerampId = intent.getLongExtra(Track.KEY_REAL_ID, PowerampAPI.ID_NO_ID)
        val filePath = intent.getStringExtra(Track.KEY_FILE_PATH)
        val chosenLyricsType = intent.getStringExtra(KEY_LYRICS_TYPE)
            ?.let { name -> LyricsType.valueOf(name) }
        if (powerampId == 0L || filePath == null || lyrics == null) {
            Log.d(TAG, "onCreate: realId = $powerampId || path = $filePath || lyrics == $lyrics")
            Log.e(TAG, "onCreate: failed to get required parameters, returning")
            makeToast(R.string.failed)
            finish()
            return
        }
        setContent {
            val viewmodel: EditorViewmodel = viewModel()
            val preferredLyricsType by viewmodel.preferredLyricsType.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewmodel.initialize(
                    powerampId = powerampId,
                    filePath = filePath,
                    lyrics = lyrics,
                    preferredLyricsType = chosenLyricsType ?: preferredLyricsType
                )
            }
            LyricsForPowerAmpTheme {
                EditorScreen(viewmodel = viewmodel, onFinish = { finish() })
            }
        }
    }

    companion object {
        const val TAG = "EditorActivity"
        const val KEY_LYRICS_TYPE = "preferred_lyrics_type"
    }
}
