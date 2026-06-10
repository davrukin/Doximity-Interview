package com.davrukin.watchlist.presentation.watchlist

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.davrukin.watchlist.ui.theme.WatchlistTheme

@Composable
fun SortOrderChip(
    sortOrder: WatchlistUiModel.SortOrder,
    onCycle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = onCycle,
        label = {
            Text(
                text =
                    when (sortOrder) {
                        WatchlistUiModel.SortOrder.ADDED -> "Sort: Added"
                        WatchlistUiModel.SortOrder.SYMBOL -> "Sort: A–Z"
                        WatchlistUiModel.SortOrder.CHANGE -> "Sort: Δ%"
                    },
            )
        },
        modifier = modifier.padding(end = 8.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun SortOrderChipAddedPreview() {
    WatchlistTheme {
        SortOrderChip(sortOrder = WatchlistUiModel.SortOrder.ADDED, onCycle = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun SortOrderChipSymbolPreview() {
    WatchlistTheme {
        SortOrderChip(sortOrder = WatchlistUiModel.SortOrder.SYMBOL, onCycle = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun SortOrderChipChangePreview() {
    WatchlistTheme {
        SortOrderChip(sortOrder = WatchlistUiModel.SortOrder.CHANGE, onCycle = {})
    }
}
