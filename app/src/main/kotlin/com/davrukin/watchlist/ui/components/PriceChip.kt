package com.davrukin.watchlist.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.davrukin.watchlist.ui.theme.LocalExtendedColors
import com.davrukin.watchlist.ui.theme.MotionTokens
import com.davrukin.watchlist.ui.theme.WatchlistTheme

@Composable
fun PriceChip(
    price: String,
    isGain: Boolean?,
    isStale: Boolean,
    modifier: Modifier = Modifier,
) {
    val backgroundColor by animateColorAsState(
        targetValue =
            when {
                isStale -> LocalExtendedColors.current.stale.copy(alpha = 0.1f)
                isGain == true -> LocalExtendedColors.current.gain.copy(alpha = 0.1f)
                isGain == false -> LocalExtendedColors.current.loss.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
        animationSpec = tween(durationMillis = MotionTokens.EMPHASIS_MILLIS),
        label = "PriceChipBackground",
    )

    val contentColor by animateColorAsState(
        targetValue =
            when {
                isStale -> LocalExtendedColors.current.stale
                isGain == true -> LocalExtendedColors.current.gain
                isGain == false -> LocalExtendedColors.current.loss
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        animationSpec = tween(durationMillis = MotionTokens.EMPHASIS_MILLIS),
        label = "PriceChipContent",
    )

    Box(
        modifier =
            modifier
                .clip(shape = RoundedCornerShape(size = 8.dp))
                .background(color = backgroundColor)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        content = {
            Text(
                text = price,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun PriceChipGainPreview() {
    WatchlistTheme {
        PriceChip(
            price = "$150.00",
            isGain = true,
            isStale = false,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PriceChipLossPreview() {
    WatchlistTheme {
        PriceChip(
            price = "$145.00",
            isGain = false,
            isStale = false,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PriceChipStalePreview() {
    WatchlistTheme {
        PriceChip(
            price = "$148.00",
            isGain = null,
            isStale = true,
            modifier = Modifier.padding(16.dp),
        )
    }
}
