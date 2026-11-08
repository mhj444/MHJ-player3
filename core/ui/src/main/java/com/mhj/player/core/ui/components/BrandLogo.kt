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
 * Board-faithful centered brand header:
 * metallic MHJ mark (flame + headphones) + "MHJ Player" with flanking rules.
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
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Image(
            painter = painterResource(
                id = if (colors.isDark) R.drawable.logo_mhj_header_night else R.drawable.logo_mhj_header,
            ),
            contentDescription = "MHJ",
            modifier = Modifier
                .height(58.dp)
                .widthIn(max = 172.dp)
                .fillMaxWidth(0.48f),
            contentScale = ContentScale.Fit,
        )
        if (showWordmark) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Hairline(Modifier.width(28.dp), colors.primary.copy(alpha = 0.55f))
                Text(
                    text = "MHJ Player",
                    color = colors.primary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.4.sp,
                )
                Hairline(Modifier.width(28.dp), colors.primary.copy(alpha = 0.55f))
            }
        }
    }
}

@Composable
private fun Hairline(modifier: Modifier, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = modifier
            .height(1.dp)
            .drawBehind {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 1.2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            },
    )
}
