package com.claudescreensaver.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.claudescreensaver.data.models.AgentState
import com.claudescreensaver.data.models.AgentStatus
import com.claudescreensaver.ui.components.GhostMascot
import com.claudescreensaver.ui.components.ConnectionBadge
import com.claudescreensaver.ui.components.SessionCard
import com.claudescreensaver.ui.components.StatusIndicator
import com.claudescreensaver.ui.theme.ClaudeBgDark
import com.claudescreensaver.ui.theme.ClaudeGray
import com.claudescreensaver.ui.theme.ClaudeAccent
import com.claudescreensaver.viewmodel.UiState
import kotlin.math.roundToInt

@Composable
fun StatusDashboardScreen(
    uiState: UiState,
    displayMode: String = "advanced",
    onSessionTap: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (displayMode == "simple") {
        SimpleStatusScreen(uiState = uiState, modifier = modifier)
        return
    }

    // Burn-in prevention: Lissajous pixel shift
    val infiniteTransition = rememberInfiniteTransition(label = "pixelShift")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shiftX",
    )
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 90_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shiftY",
    )

    val maxPanes = 10
    val activeSessions = uiState.sessions.values
        .sortedByDescending { it.timestamp }
        .take(maxPanes)

    // Reorder state: maps slot index to session ID
    var slotOrder by remember { mutableStateOf<List<String>>(emptyList()) }
    // Track which slot is selected for swap (long-press mode)
    var selectedSlot by remember { mutableIntStateOf(-1) }

    // Sync slot order with incoming sessions, preserving user order
    val sessionIds = activeSessions.map { it.sessionId }
    val currentOrder = remember(sessionIds.toSet()) {
        // Keep existing ordered IDs that are still active, append new ones
        val kept = slotOrder.filter { it in sessionIds }
        val newIds = sessionIds.filter { it !in kept }
        kept + newIds
    }
    LaunchedEffect(currentOrder) { slotOrder = currentOrder }

    // Build ordered list
    val orderedSessions = slotOrder.mapNotNull { id ->
        activeSessions.find { it.sessionId == id }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ClaudeBgDark),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .padding(8.dp),
        ) {
            // Top bar: Ghost mascot + connection badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GhostMascot(
                        state = uiState.agentStatus.state,
                        skin = uiState.activeSkin,
                        modifier = Modifier.size(32.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Agent Code",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                ConnectionBadge(
                    state = uiState.connectionState,
                    instanceName = uiState.agentStatus.instanceName,
                )
            }

            when {
                orderedSessions.isNotEmpty() -> {
                    // Dynamic grid: pick columns based on count
                    // 1=1col, 2=2col, 3-4=2col, 5-6=3col, 7-9=3col, 10=3col (last row has ghost)
                    val cols = when {
                        orderedSessions.size <= 1 -> 1
                        orderedSessions.size <= 4 -> 2
                        else -> 3
                    }

                    // For single session, show mascot above
                    if (orderedSessions.size == 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.35f),
                            contentAlignment = Alignment.Center,
                        ) {
                            GhostMascot(
                                state = orderedSessions.first().state,
                                skin = uiState.activeSkin,
                                modifier = Modifier
                                    .fillMaxHeight(0.85f)
                                    .aspectRatio(1f),
                            )
                        }
                    }

                    val rows = orderedSessions.chunked(cols)
                    val showMascotInLastRow = orderedSessions.size > 1 &&
                            rows.last().size < cols && orderedSessions.size % cols != 0

                    rows.forEachIndexed { rowIdx, row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            row.forEachIndexed { colIdx, session ->
                                val slotIdx = rowIdx * cols + colIdx
                                SessionSlot(
                                    session = session,
                                    slotIdx = slotIdx,
                                    selectedSlot = selectedSlot,
                                    slotOrder = slotOrder,
                                    onSlotOrderChanged = { slotOrder = it },
                                    onSelectedSlotChanged = { selectedSlot = it },
                                    onSessionTap = onSessionTap,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(vertical = 2.dp),
                                )
                            }
                            // Fill empty slots in last row: mascot in first empty, spacers for rest
                            if (showMascotInLastRow && rowIdx == rows.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(vertical = 2.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    GhostMascot(
                                        state = uiState.agentStatus.state,
                                        skin = uiState.activeSkin,
                                        modifier = Modifier
                                            .fillMaxSize(0.75f)
                                            .aspectRatio(1f),
                                    )
                                }
                                // Any remaining empty slots
                                repeat(cols - row.size - 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                else -> {
                    // No sessions: centered status display
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        StatusIndicator(
                            state = uiState.agentStatus.state,
                            modifier = Modifier.padding(bottom = 16.dp),
                        )

                        val statusLabel = when (uiState.agentStatus.state) {
                            AgentState.IDLE -> "Idle"
                            AgentState.THINKING -> "Thinking"
                            AgentState.TOOL_CALL -> uiState.agentStatus.tool ?: "Working"
                            AgentState.AWAITING_INPUT -> "Input Required"
                            AgentState.ERROR -> "Error"
                            AgentState.COMPLETE -> "Complete"
                        }

                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )

                        if (uiState.agentStatus.toolInputSummary.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = uiState.agentStatus.toolInputSummary,
                                style = MaterialTheme.typography.headlineMedium,
                                color = ClaudeGray,
                                maxLines = 2,
                            )
                        }

                        if (uiState.agentStatus.message.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = uiState.agentStatus.message,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (uiState.agentStatus.requiresInput)
                                    MaterialTheme.colorScheme.primary
                                else ClaudeGray,
                                maxLines = 3,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionSlot(
    session: AgentStatus,
    slotIdx: Int,
    selectedSlot: Int,
    slotOrder: List<String>,
    onSlotOrderChanged: (List<String>) -> Unit,
    onSelectedSlotChanged: (Int) -> Unit,
    onSessionTap: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val isSelected = selectedSlot == slotIdx
    Box(
        modifier = modifier
            .then(
                if (isSelected) Modifier.background(
                    ClaudeAccent.copy(alpha = 0.15f)
                ) else Modifier
            )
            .pointerInput(slotIdx) {
                detectTapGestures(
                    onLongPress = {
                        onSelectedSlotChanged(if (isSelected) -1 else slotIdx)
                    },
                    onTap = {
                        if (selectedSlot >= 0 && selectedSlot != slotIdx) {
                            val newOrder = slotOrder.toMutableList()
                            if (selectedSlot < newOrder.size && slotIdx < newOrder.size) {
                                val tmp = newOrder[selectedSlot]
                                newOrder[selectedSlot] = newOrder[slotIdx]
                                newOrder[slotIdx] = tmp
                                onSlotOrderChanged(newOrder)
                            }
                            onSelectedSlotChanged(-1)
                        } else if (selectedSlot < 0) {
                            onSessionTap?.invoke(session.sessionId)
                        }
                    },
                )
            },
    ) {
        SessionCard(
            status = session,
            modifier = Modifier.fillMaxSize(),
        )
        if (isSelected) {
            Text(
                text = "TAP TARGET TO SWAP",
                fontSize = 9.sp,
                color = ClaudeAccent,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp),
            )
        }
    }
}
