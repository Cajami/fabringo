package com.fabringo.mobile

import android.media.AudioManager
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private lateinit var storage: SessionStore
    private lateinit var announcer: BallAnnouncer
    private var active by mutableStateOf<Session?>(null)
    private var foreground by mutableStateOf(false)
    private var announcing by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storage = SessionStore(this)
        announcer = BallAnnouncer(this) { announcing = it }
        volumeControlStream = AudioManager.STREAM_MUSIC
        active = storage.load()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            MaterialTheme(colorScheme = GameColors) {
                Box(
                    Modifier.fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color(0xFF172D75), Color(0xFF0A143D), Color(0xFF241854))))
                        .safeDrawingPadding(),
                ) {
                    Fabringo(active, foreground, announcing, ::updateSession, ::markCard) { voice ->
                        announcer.announce(60, voice)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        foreground = true
    }

    override fun onStop() {
        foreground = false
        announcer.stop()
        super.onStop()
    }

    private fun updateSession(session: Session?) {
        val previous = active as? Session.Host
        storage.save(session)
        active = session
        if (session is Session.Host && previous != null && session.called.size > previous.called.size) {
            announcer.announce(session.called.last(), session.voice)
        } else if (session == null) {
            announcer.stop()
        }
    }

    private fun markCard(index: Int, position: Int) {
        val current = active as? Session.Player ?: return
        val cards = current.cards.toMutableList()
        cards[index] = cards[index].toggle(position)
        updateSession(current.copy(cards = cards))
    }
}

internal val Gold = Color(0xFFFFD16A)
internal val Ice = Color(0xFFD9E6FF)
internal val Navy = Color(0xFF101E50)
internal val BlueEdge = Color(0xFF5B93FF)
internal val BallInk = Color(0xFF251743)
private val GameColors = darkColorScheme(
    primary = Gold,
    onPrimary = BallInk,
    secondary = Color(0xFF90C3FF),
    onSecondary = Navy,
    surface = Color(0xFF162756),
    onSurface = Color(0xFFF6F8FF),
    background = Navy,
    onBackground = Ice,
)

private enum class Setup { Home, Host, Player }

@Composable
private fun Fabringo(
    session: Session?,
    foreground: Boolean,
    announcing: Boolean,
    onSession: (Session?) -> Unit,
    onMarkCard: (Int, Int) -> Unit,
    onVoiceSample: (Voice) -> Unit,
) {
    var setup by rememberSaveable { mutableStateOf(Setup.Home) }
    var showHostNotice by rememberSaveable { mutableStateOf(false) }
    var askToEnd by remember { mutableStateOf(false) }
    val back = {
        if (session != null) askToEnd = true else setup = Setup.Home
    }
    BackHandler(session != null || setup != Setup.Home, onBack = back)

    when (session) {
        is Session.Host -> HostGame(session, foreground && !askToEnd, announcing, { askToEnd = true }, onSession, onVoiceSample)
        is Session.Player -> PlayerGame(session, { askToEnd = true }, onMarkCard)
        null -> when (setup) {
            Setup.Home -> Welcome(
                onHost = { showHostNotice = true },
                onPlayer = { setup = Setup.Player },
            )
            Setup.Host -> HostSetup(back, announcing, onVoiceSample) { pattern, filtered, interval, voice ->
                setup = Setup.Home
                onSession(Session.Host(pattern, filtered, interval, voice = voice))
            }
            Setup.Player -> PlayerSetup(back) { cards ->
                setup = Setup.Home
                onSession(Session.Player(cards))
            }
        }
    }

    if (showHostNotice) {
        AlertDialog(
            onDismissRequest = { showHostNotice = false },
            title = { Text("Modo Anfitrión") },
            text = { Text("Este modo está en desarrollo.") },
            confirmButton = {
                TextButton(onClick = { showHostNotice = false }) { Text("Entendido") }
            },
        )
    }

    if (askToEnd && session != null) {
        AlertDialog(
            onDismissRequest = { askToEnd = false },
            title = { Text("¿Terminar la partida?") },
            text = { Text("Se eliminará la partida activa guardada en este dispositivo.") },
            confirmButton = {
                TextButton(onClick = {
                    askToEnd = false
                    onSession(null)
                }) { Text("Terminar") }
            },
            dismissButton = { TextButton(onClick = { askToEnd = false }) { Text("Continuar") } },
        )
    }
}

@Composable
private fun Welcome(onHost: () -> Unit, onPlayer: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val compactHeight = maxHeight < 440.dp
        Column(
            Modifier.widthIn(max = 480.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compactHeight) 6.dp else 12.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.fabringo_welcome_logo),
                contentDescription = "Fabringo",
                modifier = Modifier.fillMaxWidth().heightIn(max = if (compactHeight) 134.dp else 258.dp),
                contentScale = ContentScale.Fit,
            )
            Text("Bingo de 75 bolas", color = Ice)
            WelcomeRole(
                title = "Anfitrión",
                detail = "Sorteo en desarrollo",
                icon = R.drawable.ic_role_host,
                emphasized = false,
                onClick = onHost,
            )
            WelcomeRole(
                title = "Jugador",
                detail = "Genera o escanea cartillas",
                icon = R.drawable.ic_role_player,
                emphasized = true,
                onClick = onPlayer,
            )
        }
    }
}

@Composable
private fun WelcomeRole(
    title: String,
    detail: String,
    @DrawableRes icon: Int,
    emphasized: Boolean,
    onClick: () -> Unit,
) {
    val background = if (emphasized) {
        listOf(Color(0xFFFFE39A), Color(0xFFFFC458))
    } else {
        listOf(Color(0xFF293C7A), Color(0xFF162754))
    }
    val foreground = if (emphasized) BallInk else Color(0xFFF6F8FF)
    val secondary = if (emphasized) Color(0xFF523918) else Ice
    val outline = if (emphasized) Color(0xFFFFECC0) else BlueEdge.copy(alpha = .65f)
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().semantics { role = Role.Button },
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, outline),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 68.dp)
                .background(Brush.horizontalGradient(background)).padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(42.dp).background(foreground.copy(alpha = .12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(icon), contentDescription = null, tint = foreground, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = foreground, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(detail, color = secondary, style = MaterialTheme.typography.bodySmall)
            }
            Icon(
                painterResource(R.drawable.ic_role_next), contentDescription = null,
                tint = foreground, modifier = Modifier.size(20.dp),
            )
        }
    }
}
