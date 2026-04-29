package com.example.triptip_yaron_and_alon.util

import android.widget.ImageView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.triptip_yaron_and_alon.R
import java.io.File

/**
 * Loads a profile image from an https URL or a local file path (legacy cache).
 */
fun ImageView.loadProfileImage(urlOrPath: String?, placeholder: Int = R.drawable.ic_profile_frame) {
    val s = urlOrPath?.trim().orEmpty()
    if (s.isEmpty()) {
        setImageResource(placeholder)
        return
    }
    if (s.startsWith("http", ignoreCase = true)) {
        load(s) {
            placeholder(placeholder)
            error(placeholder)
            transformations(CircleCropTransformation())
        }
    } else {
        try {
            load(File(s)) {
                placeholder(placeholder)
                error(placeholder)
                transformations(CircleCropTransformation())
            }
        } catch (_: Exception) {
            setImageResource(placeholder)
        }
    }
}
