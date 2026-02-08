package io.github.abhishekabhi789.lyricsforpoweramp.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.ui.searchresult.ResultScreen
import io.github.abhishekabhi789.lyricsforpoweramp.ui.theme.LyricsForPowerAmpTheme
import io.github.abhishekabhi789.lyricsforpoweramp.ui.utils.isDarkTheme
import io.github.abhishekabhi789.lyricsforpoweramp.utils.makeToast
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.SearchResultViewmodel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class SearchResultActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val searchResultData = intent.getStringExtra(KEY_RESULT_DATA)
        setContent {
            val viewmodel: SearchResultViewmodel = viewModel()
            val preferredTheme by viewmodel.appTheme.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) {
                searchResultData?.let {
                    if (!viewmodel.setSearchResultDataKey(it)) {
                        Log.w(TAG, "onCreate: failed to prepare search result with key $it")
                        makeToast(R.string.failed_to_prepare_search_result)
                        delay(3.seconds)
                        finish()
                    }
                }
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

    companion object {
        const val TAG = "SearchResultActivity"
        const val KEY_RESULT_DATA = "search_result_data_cache_key"
    }
}
