package com.example.triptip_yaron_and_alon.domain.model

/**
 * One row on a day: POST → [value] is postId; PLACE → [value] is the place name (string only).
 */
data class DayItem(
    val id: String,
    val dayId: String,
    val type: String,
    val value: String,
    val sortOrder: Int,
    /** Filled for POST rows when loading for UI preview. */
    val post: Post? = null
) {
    fun isPost() = type == DayItemType.POST
    fun isPlace() = type == DayItemType.PLACE
}

object DayItemType {
    const val POST = "POST"
    const val PLACE = "PLACE"
}
