# Mobile Chinese Chess Multiplayer (Xiangqi)

A full-stack, cross-platform Xiangqi (Chinese Chess) app built with **Kotlin Multiplatform + Compose Multiplatform** for Android and iOS, backed by a **Spring Boot** server.

**Features:**
- Online multiplayer (real-time WebSocket matchmaking & private rooms)
- AI opponents (Beginner → Expert difficulty)
- Local two-player mode
- Google OAuth + guest login (via Supabase)
- Spectator mode, global chat, draw/undo/resign, XP & levelling system

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Prerequisites](#prerequisites)
3. [Project Structure](#project-structure)
4. [Backend Setup (Server)](#backend-setup-server)
5. [Client Setup (Android & iOS)](#client-setup-android--ios)
6. [Running Locally](#running-locally)
7. [Running Tests](#running-tests)
8. [Architecture Overview](#architecture-overview)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Mobile (Android + iOS) | Kotlin Multiplatform, Compose Multiplatform |
| Server | Spring Boot (Kotlin), WebSocket |
| Database | PostgreSQL (self-hosted) |
| Auth | Supabase (Google OAuth + anonymous) |
| DI | Koin + KSP |
| Networking | Ktor (HTTP + WebSocket client) |
| Serialization | kotlinx.serialization |
| Game Engine | Custom Xiangqi rules & AI (shared KMM library) |

---

## Prerequisites

Install the following before getting started.

### Required for all platforms

| Tool | Version | Install |
|---|---|---|
| JDK | 17+ | [Adoptium](https://adoptium.net/) or `brew install temurin@17` |
| Android Studio | Latest stable | [developer.android.com/studio](https://developer.android.com/studio) |
| Kotlin Multiplatform plugin | Latest | Android Studio → Plugins → search "Kotlin Multiplatform" |

### Required for iOS (macOS only)

| Tool | Version | Install |
|---|---|---|
| Xcode | 15+ | Mac App Store |
| CocoaPods | Latest | `sudo gem install cocoapods` |
| kdoctor | Latest | `brew install kdoctor` (then run `kdoctor` to verify KMP setup) |

### Required for the backend

| Tool | Install |
|---|---|
| JDK 17+ | (same as above) |
| A [Supabase](https://supabase.com) account (free tier works) | Sign up at supabase.com |

> **Tip:** After installing the Kotlin Multiplatform plugin in Android Studio and Xcode, run `kdoctor` in your terminal. Fix any issues it reports before continuing.

---

## Project Structure

```
.
├── composeApp/          # KMM module: shared UI + business logic
│   ├── commonMain/      # Platform-agnostic Kotlin (features, networking)
│   ├── androidMain/     # Android-specific actuals (MainActivity, DI init)
│   └── iosMain/         # iOS-specific actuals
├── server/              # Spring Boot backend
│   └── src/main/
│       ├── kotlin/      # WebSocket handler, REST API, game service, bot AI
│       └── resources/
│           ├── application.yml
│           └── .env.properties.example   ← copy this → .env.properties
├── game-engine/         # Shared KMM library: Xiangqi rules, move generator, AI
├── iosApp/              # Swift/SwiftUI iOS entry point
└── build.gradle.kts
```

---

## Backend Setup (Server)

### 1. Create a Supabase Project

1. Go to [supabase.com](https://supabase.com) and create a new project.
2. Wait for provisioning to complete (~2 minutes).
3. Note down the following from **Project Settings → API** and **Project Settings → Database**:
   - **Project Reference ID** (looks like `abcdefghij`)
   - **Database Password** (set during project creation)

### 2. Configure Environment Variables

```bash
cd server/src/main/resources
cp .env.properties.example .env.properties
```

Open `.env.properties` and fill in your Supabase values:

```properties
SUPABASE_JWK_SET_URI=https://<your-project-ref>.supabase.co/auth/v1/.well-known/jwks.json
POSTGRES_HOST=<your-postgres-host>     # e.g. "db" for Docker, or a remote hostname
POSTGRES_PASSWORD=<your-database-password>

# Leave blank to disable Discord notifications
DISCORD_WEBHOOK_URL=
DISCORD_WEBHOOK_URL_TEXT=
DISCORD_BOT_TOKEN=
ADMIN_PLAYER_IDS=
```

> The server uses `spring.jpa.hibernate.ddl-auto: update`, so the `players` table is created automatically on first run. No manual SQL migrations needed.

### 3. Configure Supabase Auth (for Google Sign-In)

1. In Supabase Dashboard → **Authentication → Providers**, enable **Google**.
2. Follow the prompts to create a Google OAuth app in [Google Cloud Console](https://console.cloud.google.com) and paste the Client ID + Secret into Supabase.
3. Add the redirect URI shown by Supabase back into your Google OAuth app.

> **Guest login works without any OAuth setup** — skip this step if you just want to test locally.

### 4. Start the Server

```bash
# From the project root:
./gradlew :server:bootRun
```

You should see:
```
Started XiangqiServerApplicationKt in X.XXX seconds
```

Verify it's running:
```bash
curl http://localhost:8080/api/rooms
# Expected: {"rooms":[]}
```

---

## Client Setup (Android & iOS)

### 1. Point the Client at Your Server

Open:
```
composeApp/src/commonMain/kotlin/com/charmflex/app/mobile_chinese_chess_multiplayer/feature/auth/constant/AuthConstant.kt
```

Replace the placeholder values with your own:

```kotlin
object AuthConstant {
    // Android Emulator → host machine:
    const val DEFAULT_HTTP_URL = "http://10.0.2.2:8080"
    const val DEFAULT_WS_URL   = "ws://10.0.2.2:8080/ws"

    // Real device / LAN:
    // const val DEFAULT_HTTP_URL = "http://192.168.1.100:8080"
    // const val DEFAULT_WS_URL   = "ws://192.168.1.100:8080/ws"

    // Supabase Dashboard → Project Settings → API → Project URL / anon public
    const val SUPABASE_URL      = "https://<your-project-ref>.supabase.co"
    const val SUPABASE_ANON_KEY = "<your-supabase-anon-key>"

    // Deep link scheme for OAuth callback — keep as-is unless you change the app ID
    const val DEEP_LINK_SCHEME = "com.charmflex.xiangqi"
    const val DEEP_LINK_HOST   = "auth-callback"
}
```

> The **anon key** is safe to embed in client code — it's designed to be public. Never embed your **service role key**.

### 2. Android

#### Option A: Android Studio (recommended)

1. Open the project root in Android Studio.
2. Let Gradle sync finish.
3. Select the `composeApp` run configuration and press **Run**.

#### Option B: Command line

```bash
./gradlew :composeApp:assembleDebug
```

Install the APK:
```bash
adb install composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

### 3. iOS (macOS only)

1. Open `iosApp/` in Xcode.
2. Select your target device or simulator.
3. Press **Run** (⌘R).

> On first build, Xcode will compile the KMM shared framework automatically via the Gradle build phase.

---

## Running Locally

### Quick local multiplayer (two emulators / devices on the same machine)

1. Start the server: `./gradlew :server:bootRun`
2. Set `DEFAULT_HTTP_URL = "http://10.0.2.2:8080"` (emulator) or your LAN IP (real device).
3. Build and install the app on both devices.
4. On Device 1: open app → **Battle Lobby** → **Create Room**.
5. On Device 2: open app → **Battle Lobby** → **Refresh** → tap the room to join.

### Single-device testing (vs. AI)

No server needed for AI or local two-player modes — just build and run the app.

### Multiplayer across devices on the same Wi-Fi

Find your machine's local IP:
```bash
# macOS / Linux
ifconfig | grep "inet " | grep -v 127.0.0.1

# Windows
ipconfig
```

Set the client URLs to `http://<YOUR_LAN_IP>:8080` and `ws://<YOUR_LAN_IP>:8080/ws`, then make sure port 8080 is open in your firewall:

```bash
# macOS (temporarily disable PF firewall for testing)
sudo pfctl -d

# Linux (ufw)
sudo ufw allow 8080
```

---

## Running Tests

```bash
# Shared (common) unit tests — game engine rules, AI
./gradlew :composeApp:testDebugUnitTest

# All tests
./gradlew :composeApp:allTests
```

---

## Architecture Overview

```
┌─────────────────────────────────────────┐
│           composeApp (KMM)              │
│  ┌──────────┐  ┌──────────┐            │
│  │ Android  │  │   iOS    │            │
│  └────┬─────┘  └────┬─────┘            │
│       └──────┬───────┘                 │
│         commonMain                     │
│    (features, Ktor, Koin, UI)          │
└──────────────┬──────────────────────────┘
               │ HTTP + WebSocket (Ktor)
┌──────────────▼──────────────────────────┐
│         server (Spring Boot)            │
│  GameWebSocketHandler                   │
│  GameService  BotService                │
│  RoomController (REST)                  │
└──────────────┬──────────────────────────┘
               │ JPA / JDBC
┌──────────────▼──────────────────────────┐
│        PostgreSQL (players)             │
└─────────────────────────────────────────┘
```

### WebSocket message envelope

All WS messages share a 3-field envelope:

```json
{ "scene": "game|global", "type": "message_type", "payload": { ... } }
```

Client-side: `GameChannel` filters `scene=game`; `GlobalChatChannel` filters `scene=global`.

### Key server components

| Component | Responsibility |
|---|---|
| `GameWebSocketHandler` | Receives & dispatches all WS messages |
| `GameService` | In-memory game state (rooms, queue, moves); all mutations go through here |
| `BotService` | Bot pool, AI game loop, lobby simulation |
| `SessionService` | Maps WebSocket session IDs ↔ player tokens |
| `WsMessages.kt` | Canonical source for `WsScene`/`WsType` constants and all payload data classes |

### Client feature structure

Each feature (`auth/`, `game/`, `home/`, `session/`) follows the same layout:

```
feature/
  auth/
    data/       ← repositories, remote data sources
    domain/     ← use cases
    ui/         ← Composable screens + ViewModels
    di/         ← Koin module
    route/      ← navigation
    network/    ← WS/HTTP message definitions
```

---

## Troubleshooting

| Symptom | Likely Cause | Fix |
|---|---|---|
| App shows "Failed to connect" | Wrong server URL | Update `DEFAULT_HTTP_URL` / `DEFAULT_WS_URL` in `AuthConstant.kt` |
| App hangs on "Waiting..." | Server not running | Run `./gradlew :server:bootRun` |
| Emulator connects but real device doesn't | Using `10.0.2.2` | Change to your LAN IP |
| Login fails / 401 errors | Supabase not configured | Check `SUPABASE_JWK_SET_URI` in `.env.properties` |
| DB connection refused | Wrong DB host/password | Verify `POSTGRES_HOST` and `POSTGRES_PASSWORD` |
| iOS build fails on KMP framework | Gradle or Xcode out of sync | Clean → `./gradlew clean` then rebuild in Xcode |

**Server logs** (in the `bootRun` terminal) use structured prefixes:
- `[API]` REST calls  `[WS]` WebSocket events  `[SVC]` GameService operations

**Android logcat:**
```bash
adb logcat | grep -E "\[(API|WS|USER|LOBBY|GAME)\]"
```
