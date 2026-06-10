package com.davrukin.watchlist.presentation.watchlist

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
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

private class WatchlistPreviewProvider : PreviewParameterProvider<WatchlistUiModel> {
    override val values: Sequence<WatchlistUiModel> =
        sequenceOf(
            previewModel(),
            previewModel().copy(connectionState = ConnectionState.RECONNECTING),
            previewModel().copy(connectionState = ConnectionState.OFFLINE),
            previewModel().copy(items = emptyList()),
            previewModel().copy(isLoading = true),
            previewModel().copy(isRefreshing = true),
        )

    private fun previewModel(): WatchlistUiModel {
        return WatchlistUiModel(
            items = listOf(
                WatchlistRowUiModel(
                    symbol = "AAPL",
                    displaySymbol = "AAPL",
                    description = "APPLE INC",
                    price = "228.40",
                    change = "+2.15 (0.95%)",
                    isGain = true,
                    isStale = false,
                ),
                WatchlistRowUiModel(
                    symbol = "BINANCE:BTCUSDT",
                    displaySymbol = "BTC/USDT",
                    description = "Binance BTCUSDT",
                    price = "104,250.00",
                    change = "-1,200.50 (1.14%)",
                    isGain = false,
                    isStale = true,
                    staleAsOf = "Jun 9, 8:05 PM",
                ),
            ),
            connectionState = ConnectionState.CONNECTED,
            isLoading = false,
            isRefreshing = false,
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
    WatchlistTheme {
        WatchlistScreen(model = model)
    }
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
