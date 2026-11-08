package com.mhj.player.core.ui.gestures

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Edge-aware vertical swipe helpers for drawer panels.
 * Horizontal music→video navigation is handled by a pager (no mode button).
 */
fun Modifier.playerEdgeSwipes(
    topEdge: Dp = 96.dp,
    bottomEdge: Dp = 120.dp,
    threshold: Dp = 56.dp,
    onSwipeDownFromTop: () -> Unit,
    onSwipeUpFromBottom: () -> Unit,
): Modifier = this.then(
    Modifier.pointerInput(topEdge, bottomEdge, threshold) {
        val topPx = topEdge.toPx()
        val bottomPx = size.height - bottomEdge.toPx()
        val thresholdPx = threshold.toPx()
        var origin: Offset? = null
        var total = 0f
        detectVerticalDragGestures(
            onDragStart = { offset ->
                origin = offset
                total = 0f
            },
            onVerticalDrag = { _, dragAmount ->
                total += dragAmount
            },
            onDragEnd = {
                val start = origin ?: return@detectVerticalDragGestures
                if (abs(total) < thresholdPx) return@detectVerticalDragGestures
                when {
                    start.y <= topPx && total > 0 -> onSwipeDownFromTop()
                    start.y >= bottomPx && total < 0 -> onSwipeUpFromBottom()
                }
            },
            onDragCancel = {
                origin = null
                total = 0f
            },
        )
    },
)
