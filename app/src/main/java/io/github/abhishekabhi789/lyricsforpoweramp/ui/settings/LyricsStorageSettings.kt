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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.activities.SettingsActivity.Companion.TAG
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.Disclaimer
import io.github.abhishekabhi789.lyricsforpoweramp.ui.components.PermissionDialog
import io.github.abhishekabhi789.lyricsforpoweramp.ui.utils.rememberFolderAccess
import io.github.abhishekabhi789.lyricsforpoweramp.utils.getTreeDocumentId
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.SettingsViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LyricsStorageSettings(
    modifier: Modifier = Modifier,
    topbar: @Composable (() -> Unit),
    viewmodel: SettingsViewModel
) {
    val context = LocalContext.current
    SettingsPage(topbar = topbar, modifier = modifier) {
        BasicSettings(
            label = stringResource(R.string.settings_send_to_poweramp_label),
            description = stringResource(R.string.settings_send_to_poweramp_description)
        ) { interactionSource ->
            val savedChoice by viewmodel.sendLyricsToPoweramp.collectAsStateWithLifecycle()
            val onSwitchToggle = { enabled: Boolean -> viewmodel.setSendLyricsToPoweramp(enabled) }
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
        val saveAsFile by viewmodel.saveAsFile.collectAsStateWithLifecycle()
        BasicSettings(
            label = stringResource(R.string.settings_save_as_file_label),
            description = stringResource(R.string.settings_save_as_file_description)
        ) { interactionSource ->
            val onSwitchToggle = { enabled: Boolean -> viewmodel.setSaveAsFile(enabled) }
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
        val saveIdTagsInFile by viewmodel.saveIdTagsInFile.collectAsStateWithLifecycle()
        AnimatedVisibility(visible = saveAsFile) {
            BasicSettings(
                label = stringResource(R.string.settings_save_id_tags_in_lrc_file_label),
                description = stringResource(R.string.settings_save_id_tags_in_lrc_file_description)
            ) { interactionSource ->
                val onSwitchToggle = { enabled: Boolean ->
                    viewmodel.setSaveIdTagsInFile(enabled)
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

        val embedIntoFile by viewmodel.embedLyricsIntoFile.collectAsStateWithLifecycle()
        BasicSettings(
            label = stringResource(R.string.settings_embed_into_song_file_label),
            description = stringResource(R.string.settings_embed_into_song_file_description)
        ) { interactionSource ->
            val onSwitchToggle = { enabled: Boolean ->
                viewmodel.setEmbedLyricsIntoFile(enabled)
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

        val accessRequestedPath by viewmodel.accessRequestedPath.collectAsStateWithLifecycle()
        val savedUris by viewmodel.savedUris.collectAsStateWithLifecycle()
        val pickFolderLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                viewmodel.saveNewUri(uri)
            }
        }
        Column(modifier = Modifier.fillMaxWidth()) {
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
                    val onPermissionRequest = { folderAccessState.requestAccess() }
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
                            viewmodel.removeUri(uri)
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
                    textContent = AnnotatedString(stringResource(R.string.settings_add_folder_empty_list)),
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
