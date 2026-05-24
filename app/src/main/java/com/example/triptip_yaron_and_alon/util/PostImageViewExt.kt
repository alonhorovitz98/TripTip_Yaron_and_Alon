package com.example.triptip_yaron_and_alon.util

import android.net.Uri
import android.view.View
import android.widget.ImageView
import coil.load
import com.example.triptip_yaron_and_alon.R
import java.io.File

/**
 * Loads a post image from Firebase Storage (https), content URI, or local file path.
 * Used in feed and post details so images work across devices.
 */
fun ImageView.loadPostImage(
    urlOrPath: String?,
    placeholder: Int = R.drawable.ic_launcher_background,
    hideWhenEmpty: Boolean = true
) {
    val s = urlOrPath?.trim().orEmpty()
    if (s.isEmpty()) {
        if (hideWhenEmpty) visibility = View.GONE
        return
    }
    visibility = View.VISIBLE
    when {
        s.startsWith("http", ignoreCase = true) -> {
            load(s) {
                placeholder(placeholder)
                error(placeholder)
                crossfade(true)
            }
        }
        s.startsWith("content:", ignoreCase = true) -> {
            load(Uri.parse(s)) {
                placeholder(placeholder)
                error(placeholder)
                crossfade(true)
            }
        }
        else -> {
            val file = File(s)
            if (file.exists()) {
                load(file) {
                    placeholder(placeholder)
                    error(placeholder)
                    crossfade(true)
                }
            } else {
                // Legacy local path from another device — show placeholder, keep image area visible
                setImageResource(placeholder)
            }
        }
    }
}

/** Strip Google Places pipe suffix from stored location strings for display. */
fun displayLocationName(location: String?): String? {
    val raw = location?.trim().orEmpty()
    if (raw.isEmpty()) return null
    return raw.substringBefore("|").trim().ifEmpty { null }
}
