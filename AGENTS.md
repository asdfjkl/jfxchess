# AGENTS.md

## Project overview

JFXChess is a GPL-2.0-or-later cross-platform chess GUI. It is a Java 21
Swing application built with Maven. The runnable application entry point is
`org.asdfjkl.jfxchess.gui.App`; Maven packages an executable fat JAR at
`target/jfxchess-5.0-jar-with-dependencies.jar`.

The application supports tabbed game sessions, UCI engine integration
(bundled Stockfish 18 and Stockfish 5 bots), opening book exploration
(Polyglot binary format), and transactional PGN database editing.

## Repository layout

| Path | Purpose |
| --- | --- |
| `src/main/java/org/asdfjkl/jfxchess/lib` | Chess domain logic: 10x12 mailbox board, legal move generation, game tree (`Game`, `GameNode`), FEN/PGN parsing/printing, Polyglot opening books, and test suite. Keep this package independent of Swing. |
| `src/main/java/org/asdfjkl/jfxchess/gui` | Swing presentation and application layer. Manages workspaces, tabbed game sessions, controllers, dialogs, engine processes, and user preferences. |
| `src/main/resources` | Runtime classpath assets: SVG/PNG icons, piece vector sets (Merida, Old, USCF), and bot portraits. Preserve paths and attributions. |
| `build` | Platform packaging scripts and configurations: Linux `.deb`, generic JAR, Windows `jpackage` / Inno Setup (`winSetup.iss`), Windows MSIX (`build_msix.bat`), and Snap (`build_snap`). |
| `docs` | Static GitHub Pages documentation and project website. |
| `target` | Maven build output; generated and ignored. Never edit or commit it. |

## Core architecture

### Workspace and session model

The GUI is structured around a clean, tabbed session and workspace architecture:

- **`ApplicationModel`**: Top-level model holding the shared `Workspace`, user
  preferences, default board styles, and theme settings.
- **`Workspace`**: Manages the collection of active `GameSession` instances,
  the active session pointer, and a canonical cache of open `PgnDocument`
  instances.
- **`GameSession`**: Owns per-game/per-tab mutable state:
  - The `Game` variation tree.
  - Per-session UI states: board orientation (`flipBoard`), mode (`MODE_ENTER_MOVES`,
    `MODE_ANALYSIS`, `MODE_PLAY_WHITE`, etc.), engine evaluation metrics, and
    thinking time.
  - Session lifecycle flags: `dirty` (user has unpersisted changes), `stale`
    (underlying file was modified externally or by another tab), and
    `pgnSourceReference` (identifying file path, `PgnGameId`, and document revision).
  - Dedicated `EngineSession` worker.
- **`Model_JFXChess`**: Compatibility and coordinator facade that delegates
  active session operations to `workspace.getActiveSession()` while exposing
  global settings (themes, custom font sizes, global engine catalogue, screen geometry).
- **`View_GameTab`**: Self-contained Swing panel for each tab, hosting the
  chessboard (`View_Chessboard`), move list (`View_Moves`), opening book (`View_Book`),
  evaluation bar (`View_Eval`), and engine output (`View_EngineOutput`).
- **`Controller_Board`**: Bound to a specific tab's `GameSession` (`new Controller_Board(model, session)`),
  ensuring user board interactions (clicks, drags, moves) modify only that tab's game tree.
- **Tabbed event routing**: In the single-window tabbed interface, only the active
  tab is visible and interactable. Tab switching synchronizes `workspace.setActiveSession()`
  and `model.setGameSession()`, guaranteeing menus, toolbars, shortcuts, and views
  always operate on the currently active tab.

### Concurrency and engine integration

- **Engine lifecycle (`EngineSession` & `EngineThread`)**: Each `GameSession`
  lazily creates its own `EngineSession`, which spawns a dedicated daemon
  `EngineThread`. Commands are dispatched asynchronously via a `BlockingQueue<String>`,
  communicating over standard UCI protocol streams. Closing a session cleanly stops
  its engine thread.
- **Thread safety**:
  - Chess domain structures (`Board`, `Game`, `GameNode`) are mutable and NOT
    thread-safe. They must only be accessed or modified on the Swing Event
    Dispatch Thread (EDT).
  - Background operations (PGN file scanning, game replacements, engine output parsing)
    must run off the EDT (via `SwingWorker`, `CompletableFuture`, or `EngineThread`)
    and publish results back to the EDT via `SwingUtilities.invokeLater` or
    property change listeners.

### PGN document management and file synchronization

- **`PgnDocument`**: Provides transactional, indexed access to PGN files.
  Games are keyed by unique `PgnGameId` with computed SHA fingerprints and
  monotonically increasing document revisions.
- **Atomic file writes**: File modifications (`replaceGame`, `appendGame`,
  `deleteGame`) write to temporary files with atomic move fallbacks and notify
  registered `PgnDocumentListener`s.
- **Synchronization semantics**:
  - When a document changes, `Workspace` notifies affected sessions.
  - If a session is **clean**, it automatically reloads the updated game from the document.
  - If a session is **dirty** (user modified), it is marked **stale** rather than
    overwritten, allowing the user to resolve conflicts without losing work.

## Build and run

- **Prerequisites**: JDK 21. Configured with `maven.compiler.release=21`.
- **Compile and package**:
  ```powershell
  mvn clean package
  ```
  Produces `target/jfxchess-5.0.jar` and `target/jfxchess-5.0-jar-with-dependencies.jar`.
- **Launch GUI**:
  ```powershell
  java -jar target/jfxchess-5.0-jar-with-dependencies.jar
  ```
- **Windows development note**: If Maven is not installed in the system PATH,
  the IntelliJ IDEA bundled Maven binary can be used:
  `"C:\Program Files\JetBrains\IntelliJ IDEA Community Edition ...\plugins\maven\lib\maven3\bin\mvn.cmd"`.

## Development conventions

- **Code style**: Four-space indentation, braces on the same line (K&R style),
  explicit types where existing code uses them. Do not reformat unrelated files.
- **GPL license headers**: Preserve the GNU GPL header at the top of all Java
  source files. Use copyright year `2020-2026 Dominik Klein` on new or modified files.
- **Component separation**:
  - Keep chess domain logic (`Board`, `Move`, `Game`, `PgnReader`, `PgnPrinter`, `Polyglot`)
    strictly in `org.asdfjkl.jfxchess.lib` with zero Swing/AWT UI imports.
  - Keep UI components, dialogs, controllers, preferences, and engine process execution
    in `org.asdfjkl.jfxchess.gui`.
- **State updates**: Route user actions through session-scoped controllers (`Controller_Board`)
  and coordinator controllers (`Controller_Engine`, `Controller_Pgn`, `Controller_UI`).
  Notify views via `PropertyChangeListener` events.
- **Preferences persistence**: `Model_JFXChess` stores preferences under
  `/org/asdfjkl/jfxchess` in Java Preferences (`Preferences.userRoot().node(...)`).
  Maintain backward compatibility for preference keys and respect `modelVersion` (currently `501`).
- **Asset path resolution**: Internal engine (`engine/stockfish.exe`, `engine/stockfish5.exe`),
  opening book (`book/extbook.bin`), and splash assets resolve relative to the packaged
  JAR directory (`Model_JFXChess.getJarPath()`). Keep packaging script layouts aligned.

## Packaging

- Run packaging scripts from within the `build/` directory:
  - Linux DEB: `build/build_deb.sh`
  - Generic JAR: `build/build_jar.sh`
  - Windows Inno Setup & jpackage: `build/build_win.bat` (creates installer via `winSetup.iss`)
  - Windows MSIX: `build/build_msix.bat`
  - Linux Snap: `build/build_snap.sh` (`build/build_snap/snapcraft.yaml`)
- Keep version numbers synchronized across `pom.xml`, `README.md`, `build_snap/snapcraft.yaml`,
  `winSetup.iss`, and `Model_JFXChess.modelVersion`.
- Do not check in generated packaging directories (`build/build_win/output`,
  `build/build_win/jar`, `build/build_msix/output`).

## Change validation and testing

All verification tests and test runner logic reside under `src/test/java/org/asdfjkl/jfxchess/lib/`
(`TestCases.java` and `Main.java`), and test database fixtures reside under `src/test/resources/`.
They are excluded from the production distribution JAR.

Compile test classes via:
```powershell
mvn test-compile
```

### Fast regression test suite

Always run the focused, sub-second test suites after modifying session,
workspace, PGN, SCID5, or controller logic:

```powershell
# Verify database lifecycle, detachment, and modified timestamp tracking (PGN & SCID5)
java -cp "target/test-classes;target/classes;target/jfxchess-5.0-jar-with-dependencies.jar" org.asdfjkl.jfxchess.lib.Main database-lifecycle-test

# Verify session isolation, dirty tracking, and engine shutdown
java -cp "target/test-classes;target/classes;target/jfxchess-5.0-jar-with-dependencies.jar" org.asdfjkl.jfxchess.lib.Main workspace-session-isolation-test

# Verify transactional PGN document reloading, stale tracking, and cross-session sync
java -cp "target/test-classes;target/classes;target/jfxchess-5.0-jar-with-dependencies.jar" org.asdfjkl.jfxchess.lib.Main pgn-document-session-synchronization-test

# Verify browser-like tab context switching, left-tab selection, and single tab protection
java -cp "target/test-classes;target/classes;target/jfxchess-5.0-jar-with-dependencies.jar" org.asdfjkl.jfxchess.lib.Main browser-tab-behavior-test

# Verify PGN player surname extraction and versus title formatting
java -cp "target/test-classes;target/classes;target/jfxchess-5.0-jar-with-dependencies.jar" org.asdfjkl.jfxchess.lib.Main pgn-game-info-surname-test

# Run all session tests in one suite
java -cp "target/test-classes;target/classes;target/jfxchess-5.0-jar-with-dependencies.jar" org.asdfjkl.jfxchess.lib.Main --run-session-tests

# Run SCID5 tests in one suite
java -cp "target/test-classes;target/classes;target/jfxchess-5.0-jar-with-dependencies.jar" org.asdfjkl.jfxchess.lib.Main --scid-tests
```

### Full / legacy test suite

- Run `java -cp "target/test-classes;target/classes;target/jfxchess-5.0-jar-with-dependencies.jar" org.asdfjkl.jfxchess.lib.Main --all-tests`
  when validating core chess domain changes (FEN parsing, deep perft move generation,
  SAN, Zobrist hashing, and large PGN database scanning).
- For GUI and theme modifications, manually test the workflow (tab opening/switching,
  engine analysis, and preference saving).
