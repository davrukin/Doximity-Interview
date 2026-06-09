# Requirements Traceability

Requirements distilled from [INSTRUCTIONS.RTF](INSTRUCTIONS.RTF). Each row is updated as work lands;
commit messages reference the IDs they advance (e.g. `Req: R4, R5`).

Status legend: ⬜ planned · 🔄 in progress · ✅ done

## Required

| ID  | Requirement | Implementation | Tests | Status |
|-----|-------------|----------------|-------|--------|
| R1  | Search for instruments via REST (stocks + crypto) | `data/remote/FinnhubApi` (`/search`, `/crypto/symbol`) | mapper tests (pending) | 🔄 |
| R2  | Add and remove instruments from the watchlist | — | — | ⬜ |
| R3  | Latest known price per watchlist item (REST snapshot) | `data/remote/FinnhubApi.quote`, `QuoteDto.toQuote` | mapper tests (pending) | 🔄 |
| R4  | Live price updates via WebSocket while the app runs | `data/remote/OkHttpPriceSocket`, `data/stream/ReconnectingPriceStream`, `data/PriceRepositoryImpl` | stream + repository tests (pending) | 🔄 |
| R5  | Loading, empty, error, missing-price, stale, network-loss, and reconnecting states | — | — | ⬜ |
| R6  | Watchlist persists across app launches (Room) | `data/local/` (`WatchlistDatabase`, `WatchlistDao`, `WatchlistItemEntity`) | `WatchlistDaoTest` (pending) | 🔄 |
| R7  | Documented demo/fake-data mode (auto when no API key, runtime toggle) | `data/demo/`, `MarketDataModeRepositoryImpl`, `MarketDataSelector` | demo source tests (pending) | 🔄 |
| R8  | Screen state exposed safely for Compose observation (UDF, immutable models) | `presentation/core/` (`Presenter`, `UiModel`, `UiEvent`, `EventHandler`, `launchUseCase`) | via presenter tests | 🔄 |
| R9  | Dependency injection (Koin) | `app/WatchlistApplication`, `app/di/AppModule` | exercised by all presenter/data tests | 🔄 |
| R10 | Relevant unit tests | — | — | ⬜ |
| R11 | README with setup, architecture notes, tradeoffs, and AI/tooling assistance | — | — | ⬜ |

## Optional enhancements (tackled only after all required rows are ✅)

| ID | Enhancement | Implementation | Tests | Status |
|----|-------------|----------------|-------|--------|
| O1 | Price movement indicators | — | — | ⬜ |
| O2 | Sorting the watchlist | — | — | ⬜ |
| O3 | Sparkline | — | — | ⬜ |
| O4 | Pull to refresh | — | — | ⬜ |
| O5 | Offline cache display | — | — | ⬜ |
| O6 | Compose UI tests | — | — | ⬜ |
| O7 | Advanced retry/backoff (exponential backoff with jitter) | — | — | ⬜ |
