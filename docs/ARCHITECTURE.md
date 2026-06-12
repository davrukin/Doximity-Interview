# Architecture

## Composable Presenter

This app uses the Composable Presenter pattern from Doximity's engineering blog
([Part 1: Simplifying State Management with Compose](https://technology.doximity.com/articles/simplifying-state-management-with-compose),
[Part 2: Building a Note-Taking App in Compose](https://technology.doximity.com/articles/building-a-note-taking-app-in-compose)).

A presenter implements `Presenter<Model : UiModel, Params>` with a single
`@Composable fun present(params): Model` containing presentation logic only — no UI. State lives
in the presenter via `remember`/`rememberSaveable`; data-layer flows are collected once per
composition with `launchUseCase` (a `produceState` wrapper). The returned `UiModel` is an
`@Immutable` data class carrying a single `EventHandler<E : UiEvent>`; the UI dispatches typed
events from a sealed interface nested in the model, and the presenter handles them in one `when`.

Presenters compose hierarchically: `WatchlistPresenter` invokes a `WatchlistItemPresenter` per row
inside `key(symbol)`, mirroring the note-item composition in Part 2. Event handlers are
`remember`ed so the same instance is reused across recompositions — otherwise every produced model
would compare unequal and Compose would re-render the whole screen on every price tick instead of
skipping unchanged rows.

Screens are pure renderers (`WatchlistScreen(model)`, `SearchScreen(model)`): state flows down,
events flow up, and every meaningful state has a `@Preview` driven by a
`PreviewParameterProvider`.

## No ViewModels — and what replaces them

The pattern deliberately omits ViewModels. The two things a ViewModel normally provides are
handled explicitly:

- **Work that must outlive composition** (e.g. removing a watchlist row mid-navigation) runs on an
  application-scoped `CoroutineScope` provided by Koin.
- **The socket lifecycle** is owned by the data layer: tick/connection flows are shared with
  `shareIn(appScope, WhileSubscribed(5s))`, so the WebSocket connects when the UI starts
  collecting, survives configuration changes (the 5s linger bridges the recreation), and tears
  down shortly after the UI stops observing. `PriceRepositoryImpl` additionally gates the upstream
  on `ProcessLifecycleOwner` foreground state, so the socket is also suspended while the app is
  in the background — no background battery or data drain.
- **Rotation-survivable UI state**: Search queries use `rememberSaveable`. The individual row state
  in `WatchlistItemPresenter` (sparkline buffer, previous price, and movement direction) also uses
  `rememberSaveable` (with a custom `DoubleArray` Saver for the buffer to prevent boxing overhead) so
  the chart history is preserved across rotations and backstack navigation, with deduplication checks
  to prevent drawing flat baselines on re-entry.

The tradeoff: state held in plain `remember` does not survive configuration changes. Here that
costs one in-flight search (re-triggered automatically by the saved query) — an acceptable price
for presenters that are plain classes testable with Molecule and Turbine on the JVM.

## Data flow

```
Finnhub REST ──snapshot──┐
                         ├──> PriceRepository ──Map<symbol, Quote>──┐
Finnhub WS  ──ticks──────┘         │                                ├──> WatchlistPresenter ──UiModel──> WatchlistScreen
   (single shared socket)          │ connection state ──────────────┤         │
                                   │                                │   WatchlistItemPresenter (per row)
Room (watchlist + cached quotes) ──┴──> WatchlistRepository ────────┘
```

- **Snapshot ⊕ ticks**: REST quotes seed prices and previous-close baselines; streamed trades
  update prices and recompute day change against those baselines.
- **Reconnect protocol**: after every reconnect the snapshots are re-fetched (covering ticks
  missed while down) and all symbols are resubscribed.
- **Deduplicated subscriptions**: `ObserveQuotesUseCase` applies `.distinctUntilChanged()` to the
  watchlist flow. This ensures that database writes (persisting ticks every 5s) do not restart the
  `flatMapLatest` quotes stream when the set of watched symbols is unchanged, preventing redundant
  socket reconnection overhead and flashing "Stale" labels.
- **Staleness**: a quote is stale when it comes from the Room cache and has not been refreshed
  this session, or when the connection is down — never merely because a market is quiet
  (US stocks legitimately stop ticking after hours while connected).
- **Cache**: fresh quotes are persisted (throttled to 5s) so the next launch shows last-known
  prices immediately, marked stale until refreshed. Demo prices are never persisted.

## Engineering Standards & Performance

To optimize for maintainability, debuggability, and runtime performance, the project follows
these strict engineering standards:

- **Performance**: Floating-point primitives (prices, changes) use `Double.NaN` instead of
  nullable types. This prevents JVM object wrapping (boxing) overhead, which is critical in a
  high-frequency data application.
- **Maintainability**: All functions use block bodies (`{ return ... }`) instead of expression
  bodies. This provides clear entry/exit points for debuggers and improves readability as
  complexity grows.
- **Safety**: The non-null assertion operator (`!!`) is strictly prohibited. All if/else
  statements require braces to prevent logic errors during refactoring.
- **Clarity**: Lambda parameters are explicitly named (avoiding `it`) to ensure the source
  and purpose of data are always obvious in nested scopes.

## Demo mode

Both the live Finnhub stack and a demo stack implement one `MarketDataSource` contract (search,
snapshot, price stream). With no API key configured the app locks to demo mode; with a key, a chip
in the top bar toggles modes at runtime and repositories swap sources reactively. The demo stream
is a random walk anchored to a canned catalog and periodically simulates a brief connection blip
so the reconnecting state is observable without touching the network.

## Testing

The project has **59 automated tests** (49 JVM Unit Tests + 10 Compose UI Tests) verifying components in isolation and end-to-end:

- **Presenters (21 tests)**: `moleculeFlow { presenter.present(params) }` + Turbine, asserting emitted `UiModel`s and event handling against in-memory fakes. Includes `WatchlistPresenterTest` and `WatchlistItemPresenterTest`.
- **Domain / Use Cases (1 test)**: `ObserveQuotesUseCaseTest` verifying distinctUntilChanged stream-deduplication to block redundant WebSocket restarts during DB updates.
- **Reconnect policy (6 tests)**: `ReconnectingPriceStream` is a plain class over a `PriceSocket` interface, tested with a scripted fake socket and virtual time (drop → reconnect → resubscribe → offline after repeated failures).
- **Quote merging (4 tests)**: snapshot/tick merge, reconnect refetch, and live-only persistence in `PriceRepositoryImpl`, with virtual time for the persistence throttle.
- **Room DB (4 tests)**: DAO tests run on the JVM via Robolectric, including the conflict strategy that preserves cached quotes on re-insert.
- **Compose UI (10 tests)**: Testing layout rendering, navigation, and user-initiated state changes (e.g. dialog interactions) on-device or emulator.

## AI usage

This project was built with AI pair-programming agents — Claude (Anthropic) for the large
majority, plus a brief session with Gemini CLI (Google) — directed and reviewed by me; each
commit names its agent in a `Co-Authored-By` trailer. Commit breakdown (run
`python3 scripts/authorCount.py`):

| Person            | Author | Co-author |
|-------------------|-------:|----------:|
| Daniel Avrukin    |     82 |         — |
| Claude Fable 5    |      — |        50 |
| Gemini CLI †      |     12 |        21 |
| Claude Sonnet 4.6 |      — |         7 |
| Gemini 3.5 Flash  |      — |         5 |
| Gemini 3.1 Pro    |      — |         2 |
| Claude Opus 4.6   |      — |         1 |

† Gemini CLI can commit directly via its own tooling and did not always observe the
`Co-Authored-By` convention — hence the 12 primary-author commits. Claude always
appears as co-author only.

The split:

- **Mine**: all product and architecture decisions — adopting the Composable Presenter pattern,
  Koin, Room, Navigation 3, the staleness semantics, the demo-mode design, requirement
  traceability workflow (REQUIREMENTS.md / PLAN.md), and final review of every commit.
- **AI-generated under those constraints**: the bulk of the implementation code, tests, and these
  documents, produced incrementally against the conventions in [CLAUDE.md](../CLAUDE.md) with the
  build (`ktlintCheck assembleDebug testDebugUnitTest`) green before each commit, plus an
  on-device smoke test of the full search → add → live-tick → relaunch flow. Cross-review between
  agents was part of the workflow: a regression introduced in one session (NaN sentinels meeting
  SQLite's NaN→NULL conversion, which silently broke watchlist persistence) was caught and fixed
  in review, with the root cause documented in PLAN.md's deviations log.

The pattern itself is reproduced from the two Doximity engineering blog posts cited above.
