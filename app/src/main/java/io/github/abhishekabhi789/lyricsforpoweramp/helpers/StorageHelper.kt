package io.github.abhishekabhi789.lyricsforpoweramp.helpers

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import io.github.abhishekabhi789.lyricsforpoweramp.R
import io.github.abhishekabhi789.lyricsforpoweramp.model.LyricsType
import io.github.abhishekabhi789.lyricsforpoweramp.utils.getTreeDocumentId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object StorageHelper {
    private const val TAG = "StorageHelper"
    private const val MIME_SYNCED_LYRICS = "text/lrc"
    private const val MIME_PLAIN_LYRICS = "text/plain"
    private const val EXTENSION_LRC = "lrc"
    private const val EXTENSION_TXT = "txt"

    suspend fun writeLyricsFile(
        context: Context,
        filePath: String?,
        lyricsContent: String,
        lyricsType: LyricsType,
    ): Result = withContext(Dispatchers.IO) {
        if (filePath == null) {
            Log.e(TAG, "writeLyricsFile: filepath is null; aborting")
            return@withContext Result.INVALID_FILEPATH
        }
        if (lyricsContent.isBlank()) {
            Log.e(TAG, "writeLyricsFile: aborting lyrics write due to null lyrics")
            return@withContext Result.INVALID_LYRICS
        }
        val filePath = normalizeToDocumentId(filePath)
        Log.d(TAG, "writeLyricsFile: track file path $filePath")
        if (filePath.isNullOrBlank()) return@withContext Result.INVALID_FILEPATH
        val parentFolder = getParentFolder(context, filePath)
        if (parentFolder == null || !parentFolder.isDirectory) {
            Log.e(TAG, "writeLyricsFile: failed to resolve parent folder for $filePath")
            return@withContext Result.NO_PERMISSION //no access
        }

        val (mimeType, extension) = if (lyricsType == LyricsType.SYNCED) {
            Pair(MIME_SYNCED_LYRICS, EXTENSION_LRC)
        } else {
            Pair(MIME_PLAIN_LYRICS, EXTENSION_TXT)
        }

        val lyricsFileName = filePath.substringAfterLast("/").replaceAfterLast(".", extension)
        if (lyricsFileName.isBlank()) {
            Log.e(TAG, "writeLyricsFile: failed to make lyrics file name $lyricsFileName")
            return@withContext Result.INVALID_FILEPATH
        }

        val lyricsFile = runCatching {
            parentFolder.findFile(lyricsFileName)?.delete()
            parentFolder.createFile(mimeType, lyricsFileName)
        }.fold(onSuccess = { it }, onFailure = { tr ->
            Log.e(TAG, "writeLyricsFile: failed to create lyrics file", tr)
            null
        })
        if (lyricsFile == null) {
            Log.e(TAG, "writeLyricsFile: Failed to create file $lyricsFileName")
            return@withContext Result.INVALID_FILEPATH
        }

        return@withContext runCatching {
            context.contentResolver.openOutputStream(lyricsFile.uri)?.use { outputStream ->
                outputStream.write(lyricsContent.toByteArray())
            }
        }.fold(onSuccess = { Result.SUCCESS }, onFailure = { tr ->
            Log.e(TAG, "writeLyricsFile: failed to write file ${lyricsFile.uri}", tr)
            Result.UNKNOWN_ERROR
        })
    }

    fun normalizeToDocumentId(filePath: String): String? {
        return when {
            filePath.startsWith("/storage/emulated/0/") -> {
                val relative = filePath.removePrefix("/storage/emulated/0/").trimStart('/')
                "primary:$relative"
            }

            filePath.startsWith("/storage/") -> {
                //external storage devices
                val parts = filePath.removePrefix("/storage/").split("/", limit = 2)
                if (parts.size == 2) {
                    val volumeId = parts[0]
                    val relative = parts[1]
                    "$volumeId:$relative"
                } else null
            }

            filePath.contains(":") -> filePath

            else -> null
        }
    }

    fun getParentFolder(context: Context, filePath: String): DocumentFile? {
        val savedUris = context.contentResolver.persistedUriPermissions
            .filter { it.isWritePermission }
            .mapNotNull { it.uri }

        val permittedParentFolderUri =
            savedUris.find { filePath.startsWith(it.getTreeDocumentId()) }
        if (permittedParentFolderUri == null) {
            Log.e(TAG, "writeLyricsFile: no access ${filePath.substringBeforeLast("/")}")
            return null
        }

        val permittedFolder = DocumentFile.fromTreeUri(context, permittedParentFolderUri)
        if (permittedFolder == null) {
            Log.e(
                TAG,
                "writeLyricsFile: failed to get permitted folder from uri $permittedParentFolderUri",
            )
        }
        Log.d(TAG, "getParentFolder: permitted folder ${permittedFolder?.uri}")
        val segments = filePath.removePrefix(permittedParentFolderUri.getTreeDocumentId())
            .trimStart('/')
            .split('/')
            .dropLast(1)
        var parentFolder: DocumentFile? = permittedFolder
        for (segment in segments) {
            parentFolder = parentFolder?.findFile(segment)
            if (parentFolder == null) break
        }
        Log.d(TAG, "getParentFolder: parent folder found ${parentFolder?.uri}")
        return parentFolder
    }

    enum class Result(val messageResId: Int) {
        /** Successfully wrote the lyrics file. */
        SUCCESS(R.string.lyrics_saved_to_storage),

        /** Missing permission to access the selected folder. */
        NO_PERMISSION(R.string.lyrics_write_no_permission),

        /** Invalid file path. */
        INVALID_FILEPATH(R.string.lyrics_write_invalid_path),

        /** Invalid lyrics content. */
        INVALID_LYRICS(R.string.lyrics_write_invalid_lyrics),

        /** An unknown error occurred while writing the file. */
        UNKNOWN_ERROR(R.string.lyrics_write_unknown_error)
    }

    fun getFileUriFromTreeUri(treeUri: Uri?, documentId: String): Uri? {
        if (treeUri == null) {
            Log.e(TAG, "getFileUriFromTreeUri: tree uri is null")
            return null
        }
        val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
        if (!documentId.startsWith(treeDocId)) {
            Log.e(TAG, "getFileUriFromTreeUri: document id not starts with normalized path")
            Log.d(TAG, "getFileUriFromTreeUri: documentId $documentId")
            Log.d(TAG, "getFileUriFromTreeUri: normalized path $treeDocId")
            return null
        }
        val relativePath = documentId.removePrefix(treeDocId).trimStart('/')
        val fileDocId = if (relativePath.isEmpty()) treeDocId else "$treeDocId/$relativePath"
        return DocumentsContract.buildDocumentUriUsingTree(treeUri, fileDocId)
    }
}
