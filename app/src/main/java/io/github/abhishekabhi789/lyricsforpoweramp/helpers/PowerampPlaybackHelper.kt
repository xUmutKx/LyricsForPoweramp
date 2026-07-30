package io.github.abhishekabhi789.lyricsforpoweramp.helpers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.maxmpz.poweramp.player.PowerampAPI

/** Hands an audio file to Poweramp and asks it to start playing at a given position. */
object PowerampPlaybackHelper {

    private const val TAG = "PowerampPlaybackHelper"

    /**
     * Poweramp reads the seek position from `pos` in seconds. Older builds only honour the
     * millisecond extra, so both are sent - the one that isn't understood is ignored.
     */
    private const val EXTRA_TRACK_POSITION_MS = "com.maxmpz.audioplayer.TRACK_POSITION"

    fun isPowerampInstalled(context: Context): Boolean {
        return runCatching {
            context.packageManager.getPackageInfo(PowerampAPI.PACKAGE_NAME, 0) != null
        }.getOrDefault(false)
    }

    /**
     * @param positionMs where playback should start, negative or null to play from the beginning.
     * @return false when the command couldn't be delivered - the caller should offer the chooser.
     */
    fun openToPlay(context: Context, audioUri: Uri, positionMs: Long?): Boolean {
        if (!isPowerampInstalled(context)) {
            Log.i(TAG, "openToPlay: Poweramp is not installed")
            return false
        }
        return runCatching {
            val intent = Intent(PowerampAPI.ACTION_API_COMMAND).apply {
                component = ComponentName(PowerampAPI.PACKAGE_NAME, PowerampAPI.API_RECEIVER_NAME)
                setDataAndType(audioUri, "audio/*")
                putExtra(PowerampAPI.EXTRA_COMMAND, PowerampAPI.Commands.OPEN_TO_PLAY)
                if (positionMs != null && positionMs >= 0) {
                    putExtra(PowerampAPI.EXTRA_POSITION, (positionMs / 1000).toInt())
                    putExtra(EXTRA_TRACK_POSITION_MS, positionMs)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            }
            context.grantUriPermissionSafely(audioUri)
            context.sendBroadcast(intent)
            context.bringPowerampToFront()
            true
        }.onFailure { Log.e(TAG, "openToPlay: failed for $audioUri", it) }.getOrDefault(false)
    }

    /**
     * The API command alone only starts playback in the background, which looks like nothing
     * happened. Opening Poweramp right after puts the player on screen at the line that was tapped.
     */
    private fun Context.bringPowerampToFront() {
        runCatching {
            val launchIntent = packageManager.getLaunchIntentForPackage(PowerampAPI.PACKAGE_NAME)
                ?: return
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(launchIntent)
        }.onFailure { Log.w(TAG, "bringPowerampToFront: couldn't open Poweramp", it) }
    }

    /** Opens the file with whatever player the user picks, used when Poweramp can't take it. */
    fun openWithChooser(context: Context, audioUri: Uri, chooserTitle: String): Boolean {
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(audioUri, "audio/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, chooserTitle))
            true
        }.onFailure { Log.e(TAG, "openWithChooser: failed for $audioUri", it) }.getOrDefault(false)
    }

    /**
     * Forwarding a granted SAF permission works through the intent flag alone, but an explicit
     * grant helps on the builds where Poweramp reads the uri after the broadcast is handled.
     */
    private fun Context.grantUriPermissionSafely(uri: Uri) {
        runCatching {
            grantUriPermission(
                PowerampAPI.PACKAGE_NAME, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }
}
