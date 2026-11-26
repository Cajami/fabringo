package com.fabringo.mobile

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

internal const val FreeCell = 12
internal const val MaxCards = 10
internal const val BallCount = 75
internal val BingoLetters = "BINGO"
internal val DrawIntervals = listOf(5, 10, 15, 20, 30)

internal enum class Pattern(val label: String, val cells: Set<Int>) {
    Full("Cartón lleno", (0 until 25).toSet()),
    ColumnB("Columna B", columnCells(0)),
    ColumnI("Columna I", columnCells(1)),
    ColumnN("Columna N", columnCells(2)),
    ColumnG("Columna G", columnCells(3)),
    ColumnO("Columna O", columnCells(4)),
    H("Letra H", columnCells(0) + columnCells(4) + (10..14)),
    Corners("Cuatro esquinas", setOf(0, 4, 20, 24)),
    X("Letra X", (0..4).flatMap { listOf(it * 6, 4 + it * 4) }.toSet()),
    Plus("Más (+)", columnCells(2) + (10..14)),
    O("Letra O", columnCells(0) + columnCells(4) + (0..4) + (20..24)),
    Diamond("Diamante", (0 until 25).filter {
        kotlin.math.abs(it / 5 - 2) + kotlin.math.abs(it % 5 - 2) <= 2
    }.toSet());

    val requiredColumns: Set<Int> = cells.filter { it != FreeCell }.map { it % 5 }.toSet()

    fun ballPool(filtered: Boolean): List<Int> = (1..BallCount).filter {
        !filtered || (it - 1) / 15 in requiredColumns
    }
}

private fun columnCells(column: Int) = (0..4).map { it * 5 + column }.toSet()

internal fun ballLetter(number: Int): Char = BingoLetters[(number - 1) / 15]

internal sealed interface Session {
    data class Host(
        val pattern: Pattern,
        val filtered: Boolean,
        val interval: Int? = null,
        val called: List<Int> = emptyList(),
        val voice: Voice = Voice.Feminine,
    ) : Session {
        val pool: List<Int> get() = pattern.ballPool(filtered)
    }

    data class Player(val cards: List<BingoCard>) : Session
}

internal data class BingoCard(val numbers: List<Int>, val marks: Set<Int> = emptySet()) {
    val qrText: String get() = numbers.joinToString("") { it.toString().padStart(2, '0') }

    fun toggle(position: Int): BingoCard = copy(
        marks = if (position in marks) marks - position else marks + position,
    )
}

internal sealed interface CardScan {
    data class Ready(val card: BingoCard) : CardScan
    data object Invalid : CardScan
    data object Duplicate : CardScan
    data object Limit : CardScan
}

internal fun decodeCardQr(text: String): BingoCard? {
    if (text.length != 50 || text.any { it !in '0'..'9' }) return null
    val numbers = text.chunked(2).map(String::toInt)
    val nonzero = numbers.filter { it != 0 }
    if (nonzero.size != nonzero.toSet().size) return null
    val valid = numbers.withIndex().all { (index, number) ->
        val first = 1 + (index % 5) * 15
        (number == 0 && index == FreeCell) || number in first..(first + 14)
    }
    return if (valid) BingoCard(numbers) else null
}

internal fun reviewCardScan(text: String, accepted: Collection<String>): CardScan {
    val card = decodeCardQr(text) ?: return CardScan.Invalid
    if (card.qrText in accepted) return CardScan.Duplicate
    if (accepted.size >= MaxCards) return CardScan.Limit
    return CardScan.Ready(card)
}

internal fun generateCards(count: Int): List<BingoCard> {
    require(count in 1..MaxCards)
    val issued = linkedSetOf<List<Int>>()
    while (issued.size < count) {
        val columns = (0..4).map { column ->
            (column * 15 + 1..column * 15 + 15).shuffled().take(5)
        }
        issued += List(25) { position ->
            if (position == FreeCell) 0 else columns[position % 5][position / 5]
        }
    }
    return issued.map { BingoCard(it) }
}

internal class SessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("fabringo", Context.MODE_PRIVATE)

    fun load(): Session? {
        val saved = preferences.getString("active_session", null) ?: return null
        return runCatching {
            val json = JSONObject(saved)
            when (json.getString("role")) {
                "host" -> {
                    val pattern = Pattern.valueOf(json.getString("pattern"))
                    val filtered = json.getBoolean("filtered")
                    val pool = pattern.ballPool(filtered).toSet()
                    Session.Host(
                        pattern,
                        filtered,
                        json.optInt("interval").takeIf { it in DrawIntervals },
                        json.getJSONArray("called").integers().filter { it in pool }.distinct(),
                        Voice.entries.find { it.name == json.optString("voice") } ?: Voice.Feminine,
                    )
                }
                "player" -> {
                    val cards = json.getJSONArray("cards")
                    Session.Player(List(cards.length()) { index ->
                        val card = cards.getJSONObject(index)
                        BingoCard(card.getJSONArray("numbers").integers(), card.getJSONArray("marks").integers().toSet())
                    })
                }
                else -> null
            }
        }.getOrNull()
    }

    fun save(session: Session?) {
        val json = when (session) {
            is Session.Host -> JSONObject()
                .put("role", "host").put("pattern", session.pattern.name)
                .put("filtered", session.filtered).put("interval", session.interval ?: 0)
                .put("voice", session.voice.name)
                .put("called", JSONArray(session.called))
            is Session.Player -> JSONObject().put("role", "player").put("cards", JSONArray(
                session.cards.map { card ->
                    JSONObject().put("numbers", JSONArray(card.numbers)).put("marks", JSONArray(card.marks.toList()))
                },
            ))
            null -> null
        }
        preferences.edit {
            if (json == null) remove("active_session") else putString("active_session", json.toString())
        }
    }
}

private fun JSONArray.integers(): List<Int> = List(length(), ::getInt)
