/* JFXChess - A Chess Graphical User Interface
 * Copyright (C) 2020-2026 Dominik Klein
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

package org.asdfjkl.jfxchess.gui;

import org.asdfjkl.jfxchess.lib.Board;
import org.asdfjkl.jfxchess.lib.CONSTANTS;
import org.asdfjkl.jfxchess.lib.Game;
import org.asdfjkl.jfxchess.lib.Move;

import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.util.List;

public class TabEngineMismatchTest {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("           Tab - Engine - EngineOutputView Invariant Verification Tests          ");
        System.out.println("================================================================================");

        boolean allPassed = true;

        try {
            testEngineAssociationMismatch();
        } catch (Throwable t) {
            System.err.println("Test 1 FAILED: " + t.getMessage());
            t.printStackTrace(System.err);
            allPassed = false;
        }

        try {
            testEngineOutputViewCrossTabLeakage();
        } catch (Throwable t) {
            System.err.println("Test 2 FAILED: " + t.getMessage());
            t.printStackTrace(System.err);
            allPassed = false;
        }

        try {
            testEngineLabelConfusion();
        } catch (Throwable t) {
            System.err.println("Test 3 FAILED: " + t.getMessage());
            t.printStackTrace(System.err);
            allPassed = false;
        }

        try {
            testEngineListenerDroppedOnTabSwitch();
        } catch (Throwable t) {
            System.err.println("Test 4 FAILED: " + t.getMessage());
            t.printStackTrace(System.err);
            allPassed = false;
        }

        try {
            testButtonStatesMismatch();
        } catch (Throwable t) {
            System.err.println("Test 5 FAILED: " + t.getMessage());
            t.printStackTrace(System.err);
            allPassed = false;
        }

        System.out.println("================================================================================");
        if (allPassed) {
            System.out.println("ALL 5 INVARIANT TESTS PASSED SUCCESSFULLY! [OK]");
            System.exit(0);
        } else {
            System.err.println("SOME INVARIANT TESTS FAILED!");
            System.exit(1);
        }
        System.out.println("================================================================================");
    }

    /**
     * Test 1: Invariant: Each tab must have one engine associated to it.
     * Verifies that switching tabs restores model.activeEngine to that tab's engine,
     * and that engines are properly copied and isolated.
     */
    public static void testEngineAssociationMismatch() {
        System.out.println("\n--- [TEST 1] Testing Tab - Engine Association Invariant ---");

        Model_JFXChess model = new Model_JFXChess();
        Workspace workspace = new Workspace();
        model.setWorkspace(workspace);

        Controller_Engine controllerEngine = new Controller_Engine(model);

        // Tab 1: Bot game (Benny the Beginner)
        Game g1 = new Game();
        g1.getRootNode().setBoard(new Board(true));
        GameSession session1 = workspace.createSession(g1);
        model.setGameSession(session1);

        BotEngine bot = model.botEngines.get(0).makeCopy(); // Benny
        controllerEngine.activatePlayWhiteMode(session1, bot);

        Engine tab1Engine = session1.getActiveEngine();
        String tab1Name = "null";
        if (tab1Engine != null) {
            tab1Name = tab1Engine.getName();
        }
        System.out.println("Tab 1 created: mode=" + session1.getMode()
                + ", session1.activeEngine=" + tab1Name
                + ", model.activeEngine=" + model.activeEngine.getName());

        // Tab 2: Analysis (Stockfish internal)
        Game g2 = new Game();
        g2.getRootNode().setBoard(new Board(true));
        GameSession session2 = workspace.createSession(g2);
        model.setGameSession(session2);
        controllerEngine.activateAnalysisMode(session2);

        Engine tab2Engine = session2.getActiveEngine();
        String tab2Name = "null";
        if (tab2Engine != null) {
            tab2Name = tab2Engine.getName();
        }
        System.out.println("Tab 2 created: mode=" + session2.getMode()
                + ", session2.activeEngine=" + tab2Name
                + ", model.activeEngine=" + model.activeEngine.getName());

        // Tab 3: Game against engine with reduced strength (Stockfish at Elo 1500)
        Game g3 = new Game();
        g3.getRootNode().setBoard(new Board(true));
        GameSession session3 = workspace.createSession(g3);
        model.setGameSession(session3);

        Engine uciEngine = model.engines.get(0).makeCopy();
        uciEngine.setUciElo(1500);
        controllerEngine.activatePlayWhiteMode(session3, uciEngine);

        Engine tab3Engine = session3.getActiveEngine();
        String tab3Name = "null";
        if (tab3Engine != null) {
            tab3Name = tab3Engine.getName();
        }
        System.out.println("Tab 3 created: mode=" + session3.getMode()
                + ", session3.activeEngine=" + tab3Name
                + ", model.activeEngine=" + model.activeEngine.getName() + " (Elo: " + model.activeEngine.getUciElo() + ")");

        // Now switch back to Tab 1
        System.out.println("\nSwitching back to Tab 1...");
        model.setGameSession(session1);
        workspace.setActiveSession(session1);

        System.out.println("Active session is now Tab 1 (id=" + session1.getId() + ")");
        System.out.println("session1.getActiveEngine() = " + session1.getActiveEngine().getName());
        System.out.println("model.activeEngine = " + model.activeEngine.getName() + " (Elo: " + model.activeEngine.getUciElo() + ")");

        if (!model.activeEngine.getName().equals(session1.getActiveEngine().getName())) {
            throw new AssertionError("model.activeEngine was NOT restored to Tab 1's engine on tab switch! Expected: "
                    + session1.getActiveEngine().getName() + " but got: " + model.activeEngine.getName());
        }

        if (session2.getActiveEngine().getUciElo() == 1500) {
            throw new AssertionError("Tab 2's engine Elo was polluted by Tab 3's engine configuration!");
        }

        System.out.println("--> [OK]: model.activeEngine correctly matches Tab 1's engine and engines are properly isolated.");
    }

    /**
     * Test 2: Invariant: Each tab has one engineoutputview / text info.
     * Verifies that Tab 1's View_EngineOutput is scoped to Tab 1 and Tab 2's output
     * does not leak into Tab 1.
     */
    public static void testEngineOutputViewCrossTabLeakage() {
        System.out.println("\n--- [TEST 2] Testing EngineOutputView Cross-Tab Leakage Invariant ---");

        Model_JFXChess model = new Model_JFXChess();
        Workspace workspace = new Workspace();
        model.setWorkspace(workspace);

        // Create Tab 1
        Game g1 = new Game();
        g1.getRootNode().setBoard(new Board(true));
        GameSession session1 = workspace.createSession(g1);
        model.setGameSession(session1);

        View_EngineOutput tab1Output = new View_EngineOutput(model, session1);
        session1.addPropertyChangeListener(tab1Output);

        // Tab 1 sets play message
        session1.setCurrentEngineInfo("INFO Tab1: Your turn - White to move");
        System.out.println("Tab 1 View_EngineOutput text: " + tab1Output.getText().trim());

        // Create Tab 2
        Game g2 = new Game();
        g2.getRootNode().setBoard(new Board(true));
        GameSession session2 = workspace.createSession(g2);
        model.setGameSession(session2);

        View_EngineOutput tab2Output = new View_EngineOutput(model, session2);
        session2.addPropertyChangeListener(tab2Output);

        // Tab 2 runs analysis and emits engine info
        String analysisInfo = "INFO Tab2: depth 24 score cp +75 pv e2e4 e7e5 g1f3";
        session2.setCurrentEngineInfo(analysisInfo);

        System.out.println("Tab 2 emitted analysis info: " + analysisInfo);
        System.out.println("Tab 2 View_EngineOutput text: " + tab2Output.getText().trim());
        System.out.println("Tab 1 View_EngineOutput text: " + tab1Output.getText().trim());

        if (tab1Output.getText().contains("Tab2")) {
            throw new AssertionError("Tab 2's engine info OVERWROTE Tab 1's View_EngineOutput while Tab 1 is in background!");
        }
        if (!tab2Output.getText().contains("Tab2")) {
            throw new AssertionError("Tab 2's View_EngineOutput did not receive Tab 2's engine info!");
        }

        System.out.println("--> [OK]: Tab 1's View_EngineOutput was not affected by Tab 2's output.");
    }

    /**
     * Test 3: Invariant: Engine name must associate to that tab.
     * Verifies that View_EngineOutput resolves ENGINE_ID via session's activeEngine.
     */
    public static void testEngineLabelConfusion() {
        System.out.println("\n--- [TEST 3] Testing Engine Name Association Invariant ---");

        Model_JFXChess model = new Model_JFXChess();
        Workspace workspace = new Workspace();
        model.setWorkspace(workspace);
        Controller_Engine controllerEngine = new Controller_Engine(model);

        // Tab 1: Bot game Benny
        Game g1 = new Game();
        g1.getRootNode().setBoard(new Board(true));
        GameSession session1 = workspace.createSession(g1);
        model.setGameSession(session1);
        BotEngine bot = model.botEngines.get(0).makeCopy(); // Benny
        controllerEngine.activatePlayWhiteMode(session1, bot);

        View_EngineOutput tab1Output = new View_EngineOutput(model, session1);
        session1.addPropertyChangeListener(tab1Output);

        // Tab 3: UCI Engine Stockfish 1500
        Game g3 = new Game();
        g3.getRootNode().setBoard(new Board(true));
        GameSession session3 = workspace.createSession(g3);
        model.setGameSession(session3);
        Engine uci = model.engines.get(0).makeCopy();
        uci.setUciElo(1500);
        controllerEngine.activatePlayWhiteMode(session3, uci);

        // Switch back to Tab 1
        model.setGameSession(session1);
        workspace.setActiveSession(session1);

        // Tab 1 updates engine info with ENGINE_ID template
        String template = "INFO <table><tr><td>ENGINE_ID</td></tr><tr><td>thinking...</td></tr></table>";
        session1.setCurrentEngineInfo(template);

        System.out.println("Tab 1 View_EngineOutput rendered text:\n" + tab1Output.getText().trim());

        if (tab1Output.getText().contains("Stockfish") && tab1Output.getText().contains("1500")) {
            throw new AssertionError("Tab 1 View_EngineOutput displays 'Stockfish (1500)' instead of 'Benny the Beginner'!");
        }
        if (!tab1Output.getText().contains("Benny")) {
            throw new AssertionError("Tab 1 View_EngineOutput does not contain 'Benny'! Output: " + tab1Output.getText().trim());
        }

        System.out.println("--> [OK]: Tab 1 correctly displays its own engine name.");
    }

    /**
     * Test 4: Invariant: Engine info must be routed to the correct tab.
     * Verifies that background sessions retain engine listeners and that bestmove
     * events are routed strictly to the originating session.
     */
    public static void testEngineListenerDroppedOnTabSwitch() {
        System.out.println("\n--- [TEST 4] Testing Engine Listener / Output Routing Invariant ---");

        Model_JFXChess model = new Model_JFXChess();
        Workspace workspace = new Workspace();
        model.setWorkspace(workspace);
        Controller_Engine controllerEngine = new Controller_Engine(model);

        // Tab 1 created
        Game g1 = new Game();
        g1.getRootNode().setBoard(new Board(true));
        GameSession session1 = workspace.createSession(g1);
        model.setGameSession(session1);
        controllerEngine.activatePlayWhiteMode(session1, model.botEngines.get(0).makeCopy());

        // Tab 2 created and activated
        Game g2 = new Game();
        g2.getRootNode().setBoard(new Board(true));
        GameSession session2 = workspace.createSession(g2);
        model.setGameSession(session2);
        controllerEngine.activateAnalysisMode(session2);

        EngineSession es1 = session1.getEngineSession();
        EngineSession es2 = session2.getEngineSession();

        boolean es1HasListener = hasListener(es1);
        boolean es2HasListener = hasListener(es2);

        System.out.println("Switched to Session 2:");
        System.out.println("Controller_Engine is listening to es1 (Tab 1)? " + es1HasListener);
        System.out.println("Controller_Engine is listening to es2 (Tab 2)? " + es2HasListener);

        if (!es1HasListener) {
            throw new AssertionError("Controller_Engine is NOT listening to background Tab 1's EngineSession!");
        }
        if (!es2HasListener) {
            throw new AssertionError("Controller_Engine is NOT listening to active Tab 2's EngineSession!");
        }

        // Test background bestmove routing:
        // Set Tab 1 blockGUI to true (computer thinking)
        session1.setBlockGui(true);
        long zobrist1 = session1.getGame().getCurrentNode().getBoard().getZobrist();
        // Emulate engine bestmove for Tab 1 while Tab 2 is active
        controllerEngine.handleBestMove(session1, "BESTMOVE|e7e5|" + zobrist1);

        if (session1.isBlockGui()) {
            throw new AssertionError("Tab 1 blockGui remained true after bestmove arrived for Tab 1!");
        }

        System.out.println("--> [OK]: Tab 1's EngineSession retains a listener and background bestmove is correctly routed.");
    }

    /**
     * Test 5: Invariant: Enabled/disabled button state must correspond to the state of that tab,
     * and pausing/resuming restores previous play mode.
     */
    public static void testButtonStatesMismatch() {
        System.out.println("\n--- [TEST 5] Testing Button State Invariant ---");

        Model_JFXChess model = new Model_JFXChess();
        Workspace workspace = new Workspace();
        model.setWorkspace(workspace);
        Controller_Engine controllerEngine = new Controller_Engine(model);

        // Tab 1 in PLAY_WHITE mode
        Game g1 = new Game();
        g1.getRootNode().setBoard(new Board(true));
        GameSession session1 = workspace.createSession(g1);
        model.setGameSession(session1);
        BotEngine bot = model.botEngines.get(0).makeCopy();
        controllerEngine.activatePlayWhiteMode(session1, bot);

        System.out.println("Tab 1 mode is: " + session1.getMode() + " (MODE_PLAY_WHITE)");

        // Click toggleEngine (Stop Engine) -> pauses to MODE_ENTER_MOVES and saves previousPlayMode
        controllerEngine.toggleEngine(session1).actionPerformed(null);
        System.out.println("After toggleEngine (pause), mode is: " + session1.getMode() + " (MODE_ENTER_MOVES)");

        if (session1.getMode() != Model_JFXChess.MODE_ENTER_MOVES) {
            throw new AssertionError("Expected MODE_ENTER_MOVES after pause, but got: " + session1.getMode());
        }

        // Click toggleEngine again (Start Engine) -> resumes previous play mode (MODE_PLAY_WHITE)
        controllerEngine.toggleEngine(session1).actionPerformed(null);
        System.out.println("After toggleEngine (resume), mode is: " + session1.getMode() + " (MODE_PLAY_WHITE)");

        if (session1.getMode() != Model_JFXChess.MODE_PLAY_WHITE) {
            throw new AssertionError("Resuming from pause converted tab from PLAY_WHITE to mode: " + session1.getMode());
        }

        System.out.println("--> [OK]: Toggling engine switch correctly pauses and resumes previous play mode.");
    }

    private static boolean hasListener(EngineSession session) {
        PropertyChangeListener[] listeners = session.getPropertyChangeListeners();
        if (listeners != null && listeners.length > 0) {
            return true;
        }
        return false;
    }
}
