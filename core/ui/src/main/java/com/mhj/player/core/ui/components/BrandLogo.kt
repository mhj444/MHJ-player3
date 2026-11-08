package com.mhj.player.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhj.player.core.ui.R
import com.mhj.player.core.ui.theme.MhjColors
import com.mhj.player.core.ui.theme.MhjTheme

/**
 * Centered header mark using the cropped raw MHJ logo (not the tall source canvas).
 * Theme picks transparent gold mark on light, midnight plate on dark.
 */
@Composable
fun BrandLogoHeader(
    modifier: Modifier = Modifier,
    showWordmark: Boolean = true,
) {
    val colors = MhjTheme.colors
    val logoRes = if (colors.isDark) {
        R.drawable.logo_mhj_raw
    } else {
        R.drawable.logo_mhj_raw
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Image(
            painter = painterResource(id = logoRes),
            contentDescription = "MHJ",
            modifier = Modifier
                .height(46.dp)
                .widthIn(max = 168.dp)
                .fillMaxWidth(0.46f),
            contentScale = ContentScale.Fit,
        )
        if (showWordmark) {
            Text(
                text = "MHJ Player",
                color = colors.primary,
                fontSize = 13.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.6.sp,
            )
            // Decorative gold arch under wordmark (mockup cue)
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .height(10.dp)
                    .fillMaxWidth(0.28f)
                    .drawBehind {
                        val stroke = 2.2.dp.toPx()
                        drawArc(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    MhjColors.GoldDeep.copy(alpha = 0.15f),
                                    MhjColors.GoldBright,
                                    MhjColors.Gold,
                                    MhjColors.GoldDeep.copy(alpha = 0.15f),
                                ),
                            ),
                            startAngle = 200f,
                            sweepAngle = 140f,
                            useCenter = false,
                            topLeft = Offset(0f, -size.height),
                            size = androidx.compose.ui.geometry.Size(size.width, size.height * 2.4f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = stroke,
                                cap = StrokeCap.Round,
                            ),
                        )
                    },
            )
        }
    }
}
