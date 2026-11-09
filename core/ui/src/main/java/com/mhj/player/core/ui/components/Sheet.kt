package com.mhj.player.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.mhj.player.core.ui.theme.MhjColors
import com.mhj.player.core.ui.theme.MhjTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * A panel that slides in from the top or the bottom over frosted glass. Tap outside or drag the
 * little handle to close it.
 */
@Composable
fun MhjSheet(
    visible: Boolean,
    fromTop: Boolean,
    heightFraction: Float,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = MhjTheme.colors
    val r = 30.dp
    val shape = if (fromTop) RoundedCornerShape(bottomStart = r, bottomEnd = r) else RoundedCornerShape(topStart = r, topEnd = r)
    val style = if (c.isDark) {
        HazeStyle(
            backgroundColor = Color(0xFF0E0E10),
            tints = listOf(HazeTint(Color(0xC2131315)), HazeTint(MhjColors.Gold.copy(alpha = 0.03f))),
            blurRadius = 26.dp,
            noiseFactor = 0.05f,
        )
    } else {
        HazeStyle(
            backgroundColor = Color(0xFFF4EEE6),
            tints = listOf(HazeTint(Color(0xD9F8F2EA))),
            blurRadius = 26.dp,
            noiseFactor = 0.04f,
        )
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(160)) + slideInVertically(tween(280)) { if (fromTop) -it / 2 else it / 2 },
        exit = fadeOut(tween(140)) + slideOutVertically(tween(220)) { if (fromTop) -it / 2 else it / 2 },
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (c.isDark) 0.35f else 0.18f))
                    .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            )
            Column(
                Modifier
                    .align(if (fromTop) Alignment.TopCenter else Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(heightFraction)
                    .raised(r, 10.dp, if (c.isDark) Color(0xFF121214) else Color(0xFFF7F1E9), Emboss.light, Emboss.dark)
                    .clip(shape)
                    .then(if (hazeState != null) Modifier.hazeEffect(state = hazeState, style = style) else Modifier.background(style.backgroundColor))
                    .border(1.dp, Emboss.rim(0.6f), shape)
                    // Catch touches on empty parts of the panel (so they don't close it) without
                    // taking taps away from the rows inside.
                    .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false) } },
            ) {
                if (!fromTop) Handle(onDismiss, down = true)
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, top = if (fromTop) 18.dp else 2.dp, bottom = if (fromTop) 2.dp else 16.dp),
                    content = content,
                )
                if (fromTop) Handle(onDismiss, down = false)
            }
        }
    }
}

@Composable
private fun Handle(onDismiss: () -> Unit, down: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(26.dp)
            .pointerInput(down) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onVerticalDrag = { ch, d -> ch.consume(); total += d },
                    onDragEnd = { if ((down && total > 50f) || (!down && total < -50f)) onDismiss() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = 44.dp, height = 5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Brush.horizontalGradient(listOf(MhjColors.GoldLo, MhjColors.GoldHi, MhjColors.GoldLo))),
        )
    }
}
