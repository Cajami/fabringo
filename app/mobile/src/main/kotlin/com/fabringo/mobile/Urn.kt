package com.fabringo.mobile

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private val PelletColors = listOf(
    Color(0xFFFF5578), Color(0xFFFFCA50), Color(0xFF50CDFF), Color(0xFF8EE15E), Color(0xFFB895FF),
)

private val PelletPositions = listOf(
    .24f to .57f, .36f to .49f, .47f to .53f, .56f to .48f, .69f to .54f,
    .18f to .66f, .29f to .65f, .41f to .63f, .52f to .61f, .64f to .65f, .76f to .65f,
    .25f to .74f, .36f to .73f, .48f to .74f, .59f to .72f, .70f to .75f,
    .33f to .80f, .45f to .82f, .56f to .82f, .65f to .80f,
)

@Composable
internal fun Urn(mixing: Boolean, modifier: Modifier = Modifier) {
    val motion = remember { Animatable(0f) }
    LaunchedEffect(mixing) {
        motion.snapTo(0f)
        if (mixing) motion.animateTo(1f, tween(790, easing = LinearEasing))
    }
    Canvas(modifier.aspectRatio(1.02f)) {
        val w = size.width
        val motionFraction = motion.value
        val amplitude = sin(PI * motionFraction).toFloat()
        val tilt = sin(PI * motionFraction * 8).toFloat() * amplitude * 7
        val center = Offset(w * .50f, w * .45f)
        val radius = w * .39f
        val gold = listOf(Color(0xFFFFF2BE), Color(0xFFF0B145), Color(0xFF8B3B19))
        drawOval(
            Brush.radialGradient(listOf(Color(0xFF8E80F5), Color.Transparent), Offset(w / 2, w * .93f), w * .48f),
            Offset(w * .04f, w * .84f), Size(w * .92f, w * .16f),
        )
        rotate(tilt, pivot = Offset(w * .5f, w * .8f)) {
            drawRoundRect(Brush.verticalGradient(gold, w * .77f, w * .92f), Offset(w * .08f, w * .80f), Size(w * .84f, w * .115f), CornerRadius(w * .045f))
            drawRoundRect(Brush.verticalGradient(gold, w * .02f, w * .13f), Offset(w * .37f, w * .03f), Size(w * .26f, w * .085f), CornerRadius(w * .03f))
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFFBFEBFF), Color(0xFF3C62AA), Color(0xFF131A55)), Offset(w * .31f, w * .24f), w * .57f),
                radius, center,
            )
            val interior = Path().apply { addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)) }
            clipPath(interior) {
                PelletPositions.forEachIndexed { index, (x, y) ->
                    val jump = abs(sin(PI * (motionFraction * 2.2 + index * .19))).toFloat() * amplitude
                    val sway = sin(PI * (motionFraction * 5 + index * .32)).toFloat() * amplitude
                    val location = Offset(w * (x + .11f * sway), w * (y - (.21f + index % 4 * .045f) * jump))
                    val pelletRadius = w * (.051f + index % 3 * .0045f)
                    val tint = PelletColors[(index + index / 5) % PelletColors.size]
                    val highlight = location - Offset(pelletRadius * .33f, pelletRadius * .40f)
                    drawCircle(Color(0x60250F46), pelletRadius, location + Offset(w * .008f, w * .012f))
                    drawCircle(
                        Brush.radialGradient(listOf(Color(0xFFFFF9F0), tint, Color(0xFF20154C)), highlight, pelletRadius * 1.5f),
                        pelletRadius, location,
                    )
                    drawCircle(Color(0xDFFFEDCC), pelletRadius * .29f, location + Offset(pelletRadius * .15f, pelletRadius * .13f))
                }
                drawOval(
                    Brush.radialGradient(listOf(Color(0xDEFFFFFF), Color.Transparent), Offset(w * .33f, w * .25f), w * .18f),
                    Offset(w * .20f, w * .15f), Size(w * .32f, w * .15f),
                )
            }
            drawCircle(Color(0xFFAAD7FF), radius, center, style = Stroke(w * .012f))
            drawCircle(Color(0x65FFFFFF), radius * .965f, center, style = Stroke(w * .006f))
        }
    }
}
