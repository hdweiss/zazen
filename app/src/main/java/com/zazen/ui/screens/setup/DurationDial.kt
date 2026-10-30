package com.zazen.ui.screens.setup

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/** One full turn of the dial. */
private const val MINUTES_PER_TURN = 60f
const val DIAL_MAX_MINUTES = 240

/**
 * Snap a continuous dial position to the values the +/- steppers used to offer:
 * 0 (open-ended), 1-minute steps up to 5, then 5-minute steps.
 */
private fun snapMinutes(raw: Float): Int = when {
    raw < 0.5f -> 0
    raw < 5f -> raw.roundToInt().coerceAtLeast(1)
    else -> ((raw / 5f).roundToInt() * 5).coerceAtMost(DIAL_MAX_MINUTES)
}

/**
 * The idle timer ring, dragged around its circumference to set the sit length.
 * One turn is an hour and further turns keep adding time; dragging back past
 * zero selects an open-ended sit ([minutes] == 0).
 *
 * Only drags that start on the ring itself count, so a stray touch near the
 * centre (where the time is shown and tappable) can't change the duration.
 */
@Composable
fun DurationDial(
    minutes: Float,
    onMinutesChange: (Int) -> Unit,
    contentDescription: String,
    stateDescription: String,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 8.dp,
) {
    val haptics = LocalHapticFeedback.current
    val currentMinutes by rememberUpdatedState(minutes)
    val currentOnChange by rememberUpdatedState(onMinutesChange)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val arcColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
            .semantics {
                this.contentDescription = contentDescription
                this.stateDescription = stateDescription
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = minutes,
                    range = 0f..DIAL_MAX_MINUTES.toFloat(),
                )
                setProgress { target ->
                    val snapped = snapMinutes(target)
                    if (snapped != currentMinutes.roundToInt()) currentOnChange(snapped)
                    true
                }
            }
            .pointerInput(Unit) {
                val touchSlop = 32.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = (size.width.coerceAtMost(size.height) - strokeWidth.toPx()) / 2f
                    val dist = hypot(down.position.x - center.x, down.position.y - center.y)
                    if (kotlin.math.abs(dist - radius) > touchSlop) return@awaitEachGesture

                    down.consume()
                    var raw = currentMinutes
                    var lastAngle = atan2(down.position.y - center.y, down.position.x - center.x)
                    var lastSnapped = snapMinutes(raw)

                    drag(down.id) { change ->
                        change.consume()
                        val angle = atan2(change.position.y - center.y, change.position.x - center.x)
                        // Wrap into (-π, π] so crossing 12 o'clock doesn't jump a turn.
                        var delta = angle - lastAngle
                        if (delta > PI) delta -= (2 * PI).toFloat()
                        if (delta < -PI) delta += (2 * PI).toFloat()
                        lastAngle = angle
                        // Screen y points down, so a positive delta is clockwise.
                        raw = (raw + delta / (2 * PI).toFloat() * MINUTES_PER_TURN)
                            .coerceIn(0f, DIAL_MAX_MINUTES.toFloat())
                        val snapped = snapMinutes(raw)
                        if (snapped != lastSnapped) {
                            lastSnapped = snapped
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentOnChange(snapped)
                        }
                    }
                }
            },
    ) {
        val stroke = strokeWidth.toPx()
        val diameter = size.minDimension - stroke
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
        val arcSize = Size(diameter, diameter)
        val ringStyle = Stroke(width = stroke, cap = StrokeCap.Round)

        // Completed turns tint the whole track, so 75 min reads as "a full hour
        // plus a quarter" rather than looking the same as 15 min.
        val turns = (minutes / MINUTES_PER_TURN).toInt()
        val partial = (minutes % MINUTES_PER_TURN) / MINUTES_PER_TURN
        drawArc(
            color = if (turns > 0) arcColor.copy(alpha = 0.35f) else trackColor,
            startAngle = -90f, sweepAngle = 360f, useCenter = false,
            topLeft = topLeft, size = arcSize, style = ringStyle,
        )
        val sweep = when {
            minutes <= 0f -> 0f
            partial == 0f -> 360f
            else -> 360f * partial
        }
        if (sweep > 0f) {
            drawArc(
                color = arcColor,
                startAngle = -90f, sweepAngle = sweep, useCenter = false,
                topLeft = topLeft, size = arcSize, style = ringStyle,
            )
        }

        // Handle at the end of the arc (12 o'clock when open-ended): shows where to grab.
        val endRad = Math.toRadians((-90f + sweep).toDouble())
        val radius = diameter / 2
        drawCircle(
            color = arcColor,
            radius = stroke * 1.6f,
            center = Offset(
                size.width / 2 + radius * cos(endRad).toFloat(),
                size.height / 2 + radius * sin(endRad).toFloat(),
            ),
        )
    }
}
