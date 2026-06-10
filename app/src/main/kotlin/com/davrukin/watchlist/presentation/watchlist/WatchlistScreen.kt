package com.davrukin.watchlist.presentation.watchlist

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.davrukin.watchlist.domain.model.ConnectionState
import com.davrukin.watchlist.domain.model.MarketDataMode
import com.davrukin.watchlist.ui.theme.WatchlistTheme

import com.davrukin.watchlist.ui.components.MovementIndicator
import com.davrukin.watchlist.ui.components.PriceChip
import com.davrukin.watchlist.ui.components.WatchlistDesignSystem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    model: WatchlistUiModel,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Watchlist")
                },
                actions = {
                    DataModeChip(
                        dataMode = model.dataMode,
                        isLiveAvailable = model.isLiveAvailable,
                        onToggle = {
                            model.eventHandler.onEvent(
                                event = WatchlistUiModel.Event.ToggleDataMode,
                            )
                        },
                    )
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    model.eventHandler.onEvent(
                        event = WatchlistUiModel.Event.OpenSearch,
                    )
                },
                content = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Search instruments",
                    )
                },
            )
        },
        content = { paddingValues: androidx.compose.foundation.layout.PaddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues = paddingValues),
                content = {
                    ConnectionBanner(
                        connectionState = model.connectionState,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    when {
                        model.isLoading -> {
                            LoadingState(modifier = Modifier.fillMaxSize())
                        }

                        model.items.isEmpty() -> {
                            EmptyState(modifier = Modifier.fillMaxSize())
                        }

                        else -> {
                            PullToRefreshBox(
                                isRefreshing = model.isRefreshing,
                                onRefresh = {
                                    model.eventHandler.onEvent(
                                        event = WatchlistUiModel.Event.Refresh,
                                    )
                                },
                                modifier = Modifier.fillMaxSize(),
                                content = {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        content = {
                                            items(
                                                items = model.items,
                                                key = { row: WatchlistRowUiModel ->
                                                    row.symbol
                                                },
                                                itemContent = { row: WatchlistRowUiModel ->
                                                    WatchlistRow(
                                                        row = row,
                                                        onRemove = {
                                                            model.eventHandler.onEvent(
                                                                event = WatchlistUiModel.Event.Remove(
                                                                    symbol = row.symbol,
                                                                ),
                                                            )
                                                        },
                                                        modifier = Modifier.fillMaxWidth(),
                                                    )
                                                },
                                            )
                                        },
                                    )
                                },
                            )
                        }
                    }
                },
            )
        },
    )
}

@Composable
private fun DataModeChip(
    dataMode: MarketDataMode,
    isLiveAvailable: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = onToggle,
        enabled = isLiveAvailable,
        label = {
            Text(
                text = when (dataMode) {
                    MarketDataMode.LIVE -> {
                        "Live"
                    }

                    MarketDataMode.DEMO -> {
                        "Demo"
                    }
                },
            )
        },
        colors = when (dataMode) {
            MarketDataMode.LIVE -> {
                AssistChipDefaults.assistChipColors()
            }

            MarketDataMode.DEMO -> {
                AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        },
        modifier = modifier.padding(end = 12.dp),
    )
}

@Composable
private fun ConnectionBanner(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier,
) {
    if (connectionState == ConnectionState.CONNECTED) {
        return
    }
    Surface(
        color = when (connectionState) {
            ConnectionState.OFFLINE -> {
                MaterialTheme.colorScheme.errorContainer
            }

            else -> {
                MaterialTheme.colorScheme.secondaryContainer
            }
        },
        modifier = modifier,
        content = {
            Text(
                text = when (connectionState) {
                    ConnectionState.CONNECTING -> {
                        "Connecting to live prices…"
                    }

                    ConnectionState.RECONNECTING -> {
                        "Connection lost — reconnecting…"
                    }

                    ConnectionState.OFFLINE -> {
                        "Offline — retrying. Prices may be out of date."
                    }

                    ConnectionState.CONNECTED -> {
                        ""
                    }
                },
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        },
    )
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
        content = {
            CircularProgressIndicator()
        },
    )
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            Text(
                text = "Your watchlist is empty",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Tap + to search for stocks and crypto",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        },
    )
}

@Composable
private fun WatchlistRow(
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
                                text = row.staleAsOf?.let { asOf: String ->
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
                                color = when (row.isGain) {
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

private class WatchlistPreviewProvider : PreviewParameterProvider<WatchlistUiModel> {
    override val values: Sequence<WatchlistUiModel> =
        sequenceOf(
            previewModel(),
            previewModel().copy(connectionState = ConnectionState.RECONNECTING),
            previewModel().copy(connectionState = ConnectionState.OFFLINE, dataMode = MarketDataMode.DEMO),
            previewModel().copy(items = emptyList()),
            previewModel().copy(items = emptyList(), isLoading = true),
        )

    private fun previewModel(): WatchlistUiModel {
        return WatchlistUiModel(
            items = listOf(
                WatchlistRowUiModel(
                    symbol = "AAPL",
                    displaySymbol = "AAPL",
                    description = "APPLE INC",
                    price = "228.40",
                    change = "+1.20 (+0.53%)",
                    isGain = true,
                    isStale = false,
                    movement = WatchlistRowUiModel.PriceMovement.UP,
                ),
                WatchlistRowUiModel(
                    symbol = "TSLA",
                    displaySymbol = "TSLA",
                    description = "TESLA INC",
                    price = "318.50",
                    change = "-4.10 (-1.27%)",
                    isGain = false,
                    isStale = false,
                    movement = WatchlistRowUiModel.PriceMovement.DOWN,
                ),
                WatchlistRowUiModel(
                    symbol = "MSFT",
                    displaySymbol = "MSFT",
                    description = "MICROSOFT CORP",
                    price = "512.70",
                    change = "+0.90 (+0.18%)",
                    isGain = true,
                    isStale = true,
                ),
                WatchlistRowUiModel(
                    symbol = "BINANCE:BTCUSDT",
                    displaySymbol = "BTC/USDT",
                    description = "Binance BTCUSDT",
                    price = null,
                    change = null,
                    isGain = null,
                    isStale = false,
                ),
            ),
            isLoading = false,
            isRefreshing = false,
            connectionState = ConnectionState.CONNECTED,
            dataMode = MarketDataMode.LIVE,
            isLiveAvailable = true,
            eventHandler = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WatchlistScreenPreview(
    @PreviewParameter(provider = WatchlistPreviewProvider::class) model: WatchlistUiModel,
) {
    WatchlistTheme(
        dynamicColor = false,
        content = {
            WatchlistScreen(model = model)
        },
    )
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WatchlistScreenDarkPreview() {
    WatchlistTheme(
        dynamicColor = false,
        content = {
            WatchlistScreen(model = WatchlistPreviewProvider().values.first())
        },
    )
}
