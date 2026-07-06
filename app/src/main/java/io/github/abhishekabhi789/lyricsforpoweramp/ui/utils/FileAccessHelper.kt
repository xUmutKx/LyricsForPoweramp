package io.github.abhishekabhi789.lyricsforpoweramp.ui.utils

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.StorageHelper.getFileUriFromTreeUri
import io.github.abhishekabhi789.lyricsforpoweramp.ui.utils.FolderAccessState.Companion.PERMISSION_MODE_FLAGS
import io.github.abhishekabhi789.lyricsforpoweramp.ui.utils.FolderAccessState.Companion.TAG
import io.github.abhishekabhi789.lyricsforpoweramp.utils.getTreeDocumentId
import io.github.abhishekabhi789.lyricsforpoweramp.viewmodels.SettingsViewModel

class FolderAccessState internal constructor(
    private val onRequestAccess: (onResult: ((Uri?) -> Unit)?) -> Unit,
    private val onRevokeAccess: () -> Unit,
    private val onChildUriRequest: (documentId: String) -> Uri?,
    val hasPermission: Boolean,
    val isRemovable: Boolean
) {
    fun requestAccess(onResult: ((Uri?) -> Unit)? = null) = onRequestAccess(onResult)
    fun revokeAccess() = onRevokeAccess()
    fun getChildUri(documentId: String): Uri? = onChildUriRequest(documentId)

    companion object {
        const val TAG = "FolderAccessState"
        const val PERMISSION_MODE_FLAGS =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

    }
}

/** @param documentId the id of the folder document to access. e.g. `primary:Music/SubFolder` */
@Composable
fun rememberFolderAccess(
    documentId: String,
    settingsViewModel: SettingsViewModel = hiltViewModel()
): FolderAccessState {
    val context = LocalContext.current
    val savedUris by settingsViewModel.savedUris.collectAsStateWithLifecycle()
    //actual path from documentId.
    val normalizedDocumentId by remember(documentId) {
        derivedStateOf {
            val path = if (documentId.substringAfterLast("/").contains(".")) {
                documentId.substringBeforeLast("/")
            } else documentId
            path.removeSuffix("/")
        }
    }
    var askPermission by rememberSaveable(normalizedDocumentId) { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var pendingResultCallback by remember { mutableStateOf<((Uri?) -> Unit)?>(null) }

    val invokePendingResultCallback = { uri: Uri? ->
        pendingResultCallback?.invoke(uri)
        pendingResultCallback = null
    }

    //askPermission is used as key since it's change when user interacts with permission dialog
    val accessibleParentFolder: Uri? by remember(
        normalizedDocumentId, askPermission, refreshTrigger, savedUris
    ) {
        derivedStateOf {
            val uriPermissions = context.contentResolver.persistedUriPermissions
            uriPermissions.filterNotNull()
                .filter { it.isReadPermission && it.isWritePermission }.mapNotNull { it.uri }
                .let { uris -> findParentUri(uris, normalizedDocumentId) }
        }
    }
    //this saved uri helps the picker to open correct folder
    val savedParentFolder: Uri? by remember(savedUris, normalizedDocumentId) {
        derivedStateOf { findParentUri(savedUris, normalizedDocumentId) }
    }
    val hasPermission by remember(accessibleParentFolder) {
        derivedStateOf { accessibleParentFolder != null }
    }

    LaunchedEffect(accessibleParentFolder) {
        if (accessibleParentFolder != null) {
            askPermission = false
        }
    }

    val pickFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { pickedUri ->
        if (pickedUri == null) {
            Log.w(TAG, "rememberFolderAccess: picked uri is null")
            invokePendingResultCallback(null)
            return@rememberLauncherForActivityResult
        }
        val pickedUriPath = pickedUri.getTreeDocumentId()
        Log.i(TAG, "rememberFolderAccess: picked uri $pickedUriPath")
        context.contentResolver.takePersistableUriPermission(pickedUri, PERMISSION_MODE_FLAGS)
        settingsViewModel.saveNewUri(pickedUri)
        val isValidParentPicked = normalizedDocumentId.startsWith(pickedUriPath)
        if (isValidParentPicked) {
            invokePendingResultCallback(pickedUri)
        }
        refreshTrigger++
        askPermission = !isValidParentPicked
    }

    if (askPermission) {
        val launchInput by remember(normalizedDocumentId, savedParentFolder) {
            derivedStateOf {
                getFileUriFromTreeUri(savedParentFolder, normalizedDocumentId)
            }
        }
        val onPermissionRequest = {
            runCatching { pickFolderLauncher.launch(launchInput) }.exceptionOrNull()?.let {
                Log.e(TAG, "rememberFolderAccess: request failed", it)
                Toast(context).run {
                    setText(R.string.failed_to_open_folder_picker)
                    duration = Toast.LENGTH_SHORT
                    show()
                }
            }
            Unit
        }
        AlertDialog(
            onDismissRequest = { askPermission = false },
            confirmButton = {
                TextButton(onClick = onPermissionRequest) {
                    Text(stringResource(R.string.grant_access))
                }
            },
            dismissButton = {
                TextButton({
                    askPermission = false
                    invokePendingResultCallback(null)
                }) {
                    Text(stringResource(R.string.dismiss))
                }
            },
            icon = { Icon(Icons.Default.Info, null) },
            title = { Text(stringResource(R.string.folder_access_request_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.folder_access_request_explanation,
                        normalizedDocumentId.substringBeforeLast("/")
                    )
                )
            })
    }
    return remember(normalizedDocumentId, accessibleParentFolder, refreshTrigger) {
        FolderAccessState(
            hasPermission = hasPermission,
            isRemovable = accessibleParentFolder?.getTreeDocumentId()
                ?.removeSuffix("/") == normalizedDocumentId,
            onRequestAccess = { onResult ->
                askPermission = true
                pendingResultCallback = onResult
            },
            onRevokeAccess = {
                accessibleParentFolder?.let { uri ->
                    try {
                        context.contentResolver.releasePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        settingsViewModel.removeUri(uri)
                    } catch (e: Exception) {
                        Log.e(TAG, "rememberFolderAccess: failed to revoke ${uri.path}", e)
                    }
                }
                refreshTrigger++
            },
            onChildUriRequest = { documentId ->
                getFileUriFromTreeUri(accessibleParentFolder, documentId)
            }
        )
    }
}

fun findParentUri(uris: List<Uri>, documentId: String): Uri? {
    val (docRoot, docPath) = getRootAndPathList(documentId)

    var bestMatch: Uri? = null
    var bestDepth = -1

    for (uri in uris) {
        val (uriRoot, uriPath) = getRootAndPathList(uri.getTreeDocumentId())

        if (uriRoot != docRoot) continue
        if (uriPath.size > docPath.size) continue

        if (uriPath.indices.all { uriPath[it] == docPath[it] }) {
            if (uriPath.size > bestDepth) {
                bestDepth = uriPath.size
                bestMatch = uri
            }
        }
    }

    return bestMatch
}

fun getRootAndPathList(documentId: String): Pair<String, List<String>> {
    val root = documentId.substringBefore(":")
    val path = documentId.substringAfter(":", "")
        .split("/")
        .filter { it.isNotEmpty() }

    return root to path
}
