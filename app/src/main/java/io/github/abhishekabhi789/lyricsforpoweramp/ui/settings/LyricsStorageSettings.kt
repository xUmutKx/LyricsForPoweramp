package io.github.abhishekabhi789.lyricsforpoweramp.ui.settings


import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.activities.SettingsActivity.Companion.TAG
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.getTreeDocumentId
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.Disclaimer
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.PermissionDialog
import io.github.abhishekabhi789.lyricsforpoweramp.ui.theme.LyricsForPowerAmpTheme
import io.github.abhishekabhi789.lyricsforpoweramp.ui.utils.rememberFolderAccess
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.SettingsViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LyricsStorageSettings(
    modifier: Modifier = Modifier,
    viewmodel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    SettingsGroup(
        title = stringResource(R.string.settings_lyrics_storage_label),
        icon = Icons.Default.Storage
    ) {
        BasicSettings(
            label = stringResource(R.string.settings_send_to_poweramp_label),
            description = stringResource(R.string.settings_send_to_poweramp_description)
        ) { interactionSource ->
            var savedChoice by remember {
                mutableStateOf(AppPreference.getSendLyricsToPoweramp(context))
            }
            val onSwitchToggle = { enabled: Boolean ->
                AppPreference.setSendLyricsToPoweramp(context, enabled)
                savedChoice = enabled
            }
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interaction ->
                    if (interaction is PressInteraction.Release) {
                        onSwitchToggle(!savedChoice)
                    }
                }
            }

            val accessibilityLabel = (if (savedChoice) stringResource(R.string.disable)
            else stringResource(R.string.enable)).let {
                "$it ${stringResource(R.string.settings_send_to_poweramp_label)}"
            }
            Switch(
                checked = savedChoice, onCheckedChange = onSwitchToggle,
                modifier = Modifier.semantics { contentDescription = accessibilityLabel })
        }
        var saveAsFile by remember { mutableStateOf(AppPreference.getSaveAsFile(context)) }
        BasicSettings(
            label = stringResource(R.string.settings_save_as_file_label),
            description = stringResource(R.string.settings_save_as_file_description)
        ) { interactionSource ->
            val onSwitchToggle = { enabled: Boolean ->
                AppPreference.setSaveAsFile(context, enabled)
                saveAsFile = enabled
            }
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interaction ->
                    if (interaction is PressInteraction.Release) {
                        onSwitchToggle(!saveAsFile)
                    }
                }
            }
            val accessibilityLabel = (if (saveAsFile) stringResource(R.string.disable)
            else stringResource(R.string.enable)).let {
                "$it ${stringResource(R.string.settings_save_as_file_label)}"
            }
            Switch(
                checked = saveAsFile, onCheckedChange = onSwitchToggle,
                modifier = Modifier.semantics { contentDescription = accessibilityLabel })
        }
        var saveIdTagsInFile by remember {
            mutableStateOf(AppPreference.getSaveIdTagsInFile(context))
        }
        AnimatedVisibility(visible = saveAsFile) {
            BasicSettings(
                label = stringResource(R.string.settings_save_id_tags_in_lrc_file_label),
                description = stringResource(R.string.settings_save_id_tags_in_lrc_file_description)
            ) { interactionSource ->
                val onSwitchToggle = { enabled: Boolean ->
                    AppPreference.setSaveIdTagsInFile(context, enabled)
                    saveIdTagsInFile = enabled
                }
                LaunchedEffect(interactionSource) {
                    interactionSource.interactions.collect { interaction ->
                        if (interaction is PressInteraction.Release) {
                            onSwitchToggle(!saveIdTagsInFile)
                        }
                    }
                }
                val accessibilityLabel = (if (saveIdTagsInFile) stringResource(R.string.disable)
                else stringResource(R.string.enable)).let {
                    "$it ${stringResource(R.string.settings_save_id_tags_in_lrc_file_label)}"
                }
                Switch(
                    checked = saveIdTagsInFile, onCheckedChange = onSwitchToggle,
                    modifier = Modifier.semantics { contentDescription = accessibilityLabel }
                )
            }
        }

        var embedIntoFile by remember { mutableStateOf(AppPreference.getEmbedLyricsAsTag(context)) }
        BasicSettings(
            label = stringResource(R.string.settings_embed_into_song_file_label),
            description = stringResource(R.string.settings_embed_into_song_file_description)
        ) { interactionSource ->
            val onSwitchToggle = { enabled: Boolean ->
                AppPreference.setEmbedLyricsAsTag(context, enabled)
                embedIntoFile = enabled
            }
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interaction ->
                    if (interaction is PressInteraction.Release) {
                        onSwitchToggle(!embedIntoFile)
                    }
                }
            }
            val accessibilityLabel =
                stringResource(if (embedIntoFile) R.string.disable else R.string.enable).let {
                    "$it ${stringResource(R.string.settings_embed_into_song_file_label)}"
                }
            Switch(
                checked = embedIntoFile, onCheckedChange = onSwitchToggle,
                modifier = Modifier.semantics { contentDescription = accessibilityLabel })
        }
        AnimatedVisibility(visible = embedIntoFile) {
            var fixMetadata by remember { mutableStateOf(AppPreference.getFixMetadata(context)) }
            BasicSettings(
                label = stringResource(R.string.settings_fix_metadata_with_lyrics_info_label),
                description = stringResource(R.string.settings_fix_metadata_with_lyrics_info_description),
            ) { interactionSource ->
                val onSwitchToggle = { enabled: Boolean ->
                    AppPreference.setFixMetadata(context, enabled)
                    fixMetadata = enabled
                }
                LaunchedEffect(interactionSource) {
                    interactionSource.interactions.collect { interaction ->
                        if (interaction is PressInteraction.Release) {
                            onSwitchToggle(!fixMetadata)
                        }
                    }
                }
                val accessibilityLabel =
                    stringResource(if (fixMetadata) R.string.disable else R.string.enable).let {
                        "$it ${stringResource(R.string.settings_fix_metadata_with_lyrics_info_label)}"
                    }
                Switch(
                    checked = fixMetadata, onCheckedChange = onSwitchToggle,
                    modifier = Modifier.semantics { contentDescription = accessibilityLabel })
            }
        }
        val accessRequestedPath by viewmodel.accessRequestedPath.collectAsState()
        var savedUris by rememberSaveable { mutableStateOf(AppPreference.getSavedUris(context)) }
        val pickFolderLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                AppPreference.saveFolderUri(context, it)
                savedUris = savedUris.toMutableSet().apply { add(it) }.distinct().toList()
                //clearing the requested URI
                viewmodel.setAccessRequestedPath(null)
            }
        }
        Column(modifier = modifier.fillMaxWidth()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = stringResource(R.string.settings_add_folder),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 8.dp)
            )
            val onPermissionRequest = {
                runCatching { pickFolderLauncher.launch(null) }.exceptionOrNull()?.let {
                    Log.e(TAG, "LyricsStorageSettings: request failed", it)
                    Toast(context).run {
                        setText(R.string.failed_to_open_folder_picker)
                        duration = Toast.LENGTH_SHORT
                        show()
                    }
                }
                Unit
            }
            BasicSettings(label = stringResource(R.string.settings_add_folder_list_title)) { interactionSource ->
                LaunchedEffect(interactionSource) {
                    interactionSource.interactions.collect { interaction ->
                        if (interaction is PressInteraction.Release) {
                            onPermissionRequest()
                        }
                    }
                }
                TextButton(onClick = { onPermissionRequest() }) {
                    Icon(Icons.Default.AddCircle, null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.settings_add_folder_add_new))
                }
            }
            if (savedUris.isNotEmpty()) {
                for ((i, uri) in savedUris.withIndex()) {
                    val path by remember(uri) { derivedStateOf { uri.getTreeDocumentId() } }
                    val folderAccessState = rememberFolderAccess(path)
                    val onPermissionRequest = {
                        folderAccessState.requestAccess { uri ->
                            uri?.let {
                                AppPreference.saveFolderUri(context, it)
                                savedUris = savedUris.toMutableSet().apply {
                                    add(it)
                                }.distinct().toList()
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        Text("${i + 1}.", modifier = Modifier.padding(end = 8.dp))
                        Text(
                            path,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (!folderAccessState.hasPermission) {
                            TextButton(onClick = onPermissionRequest) {
                                Text(stringResource(R.string.settings_add_folder_button_grant_access))
                            }
                        }
                        IconButton(onClick = {
                            folderAccessState.revokeAccess()
                            val success = AppPreference.removeSavedFolder(context, uri)
                            if (success) savedUris = savedUris - uri

                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.settings_add_folder_button_remove),
                                tint = Color.Red.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                Disclaimer(
                    textContent = stringResource(R.string.settings_add_folder_empty_list),
                    icon = Icons.Default.Error,
                    foregroundColor = MaterialTheme.colorScheme.onErrorContainer,
                    backgroundColor = MaterialTheme.colorScheme.errorContainer
                )
            }
        }
        accessRequestedPath?.let { pathUri ->
            PermissionDialog(
                explanation = stringResource(
                    R.string.settings_add_folder_permission_explanation,
                    pathUri.getTreeDocumentId()
                ),
                allowToSuppress = false,
                onConfirm = { pickFolderLauncher.launch(pathUri) },
                onDismiss = {
                    viewmodel.setAccessRequestedPath(null)
                    Log.w(TAG, "LyricsStorageSettings: user ignored storage access request")
                }
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun PreviewStorageAccess() {
    LyricsForPowerAmpTheme {
        LyricsStorageSettings()
    }
}
