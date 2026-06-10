package com.davrukin.watchlist.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colors Material 3 has no roles for (market gain/loss/stale), themed for light and
 * dark and provided through [LocalExtendedColors] by [WatchlistTheme].
 */
@Immutable
data class ExtendedColors(
    val gain: Color,
    val loss: Color,
    val stale: Color,
)

val LightExtendedColors: ExtendedColors =
    ExtendedColors(
        gain = Color(color = 0xFF1B873B),
        loss = Color(color = 0xFFD32F2F),
        stale = Color(color = 0xFF757575),
    )

val DarkExtendedColors: ExtendedColors =
    ExtendedColors(
        gain = Color(color = 0xFF63D389),
        loss = Color(color = 0xFFFF8A80),
        stale = Color(color = 0xFFB0B0B0),
    )

val LocalExtendedColors =
    staticCompositionLocalOf {
        LightExtendedColors
    }
