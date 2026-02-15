# Quick Start: Multiplayer on Two Devices

## Prerequisites
- Java 17+ (for server)
- Android Studio (for building the app)
- Two Android devices/emulators on the same network

## Step 1: Find Your Server IP

On your computer (the machine running the server), find your local network IP:

```bash
# macOS
ifconfig | grep "inet " | grep -v 127.0.0.1

# Linux
hostname -I

# Windows
ipconfig
```

Look for an IP like `192.168.x.x` or `10.0.x.x`. This is your **SERVER_IP**.

## Step 2: Configure the App to Use Your Server IP

Edit `composeApp/src/commonMain/kotlin/.../core/di/AppModule.kt`:

```kotlin
// Change these lines to your server's LAN IP:
const val DEFAULT_HTTP_URL = "http://<SERVER_IP>:8080"
const val DEFAULT_WS_URL = "ws://<SERVER_IP>:8080/ws"
```

For example, if your IP is `192.168.1.100`:
```kotlin
const val DEFAULT_HTTP_URL = "http://192.168.1.100:8080"
const val DEFAULT_WS_URL = "ws://192.168.1.100:8080/ws"
```

> **Note:** The default `10.0.2.2` only works for Android Emulator talking to the host machine. For real devices or cross-device play, you MUST use your LAN IP.

## Step 3: Start the Server

```bash
cd server
./gradlew bootRun
```

You should see:
```
Started XiangqiServerApplicationKt in X.XXX seconds
```

Verify it's running:
```bash
curl http://localhost:8080/api/rooms
# Should return: {"rooms":[]}
```

## Step 4: Build and Install the App

```bash
cd /path/to/MobileChineseChessMultiplayer
./gradlew :composeApp:assembleDebug
```

Install on both devices:
```bash
adb -s <device1_serial> install composeApp/build/outputs/apk/debug/composeApp-debug.apk
adb -s <device2_serial> install composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

Or install via Android Studio (Run > Select Device).

## Step 5: Play!

### Option A: Room-based (Create & Join)

1. **Device 1**: Open app > "Battle Lobby" > tap "Create Room"
   - This creates a room and waits for an opponent
2. **Device 2**: Open app > "Battle Lobby" > tap "Refresh" to see the room > tap the room to join
3. Game starts automatically when both players are connected

### Option B: Matchmaking

1. **Device 1**: Open app > "Battle Lobby" > tap "Find Match"
2. **Device 2**: Open app > "Battle Lobby" > tap "Find Match"
3. Server matches both players and game starts

## Troubleshooting

### Check Logs

**Server logs** appear in the terminal where `./gradlew bootRun` is running. Look for:
- `[API]` - REST API calls (guest login, room creation)
- `[SVC]` - GameService operations (player/room management)
- `[WS]` - WebSocket events (connections, messages, broadcasts)

**Android logs** via logcat:
```bash
adb logcat | grep -E "\[(API|WS|USER|LOBBY|GAME)\]"
```

Log prefixes on the client:
- `[API]` - HTTP requests/responses
- `[WS]` - WebSocket connect/send/receive
- `[USER]` - Login flow
- `[LOBBY]` - Matchmaking & room management
- `[GAME]` - In-game events & moves

### Common Issues

| Symptom | Cause | Fix |
|---------|-------|-----|
| "Failed to connect" on app | Wrong server IP | Check `AppModule.kt` URLs match your LAN IP |
| App hangs on "Waiting..." | Server not running | Start server with `./gradlew bootRun` |
| Connection works on emulator but not real device | Using `10.0.2.2` | Change to your LAN IP in `AppModule.kt` |
| Server rejects WebSocket | Token not found | Check server logs for `[SVC] registerPlayerByToken: token ... not found` |
| Moves not syncing | WS session not in room | Check server logs for `[WS] room_join` and `Room X sessions: 2` |
| "Cannot join room" | Room already full or finished | Create a new room |

### Verify Network Connectivity

From your Android device, verify it can reach the server:
```bash
# In a terminal app on the device, or via adb shell:
ping <SERVER_IP>
curl http://<SERVER_IP>:8080/api/rooms
```

### Firewall

Make sure port 8080 is open on your server machine:
```bash
# macOS - temporarily allow incoming connections
sudo pfctl -d  # disable firewall (re-enable after testing)

# Or add a rule for port 8080
```

On Linux:
```bash
sudo ufw allow 8080
```
