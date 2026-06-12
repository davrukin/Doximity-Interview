# Watchlist

A real-time financial watchlist for Android: search US stocks and crypto (Finnhub), add them to a
persisted watchlist, and watch prices update live over WebSocket — with a full demo mode that runs
the entire experience on generated data.

Built with Kotlin, Jetpack Compose, the
[Composable Presenter pattern](docs/ARCHITECTURE.md) (no ViewModels), Koin, Room, Navigation 3,
Retrofit/OkHttp, and kotlinx.serialization.

**About this submission:** I used AI pair-programming throughout (Claude & Gemini; see
[ARCHITECTURE.md](docs/ARCHITECTURE.md) for the full disclosure and what decisions were mine).
Required scope (R1–R11) landed first; optional enhancements came only after all requirements were
met. Two features — after-hours market-session pricing and swipe-to-remove with undo — were
designed and deliberately cut when breadth began to outweigh explainability; both designs are
preserved in [PLAN.md](docs/PLAN.md). The supporting docs (REQUIREMENTS.md, PLAN.md) exist because
traceability is how I kept AI-generated work auditable, not because they took long to produce.

## Setup

Requirements: JDK 17+, Android SDK (compileSdk 37), a device or emulator on API 26+.

```bash
git clone <this repo>
# Optional — for live data, add a free Finnhub key (https://finnhub.io/register):
echo "FINNHUB_API_KEY=your_key_here" >> local.properties

# Configure the local git hooks path (also runs automatically during Gradle builds):
git config core.hooksPath scripts

./gradlew installDebug
```

The API key is read from `local.properties` into `BuildConfig` and is never committed.

## Demo mode (no API key needed)

**With no API key configured the app runs entirely in demo mode** — reviewers can exercise the
full experience without external service availability, market hours, or rate limits:

- Search serves a canned catalog of stocks and crypto pairs (try `apple`, `btc`, `tsla`).
- Prices tick from a random walk anchored to realistic base prices.
- The stream periodically simulates a brief connection blip, so the reconnecting banner and stale
  treatment are observable on demand.
- Demo prices are never written to the persistent quote cache.

With a key configured, the app starts in live mode and the **Live/Demo chip** in the top bar
toggles modes at runtime. Crypto pairs (Binance) tick 24/7 in live mode, so real-time updates are
visible even outside US market hours.

## State handling & Polish

Loading (initial watchlist read), empty (watchlist and search), error with retry (search), missing
price (em dash, e.g. crypto before its first tick), stale (cached or disconnected values, labeled),
network loss and reconnecting (banner; automatic resubscribe + snapshot re-fetch on recovery).
Day change renders green/red against the previous close.

**UI Polish:** Price movement indicators (▲/▼), per-row sparklines, themed gain/loss colors with
dark-mode palettes, animated banner/list/state transitions, and a deletion confirmation dialog.

**Data Integrity:** The repository layer includes defensive pre-validation and row ID verification
to catch SQLite constraint violations (like `NaN` mapping) and throw loudly during development
rather than failing silently.

## Tests

```bash
./gradlew ktlintCheck detekt assembleDebug testDebugUnitTest   # full gate (49 JVM unit tests)
./gradlew connectedDebugAndroidTest                            # Compose UI tests (10 tests, device/emulator)
```

The project has **59 automated tests** providing extensive coverage across layers:
- **49 JVM Unit Tests**:
  - Presenter behavior via Molecule + Turbine (loading/content/error/retry, add/remove, debounce, mode toggling)
  - WebSocket reconnect/resubscribe/offline policy with a fake socket and virtual time; snapshot/tick merging and background pause/resume
  - Room DB DAO via Robolectric; and serialization.
- **10 Compose UI Tests**:
  - Live layout verification, screen rendering, dialog flows, and navigation transitions.

Detailed manual scenarios and the automated test breakdown are located in the [Testing Guide](docs/TESTING.md).

## Assumptions & limitations

- **Market hours**: US stock trades only stream while the market is open; quotes still load via
  REST and the UI marks values stale only on cache/disconnect, not on a quiet market. Demo mode
  (or crypto symbols) demonstrates live ticking at any hour.
- **Crypto snapshots**: Finnhub has no REST quote for crypto; crypto rows show "—" until the first
  WebSocket tick (instant on liquid Binance pairs), and day change requires a snapshot baseline so
  it appears for stocks (and everything in demo mode) only.
- **Free-tier limits**: 60 REST calls/min and ~50 WebSocket symbols — comfortably above this app's
  needs; stock `/search` may return non-US listings whose quotes 403 on the free tier, which
  surface as the missing-price state.
- **Scope cuts** are tracked in [PLAN.md](docs/PLAN.md) and [REQUIREMENTS.md](docs/REQUIREMENTS.md),
  which
  also map every assignment requirement to its implementation and tests.

## Documents

- [ARCHITECTURE.md](docs/ARCHITECTURE.md) — pattern rationale, data flow, tradeoffs, **AI usage**.
- [REQUIREMENTS.md](docs/REQUIREMENTS.md) — requirement → implementation → test traceability.
- [PLAN.md](docs/PLAN.md) — the living development plan the commit history follows.
- [TESTING.md](docs/TESTING.md) — manual test scenarios with expected results.
- [CLAUDE.md](CLAUDE.md) — engineering conventions all code in this repo follows.
