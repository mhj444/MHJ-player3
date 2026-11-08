package com.mhj.player.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhj.player.core.ui.R
import com.mhj.player.core.ui.theme.MhjTheme

/**
 * Board logos as cropped metallic bitmaps:
 * - Midnight: sharp MHJ flame mark (no circular crest)
 * - Porcelain: headphones + MHJ metallic mark
 */
@Composable
fun BrandLogoHeader(
    modifier: Modifier = Modifier,
    showWordmark: Boolean = true,
) {
    val colors = MhjTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(
                id = if (colors.isDark) R.drawable.chrome_mid_logo else R.drawable.chrome_porc_logo,
            ),
            contentDescription = "MHJ",
            modifier = Modifier
                .height(if (colors.isDark) 52.dp else 58.dp)
                .widthIn(max = if (colors.isDark) 200.dp else 168.dp)
                .fillMaxWidth(if (colors.isDark) 0.56f else 0.44f),
            contentScale = ContentScale.Fit,
        )
        if (showWordmark) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                // Hairlines are a porcelain signature; midnight board is mark + wordmark only
                if (!colors.isDark) {
                    Hairline(Modifier.width(34.dp), colors.primary.copy(alpha = 0.55f))
                }
                Text(
                    text = "MHJ Player",
                    color = if (colors.isDark) colors.primary else colors.onBackground,
                    fontSize = if (colors.isDark) 13.sp else 14.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.2.sp,
                )
                if (!colors.isDark) {
                    Hairline(Modifier.width(34.dp), colors.primary.copy(alpha = 0.55f))
                }
            }
        }
    }
}

@Composable
private fun Hairline(modifier: Modifier, color: Color) {
    Box(
        modifier = modifier
            .height(1.dp)
            .drawBehind {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 1.1.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            },
    )
}
