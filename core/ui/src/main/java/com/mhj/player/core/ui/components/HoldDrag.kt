package com.mhj.player.core.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange

/**
 * One gesture handler for a list row: a tap does [onTap]; holding it lifts the row, after which
 * moving the finger reports [onDrag] (up/down pixels) until it is let go ([onEnd]).
 * Keyed on [key] only, so the row moving to a new place does not cancel the drag.
 */
@Composable
fun Modifier.tapOrHoldDrag(
    key: Any,
    enabled: Boolean = true,
    onTap: () -> Unit,
    onHold: () -> Unit,
    onDrag: (Float) -> Unit,
    onEnd: () -> Unit,
): Modifier {
    val tap by rememberUpdatedState(onTap)
    val hold by rememberUpdatedState(onHold)
    val move by rememberUpdatedState(onDrag)
    val end by rememberUpdatedState(onEnd)
    val on by rememberUpdatedState(enabled)
    return this.pointerInput(key) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val lp = awaitLongPressOrCancellation(down.id)
            if (lp == null) {
                val ev = currentEvent
                if (ev.changes.isNotEmpty() && ev.changes.all { it.changedToUp() && !it.isConsumed }) tap()
                return@awaitEachGesture
            }
            if (!on) return@awaitEachGesture
            lp.consume()
            hold()
            drag(lp.id) { ch ->
                val d = ch.positionChange()
                ch.consume()
                move(d.y)
            }
            end()
        }
    }
}
