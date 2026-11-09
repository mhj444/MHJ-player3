package com.mhj.player.core.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhj.player.core.ui.theme.MhjTheme

/** Full text of one saved transcript: read, select, copy or share. */
@Composable
fun TranscriptViewerOverlay(
    visible: Boolean,
    title: String,
    body: String,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    BackHandler(onBack = onDismiss)
    val context = LocalContext.current
    val c = MhjTheme.colors
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        RaisedCard(
            Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.82f)
                .clickable(remember { MutableInteractionSource() }, indication = null) {},
            radius = 26.dp,
            padding = 16.dp,
        ) {
            SheetTitle(title.ifBlank { "متن" }, "متن تبدیل‌شده از صدا") {
                RaisedIconButton(Icons.Outlined.Close, "بستن", onDismiss, size = 40.dp, iconSize = 20.dp)
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(c.recess.copy(alpha = 0.6f), androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .padding(12.dp),
            ) {
                SelectionContainer {
                    Text(
                        body,
                        color = c.onBackground,
                        fontSize = 15.sp,
                        lineHeight = 26.sp,
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                RaisedIconButton(Icons.Outlined.ContentCopy, "کپی", {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText(title, body))
                    Toast.makeText(context, "کپی شد", Toast.LENGTH_SHORT).show()
                }, size = 46.dp, iconSize = 22.dp)
                RaisedIconButton(Icons.Outlined.Share, "اشتراک", { shareText(context, title, body) }, size = 46.dp, iconSize = 22.dp)
            }
        }
    }
}

fun shareText(context: Context, title: String, body: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    runCatching {
        context.startActivity(Intent.createChooser(send, "اشتراک متن").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
