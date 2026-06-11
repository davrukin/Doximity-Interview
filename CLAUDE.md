# Project Conventions

This file defines the engineering conventions for this project. All code — human- or AI-authored —
must follow them.

## Project & workflow

This is a real-time financial watchlist app on Finnhub (REST search/snapshots + WebSocket live
prices), with Room persistence and a demo/fake-data mode.

- Before starting any work, read [PLAN.md](docs/PLAN.md)
  and [REQUIREMENTS.md](docs/REQUIREMENTS.md).
- Update both documents in the same commit as the work they track: tick PLAN.md checkboxes, fill
  REQUIREMENTS.md implementation/test columns and statuses, and log plan deviations with a one-line
  rationale.
- Commit bodies cite the requirement IDs they advance, e.g. `Req: R4, R5`. Infrastructure-only
  commits cite none.
- Optional enhancements (O1–O7) are tackled only after every required row (R1–R11) is done.
- The Finnhub API key lives in `local.properties` as `FINNHUB_API_KEY` and is never committed.
  When no key is configured the app runs in demo mode.

## Architecture: Composable Presenter

This project uses the Composable Presenter pattern, as described in Doximity's engineering blog
([Part 1](https://technology.doximity.com/articles/simplifying-state-management-with-compose),
[Part 2](https://technology.doximity.com/articles/building-a-note-taking-app-in-compose)).

- A presenter implements `Presenter<Model : UiModel, Params>` with a single
  `@Composable fun present(params: Params): Model`. It contains presentation logic only — it never
  emits UI.
- `present()` reads top to bottom: inputs → internal state (`remember` / `mutableStateOf` /
  `rememberSaveable`) → returned immutable `UiModel`. Data-layer flows are collected with
  `produceState`-style collection so they are not re-collected on recomposition.
- Screen state is an `@Immutable` data class implementing `UiModel`. User actions are a sealed
  interface implementing `UiEvent`, nested inside the model. The model carries a single
  `EventHandler<E : UiEvent>`; UI dispatches typed events, the presenter handles them in a `when`.
- No ViewModels. Presenters are injected into composables via Koin. Work that must outlive
  composition runs on the app-scoped `CoroutineScope` provided through Koin. Rotation-survivable UI
  state uses `rememberSaveable`.
- Presenters compose hierarchically: a parent presenter calls child presenters' `present()`
  functions.
- UI composables are pure renderers: `XScreen(model: XUiModel, modifier: Modifier = Modifier)`.
  State is hoisted; data flows down, events flow up (unidirectional data flow). Components are
  self-contained black boxes.

## Kotlin / Compose style

- Use block body for functions instead of expression body (e.g., `fun getX(): T { return x }`
  instead of `fun getX(): T = x`).
- Specify explicit names for lambda parameters (avoid using `it`).
- Avoid nullable "primitive" data types (e.g., `Int?`, `Long?`, `Boolean?`, `Double?`, `Float?`) to
  prevent unnecessary object wrapping. Use meaningful default values or constants (e.g., `-1.0`,
  `Long.MIN_VALUE`) where appropriate, or use wrapper types only if essential for API compatibility.
- Specify data types for all public values and functions.
- Specify parameter names explicitly when using annotations (e.g., `@SerialName(value = "name")`).
- Do not use scoping functions (`let`, `run`, `apply`, `also`, `with`) on a single line.
- Avoid placing the expression after an equals sign on a new line for assignments.
- Use braces for all `if` and `else` statements, even if they contain only a single line.
- Never use the non-null assertion operator (`!!`).
- Use named arguments at every Compose call site, including trailing `content = { ... }` blocks.
- Use trailing commas everywhere argument/parameter lists span multiple lines (enforced via
  `.editorconfig`).
- Avoid extension functions; prefer member functions on the owning class, or top-level private
  functions where no class fits. Exception: extensions that are part of the Composable Presenter
  pattern itself (e.g. a `launchUseCase()` helper) are allowed.
- One composable per file. Exception: a screen file may contain that screen's own private content
  composables.
- Every composable has multiple `@Preview` functions covering its meaningful states, in the same
  file, using `PreviewParameterProvider` where the state space warrants it. Preview fixtures are
  clearly named and preview-only.
- No narrating comments. Comment only non-obvious constraints or rationale. KDoc only on the core
  pattern interfaces and public data-layer API.
- Style enforcement is mechanical wherever a check exists: ktlint (formatting, trailing commas,
  composable naming) + compose-rules (modifier/remember/CompositionLocal conventions) + detekt
  (`config/detekt/detekt.yml`: braces on all if/else, no `!!`, no TODO/FIXME comments, no
  `android.util.Log`, no swallowed exceptions). Rules with no off-the-shelf check remain
  conventions enforced by review: block bodies, named arguments everywhere (including trailing
  `content` lambdas), no nullable primitives, explicit public types, no extension functions.

## Testing

- Presenters are unit-tested with Molecule (`moleculeFlow { presenter.present(...) }`) and Turbine,
  asserting emitted `UiModel`s and event handling.
- Key screens have Compose UI tests (interaction → state change → assertion).
- `./gradlew ktlintCheck detekt assembleDebug testDebugUnitTest` must be green before every commit.

## Commits

- Conventional Commits: `feat:`, `fix:`, `test:`, `chore:`, `docs:`.
- Commit frequently — one coherent step per commit (a type, a screen, a presenter, a test suite), so
  the history reads as a reviewable train of thought and provides rollback points.
- Imperative subject line; body explains why when non-obvious.
