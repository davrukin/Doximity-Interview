package com.davrukin.watchlist.presentation.search

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.davrukin.watchlist.domain.model.Instrument
import com.davrukin.watchlist.domain.model.InstrumentType
import com.davrukin.watchlist.ui.components.CenteredMessage
import com.davrukin.watchlist.ui.components.ErrorState
import com.davrukin.watchlist.ui.components.LoadingState
import com.davrukin.watchlist.ui.theme.WatchlistTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    model: SearchUiModel,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add to watchlist",
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            model.eventHandler.onEvent(
                                event = SearchUiModel.Event.Back,
                            )
                        },
                        content = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        },
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
                    OutlinedTextField(
                        value = model.query,
                        onValueChange = { query: String ->
                            model.eventHandler.onEvent(
                                event = SearchUiModel.Event.QueryChanged(query = query),
                            )
                        },
                        placeholder = {
                            Text(
                                text = "Symbol or name, e.g. AAPL or BTC",
                            )
                        },
                        singleLine = true,
                        trailingIcon = {
                            if (model.query.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        model.eventHandler.onEvent(
                                            event = SearchUiModel.Event.QueryChanged(query = ""),
                                        )
                                    },
                                    content = {
                                        Icon(
                                            imageVector = Icons.Filled.Clear,
                                            contentDescription = "Clear query",
                                        )
                                    },
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    when (model.phase) {
                        SearchUiModel.Phase.IDLE -> {
                            CenteredMessage(
                                title = "Search stocks and crypto",
                                subtitle = "Results appear as you type",
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        SearchUiModel.Phase.LOADING -> {
                            LoadingState(modifier = Modifier.fillMaxSize())
                        }

                        SearchUiModel.Phase.EMPTY -> {
                            CenteredMessage(
                                title = "No matches for \"${model.query}\"",
                                subtitle = "Try a different symbol or name",
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        SearchUiModel.Phase.ERROR -> {
                            ErrorState(
                                onRetry = {
                                    model.eventHandler.onEvent(event = SearchUiModel.Event.Retry)
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        SearchUiModel.Phase.RESULTS -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                content = {
                                    items(
                                        items = model.results,
                                        key = { result: SearchUiModel.Result ->
                                            result.instrument.symbol
                                        },
                                        itemContent = { result: SearchUiModel.Result ->
                                            SearchResultRow(
                                                result = result,
                                                onToggle = {
                                                    model.eventHandler.onEvent(
                                                        event = SearchUiModel.Event.ToggleWatchlist(
                                                            instrument = result.instrument,
                                                        ),
                                                    )
                                                },
                                                modifier = Modifier.fillMaxWidth(),
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

private class SearchPreviewProvider : PreviewParameterProvider<SearchUiModel> {
    override val values: Sequence<SearchUiModel> =
        sequenceOf(
            previewModel(),
            previewModel().copy(query = "", results = emptyList(), phase = SearchUiModel.Phase.IDLE),
            previewModel().copy(results = emptyList(), phase = SearchUiModel.Phase.LOADING),
            previewModel().copy(query = "zzz", results = emptyList(), phase = SearchUiModel.Phase.EMPTY),
            previewModel().copy(results = emptyList(), phase = SearchUiModel.Phase.ERROR),
        )

    private fun previewModel(): SearchUiModel {
        return SearchUiModel(
            query = "ap",
            results = listOf(
                SearchUiModel.Result(
                    instrument = Instrument(
                        symbol = "AAPL",
                        displaySymbol = "AAPL",
                        description = "APPLE INC",
                        type = InstrumentType.STOCK,
                    ),
                    isOnWatchlist = true,
                ),
                SearchUiModel.Result(
                    instrument = Instrument(
                        symbol = "BINANCE:APTUSDT",
                        displaySymbol = "APT/USDT",
                        description = "Binance APTUSDT",
                        type = InstrumentType.CRYPTO,
                    ),
                    isOnWatchlist = false,
                ),
            ),
            phase = SearchUiModel.Phase.RESULTS,
            eventHandler = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchScreenPreview(
    @PreviewParameter(provider = SearchPreviewProvider::class) model: SearchUiModel,
) {
    WatchlistTheme {
        SearchScreen(model = model)
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SearchScreenDarkPreview() {
    WatchlistTheme(
        dynamicColor = false,
        content = {
            SearchScreen(model = SearchPreviewProvider().values.first())
        },
    )
}
