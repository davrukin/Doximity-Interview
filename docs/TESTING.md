# Testing Guide

## Automated tests

```bash
# Unit tests (presenters, data layer) — runs on JVM
./gradlew testDebugUnitTest

# Compose UI tests — requires a connected device or emulator
./gradlew connectedDebugAndroidTest

# Full gate (lint + static analysis + build + unit tests)
./gradlew ktlintCheck detekt assembleDebug testDebugUnitTest
```

| Suite | Count | Location |
|-------|-------|----------|
| Unit — presenters | 25 | `app/src/test/.../presentation/` |
| Unit — data layer | 15 | `app/src/test/.../data/` |
| Compose UI tests | 10 | `app/src/androidTest/` |

---

## Manual test plan

### Prerequisites

**Demo mode** (no API key needed): remove or leave empty the `FINNHUB_API_KEY` entry in
`local.properties`. The app starts in demo mode automatically.

**Live mode**: add a free Finnhub API key to `local.properties`:

```
FINNHUB_API_KEY=your_key_here
```

US-market hours give the most interesting live-tick behavior; crypto (BTC, ETH) trades 24/7.

---

### S1 — Search: results

| Step | Expected |
|------|----------|
| Open the app | Watchlist screen shown |
| Tap the search FAB (bottom right) | Search screen opens; query field is focused |
| Type `AAPL` | After ~300 ms debounce: a list of matching instruments appears (live) or demo instruments containing "AAPL" |
| Tap a result | Row highlighted; "+" button changes to "✓" (already-added items show a check) |

Automated coverage: `SearchPresenterTest.search_results_emitted_for_non_blank_query`

---

### S2 — Search: empty state

| Step | Expected |
|------|----------|
| In search, type a query that returns no results (e.g. `ZZZZZZ`) | Empty-state message is shown |

Automated coverage: `SearchPresenterTest.empty_state_shown_for_query_with_no_results`

---

### S3 — Search: error and retry

| Step | Expected |
|------|----------|
| Enable airplane mode | — |
| Open search, type `AAPL` | Error state shown with a Retry button |
| Tap Retry | Another attempt fires; error persists while offline |
| Re-enable network | Tap Retry; results appear |

Automated coverage: `SearchPresenterTest.error_state_shown_on_search_failure`,
`SearchPresenterTest.retry_re_invokes_search`

---

### S4 — Search: debounce

| Step | Expected |
|------|----------|
| Type `A`, then immediately `B`, then `C` rapidly | Only one network request fires (for `ABC`); no intermediate requests are visible in network logs |

Automated coverage: `SearchPresenterTest.debounce_prevents_rapid_fire_requests`

---

### S5 — Add an instrument

| Step | Expected |
|------|----------|
| Search for `AAPL` (or any demo symbol) | Results shown |
| Tap the `+` button on a result | Button changes to `✓`; navigate back |
| Observe the watchlist | The instrument appears as a new row |

Automated coverage: `SearchPresenterTest.adding_instrument_updates_toggle_state`;
`WatchlistScreenTest.add_instrument_appears_in_list` (UI test)

---

### S6 — Remove an instrument (confirm flow)

| Step | Expected |
|------|----------|
| Long-press or tap the `×` button on a watchlist row | Confirmation dialog appears: "Remove [symbol]?" with Confirm and Cancel |
| Tap Cancel | Dialog dismisses; row remains |
| Tap `×` again, then Confirm | Dialog dismisses; row is removed from the list |

Automated coverage: `WatchlistPresenterTest.remove_shows_confirmation_dialog`,
`WatchlistPresenterTest.confirm_remove_removes_item`,
`WatchlistPresenterTest.dismiss_remove_keeps_item`

---

### S7 — Live price ticks and movement indicators

| Step | Expected |
|------|----------|
| Add one or more instruments (live mode with US stocks during market hours, or crypto anytime) | Prices update in real time |
| Watch an updating row | A green ▲ or red ▼ indicator flashes briefly after each tick; price text reflects the latest trade |
| Watch the sparkline (small chart on the right) | Fills gradually from left to right with each tick (up to 40 ticks retained) |

**Note — sparkline only appears when ticks arrive in the current session.** The REST snapshot
populates the price display but not the sparkline buffer. Instruments with low or zero after-hours
volume (e.g. ETFs like VOO) will show a price but no chart outside market hours, while actively
traded after-hours instruments (e.g. AAPL) will accumulate ticks and show a chart. This is
correct behavior: an empty sparkline means "no in-session activity," which is more honest than
drawing a flat line.

Automated coverage: `WatchlistPresenterTest.price_tick_updates_row_and_movement_direction`

---

### S8 — Connection banner lifecycle (airplane-mode test)

| Step | Expected |
|------|----------|
| Open the watchlist with instruments added | No banner when connected |
| Enable airplane mode | An orange "Reconnecting…" banner appears at the top within ~1 s |
| Wait ~30 s with airplane mode on | Banner changes to red "Offline" (after retry budget exhausted) |
| Re-enable network | Banner changes to "Reconnecting…", then disappears; prices refresh (snapshot refetch visible in logs) |

Automated coverage: `ReconnectingPriceStreamTest` (virtual-time tests for reconnect/offline
transitions);
`WatchlistPresenterTest.connection_state_banner_shown`

---

### S9 — Stale price display after relaunch

| Step | Expected |
|------|----------|
| Add instruments; wait for live prices to arrive | Prices shown without stale label |
| Force-close the app (swipe from recents) and relaunch | Watchlist shows immediately with last-known prices; each row shows a grey "Stale · Xm ago" label until the socket reconnects |
| After reconnect | Stale label disappears; prices refresh |

Automated coverage: `WatchlistPresenterTest.cached_quotes_show_stale_label_on_launch`

---

### S10 — Missing price (no baseline)

| Step | Expected |
|------|----------|
| Add a crypto symbol (e.g. `BINANCE:BTCUSDT`) before any tick has arrived | Row shows the price from the REST snapshot but "—" for the day-change field |
| (Demo mode) Add the unsupported demo symbol `UNSUPPORTED` | Row shows a "Price unavailable" message instead of a price |

Automated coverage: `WatchlistPresenterTest.missing_quote_shows_no_price_state`

---

### S11 — Demo blip cycle

| Step | Expected |
|------|----------|
| Run in demo mode (no API key) | App shows demo prices updating on a random walk |
| Wait ~40 s | The connection banner flashes "Reconnecting…" briefly, then returns to normal — simulating a network blip |
| Observe that data continues | Prices resume after the simulated blip |

Automated coverage: `DemoPriceStreamSourceTest.blip_emits_disconnecting_then_connecting`

---

### S12 — Demo / live mode toggle

| Step | Expected |
|------|----------|
| Configure a valid API key in `local.properties` | A "Demo" / "Live" chip appears in the top bar |
| Tap the chip | Mode switches; prices change source; socket reconnects to Finnhub (live) or demo stream |
| Tap again | Reverts to previous mode |

Automated coverage: `MarketDataModeRepositoryImplTest`

---

### S13 — Sort order cycling

| Step | Expected |
|------|----------|
| Add 3+ instruments | Default order: by time added (newest first) |
| Tap the sort chip | Cycles: Added → A–Z → Day change; chip label updates |
| Select "Day change" | Rows reorder by percentage change (highest gain at top) |

Automated coverage: `WatchlistPresenterTest.sort_order_changes_row_ordering`

---

### S14 — Pull to refresh

| Step | Expected |
|------|----------|
| With the watchlist open | Swipe down from the top of the list |
| Release | A loading indicator appears briefly; REST snapshots are re-fetched; stale labels clear |

Automated coverage: `WatchlistPresenterTest.pull_to_refresh_triggers_snapshot_refetch`

---

### S15 — Instrument detail dialog

| Step | Expected |
|------|----------|
| Tap a watchlist row (not the `×` button) | Detail dialog opens, showing: symbol, full name, price, day change %, volume (if available), instrument type |
| Tap outside the dialog or the dismiss button | Dialog closes; watchlist is unchanged |

Automated coverage: `WatchlistPresenterTest.row_tap_opens_detail_dialog`,
`WatchlistPresenterTest.dismiss_detail_closes_dialog`

---

### S16 — Rotation persistence

| Step | Expected |
|------|----------|
| Open search, type a query | Results shown |
| Rotate device | Query is preserved; results re-appear |
| Open the remove-confirmation dialog on the watchlist | Rotate device | Dialog is still open for the same symbol |
| Dismiss; change sort order; rotate | Sort order is preserved |

Automated coverage: `rememberSaveable` used for query, pending removal, and sort order (verified by
`WatchlistPresenterTest.sort_order_persists_across_recomposition`)

---

### S17 — Process death recovery

| Step | Expected |
|------|----------|
| In Developer Options, enable "Don't keep activities" | — |
| Add instruments; navigate to search | On returning to the watchlist, the list is restored from Room |
| Open the detail dialog; press Home; return | Dialog state is not preserved (expected: rememberSaveable does not survive process death without SavedStateHandle) |

---

### S18 — Background socket teardown (logcat)

| Step | Expected |
|------|----------|
| With the app running and prices updating | Open Android Studio Logcat (filter tag: `PriceRepository`) |
| Press Home to background the app | Within a few seconds: log shows socket paused / unsubscribed |
| Return to foreground | Log shows socket resumed / resubscribed + snapshot refetch |

Logcat tags: `PriceRepositoryImpl`, `ReconnectingPriceStream`

---

## Where automation does not cover manual tests

| Scenario | Why manual |
|----------|------------|
| S8 airplane-mode banner | Requires real network state change; Compose UI tests run on emulator in isolation |
| S9 relaunch staleness | Process boundary; unit tests verify the stale flag logic independently |
| S17 process death | Needs Developer Options setting; unit tests cover data survival via Room |
| S18 logcat background teardown | Side-effect observable; automated test verifies `appLifecycleState` gating in `PriceRepositoryImplTest` |
