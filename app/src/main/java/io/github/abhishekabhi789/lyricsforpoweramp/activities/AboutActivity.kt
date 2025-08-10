package io.github.abhishekabhi789.lyricsforpoweramp.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.abhishekabhi789.lyricsforpoweramp.ui.about.AboutScreen
import io.github.abhishekabhi789.lyricsforpoweramp.ui.theme.LyricsForPowerAmpTheme
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference

class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val preferredTheme = AppPreference.getTheme(this)
            val useDarkTheme = AppPreference.isDarkTheme(theme = preferredTheme)
            LyricsForPowerAmpTheme(useDarkTheme = useDarkTheme) {
                AboutScreen(onFinish = ::finish)
            }
        }
    }

    companion object {
        const val TAG = "AboutActivity"
    }
}
