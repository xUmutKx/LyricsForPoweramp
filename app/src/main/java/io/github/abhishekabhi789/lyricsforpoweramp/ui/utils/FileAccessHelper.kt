package io.github.abhishekabhi789.lyricsforpoweramp.ui.utils

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.helpers.getCleanedPath
import io.github.abhishekabhi789.lyricsforpoweramp.ui.utils.FolderAccessState.Companion.TAG
import io.github.abhishekabhi789.lyricsforpoweramp.utils.AppPreference

class FolderAccessState internal constructor(
    private val onRequestAccess: (onResult: ((Uri?) -> Unit)?) -> Unit,
    private val onRevokeAccess: () -> Unit,
    private val onChildUriRequest: (documentId: String) -> Uri?,
    val hasPermission: Boolean
) {
    fun requestAccess(onResult: ((Uri?) -> Unit)? = null) = onRequestAccess(onResult)
    fun revokeAccess() = onRevokeAccess()
    fun getChildUri(documentId: String): Uri? = onChildUriRequest(documentId)

    companion object {
        const val TAG = "FileAccessState"
    }
}

/** @param documentId the id of the folder document to access. e.g. primary/Music/Folder*/
@Composable
fun rememberFolderAccess(documentId: String): FolderAccessState {
    val context = LocalContext.current
    val modeFlags = remember {
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    }
    val documentId by remember(documentId) {
        derivedStateOf {
            if (documentId.substringAfterLast("/").contains(".")) {
                documentId.substringBeforeLast("/")
            } else documentId
        }
    }
    var askPermission by rememberSaveable(documentId) { mutableStateOf(false) }
    var pendingResultCallback by remember { mutableStateOf<((Uri?) -> Unit)?>(null) }
    val invokePendingResultCallback = { uri: Uri? ->
        pendingResultCallback?.invoke(uri)
        pendingResultCallback = null
    }
    //askPermission is used as key since it's change when user interacts with permission dialog
    val accessibleParentFolder: Uri? by remember(documentId, askPermission) {
        derivedStateOf {
            context.contentResolver.persistedUriPermissions.filterNotNull()
                .filter { it.isReadPermission }.mapNotNull { it.uri }
                .maxByOrNull { uri ->
                    calculateCommonPrefixLength(uri.getCleanedPath(), documentId)
                }
        }
    }
    //this saved uri helps the picker to open correct folder
    val savedParentFolder: Uri? by remember(documentId, askPermission) {
        derivedStateOf {
            AppPreference.getSavedUris(context).maxByOrNull { uri ->
                calculateCommonPrefixLength(uri.getCleanedPath(), documentId)
            }
        }
    }
    val hasPermission by remember(accessibleParentFolder) {
        derivedStateOf { accessibleParentFolder != null }
    }

    LaunchedEffect(accessibleParentFolder, documentId) {
        accessibleParentFolder?.let { uri ->
            Log.i(TAG, "rememberFolderAccess: found parent folder with access $savedParentFolder")
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
        Log.i(TAG, "rememberFolderAccess: picked uri $pickedUri")
        context.contentResolver.takePersistableUriPermission(pickedUri, modeFlags)
        AppPreference.saveFolderUri(context, pickedUri)
        val isValidParentPicked = documentId.startsWith(pickedUri.getCleanedPath())
        invokePendingResultCallback(pickedUri)
        askPermission = !isValidParentPicked
    }

    if (askPermission) {
        val launchInput by remember(documentId, savedParentFolder) {
            derivedStateOf {
                getFileUriFromTreeUri(savedParentFolder, documentId)
            }
        }
        AlertDialog(
            onDismissRequest = { askPermission = false },
            confirmButton = {
                TextButton({ pickFolderLauncher.launch(launchInput) }) {
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
                        documentId.substringBeforeLast("/")
                    )
                )
            })
    }
    return remember(documentId, hasPermission) {
        FolderAccessState(
            hasPermission = hasPermission,
            onRequestAccess = { onResult ->
                askPermission = true
                pendingResultCallback = onResult
            },
            onRevokeAccess = {
                accessibleParentFolder?.let {
                    context.contentResolver.releasePersistableUriPermission(it, modeFlags)
                }
            },
            onChildUriRequest = { documentId ->
                getFileUriFromTreeUri(accessibleParentFolder, documentId)
            }
        )
    }
}

fun getFileUriFromTreeUri(treeUri: Uri?, documentId: String): Uri? {
    if (treeUri == null) {
        Log.e(TAG, "getFileUriFromTreeUri: tree uri is null")
        return null
    }
    val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
    val normalizedTreePath = treeDocId.replace(":", "/")
    if (!documentId.startsWith(normalizedTreePath)) {
        Log.e(TAG, "getFileUriFromTreeUri: document id not starts with normalized path")
        Log.d(TAG, "getFileUriFromTreeUri: documentId $documentId")
        Log.d(TAG, "getFileUriFromTreeUri: normalized path $normalizedTreePath")
        return null
    }
    val relativePath = documentId.removePrefix(normalizedTreePath).trimStart('/')
    val fileDocId = if (relativePath.isEmpty()) treeDocId else "$treeDocId/$relativePath"
    return DocumentsContract.buildDocumentUriUsingTree(treeUri, fileDocId)
}

fun calculateCommonPrefixLength(a: String, b: String): Int {
    val minLen = minOf(a.length, b.length)
    for (i in 0 until minLen) {
        if (a[i] != b[i]) return i
    }
    return minLen
}
