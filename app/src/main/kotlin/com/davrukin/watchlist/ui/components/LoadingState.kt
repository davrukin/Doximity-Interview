package com.davrukin.watchlist.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.davrukin.watchlist.ui.theme.WatchlistTheme

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
        content = {
            CircularProgressIndicator()
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun LoadingStatePreview() {
    WatchlistTheme {
        LoadingState()
    }
}
