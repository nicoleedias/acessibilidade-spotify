# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**SAC — Sistema de Acessibilidade Cefálica para Spotify** is an Android MVP that lets users with motor disabilities control Spotify playback using head gestures detected via the front-facing camera. All video processing is local (Edge Computing) — no frames are sent to any server.

The project is in the **early implementation phase**: five UI screens are implemented (no MVVM/ViewModel/Room yet — that refactor comes next). The `Docs/` folder contains all design documents.

> **TCC context:** This is an undergraduate final project. Every non-trivial technical decision must be justifiable on a defense panel. Prefer well-documented, mainstream solutions over clever ones, and record decisions in `Docs/decisions/` (ADR format).

## Tech Stack

| Layer                | Technology                                                       |
| -------------------- | ---------------------------------------------------------------- |
| UI                   | Kotlin + Jetpack Compose                                         |
| Computer Vision      | OpenCV (camera buffer) + MediaPipe Face Mesh (landmark tracking) |
| ML Classification    | MediaPipe-based gesture classifier                               |
| External Integration | Spotify Web API (REST, OAuth 2.0 with PKCE)                      |
| Local Persistence    | SQLite (via Room)                                                |
| Architecture Pattern | MVVM                                                             |
| DI                   | Hilt (`@HiltAndroidApp` on `SacApplication`, `@AndroidEntryPoint` on `MainActivity`) |
| Async                | Kotlin Coroutines + Flow                                         |
| Tests                | JUnit 4, MockK, Compose UI Test, Espresso                        |

## Current Source Structure

```
app/src/main/java/com/sac/acessibilidade/
├── MainActivity.kt           # Entry point — sets SacTheme + SacNavHost
├── SacApplication.kt         # @HiltAndroidApp
└── ui/
    ├── navigation/
    │   ├── Screen.kt         # sealed class Screen(val route: String) with 5 destinations
    │   └── SacNavGraph.kt    # SacNavHost composable (NavHost wiring)
    ├── screens/
    │   ├── LoginScreen.kt
    │   ├── HomeScreen.kt
    │   ├── CalibrationScreen.kt
    │   ├── GestureConfigScreen.kt
    │   ├── GestureMappingUi.kt      # data class used by GestureConfigScreen
    │   ├── PlayerAtivoScreen.kt
    │   └── PlayerAtivoUiState.kt    # data class used by PlayerAtivoScreen
    └── theme/
        ├── Color.kt           # design tokens (SpotifyGreen, BackgroundDark, TextPrimary…)
        ├── Theme.kt
        └── Type.kt
```

**Planned but not yet created:** `vision/`, `spotify/`, `service/`, `data/`, `domain/`, `di/` packages.

The `domain/` layer must have **zero Android imports** so it can be unit-tested without instrumentation.

## Gesture-to-Command Mapping

**Mappings are fully user-customizable.** Different users have different motor capabilities — what is comfortable for one user may be impossible for another. Therefore the app must NOT enforce a fixed gesture→action mapping.

### Supported Gestures (Vocabulary)

| Gesture           | Detection signal                                 |
| ----------------- | ------------------------------------------------ |
| `TILT_HEAD_RIGHT` | Roll axis > positive threshold                   |
| `TILT_HEAD_LEFT`  | Roll axis < negative threshold                   |
| `TILT_HEAD_UP`    | Pitch axis > positive threshold (chin lifted)    |
| `TILT_HEAD_DOWN`  | Pitch axis < negative threshold (chin tucked)    |
| `TURN_FACE_RIGHT` | Yaw axis > positive threshold                    |
| `TURN_FACE_LEFT`  | Yaw axis < negative threshold                    |
| `NOD`             | Vertical pitch oscillation pattern (yes)         |
| `BLINK_RIGHT_EYE` | Right eye EAR (Eye Aspect Ratio) below threshold |
| `BLINK_LEFT_EYE`  | Left eye EAR below threshold                     |

### Supported Actions

| Action           | Spotify Web API endpoint                  |
| ---------------- | ----------------------------------------- |
| `PLAY_PAUSE`     | `PUT /me/player/play` or `/pause`         |
| `NEXT_TRACK`     | `POST /me/player/next`                    |
| `PREVIOUS_TRACK` | `POST /me/player/previous`                |
| `VOLUME_UP`      | `PUT /me/player/volume?volume_percent=+5` |
| `VOLUME_DOWN`    | `PUT /me/player/volume?volume_percent=-5` |

### Default Mapping

| Default gesture   | Default action   |
| ----------------- | ---------------- |
| `TILT_HEAD_RIGHT` | `VOLUME_UP`      |
| `TILT_HEAD_LEFT`  | `VOLUME_DOWN`    |
| `TURN_FACE_RIGHT` | `NEXT_TRACK`     |
| `TURN_FACE_LEFT`  | `PREVIOUS_TRACK` |
| `NOD`             | `PLAY_PAUSE`     |

### Customization Rules

- A gesture may be mapped to **at most one action**.
- An action may be mapped to **at most one gesture**.
- The user can leave a gesture unmapped (no action).
- Saving the mapping screen persists changes immediately to SQLite.
- The settings screen must surface a **"Restore defaults"** option.
- Hardcoding gesture→action elsewhere in the code is forbidden — always read from the database.

> **TCC defense angle:** Gesture customization is itself an accessibility feature. Hardcoding the mapping would exclude users who cannot perform a specific gesture. Cite WCAG 2.1 SC 2.5.6 (Concurrent Input Mechanisms) when questioned.

## Architecture

Four layers:

1. **Presentation** — Jetpack Compose screens + ViewModels. Five screens: Login (OAuth), Home/Dashboard, Calibration, Command Mapping, Active Tracking.
2. **Processing** — OpenCV manages the Android Camera2 API buffer; MediaPipe Face Mesh extracts facial landmarks and classifies gestures.
3. **Integration** — REST client sends commands to the Spotify Web API; responses trigger UI feedback.
4. **Data** — SQLite stores per-user calibration parameters (amplitude thresholds) and customized gesture-to-command mappings.

An Android `AccessibilityService` (or Broadcast mechanism) is needed so commands reach Spotify when it runs in the background.

## Key Non-Functional Requirements

- **Latency:** ≤ 500 ms from gesture detection to Spotify action.
- **Accuracy:** > 90% correct gesture recognition in adequately lit environments.
- **Privacy:** Zero video frames stored or transmitted; LGPD compliant.
- **Calibration UX:** User can complete calibration in < 2 minutes.

These are **measurable targets** for the TCC defense — every PR touching the vision pipeline or the Spotify dispatcher should mention impact on these metrics.

## Privacy & LGPD (Non-Negotiable Rules)

The user's camera feed is sensitive biometric data. The following rules are absolute:

- **Never** write camera frames to disk (no debug dumps, no crash reports including frames).
- **Never** send camera frames or facial landmarks over the network — not to Spotify, not to analytics, not to crash reporters.
- **Never** log raw landmark coordinates (only aggregate metrics like FPS, classification confidence).
- Camera permission must be requested **just-in-time** with a clear in-app rationale screen explaining local-only processing.
- The user must be able to revoke camera access and delete all calibration data from inside the app (LGPD right to erasure).
- Add a `.gitignore` rule preventing accidental commit of any `.mp4`, `.jpg`, `.png` from the device under `app/` other than design assets.

## Accessibility (For the End User Operating the App)

The target user has motor disabilities — touch interactions are limited. Therefore:

- **TalkBack support is mandatory.** Every Compose interactive element needs a `contentDescription` or `Modifier.semantics`.
- Touch targets ≥ **48dp × 48dp** (Material guideline) — but assume the user may not be able to touch the screen at all. Critical actions must be fully reachable via the head gesture system itself.
- Calibration screen must work even before gestures are calibrated — provide a fallback (voice command, large button, or external Switch Access).
- Visual feedback for every gesture detected (e.g., a subtle on-screen indicator) so the user understands what the app "saw".
- Audio feedback option (Text-to-Speech) confirming each Spotify action: _"Playing", "Paused", "Volume up"_.
- Reduce-motion preference must disable any non-essential animation.
- Dark mode and high-contrast mode must both pass **4.5:1 contrast** for text.
- **Gesture configuration screen must itself be operable by users who cannot perform all gestures** — chicken-and-egg problem solved by allowing keyboard / Switch Access / voice as configuration alternatives.

## Code Conventions

### Kotlin

- Kotlin official style guide (`ktlint` enforced).
- Prefer immutability: `val` over `var`, `data class` for state.
- Sealed classes / sealed interfaces for state machines (gesture states, auth states).
- No `!!` operator. Use `requireNotNull()` with a descriptive message or handle the null branch.
- Suspend functions for I/O; never block the main thread.

### Compose

- One Composable per screen file, named after the screen (`CalibrationScreen.kt`).
- State hoisting: Composables are stateless; ViewModels own state via `StateFlow`.
- Use `@Preview` for every reusable Composable. All `@Preview` functions must be `private` and annotated with `@Suppress("UnusedPrivateMember")` — Detekt cannot see Android Studio's runtime usage of them.
- No business logic inside Composables — only rendering and event forwarding.

### Design tokens

All screens import color constants directly from `ui/theme/Color.kt` (`BackgroundDark`, `SpotifyGreen`, `TextPrimary`, etc.) — do **not** use `MaterialTheme.colorScheme` for these. Typography uses `MaterialTheme.typography`.

### Directional icons

Use `Icons.AutoMirrored.Filled.*` for directional icons (`ArrowBack`, `KeyboardArrowLeft`, `KeyboardArrowRight`) — the `Icons.Filled.*` variants are deprecated and emit compiler warnings. The import is `androidx.compose.material.icons.automirrored.filled.*`.

### Detekt: one public declaration per file

Detekt enforces `MatchingDeclarationName`: if a `.kt` file has a single public top-level declaration, the file name must match it. When a screen needs a companion data class (e.g., `PlayerAtivoUiState` for `PlayerAtivoScreen`), put it in its own file. Similarly, `sealed class Screen` lives in `Screen.kt`, not in `SacNavGraph.kt`.

### MVVM

- ViewModels expose `StateFlow<UiState>`; UI collects with `collectAsStateWithLifecycle()`.
- Use cases live in `domain/`, are single-purpose, named with imperative verbs (`StartCalibrationUseCase`).
- Repositories abstract data sources; ViewModels depend on repositories, not on Room/Retrofit directly.

### Naming

- Packages: lowercase, no underscores.
- Classes: `PascalCase`. Composables: `PascalCase` too.
- Functions/properties: `camelCase`.
- Constants: `SCREAMING_SNAKE_CASE` inside `companion object`.
- All identifiers in **English**. User-facing strings live in `res/values/strings.xml` (and `strings-pt-rBR.xml` for Portuguese).

## Spotify Web API Integration

- **OAuth 2.0 with PKCE** is required (Spotify deprecated implicit grant).
- Store `client_id` in `local.properties` (gitignored), expose via `BuildConfig`.
- Tokens stored in `EncryptedSharedPreferences` — **never** in plain `SharedPreferences` or SQLite.
- Centralize all calls in `spotify/SpotifyApiClient.kt` with a Retrofit interface.
- Handle `429 Too Many Requests`: read `Retry-After` header, suspend, retry.
- Handle `401 Unauthorized`: refresh token automatically, retry once, then surface error.
- Map all API errors to a domain `SpotifyError` sealed class — never let raw HTTP exceptions reach the UI.
- The Spotify Web API requires the **Spotify app to be active and playing** on the device for some endpoints (`/me/player/*`). Document this clearly to the user during onboarding.

## Vision Pipeline Rules

- The camera processor runs on a **dedicated background thread**, never on the main thread.
- Frame rate target: 15–30 FPS. If processing falls behind, **drop frames**, do not queue them (memory pressure).
- Gesture classification must include a **confidence threshold** (e.g., > 0.85) before dispatching the Spotify command.
- Implement a **debounce/cooldown** (e.g., 800 ms) between commands to prevent the same gesture firing twice.
- Calibration thresholds are **per-user** and stored in SQLite; never hardcoded.
- The `GestureClassifier` must be unit-testable with synthetic landmark inputs — no `Context` dependencies.
- The classifier emits a `Gesture` enum value. Mapping to a Spotify `Action` happens in a separate `GestureActionMapper` that reads from the database — keeping classifier independent of user preferences.

## Testing Strategy

- **Domain (use cases):** pure JUnit + MockK. 100% target coverage on critical paths.
- **Data (repositories, DAOs):** Room in-memory tests.
- **Vision (classifier):** synthetic landmark fixtures stored in `test/resources/landmarks/`. Test each gesture's positive and negative cases (false positive matters more than miss for accessibility).
- **Spotify client:** MockWebServer for HTTP tests.
- **UI:** Compose UI Test for screen-level assertions; include a **TalkBack semantics test** for every interactive element.
- **Mapping:** test that user customizations are persisted and respected across app restarts.
- **Manual:** Document accessibility test runs (TalkBack walkthrough, gesture latency stopwatch) in `Docs/testes-manuais.md`.

Minimum gate before merge: `./gradlew ktlintCheck detekt test` passes, no new warnings.

## Documents in `Docs/`

| File                        | Contents                                                               |
| --------------------------- | ---------------------------------------------------------------------- |
| `documento_visao.md`        | Vision document — problem, stakeholders, feature list                  |
| `documento_arquitetura.md`  | Architecture document (4+1 views, MVVM rationale, diagrams)            |
| `documento_casosDeUso.md`   | Use case specifications (UC01–UC03)                                    |
| `documento_interface.md`    | UI/UX spec — screen descriptions, navigation flow, design system       |
| `documento_prototipagem.md` | Prototyping support doc — screen wireframe descriptions and user flows |
| `Protótipo/protótipo.md`    | Prototype assets                                                       |

## Hardware Requirements

Android 10+ (API 29+), front camera ≥ 720p, active internet connection, active Spotify account (Free or Premium — note that some Spotify Web API endpoints require Premium; document this limitation).

## Useful Commands

```bash
# Build & install on connected device
./gradlew installDebug

# Run unit tests (fast, no device needed)
./gradlew test

# Run instrumented tests (needs emulator/device)
./gradlew connectedAndroidTest

# Auto-fix formatting (run before ktlintCheck)
./gradlew ktlintFormat

# Lint & static analysis
./gradlew ktlintCheck
./gradlew detekt

# Full pre-commit gate
./gradlew ktlintCheck detekt test
```

## Things NOT to Do

- ❌ Do not log, save, or transmit camera frames or facial landmarks anywhere.
- ❌ Do not store Spotify tokens in plain `SharedPreferences` or SQLite.
- ❌ Do not block the main thread with vision or network work.
- ❌ Do not hardcode gesture→action mappings — they live in the database and are user-customizable.
- ❌ Do not call the Spotify API directly from a `Composable` or `ViewModel` — go through a repository.
- ❌ Do not use `!!` to silence nullability warnings.
- ❌ Do not commit `local.properties`, `*.keystore`, or anything with secrets.
- ❌ Do not add a new dependency without justifying it (TCC defense will ask).
- ❌ Do not break TalkBack — every UI change should be tested with the screen reader on.

## Notes for Claude Code

When implementing a new feature:

1. **Start with a plan.** State which layers will change, which use cases are affected, what tests will be added, and which non-functional requirement (latency / accuracy / privacy / a11y) the change touches.
2. **Ask before guessing.** If a doc in `Docs/` is unclear or absent, ask Pedro or Nicole — do not invent design decisions.
3. **Tests come with the feature**, not in a follow-up PR.
4. **Run the gate** (`./gradlew ktlintCheck detekt test`) before declaring the work done. Report results.
5. **Commit messages** must reference the use case (`UC01`, `UC02`...) when applicable, e.g.: `feat(calibration): persist user thresholds [UC02]`.
6. **Privacy is a hard constraint.** If you find yourself about to log a frame, save a landmark, or send any biometric data over the network, stop and flag it instead.
7. **Justify trade-offs.** When choosing between two approaches, briefly explain why — this saves time during the TCC defense.
