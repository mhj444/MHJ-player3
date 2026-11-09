package com.mhj.player.core.ui.components

import com.mhj.player.core.model.L

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import com.mhj.player.core.model.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhj.player.core.model.AppSettings
import com.mhj.player.core.model.Eq
import com.mhj.player.core.ui.theme.MhjColors
import com.mhj.player.core.ui.theme.MhjTheme
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class AudioPage { ROOT, SPEED, TIMER, EQUALIZER, BOOKMARKS }

/** Everything the sound panel can do; the app fills it in. */
class AudioActions(
    val onDialogue: (Boolean) -> Unit,
    val onNightMode: (Boolean) -> Unit,
    val onSpeed: (Float) -> Unit,
    val onKeepPitch: (Boolean) -> Unit,
    val onAbClick: () -> Unit,
    val onEqEnabled: (Boolean) -> Unit,
    val onEqGain: (Int, Float) -> Unit,
    val onEqPreamp: (Float) -> Unit,
    val onEqPreset: (String) -> Unit,
    val onTimerMinutes: (Int) -> Unit,
    val onTimerAtEnd: () -> Unit,
    val onTimerOff: () -> Unit,
    val onOpenBookmark: (Bookmark) -> Unit = {},
    val onDeleteBookmark: (Bookmark) -> Unit = {},
)

@Composable
fun BottomAudioDrawer(
    visible: Boolean,
    settings: AppSettings,
    abActive: Boolean,
    abHalf: Boolean,
    bookmarks: List<Bookmark> = emptyList(),
    actions: AudioActions,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    backEnabled: Boolean = true,
) {
    var page by remember { mutableStateOf(AudioPage.ROOT) }
    LaunchedEffect(visible) { if (!visible) page = AudioPage.ROOT }
    BackHandler(enabled = visible && backEnabled) { if (page != AudioPage.ROOT) page = AudioPage.ROOT else onDismiss() }

    // A clock for the timer's countdown.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(visible, settings.sleepAtMs) {
        while (visible && settings.sleepAtMs > 0) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val leftMs = if (settings.sleepAtMs > 0) (settings.sleepAtMs - now).coerceAtLeast(0) else 0L

    MhjSheet(
        visible = visible,
        fromTop = false,
        heightFraction = when (page) {
            AudioPage.ROOT -> 0.66f
            AudioPage.EQUALIZER, AudioPage.BOOKMARKS -> 0.90f
            else -> 0.84f
        },
        onDismiss = { page = AudioPage.ROOT; onDismiss() },
        modifier = modifier,
        hazeState = hazeState,
    ) {
        val back = { page = AudioPage.ROOT }
        when (page) {
            AudioPage.ROOT -> RootPage(settings, abActive, abHalf, leftMs, bookmarks.size, actions) { page = it }
            AudioPage.SPEED -> SpeedPage(settings, actions, back)
            AudioPage.TIMER -> TimerPage(settings, leftMs, actions, back)
            AudioPage.EQUALIZER -> EqPage(settings, actions, back)
            AudioPage.BOOKMARKS -> BookmarksPage(bookmarks, actions, back)
        }
    }
}

private fun timerValue(s: AppSettings, leftMs: Long): String = when {
    s.sleepAtEnd -> L.t("پایان آهنگ")
    s.sleepAtMs > 0 -> Fa.time(leftMs)
    else -> L.t("خاموش")
}

@Composable
private fun ColumnScope.RootPage(
    s: AppSettings,
    abActive: Boolean,
    abHalf: Boolean,
    leftMs: Long,
    bookmarkCount: Int,
    a: AudioActions,
    go: (AudioPage) -> Unit,
) {
    SheetTitle(L.t("پردازش صدا"), L.t("تنظیم‌ها روی همهٔ آهنگ‌ها و ویدیوها می‌مانند"))
    Column(
        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        FeatureRow(Icons.Outlined.RecordVoiceOver, L.t("وضوح گفتگو"), L.t("صدای حرف‌زدن واضح‌تر، غرش بم کمتر")) {
            GoldSwitch(s.dialogueClarity, a.onDialogue)
        }
        FeatureRow(Icons.Outlined.Bedtime, L.t("حالت شب"), L.t("صداهای بلند آرام‌تر، صداهای آرام شنیدنی‌تر")) {
            GoldSwitch(s.nightModeAudio, a.onNightMode)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            FeatureTile(Icons.Outlined.Repeat, L.t("تکرار A-B"),
                when { abActive -> L.t("روشن — بزنید تا خاموش شود"); abHalf -> L.t("نقطهٔ B را بزنید"); else -> L.t("نقطهٔ A را بزنید") },
                Modifier.weight(1f).height(108.dp), active = abActive || abHalf, onClick = a.onAbClick)
            FeatureTile(Icons.Outlined.Speed, L.t("سرعت"), Fa.speed(s.playbackSpeed),
                Modifier.weight(1f).height(108.dp), active = kotlin.math.abs(s.playbackSpeed - 1f) > 0.01f) { go(AudioPage.SPEED) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            FeatureTile(Icons.Outlined.Equalizer, L.t("اکولایزر"),
                if (!s.eqEnabled) L.t("خاموش") else L.t(Eq.presetName(s.eqPreset)),
                Modifier.weight(1f).height(108.dp), active = s.eqEnabled && s.eqGains.any { it != 0f }) { go(AudioPage.EQUALIZER) }
            FeatureTile(Icons.Outlined.Timer, L.t("تایمر خواب"), timerValue(s, leftMs),
                Modifier.weight(1f).height(108.dp), active = s.sleepAtEnd || s.sleepAtMs > 0) { go(AudioPage.TIMER) }
        }
        FeatureRow(
            Icons.Outlined.BookmarkBorder, L.t("نشانک‌ها"),
            if (bookmarkCount == 0) L.t("برای ساختن، روی عکس آهنگ انگشت نگه دارید")
            else L.pick("${Fa.num(bookmarkCount)} نشانک", "$bookmarkCount bookmarks"),
            onClick = { go(AudioPage.BOOKMARKS) },
        ) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MhjColors.Gold, modifier = Modifier.width(24.dp))
        }
        Spacer(Modifier.height(4.dp))
    }
}

// ---------------------------------------------------------------- speed

private const val SPEED_MIN = 0.25f
private const val SPEED_MAX = 3f

@Composable
private fun ColumnScope.SpeedPage(s: AppSettings, a: AudioActions, onBack: () -> Unit) {
    val c = MhjTheme.colors
    var drag by remember { mutableStateOf<Float?>(null) }
    val v = drag ?: s.playbackSpeed
    fun snap(x: Float) = (Math.round(x.coerceIn(SPEED_MIN, SPEED_MAX) * 20f) / 20f)
    SheetTitle(L.t("سرعت پخش"), L.t("عقربه را بچرخانید"), onBack)
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        val labels = listOf(0.25f, 0.5f, 1f, 1.5f, 2f, 2.5f, 3f).map { (it - SPEED_MIN) / (SPEED_MAX - SPEED_MIN) to Fa.digits(if (it == 0.25f) ".25" else (if (it % 1f == 0f) it.toInt().toString() else it.toString())) }
        ArcDial(
            fraction = (v - SPEED_MIN) / (SPEED_MAX - SPEED_MIN),
            onChange = { f -> drag = snap(SPEED_MIN + f * (SPEED_MAX - SPEED_MIN)) },
            onRelease = { drag?.let(a.onSpeed); drag = null },
            startDeg = 150f, sweepDeg = 240f, wrap = false,
            labels = labels, minorTicks = 22,
            modifier = Modifier.widthIn(max = 320.dp).padding(horizontal = 8.dp),
        ) {
            Text(Fa.speed(v), color = if (c.isDark) MhjColors.GoldHi else MhjColors.GoldLo, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text(L.t("سرعت"), color = c.muted, fontSize = 13.sp)
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                RaisedIconButton(Icons.Outlined.Remove, L.t("کمتر"), { a.onSpeed(snap(s.playbackSpeed - 0.05f)) }, size = 48.dp)
                RaisedPill(L.t("۱×"), selected = kotlin.math.abs(s.playbackSpeed - 1f) < 0.01f) { a.onSpeed(1f) }
                RaisedIconButton(Icons.Outlined.Add, L.t("بیشتر"), { a.onSpeed(snap(s.playbackSpeed + 0.05f)) }, size = 48.dp)
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                listOf(0.5f, 0.75f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f).forEach { x ->
                    RaisedPill(Fa.speed(x), selected = kotlin.math.abs(s.playbackSpeed - x) < 0.01f) { a.onSpeed(x) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        FeatureRow(Icons.Outlined.GraphicEq, L.t("حفظ صدای طبیعی"), L.t("با تند یا کند شدن، صدا زیر یا بم نمی‌شود"), Modifier.padding(horizontal = 4.dp)) {
            GoldSwitch(s.keepNaturalPitch, a.onKeepPitch)
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ---------------------------------------------------------------- sleep timer

private const val TIMER_MAX = 120

@Composable
private fun ColumnScope.TimerPage(s: AppSettings, leftMs: Long, a: AudioActions, onBack: () -> Unit) {
    val c = MhjTheme.colors
    var drag by remember { mutableStateOf<Int?>(null) }
    val running = s.sleepAtMs > 0
    val fraction = when {
        drag != null -> drag!! / TIMER_MAX.toFloat()
        running -> (leftMs / 60_000f / TIMER_MAX).coerceIn(0f, 1f)
        else -> 0f
    }
    val stopAt = remember(s.sleepAtMs) {
        if (s.sleepAtMs > 0) Fa.digits(SimpleDateFormat("HH:mm", Locale.US).format(Date(s.sleepAtMs))) else ""
    }
    SheetTitle(
        L.t("تایمر خواب"),
        when {
            s.sleepAtEnd -> L.t("بعد از تمام شدن همین آهنگ، پخش می‌ایستد")
            running -> L.pick("ساعت $stopAt صدا آرام کم می‌شود و پخش می‌ایستد", "At $stopAt the sound fades out and playback stops")
            else -> L.t("عقربه را بچرخانید یا یکی از گزینه‌ها را بزنید")
        },
        onBack,
    )
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        val labels = (0 until TIMER_MAX step 15).map { it / TIMER_MAX.toFloat() to Fa.num(it) }
        ArcDial(
            fraction = fraction,
            onChange = { f -> drag = (Math.round(f * TIMER_MAX)).coerceIn(0, TIMER_MAX) },
            onRelease = {
                val m = drag
                drag = null
                if (m != null) { if (m > 0) a.onTimerMinutes(m) else a.onTimerOff() }
            },
            startDeg = -90f, sweepDeg = 360f, wrap = true,
            labels = labels, minorTicks = 24,
            modifier = Modifier.widthIn(max = 320.dp).padding(horizontal = 8.dp),
        ) {
            val big = when {
                drag != null -> if (drag == 0) L.t("خاموش") else Fa.num(drag!!)
                s.sleepAtEnd -> L.t("پایان")
                running -> Fa.time(leftMs)
                else -> L.t("خاموش")
            }
            val small = when {
                drag != null -> if (drag == 0) "" else L.t("دقیقه")
                s.sleepAtEnd -> L.t("همین آهنگ")
                running -> L.t("مانده")
                else -> ""
            }
            Text(big, color = if (c.isDark) MhjColors.GoldHi else MhjColors.GoldLo, fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (small.isNotEmpty()) Text(small, color = c.muted, fontSize = 13.sp)
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RaisedPill(L.t("خاموش"), selected = !running && !s.sleepAtEnd, onClick = a.onTimerOff)
            RaisedPill(L.t("پایان همین آهنگ"), selected = s.sleepAtEnd, onClick = a.onTimerAtEnd)
            listOf(15, 30, 45, 60, 90).forEach { m ->
                RaisedPill(Fa.minutes(m), selected = running && s.sleepTimerMinutes == m) { a.onTimerMinutes(m) }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ---------------------------------------------------------------- equalizer

@Composable
private fun ColumnScope.EqPage(s: AppSettings, a: AudioActions, onBack: () -> Unit) {
    val c = MhjTheme.colors
    SheetTitle(L.t("اکولایزر"), if (!s.eqEnabled) L.t("خاموش") else L.pick("حالت: ", "Preset: ") + L.t(Eq.presetName(s.eqPreset)), onBack) {
        GoldSwitch(s.eqEnabled, a.onEqEnabled)
    }
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Eq.PRESETS.forEach { p ->
                RaisedPill(L.t(p.nameFa), selected = s.eqEnabled && s.eqPreset == p.id) { a.onEqPreset(p.id) }
            }
        }
        Spacer(Modifier.height(8.dp))
        RaisedCard(Modifier.fillMaxWidth().padding(horizontal = 4.dp), radius = 22.dp, padding = 12.dp) {
            EqBoard(s.eqGains, s.eqEnabled, a.onEqGain)
        }
        Spacer(Modifier.height(16.dp))
        RaisedCard(Modifier.fillMaxWidth().padding(horizontal = 4.dp), radius = 20.dp, padding = 12.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(L.t("بلندی کلی"), color = c.onBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(Fa.db(s.eqPreamp) + " dB", color = MhjColors.Gold, fontSize = 13.sp)
            }
            GoldSlider(s.eqPreamp, -12f..6f, a.onEqPreamp)
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Text(L.t("برگرداندن به صاف"), color = c.muted, fontSize = 13.sp)
            Spacer(Modifier.width(10.dp))
            RaisedIconButton(Icons.Outlined.RestartAlt, L.t("صاف"), { a.onEqPreset("flat"); a.onEqPreamp(0f) }, size = 44.dp, iconSize = 22.dp)
        }
        Spacer(Modifier.height(10.dp))
    }
}



// ---------------------------------------------------------------- bookmarks

@Composable
private fun ColumnScope.BookmarksPage(list: List<Bookmark>, a: AudioActions, onBack: () -> Unit) {
    val c = MhjTheme.colors
    SheetTitle(L.t("نشانک‌ها"), L.t("روی عکس آهنگ انگشت نگه دارید تا همان لحظه نشانک شود"), onBack)
    if (list.isEmpty()) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(L.t("هنوز نشانکی ندارید"), color = c.muted, fontSize = 14.sp)
        }
        return
    }
    val fmt = remember { SimpleDateFormat("yyyy/MM/dd", Locale.US) }
    androidx.compose.foundation.lazy.LazyColumn(
        Modifier.weight(1f).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp, 8.dp),
    ) {
        items(list.size, key = { list[it].id }) { i ->
            val b = list[i]
            RaisedCard(Modifier.fillMaxWidth(), radius = 18.dp, depth = 5.dp, padding = 12.dp, onClick = { a.onOpenBookmark(b) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(64.dp), contentAlignment = Alignment.Center) {
                        Text(Fa.time(b.positionMs), color = if (c.isDark) MhjColors.GoldHi else MhjColors.GoldLo, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(b.title, color = c.onBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(Fa.digits(fmt.format(Date(b.createdAt))), color = c.muted, fontSize = 11.sp)
                    }
                    RaisedIconButton(Icons.Outlined.DeleteOutline, L.t("حذف"), { a.onDeleteBookmark(b) }, size = 40.dp, iconSize = 19.dp)
                }
            }
        }
    }
}
