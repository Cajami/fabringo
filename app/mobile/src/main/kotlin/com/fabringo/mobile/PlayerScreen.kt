package com.fabringo.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlin.math.floor

private val TicketShape = RoundedCornerShape(18.dp)
private val CellShape = RoundedCornerShape(6.dp)
private val ScannedCardsSaver = listSaver<List<String>, String>(save = { it }, restore = { it })
private val TicketColors = listOf(
    Color(0xFFB22B4D) to Color(0xFFFFF0EB),
    Color(0xFF267253) to Color(0xFFF0F8E9),
    Color(0xFFA65A1C) to Color(0xFFFFF2D8),
)

internal fun addedCardsLabel(count: Int) = if (count == 1) "1 cartilla agregada" else "$count cartillas agregadas"

@Composable
internal fun PlayerSetup(onBack: () -> Unit, start: (List<BingoCard>) -> Unit) {
    var quantity by rememberSaveable { mutableIntStateOf(1) }
    var importing by rememberSaveable { mutableStateOf(false) }
    var usingCamera by rememberSaveable { mutableStateOf(false) }
    var accepted by rememberSaveable(stateSaver = ScannedCardsSaver) { mutableStateOf(emptyList<String>()) }
    var candidate by rememberSaveable { mutableStateOf<String?>(null) }
    var notice by rememberSaveable { mutableStateOf<String?>(null) }

    fun goBack() {
        when {
            usingCamera -> usingCamera = false
            candidate != null -> candidate = null
            importing -> importing = false
            else -> onBack()
        }
    }
    BackHandler(importing) { goBack() }

    if (usingCamera) {
        CardCamera(accepted.size, ::goBack) { text ->
            usingCamera = false
            notice = when (val scan = reviewCardScan(text, accepted)) {
                is CardScan.Ready -> {
                    candidate = scan.card.qrText
                    null
                }
                CardScan.Invalid -> "Este QR no corresponde a una cartilla válida de bingo de 75 bolas."
                CardScan.Duplicate -> "Esta cartilla ya está agregada. Escanea una diferente."
                CardScan.Limit -> "Ya llegaste al máximo de $MaxCards cartillas."
            }
        }
        return
    }

    if (importing) {
        val review = candidate?.let(::decodeCardQr)
        if (review != null) {
            ScanReview(
                card = review,
                index = accepted.size,
                onBack = ::goBack,
                onAdd = {
                    candidate?.let { text ->
                        if (reviewCardScan(text, accepted) is CardScan.Ready) {
                            accepted = accepted + text
                        }
                        candidate = null
                    }
                },
                onRescan = {
                    candidate = null
                    usingCamera = true
                },
            )
            return
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = ::goBack) { Text("Volver") }
            Text("Cartillas escaneadas", style = MaterialTheme.typography.headlineMedium, color = Gold, fontWeight = FontWeight.Black)
            if (accepted.isNotEmpty()) {
                Text(
                    addedCardsLabel(accepted.size),
                    color = Ice,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            notice?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            if (accepted.isEmpty() && notice == null) {
                Text("Escanea el QR de una cartilla para empezar.", color = Ice)
            }
            if (accepted.size < MaxCards) {
                OutlinedButton(onClick = { notice = null; usingCamera = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (accepted.isEmpty()) "Escanear cartilla" else "Escanear siguiente")
                }
            }
            if (accepted.isNotEmpty()) {
                Button(onClick = { start(accepted.mapNotNull(::decodeCardQr)) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Empezar a jugar", fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onBack) { Text("Volver") }
        Text("Jugador", style = MaterialTheme.typography.headlineLarge, color = Gold, fontWeight = FontWeight.Black)

        Column(
            Modifier.fillMaxWidth().clip(TicketShape).background(Color(0xFF1A285A))
                .border(1.dp, Gold.copy(alpha = .68f), TicketShape).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Cartillas nuevas", color = Gold, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Elige de 1 a $MaxCards cartillas.", color = Ice)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = { quantity = (quantity - 1).coerceAtLeast(1) }, enabled = quantity > 1,
                    modifier = Modifier.size(48.dp).semantics { contentDescription = "Quitar una cartilla" },
                    contentPadding = PaddingValues(0.dp),
                ) { Text("−", fontSize = 24.sp) }
                Text(
                    quantity.toString(), color = Gold, fontSize = 34.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                OutlinedButton(
                    onClick = { quantity = (quantity + 1).coerceAtMost(MaxCards) }, enabled = quantity < MaxCards,
                    modifier = Modifier.size(48.dp).semantics { contentDescription = "Añadir una cartilla" },
                    contentPadding = PaddingValues(0.dp),
                ) { Text("+", fontSize = 24.sp) }
            }
            Button(onClick = { start(generateCards(quantity)) }, modifier = Modifier.fillMaxWidth()) {
                Text("Generar cartillas", fontWeight = FontWeight.Bold)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f), color = BlueEdge.copy(alpha = .48f))
            Text("o", color = Ice, style = MaterialTheme.typography.labelLarge)
            HorizontalDivider(Modifier.weight(1f), color = BlueEdge.copy(alpha = .48f))
        }

        Column(
            Modifier.fillMaxWidth().clip(TicketShape).background(Color(0xFF142549))
                .border(1.dp, BlueEdge, TicketShape).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Cartillas con QR", color = Ice, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Escanea y confirma una cartilla a la vez.", color = Ice)
            OutlinedButton(onClick = {
                importing = true
                usingCamera = accepted.isEmpty()
            }, modifier = Modifier.fillMaxWidth()) {
                Text(if (accepted.isEmpty()) "Escanear QR" else "Continuar escaneo (${accepted.size})")
            }
        }
    }
}

@Composable
private fun ScanReview(
    card: BingoCard,
    index: Int,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onRescan: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onBack) { Text("Volver") }
            Text(
                "Revisa la cartilla ${index + 1}",
                color = Gold,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
            )
            Text("Comprueba los números antes de agregarla.", color = Ice)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Ticket(card, index, modifier = Modifier.widthIn(max = 450.dp))
            }
        }
        Surface(modifier = Modifier.fillMaxWidth(), color = Navy) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(onClick = onAdd, modifier = Modifier.widthIn(max = 450.dp).fillMaxWidth()) {
                    Text("Agregar cartilla", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onRescan) { Text("Descartar y escanear otra", color = Ice) }
            }
        }
    }
}

@Composable
internal fun PlayerGame(session: Session.Player, onEnd: () -> Unit, onMarkCard: (Int, Int) -> Unit) {
    val listState = rememberLazyListState()
    val total = session.cards.size
    var openQr by rememberSaveable { mutableStateOf<Int?>(null) }
    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Mis cartillas", color = Gold, fontWeight = FontWeight.Black, fontSize = 25.sp)
                Text(if (total == 1) "1 cartilla en juego" else "$total cartillas en juego", color = Ice)
            }
            TextButton(onClick = onEnd) { Text("Terminar") }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            itemsIndexed(session.cards, key = { _, card -> card.qrText }) { index, card ->
                Ticket(
                    card = card, index = index, footer = "Cartilla ${index + 1} de $total",
                    onQr = { openQr = index },
                ) { position -> onMarkCard(index, position) }
            }
        }
    }

    openQr?.let { index ->
        AlertDialog(
            onDismissRequest = { openQr = null },
            title = { Text("QR de la cartilla ${index + 1}") },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    QrCode(session.cards[index].qrText, Modifier.fillMaxWidth(.93f))
                }
            },
            confirmButton = { TextButton(onClick = { openQr = null }) { Text("Cerrar") } },
        )
    }
}

@Composable
private fun Ticket(
    card: BingoCard,
    index: Int,
    modifier: Modifier = Modifier,
    footer: String? = null,
    onQr: (() -> Unit)? = null,
    onMark: ((Int) -> Unit)? = null,
) {
    val (rim, paper) = TicketColors[index % TicketColors.size]
    Column(
        modifier.fillMaxWidth().clip(TicketShape)
            .background(Brush.verticalGradient(listOf(rim, Color(0xFF241930))))
            .border(1.dp, rim, TicketShape).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            BingoLetters.forEach { letter ->
                Box(Modifier.weight(1f).heightIn(min = 35.dp), contentAlignment = Alignment.Center) {
                    Text(letter.toString(), fontSize = 24.sp, lineHeight = 27.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }
        }
        for (row in 0..4) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (column in 0..4) {
                    val position = row * 5 + column
                    val cell = Modifier.weight(1f).heightIn(min = 48.dp).clip(CellShape).background(paper)
                    if (card.numbers[position] == 0) {
                        Box(cell, contentAlignment = Alignment.Center) {
                            val qrSize = Modifier.size(39.dp)
                            if (onQr == null) {
                                QrCode(card.qrText, qrSize)
                            } else {
                                TextButton(onClick = onQr, contentPadding = PaddingValues(0.dp)) {
                                    QrCode(card.qrText, qrSize.semantics { contentDescription = "Ampliar código QR de la cartilla" })
                                }
                            }
                        }
                    } else {
                        val marked = position in card.marks
                        Box(
                            if (onMark == null) cell else cell.toggleable(
                                value = marked, role = Role.Checkbox, onValueChange = { onMark(position) },
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (marked) {
                                Canvas(Modifier.matchParentSize().padding(4.dp)) {
                                    val ink = rim.copy(alpha = .78f)
                                    drawLine(ink, Offset.Zero, Offset(size.width, size.height), 4.dp.toPx(), StrokeCap.Round)
                                    drawLine(ink, Offset(0f, size.height), Offset(size.width, 0f), 4.dp.toPx(), StrokeCap.Round)
                                }
                            }
                            Text(card.numbers[position].toString(), color = BallInk, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
        if (footer != null) {
            val showQrLink = card.numbers[FreeCell] != 0 && onQr != null
            Row(
                Modifier.fillMaxWidth().clip(CellShape).background(rim.copy(alpha = .30f)).padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    footer,
                    modifier = (if (showQrLink) Modifier.weight(1f) else Modifier).padding(vertical = 8.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                if (showQrLink) TextButton(onClick = onQr) { Text("Ver QR", color = Ice) }
            }
        }
    }
}

@Composable
private fun QrCode(text: String, modifier: Modifier) {
    val matrix = remember(text) {
        QRCodeWriter().encode(
            text, BarcodeFormat.QR_CODE, 0, 0,
            mapOf(EncodeHintType.MARGIN to 4, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M),
        )
    }
    Canvas(modifier.aspectRatio(1f).background(Color.White)) {
        val module = floor(size.width / matrix.width).coerceAtLeast(1f)
        val edge = floor((size.width - module * matrix.width) / 2)
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix[x, y]) {
                    drawRect(Color.Black, Offset(edge + x * module, edge + y * module), Size(module, module))
                }
            }
        }
    }
}
