package io.github.abhishekabhi789.lyricsforpoweramp.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.abhishekabhi789.lyricsforpoweramp.ui.about.AboutScreen
import io.github.abhishekabhi789.lyricsforpoweramp.ui.theme.LyricsForPowerAmpTheme
import io.github.abhishekabhi789.lyricsforpoweramp.ui.utils.isDarkTheme
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppTheme
import javax.inject.Inject

@AndroidEntryPoint
class AboutActivity : ComponentActivity() {
    @Inject
    lateinit var appPreference: AppPreference
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val appTheme by appPreference.appTheme.collectAsStateWithLifecycle()
            val accent by appPreference.accentColor.collectAsStateWithLifecycle()
            LyricsForPowerAmpTheme(
                useDarkTheme = isDarkTheme(appTheme),
                amoled = appTheme == AppTheme.Amoled,
                accent = accent
            ) {
                AboutScreen(onFinish = ::finish)
            }
        }
    }

    companion object {
        const val TAG = "AboutActivity"
    }
}
