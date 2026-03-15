package com.claudescreensaver.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ClaudeDarkColorScheme = darkColorScheme(
    primary = ClaudeAccent,
    secondary = ClaudeAccentDeep,
    background = ClaudeBgDark,
    surface = ClaudeBgDark,
    onPrimary = ClaudeTextLight,
    onSecondary = ClaudeTextLight,
    onBackground = ClaudeTextLight,
    onSurface = ClaudeTextLight,
    outline = ClaudeGray,
)

@Composable
fun ClaudeScreenSaverTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ClaudeDarkColorScheme,
        typography = ClaudeTypography,
        content = content,
    )
}
