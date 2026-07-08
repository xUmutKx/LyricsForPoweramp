package io.github.abhishekabhi789.lyricsforpoweramp.ui.settings

import android.content.res.Configuration
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
import kotlinx.coroutines.flow.collectLatest

@Composable
private fun AppThemeSettingsContent(
    modifier: Modifier = Modifier,
    topbar: @Composable (() -> Unit),
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit
) {
    SettingsPageLayout(topbar = topbar, modifier = modifier) {
        BasicSettings(label = stringResource(R.string.settings_app_theme_description)) { interactionSource ->
            var expanded by remember { mutableStateOf(false) }
            var ignoreInteractions by remember { mutableStateOf(false) }
            val allThemes = remember { AppPreference.getThemes() }
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collectLatest { interaction ->
                    if (interaction is PressInteraction.Release) {
                        if (!ignoreInteractions) expanded = !expanded
                        ignoreInteractions = false
                    }
                }
            }
            DropdownSettings(
                expanded = expanded,
                currentValue = currentTheme,
                values = allThemes,
                onSelection = {
                    onThemeChange(it)
                    expanded = false
                },
                onExpandChanged = {
                    ignoreInteractions = true
                    expanded = it
                },
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewAppThemeSettings() {
    var appTheme by remember { mutableStateOf(AppTheme.Auto) }
    LyricsForPowerAmpTheme(useDarkTheme = isDarkTheme(appTheme)) {
        AppThemeSettingsContent(
            topbar = { TopAppBar(title = { Text("App Theme") }) },
            currentTheme = appTheme,
            onThemeChange = { appTheme = it }
        )
    }
}
