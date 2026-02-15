╭─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────╮
│ Plan to implement                                                                                                                                                   │
│                                                                                                                                                                     │
│ Chinese Chess Multiplayer Game - Implementation Plan                                                                                                                │
│                                                                                                                                                                     │
│ Context                                                                                                                                                             │
│                                                                                                                                                                     │
│ Build a KMM Chinese Chess (Xiangqi) game with online multiplayer, AI bot, friends system, and in-game chat. The UI reference designs (4 screens) exist in           │
│ chinese-chess-ui-references/. The composeApp/ module is currently boilerplate that needs to be replaced with the full game.                                         │
│                                                                                                                                                                     │
│ Key Decisions:                                                                                                                                                      │
│ - Backend: Kotlin Spring Boot + WebSocket (separate server project)                                                                                                 │
│ - AI Bot: Built-in Kotlin minimax/alpha-beta engine in shared code                                                                                                  │
│ - Priority: Local playable game first, then expand                                                                                                                  │
│                                                                                                                                                                     │
│ ---                                                                                                                                                                 │
│ Package Structure                                                                                                                                                   │
│                                                                                                                                                                     │
│ All shared code under composeApp/src/commonMain/kotlin/com/charmflex/app/mobile_chinese_chess_multiplayer/:                                                         │
│                                                                                                                                                                     │
│ core/                                                                                                                                                               │
│   navigation/    -- Route sealed class, AppNavigation composable, bottom nav                                                                                        │
│   theme/         -- Color, Type, Theme (ported from references)                                                                                                     │
│   di/            -- Manual DI container                                                                                                                             │
│   util/          -- Clock, SoundManager (expect/actual)                                                                                                             │
│                                                                                                                                                                     │
│ domain/                                                                                                                                                             │
│   model/         -- Position, Piece, Board, Move, GameState, Player, Room, Friend, ChatMessage                                                                      │
│   engine/        -- PieceRules, MoveGenerator, MoveValidator, GameRules                                                                                             │
│   ai/            -- Evaluator, AiEngine, AiDifficulty, ZobristHash                                                                                                  │
│                                                                                                                                                                     │
│ data/                                                                                                                                                               │
│   repository/    -- GameRepository, UserRepository, FriendRepository, ChatRepository                                                                                │
│   local/         -- GameLocalDataSource, UserPreferences                                                                                                            │
│   remote/        -- ApiClient (Ktor HTTP), WebSocketClient (Ktor WS), dto/                                                                                          │
│                                                                                                                                                                     │
│ presentation/                                                                                                                                                       │
│   common/        -- ChinesePatternBackground, BottomNavigationBar, XiangqiButton, PlayerAvatar, TimerDisplay, RatingBadge                                           │
│   mainmenu/      -- MainMenuScreen, ViewModel, components (ProfileHeader, XPProgressBar, SearchSection, NavigationGrid, RecentActivities)                           │
│   battlelobby/   -- BattleLobbyScreen, ViewModel, components (MatchmakingRadar, RoomCard, ActiveRoomsList, etc.)                                                    │
│   gameroom/      -- GameRoomScreen, ViewModel, components (XiangqiBoard, PieceComposable, MoveHighlight, MoveLog, ChatPanel, OpponentHeader, UserFooter,            │
│ GameActionButtons, GameOverDialog, AiDifficultySelector)                                                                                                            │
│   social/        -- SocialScreen, ViewModel, components (FriendCard, FriendTabs, FriendRequestCard, etc.)                                                           │
│   settings/      -- SettingsScreen, ViewModel                                                                                                                       │
│                                                                                                                                                                     │
│ ---                                                                                                                                                                 │
│ Game Engine Design                                                                                                                                                  │
│                                                                                                                                                                     │
│ Board: 10x9 grid as Array<Array<Piece?>>, row 0 = black's back rank (top), row 9 = red's back rank                                                                  │
│                                                                                                                                                                     │
│ 7 Piece Types with rules:                                                                                                                                           │
│                                                                                                                                                                     │
│ - General (帥/將): 1 step orthogonal, confined to palace. Flying general rule (cannot face opposing general on open file)                                           │
│ - Advisor (仕/士): 1 step diagonal, confined to palace                                                                                                              │
│ - Elephant (相/象): 2 steps diagonal, cannot cross river, blocked by intervening piece                                                                              │
│ - Horse (馬): L-shape, blocked by adjacent orthogonal piece (horse's leg)                                                                                           │
│ - Chariot (車): Unlimited orthogonal, blocked by first piece                                                                                                        │
│ - Cannon (炮): Moves like chariot, captures by jumping exactly one piece                                                                                            │
│ - Soldier (兵/卒): Forward only before river; forward/left/right after river                                                                                        │
│                                                                                                                                                                     │
│ AI Engine: Minimax with alpha-beta pruning                                                                                                                          │
│                                                                                                                                                                     │
│ - Difficulty levels via search depth (1-8 ply)                                                                                                                      │
│ - Material + positional evaluation (piece-square tables)                                                                                                            │
│ - Move ordering (captures first), iterative deepening, Zobrist hashing for transposition table                                                                      │
│ - Runs on Dispatchers.Default, beginner adds random noise                                                                                                           │
│                                                                                                                                                                     │
│ ---                                                                                                                                                                 │
│ Networking                                                                                                                                                          │
│                                                                                                                                                                     │
│ Client: Ktor HTTP + WebSocket                                                                                                                                       │
│                                                                                                                                                                     │
│ - REST for auth, profile, rooms, friends, leaderboard                                                                                                               │
│ - WebSocket for real-time: moves, matchmaking, chat, presence                                                                                                       │
│                                                                                                                                                                     │
│ WebSocket Messages (sealed class):                                                                                                                                  │
│                                                                                                                                                                     │
│ - Game: MakeMove, MoveMade, GameOver, UndoRequest/Response, DrawOffer/Response, Resign                                                                              │
│ - Chat: ChatSend, ChatReceive                                                                                                                                       │
│ - Matchmaking: QueueJoin, QueueUpdate, MatchFound, QueueLeave                                                                                                       │
│ - Presence: FriendOnline, FriendOffline                                                                                                                             │
│ - Room: RoomJoined, OpponentJoined, TimerUpdate                                                                                                                     │
│                                                                                                                                                                     │
│ Server (Spring Boot, separate project):                                                                                                                             │
│                                                                                                                                                                     │
│ - REST controllers: Auth, Profile, Room, Friend, Leaderboard                                                                                                        │
│ - WebSocket handler: connection registry, matchmaking queue, game room management, chat relay, presence broadcast                                                   │
│ - Server-authoritative: validates all moves using same game logic                                                                                                   │
│ - Database: PostgreSQL (users, games, rooms, friend_relations, chat_messages)                                                                                       │
│                                                                                                                                                                     │
│ ---                                                                                                                                                                 │
│ Implementation Phases                                                                                                                                               │
│                                                                                                                                                                     │
│ Phase 1: Game Engine + Local 2-Player (Start Here)                                                                                                                  │
│                                                                                                                                                                     │
│ Goal: Fully functional Xiangqi rules + playable board                                                                                                               │
│                                                                                                                                                                     │
│ Create:                                                                                                                                                             │
│ - domain/model/ — Position, Piece (PieceType, PieceColor), Board (10x9 grid, initial setup, FEN), Move, GameState                                                   │
│ - domain/engine/ — PieceRules (all 7 pieces), MoveGenerator, MoveValidator, GameRules (check, checkmate, stalemate, flying general)                                 │
│ - core/theme/ — Color.kt, Type.kt, Theme.kt (port from chinese-chess-ui-references/app/src/main/java/com/example/xiangqi/ui/theme/)                                 │
│ - presentation/gameroom/ — GameRoomScreen, GameRoomViewModel (LOCAL_2P mode), XiangqiBoard (interactive Canvas), PieceComposable, MoveHighlight, MoveLog,           │
│ GameOverDialog                                                                                                                                                      │
│ - commonTest/ — PieceRulesTest, MoveGeneratorTest, GameRulesTest, BoardTest                                                                                         │
│ - Replace App.kt with theme + GameRoomScreen                                                                                                                        │
│                                                                                                                                                                     │
│ KMP porting notes: Reference XiangqiBoard uses android.graphics.Paint for river text — replace with compose Text overlay. Modifier.border(top = BorderStroke(...))  │
│ isn't standard — use drawBehind for partial borders.                                                                                                                │
│                                                                                                                                                                     │
│ Phase 2: AI Bot                                                                                                                                                     │
│                                                                                                                                                                     │
│ Goal: Play vs computer at 8 difficulty levels                                                                                                                       │
│                                                                                                                                                                     │
│ Create:                                                                                                                                                             │
│ - domain/ai/ — Evaluator, AiEngine, AiDifficulty, ZobristHash                                                                                                       │
│ - presentation/gameroom/components/AiDifficultySelector.kt                                                                                                          │
│ - Tests: EvaluatorTest, AiEngineTest                                                                                                                                │
│                                                                                                                                                                     │
│ Modify: GameRoomViewModel (add VS_AI mode), App.kt (mode selection)                                                                                                 │
│                                                                                                                                                                     │
│ Phase 3: UI Scaffold (All 4 Screens)                                                                                                                                │
│                                                                                                                                                                     │
│ Goal: Port all reference screens to KMP with navigation, using mock data                                                                                            │
│                                                                                                                                                                     │
│ Create:                                                                                                                                                             │
│ - core/navigation/ — Route.kt, AppNavigation.kt                                                                                                                     │
│ - presentation/common/ — all shared widgets (6 components)                                                                                                          │
│ - presentation/mainmenu/ — screen + viewmodel + 5 sub-components                                                                                                    │
│ - presentation/battlelobby/ — screen + viewmodel + 6 sub-components                                                                                                 │
│ - presentation/social/ — screen + viewmodel + 5 sub-components                                                                                                      │
│ - presentation/settings/ — screen + viewmodel                                                                                                                       │
│ - Extract OpponentHeader, UserFooter, ChatPanel, GameActionButtons from GameRoomScreen                                                                              │
│                                                                                                                                                                     │
│ Modify: App.kt (replace with AppNavigation), GameRoomScreen (refactor to use extracted components)                                                                  │
│                                                                                                                                                                     │
│ Phase 4: Backend Server + Online Multiplayer                                                                                                                        │
│                                                                                                                                                                     │
│ Goal: Real-time online play                                                                                                                                         │
│                                                                                                                                                                     │
│ Create:                                                                                                                                                             │
│ - data/remote/ — ApiClient, WebSocketClient, all DTOs                                                                                                               │
│ - data/repository/ — GameRepository, UserRepository                                                                                                                 │
│ - data/local/ — GameLocalDataSource                                                                                                                                 │
│ - core/di/AppModule.kt                                                                                                                                              │
│ - Spring Boot server project (separate repo)                                                                                                                        │
│                                                                                                                                                                     │
│ Modify: GameRoomViewModel (ONLINE mode), BattleLobbyViewModel (real matchmaking), build.gradle.kts (add Ktor + serialization deps)                                  │
│                                                                                                                                                                     │
│ Phase 5: Social Features                                                                                                                                            │
│                                                                                                                                                                     │
│ Goal: Friends, invites, chat                                                                                                                                        │
│                                                                                                                                                                     │
│ Create: FriendRepository, ChatRepository, FriendRequestCard                                                                                                         │
│                                                                                                                                                                     │
│ Modify: SocialViewModel (real API), FriendCard (invite), ChatPanel (real chat), WebSocketClient (presence events)                                                   │
│                                                                                                                                                                     │
│ Phase 6: Polish                                                                                                                                                     │
│                                                                                                                                                                     │
│ Goal: Rankings, XP, animations, sound                                                                                                                               │
│                                                                                                                                                                     │
│ Create: RankingsScreen, ProfileScreen, SoundManager, HapticFeedback                                                                                                 │
│                                                                                                                                                                     │
│ Modify: XiangqiBoard (piece movement animations), XPProgressBar (animate), MainMenuViewModel (real data), navigation (new routes)                                   │
│                                                                                                                                                                     │
│ ---                                                                                                                                                                 │
│ Verification                                                                                                                                                        │
│                                                                                                                                                                     │
│ After each phase:                                                                                                                                                   │
│ - Phase 1: ./gradlew :composeApp:testDebugUnitTest passes. Run on Android emulator — can play a full game of chess with correct rules, checkmate detection works.   │
│ - Phase 2: Tests pass. Can play vs AI at different difficulties. AI responds within 3 seconds.                                                                      │
│ - Phase 3: All 4 screens render. Bottom nav works. Build succeeds on both Android (./gradlew :composeApp:assembleDebug) and iOS (Xcode build).                      │
│ - Phase 4: Can connect to server, matchmake, and play a full online game with moves syncing in real-time.                                                           │
│ - Phase 5: Can add friends, see online status, invite to game, chat during game.                                                                                    │
│ - Phase 6: Smooth animations, sound on move/capture/check, leaderboard populated.    