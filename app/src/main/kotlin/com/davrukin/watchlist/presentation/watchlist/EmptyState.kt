package com.davrukin.watchlist.presentation.watchlist

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.davrukin.watchlist.ui.components.CenteredMessage
import com.davrukin.watchlist.ui.theme.WatchlistTheme

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    CenteredMessage(
        title = "Your watchlist is empty",
        subtitle = "Tap + to search for stocks and crypto",
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    WatchlistTheme {
        EmptyState()
    }
}
