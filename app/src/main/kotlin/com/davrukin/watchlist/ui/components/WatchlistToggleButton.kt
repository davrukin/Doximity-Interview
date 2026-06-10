package com.davrukin.watchlist.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.davrukin.watchlist.R
import com.davrukin.watchlist.ui.theme.WatchlistTheme

@Composable
fun WatchlistToggleButton(
    displaySymbol: String,
    isOnWatchlist: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        colors =
            IconButtonDefaults.iconButtonColors(
                contentColor =
                    if (isOnWatchlist) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            ),
        modifier = modifier,
    ) {
        Icon(
            imageVector =
                if (isOnWatchlist) {
                    Icons.Filled.Check
                } else {
                    Icons.Filled.Add
                },
            contentDescription =
                if (isOnWatchlist) {
                    stringResource(id = R.string.toggle_remove_from_watchlist, displaySymbol)
                } else {
                    stringResource(id = R.string.toggle_add_to_watchlist, displaySymbol)
                },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WatchlistToggleButtonOnPreview() {
    WatchlistTheme {
        WatchlistToggleButton(displaySymbol = "AAPL", isOnWatchlist = true, onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun WatchlistToggleButtonOffPreview() {
    WatchlistTheme {
        WatchlistToggleButton(displaySymbol = "AAPL", isOnWatchlist = false, onClick = {})
    }
}
