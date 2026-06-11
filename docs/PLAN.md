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
persistence; Navigation 3 with an owned back stack. Conventions in [CLAUDE.md](../CLAUDE.md).

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
- [x] Watchlist presenter + tests (R2–R5, R8, R10)
- [x] Search presenter + tests (R1, R2, R8, R10)
- [x] Watchlist screen + previews (R5)
- [x] Search screen + previews (R1, R5)
- [x] Navigation 3 wiring + MainActivity
- [x] README + ARCHITECTURE.md (R11)

### Phase 2 — optional enhancements (in priority order)
- [x] O1 price movement indicators
- [x] O6 Compose UI tests
- [x] O7 exponential backoff + jitter
- [x] O5 offline cache display surfacing
- [x] O4 pull to refresh
- [x] O8 custom design system components (developer addition)
- [x] O2 sorting
- [x] O3 sparkline

### Phase 3 — hardening and developer additions
- [x] Compiler warnings resolved (safe call, v2 compose test rule, unused imports)
- [x] Defensive DB operations: check row ID and exists-on-fail to prevent silent SQLite drops
- [x] Defensive round-trip regression tests for the silent-persistence bug class
- [x] D3 strings.xml extraction
- [x] D1 delete confirmation dialog
- [x] D5 instrument detail dialog
- [x] D2 animations (banner, list items, state transitions)
- [x] D4 design-system visual polish
- [x] Developer-additions tracking restructure in REQUIREMENTS.md

### Phase 4 — enforcement and scope freeze
- [x] Stabilize cross-agent regressions (unsupported-instrument contract, test suite)
- [x] Machine-enforce style rules (detekt + compose-rules; see CLAUDE.md)
- [x] Scope frozen for submission — remaining designs recorded below, deliberately not built

## Designed but deliberately cut

The brief prefers "a smaller app with well-organized, maintainable code and thoughtful
tradeoffs … over a broad but fragile implementation," and these were the next features in
flight when breadth began to outweigh explainability. Both are designed and ready to build.

**Market sessions & after-hours pricing.** A `MarketStatusRepository` polling Finnhub's
`/stock/market-status` (locally computed NYSE schedule as fallback and as the demo source),
a `MarketSession` tag on quotes, the listed stock price frozen at the regular-session close
while after-hours ticks feed a separate subdued "After hours" line (matching brokerage UX),
explicit at-close/after-hours fields in the detail dialog, and a session-segmented sparkline
that renders extended-hours data in a muted style and refuses to connect across data gaps —
so missing data reads as a visible break, never a fabricated line. Crypto stays 24/7.

**Swipe-to-remove with undo.** Replace the X + confirmation dialog with swipe-to-dismiss and
an Undo snackbar: confirmation dialogs suit costly or irreversible actions, while removing a
watchlist item is cheaply reversible, which favors undo (lossless — the cached quote would be
restored via a repository-level restore, not a fresh re-add). A custom accessibility action
and a remove affordance in the detail dialog would keep non-swipe paths available.

## Deviations log

| Date | Change | Why |
|------|--------|-----|
| 2026-06-09 | No standalone Kotlin Android plugin | AGP 9 ships built-in Kotlin support and rejects `org.jetbrains.kotlin.android` |
| 2026-06-09 | compileSdk 37 (target stays 36) | Current androidx releases require compiling against android-37 |
| 2026-06-09 | Watchlist + search presenters landed in one commit | They share the Koin presentation module and test fakes; splitting would have left an intermediate commit referencing missing classes |
| 2026-06-10 | Cached-quote columns are nullable in Room despite the no-nullable-primitives rule | SQLite stores NaN as NULL, so NaN sentinels cannot live in NOT NULL REAL columns — INSERT OR IGNORE silently dropped every row. NULL maps to NaN at the entity boundary |
| 2026-06-10 | Database back to version 1; v2 schema and migration removed | The v2 NOT NULL schema was unshippable (see above) and its migration declared DEFAULT clauses Room's validator rejects. No release ever shipped v2; dev devices downgrade destructively |
| 2026-06-11 | After-hours feature (D6–D8) cut at ~25% built | Breadth began to outweigh explainability; the brief explicitly prefers smaller-but-excellent. Design preserved above |
| 2026-06-11 | Swipe-to-remove with undo (D9) cut before starting | Same scope freeze; the shipped confirm dialog is tested and accessible. Design preserved above |
| 2026-06-09 | Engineering style: block bodies, explicit lambda names | Developer preference for debugging clarity and readability |
| 2026-06-09 | Performance: Double.NaN for primitives | Avoid JVM object wrapping/boxing overhead for price data |
| 2026-06-09 | Safety: Braces for all if/else, no !! | Enforced robustness and strict null safety |
