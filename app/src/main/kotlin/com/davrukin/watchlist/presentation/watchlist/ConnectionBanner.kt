package com.davrukin.watchlist.presentation.watchlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.davrukin.watchlist.R
import com.davrukin.watchlist.domain.model.ConnectionState
import com.davrukin.watchlist.ui.theme.MotionTokens
import com.davrukin.watchlist.ui.theme.WatchlistTheme

@Composable
fun ConnectionBanner(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier,
) {
    // The last problem state keeps rendering while the banner animates out after reconnecting.
    var displayedState: ConnectionState by remember { mutableStateOf(value = connectionState) }
    LaunchedEffect(key1 = connectionState) {
        if (connectionState != ConnectionState.CONNECTED) {
            displayedState = connectionState
        }
    }
    AnimatedVisibility(
        visible = connectionState != ConnectionState.CONNECTED,
        enter =
            expandVertically(animationSpec = tween(durationMillis = MotionTokens.STANDARD_MILLIS)) +
                fadeIn(animationSpec = tween(durationMillis = MotionTokens.STANDARD_MILLIS)),
        exit =
            shrinkVertically(animationSpec = tween(durationMillis = MotionTokens.STANDARD_MILLIS)) +
                fadeOut(animationSpec = tween(durationMillis = MotionTokens.QUICK_MILLIS)),
        modifier = modifier,
        content = {
            Surface(
                color =
                    when (displayedState) {
                        ConnectionState.OFFLINE -> {
                            MaterialTheme.colorScheme.errorContainer
                        }

                        else -> {
                            MaterialTheme.colorScheme.secondaryContainer
                        }
                    },
                modifier = Modifier.fillMaxWidth(),
                content = {
                    Text(
                        text =
                            when (displayedState) {
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
