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
import java.io.File


class TaglibHelper(private val context: Context) {
    private var fd: ParcelFileDescriptor? = null
    private var file: DocumentFile? = null

    suspend fun prepareFile(
        filePath: String,
        onError: (error: StorageHelper.Result) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val parentFolder = StorageHelper.getParentFolder(context, filePath)
        if (parentFolder == null) {
            Log.e(TAG, "prepareFile: no access to file path; returning")
            onError(StorageHelper.Result.NO_PERMISSION)
            return@withContext false
        }
        val trackFile = parentFolder.findFile(filePath.substringAfterLast("/"))
        if (trackFile == null || !trackFile.exists() || !trackFile.isFile) {
            Log.e(TAG, "prepareFile: failed to find track file; returning")
            onError(StorageHelper.Result.NO_PERMISSION)//may get fixed by re-selecting the path
            return@withContext false
        }
        this@TaglibHelper.file = trackFile
        context.contentResolver.openInputStream(trackFile.uri).use { inputStream ->
            if (inputStream == null) {
                Log.e(TAG, "prepareFile: input stream is null; returning")
                onError(StorageHelper.Result.INVALID_FILEPATH)
                return@withContext false
            }
            val fileName = trackFile.name
            if (fileName.isNullOrBlank()) {
                Log.e(TAG, "prepareFile: invalid filename; returning")
                onError(StorageHelper.Result.INVALID_FILEPATH)
                return@withContext false
            }
            val outputFile = File(context.cacheDir, fileName)
            outputFile.outputStream().use { outputStream -> inputStream.copyTo(outputStream) }
            this@TaglibHelper.fd =
                ParcelFileDescriptor.open(outputFile, ParcelFileDescriptor.MODE_READ_WRITE)
            return@withContext true
        }
    }

    private fun getMetadata(): PropertyMap? {
        if (fd == null) {
            Log.e(TAG, "getMetadata: fd is null; returning")
            return null
        }
        val metadata = TagLib.getMetadata(fd!!.dup().detachFd(), false)
        return metadata?.propertyMap
    }

    private fun setMetadata(newMetadata: PropertyMap): Boolean {
        if (fd == null) {
            Log.e(TAG, "setMetadata: fd is null; returning")
            return false
        }
        return TagLib.savePropertyMap(fd!!.dup().detachFd(), newMetadata)
    }

    fun updateLyricsTag(lyrics: String): Boolean {
        val metadata = getMetadata() ?: PropertyMap()
        metadata[KEY_LYRICS] = arrayOf(lyrics)
        return setMetadata(metadata)
    }

    fun getLyricsTag(): String? {
        val metadata = getMetadata() ?: PropertyMap()
        return metadata[KEY_LYRICS]?.firstOrNull()
    }

    fun fixMetadata(lyrics: Lyrics): Boolean {
        val metadata = getMetadata() ?: PropertyMap()
        metadata[KEY_TITLE] = arrayOf(lyrics.trackName)
        lyrics.artistName?.let { metadata[KEY_ARTIST] = arrayOf(it) }
        lyrics.albumName?.let { metadata[KEY_ALBUM] = arrayOf(it) }
        return setMetadata(metadata)
    }

    fun saveModifiedFile(): Boolean {
        if (fd == null) {
            Log.e(TAG, "saveModifiedFile: fd is null; returning")
            return false
        }

        if (file == null || file?.exists() == false) {
            Log.e(TAG, "saveModifiedFile: file not exists returning")
            return false
        }

        val fileName = file?.name
        if (fileName.isNullOrBlank()) {
            Log.e(TAG, "saveModifiedFile: invalid filename; returning")
            return false
        }

        val tempFile = File(context.cacheDir, fileName)
        if (!tempFile.exists()) {
            Log.e(TAG, "saveModifiedFile: temp file not found; returning")
            return false
        }

        return try {
            context.contentResolver.openOutputStream(file!!.uri, "wt")?.use { outputStream ->
                tempFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
                fd?.closeQuietly()
                if (tempFile.delete()) {
                    Log.d(TAG, "saveModifiedFile: temp file deleted successfully")
                } else {
                    Log.w(TAG, "saveModifiedFile: failed to delete temp file")
                }
            } ?: run {
                Log.e(TAG, "saveModifiedFile: failed to open output stream; returning")
                return false
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "saveModifiedFile: ${e.message}", e)
            false
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
