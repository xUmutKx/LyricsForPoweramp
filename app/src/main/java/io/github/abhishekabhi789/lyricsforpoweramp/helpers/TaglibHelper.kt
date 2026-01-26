package io.github.abhishekabhi789.lyricsforpoweramp.helpers

import android.content.Context
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.kyant.taglib.PropertyMap
import com.kyant.taglib.TagLib
import io.github.abhishekabhi789.lyricsforpoweramp.model.Lyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.internal.closeQuietly
import java.io.Closeable
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.concurrent.atomics.ExperimentalAtomicApi

class TaglibHelper @Inject constructor(private val context: Context) {

    @OptIn(ExperimentalAtomicApi::class)
    inner class TagLibSession(
        private val fd: ParcelFileDescriptor,
        private val trackFile: DocumentFile,
        private val tempFile: File
    ) : Closeable {
        private val closed = AtomicBoolean(false)
        private fun getMetadata(): PropertyMap? {
            val metadata =
                runCatching { TagLib.getMetadata(fd.dup().detachFd(), false) }.getOrNull()
            return metadata?.propertyMap
        }

        private fun setMetadata(newMetadata: PropertyMap): Boolean =
            runCatching {
                TagLib.savePropertyMap(fd.dup().detachFd(), newMetadata)
            }.getOrElse { false }


        suspend fun updateLyricsTag(lyrics: String): Boolean = withContext(Dispatchers.IO) {
            if (closed.get()) {
                Log.w(TAG, "updateLyricsTag: session closed")
                return@withContext false
            }
            val metadata = getMetadata() ?: PropertyMap()
            metadata[KEY_LYRICS] = arrayOf(lyrics)
            return@withContext setMetadata(metadata)
        }

        suspend fun getLyricsTag(): String? = withContext(Dispatchers.IO) {
            if (closed.get()) {
                Log.w(TAG, "getLyricsTag: session closed")
                return@withContext null
            }
            val metadata = getMetadata() ?: PropertyMap()
            return@withContext metadata[KEY_LYRICS]?.firstOrNull()
        }

        suspend fun fixMetadata(lyrics: Lyrics): Boolean = withContext(Dispatchers.IO) {
            if (closed.get()) {
                Log.w(TAG, "fixMetadata: session closed")
                return@withContext false
            }
            val metadata = getMetadata() ?: PropertyMap()
            metadata[KEY_TITLE] = arrayOf(lyrics.trackName)
            lyrics.artistName?.let { metadata[KEY_ARTIST] = arrayOf(it) }
            lyrics.albumName?.let { metadata[KEY_ALBUM] = arrayOf(it) }
            return@withContext setMetadata(metadata)
        }

        suspend fun saveModifiedFile(): Boolean = withContext(Dispatchers.IO) {
            if (closed.get()) {
                Log.w(TAG, "saveModifiedFile: session closed")
                return@withContext false
            }
            if (!trackFile.exists()) {
                Log.e(TAG, "saveModifiedFile: file not exists returning")
                return@withContext false
            }

            val fileName = trackFile.name
            if (fileName.isNullOrBlank()) {
                Log.e(TAG, "saveModifiedFile: invalid filename; returning")
                return@withContext false
            }

            if (!tempFile.exists()) {
                Log.e(TAG, "saveModifiedFile: temp file not found; returning")
                return@withContext false
            }

            return@withContext try {
                context.contentResolver.openOutputStream(trackFile.uri, "wt")?.use { outputStream ->
                    tempFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: run {
                    Log.e(TAG, "saveModifiedFile: failed to open output stream; returning")
                    return@run false
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "saveModifiedFile: ${e.message}", e)
                false
            }
        }

        override fun close() {
            if (closed.compareAndSet(false, true)) {
                fd.closeQuietly()
                tempFile.delete()
            }
        }
    }

    suspend fun getTaglibSession(
        filePath: String,
        onError: (error: StorageHelper.Result) -> Unit
    ): TagLibSession? {
        var trackFile: DocumentFile?
        try {
            val parentFolder = StorageHelper.getParentFolder(context, filePath)
            if (parentFolder == null) {
                Log.e(TAG, "prepareFile: no access to file path; returning")
                onError(StorageHelper.Result.NO_PERMISSION)
                return null
            }
            trackFile = parentFolder.findFile(filePath.substringAfterLast("/"))
            if (trackFile == null || !trackFile.exists() || !trackFile.isFile) {
                Log.e(TAG, "prepareFile: failed to find track file; returning")
                onError(StorageHelper.Result.NO_PERMISSION)//may get fixed by re-selecting the path
                return null
            }
            context.contentResolver.openInputStream(trackFile.uri).use { inputStream ->
                if (inputStream == null) {
                    Log.e(TAG, "prepareFile: input stream is null; returning")
                    onError(StorageHelper.Result.INVALID_FILEPATH)
                    return null
                }
                val fileName = trackFile.name
                if (fileName.isNullOrBlank()) {
                    Log.e(TAG, "prepareFile: invalid filename; returning")
                    onError(StorageHelper.Result.INVALID_FILEPATH)
                    return null
                }
                val tempFile = File(context.cacheDir, fileName)
                tempFile.outputStream()
                    .use { outputStream -> inputStream.copyTo(outputStream) }
                val fd = withContext(Dispatchers.IO) {
                    ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_WRITE)
                }
                return TagLibSession(fd, trackFile, tempFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "prepareFile: ${e.message}", e)
            return null
        }
    }

    companion object {
        private const val TAG = "TaglibHelper"
        const val KEY_TITLE = "TITLE"
        const val KEY_ALBUM = "ALBUM"
        const val KEY_ARTIST = "ARTIST"
        const val KEY_LYRICS = "LYRICS"
    }
}
