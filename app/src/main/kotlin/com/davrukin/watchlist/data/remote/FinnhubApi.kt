package com.davrukin.watchlist.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface FinnhubApi {
    @GET(value = "search")
    suspend fun search(
        @Query("q") query: String,
    ): SearchResponseDto

    @GET(value = "quote")
    suspend fun quote(
        @Query("symbol") symbol: String,
    ): QuoteDto

    @GET(value = "crypto/symbol")
    suspend fun cryptoSymbols(
        @Query("exchange") exchange: String,
    ): List<CryptoSymbolDto>

    companion object {
        // TODO: is this the correct/best places for this? should it be remote config? built-in?
        const val BASE_URL: String = "https://finnhub.io/api/v1/"
    }
}
