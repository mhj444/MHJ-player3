package com.mhj.player.core.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhj.player.core.ui.theme.MhjColors
import com.mhj.player.core.ui.theme.MhjTheme
import com.mhj.player.core.ui.theme.Vazir
import dev.chrisbanes.haze.HazeState

/** One playlist in the picker. [hasAll] = every chosen song is already in it. */
data class PickerPlaylist(val id: Long, val name: String, val count: Int, val hasAll: Boolean, val isFavorites: Boolean)

/**
 * "Add to playlist" panel: a search box and two columns of cards (with room for many playlists).
 * Tap a card to add and close; the dashed card makes a new playlist.
 */
@Composable
fun PlaylistPickerSheet(
    visible: Boolean,
    subtitle: String,
    playlists: List<PickerPlaylist>,
    onPick: (PickerPlaylist) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
    hazeState: HazeState? = null,
    backEnabled: Boolean = true,
) {
    val c = MhjTheme.colors
    var query by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    LaunchedEffect(visible) { if (!visible) { query = ""; creating = false; newName = "" } }
    BackHandler(enabled = visible && backEnabled) { if (creating) creating = false else onDismiss() }

    MhjSheet(visible = visible, fromTop = false, heightFraction = 0.80f, onDismiss = onDismiss, hazeState = hazeState) {
        SheetTitle("افزودن به پلی‌لیست", subtitle)
        Field(query, { query = it }, "جستجوی پلی‌لیست…", Icons.Outlined.Search)
        Spacer(Modifier.height(12.dp))
        if (creating) {
            val focus = remember { FocusRequester() }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { Field(newName, { newName = it }, "نام پلی‌لیست تازه", Icons.Outlined.Add, Modifier.focusRequester(focus)) }
                Spacer(Modifier.width(10.dp))
                RaisedIconButton(Icons.Outlined.Check, "ساختن", {
                    val n = newName.trim()
                    if (n.isNotEmpty()) onCreate(n)
                }, size = 48.dp, iconSize = 24.dp, active = newName.isNotBlank())
            }
            LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
            Spacer(Modifier.height(12.dp))
        }
        val shown = playlists.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(4.dp, 6.dp),
        ) {
            if (!creating && query.isBlank()) item(key = "new") {
                Column(
                    Modifier
                        .height(96.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MhjColors.Gold.copy(alpha = 0.07f))
                        .border(1.5.dp, MhjColors.Gold.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
                        .clickable { creating = true },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Outlined.Add, null, tint = MhjColors.Gold, modifier = Modifier.size(28.dp))
                    Text("پلی‌لیست تازه", color = if (c.isDark) MhjColors.GoldHi else MhjColors.GoldLo, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            items(shown, key = { it.id }) { p ->
                RaisedCard(Modifier.height(96.dp), radius = 18.dp, depth = 5.dp, padding = 12.dp, rimAlpha = if (p.hasAll) 0.9f else 0.4f, onClick = { onPick(p) }) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(32.dp).clip(CircleShape).background(MhjColors.Gold.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                            if (p.isFavorites) Icon(Icons.Outlined.Favorite, null, tint = Color(0xFFE0475B), modifier = Modifier.size(17.dp))
                            else Icon(Icons.Outlined.MusicNote, null, tint = MhjColors.Gold, modifier = Modifier.size(17.dp))
                        }
                        Spacer(Modifier.weight(1f))
                        if (p.hasAll) Box(Modifier.size(22.dp).clip(CircleShape).background(MhjColors.Gold), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Check, "از قبل اضافه شده", tint = Color(0xFF111111), modifier = Modifier.size(15.dp))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(p.name, color = c.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${Fa.num(p.count)} آهنگ", color = c.muted, fontSize = 11.sp)
                }
            }
            if (shown.isEmpty() && query.isNotBlank()) item(span = { GridItemSpan(2) }) {
                Text("پلی‌لیستی با این نام نیست", color = c.muted, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
            }
        }
    }
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, hint: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    val c = MhjTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(c.recess)
            .border(1.dp, Color.Black.copy(alpha = if (c.isDark) 0.6f else 0.12f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = c.muted, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = c.onBackground, fontSize = 14.sp, fontFamily = Vazir),
            cursorBrush = SolidColor(MhjColors.Gold),
            modifier = modifier.weight(1f),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(hint, color = c.muted, fontSize = 13.sp)
                inner()
            },
        )
    }
}
