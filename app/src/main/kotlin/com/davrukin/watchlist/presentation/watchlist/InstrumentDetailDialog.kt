package com.davrukin.watchlist.presentation.watchlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.davrukin.watchlist.R
import com.davrukin.watchlist.domain.model.InstrumentType
import com.davrukin.watchlist.ui.components.PriceChip
import com.davrukin.watchlist.ui.components.Sparkline
import com.davrukin.watchlist.ui.components.WatchlistDesignSystem
import com.davrukin.watchlist.ui.theme.WatchlistTheme

@Composable
fun InstrumentDetailDialog(
    row: WatchlistRowUiModel,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = row.displaySymbol)
        },
        text = {
            Column(
                content = {
                    Text(
                        text = row.description,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    DetailField(
                        label = stringResource(id = R.string.detail_symbol_label),
                        value = row.symbol,
                    )
                    DetailField(
                        label = stringResource(id = R.string.detail_type_label),
                        value =
                            when (row.type) {
                                InstrumentType.STOCK -> stringResource(id = R.string.detail_type_stock)
                                InstrumentType.CRYPTO -> stringResource(id = R.string.detail_type_crypto)
                            },
                    )
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        content = {
                            PriceChip(
                                price = row.price ?: stringResource(id = R.string.price_missing),
                                isGain = row.isGain,
                                isStale = row.isStale,
                            )
                        },
                    )
                    if (row.change != null) {
                        Text(
                            text = row.change,
                            style = MaterialTheme.typography.labelMedium,
                            color =
                                when (row.isGain) {
                                    true -> WatchlistDesignSystem.GainColor
                                    false -> WatchlistDesignSystem.LossColor
                                    null -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    if (row.isStale && row.staleAsOf != null) {
                        Text(
                            text = stringResource(id = R.string.price_stale_as_of, row.staleAsOf),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    if (row.sparkline.size >= 2) {
                        Sparkline(
                            prices = row.sparkline,
                            color =
                                when (row.isGain) {
                                    false -> WatchlistDesignSystem.LossColor
                                    else -> WatchlistDesignSystem.GainColor
                                },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(height = 48.dp)
                                    .padding(top = 12.dp),
                        )
                    }
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                content = {
                    Text(text = stringResource(id = R.string.detail_close))
                },
            )
        },
    )
}

@Composable
private fun DetailField(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.padding(top = 8.dp),
        content = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(weight = 1f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun InstrumentDetailDialogStockPreview() {
    WatchlistTheme {
        InstrumentDetailDialog(
            row =
                WatchlistRowUiModel(
                    symbol = "AAPL",
                    displaySymbol = "AAPL",
                    description = "APPLE INC",
                    type = InstrumentType.STOCK,
                    price = "228.40",
                    change = "+2.15 (+0.95%)",
                    isGain = true,
                    isStale = false,
                    sparkline = listOf(226.1, 226.8, 227.4, 227.0, 228.4),
                ),
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InstrumentDetailDialogStaleCryptoPreview() {
    WatchlistTheme {
        InstrumentDetailDialog(
            row =
                WatchlistRowUiModel(
                    symbol = "BINANCE:BTCUSDT",
                    displaySymbol = "BTC/USDT",
                    description = "Binance BTCUSDT",
                    type = InstrumentType.CRYPTO,
                    price = "104,250.00",
                    change = null,
                    isGain = null,
                    isStale = true,
                    staleAsOf = "Jun 9, 8:05 PM",
                ),
            onDismiss = {},
        )
    }
}
