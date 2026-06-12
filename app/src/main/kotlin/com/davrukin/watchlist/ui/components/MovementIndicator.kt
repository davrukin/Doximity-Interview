package com.davrukin.watchlist.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.davrukin.watchlist.presentation.watchlist.WatchlistRowUiModel
import com.davrukin.watchlist.ui.theme.LocalExtendedColors
import com.davrukin.watchlist.ui.theme.WatchlistTheme

@Composable
fun MovementIndicator(
    movement: WatchlistRowUiModel.PriceMovement?,
    modifier: Modifier = Modifier,
) {
    if (movement == null) {
        return
    }

    val color =
        when (movement) {
            WatchlistRowUiModel.PriceMovement.UP -> LocalExtendedColors.current.gain
            WatchlistRowUiModel.PriceMovement.DOWN -> LocalExtendedColors.current.loss
        }

    val icon =
        when (movement) {
            WatchlistRowUiModel.PriceMovement.UP -> "▲"
            WatchlistRowUiModel.PriceMovement.DOWN -> "▼"
        }

    Text(
        text = icon,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun MovementIndicatorUpPreview() {
    WatchlistTheme {
        MovementIndicator(movement = WatchlistRowUiModel.PriceMovement.UP)
    }
}

@Preview(showBackground = true)
@Composable
private fun MovementIndicatorDownPreview() {
    WatchlistTheme {
        MovementIndicator(movement = WatchlistRowUiModel.PriceMovement.DOWN)
    }
}
