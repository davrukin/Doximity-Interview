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
  down shortly after the UI stops observing. No background battery drain, no lifecycle plumbing.
- **Rotation-survivable UI state** (the search query) uses `rememberSaveable`; everything else is
  derived from persistent or remote state and simply re-collects.

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
- **Staleness**: a quote is stale when it comes from the Room cache and has not been refreshed
  this session, or when the connection is down — never merely because a market is quiet
  (US stocks legitimately stop ticking after hours while connected).
- **Cache**: fresh quotes are persisted (throttled to 5s) so the next launch shows last-known
  prices immediately, marked stale until refreshed. Demo prices are never persisted.

## Demo mode

Both the live Finnhub stack and a demo stack implement one `MarketDataSource` contract (search,
snapshot, price stream). With no API key configured the app locks to demo mode; with a key, a chip
in the top bar toggles modes at runtime and repositories swap sources reactively. The demo stream
is a random walk anchored to a canned catalog and periodically simulates a brief connection blip
so the reconnecting state is observable without touching the network.

## Testing

- **Presenters**: `moleculeFlow { presenter.present(params) }` + Turbine, asserting emitted
  `UiModel`s and event handling against in-memory fakes.
- **Reconnect policy**: `ReconnectingPriceStream` is a plain class over a `PriceSocket` interface,
  tested with a scripted fake socket and virtual time (drop → reconnect → resubscribe → offline
  after repeated failures).
- **Quote merging**: snapshot/tick merge, reconnect refetch, and live-only persistence in
  `PriceRepositoryImpl`, with virtual time for the persistence throttle.
- **Room**: DAO tests run on the JVM via Robolectric, including the conflict strategy that
  preserves cached quotes on re-insert.

## AI usage

This project was built with Claude (Anthropic) as a pair-programming agent, directed and reviewed
by me. The split:

- **Mine**: all product and architecture decisions — adopting the Composable Presenter pattern,
  Koin, Room, Navigation 3, the staleness semantics, the demo-mode design, requirement
  traceability workflow (REQUIREMENTS.md / PLAN.md), and final review of every commit.
- **AI-generated under those constraints**: the bulk of the implementation code, tests, and these
  documents, produced incrementally against the conventions in [CLAUDE.md](CLAUDE.md) with the
  build (`ktlintCheck assembleDebug testDebugUnitTest`) green before each commit, plus an
  on-device smoke test of the full search → add → live-tick → relaunch flow.

The pattern itself is reproduced from the two Doximity engineering blog posts cited above.
