# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Kotlin Multiplatform (KMM) Chinese Chess (Xiangqi) multiplayer game targeting Android and iOS, using Compose Multiplatform for shared UI.

## Build Commands

```bash
# Build Android debug APK
./gradlew :composeApp:assembleDebug

# Run tests (shared common tests)
./gradlew :composeApp:allTests

# Run only common tests
./gradlew :composeApp:testDebugUnitTest
```

iOS: Open `iosApp/` in Xcode and run from there.

## Architecture

### Module Structure

- **composeApp/** — Main KMM module with shared code and platform-specific implementations
  - `commonMain/` — Shared Kotlin code (business logic, UI via Compose Multiplatform)
  - `androidMain/` — Android-specific code (MainActivity, platform actuals)
  - `iosMain/` — iOS-specific code (MainViewController, platform actuals)
  - `commonTest/` — Shared tests using kotlin-test
- **iosApp/** — iOS app entry point (Swift/SwiftUI)
- **chinese-chess-ui-references/** — Standalone UI reference module (not part of the main build). Contains complete Jetpack Compose screen designs for MainMenu, BattleLobby, GameRoom, and Social screens with a dark theme + gold accent design system.

### Key Patterns

- **Expect/Actual**: Platform abstractions defined in `commonMain/Platform.kt` with implementations in `androidMain/` and `iosMain/`.
- **Single Activity**: Android uses one `MainActivity` hosting Compose UI via `App()` composable.
- **Entry points**: Android — `MainActivity.kt`, iOS — `MainViewController.kt`, Shared UI — `App.kt`.

### Design System (from UI references)

- Dark theme with gold accents (primary: #D8B646)
- Custom Canvas-drawn Xiangqi board (9×10 grid with palace diagonals, river text "楚河 漢界")
- Manrope font family, Material 3

## Tech Stack

- Kotlin 2.3.0, Compose Multiplatform 1.10.0, AGP 8.11.2
- Android: compileSdk 36, minSdk 24, targetSdk 36, JVM target 11
- Dependencies managed via Gradle version catalog (`gradle/libs.versions.toml`)

## Package

`com.charmflex.app.mobile_chinese_chess_multiplayer`
