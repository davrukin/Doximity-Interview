# Requirements Traceability

Requirements distilled from [INSTRUCTIONS.RTF](INSTRUCTIONS.RTF). Each row is updated as work lands;
commit messages reference the IDs they advance (e.g. `Req: R4, R5`).

Status legend: ⬜ planned · 🔄 in progress · ✅ done

## Required

| ID  | Requirement | Implementation | Tests | Status |
|-----|-------------|----------------|-------|--------|
| R1  | Search for instruments via REST (stocks + crypto) | `data/remote/FinnhubApi`, `LiveMarketDataSource`, `SearchPresenter` | `SearchPresenterTest`, `DemoMarketDataSourceTest` | ✅ |
| R2  | Add and remove instruments from the watchlist | `WatchlistRepositoryImpl`, `WatchlistPresenter` (remove), `SearchPresenter` (toggle) | `WatchlistPresenterTest`, `SearchPresenterTest`, `WatchlistDaoTest`; verified on device | ✅ |
| R3  | Latest known price per watchlist item (REST snapshot) | `data/remote/FinnhubApi.quote`, `QuoteDto.toQuote`, `PriceRepositoryImpl` | `QuoteDtoTest`, `PriceRepositoryImplTest` | ✅ |
| R4  | Live price updates via WebSocket while the app runs | `data/remote/OkHttpPriceSocket`, `data/stream/ReconnectingPriceStream`, `data/PriceRepositoryImpl` | `ReconnectingPriceStreamTest`, `PriceRepositoryImplTest` | ✅ |
| R5  | Loading, empty, error, missing-price, stale, network-loss, and reconnecting states | `WatchlistUiModel`/`WatchlistRowUiModel` + `WatchlistScreen` banners, `SearchUiModel.Phase` + `SearchScreen` | `WatchlistPresenterTest`, `SearchPresenterTest`, `ReconnectingPriceStreamTest` | ✅ |
| R6  | Watchlist persists across app launches (Room) | `data/local/` (`WatchlistDatabase`, `WatchlistDao`, `WatchlistItemEntity`) | `WatchlistDaoTest`; verified across relaunch on device | ✅ |
| R7  | Documented demo/fake-data mode (auto when no API key, runtime toggle) | `data/demo/`, `MarketDataModeRepositoryImpl`, `MarketDataSelector`; documented in README | `DemoMarketDataSourceTest`, `DemoPriceStreamSourceTest`, `MarketDataModeRepositoryImplTest` | ✅ |
| R8  | Screen state exposed safely for Compose observation (UDF, immutable models) | `presentation/core/`, `WatchlistPresenter`, `SearchPresenter` | `WatchlistPresenterTest`, `SearchPresenterTest` (Molecule + Turbine) | ✅ |
| R9  | Dependency injection (Koin) | `app/WatchlistApplication`, `app/di/` (4 modules) | exercised by all presenter/data tests | ✅ |
| R10 | Relevant unit tests | `app/src/test/` (35 tests: data layer + presenters) | `./gradlew testDebugUnitTest` | ✅ |
| R11 | README with setup, architecture notes, tradeoffs, and AI/tooling assistance | `README.md`, `ARCHITECTURE.md` (incl. AI usage section) | — | ✅ |

## Optional enhancements (tackled only after all required rows are ✅)

| ID | Enhancement | Implementation | Tests | Status |
|----|-------------|----------------|-------|--------|
| O1 | Price movement indicators | `WatchlistItemPresenter` (tick direction), `WatchlistScreen` (▲/▼) | `WatchlistPresenterTest` | ✅ |
| O2 | Sorting the watchlist | `WatchlistPresenter.sortItems`, `SortOrderChip` (added / A–Z / day-change) | `WatchlistPresenterTest` | ✅ |
| O3 | Sparkline | — | — | ⬜ |
| O4 | Pull to refresh | `PullToRefreshBox` on the watchlist; `PriceRepository.refreshQuotes` re-fetches snapshots | `WatchlistPresenterTest` | ✅ |
| O5 | Offline cache display | cached quotes render immediately on launch (R6 design); stale label now shows last-updated time | `WatchlistPresenterTest`, `WatchlistDaoTest` | ✅ |
| O6 | Compose UI tests | `app/src/androidTest/` (10 tests) | `./gradlew connectedDebugAndroidTest`; green on device | ✅ |
| O7 | Advanced retry/backoff (exponential backoff with jitter) | `ReconnectingPriceStream.retryDelayFor` (1s base, x2, 30s cap, ±20% jitter) | `ReconnectingPriceStreamTest` | ✅ |
| O8 | Custom Design System components (developer addition; not in initial brief) | `ui/components/DesignSystem.kt`, `PriceChip`, `WatchlistToggleButton` | — | ✅ |
