package com.fabringo.mobile

import android.animation.ValueAnimator
import android.os.Build
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

private val Panel = RoundedCornerShape(20.dp)
private const val MixDurationMillis = 820L
internal val LetterColors = listOf(
    Color(0xFFAD2648), Color(0xFF9D4D12), Color(0xFF1C56AE), Color(0xFF327139), Color(0xFF7231A5),
)

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun HostSetup(
    onBack: () -> Unit,
    announcing: Boolean,
    onVoiceSample: (Voice) -> Unit,
    onStart: (Pattern, Boolean, Int?, Voice) -> Unit,
) {
    var pattern by rememberSaveable { mutableStateOf(Pattern.Full) }
    var filtered by rememberSaveable { mutableStateOf(false) }
    var interval by rememberSaveable { mutableStateOf<Int?>(null) }
    var voice by rememberSaveable { mutableStateOf(Voice.Feminine) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TextButton(onClick = onBack) { Text("Volver") }
        Text("Anfitrión", style = MaterialTheme.typography.headlineLarge, color = Gold, fontWeight = FontWeight.Black)
        Text("Patrón ganador", fontWeight = FontWeight.Bold)
        Pattern.entries.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { option ->
                    Column(
                        Modifier.weight(1f).clip(Panel)
                            .background(if (option == pattern) Color(0xFF2F4B91) else Navy)
                            .border(if (option == pattern) 2.dp else 1.dp, if (option == pattern) Gold else BlueEdge, Panel)
                            .clickable(role = Role.RadioButton) { pattern = option }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        PatternGrid(option, Modifier.size(38.dp))
                        Text(option.label, style = MaterialTheme.typography.labelMedium)
                    }
                }
                repeat(3 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
        Text("Bolas del sorteo", fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!filtered, { filtered = false }, { Text("Todas (75)") })
            FilterChip(filtered, { filtered = true }, { Text("Solo letras del patrón") })
        }
        Text("${pattern.ballPool(filtered).size} bolas disponibles", color = Ice)
        Text("Ritmo inicial", fontWeight = FontWeight.Bold)
        DrawOptions(interval) { interval = it }
        Text("Voz del sorteo", fontWeight = FontWeight.Bold)
        VoiceOptions(voice) { voice = it }
        TextButton(onClick = { onVoiceSample(voice) }, enabled = !announcing) { Text("Escuchar muestra") }
        Button(onClick = { onStart(pattern, filtered, interval, voice) }, modifier = Modifier.fillMaxWidth()) {
            Text("Iniciar partida", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun VoiceOptions(selected: Voice, onSelect: (Voice) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Voice.entries.forEach { voice ->
            FilterChip(selected == voice, { onSelect(voice) }, { Text(voice.label) })
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun DrawOptions(selected: Int?, onSelect: (Int?) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        (listOf(null) + DrawIntervals).forEach { interval ->
            FilterChip(
                selected == interval,
                { onSelect(interval) },
                { Text(interval?.let { "$it s" } ?: "Manual") },
            )
        }
    }
}

@Composable
private fun PatternGrid(pattern: Pattern, modifier: Modifier = Modifier) {
    Canvas(modifier.semantics { contentDescription = "Patrón ${pattern.label}" }) {
        val margin = size.width / 65f
        val step = size.width / 5
        for (position in 0..24) {
            drawRoundRect(
                if (position in pattern.cells) Gold else Color(0xFF405583),
                Offset(position % 5 * step, position / 5 * step),
                Size(step - margin, step - margin),
                CornerRadius(margin),
            )
        }
    }
}

@Composable
internal fun HostGame(
    session: Session.Host,
    foreground: Boolean,
    announcing: Boolean,
    onEnd: () -> Unit,
    onSession: (Session.Host) -> Unit,
    onVoiceSample: (Voice) -> Unit,
) {
    val pool = remember(session.pattern, session.filtered) { session.pool.toSet() }
    val called = remember(session.called) { session.called.toSet() }
    val finished = called.size == pool.size
    val interval = session.interval
    var paused by rememberSaveable { mutableStateOf(session.called.isNotEmpty() && interval != null) }
    var mixing by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    var showRhythm by remember { mutableStateOf(false) }
    val reducedMotion = Build.VERSION.SDK_INT >= 26 && !ValueAnimator.areAnimatorsEnabled()
    val mixDuration = if (reducedMotion) 0L else MixDurationMillis
    val audioInProgress = rememberUpdatedState(announcing)

    LaunchedEffect(foreground) {
        if (!foreground) {
            mixing = false
            paused = interval != null
        }
    }
    LaunchedEffect(interval, paused, foreground, mixing, called.size, showRhythm, mixDuration) {
        countdown = 0
        if (interval != null && !paused && foreground && !mixing && !finished && !showRhythm) {
            val deadline = SystemClock.elapsedRealtime() + interval * 1000L
            var remaining = deadline - SystemClock.elapsedRealtime()
            while (remaining > mixDuration) {
                countdown = ((remaining + 999) / 1000).toInt()
                delay(minOf(remaining - mixDuration, ((remaining - 1) % 1000) + 1))
                remaining = deadline - SystemClock.elapsedRealtime()
            }
            countdown = 0
            mixing = true
        }
    }
    LaunchedEffect(mixing, foreground, mixDuration) {
        if (mixing && foreground) {
            delay(mixDuration)
            snapshotFlow { audioInProgress.value }.first { !it }
            val next = (pool - called).randomOrNull()
            if (next != null) onSession(session.copy(called = session.called + next))
            mixing = false
        }
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("FABRINGO", color = Gold, fontWeight = FontWeight.Black, fontSize = 24.sp)
            Text("Anfitrión", color = Ice, fontWeight = FontWeight.Bold)
        }
        Row(
            Modifier.fillMaxWidth().clip(Panel).background(Color(0xFF162861)).padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PatternGrid(session.pattern, Modifier.size(30.dp))
            Column(Modifier.weight(1f)) {
                Text(session.pattern.label, fontWeight = FontWeight.Bold)
                Text(if (session.filtered) "Solo letras del patrón" else "Todas las bolas", color = Ice, fontSize = 12.sp)
            }
            Text("${called.size} de ${pool.size}", color = Gold, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth >= 600.dp) {
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        HostStage(session.called.lastOrNull(), mixing, Modifier.weight(.9f))
                        Board(pool, called, session.called.lastOrNull(), Modifier.weight(1.1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HostStage(session.called.lastOrNull(), mixing)
                        Board(pool, called, session.called.lastOrNull())
                    }
                }
            }
        }
        if (finished) Text("Salieron todas las bolas de este sorteo.", color = Gold)
        if (!finished) {
            if (interval == null) {
                Button(
                    onClick = { mixing = true },
                    enabled = !mixing && !announcing && foreground,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        when {
                            mixing -> "Mezclando…"
                            announcing -> "Cantando…"
                            else -> "Bola siguiente"
                        },
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when {
                            paused -> "Sorteo en pausa"
                            mixing -> "Mezclando…"
                            else -> "Siguiente en $countdown s"
                        },
                        Modifier.weight(1f),
                        color = Gold,
                        fontWeight = FontWeight.Bold,
                    )
                    Button(onClick = {
                        paused = !paused
                        if (paused) mixing = false
                    }) { Text(if (paused) "Reanudar" else "Pausar") }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = { showRhythm = true }, enabled = !mixing && !finished) {
                Text("Ritmo y voz")
            }
            TextButton(onClick = onEnd) { Text("Terminar") }
        }
    }

    if (showRhythm) {
        AlertDialog(
            onDismissRequest = { showRhythm = false },
            title = { Text("Ritmo y voz") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ritmo del sorteo")
                    DrawOptions(interval) { choice ->
                        if (choice != interval) {
                            paused = false
                            onSession(session.copy(interval = choice))
                        }
                    }
                    Text("Voz del sorteo")
                    VoiceOptions(session.voice) { onSession(session.copy(voice = it)) }
                    TextButton(onClick = { onVoiceSample(session.voice) }, enabled = !announcing) { Text("Escuchar muestra") }
                }
            },
            confirmButton = { TextButton(onClick = { showRhythm = false }) { Text("Cerrar") } },
        )
    }
}

@Composable
private fun HostStage(number: Int?, mixing: Boolean, modifier: Modifier = Modifier) {
    val reveal = remember { Animatable(1f) }
    LaunchedEffect(number) {
        if (number != null) {
            reveal.snapTo(.87f)
            reveal.animateTo(1f, tween(240))
        }
    }
    Row(
        modifier.fillMaxWidth().clip(Panel)
            .background(Brush.radialGradient(listOf(Color(0xFF365DC5), Color(0xFF202866), Color(0xFF141D53))))
            .border(1.dp, BlueEdge, Panel).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Urn(mixing, Modifier.weight(1.2f))
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(
                Modifier.fillMaxWidth(.82f).aspectRatio(1f)
                    .graphicsLayer { scaleX = reveal.value; scaleY = reveal.value }
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFFFFF6D9), Gold, Color(0xFFC9731F))))
                    .border(2.dp, Color(0xFFFFE6A5), CircleShape)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (number != null) Text(ballLetter(number).toString(), color = BallInk, fontSize = 23.sp, fontWeight = FontWeight.Black, lineHeight = 24.sp)
                    Text(number?.toString() ?: "—", color = BallInk, fontSize = 39.sp, fontWeight = FontWeight.Black, lineHeight = 41.sp)
                }
            }
            Text(if (mixing) "Mezclando…" else if (number == null) "Por salir" else "Última bola", color = Ice, fontSize = 12.sp)
        }
    }
}

@Composable
private fun Board(pool: Set<Int>, called: Set<Int>, latest: Int?, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(Panel).background(Color(0xFF0B1843)).border(1.dp, Color(0xFF456DBD), Panel).padding(7.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        BingoLetters.forEachIndexed { column, letter ->
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 30.dp).clip(RoundedCornerShape(6.dp)).background(LetterColors[column]),
                    contentAlignment = Alignment.Center,
                ) { Text(letter.toString(), fontWeight = FontWeight.Black, fontSize = 17.sp) }
                for (number in (column * 15 + 1)..(column * 15 + 15)) {
                    val isOutside = number !in pool
                    val isCalled = number in called
                    val isLatest = number == latest
                    val shape = RoundedCornerShape(5.dp)
                    val tile = Modifier.fillMaxWidth().heightIn(min = 20.dp).clip(shape).background(
                        if (isOutside || isCalled) Color(0xFF131F48) else Color(0xFF203873),
                    )
                    Box(
                        if (isLatest) tile.border(1.dp, Gold, shape) else tile,
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            number.toString(),
                            color = when {
                                isOutside -> Color(0xFF8C9BB8)
                                isCalled -> Color(0xFFB1C2DD)
                                else -> Color(0xFFF4FAFF)
                            },
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            fontWeight = if (isCalled || isOutside) FontWeight.Normal else FontWeight.SemiBold,
                            modifier = Modifier.semantics {
                                contentDescription = when {
                                    isOutside -> "$letter $number, fuera del sorteo"
                                    isCalled -> "$letter $number, cantada"
                                    else -> "$letter $number, pendiente"
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
