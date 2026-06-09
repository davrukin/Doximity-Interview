package com.davrukin.watchlist.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface FinnhubApi {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
    ): SearchResponseDto

    @GET("quote")
    suspend fun quote(
        @Query("symbol") symbol: String,
    ): QuoteDto

    @GET("crypto/symbol")
    suspend fun cryptoSymbols(
        @Query("exchange") exchange: String,
    ): List<CryptoSymbolDto>

    companion object {
        const val BASE_URL = "https://finnhub.io/api/v1/"
    }
}
