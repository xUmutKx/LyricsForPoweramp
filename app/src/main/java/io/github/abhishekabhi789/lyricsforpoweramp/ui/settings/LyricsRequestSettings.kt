package io.github.abhishekabhi789.lyricsforpoweramp.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.PermissionDialog
import io.github.abhishekabhi789.lyricsforpoweramp.ui.theme.LyricsForPowerAmpTheme
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.SettingsViewModel

@SuppressLint("InlinedApi", "PermissionLaunchedDuringComposition")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun LyricsRequestSettingsContent(
    modifier: Modifier = Modifier,
    topbar: @Composable (() -> Unit),
    fallbackToSearch: Boolean,
    onFallbackToSearchChange: (Boolean) -> Unit,
    showNotification: Boolean,
    onShowNotificationChange: (Boolean) -> Unit,
    overwriteNotification: Boolean,
    onOverwriteNotificationChange: (Boolean) -> Unit,
    preferredLyricsType: LyricsType,
    onPreferredLyricsTypeChange: (LyricsType) -> Unit,
    markInstrumental: Boolean,
    onMarkInstrumentalChange: (Boolean) -> Unit
) {
    SettingsPageLayout(topbar = topbar, modifier = modifier) {
        val context = LocalContext.current
        var hasNotificationPermission by rememberSaveable { mutableStateOf(false) }
        var askPermission by rememberSaveable { mutableStateOf(false) }
        var showPermissionDialog by rememberSaveable { mutableStateOf(false) }
        val permissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        hasNotificationPermission = when (permissionState.status) {
            PermissionStatus.Granted -> true
            is PermissionStatus.Denied -> false
        }
        LaunchedEffect(askPermission) {
            if (askPermission) {
                if (permissionState.status.shouldShowRationale) {
                    showPermissionDialog = true
                } else {
                    permissionState.launchPermissionRequest()
                }
                askPermission = false // resetting
            }
        }
        if (showPermissionDialog) {
            PermissionDialog(
                explanation = stringResource(R.string.settings_notification_permission_description),
                onConfirm = {
                    askPermission = false
                    showPermissionDialog = false
                    val intent =
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                    context.startActivity(intent)
                },
                onDismiss = {
                    askPermission = false
                    showPermissionDialog = false
                }
            )
        }
        BasicSettings(
            label = stringResource(id = R.string.settings_fallback_to_search_label),
            description = stringResource(id = R.string.settings_fallback_to_search_description),
            modifier = Modifier
        ) { interactionSource ->
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interaction ->
                    if (interaction is PressInteraction.Release) {
                        onFallbackToSearchChange(!fallbackToSearch)
                    }
                }
            }
            val accessibilityLabel = (if (fallbackToSearch) stringResource(R.string.disable)
            else stringResource(R.string.enable)).let {
                "$it ${stringResource(R.string.settings_fallback_to_search_label)}"
            }
            Switch(
                checked = fallbackToSearch,
                onCheckedChange = onFallbackToSearchChange,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .semantics { contentDescription = accessibilityLabel }
            )
        }
        BasicSettings(
            label = stringResource(id = R.string.settings_request_fail_notification_label),
            description = stringResource(id = R.string.settings_request_fail_notification_description),
            modifier = Modifier
        ) { interactionSource ->
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interaction ->
                    if (interaction is PressInteraction.Release) {
                        onShowNotificationChange(!showNotification)
                    }
                }
            }
            val accessibilityLabel = (if (showNotification) stringResource(R.string.disable)
            else stringResource(R.string.enable)).let {
                "$it ${stringResource(R.string.settings_request_fail_notification_label)}"
            }
            Switch(
                checked = showNotification,
                onCheckedChange = onShowNotificationChange,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .semantics { contentDescription = accessibilityLabel }

            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AnimatedVisibility(
                visible = showNotification && !hasNotificationPermission,
                enter = slideInVertically() + expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = slideOutVertically() + shrinkVertically() + fadeOut()
            ) {
                BasicSettings(
                    label = stringResource(R.string.settings_notification_permission_label),
                    description = stringResource(R.string.settings_notification_permission_description)
                ) { interactionSource ->
                    val onButtonClick = { askPermission = true }
                    LaunchedEffect(interactionSource) {
                        interactionSource.interactions.collect { interaction ->
                            if (interaction is PressInteraction.Release) {
                                onButtonClick()
                            }
                        }
                    }
                    Button(onClick = onButtonClick, enabled = !hasNotificationPermission) {
                        Text(stringResource(R.string.settings_permission_button_grant))
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = showNotification,
            enter = slideInVertically() + expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = slideOutVertically() + shrinkVertically() + fadeOut()
        ) {
            BasicSettings(
                label = stringResource(id = R.string.settings_overwrite_existing_notification_label),
                description = stringResource(id = R.string.settings_overwrite_existing_notification_description),
                modifier = Modifier.alpha(if (hasNotificationPermission) 1.0f else 0.7f)
            ) { interactionSource ->
                LaunchedEffect(interactionSource) {
                    interactionSource.interactions.collect { interaction ->
                        if (interaction is PressInteraction.Release) {
                            onOverwriteNotificationChange(!overwriteNotification)
                        }
                    }
                }
                val accessibilityLabel =
                    (if (overwriteNotification) stringResource(R.string.disable)
                    else stringResource(R.string.enable)).let {
                        "$it ${stringResource(R.string.settings_overwrite_existing_notification_label)}"
                    }
                Switch(
                    checked = overwriteNotification,
                    enabled = hasNotificationPermission,
                    onCheckedChange = onOverwriteNotificationChange,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .semantics { contentDescription = accessibilityLabel }

                )
            }
        }

        BasicSettings(
            label = stringResource(R.string.settings_preferred_lyrics_type_label),
            description = stringResource(R.string.settings_preferred_lyrics_type_description)
        ) { interactionSource ->
            var expanded by remember { mutableStateOf(false) }
            var ignoreInteractions by remember { mutableStateOf(false) }
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interaction ->
                    if (interaction is PressInteraction.Release) {
                        if (!ignoreInteractions) expanded = !expanded
                        ignoreInteractions = false
                    }
                }
            }
            DropdownSettings(
                expanded = expanded,
                currentValue = preferredLyricsType,
                values = listOf(LyricsType.SYNCED, LyricsType.PLAIN),
                onSelection = {
                    onPreferredLyricsTypeChange(it)
                    expanded = false
                },
                onExpandChanged = {
                    ignoreInteractions = true
                    expanded = it
                },
                getLabel = { stringResource(it.shortLabelResId) }
            )
        }
        BasicSettings(
            label = stringResource(R.string.settings_mark_instrumental_tracks),
            description = stringResource(R.string.settings_mark_instrumental_tracks_description)
        ) { interactionSource ->
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interaction ->
                    if (interaction is PressInteraction.Release) {
                        onMarkInstrumentalChange(!markInstrumental)
                    }
                }
            }

            val accessibilityLabel = (if (markInstrumental) stringResource(R.string.disable)
            else stringResource(R.string.enable)).let {
                "$it ${stringResource(R.string.settings_mark_instrumental_tracks)}"
            }
            Switch(
                checked = markInstrumental,
                onCheckedChange = onMarkInstrumentalChange,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .semantics { contentDescription = accessibilityLabel }
            )
        }
    }
}

@Composable
fun LyricsRequestSettings(
    modifier: Modifier = Modifier,
    topbar: @Composable (() -> Unit),
    viewmodel: SettingsViewModel
) {
    val fallbackToSearch by viewmodel.fallbackToSearch.collectAsStateWithLifecycle()
    val showNotification by viewmodel.showNotification.collectAsStateWithLifecycle()
    val overwriteNotification by viewmodel.overwriteNotification.collectAsStateWithLifecycle()
    val preferredLyricsType by viewmodel.preferredLyricsType.collectAsStateWithLifecycle()
    val markInstrumental by viewmodel.markInstrumental.collectAsStateWithLifecycle()
    LyricsRequestSettingsContent(
        modifier = modifier,
        topbar = topbar,
        fallbackToSearch = fallbackToSearch,
        onFallbackToSearchChange = viewmodel::setFallbackToSearchMode,
        showNotification = showNotification,
        onShowNotificationChange = viewmodel::setShowNotification,
        overwriteNotification = overwriteNotification,
        onOverwriteNotificationChange = viewmodel::setOverwriteNotification,
        preferredLyricsType = preferredLyricsType,
        onPreferredLyricsTypeChange = viewmodel::setPreferredLyricsType,
        markInstrumental = markInstrumental,
        onMarkInstrumentalChange = viewmodel::setMarkInstrumental,
    )
}

@Preview(showSystemUi = true)
@Composable
private fun LyricsRequestSettingsPreview() {
    LyricsForPowerAmpTheme {
        var fallbackToSearch by remember { mutableStateOf(false) }
        var showNotification by remember { mutableStateOf(true) }
        var overwriteNotification by remember { mutableStateOf(false) }
        var preferredLyricsType by remember { mutableStateOf(LyricsType.SYNCED) }
        var markInstrumental by remember { mutableStateOf(false) }
        LyricsRequestSettingsContent(
            topbar = { TopAppBar(title = { Text("Lyrics Request Settings") }) },
            fallbackToSearch = fallbackToSearch,
            onFallbackToSearchChange = { fallbackToSearch = it },
            showNotification = showNotification,
            onShowNotificationChange = { showNotification = it },
            overwriteNotification = overwriteNotification,
            onOverwriteNotificationChange = { overwriteNotification = it },
            preferredLyricsType = preferredLyricsType,
            onPreferredLyricsTypeChange = { preferredLyricsType = it },
            markInstrumental = markInstrumental,
            onMarkInstrumentalChange = { markInstrumental = it }
        )
    }
}
