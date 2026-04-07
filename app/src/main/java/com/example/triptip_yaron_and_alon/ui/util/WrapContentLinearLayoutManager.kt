package com.example.triptip_yaron_and_alon.ui.util

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * LinearLayoutManager that measures all children so RecyclerView with wrap_content
 * inside a ScrollView/NestedScrollView shows the full list instead of collapsing.
 */
class WrapContentLinearLayoutManager(context: Context) : LinearLayoutManager(context) {

    override fun onMeasure(
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State,
        widthSpec: Int,
        heightSpec: Int
    ) {
        val heightMode = View.MeasureSpec.getMode(heightSpec)
        if (heightMode == View.MeasureSpec.UNSPECIFIED || heightMode == View.MeasureSpec.AT_MOST) {
            val itemCount = state.itemCount
            if (itemCount == 0) {
                super.onMeasure(recycler, state, widthSpec, heightSpec)
                return
            }
            var totalHeight = 0
            for (i in 0 until itemCount) {
                val view = recycler.getViewForPosition(i) ?: continue
                measureChildWithMargins(view, 0, 0)
                totalHeight += getDecoratedMeasuredHeight(view)
            }
            val height = totalHeight.coerceAtLeast(0)
            setMeasuredDimension(
                View.MeasureSpec.getSize(widthSpec),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
            )
        } else {
            super.onMeasure(recycler, state, widthSpec, heightSpec)
        }
    }
}
