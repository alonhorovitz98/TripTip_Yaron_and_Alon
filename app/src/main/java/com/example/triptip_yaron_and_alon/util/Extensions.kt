package com.example.triptip_yaron_and_alon.util

import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

/**
 * Extension functions for common operations
 */

/**
 * Snackbar needs a view attached to a window. Tab / ViewPager fragments often fire
 * observers during [Fragment.performStart] before the view is attached — use activity content as fallback.
 */
fun Fragment.resolveSnackbarAnchor(): View? {
    if (!isAdded) return null
    val v = view
    if (v != null && v.isAttachedToWindow) return v
    return activity?.findViewById(android.R.id.content)
}

/**
 * Shows a Snackbar with error message
 */
fun Fragment.showError(message: String, duration: Int = Snackbar.LENGTH_LONG) {
    val anchor = resolveSnackbarAnchor() ?: return
    Snackbar.make(anchor, message, duration)
        .setAction("Dismiss") { }
        .show()
}

/**
 * Shows a Snackbar with success message
 */
fun Fragment.showSuccess(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
    val anchor = resolveSnackbarAnchor() ?: return
    Snackbar.make(anchor, message, duration).show()
}

/**
 * Shows a Toast message
 */
fun Fragment.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    context?.let {
        Toast.makeText(it, message, duration).show()
    }
}

/**
 * Shows a Snackbar with error message (View extension)
 */
fun View.showError(message: String, duration: Int = Snackbar.LENGTH_LONG) {
    Snackbar.make(this, message, duration)
        .setAction("Dismiss") { }
        .show()
}

/**
 * Shows a Snackbar with success message (View extension)
 */
fun View.showSuccess(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
    Snackbar.make(this, message, duration).show()
}

/**
 * Hides the view
 */
fun View.hide() {
    visibility = View.GONE
}

/**
 * Shows the view
 */
fun View.show() {
    visibility = View.VISIBLE
}

/**
 * Makes the view invisible (but keeps space)
 */
fun View.invisible() {
    visibility = View.INVISIBLE
}

