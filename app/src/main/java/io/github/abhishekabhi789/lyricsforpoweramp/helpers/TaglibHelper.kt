package io.github.abhishekabhi789.lyricsforpoweramp.helpers

import android.content.Context
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.kyant.taglib.PropertyMap
import com.kyant.taglib.TagLib
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
        fun getMetadata(): PropertyMap? {
            val metadata =
                runCatching { TagLib.getMetadata(fd.dup().detachFd(), false) }.getOrNull()
            return metadata?.propertyMap
        }

        fun setMetadata(newMetadata: PropertyMap): Boolean =
            runCatching {
                TagLib.savePropertyMap(fd.dup().detachFd(), newMetadata)
            }.getOrElse { false }


        fun updateLyricsTag(lyrics: String): Boolean {
            if (closed.get()) {
                Log.w(TAG, "updateLyricsTag: session closed")
                return false
            }
            val metadata = getMetadata() ?: PropertyMap()
            metadata[KEY_LYRICS] = arrayOf(lyrics)
            return setMetadata(metadata)
        }

        fun getLyricsTag(): String? {
            if (closed.get()) {
                Log.w(TAG, "getLyricsTag: session closed")
                return null
            }
            val metadata = getMetadata() ?: PropertyMap()
            return metadata[KEY_LYRICS]?.firstOrNull()
        }

        fun saveModifiedFile(): Boolean {
            if (closed.get()) {
                Log.w(TAG, "saveModifiedFile: session closed")
                return false
            }
            if (!trackFile.exists()) {
                Log.e(TAG, "saveModifiedFile: file not exists returning")
                return false
            }

            val fileName = trackFile.name
            if (fileName.isNullOrBlank()) {
                Log.e(TAG, "saveModifiedFile: invalid filename; returning")
                return false
            }

            if (!tempFile.exists()) {
                Log.e(TAG, "saveModifiedFile: temp file not found; returning")
                return false
            }

            return try {
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
        suspend fun closeSafely() = withContext(Dispatchers.IO) {
            if (closed.compareAndSet(false, true)) {
                fd.closeQuietly()
                tempFile.delete()
            }
        }
    }

    suspend fun getTaglibSession(
        filePath: String,
        onError: (error: StorageHelper.Result) -> Unit
    ): TagLibSession? = withContext(Dispatchers.IO) {
        try {
            val parentFolder = StorageHelper.getParentFolder(context, filePath) ?: run {
                Log.e(TAG, "prepareFile: no access to file path; returning")
                onError(StorageHelper.Result.NO_PERMISSION)
                return@withContext null
            }
            val trackFile = parentFolder.findFile(filePath.substringAfterLast("/"))
            if (trackFile == null || !trackFile.exists() || !trackFile.isFile) {
                Log.e(TAG, "prepareFile: failed to find track file; returning")
                onError(StorageHelper.Result.NO_PERMISSION)//may get fixed by re-selecting the path
                return@withContext null
            }
            context.contentResolver.openInputStream(trackFile.uri).use { inputStream ->
                if (inputStream == null) {
                    Log.e(TAG, "prepareFile: input stream is null; returning")
                    onError(StorageHelper.Result.INVALID_FILEPATH)
                    return@withContext null
                }
                val fileName = trackFile.name
                if (fileName.isNullOrBlank()) {
                    Log.e(TAG, "prepareFile: invalid filename; returning")
                    onError(StorageHelper.Result.INVALID_FILEPATH)
                    return@withContext null
                }
                val tempFile = File(context.cacheDir, fileName)
                tempFile.outputStream().use { os -> inputStream.copyTo(os) }
                val fd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_WRITE)
                return@withContext TagLibSession(fd, trackFile, tempFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "prepareFile: ${e.message}", e)
            return@withContext null
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
