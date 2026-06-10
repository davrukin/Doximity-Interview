package com.davrukin.watchlist.presentation.watchlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.davrukin.watchlist.ui.components.MovementIndicator
import com.davrukin.watchlist.ui.components.PriceChip
import com.davrukin.watchlist.ui.components.WatchlistDesignSystem
import com.davrukin.watchlist.ui.theme.WatchlistTheme

@Composable
fun WatchlistRow(
    row: WatchlistRowUiModel,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        content = {
            Column(
                modifier = Modifier.weight(weight = 1f),
                content = {
                    Text(
                        text = row.displaySymbol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = row.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
            Column(
                horizontalAlignment = Alignment.End,
                content = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        content = {
                            MovementIndicator(
                                movement = row.movement,
                                modifier = Modifier.padding(end = 4.dp),
                            )
                            PriceChip(
                                price = row.price ?: "—",
                                isGain = row.isGain,
                                isStale = row.isStale,
                            )
                        },
                    )
                    when {
                        row.isStale && row.price != null -> {
                            Text(
                                text =
                                    row.staleAsOf?.let { asOf: String ->
                                        "stale · $asOf"
                                    } ?: "stale",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        row.change != null -> {
                            Text(
                                text = row.change,
                                style = MaterialTheme.typography.labelMedium,
                                color =
                                    when (row.isGain) {
                                        true -> {
                                            WatchlistDesignSystem.GainColor
                                        }

                                        false -> {
                                            WatchlistDesignSystem.LossColor
                                        }

                                        null -> {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    },
                            )
                        }
                    }
                },
            )
            IconButton(
                onClick = onRemove,
                content = {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "Remove ${row.displaySymbol}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun WatchlistRowGainPreview() {
    WatchlistTheme {
        WatchlistRow(
            row =
                WatchlistRowUiModel(
                    symbol = "AAPL",
                    displaySymbol = "AAPL",
                    description = "Apple Inc.",
                    price = "$150.00",
                    change = "+2.50 (1.69%)",
                    isGain = true,
                    isStale = false,
                ),
            onRemove = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WatchlistRowLossPreview() {
    WatchlistTheme {
        WatchlistRow(
            row =
                WatchlistRowUiModel(
                    symbol = "TSLA",
                    displaySymbol = "TSLA",
                    description = "Tesla, Inc.",
                    price = "$240.00",
                    change = "-5.20 (2.12%)",
                    isGain = false,
                    isStale = false,
                ),
            onRemove = {},
        )
    }
}
