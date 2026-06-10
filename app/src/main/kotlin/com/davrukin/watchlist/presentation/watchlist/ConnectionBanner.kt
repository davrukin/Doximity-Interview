package com.davrukin.watchlist.presentation.watchlist

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.davrukin.watchlist.R
import com.davrukin.watchlist.domain.model.ConnectionState
import com.davrukin.watchlist.ui.theme.WatchlistTheme

@Composable
fun ConnectionBanner(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier,
) {
    if (connectionState == ConnectionState.CONNECTED) {
        return
    }
    Surface(
        color =
            when (connectionState) {
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
                text =
                    when (connectionState) {
                        ConnectionState.CONNECTING -> {
                            stringResource(id = R.string.connection_connecting)
                        }

                        ConnectionState.RECONNECTING -> {
                            stringResource(id = R.string.connection_reconnecting)
                        }

                        ConnectionState.OFFLINE -> {
                            stringResource(id = R.string.connection_offline)
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

@Preview(showBackground = true)
@Composable
private fun ConnectionBannerConnectingPreview() {
    WatchlistTheme {
        ConnectionBanner(connectionState = ConnectionState.CONNECTING)
    }
}

@Preview(showBackground = true)
@Composable
private fun ConnectionBannerOfflinePreview() {
    WatchlistTheme {
        ConnectionBanner(connectionState = ConnectionState.OFFLINE)
    }
}
