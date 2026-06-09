package com.davrukin.watchlist.data.remote

import okhttp3.Interceptor
import okhttp3.Response

class FinnhubAuthInterceptor(
    private val apiKey: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request =
            chain
                .request()
                .newBuilder()
                .header(name = "X-Finnhub-Token", value = apiKey)
                .build()
        return chain.proceed(request)
    }
}
