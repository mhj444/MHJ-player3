package com.mhj.player.core.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhj.player.core.ui.theme.MhjColors
import com.mhj.player.core.ui.theme.MhjTheme

@Composable
fun TranscriptViewerOverlay(
    visible: Boolean,
    title: String,
    body: String,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    val context = LocalContext.current
    val c = MhjTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onDismiss),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xF214141A))
                .border(1.dp, MhjColors.Gold.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                .clickable(enabled = false) {}
                .padding(18.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title.ifBlank { "متن" },
                    color = MhjColors.GoldHi,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "بستن",
                    color = c.muted,
                    modifier = Modifier
                        .clickable(onClick = onDismiss)
                        .padding(8.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                body,
                color = c.onBackground,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .verticalScroll(rememberScrollState()),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "کپی",
                color = MhjColors.GoldHi,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.End)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText(title, body))
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}
