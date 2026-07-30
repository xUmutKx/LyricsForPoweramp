package io.github.abhishekabhi789.lyricsforpoweramp.helpers

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.abhishekabhi789.lyricsforpoweramp.model.LocalAudioWithoutLyrics
import io.github.abhishekabhi789.lyricsforpoweramp.model.LocalLyricsEntry
import io.github.abhishekabhi789.lyricsforpoweramp.model.LocalLyricsIndex
import io.github.abhishekabhi789.lyricsforpoweramp.model.LocalLyricsLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

/**
 * Walks the folder the user granted access to, parses every `.lrc` file in it and keeps
 * the result in a cache file, so opening the screen again doesn't re-read the whole tree.
 * A rescan only re-parses files whose timestamp changed.
 */
class LocalLyricsIndexer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {

    private val cacheFile get() = File(context.filesDir, CACHE_FILE_NAME)

    suspend fun loadCache(treeUri: Uri): List<LocalLyricsEntry> = withContext(Dispatchers.IO) {
        val file = cacheFile
        if (!file.exists()) return@withContext emptyList()
        runCatching {
            val index = json.decodeFromString(LocalLyricsIndex.serializer(), file.readText())
            if (index.treeUri == treeUri.toString()) index.entries else emptyList()
        }.onFailure { Log.w(TAG, "loadCache: unusable cache, rescanning", it) }.getOrDefault(emptyList())
    }

    /**
     * Rebuilds the index for [treeUri]. [known] entries with an unchanged timestamp are
     * reused as is. [onProgress] reports the number of lyric files found so far.
     */
    suspend fun buildIndex(
        treeUri: Uri,
        known: List<LocalLyricsEntry> = emptyList(),
        onProgress: (found: Int) -> Unit = {}
    ): List<LocalLyricsEntry> = withContext(Dispatchers.IO) {
        val reusable = known.associateBy { it.lrcUri }
        val entries = mutableListOf<LocalLyricsEntry>()
        val rootId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
        if (rootId == null) {
            Log.e(TAG, "buildIndex: $treeUri is not a tree uri")
            return@withContext emptyList()
        }
        scanFolder(treeUri, rootId, reusable, entries, onProgress)
        saveCache(treeUri, entries)
        entries
    }

    fun clearCache() {
        cacheFile.delete()
    }

    /** Every audio file under [treeUri] that has no sibling `.lrc` file next to it. */
    suspend fun findAudioWithoutLyrics(treeUri: Uri): List<LocalAudioWithoutLyrics> =
        withContext(Dispatchers.IO) {
            val rootId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
                ?: return@withContext emptyList()
            val result = mutableListOf<LocalAudioWithoutLyrics>()
            scanForMissingLyrics(treeUri, rootId, result)
            result
        }

    private suspend fun scanForMissingLyrics(
        treeUri: Uri,
        documentId: String,
        result: MutableList<LocalAudioWithoutLyrics>
    ) {
        coroutineContext.ensureActive()
        val children = listChildren(treeUri, documentId)
        val lrcBaseNames = children.filter { it.isLrc }
            .map { it.baseName.lowercase(Locale.ROOT) }
            .toSet()

        for (child in children) {
            coroutineContext.ensureActive()
            when {
                child.isDirectory -> scanForMissingLyrics(treeUri, child.documentId, result)

                child.isAudio && child.baseName.lowercase(Locale.ROOT) !in lrcBaseNames -> {
                    val audioUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, child.documentId)
                    val (title, artist) = readAudioTags(audioUri, fallbackTitle = child.baseName)
                    result.add(
                        LocalAudioWithoutLyrics(
                            audioUri = audioUri.toString(),
                            folderDocumentId = documentId,
                            title = title,
                            artist = artist,
                            fileBaseName = child.baseName
                        )
                    )
                }
            }
        }
    }

    /** Title/artist straight from the file's own tags, falling back to the filename. */
    private fun readAudioTags(uri: Uri, fallbackTitle: String): Pair<String, String?> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() } ?: fallbackTitle
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() }
            title to artist
        } catch (e: Exception) {
            Log.d(TAG, "readAudioTags: no tags for $uri", e)
            fallbackTitle to null
        } finally {
            runCatching { retriever.release() }
        }
    }

    /** Writes [content] as a new file named [fileBaseName].[extension] inside [folderDocumentId]. */
    suspend fun writeSiblingFile(
        treeUri: Uri,
        folderDocumentId: String,
        fileBaseName: String,
        extension: String,
        mimeType: String,
        content: String
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val folderUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, folderDocumentId)
            val fileName = "$fileBaseName.$extension"
            val existing = findChildDocumentId(treeUri, folderDocumentId, fileName)
            if (existing != null) {
                DocumentsContract.deleteDocument(
                    context.contentResolver,
                    DocumentsContract.buildDocumentUriUsingTree(treeUri, existing)
                )
            }
            val newFileUri = DocumentsContract.createDocument(
                context.contentResolver, folderUri, mimeType, fileName
            ) ?: return@withContext false
            context.contentResolver.openOutputStream(newFileUri)?.use { out ->
                out.write(content.toByteArray())
            }
            true
        }.onFailure { Log.e(TAG, "writeSiblingFile: failed for $fileBaseName.$extension", it) }
            .getOrDefault(false)
    }

    private fun findChildDocumentId(treeUri: Uri, folderDocumentId: String, displayName: String): String? =
        listChildren(treeUri, folderDocumentId).firstOrNull { it.displayName == displayName }?.documentId

    private suspend fun scanFolder(
        treeUri: Uri,
        documentId: String,
        reusable: Map<String, LocalLyricsEntry>,
        entries: MutableList<LocalLyricsEntry>,
        onProgress: (found: Int) -> Unit
    ) {
        coroutineContext.ensureActive()
        val children = listChildren(treeUri, documentId)
        val folderName = documentId.substringAfterLast('/').substringAfterLast(':')
        val audioByName = children.filter { it.isAudio }.associateBy { it.baseName.lowercase(Locale.ROOT) }

        for (child in children) {
            coroutineContext.ensureActive()
            when {
                child.isDirectory -> scanFolder(treeUri, child.documentId, reusable, entries, onProgress)

                child.isLrc -> {
                    val lrcUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, child.documentId)
                    val audio = audioByName[child.baseName.lowercase(Locale.ROOT)]
                    val cached = reusable[lrcUri.toString()]
                    val entry = if (cached != null && cached.lastModified == child.lastModified) {
                        cached
                    } else {
                        parseLrc(
                            uri = lrcUri,
                            title = child.baseName,
                            folder = folderName,
                            audioUri = audio?.let {
                                DocumentsContract.buildDocumentUriUsingTree(treeUri, it.documentId).toString()
                            },
                            lastModified = child.lastModified
                        )
                    }
                    if (entry != null) {
                        entries.add(entry)
                        onProgress(entries.size)
                    }
                }
            }
        }
    }

    private fun listChildren(treeUri: Uri, documentId: String): List<DocumentEntry> {
        val childrenUri = runCatching {
            DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
        }.getOrNull() ?: return emptyList()
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )
        return runCatching {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            DocumentEntry(
                                documentId = cursor.getString(0),
                                displayName = cursor.getString(1) ?: "",
                                mimeType = cursor.getString(2) ?: "",
                                lastModified = cursor.getLong(3)
                            )
                        )
                    }
                }
            } ?: emptyList()
        }.onFailure { Log.e(TAG, "listChildren: failed for $documentId", it) }.getOrDefault(emptyList())
    }

    private fun parseLrc(
        uri: Uri,
        title: String,
        folder: String,
        audioUri: String?,
        lastModified: Long
    ): LocalLyricsEntry? {
        val lines = mutableListOf<LocalLyricsLine>()
        var artistTag: String? = null
        val readFailure = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                var number = 0
                reader.forEachLine { raw ->
                    number++
                    val artistMatch = ARTIST_TAG.find(raw)
                    when {
                        artistMatch != null -> {
                            if (artistTag.isNullOrBlank()) artistTag = artistMatch.groupValues[1].trim()
                        }

                        METADATA_TAG.matches(raw) -> Unit

                        else -> {
                            val position = TIMESTAMP.find(raw)?.let { match ->
                                val (minutes, seconds, fraction) = match.destructured
                                (minutes.toLong() * 60_000) + (seconds.toLong() * 1000) +
                                        (fraction.padEnd(3, '0').take(3).toLong())
                            } ?: -1L
                            val text = TIMESTAMP.replace(raw, "").trim()
                            if (text.isNotEmpty()) {
                                lines.add(LocalLyricsLine(number, text, position))
                            }
                        }
                    }
                }
            }
        }.exceptionOrNull()
        if (readFailure != null) {
            Log.w(TAG, "parseLrc: failed to read $title", readFailure)
            return null
        }
        if (lines.isEmpty()) return null
        return LocalLyricsEntry(
            lrcUri = uri.toString(),
            title = title,
            artist = artistTag?.takeIf { it.isNotBlank() } ?: folder,
            folder = folder,
            audioUri = audioUri,
            lastModified = lastModified,
            lines = lines
        )
    }

    private fun saveCache(treeUri: Uri, entries: List<LocalLyricsEntry>) {
        runCatching {
            val index = LocalLyricsIndex(treeUri = treeUri.toString(), entries = entries)
            cacheFile.writeText(json.encodeToString(LocalLyricsIndex.serializer(), index))
        }.onFailure { Log.w(TAG, "saveCache: failed, next launch will rescan", it) }
    }

    private data class DocumentEntry(
        val documentId: String,
        val displayName: String,
        val mimeType: String,
        val lastModified: Long
    ) {
        val isDirectory get() = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
        val extension get() = displayName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val baseName get() = displayName.substringBeforeLast('.')
        val isLrc get() = !isDirectory && extension == "lrc"
        val isAudio get() = !isDirectory && extension in AUDIO_EXTENSIONS
    }

    companion object {
        private const val TAG = "LocalLyricsIndexer"
        private const val CACHE_FILE_NAME = "local_lyrics_index.json"
        private val AUDIO_EXTENSIONS =
            setOf("mp3", "flac", "m4a", "ogg", "opus", "wav", "aac", "wma", "mp4", "aiff")
        private val METADATA_TAG = Regex("""^\s*\[[a-zA-Z]+:[^]]*]\s*$""")
        private val ARTIST_TAG = Regex("""^\s*\[ar:([^]]*)]\s*$""", RegexOption.IGNORE_CASE)
        private val TIMESTAMP = Regex("""\[(\d{1,3}):(\d{2})[.:](\d{1,3})]""")
    }
}
