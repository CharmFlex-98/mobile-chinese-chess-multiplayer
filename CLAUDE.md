# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build Android debug APK
./gradlew :composeApp:assembleDebug

# Run all tests (shared + unit)
./gradlew :composeApp:allTests

# Run common unit tests only
./gradlew :composeApp:testDebugUnitTest

# Build server (Spring Boot)
./gradlew :server:build

# Run server
./gradlew :server:bootRun
```

iOS: Open `iosApp/` in Xcode and run from there.

## Module Structure

- **composeApp/** — KMM module: shared UI (Compose Multiplatform), business logic, networking
  - `commonMain/` — Platform-agnostic Kotlin (features, networking, engine integration)
  - `androidMain/` / `iosMain/` — Platform actuals (MainActivity, SharedPrefs, DI init)
  - `commonTest/` — Unit tests (game engine rules, AI)
- **server/** — Spring Boot backend (WebSocket handler, REST API, bot AI)
- **game-engine/** — Shared KMM library: Xiangqi rules, move generator, AI engine
- **iosApp/** — Swift/SwiftUI iOS entry point

## Architecture

### Client (composeApp)

**Feature structure** (each feature has `data/`, `domain/`, `ui/`, `di/`, `route/`, `network/`):
- `auth/` — Supabase Google OAuth + guest login, JWT handling
- `game/` — Game room, matchmaking lobby, online/AI/local game modes
- `home/` — Main menu, profile, settings, social
- `session/` — `SessionManager` (current user state as StateFlow)

**DI**: Koin with KSP annotations. `@Module @ComponentScan("com.charmflex.app.mobile_chinese_chess_multiplayer")` on `AppModule` auto-discovers all `@Factory` / `@Singleton` annotated classes.

**Key DI scopes**: `WebSocketClient`, `GameChannel`, `GlobalChatChannel`, `GameRepositoryImpl` are all `@Singleton` — they share a single WebSocket connection.

### WebSocket Communication

All WS messages use a 3-field envelope:
```json
{ "scene": "game|global", "type": "message_type", "payload": { ... } }
```

**Client → Server** (defined in `core/network/WebSocketMessages.kt`):
- `GameClientMessage` subclasses: `MakeMove`, `QueueJoin`, `QueueLeave`, `ChatSend`, `Resign`, `DrawOffer`, `DrawResponse`, `UndoRequest`, `UndoResponse`, `RoomJoin`, `GameClientOverReport`
- `GlobalClientMessage` subclasses: `GlobalChatSend`

**Server → Client** (received via `GameChannel` / `GlobalChatChannel`):
- Game scene: `MoveMade`, `GameStarted`, `RoomSnapshot` (spectator join state), `GameOver`, `QueueUpdate`, `MatchFound`, `ChatReceive`, `TimerUpdate`, `OpponentJoined/Disconnected/Reconnected`, `DrawOffered`, `UndoRequested`, `SpectatorJoined/Left`, `XpUpdate`, `Error`
- Global scene: `GlobalChatReceive`

**`WebSocketClient`** emits all messages to a `SharedFlow<WsEnvelope>`. `GameChannel` filters `scene=game`, `GlobalChatChannel` filters `scene=global`.

### Server (Spring Boot)

**`GameWebSocketHandler`** — Central WS handler. Dispatches messages by `(scene, type)`. Uses `WsMessageBuilder.buildGameMessage()` / `buildGlobalMessage()` from `WsMessages.kt` for all outgoing messages.

**`WsMessages.kt`** — Server-side canonical source for:
- `WsScene` / `WsType` constants
- All payload data classes (`@Serializable`)
- `WsMessageBuilder` object for building JSON envelopes

**`GameService`** — In-memory game state: players, rooms, matchmaking queue, live board per room. Key methods: `createRoom`, `joinRoom`, `makeMove`, `resign`, `markAsDraw`, `finishGame`, `recordGameStart`, `addXp`, `getSessionIdForPlayer`, `findMatch`, `abandonGame`. `makeMove` returns `MakeMoveResult` which includes `gameStatus`, `timedOut`, and timer values — the handler never touches `GameRoom` directly.

**`BotService`** — Bot pool (15 named bots, difficulties BEGINNER–EXPERT), bot matchmaking timer (10–15s fallback), bot game loop, lobby simulation (2 bot-vs-bot games kept active). Uses `WsMessageBuilder` for all messages. Exposes `roomCleanupCallback` so `GameWebSocketHandler` can clean up `roomSessions` when a bot-initiated game ends.

**REST API** (in `RoomController`):
| Endpoint | Method | Auth | Description |
|---|---|---|---|
| `/api/auth/guest` | POST | — | Guest login → `AuthResponse` |
| `/api/auth/login/verify` | POST | — | Supabase JWT verify → `LoginVerifyResponse` |
| `/api/rooms` | GET | Token | List active rooms → `ActiveRoomsResponse` |
| `/api/rooms/create` | POST | Token | Create room → `CreateRoomResponse` |
| `/api/rooms/{id}/join` | POST | Token | Join room → `BattleRoomResponse` |

### Player XP and Level System

- `Player.xp` starts at 0 for new players (bots have legacy strength ratings)
- `Player.level = 1 + xp / 200` (computed via `Player.computeLevel()`)
- XP awarded on game over: **+50 XP** (vs real player), **+30 XP** (vs bot)
- No XP for draws; server grants XP, sends `XpUpdate` WS message to winner

### Spectator Support

- Any authenticated player can join a public, in-progress room as spectator (via WS `room_join`)
- Server detects spectators: players not in room's `redPlayer`/`blackPlayer`
- Server sends `RoomSnapshot` (full game state with move history) to the joining spectator
- Server broadcasts `SpectatorJoined` to all room members
- Spectators can chat; `ChatReceive.isSpectator = true` marks spectator messages

### Global Chat

- Client sends `GlobalChatSend(message)` with `scene=global` via `GlobalChatChannel`
- Server broadcasts `GlobalChatReceive` to **all** connected WebSocket sessions
- Client subscribes via `GameRepository.subscribeGlobalChat()`

---

## Architecture Decisions & Lessons Learnt

### Server is the Single Source of Truth for Game-Over

**Problem (before):** The client detected checkmate locally after making a move and sent a `GAME_OVER_REPORT` back to the server, which then re-broadcast `GAME_OVER` to all players (including the sender). This created duplicated game-status logic on both sides, a round-trip race between `MAKE_MOVE` and `GAME_OVER_REPORT`, and meant the game could be stuck if the client had a bug or simply didn't send the report.

**Decision:** The server evaluates `GameRules.getGameStatus()` after every `makeMove()` call (same as `BotService` already did for bot moves). `GameService.makeMove()` returns a `MakeMoveResult` containing `gameStatus` and `timedOut`. `GameWebSocketHandler.handleMakeMove()` broadcasts `GAME_OVER` and grants XP directly — no client report needed.

**Client side:** `GameRoomViewModel.makeMove()` still computes game status locally for responsive UI, but does **not** call `reportGameOver`. The authoritative `GAME_OVER` message from the server is the only signal that finalises game state for online mode. `GAME_OVER_REPORT` is now a no-op on the server (deprecated, kept for backward compat).

### GameService Owns All Game State Mutations

**Problem (before):** `GameWebSocketHandler` handlers directly set `room.status = RoomStatus.FINISHED` and `room.lastMoveTimestamp` — bypassing `GameService` and making game logic leak into the transport layer.

**Decision:** All `GameRoom` mutations go through `GameService` methods. The handler never touches a `GameRoom` field directly. Key encapsulated operations:
- `finishGame(roomId)` — atomic status transition (prevents double-processing)
- `markAsDraw(roomId)` — alias for `finishGame` used for draw agreements
- `recordGameStart(roomId)` — idempotent clock initialisation (safe to call on reconnect)
- `abandonGame(roomId, playerId)` — returns forfeit result if game was in progress

### Concurrency: Per-Room Locking for Compound Operations

**Problem (before):** `GameRoom` is mutable; `makeMove()`, `resign()`, and `abandonGame()` were compound read-modify-write sequences on shared mutable state without any lock, creating race conditions.

**Decision:** All compound operations in `GameService` use `synchronized(room)` as the per-room lock. `ConcurrentHashMap` covers map-level atomicity; `synchronized(room)` covers the multi-field operations within a single room. `GameRoom.moves` is a `CopyOnWriteArrayList` (safe for concurrent iteration during broadcasts). `GameRoom.spectators` is a `ConcurrentHashMap`.

### Atomic Match Claiming in findMatch()

**Problem (before):** `findMatch()` had a TOCTOU race: two sessions could both see each other as opponents, both claim a match, and create two rooms.

**Decision:** Use `matchmakingQueue.remove(opponent.sessionId)` as the atomic claim step. If it returns null, another thread already claimed this opponent — bail out. Only then remove self from the queue.

### finishRoom() Ordering

`grantXpForGameOver()` must always be called **before** `finishRoom()` because XP granting calls `gameService.getRoom()` and `gameService.roomHasBot()` — both need the room to still exist. `finishRoom()` removes the room, cancels the bot job, and clears `roomSessions`.

### Abandon vs. Resign

`handleRoomAbandon` was previously silent — it removed the player but sent no notification to the opponent and reset status to WAITING even for in-progress games. Now: if the game was PLAYING, `abandonGame()` returns a forfeit result and the handler broadcasts `GAME_OVER` (reason: "abandonment"), grants XP to the opponent, and calls `finishRoom()`. Only pre-game abandons reset to WAITING.

### roomSessions Cleanup

Every `finishRoom()` call (resign, draw, checkmate, timeout, abandonment) now removes the entry from `roomSessions` in `GameWebSocketHandler`. Bot-initiated game-over uses `roomCleanupCallback` (set in `init`) to achieve the same. Previously, finished rooms accumulated in `roomSessions` indefinitely.

### Game Clock Initialisation (Reconnect Safety)

`lastMoveTimestamp` is now set exactly once via `gameService.recordGameStart(roomId)`, which is idempotent (guarded by `room.gameStarted`). Previously, `handleRoomJoin` reset the timestamp on every join — including reconnects — gifting extra time to the active player.

## Package

`com.charmflex.app.mobile_chinese_chess_multiplayer` (client) / `com.charmflex.xiangqi.server` (server) / `com.charmflex.xiangqi.engine` (game engine)

## Tech Stack

- Kotlin 2.3.0, Compose Multiplatform 1.10.0, AGP 8.11.2
- Android: compileSdk 36, minSdk 24, targetSdk 36, JVM 11
- Server: Spring Boot + Ktor WebSocket + kotlinx.serialization
- Client networking: Ktor HTTP client + WebSocket
- Auth: Supabase (Google OAuth) + UUID-based guest auth
- DI: Koin with KSP annotation processing
