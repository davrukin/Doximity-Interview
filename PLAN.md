# Development Plan

A living document: updated in the same commit as the work it tracks, so the history shows how the
plan evolved. Requirement IDs refer to [REQUIREMENTS.md](REQUIREMENTS.md).

## Product

Real-time price watchlist on [Finnhub](https://finnhub.io/docs/api): REST for instrument search and
quote snapshots, WebSocket for live trades. US stocks + crypto (crypto trades 24/7, so live updates
are observable outside US market hours). Demo mode runs the full experience with generated data when
no API key is configured, and can be toggled at runtime.

## Architecture

Composable Presenter pattern (see [ARCHITECTURE.md](ARCHITECTURE.md) once written): presenters are
`@Composable` functions returning immutable `UiModel`s; no ViewModels; Koin for injection; Room for
persistence; Navigation 3 with an owned back stack. Conventions in [CLAUDE.md](CLAUDE.md).

Key design points:

- **Staleness**: a price is stale when it comes from cache and hasn't been refreshed this session,
  or when the stream connection is down — never merely because the market is quiet.
- **Socket lifecycle**: tick/connection flows are shared with `WhileSubscribed`, so the socket lives
  while the UI observes it and is torn down shortly after — no background drain, survives rotation.
- **Reconnect protocol**: on reconnect, resubscribe all symbols and re-fetch REST snapshots to cover
  ticks missed while disconnected.
- **Demo isolation**: demo prices are never written to the persistent quote cache.

## Phases

### Phase 0 — docs first
- [x] REQUIREMENTS.md + PLAN.md committed alongside the assignment brief
- [x] CLAUDE.md project workflow section

### Phase 1 — required scope
- [x] Scaffold: Gradle 9.5.1, AGP 9.2.1, Kotlin 2.4.0, version catalog, ktlint
- [x] Presenter core: `Presenter`, `UiModel`, `UiEvent`, `EventHandler`, `launchUseCase` (R8)
- [x] Koin application setup + app scope + injected dispatchers/clock (R9)
- [x] Material 3 theme
- [x] Domain models, repository interfaces, use cases
- [x] Room watchlist persistence with cached last quotes (R6)
- [x] Finnhub REST client + mappers (R1, R3)
- [x] WebSocket price stream with basic reconnect (R4, R5)
- [x] Demo-mode data sources (R7)
- [x] Repositories with live/demo switching (R7)
- [x] Data layer tests (R10)
- [ ] Watchlist presenter + tests (R2–R5, R8, R10)
- [ ] Search presenter + tests (R1, R2, R8, R10)
- [ ] Watchlist screen + previews (R5)
- [ ] Search screen + previews (R1, R5)
- [ ] Navigation 3 wiring + MainActivity
- [ ] README + ARCHITECTURE.md (R11)

### Phase 2 — optional enhancements (in priority order)
- [ ] O1 price movement indicators
- [ ] O6 Compose UI tests
- [ ] O7 exponential backoff + jitter
- [ ] O5 offline cache display surfacing
- [ ] O4 pull to refresh
- [ ] O2 sorting
- [ ] O3 sparkline

## Deviations log

| Date | Change | Why |
|------|--------|-----|
| 2026-06-09 | No standalone Kotlin Android plugin | AGP 9 ships built-in Kotlin support and rejects `org.jetbrains.kotlin.android` |
| 2026-06-09 | compileSdk 37 (target stays 36) | Current androidx releases require compiling against android-37 |
