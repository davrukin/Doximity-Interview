package com.davrukin.watchlist.data.remote

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class OkHttpPriceSocket(
    private val client: OkHttpClient,
    private val url: String,
) : PriceSocket {
    override fun connect(): Flow<PriceSocketEvent> =
        callbackFlow {
            val listener = object : WebSocketListener() {
                override fun onOpen(
                    webSocket: WebSocket,
                    response: Response,
                ) {
                    trySendBlocking(
                        element = PriceSocketEvent.Opened(
                            session = { text ->
                                webSocket.send(text)
                            },
                        ),
                    )
                }

                override fun onMessage(
                    webSocket: WebSocket,
                    text: String,
                ) {
                    trySendBlocking(
                        element = PriceSocketEvent.MessageReceived(text = text),
                    )
                }

                override fun onFailure(
                    webSocket: WebSocket,
                    t: Throwable,
                    response: Response?,
                ) {
                    trySendBlocking(
                        element = PriceSocketEvent.Failed(cause = t),
                    )
                    close()
                }

                override fun onClosed(
                    webSocket: WebSocket,
                    code: Int,
                    reason: String,
                ) {
                    close()
                }
            }
            val webSocket = client.newWebSocket(
                request = Request.Builder().url(url).build(),
                listener = listener,
            )
            awaitClose {
                webSocket.cancel()
            }
        }
}
