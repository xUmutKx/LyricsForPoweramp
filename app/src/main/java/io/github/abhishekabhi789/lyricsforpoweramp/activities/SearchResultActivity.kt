package io.github.abhishekabhi789.lyricsforpoweramp.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.os.BundleCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.Track
import io.github.abhishekabhi789.lyricsforpoweramp.model.Track.Companion.KEY_FILE_PATH
import io.github.abhishekabhi789.lyricsforpoweramp.ui.searchresult.ResultScreen
import io.github.abhishekabhi789.lyricsforpoweramp.ui.theme.LyricsForPowerAmpTheme
import io.github.abhishekabhi789.lyricsforpoweramp.ui.utils.isDarkTheme
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.SearchResultViewmodel
import java.io.Serializable
import javax.inject.Inject

@AndroidEntryPoint
class SearchResultActivity : ComponentActivity() {
    @Inject
    lateinit var appPreference: AppPreference
    private val viewmodel: SearchResultViewmodel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val searchResult: List<Lyrics>? = intent.extras?.let {
            BundleCompat.getParcelableArrayList(it, KEY_RESULT, Lyrics::class.java)
        }
        val preferredTheme = appPreference.getTheme()
        val realId: Long? = getSerializableExtra(intent, Track.KEY_REAL_ID)
        val fileUri: String? = getSerializableExtra(intent, KEY_FILE_PATH)

        setContent {
            LaunchedEffect(Unit) {
                searchResult?.let { viewmodel.setSearchResults(it) }
                realId?.let { viewmodel.setPowerampId(it) }
                fileUri?.let { viewmodel.setFilePath(it) }
            }
            val useDarkTheme = isDarkTheme(theme = preferredTheme)
            LyricsForPowerAmpTheme(useDarkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    ResultScreen(
                        viewmodel = viewmodel,
                        onNavigateUp = { finish() },//takes to MainActivity
                        onFinish = { finishAffinity() }//takes back to Poweramp
                    )
                }
            }
        }
    }

    private inline fun <reified T : Serializable> getSerializableExtra(
        intent: Intent,
        key: String
    ): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(key) as? T
        }
    }

    companion object {
        const val TAG = "SearchResultActivity"
        const val KEY_RESULT = "search_result"
    }
}
