package io.github.abhishekabhi789.lyricsforpoweramp.ui.settings

import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.ui.theme.LyricsForPowerAmpTheme
import io.github.abhishekabhi789.lyricsforpoweramp.ui.utils.isDarkTheme
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppTheme
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.SettingsViewModel

@Composable
private fun AppThemeSettingsContent(
    modifier: Modifier = Modifier,
    topbar: @Composable (() -> Unit),
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit
) {
    SettingsPageLayout(topbar = topbar, modifier = modifier) {
        var expanded by remember { mutableStateOf(false) }
        val allThemes = remember { AppPreference.getThemes() }
        BasicSettings(label = stringResource(R.string.settings_app_theme_description)) { interactionSource ->
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interaction ->
                    if (interaction is PressInteraction.Release) {
                        if (!expanded) expanded = true
                    }
                }
            }
            DropdownSettings(
                expanded = expanded,
                currentValue = currentTheme,
                values = allThemes,
                onSelection = onThemeChange,
                onExpandChanged = { if (expanded) expanded = false },
                getLabel = { theme -> stringResource(theme.labelResId) }
            )
        }
    }
}

@Composable
fun AppThemeSettings(
    modifier: Modifier = Modifier,
    topbar: @Composable (() -> Unit),
    viewmodel: SettingsViewModel
) {
    val appTheme by viewmodel.appTheme.collectAsStateWithLifecycle()
    AppThemeSettingsContent(
        modifier = modifier,
        topbar = topbar,
        currentTheme = appTheme,
        onThemeChange = viewmodel::updateTheme
    )
}

@Preview(showSystemUi = true)
@Composable
private fun PreviewAppThemeSettings() {
    var appTheme by remember { mutableStateOf(AppTheme.Auto) }
    LyricsForPowerAmpTheme (useDarkTheme = isDarkTheme(appTheme)){
        AppThemeSettingsContent(
            topbar = { TopAppBar(title = { Text("App Theme") }) },
            currentTheme = appTheme,
            onThemeChange = { appTheme = it }
        )
    }
}
