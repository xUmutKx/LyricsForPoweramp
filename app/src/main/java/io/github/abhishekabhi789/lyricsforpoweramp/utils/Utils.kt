package io.github.abhishekabhi789.lyricsforpoweramp.utils

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.widget.Toast

fun Context.makeToast(messageRes: Int) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
    } else {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
        }
    }
}

/**@return document tree id of the uri*/
fun Uri.getTreeDocumentId(): String {
    return DocumentsContract.getTreeDocumentId(this)
}
