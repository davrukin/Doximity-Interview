package com.davrukin.watchlist.presentation.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.davrukin.watchlist.domain.model.Instrument
import com.davrukin.watchlist.domain.model.InstrumentType
import com.davrukin.watchlist.ui.components.WatchlistToggleButton
import com.davrukin.watchlist.ui.theme.WatchlistTheme

@Composable
fun SearchResultRow(
    result: SearchUiModel.Result,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        content = {
            Column(
                modifier = Modifier.weight(weight = 1f),
                content = {
                    Text(
                        text = result.instrument.displaySymbol,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = result.instrument.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
            WatchlistToggleButton(
                isOnWatchlist = result.isOnWatchlist,
                onClick = onToggle,
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun SearchResultRowOnWatchlistPreview() {
    WatchlistTheme {
        SearchResultRow(
            result = SearchUiModel.Result(
                instrument = Instrument(
                    symbol = "AAPL",
                    displaySymbol = "AAPL",
                    description = "APPLE INC",
                    type = InstrumentType.STOCK,
                ),
                isOnWatchlist = true,
            ),
            onToggle = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchResultRowOffWatchlistPreview() {
    WatchlistTheme {
        SearchResultRow(
            result = SearchUiModel.Result(
                instrument = Instrument(
                    symbol = "BINANCE:BTCUSDT",
                    displaySymbol = "BTC/USDT",
                    description = "Binance BTCUSDT",
                    type = InstrumentType.CRYPTO,
                ),
                isOnWatchlist = false,
            ),
            onToggle = {},
        )
    }
}
