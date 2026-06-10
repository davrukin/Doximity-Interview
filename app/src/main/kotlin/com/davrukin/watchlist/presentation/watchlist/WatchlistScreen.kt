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
        content = { padding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues = padding),
                content = {
                    ConnectionBanner(connectionState = model.connectionState)
                    when {
                        model.isLoading -> LoadingState()
                        model.items.isEmpty() -> EmptyState()
                        else ->
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
                                                key = {
                                                    it.symbol
                                                },
                                                itemContent = { row ->
                                                    WatchlistRow(
                                                        row = row,
                                                        onRemove = {
                                                            model.eventHandler.onEvent(
                                                                event = WatchlistUiModel.Event.Remove(
                                                                    symbol = row.symbol,
                                                                ),
                                                            )
                                                        },
                                                    )
                                                },
                                            )
                                        },
                                    )
                                },
                            )
                    }
                },
            )
        },
    )
}

// TODO: Modifier for each nested Composable
// TODO: maybe move each into its own file
@Composable
private fun DataModeChip(
    dataMode: MarketDataMode,
    isLiveAvailable: Boolean,
    onToggle: () -> Unit,
) {
    AssistChip(
        onClick = onToggle,
        enabled = isLiveAvailable,
        label = {
            Text(
                text =
                    when (dataMode) {
                        MarketDataMode.LIVE -> "Live"
                        MarketDataMode.DEMO -> "Demo"
                    },
            )
        },
        colors = when (dataMode) {
            MarketDataMode.LIVE -> AssistChipDefaults.assistChipColors()
            MarketDataMode.DEMO ->
                AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
        },
        modifier = Modifier.padding(end = 12.dp),
    )
}

@Composable
private fun ConnectionBanner(connectionState: ConnectionState) {
    if (connectionState == ConnectionState.CONNECTED) {
        return
    }
    Surface(
        color =
            when (connectionState) {
                ConnectionState.OFFLINE -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.secondaryContainer
            },
        modifier = Modifier.fillMaxWidth(),
        content = {
            Text(
                text = when (connectionState) {
                    ConnectionState.CONNECTING -> "Connecting to live prices…"
                    ConnectionState.RECONNECTING -> "Connection lost — reconnecting…"
                    ConnectionState.OFFLINE -> "Offline — retrying. Prices may be out of date."
                    ConnectionState.CONNECTED -> ""
                },
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        },
    )
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
        content = {
            CircularProgressIndicator()
        },
    )
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            Text(
                text = "Your watchlist is empty",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                // TODO: put these strings and others into strings.xml
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
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
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
                            when (row.movement) {
                                WatchlistRowUiModel.PriceMovement.UP -> {
                                    // TODO: is Text the best Composable for this?
                                    Text(
                                        text = "▲",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GainColor,
                                        modifier = Modifier.padding(end = 4.dp),
                                    )
                                }
                                WatchlistRowUiModel.PriceMovement.DOWN -> {
                                    Text(
                                        text = "▼",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(end = 4.dp),
                                    )
                                }
                                null -> {}
                            }
                            Text(
                                text = row.price ?: "—",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (row.isStale || row.price == null) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        },
                    )
                    when {
                        row.isStale && row.price != null -> {
                            Text(
                                text = row.staleAsOf?.let { "stale · $it" } ?: "stale",
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
                                        true -> GainColor
                                        false -> MaterialTheme.colorScheme.error
                                        null -> MaterialTheme.colorScheme.onSurfaceVariant
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

// TODO: put into Colors file
private val GainColor = Color(color = 0xFF1B873B)

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
