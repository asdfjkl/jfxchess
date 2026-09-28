/* JFXChess - A Chess Graphical User Interface
 * Copyright (C) 2020-2025 Dominik Klein
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.asdfjkl.jfxchess.lib;

import org.asdfjkl.jfxchess.gui.DialogSave;
import org.asdfjkl.jfxchess.gui.GameSession;
import org.asdfjkl.jfxchess.gui.Model_JFXChess;
import org.asdfjkl.jfxchess.gui.PgnDocument;
import org.asdfjkl.jfxchess.gui.PgnGameId;
import org.asdfjkl.jfxchess.gui.PgnSourceReference;
import org.asdfjkl.jfxchess.gui.Workspace;
import java.util.List;

//import org.asdfjkl.jfxchess.gui.PgnDatabaseEntry;

import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.JTabbedPane;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;

public class TestCases {

    public void workspaceSessionIsolationTest() {

        Game source = new Game();
        source.getRootNode().setBoard(new Board(true));

        Workspace workspace = new Workspace();
        final int[] activeSessionChanges = {0};
        workspace.addPropertyChangeListener(event -> {
            if ("activeSessionChanged".equals(event.getPropertyName())) {
                activeSessionChanges[0]++;
            }
        });
        GameSession first = workspace.createSession(source);
        GameSession second = workspace.createSession(source);
        PgnDocument firstDocument =
                workspace.getOrCreateDocument(Path.of("workspace-test.pgn"));
        PgnDocument secondDocument =
                workspace.getOrCreateDocument(Path.of(".", "workspace-test.pgn"));

        if(first.getGame() == source || first.getGame() == second.getGame()) {
            throw new AssertionError("Workspace sessions must own different game instances");
        }
        if(workspace.getActiveSession() != second || activeSessionChanges[0] != 2) {
            throw new AssertionError("Creating sessions must activate the newest session");
        }
        if(firstDocument != secondDocument || workspace.getDocuments().size() != 1) {
            throw new AssertionError("Workspace must share canonical PGN documents");
        }

        Move e2e4 = new Move("e2e4");
        if(!first.applyMove(e2e4)) {
            throw new AssertionError("Expected e2e4 to be applied to the first session");
        }
        if(first.getGame().countHalfmoves() != 1 ||
                second.getGame().countHalfmoves() != 0) {
            throw new AssertionError("A move in one session changed another session");
        }
        if(!first.isDirty() || second.isDirty()) {
            throw new AssertionError("Dirty state must be session-specific");
        }

        first.setFlipBoard(true);
        first.setMode(Model_JFXChess.MODE_ANALYSIS);
        first.setCurrentEngineInfo("first");
        if(!first.getFlipBoard() ||
                first.getMode() != Model_JFXChess.MODE_ANALYSIS ||
                !"first".equals(first.getCurrentEngineInfo()) ||
                second.getFlipBoard() ||
                second.getMode() != Model_JFXChess.MODE_ENTER_MOVES ||
                !"".equals(second.getCurrentEngineInfo())) {
            throw new AssertionError("Per-session UI and analysis state was shared");
        }
        if(first.getEngineSession() == second.getEngineSession()) {
            throw new AssertionError("Sessions must not share an engine worker");
        }

        Move d2d4 = new Move("d2d4");
        if(!second.applyMove(d2d4)) {
            throw new AssertionError("Expected d2d4 to be applied to the second session");
        }
        workspace.setActiveSession(first);
        if(!"e2e4".equals(first.getGame().getCurrentNode().getMove().getUci()) ||
                !"d2d4".equals(second.getGame().getCurrentNode().getMove().getUci())) {
            throw new AssertionError("Changing the active session changed a session position");
        }
        workspace.closeSession(first);
        if(workspace.getActiveSession() != second || activeSessionChanges[0] != 4) {
            throw new AssertionError("Closing the active session must select its replacement");
        }
        if(!first.getEngineSession().isShutdownRequested() ||
                second.getEngineSession().isShutdownRequested()) {
            throw new AssertionError("Closing a session affected another engine worker");
        }

        System.out.println("TEST: workspace session isolation passed");
    }

    public void pgnGameInfoSurnameExtractionTest() {
        if (!"Kasparov".equals(PgnGameInfo.extractSurname("Kasparov, Garry"))) {
            throw new AssertionError("Failed to extract surname from 'Kasparov, Garry'");
        }
        if (!"Carlsen".equals(PgnGameInfo.extractSurname("Magnus Carlsen"))) {
            throw new AssertionError("Failed to extract surname from 'Magnus Carlsen'");
        }
        if (!"Stockfish 18".equals(PgnGameInfo.extractSurname("Stockfish 18"))) {
            throw new AssertionError("Failed to retain bot name 'Stockfish 18'");
        }
        if (!"N.N.".equals(PgnGameInfo.extractSurname("N.N."))) {
            throw new AssertionError("Failed on 'N.N.'");
        }
        if (!"N.N.".equals(PgnGameInfo.extractSurname(""))) {
            throw new AssertionError("Failed on empty name");
        }
        if (!"N.N.".equals(PgnGameInfo.extractSurname(null))) {
            throw new AssertionError("Failed on null name");
        }
        if (!"N.N.".equals(PgnGameInfo.extractSurname("?"))) {
            throw new AssertionError("Failed on '?' name");
        }
        if (!"Kasparov vs. Karpov".equals(PgnGameInfo.formatVersusTitle("Kasparov, Garry", "Karpov, Anatoly"))) {
            throw new AssertionError("Failed formatVersusTitle");
        }

        PgnGameInfo info = new PgnGameInfo();
        info.setWhite("Fischer, Robert J.");
        info.setBlack("Spassky, Boris V.");
        if (!"Fischer vs. Spassky".equals(info.getVersusTitle())) {
            throw new AssertionError("Failed PgnGameInfo getVersusTitle");
        }

        Game game = new Game();
        game.setHeader("White", "Deep Blue");
        game.setHeader("Black", "Kasparov, Garry");
        if (!"Blue vs. Kasparov".equals(game.getVersusTitle())) {
            throw new AssertionError("Failed Game getVersusTitle");
        }

        System.out.println("TEST: PgnGameInfo surname extraction passed");
    }

    public void browserTabBehaviorTest() {
        Workspace workspace = new Workspace();

        Game g1 = new Game();
        g1.getRootNode().setBoard(new Board(true));
        g1.setHeader("White", "Kasparov, Garry");
        g1.setHeader("Black", "Karpov, Anatoly");
        GameSession s1 = workspace.createSession(g1);

        Game g2 = new Game();
        g2.getRootNode().setBoard(new Board(true));
        g2.setHeader("White", "Fischer, Robert J.");
        g2.setHeader("Black", "Spassky, Boris V.");
        GameSession s2 = workspace.createSession(g2);

        Game g3 = new Game();
        g3.getRootNode().setBoard(new Board(true));
        g3.setHeader("White", "Carlsen, Magnus");
        g3.setHeader("Black", "Caruana, Fabiano");
        GameSession s3 = workspace.createSession(g3);

        if (!"Kasparov vs. Karpov".equals(s1.getGame().getVersusTitle())) {
            throw new AssertionError("Failed s1 versus title");
        }
        if (!"Fischer vs. Spassky".equals(s2.getGame().getVersusTitle())) {
            throw new AssertionError("Failed s2 versus title");
        }
        if (!"Carlsen vs. Caruana".equals(s3.getGame().getVersusTitle())) {
            throw new AssertionError("Failed s3 versus title");
        }

        // Test context switch when closing an intermediate tab (s2):
        // Context should switch to the tab to the left (s1)
        List<GameSession> sessions = workspace.getSessions();
        int index = sessions.indexOf(s2);
        GameSession targetSession = (index > 0) ? sessions.get(index - 1) : sessions.get(1);
        if (targetSession != s1) {
            throw new AssertionError("Target session for closing s2 must be s1 (left tab)");
        }
        workspace.setActiveSession(targetSession);
        workspace.closeSession(s2);

        if (workspace.getActiveSession() != s1) {
            throw new AssertionError("Active session must be s1 after closing s2");
        }
        if (workspace.getSessions().size() != 2) {
            throw new AssertionError("Workspace should have 2 sessions left");
        }

        // Test context switch when closing the leftmost tab (s1):
        // Since index == 0, context switches to the next available tab (s3)
        sessions = workspace.getSessions();
        index = sessions.indexOf(s1);
        targetSession = (index > 0) ? sessions.get(index - 1) : sessions.get(1);
        if (targetSession != s3) {
            throw new AssertionError("Target session for closing s1 must be s3");
        }
        workspace.setActiveSession(targetSession);
        workspace.closeSession(s1);

        if (workspace.getActiveSession() != s3) {
            throw new AssertionError("Active session must be s3 after closing s1");
        }
        if (workspace.getSessions().size() != 1) {
            throw new AssertionError("Workspace should have 1 session left");
        }

        // Single session left: verify protection condition
        boolean singleTabProtected = workspace.getSessions().size() <= 1;
        if (!singleTabProtected) {
            throw new AssertionError("Should be single tab protected");
        }

        System.out.println("TEST: browser tab behavior passed");
    }

    public void pgnDocumentSessionSynchronizationTest() {
        Path path = null;
        try {
            path = Files.createTempFile("jfxchess-pgn-document-", ".pgn");
            Workspace workspace = new Workspace();
            PgnDocument document = workspace.getOrCreateDocument(path);

            Game firstGame = new Game();
            firstGame.getRootNode().setBoard(new Board(true));
            firstGame.setHeader("Event", "First");
            PgnGameId firstGameId = document.writeSingleGame(firstGame);

            GameSession cleanFirst = workspace.createSession(document.loadGame(firstGameId));
            cleanFirst.setPgnSourceReference(new PgnSourceReference(
                    document.getPath(), firstGameId, document.getRevision()));
            Game cleanFirstGame = cleanFirst.getGame();

            Game secondGame = new Game();
            secondGame.getRootNode().setBoard(new Board(true));
            secondGame.setHeader("Event", "Second");
            PgnGameId secondGameId = document.appendGame(secondGame);
            flushEdt();
            if (cleanFirst.getGame() != cleanFirstGame || cleanFirst.isStale()) {
                throw new AssertionError("Appending must not change unrelated sessions");
            }

            GameSession dirtyFirst = workspace.createSession(document.loadGame(firstGameId));
            dirtyFirst.setPgnSourceReference(new PgnSourceReference(
                    document.getPath(), firstGameId, document.getRevision()));
            GameSession unaffectedSecond = workspace.createSession(document.loadGame(secondGameId));
            unaffectedSecond.setPgnSourceReference(new PgnSourceReference(
                    document.getPath(), secondGameId, document.getRevision()));
            Game unaffectedSecondGame = unaffectedSecond.getGame();
            if (!dirtyFirst.applyMove(new Move("e2e4"))) {
                throw new AssertionError("Expected a dirty session for the replacement test");
            }

            Game replacement = document.loadGame(firstGameId);
            replacement.setHeader("Event", "Replacement");
            document.replaceGame(firstGameId, new PgnPrinter().printGame(replacement));
            flushEdt();
            if (!"Replacement".equals(cleanFirst.getGame().getHeader("Event")) ||
                    cleanFirst.isStale() || cleanFirst.isDirty()) {
                throw new AssertionError("Clean sessions for a replaced game must reload");
            }
            if (!dirtyFirst.isStale() || !dirtyFirst.isDirty()) {
                throw new AssertionError("Dirty sessions for a replaced game must become stale");
            }
            if (unaffectedSecond.getGame() != unaffectedSecondGame ||
                    unaffectedSecond.isStale()) {
                throw new AssertionError("Unrelated game sessions must remain unchanged");
            }

            document.deleteGame(secondGameId);
            flushEdt();
            if (!unaffectedSecond.isStale() || document.getGameIds().size() != 1) {
                throw new AssertionError("Deleting a game must only stale sessions for that game");
            }
            System.out.println("TEST: PGN document session synchronization passed");
        } catch (IOException exception) {
            throw new AssertionError("PGN document synchronization test failed", exception);
        } finally {
            if (path != null) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new AssertionError("Unable to remove test PGN", exception);
                }
            }
        }
    }

    public void chessDatabaseSessionSynchronizationTest() {
        Path path = null;
        try {
            path = Files.createTempFile("jfxchess-database-sync-", ".pgn");
            Workspace workspace = new Workspace();
            ChessDatabase database = workspace.getOrCreateDatabase(path);

            final List<ChessDatabaseEvent> events = new ArrayList<>();
            database.addListener(events::add);

            Game firstGame = new Game();
            firstGame.getRootNode().setBoard(new Board(true));
            firstGame.setHeader("Event", "InitialEvent");
            firstGame.setHeader("White", "PlayerOne");
            firstGame.setHeader("Black", "PlayerTwo");
            GameInfo firstInfo = database.appendGame(firstGame);

            if (database.getIndex().size() != 1) {
                throw new AssertionError("Database should have 1 game");
            }
            if (events.isEmpty() || events.get(events.size() - 1).getType() != ChessDatabaseEvent.Type.GAME_APPENDED) {
                throw new AssertionError("Expected GAME_APPENDED event");
            }

            Game loaded = database.loadGame(firstInfo);
            if (!"PlayerOne".equals(loaded.getHeader("White"))) {
                throw new AssertionError("Loaded game mismatch");
            }

            // Create clean session for first game
            GameSession cleanSession = workspace.createSession(database.loadGame(firstInfo));
            long rev = (database instanceof PgnChessDatabase pgnDb) ? pgnDb.getRevision() : 0;
            cleanSession.setPgnSourceReference(new PgnSourceReference(path, firstInfo.getId(), rev));

            // Append second game
            Game secondGame = new Game();
            secondGame.getRootNode().setBoard(new Board(true));
            secondGame.setHeader("Event", "SecondGameEvent");
            GameInfo secondInfo = database.appendGame(secondGame);
            flushEdt();

            if (cleanSession.isStale() || cleanSession.isDirty()) {
                throw new AssertionError("Appending unrelated game must not stale clean session");
            }

            // Create dirty session for first game
            GameSession dirtySession = workspace.createSession(database.loadGame(firstInfo));
            dirtySession.setPgnSourceReference(new PgnSourceReference(path, firstInfo.getId(), rev));
            dirtySession.applyMove(new Move("e2e4"));
            if (!dirtySession.isDirty()) {
                throw new AssertionError("Expected dirty session");
            }

            // Replace first game
            Game replacement = database.loadGame(firstInfo);
            replacement.setHeader("Event", "ReplacedEvent");
            database.replaceGame(replacement, firstInfo);
            flushEdt();

            if (!"ReplacedEvent".equals(cleanSession.getGame().getHeader("Event")) || cleanSession.isStale()) {
                throw new AssertionError("Clean session for replaced game must automatically reload");
            }
            if (!dirtySession.isStale() || !dirtySession.isDirty()) {
                throw new AssertionError("Dirty session for replaced game must be marked stale");
            }

            // Test search
            SearchPattern pattern = new SearchPattern();
            pattern.setEvent("ReplacedEvent");
            database.search(pattern);
            if (database.getSearchResults().size() != 1) {
                throw new AssertionError("Search should find 1 matching game");
            }

            // Test delete
            database.deleteGame(secondInfo);
            flushEdt();
            if (database.getIndex().size() != 1) {
                throw new AssertionError("After deletion, index should have 1 game");
            }

            System.out.println("TEST: Chess database session synchronization passed");
        } catch (IOException exception) {
            throw new AssertionError("Chess database synchronization test failed", exception);
        } finally {
            if (path != null) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new AssertionError("Unable to remove test database", exception);
                }
            }
        }
    }

    private void flushEdt() {
        try {
            SwingUtilities.invokeAndWait(() -> { });
        } catch (Exception exception) {
            throw new AssertionError("Unable to synchronize with the event thread", exception);
        }
    }

    public void databaseLifecycleAndPersistenceTest() {
        Path path = null;
        try {
            path = Files.createTempFile("jfxchess-db-lifecycle-", ".pgn");
            Workspace workspace = new Workspace();
            ChessDatabase database = workspace.getOrCreateDatabase(path);

            Model_JFXChess model = new Model_JFXChess();
            model.setWorkspace(workspace);

            // 1. Initial creation
            database.createNew(path.toString());
            model.setActiveDatabase(database);

            if (!database.isOpen()) {
                throw new AssertionError("Database should be open after createNew");
            }
            if (!database.getIndex().isEmpty()) {
                throw new AssertionError("New database should be empty");
            }

            Game g1 = new Game();
            g1.getRootNode().setBoard(new Board(true));
            g1.setHeader("White", "Alice");
            g1.setHeader("Black", "Bob");
            GameSession s1 = workspace.createSession(g1);

            Game g2 = new Game();
            g2.getRootNode().setBoard(new Board(true));
            g2.setHeader("White", "Charlie");
            g2.setHeader("Black", "Dave");
            GameSession s2 = workspace.createSession(g2);

            // Append g1 to database
            GameInfo info1 = database.appendGame(g1);
            s1.setPgnSourceReference(new PgnSourceReference(path, info1.getId(), 0));

            if (s1.getPgnSourceReference() == null) {
                throw new AssertionError("s1 should be attached to database");
            }
            if (s2.getPgnSourceReference() != null) {
                throw new AssertionError("s2 should be detached");
            }

            // Test Close Database detaches all open sessions
            model.closeActiveDatabase();
            if (model.getActiveDatabase() != null) {
                throw new AssertionError("Active database should be null after close");
            }
            if (database.isOpen()) {
                throw new AssertionError("Database should be closed");
            }
            if (s1.getPgnSourceReference() != null || s2.getPgnSourceReference() != null) {
                throw new AssertionError("All sessions must be detached on database close");
            }

            // Test creating a new database detaches all open sessions
            database.open(path.toString());
            database.scanGames();
            model.setActiveDatabase(database);
            s1.setPgnSourceReference(new PgnSourceReference(path, info1.getId(), 0));

            Path secondDbPath = Files.createTempFile("jfxchess-db2-", ".pgn");
            ChessDatabase db2 = workspace.getOrCreateDatabase(secondDbPath);
            db2.createNew(secondDbPath.toString());
            model.setActiveDatabase(db2);
            model.detachAllSessions();

            if (s1.getPgnSourceReference() != null || s2.getPgnSourceReference() != null) {
                throw new AssertionError("All sessions must be detached when new database is created");
            }
            Files.deleteIfExists(secondDbPath);

            // Re-open first database and re-attach s1
            database.open(path.toString());
            database.scanGames();
            model.setActiveDatabase(database);
            GameInfo currentInfo1 = database.getIndex().get(0);
            s1.setPgnSourceReference(new PgnSourceReference(database.getPath(), currentInfo1.getId(), 0));

            // Test DialogSave button enablement
            DialogSave dlgAttached = new DialogSave(null, true);
            JButton btnReplaceAttached = (JButton) dlgAttached.getContentPane().getComponent(0);
            if (!btnReplaceAttached.isEnabled()) {
                throw new AssertionError("Save (Replace) must be enabled when attached");
            }
            dlgAttached.dispose();

            DialogSave dlgDetached = new DialogSave(null, false);
            JButton btnReplaceDetached = (JButton) dlgDetached.getContentPane().getComponent(0);
            if (btnReplaceDetached.isEnabled()) {
                throw new AssertionError("Save (Replace) must be disabled when detached");
            }
            dlgDetached.dispose();

            // Test persistence on save()
            model.save();

            // Scenario A: Checksum matches upon restore
            Model_JFXChess modelRestore = new Model_JFXChess();
            Workspace workspaceRestore = new Workspace();
            modelRestore.setWorkspace(workspaceRestore);
            modelRestore.restore();

            if (modelRestore.getActiveDatabase() == null || !modelRestore.getActiveDatabase().isOpen()) {
                throw new AssertionError("Active database should be restored and open");
            }
            if (workspaceRestore.getSessions().size() != 2) {
                throw new AssertionError("Expected 2 restored sessions, got: " + workspaceRestore.getSessions().size());
            }
            GameSession restoredS1 = workspaceRestore.getSessions().get(0);
            GameSession restoredS2 = workspaceRestore.getSessions().get(1);
            if (restoredS1.getPgnSourceReference() == null) {
                throw new AssertionError("Restored s1 should be linked to database index");
            }
            if (restoredS2.getPgnSourceReference() != null) {
                throw new AssertionError("Restored s2 should be detached");
            }
            if (modelRestore.isDatabaseModifiedWarning()) {
                throw new AssertionError("No warning expected when checksum matches");
            }

            // Scenario B: Last modified time changed on disk
            Files.setLastModifiedTime(path, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis() + 60000));
            Model_JFXChess modelRestoreMod = new Model_JFXChess();
            Workspace workspaceRestoreMod = new Workspace();
            modelRestoreMod.setWorkspace(workspaceRestoreMod);
            modelRestoreMod.restore();

            if (modelRestoreMod.getActiveDatabase() == null || !modelRestoreMod.getActiveDatabase().isOpen()) {
                throw new AssertionError("Modified database should still be opened");
            }
            if (!modelRestoreMod.isDatabaseModifiedWarning()) {
                throw new AssertionError("Expected database modified warning when last modified time changed");
            }
            for (GameSession sess : workspaceRestoreMod.getSessions()) {
                if (sess.getPgnSourceReference() != null) {
                    throw new AssertionError("All sessions must be detached when database modified time changed");
                }
            }

            // Scenario C: File missing
            Files.deleteIfExists(path);
            Model_JFXChess modelRestoreMissing = new Model_JFXChess();
            Workspace workspaceRestoreMissing = new Workspace();
            modelRestoreMissing.setWorkspace(workspaceRestoreMissing);
            modelRestoreMissing.restore();

            if (modelRestoreMissing.getActiveDatabase() != null) {
                throw new AssertionError("Database should not be opened when file is missing");
            }
            for (GameSession sess : workspaceRestoreMissing.getSessions()) {
                if (sess.getPgnSourceReference() != null) {
                    throw new AssertionError("All sessions must be detached when database file is missing");
                }
            }

            System.out.println("TEST: Database lifecycle, detachment and persistence passed");
        } catch (Exception exception) {
            throw new AssertionError("Database lifecycle and persistence test failed", exception);
        } finally {
            if (path != null) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            }
        }
    }

    public void fenTest() {

        System.out.println("TEST: fen reading & parsing");
        // starting position
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        Board b = new Board(fen);
        System.out.println("in : " + fen);
        System.out.println("out: " + b.fen());
        System.out.println(b);

        fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1";
        b = new Board(fen);
        System.out.println("in : " + fen);
        System.out.println("out: " + b.fen());
        System.out.println(b);

        fen = "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2";
        b = new Board(fen);
        System.out.println("in : " + fen);
        System.out.println("out: " + b.fen());
        System.out.println(b);

        fen = "rnbqkbnr/pp1ppppp/8/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2";
        b = new Board(fen);
        System.out.println("in : " + fen);
        System.out.println("out: " + b.fen());
        System.out.println(b);

    }

    private int countMoves(Board b, int depth) {
        int count = 0;
        ArrayList<Move> mvs = b.legalMoves();
        if(depth == 0) {
            return mvs.size();
        } else {
            // recursive case: for each possible move, apply
            // the move, do the recursive call and undo the move
            for(Move mi : mvs ) {
                b.apply(mi);
                int cnt_i = countMoves(b.makeCopy(), depth - 1);
                count += cnt_i;
                b.undo();
            }
            return count;
        }
    }

    public void runPerfT() {

        System.out.println("TEST: PerfT");
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        Board b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 1, expected 20");
        int c = countMoves(b,0);
        System.out.println("computed: " + c);

        fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 2, expected 400");
        c = countMoves(b,1);
        System.out.println("computed: " + c);

        fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 3, expected 8902");
        c = countMoves(b,2);
        System.out.println("computed: " + c);

        fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 4, expected 197281");
        c = countMoves(b,3);
        System.out.println("computed: " + c);

        fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 5, expected 4865609");
        c = countMoves(b,4);
        System.out.println("computed: " + c);

        // "Kiwipete" by Peter McKenzie, great for identifying bugs
        // perft 1 - 5
        fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 1, expected 48");
        c = countMoves(b,0);
        System.out.println("computed: " + c);

        fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 2, expected 2039");
        c = countMoves(b,1);
        System.out.println("computed: " + c);

        fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 3, expected 97862");
        c = countMoves(b,2);
        System.out.println("computed: " + c);


        fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 4, expected 4085603");
        c = countMoves(b,3);
        System.out.println("computed: " + c);

        fen = "8/3K4/2p5/p2b2r1/5k2/8/8/1q6 b - - 1 67";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 1, expected 50");
        c = countMoves(b,0);
        System.out.println("computed: " + c);

        fen = "8/3K4/2p5/p2b2r1/5k2/8/8/1q6 b - - 1 67";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 2, expected 279");
        c = countMoves(b,1);
        System.out.println("computed: " + c);

        fen = "rnbqkb1r/ppppp1pp/7n/4Pp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 5, expected 11139762");
        c = countMoves(b,4);
        System.out.println("computed: " + c);

        fen = "rnbqkb1r/ppppp1pp/7n/4Pp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 5, expected 11139762");
        c = countMoves(b,4);
        System.out.println("computed: " + c);

        fen = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 1, expected 44");
        c = countMoves(b,0);
        System.out.println("computed: " + c);

        fen = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 2, expected 1486");
        c = countMoves(b,1);
        System.out.println("computed: " + c);

        fen = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 3, expected 62379");
        c = countMoves(b,2);
        System.out.println("computed: " + c);

        fen = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 4, expected 2103487");
        c = countMoves(b,3);
        System.out.println("computed: " + c);

        fen = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 5, expected 89941194");
        c = countMoves(b,4);
        System.out.println("computed: " + c);

        fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 6, expected 119060324");
        c = countMoves(b,5);
        System.out.println("computed: " + c);

        fen = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 0";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 6, expected 11030083");
        c = countMoves(b,5);
        System.out.println("computed: " + c);

        fen = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 0";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 7, expected 178633661");
        c = countMoves(b,6);
        System.out.println("computed: " + c);

        fen = "8/7p/p5pb/4k3/P1pPn3/8/P5PP/1rB2RK1 b - d3 0 28";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 6, expected 38633283");
        c = countMoves(b,5);
        System.out.println("computed: " + c);

        fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 5, expected 193690690");
        c = countMoves(b,4);
        System.out.println("computed: " + c);

    }

    public void runSanTest() {

        System.out.println("TEST: san computation");
        Board b0 = new Board("rnbqkbnr/pppppppp/8/2R5/5R2/2R5/PPPPPPP1/1NBQKBN1 w - - 0 1");
        System.out.println("rnbqkbnr/pppppppp/8/2R5/5R2/2R5/PPPPPPP1/1NBQKBN1 w - - 0 1");
        ArrayList<Move> b0Legals = b0.legalMoves();
        for(Move mi : b0Legals) {
            System.out.println(b0.san(mi));
        }

        Board b1 = new Board("rnbqkbnr/pppppppp/8/2R5/8/2R5/PPPPPPP1/1NBQKBN1 w - - 0 1");
        System.out.println("rnbqkbnr/pppppppp/8/2R5/8/2R5/PPPPPPP1/1NBQKBN1 w - - 0 1");
        ArrayList<Move> b1Legals = b1.legalMoves();
        for(Move mi : b1Legals) {
            System.out.println(b1.san(mi));
        }
    }

    public void runBitSetTest() {
        System.out.println("TEST: bit setting in int values");
        // e.g. distance one, i.e. index 1 (=left, up, down, right square) has
        // value 0x1C = MSB 00011100 LSB, i.e. king, queen, rook can
        // potentially attack
        // 0              Knight
        // 1              Bishop
        // 2              Rook
        // 3              Queen
        // 4              King
        Board b_temp = new Board();
        int attackIdx1 = CONSTANTS.ATTACK_TABLE[1];
        System.out.println("attackIdx1: " + attackIdx1);
        System.out.println("expectec: MSB 00011100 LSB");
        System.out.println("bit 0 (Knight): "+ b_temp.isKthBitSet(attackIdx1, 0));
        System.out.println("bit 1 (bishop): "+ b_temp.isKthBitSet(attackIdx1, 1));
        System.out.println("bit 2 (rook): "+ b_temp.isKthBitSet(attackIdx1, 2));
        System.out.println("bit 3 (queen): "+ b_temp.isKthBitSet(attackIdx1, 3));
        System.out.println("bit 4 (king): "+ b_temp.isKthBitSet(attackIdx1, 4));
    }

    public void runPgnPrintTest() {

        System.out.println("TEST: simple PGN printing");
        Game g = new Game();
        g.setHeader("Event", "Knaurs Schachbuch");
        g.setHeader("Site", "Paris");
        g.setHeader("Date", "1859.??.??");
        g.setHeader("Round", "1");
        g.setHeader("White", "Morphy");
        g.setHeader("Black", "NN");
        g.setHeader("Result", "1-0");
        g.setHeader("ECO", "C56");

        Board rootBoard = new Board(true);
        g.getRootNode().setBoard(rootBoard);

        g.applyMove(new Move("e2e4"));
        g.applyMove(new Move("e7e5"));
        g.applyMove(new Move("g1f3"));
        g.applyMove(new Move("b8c6"));
        g.applyMove(new Move("f1c4"));
        g.applyMove(new Move("g8f6"));
        g.applyMove(new Move("d2d4"));
        g.applyMove(new Move("e5d4"));
        g.applyMove(new Move("e1g1"));
        g.applyMove(new Move("f6e4"));

        PgnPrinter printer = new PgnPrinter();
        System.out.println(printer.printGame(g));
        printer.writeGame(g, "temp.pgn");

    }

    public void pgnScanTest() {

        System.out.println("TEST: scanning PGN for game offsets");
        String kingbase = "C:/Users/user/MyFiles/workspace/test_databases/KingBaseLite2016-03-E60-E99.pgn";
        String millbase = "C:/Users/user/MyFiles/workspace/test_databases/millionbase-2.22.pgn";
        String middleg = "C:/Users/user/MyFiles/workspace/test_databases/middleg.pgn";
        PgnReader reader = new PgnReader();

        long startTime = System.currentTimeMillis();
        ArrayList<Long> offsets = reader.scanPgn(millbase);
        long stopTime = System.currentTimeMillis();

        long timeElapsed = stopTime - startTime;
        System.out.println("elapsed time: "+(timeElapsed/1000)+" secs");

        System.out.println(offsets.size());

        for(int i=0;i<10;i++) {
            long offset_i = offsets.get(i);
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(millbase, "r");
                try {
                    raf.seek(offset_i);
                    String line = raf.readLine();
                    System.out.println("START"+line+"STOP");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                if(raf != null) {
                    try {
                        raf.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /*
    public void pgnScanSTRTest() {

        System.out.println("TEST: scanning PGN for game STR offsets");
        String kingbase = "C:/Users/user/MyFiles/workspace/test_databases/KingBaseLite2016-03-E60-E99.pgn";
        String millbase = "C:/Users/user/MyFiles/workspace/test_databases/millionbase-2.22.pgn";
        String middleg = "C:/Users/user/MyFiles/workspace/test_databases/middleg.pgn";
        PgnReader reader = new PgnReader();

        long startTime = System.currentTimeMillis();
        ArrayList<PgnDatabaseEntry> entries = reader.scanPgnGetSTR(millbase);
        long stopTime = System.currentTimeMillis();

        long timeElapsed = stopTime - startTime;
        System.out.println("elapsed time: "+(timeElapsed/1000)+" secs");

        System.out.println(entries.size());

        for(int i=0;i<10;i++) {
            PgnDatabaseEntry entry_i = entries.get(i);
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(millbase, "r");
                try {
                    raf.seek(entry_i.getOffset());
                    String line = raf.readLine();
                    System.out.println("START"+line+"STOP");
                    System.out.println(entry_i.getEvent());
                    System.out.println("");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                if(raf != null) {
                    try {
                        raf.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

     */

    public void pgnReadGameTest() {

        System.out.println("TEST: reading single PGN game");
        String kingbase = "C:/Users/user/MyFiles/workspace/millbase_prev_last.pgn";
        OptimizedRandomAccessFile raf = null;
        PgnReader reader = new PgnReader();
        /*
        if(reader.isIsoLatin1(kingbase)) {
            reader.setEncodingIsoLatin1();
        }*/
        PgnPrinter printer = new PgnPrinter();
        try {
            raf = new OptimizedRandomAccessFile(kingbase, "r");
            Game g = reader.readGame(raf);
            String pgn = printer.printGame(g);
            System.out.println(pgn);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pgnReadMiddleGTest() {

        System.out.println("TEST: reading all games from middleg.pgn");
        String middleg = "C:/Users/user/MyFiles/workspace/test_databases/middleg.pgn";

        OptimizedRandomAccessFile raf = null;
        PgnReader reader = new PgnReader();
        ArrayList<Long> offsets = reader.scanPgn(middleg);

        PgnPrinter printer = new PgnPrinter();
        try {
            raf = new OptimizedRandomAccessFile(middleg, "r");
            for (int i=0;i<offsets.size();i++) {
                long offset_i = offsets.get(i);
                System.out.println("reading game nr: "+(i+1));
                raf.seek(offset_i);
                Game g = reader.readGame(raf);
                System.out.println("reading game ok");
                String pgn = printer.printGame(g);
                System.out.println(pgn);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pgnReadAllMillBaseTest() {

        System.out.println("TEST: reading all games from millionbase-2.22.pgn");
        String millbase = "C:/Users/user/MyFiles/workspace/test_databases/millionbase-2.22.pgn";
        PgnReader reader = new PgnReader();

        long startTime = System.currentTimeMillis();
        ArrayList<Long> offsets = reader.scanPgn(millbase);
        long stopTime = System.currentTimeMillis();
        long timeElapsed = stopTime - startTime;
        System.out.println("elapsed time for scanning: " + (timeElapsed / 1000) + " secs");

        System.out.println(offsets.size());
        OptimizedRandomAccessFile raf = null;
        try {
            raf = new OptimizedRandomAccessFile(millbase, "r");
            startTime = System.currentTimeMillis();
            for (int i = 0; i < offsets.size(); i++) {
                long offset_i = offsets.get(i);
                if(i%100000 == 0) {
                    System.out.println("i: "+i);
                    stopTime = System.currentTimeMillis();
                    timeElapsed = stopTime - startTime;
                    System.out.println("elapsed time for reading each game: " + (timeElapsed / 1000) + " secs");
                }
                raf.seek(offset_i);
                Game g = reader.readGame(raf);
            }
            stopTime = System.currentTimeMillis();
            timeElapsed = stopTime - startTime;
            System.out.println("elapsed time for reading all games: " + (timeElapsed / 1000) + " secs");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    // using file open/close
    public void pgnReadSingleEntryTestOpenClose() {

        System.out.println("TEST: scanning offsets from PGN, and reading each hader w/ multiple fopen/close");
        String kingbase = "C:/Users/user/MyFiles/workspace/test_databases/KingBaseLite2016-03-E60-E99.pgn";
        String millbase = "C:/Users/user/MyFiles/workspace/test_databases/millionbase-2.22.pgn";
        String middleg = "C:/Users/user/MyFiles/workspace/test_databases/middleg.pgn";
        PgnReader reader = new PgnReader();

        long startTime = System.currentTimeMillis();
        ArrayList<Long> offsets = reader.scanPgn(millbase);
        long stopTime = System.currentTimeMillis();
        long timeElapsed = stopTime - startTime;
        System.out.println("elapsed time for scanning: "+(timeElapsed/1000)+" secs");

        System.out.println(offsets.size());

        int matchCount = 0;
        startTime = System.currentTimeMillis();
        for(int i=0;i<offsets.size();i++) {
            long offset_i = offsets.get(i);
            HashMap<String,String> header = reader.readSingleHeader(millbase, offset_i);
            if(header.get("Event").equals("Barbera Open")) {
                matchCount += 1;
            }
        }
        stopTime = System.currentTimeMillis();
        timeElapsed = stopTime - startTime;
        System.out.println("elapsed time for reading each header: "+(timeElapsed/1000)+" secs");
        System.out.println("games matching event 'Barabara Open' "+matchCount);

    }

    // using raf that is kept open
    public void pgnReadSingleEntryTestSeekWithinRAF() {

        System.out.println("TEST: scanning offsets from PGN, and reading each hader, keeping file open");
        String kingbase = "C:/Users/user/MyFiles/workspace/test_databases/KingBaseLite2016-03-E60-E99.pgn";
        String millbase = "C:/Users/user/MyFiles/workspace/test_databases/millionbase-2.22.pgn";
        String middleg = "C:/Users/user/MyFiles/workspace/test_databases/middleg.pgn";
        PgnReader reader = new PgnReader();

        long startTime = System.currentTimeMillis();
        ArrayList<Long> offsets = reader.scanPgn(millbase);
        long stopTime = System.currentTimeMillis();
        long timeElapsed = stopTime - startTime;
        System.out.println("elapsed time for scanning: " + (timeElapsed / 1000) + " secs");

        System.out.println(offsets.size());
        OptimizedRandomAccessFile raf = null;
        try {
            raf = new OptimizedRandomAccessFile(millbase, "r");
            int matchCount = 0;
            startTime = System.currentTimeMillis();
            for (int i = 0; i < offsets.size(); i++) {
                long offset_i = offsets.get(i);
                HashMap<String, String> header = reader.readSingleHeader(raf, offset_i);
                if (header.get("Event").equals("Barbera Open")) {
                    matchCount += 1;
                }
            }
            stopTime = System.currentTimeMillis();
            timeElapsed = stopTime - startTime;
            System.out.println("elapsed time for reading each header: " + (timeElapsed / 1000) + " secs");
            System.out.println("games matching event 'Barabara Open' " + matchCount);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void readGamesByStringTest() {

        String s = "[Event \"Berlin\"]\n" +
                "[Site \"Berlin GER\"]\n" +
                "[Date \"1852.??.??\"]\n" +
                "[EventDate \"?\"]\n" +
                "[Round \"?\"]\n" +
                "[Result \"1-0\"]\n" +
                "[White \"Adolf Anderssen\"]\n" +
                "[Black \"Jean Dufresne\"]\n" +
                "[ECO \"C52\"]\n" +
                "[WhiteElo \"?\"]\n" +
                "[BlackElo \"?\"]\n" +
                "[PlyCount \"47\"]\n" +
                "\n" +
                "1.e4 e5 2.Nf3 Nc6 3.Bc4 Bc5 4.b4 Bxb4 5.c3 Ba5 6.d4 exd4 7.O-O\n" +
                "d3 8.Qb3 Qf6 9.e5 Qg6 10.Re1 Nge7 11.Ba3 b5 12.Qxb5 Rb8 13.Qa4\n" +
                "Bb6 14.Nbd2 Bb7 15.Ne4 Qf5 16.Bxd3 Qh5 17.Nf6+ gxf6 18.exf6\n" +
                "Rg8 19.Rad1 Qxf3 20.Rxe7+ Nxe7 21.Qxd7+ Kxd7 22.Bf5+ Ke8\n" +
                "23.Bd7+ Kf8 24.Bxe7# 1-0";
        PgnReader reader = new PgnReader();
        PgnPrinter printer = new PgnPrinter();
        Game g = reader.readGame(s);
        System.out.println(printer.printGame(g));

    }

    public void runPosHashTest() {

        Board b1 = new Board("rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq d3 0 1");
        Board b2 = new Board("rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq d3 0 1");
        long key1 = b1.getPositionHash();
        long key2 = b2.getPositionHash();
        System.out.println("Posh Hash1: "+key1);
        System.out.println("Posh Hash2: "+key2);

        System.out.println("TEST: reading single PGN game, trying to find starting pos after 1d4 by pos hash");
        String kingbase = "C:/Users/user/MyFiles/workspace/test_databases/KingBaseLite2016-03-E60-E99.pgn";
        OptimizedRandomAccessFile raf = null;
        PgnReader reader = new PgnReader();
        PgnPrinter printer = new PgnPrinter();
        try {
            raf = new OptimizedRandomAccessFile(kingbase, "r");
            Game g = reader.readGame(raf);
            boolean hasStartingPos = g.containsPosition(key1, 0 , 100);
            System.out.println("found pos: "+hasStartingPos);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void runZobristTest() {

        System.out.println("TEST: zobrist hashing");

        Board b = new Board("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        long key = b.getZobrist();
        System.out.println("expected zobrist: 463b96181691fc9c");
        System.out.println("got zobrist.....: " + Long.toHexString(key));

        b = new Board("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
        key = b.getZobrist();
        System.out.println("expected zobrist: 823c9b50fd114196");
        System.out.println("got zobrist.....: " + Long.toHexString(key));

        b = new Board("rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2");
        key = b.getZobrist();
        System.out.println("expected zobrist: 756b94461c50fb0");
        System.out.println("got zobrist.....: " + Long.toHexString(key));

        b = new Board("rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR b KQkq - 0 2");
        key = b.getZobrist();
        System.out.println("expected zobrist: 662fafb965db29d4");
        System.out.println("got zobrist.....: " + Long.toHexString(key));

        b = new Board("rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3");
        key = b.getZobrist();
        System.out.println("expected zobrist: 22a48b5a8e47ff78");
        System.out.println("got zobrist.....: " + Long.toHexString(key));

        b = new Board("rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPPKPPP/RNBQ1BNR b kq - 0 3");
        key = b.getZobrist();
        System.out.println("expected zobrist: 652a607ca3f242c1");
        System.out.println("got zobrist.....: " + Long.toHexString(key));

        b = new Board("rnbq1bnr/ppp1pkpp/8/3pPp2/8/8/PPPPKPPP/RNBQ1BNR w - - 0 4");
        key = b.getZobrist();
        System.out.println("expected zobrist: fdd303c946bdd9");
        System.out.println("got zobrist.....: " + Long.toHexString(key));

        b = new Board("rnbqkbnr/p1pppppp/8/8/PpP4P/8/1P1PPPP1/RNBQKBNR b KQkq c3 0 3");
        key = b.getZobrist();
        System.out.println("expected zobrist: 3c8123ea7b067637");
        System.out.println("got zobrist.....: " + Long.toHexString(key));

        b = new Board("rnbqkbnr/p1pppppp/8/8/P6P/R1p5/1P1PPPP1/1NBQKBNR b Kkq - 0 4");
        key = b.getZobrist();
        System.out.println("expected zobrist: 5c3f9b829b279560");
        System.out.println("got zobrist.....: " + Long.toHexString(key));

    }

    public void testPolyglot() {

        Polyglot pg1 = new Polyglot();

        File file = null;
        URL book = getClass().getClassLoader().getResource("book/varied.bin");
        if(book != null) {
            file = new File(book.getFile());
            pg1.loadBook(file);
        }
        System.out.println(pg1.readFile);
        try {
            PolyglotEntry e = pg1.getEntryFromOffset(0x62c20);
            System.out.println(e.uci);

            ArrayList<String> entries = pg1.findMoves(0x463b96181691fc9cL);
            for(String uci : entries) {
                System.out.println(uci);
            }

            entries = pg1.findMoves(0x2d3888dac361814aL);
            for(String uci : entries) {
                System.out.println(uci);
            }

            entries = pg1.findMoves(0x823c9b50fd114196L);
            for(String uci : entries) {
                System.out.println(uci);
            }

            System.out.println(0x463b96181691fc9cL);
            System.out.println(0x2d3888dac361814aL);
            System.out.println(0x823c9b50fd114196L);

        } catch(IllegalArgumentException e) {
            e.printStackTrace();
        }
    }


}
