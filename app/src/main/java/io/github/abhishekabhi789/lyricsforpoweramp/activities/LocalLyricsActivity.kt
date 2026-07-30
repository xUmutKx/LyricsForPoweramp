package io.github.abhishekabhi789.lyricsforpoweramp.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.abhishekabhi789.lyricsforpoweramp.ui.locallyrics.LocalLyricsScreen
import io.github.abhishekabhi789.lyricsforpoweramp.ui.theme.LyricsForPowerAmpTheme
import io.github.abhishekabhi789.lyricsforpoweramp.ui.utils.isDarkTheme
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppTheme
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.LocalLyricsViewModel

@AndroidEntryPoint
class LocalLyricsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: LocalLyricsViewModel by viewModels()
            val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
            val accent by viewModel.accentColor.collectAsStateWithLifecycle()
            LyricsForPowerAmpTheme(
                useDarkTheme = isDarkTheme(appTheme),
                amoled = appTheme == AppTheme.Amoled,
                accent = accent
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LocalLyricsScreen(viewModel = viewModel, onNavigateUp = ::finish)
                }
            }
        }
    }

    companion object {
        const val TAG = "LocalLyricsActivity"
    }
}
