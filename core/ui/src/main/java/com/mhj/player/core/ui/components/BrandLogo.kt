package com.mhj.player.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhj.player.core.ui.R
import com.mhj.player.core.ui.theme.MhjTheme

@Composable
fun BrandLogoHeader(
    modifier: Modifier = Modifier,
    showWordmark: Boolean = true,
) {
    val colors = MhjTheme.colors
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_mhj),
            contentDescription = "MHJ",
            modifier = Modifier.height(42.dp),
            contentScale = ContentScale.Fit,
        )
        if (showWordmark) {
            Text(
                text = "MHJ Player",
                color = colors.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.2.sp,
            )
        }
    }
}
