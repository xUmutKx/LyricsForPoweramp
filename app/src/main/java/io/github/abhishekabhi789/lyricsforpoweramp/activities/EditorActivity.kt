package io.github.abhishekabhi789.lyricsforpoweramp.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.os.BundleCompat
import com.maxmpz.poweramp.player.PowerampAPI
import dagger.hilt.android.AndroidEntryPoint
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import io.github.abhishekabhi789.lyricsforpoweramp.model.Track
import io.github.abhishekabhi789.lyricsforpoweramp.ui.editor.EditorScreen
import io.github.abhishekabhi789.lyricsforpoweramp.ui.theme.LyricsForPowerAmpTheme
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.EditorViewmodel

@AndroidEntryPoint
class EditorActivity : ComponentActivity() {
    private val viewmodel: EditorViewmodel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val lyrics: Lyrics? = intent.extras?.let {
            BundleCompat.getParcelableArrayList(it, Track.KEY_LYRICS, Lyrics::class.java)
                ?.firstOrNull()
        }
        val powerampId = intent.getLongExtra(Track.KEY_REAL_ID, PowerampAPI.ID_NO_ID)
        val filePath = intent.getStringExtra(Track.KEY_FILE_PATH)
        val preferredLyricsType = intent.getStringExtra(KEY_PREFERRED_TYPE)
            ?.let { name -> LyricsType.valueOf(name) }
            ?: AppPreference.getPreferredLyricsType(this)
        if (powerampId == 0L || filePath == null || lyrics == null) {
            Log.i(TAG, "onCreate: realId = $powerampId || path = $filePath || lyrics == $lyrics")
            Log.e(TAG, "onCreate: failed to get required parameters, returning")
            finish()
            return
        }
        viewmodel.initialize(powerampId, filePath, lyrics, preferredLyricsType)
        setContent {
            LyricsForPowerAmpTheme {
                EditorScreen(viewmodel = viewmodel, onFinish = { finish() })
            }
        }
    }

    companion object {
        const val TAG = "EditorActivity"
        const val KEY_PREFERRED_TYPE = "preferred_lyrics_type"
    }
}
