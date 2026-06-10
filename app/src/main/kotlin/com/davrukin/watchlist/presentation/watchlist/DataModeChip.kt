package com.davrukin.watchlist.presentation.watchlist

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.davrukin.watchlist.domain.model.MarketDataMode
import com.davrukin.watchlist.ui.theme.WatchlistTheme

@Composable
fun DataModeChip(
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

@Preview(showBackground = true)
@Composable
private fun DataModeChipLivePreview() {
    WatchlistTheme {
        DataModeChip(
            dataMode = MarketDataMode.LIVE,
            isLiveAvailable = true,
            onToggle = {},
            modifier = Modifier.padding(8.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DataModeChipDemoPreview() {
    WatchlistTheme {
        DataModeChip(
            dataMode = MarketDataMode.DEMO,
            isLiveAvailable = true,
            onToggle = {},
            modifier = Modifier.padding(8.dp),
        )
    }
}
