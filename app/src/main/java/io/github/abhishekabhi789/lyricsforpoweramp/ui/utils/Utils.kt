package io.github.abhishekabhi789.lyricsforpoweramp.ui.utils

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppTheme

@Composable
fun isDarkTheme(theme: AppTheme): Boolean {
    return when (theme) {
        AppTheme.Dark -> true
        AppTheme.Light -> false
        AppTheme.Auto -> isSystemInDarkTheme()
    }
}
