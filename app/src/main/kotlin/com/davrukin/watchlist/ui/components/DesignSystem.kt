package com.davrukin.watchlist.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A custom design system for the Watchlist app.
 */
object WatchlistDesignSystem {
    val GainColor: Color = Color(color = 0xFF1B873B)
    val LossColor: Color = Color(color = 0xFFD32F2F)
    val StaleColor: Color = Color(color = 0xFF757575)
}

@Composable
fun PriceChip(
    price: String,
    isGain: Boolean?,
    isStale: Boolean,
    modifier: Modifier = Modifier,
) {
    val backgroundColor: Color by animateColorAsState(
        targetValue = when {
            isStale -> WatchlistDesignSystem.StaleColor.copy(alpha = 0.1f)
            isGain == true -> WatchlistDesignSystem.GainColor.copy(alpha = 0.1f)
            isGain == false -> WatchlistDesignSystem.LossColor.copy(alpha = 0.1f)
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(durationMillis = 500),
        label = "PriceChipBackground",
    )

    val contentColor: Color by animateColorAsState(
        targetValue = when {
            isStale -> WatchlistDesignSystem.StaleColor
            isGain == true -> WatchlistDesignSystem.GainColor
            isGain == false -> WatchlistDesignSystem.LossColor
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 500),
        label = "PriceChipContent",
    )

    Box(
        modifier = modifier
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = price,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor,
        )
    }
}

@Composable
fun MovementIndicator(
    movement: com.davrukin.watchlist.presentation.watchlist.WatchlistRowUiModel.PriceMovement?,
    modifier: Modifier = Modifier,
) {
    if (movement == null) {
        return
    }

    val color: Color = when (movement) {
        com.davrukin.watchlist.presentation.watchlist.WatchlistRowUiModel.PriceMovement.UP -> WatchlistDesignSystem.GainColor
        com.davrukin.watchlist.presentation.watchlist.WatchlistRowUiModel.PriceMovement.DOWN -> WatchlistDesignSystem.LossColor
    }

    val icon: String = when (movement) {
        com.davrukin.watchlist.presentation.watchlist.WatchlistRowUiModel.PriceMovement.UP -> "▲"
        com.davrukin.watchlist.presentation.watchlist.WatchlistRowUiModel.PriceMovement.DOWN -> "▼"
    }

    Text(
        text = icon,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier,
    )
}

@Composable
fun WatchlistToggleButton(
    isOnWatchlist: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = if (isOnWatchlist) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ),
        modifier = modifier,
    ) {
        Icon(
            imageVector = if (isOnWatchlist) {
                Icons.Filled.Check
            } else {
                Icons.Filled.Add
            },
            contentDescription = if (isOnWatchlist) {
                "Remove from watchlist"
            } else {
                "Add to watchlist"
            },
        )
    }
}
