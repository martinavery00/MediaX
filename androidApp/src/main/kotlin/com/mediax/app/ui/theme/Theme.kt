@file:Suppress("ktlint:standard:function-naming")

package com.mediax.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors =
    lightColorScheme(
        primary = Color(0xFF6558D3),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE6E0FF),
        onPrimaryContainer = Color(0xFF20165F),
        secondary = Color(0xFF52606D),
        background = Color(0xFFF8F8FC),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFE9E8F0),
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFFC8BFFF),
        onPrimary = Color(0xFF35277E),
        primaryContainer = Color(0xFF4D409A),
        onPrimaryContainer = Color(0xFFE6E0FF),
        secondary = Color(0xFFBBC7D2),
        background = Color(0xFF111116),
        surface = Color(0xFF19191F),
        surfaceVariant = Color(0xFF2A2931),
    )

@Composable
fun MediaXTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
