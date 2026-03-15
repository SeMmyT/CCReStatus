package com.claudescreensaver.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.claudescreensaver.data.models.AgentState
import com.claudescreensaver.ui.theme.ClaudeAccent

/**
 * Simplified pixel-art Clawd crab rendered via Canvas.
 * Animates based on agent state.
 */
@Composable
fun ClawdMascot(
    state: AgentState,
    modifier: Modifier = Modifier,
    tint: Color = ClaudeAccent,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "clawd")

    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )

    val wobble by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wobble",
    )

    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bounce",
    )

    val scale = when (state) {
        AgentState.IDLE, AgentState.COMPLETE -> breatheScale
        else -> 1f
    }

    val offsetY = when (state) {
        AgentState.AWAITING_INPUT -> bounce
        else -> 0f
    }

    Canvas(modifier = modifier.size(80.dp)) {
        val pixelSize = size.width / 10f
        val centerX = size.width / 2f
        val centerY = size.height / 2f + offsetY

        // 8-bit crab silhouette (10x8 grid)
        val pixels = listOf(
            listOf(3, 6),                           // row 0: eye stalks
            listOf(2, 3, 6, 7),                     // row 1: eye tops
            listOf(1, 2, 3, 4, 5, 6, 7, 8),       // row 2: wide claws
            listOf(2, 3, 4, 5, 6, 7),               // row 3: upper body
            listOf(1, 3, 4, 5, 6, 8),               // row 4: body with claw gap
            listOf(2, 3, 4, 5, 6, 7),               // row 5: lower body
            listOf(3, 4, 5, 6),                     // row 6: body base
            listOf(2, 3, 5, 6, 7),                  // row 7: legs
        )

        pixels.forEachIndexed { row, cols ->
            cols.forEach { col ->
                val x = (col * pixelSize * scale) + (centerX * (1 - scale))
                val y = (row * pixelSize * scale) + (centerY * (1 - scale))
                drawRect(
                    color = tint,
                    topLeft = Offset(x, y),
                    size = Size(pixelSize * scale * 0.9f, pixelSize * scale * 0.9f),
                )
            }
        }
    }
}
