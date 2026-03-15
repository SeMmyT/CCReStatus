package com.claudescreensaver.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.claudescreensaver.data.models.AgentState
import com.claudescreensaver.ui.theme.*

@Composable
fun StatusIndicator(
    state: AgentState,
    modifier: Modifier = Modifier,
) {
    val targetColor = when (state) {
        AgentState.IDLE -> StatusDisabled
        AgentState.THINKING -> StatusStandby
        AgentState.TOOL_CALL -> StatusRunning
        AgentState.AWAITING_INPUT -> StatusWarning
        AgentState.ERROR -> StatusCritical
        AgentState.COMPLETE -> ClaudeAccent
    }

    val color by animateColorAsState(
        targetValue = targetColor,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "statusColor",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "breathe")

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == AgentState.AWAITING_INPUT) 800 else 2000,
                easing = EaseInOut,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )

    val glowRadius by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == AgentState.AWAITING_INPUT) 800 else 2000,
                easing = EaseInOut,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowRadius",
    )

    Canvas(modifier = modifier.size(120.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = size.minDimension / 4f

        // Outer glow
        drawCircle(
            color = color.copy(alpha = glowAlpha * 0.3f),
            radius = baseRadius * 2f * glowRadius,
            center = center,
        )

        // Mid glow
        drawCircle(
            color = color.copy(alpha = glowAlpha * 0.5f),
            radius = baseRadius * 1.4f * glowRadius,
            center = center,
        )

        // Core dot
        drawCircle(
            color = color,
            radius = baseRadius,
            center = center,
        )
    }
}
