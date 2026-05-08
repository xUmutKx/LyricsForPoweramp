package io.github.abhishekabhi789.lyricsforpoweramp.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import io.github.abhishekabhi789.lyricsforpoweramp.ui.settings.AppSettings
import io.github.abhishekabhi789.lyricsforpoweramp.ui.settings.SettingsSection
import io.github.abhishekabhi789.lyricsforpoweramp.ui.theme.LyricsForPowerAmpTheme
import io.github.abhishekabhi789.lyricsforpoweramp.ui.utils.isDarkTheme
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val scope = rememberCoroutineScope()
            val viewmodel: SettingsViewModel = viewModel()
            val preferredTheme by viewmodel.appTheme.collectAsStateWithLifecycle()
            val useDarkTheme = isDarkTheme(theme = preferredTheme)
            val listState = rememberLazyStaggeredGridState()
            LaunchedEffect(Unit) {
                when (intent?.action) {
                    ACTION_FOLDER_ACCESS_REQUEST -> {
                        viewmodel.setAccessRequestedPath(intent.getStringExtra(EXTRA_REQUIRED_PATH))
                        val scrollIndex = SettingsSection.STORAGE.ordinal
                        scope.launch { listState.animateScrollToItem(scrollIndex) }
                    }

                    ACTION_OPEN_SETTINGS -> {
                        val sectionIndex =
                            intent.getIntExtra(EXTRA_SETTINGS_SECTION, 0).coerceAtLeast(0)
                        scope.launch { listState.animateScrollToItem(sectionIndex) }
                    }
                }
            }

            LyricsForPowerAmpTheme(useDarkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    AppSettings(
                        viewmodel = viewmodel,
                        listState = listState,
                        onClose = { finish() })
                }
            }
        }
    }

    companion object {
        const val TAG = "SettingsActivity"
        const val ACTION_FOLDER_ACCESS_REQUEST =
            "io.github.abhishekabhi789.lyricsforpoweramp.FOLDER_ACCESS_NEEDED"
        const val ACTION_OPEN_SETTINGS =
            "io.github.abhishekabhi789.lyricsforpoweramp.OPEN_SETTINGS"
        const val EXTRA_REQUIRED_PATH = "need_permission_for_this_path"
        const val EXTRA_SETTINGS_SECTION = "extra_section_name"
    }
}
