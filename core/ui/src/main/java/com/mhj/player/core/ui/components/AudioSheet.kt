package com.mhj.player.core.ui.components

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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.History
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

/** One saved transcript in the list. [id] is whatever the app needs to find it again. */
data class SavedTranscript(val id: String, val title: String, val updatedAt: Long)

private enum class AudioPage { ROOT, SPEED, TIMER, EQUALIZER, HISTORY }

/** Everything the sound panel can do; the app fills it in. */
class AudioActions(
    val onToggleTranscript: () -> Unit,
    val onOpenSaved: (SavedTranscript) -> Unit,
    val onShareSaved: (SavedTranscript) -> Unit,
    val onDeleteSaved: (SavedTranscript) -> Unit,
    val onRefreshSaved: () -> Unit,
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
)

data class TranscriptUi(
    val running: Boolean = false,
    val downloading: Boolean = false,
    val progress: Float = 0f,
    val status: String = "",
    val error: String? = null,
)

@Composable
fun BottomAudioDrawer(
    visible: Boolean,
    settings: AppSettings,
    abActive: Boolean,
    abHalf: Boolean,
    transcript: TranscriptUi,
    saved: List<SavedTranscript>,
    actions: AudioActions,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
) {
    var page by remember { mutableStateOf(AudioPage.ROOT) }
    LaunchedEffect(visible) { if (!visible) page = AudioPage.ROOT else actions.onRefreshSaved() }
    BackHandler(enabled = visible) { if (page != AudioPage.ROOT) page = AudioPage.ROOT else onDismiss() }

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
            AudioPage.ROOT -> 0.80f
            AudioPage.EQUALIZER, AudioPage.HISTORY -> 0.90f
            else -> 0.84f
        },
        onDismiss = { page = AudioPage.ROOT; onDismiss() },
        modifier = modifier,
        hazeState = hazeState,
    ) {
        val back = { page = AudioPage.ROOT }
        when (page) {
            AudioPage.ROOT -> RootPage(settings, abActive, abHalf, transcript, leftMs, actions) { page = it }
            AudioPage.SPEED -> SpeedPage(settings, actions, back)
            AudioPage.TIMER -> TimerPage(settings, leftMs, actions, back)
            AudioPage.EQUALIZER -> EqPage(settings, actions, back)
            AudioPage.HISTORY -> HistoryPage(saved, actions, back)
        }
    }
}

private fun timerValue(s: AppSettings, leftMs: Long): String = when {
    s.sleepAtEnd -> "پایان آهنگ"
    s.sleepAtMs > 0 -> Fa.time(leftMs)
    else -> "خاموش"
}

@Composable
private fun ColumnScope.RootPage(
    s: AppSettings,
    abActive: Boolean,
    abHalf: Boolean,
    t: TranscriptUi,
    leftMs: Long,
    a: AudioActions,
    go: (AudioPage) -> Unit,
) {
    SheetTitle("پردازش صدا", "تنظیم‌ها روی همهٔ آهنگ‌ها و ویدیوها می‌مانند")
    Column(
        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TranscriptCard(t, a.onToggleTranscript) { go(AudioPage.HISTORY) }
        FeatureRow(Icons.Outlined.RecordVoiceOver, "وضوح گفتگو", "صدای حرف‌زدن واضح‌تر، غرش بم کمتر") {
            GoldSwitch(s.dialogueClarity, a.onDialogue)
        }
        FeatureRow(Icons.Outlined.Bedtime, "حالت شب", "صداهای بلند آرام‌تر، صداهای آرام شنیدنی‌تر") {
            GoldSwitch(s.nightModeAudio, a.onNightMode)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            FeatureTile(Icons.Outlined.Repeat, "تکرار A-B",
                when { abActive -> "روشن — بزنید تا خاموش شود"; abHalf -> "نقطهٔ B را بزنید"; else -> "نقطهٔ A را بزنید" },
                Modifier.weight(1f).height(108.dp), active = abActive || abHalf, onClick = a.onAbClick)
            FeatureTile(Icons.Outlined.Speed, "سرعت", Fa.speed(s.playbackSpeed),
                Modifier.weight(1f).height(108.dp), active = kotlin.math.abs(s.playbackSpeed - 1f) > 0.01f) { go(AudioPage.SPEED) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            FeatureTile(Icons.Outlined.Equalizer, "اکولایزر",
                if (!s.eqEnabled) "خاموش" else Eq.presetName(s.eqPreset),
                Modifier.weight(1f).height(108.dp), active = s.eqEnabled && s.eqGains.any { it != 0f }) { go(AudioPage.EQUALIZER) }
            FeatureTile(Icons.Outlined.Timer, "تایمر خواب", timerValue(s, leftMs),
                Modifier.weight(1f).height(108.dp), active = s.sleepAtEnd || s.sleepAtMs > 0) { go(AudioPage.TIMER) }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun TranscriptCard(t: TranscriptUi, onToggle: () -> Unit, onHistory: () -> Unit) {
    val c = MhjTheme.colors
    RaisedCard(Modifier.fillMaxWidth(), radius = 20.dp, depth = 6.dp, padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(40.dp).height(40.dp).clip(RoundedCornerShape(20.dp)).background(MhjColors.Gold.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(Icons.Outlined.Subtitles, null, tint = if (c.isDark) MhjColors.GoldHi else MhjColors.GoldLo)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("تبدیل گفتار به متن", color = c.onBackground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    t.error ?: t.status.ifBlank { "حرف‌های آهنگ یا ویدیوی در حال پخش را می‌نویسد" },
                    color = if (t.error != null) Color(0xFFE57373) else c.muted,
                    fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            RaisedIconButton(Icons.Outlined.History, "متن‌های قبلی", onHistory, size = 42.dp, iconSize = 20.dp)
            Spacer(Modifier.width(10.dp))
            RaisedIconButton(if (t.running) Icons.Outlined.Stop else Icons.Outlined.Mic, if (t.running) "توقف" else "شروع",
                onToggle, size = 42.dp, iconSize = 20.dp, active = t.running)
        }
        if (t.running || t.downloading) {
            Spacer(Modifier.height(10.dp))
            ProgressBar(t.progress)
        }
    }
}

@Composable
private fun ProgressBar(p: Float) {
    val c = MhjTheme.colors
    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(c.recess)) {
        Box(Modifier.fillMaxWidth(p.coerceIn(0.02f, 1f)).height(6.dp).clip(RoundedCornerShape(3.dp))
            .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(MhjColors.GoldLo, MhjColors.GoldHi))))
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
    SheetTitle("سرعت پخش", "عقربه را بچرخانید", onBack)
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
            Text("سرعت", color = c.muted, fontSize = 13.sp)
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                RaisedIconButton(Icons.Outlined.Remove, "کمتر", { a.onSpeed(snap(s.playbackSpeed - 0.05f)) }, size = 48.dp)
                RaisedPill("۱×", selected = kotlin.math.abs(s.playbackSpeed - 1f) < 0.01f) { a.onSpeed(1f) }
                RaisedIconButton(Icons.Outlined.Add, "بیشتر", { a.onSpeed(snap(s.playbackSpeed + 0.05f)) }, size = 48.dp)
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
        FeatureRow(Icons.Outlined.GraphicEq, "حفظ صدای طبیعی", "با تند یا کند شدن، صدا زیر یا بم نمی‌شود", Modifier.padding(horizontal = 4.dp)) {
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
        "تایمر خواب",
        when {
            s.sleepAtEnd -> "بعد از تمام شدن همین آهنگ، پخش می‌ایستد"
            running -> "ساعت $stopAt صدا آرام کم می‌شود و پخش می‌ایستد"
            else -> "عقربه را بچرخانید یا یکی از گزینه‌ها را بزنید"
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
                drag != null -> if (drag == 0) "خاموش" else Fa.num(drag!!)
                s.sleepAtEnd -> "پایان"
                running -> Fa.time(leftMs)
                else -> "خاموش"
            }
            val small = when {
                drag != null -> if (drag == 0) "" else "دقیقه"
                s.sleepAtEnd -> "همین آهنگ"
                running -> "مانده"
                else -> ""
            }
            Text(big, color = if (c.isDark) MhjColors.GoldHi else MhjColors.GoldLo, fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (small.isNotEmpty()) Text(small, color = c.muted, fontSize = 13.sp)
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RaisedPill("خاموش", selected = !running && !s.sleepAtEnd, onClick = a.onTimerOff)
            RaisedPill("پایان همین آهنگ", selected = s.sleepAtEnd, onClick = a.onTimerAtEnd)
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
    SheetTitle("اکولایزر", if (!s.eqEnabled) "خاموش" else "حالت: " + Eq.presetName(s.eqPreset), onBack) {
        GoldSwitch(s.eqEnabled, a.onEqEnabled)
    }
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Eq.PRESETS.forEach { p ->
                RaisedPill(p.nameFa, selected = s.eqEnabled && s.eqPreset == p.id) { a.onEqPreset(p.id) }
            }
        }
        Spacer(Modifier.height(8.dp))
        RaisedCard(Modifier.fillMaxWidth().padding(horizontal = 4.dp), radius = 22.dp, padding = 12.dp) {
            EqBoard(s.eqGains, s.eqEnabled, a.onEqGain)
        }
        Spacer(Modifier.height(16.dp))
        RaisedCard(Modifier.fillMaxWidth().padding(horizontal = 4.dp), radius = 20.dp, padding = 12.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("بلندی کلی", color = c.onBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(Fa.db(s.eqPreamp) + " dB", color = MhjColors.Gold, fontSize = 13.sp)
            }
            GoldSlider(s.eqPreamp, -12f..6f, a.onEqPreamp)
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Text("برگرداندن به صاف", color = c.muted, fontSize = 13.sp)
            Spacer(Modifier.width(10.dp))
            RaisedIconButton(Icons.Outlined.RestartAlt, "صاف", { a.onEqPreset("flat"); a.onEqPreamp(0f) }, size = 44.dp, iconSize = 22.dp)
        }
        Spacer(Modifier.height(10.dp))
    }
}

// ---------------------------------------------------------------- saved transcripts

@Composable
private fun ColumnScope.HistoryPage(saved: List<SavedTranscript>, a: AudioActions, onBack: () -> Unit) {
    val c = MhjTheme.colors
    var ask by remember { mutableStateOf<SavedTranscript?>(null) }
    val fmt = remember { SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.US) }
    SheetTitle("متن‌های قبلی", if (saved.isEmpty()) "هنوز متنی ساخته نشده" else "${Fa.num(saved.size)} متن ذخیره‌شده", onBack)
    if (saved.isEmpty()) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("روی دکمهٔ میکروفون بزنید تا حرف‌های فایل در حال پخش نوشته شود.", color = c.muted, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
        return
    }
    LazyColumn(
        Modifier.weight(1f).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
    ) {
        items(saved, key = { it.id }) { item ->
            RaisedCard(Modifier.fillMaxWidth(), radius = 18.dp, depth = 5.dp, padding = 12.dp, onClick = { a.onOpenSaved(item) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.title, color = c.onBackground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(Fa.digits(fmt.format(Date(item.updatedAt))), color = c.muted, fontSize = 12.sp)
                    }
                    RaisedIconButton(Icons.Outlined.Share, "اشتراک", { a.onShareSaved(item) }, size = 40.dp, iconSize = 19.dp)
                    Spacer(Modifier.width(10.dp))
                    RaisedIconButton(Icons.Outlined.DeleteOutline, "حذف", { ask = item }, size = 40.dp, iconSize = 19.dp)
                }
            }
        }
    }
    ask?.let { item ->
        AlertDialog(
            onDismissRequest = { ask = null },
            title = { Text("حذف متن") },
            text = { Text("متن «${item.title}» پاک شود؟") },
            dismissButton = { TextButton(onClick = { ask = null }) { Text("انصراف") } },
            confirmButton = { TextButton(onClick = { a.onDeleteSaved(item); ask = null }) { Text("حذف", color = Color(0xFFE57373)) } },
        )
    }
}
