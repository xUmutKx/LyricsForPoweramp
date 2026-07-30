package io.github.abhishekabhi789.lyricsforpoweramp.helpers

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Album art for the offline library, read straight out of the audio file's tags. Nothing is
 * queried from MediaStore, so this works with the SAF permission the folder picker already gave
 * us and needs no media permission of its own.
 */
object LocalArtLoader {

    private const val TAG = "LocalArtLoader"
    private const val CACHE_ENTRIES = 64
    private const val THUMBNAIL_SIZE = 256

    private val cache = LruCache<String, ImageBitmap>(CACHE_ENTRIES)
    private val withoutArt = mutableSetOf<String>()

    suspend fun load(context: Context, audioUri: String?): ImageBitmap? {
        if (audioUri.isNullOrBlank()) return null
        cache.get(audioUri)?.let { return it }
        synchronized(withoutArt) { if (audioUri in withoutArt) return null }
        return withContext(Dispatchers.IO) {
            val bytes = readEmbeddedArt(context, audioUri)
            if (bytes == null) {
                synchronized(withoutArt) { withoutArt.add(audioUri) }
                return@withContext null
            }
            val bitmap = decodeScaled(bytes)?.asImageBitmap()
            if (bitmap != null) cache.put(audioUri, bitmap)
            else synchronized(withoutArt) { withoutArt.add(audioUri) }
            bitmap
        }
    }

    private fun readEmbeddedArt(context: Context, audioUri: String): ByteArray? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, audioUri.toUri())
            retriever.embeddedPicture
        } catch (e: Exception) {
            Log.d(TAG, "readEmbeddedArt: no art for $audioUri", e)
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    /** Decodes at roughly thumbnail size - full covers are far bigger than a list row needs. */
    private fun decodeScaled(bytes: ByteArray) = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / sample > THUMBNAIL_SIZE * 2) sample *= 2
        BitmapFactory.decodeByteArray(
            bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }
        )
    }.getOrNull()
}
