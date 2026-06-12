package com.davrukin.watchlist.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.davrukin.watchlist.R
import com.davrukin.watchlist.ui.theme.WatchlistTheme

@Composable
fun ErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            Text(
                text = stringResource(id = R.string.search_error_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(id = R.string.search_error_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.padding(top = 12.dp),
                content = {
                    Text(
                        text = stringResource(id = R.string.search_retry),
                    )
                },
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun ErrorStatePreview() {
    WatchlistTheme {
        ErrorState(onRetry = {})
    }
}
