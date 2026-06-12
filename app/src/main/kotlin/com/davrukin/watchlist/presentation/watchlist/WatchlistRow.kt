package com.davrukin.watchlist.presentation.watchlist

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.davrukin.watchlist.R
import com.davrukin.watchlist.ui.components.MovementIndicator
import com.davrukin.watchlist.ui.components.PriceChip
import com.davrukin.watchlist.ui.components.Sparkline
import com.davrukin.watchlist.ui.theme.Dimens
import com.davrukin.watchlist.ui.theme.LocalExtendedColors
import com.davrukin.watchlist.ui.theme.MotionTokens
import com.davrukin.watchlist.ui.theme.WatchlistTheme

@Composable
fun WatchlistRow(
    row: WatchlistRowUiModel,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier,
        content = {
            RowContent(
                row = row,
                onRemove = onRemove,
                modifier = Modifier,
            )
        },
    )
}

@Composable
private fun RowContent(
    row: WatchlistRowUiModel,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .animateContentSize(animationSpec = tween(durationMillis = MotionTokens.STANDARD_MILLIS))
                .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.RowPadding),
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
            if (row.sparkline.size >= 2) {
                Sparkline(
                    prices = row.sparkline,
                    color =
                        if (row.sparkline.last() >= row.sparkline.first()) {
                            LocalExtendedColors.current.gain
                        } else {
                            LocalExtendedColors.current.loss
                        },
                    modifier =
                        Modifier
                            .padding(end = 12.dp)
                            .size(width = 56.dp, height = 22.dp),
                )
            }
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
                                price = row.price ?: stringResource(id = R.string.price_missing),
                                isGain = row.isGain,
                                isStale = row.isStale,
                            )
                        },
                    )
                    when {
                        row.isUnsupported -> {
                            Text(
                                text = stringResource(id = R.string.price_unsupported),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        row.isStale && row.price != null -> {
                            Text(
                                text =
                                    row.staleAsOf?.let { asOf ->
                                        stringResource(id = R.string.price_stale_as_of, asOf)
                                    } ?: stringResource(id = R.string.price_stale),
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
                                            LocalExtendedColors.current.gain
                                        }

                                        false -> {
                                            LocalExtendedColors.current.loss
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
                        contentDescription = stringResource(id = R.string.watchlist_remove, row.displaySymbol),
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
            onClick = {},
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
            onClick = {},
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
