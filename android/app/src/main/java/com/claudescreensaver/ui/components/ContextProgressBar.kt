package com.claudescreensaver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.claudescreensaver.ui.theme.contextBarColor

/**
 * Thin context window progress bar. Green < 70%, yellow 70-89%, red >= 90%.
 */
@Composable
fun ContextProgressBar(
    percent: Float,
    height: Dp = 3.dp,
    rounded: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val shape = if (rounded) RoundedCornerShape(height / 2) else RoundedCornerShape(0.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .let { if (rounded) it.clip(shape) else it }
            .background(Color(0xFF2A2A2A)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = (percent / 100f).coerceIn(0f, 1f))
                .let { if (rounded) it.clip(shape) else it }
                .background(contextBarColor(percent)),
        )
    }
}
