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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.abhishekabhi789.lyricsforpoweramp.ui.settings.AppSettings
import io.github.abhishekabhi789.lyricsforpoweramp.ui.settings.SettingsCategory
import io.github.abhishekabhi789.lyricsforpoweramp.ui.theme.LyricsForPowerAmpTheme
import io.github.abhishekabhi789.lyricsforpoweramp.ui.utils.isDarkTheme
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.SettingsViewModel
import kotlinx.serialization.json.Json

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val viewmodel: SettingsViewModel = hiltViewModel()
            val preferredTheme by viewmodel.appTheme.collectAsStateWithLifecycle()
            val useDarkTheme = isDarkTheme(theme = preferredTheme)
            val navController = rememberNavController()
            LaunchedEffect(Unit) {
                when (intent?.action) {
                    ACTION_OPEN_SETTING -> {
                        val navJson = intent.getStringExtra(EXTRA_NAV_DATA)
                        if (navJson.isNullOrBlank()) {
                            Log.w(TAG, "onCreate: EXTRA_NAV_DATA is null or blank")
                        } else {
                            val navData =
                                runCatching { Json.decodeFromString<SettingsCategory>(navJson) }.getOrNull()
                            if (navData == null) {
                                Log.e(TAG, "onCreate: failed to build navData from json-$navJson")
                            } else {
                                navController.navigate(navData)
                            }
                        }
                    }
                }
            }

            LyricsForPowerAmpTheme(useDarkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    AppSettings(
                        navController = navController,
                        onClose = { finish() })
                }
            }
        }
    }

    companion object {
        const val TAG = "SettingsActivity"
        const val ACTION_OPEN_SETTING =
            "io.github.abhishekabhi789.lyricsforpoweramp.settings.action.OPEN_SETTINGS"
        const val EXTRA_NAV_DATA =
            "io.github.abhishekabhi789.lyricsforpoweramp.settings.extra.NAV_DATA"
    }
}
