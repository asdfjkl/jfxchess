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

import org.asdfjkl.jfxchess.lib.*;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Controller_Engine implements PropertyChangeListener {

    private final Model_JFXChess model;
    private final Set<EngineSession> registeredEngineSessions = Collections.newSetFromMap(new IdentityHashMap<>());
    private final String playInfo =
            "INFO <table border=\"0\" cellspacing=\"0\" cellpadding=\"4\" width=\"100%\">" +
                    "  <tr>" +
                    "    <td>" +
                    "      <table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" width=\"50%\">" +
                    "        <tr>" +
                    "          <td>ENGINE_ID</td>" +
                    "          <td></td>" +
                    "          <td></td>" +
                    "          <td></td>" +
                    "          <td></td>" +
                    "        </tr>" +
                    "      </table>" +
                    "    </td>" +
                    "  </tr>" +
                    "  <tr>" +
                    "    <td>&nbsp;</td>" +
                    "  </tr>" +
                    "  <tr>" +
                    "    <td>" +
                    "      MESSAGE" +
                    "    </td>" +
                    "  </tr>" +
                    "</table>";

    public Controller_Engine(Model_JFXChess model) {
        this.model = model;
        model.addListener(this);
        initWorkspaceListeners();
        ensureSessionEngineListener(model.getGameSession());
    }

    private void ensureSessionEngineListener(GameSession session) {
        if (session == null) {
            return;
        }
        EngineSession engineSession = session.getEngineSession();
        if (engineSession == null) {
            return;
        }
        if (!registeredEngineSessions.contains(engineSession)) {
            registeredEngineSessions.add(engineSession);
            engineSession.addPropertyChangeListener(evt -> {
                if ("engineInfoFromThread".equals(evt.getPropertyName())) {
                    handleNewEngineInfo(session, (String) evt.getNewValue());
                }
            });
        }
    }

    private void initWorkspaceListeners() {
        Workspace ws = model.getWorkspace();
        if (ws != null) {
            for (GameSession s : ws.getSessions()) {
                ensureSessionEngineListener(s);
            }
            ws.addPropertyChangeListener(evt -> {
                if ("sessionAdded".equals(evt.getPropertyName())) {
                    ensureSessionEngineListener((GameSession) evt.getNewValue());
                }
            });
        }
    }

    private EngineSession getEngineSession() {
        return model.getGameSession().getEngineSession();
    }

    private void listenToActiveEngineSession() {
        initWorkspaceListeners();
        ensureSessionEngineListener(model.getGameSession());
    }

    public void sendCommand(GameSession session, String cmd) {
        if (session != null) {
            try {
                session.getEngineSession().sendCommand(cmd);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendCommand(String cmd) {
        sendCommand(model.getGameSession(), cmd);
    }

    public void stopEngine(GameSession session) {
        sendCommand(session, "stop");
        sendCommand(session, "quit");
    }

    public void stopEngine() {
        stopEngine(model.getGameSession());
    }

    public void restartEngine(GameSession session, Engine activeEngine) {
        if (session == null || activeEngine == null) {
            return;
        }
        session.setActiveEngine(activeEngine);
        stopEngine(session);
        sendCommand(session, "start " + activeEngine.getPath());
        int countMs = 0;
        do {
            try {
                Thread.sleep(10);
                countMs += 10;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (!session.getEngineSession().isEngineRunning() && countMs < 1500);

        sendCommand(session, "uci");
        for (EngineOption enOpt : activeEngine.options) {
            if (enOpt.isNotDefault()) {
                sendCommand(session, enOpt.toUciCommand());
            }
        }
        sendCommand(session, "isready");
        sendCommand(session, "ucinewgame");
        sendCommand(session, "isready");
    }

    public void restartEngine(Engine activeEngine) {
        restartEngine(model.getGameSession(), activeEngine);
    }

    public void setMultiPV(GameSession session, int n) {
        if (session == null) {
            return;
        }
        Engine currentEngine = session.getActiveEngine();
        if (currentEngine != null && currentEngine.supportsMultiPV()) {
            sendCommand(session, "setoption name MultiPV value " + n);
        }
        session.getEngineSession().setPvLines(n);
    }

    public void setMultiPV(int n) {
        setMultiPV(model.getGameSession(), n);
    }

    public ActionListener incMultiPV() {
        return incMultiPV(model.getGameSession());
    }

    public ActionListener incMultiPV(GameSession session) {
        return e -> {
            if (session.getMode() == Model_JFXChess.MODE_ANALYSIS) {
                Engine activeEngine = session.getActiveEngine();
                if (activeEngine != null && activeEngine.supportsMultiPV()) {
                    int currentMultiPv = activeEngine.getMultiPV();
                    int maxPv = Math.min(activeEngine.getMaxMultiPV(), Model_JFXChess.MAX_PV);
                    if (currentMultiPv < maxPv) {
                        currentMultiPv++;
                        activeEngine.setMultiPV(currentMultiPv);
                        model.setMultiPv(currentMultiPv);
                        sendCommand(session, "stop");
                        sendCommand(session, "isready");
                        sendCommand(session, "setoption name MultiPV value " + currentMultiPv);
                        sendCommand(session, "isready");
                        sendCommand(session, "go infinite");
                    }
                }
            }
        };
    }

    public ActionListener decMultiPV() {
        return decMultiPV(model.getGameSession());
    }

    public ActionListener decMultiPV(GameSession session) {
        return e -> {
            if (session.getMode() == Model_JFXChess.MODE_ANALYSIS) {
                Engine activeEngine = session.getActiveEngine();
                if (activeEngine != null && activeEngine.supportsMultiPV()) {
                    int currentMultiPv = activeEngine.getMultiPV();
                    if (currentMultiPv > 1) {
                        currentMultiPv--;
                        activeEngine.setMultiPV(currentMultiPv);
                        model.setMultiPv(currentMultiPv);
                        sendCommand(session, "stop");
                        sendCommand(session, "isready");
                        sendCommand(session, "setoption name MultiPV value " + currentMultiPv);
                        sendCommand(session, "isready");
                        sendCommand(session, "go infinite");
                    }
                }
            }
        };
    }

    public void sendNewPosition(GameSession session, String s) {
        sendCommand(session, "stop");
        sendCommand(session, s);
    }

    public void sendNewPosition(String s) {
        sendNewPosition(model.getGameSession(), s);
    }

    public void uciGoMoveTime(GameSession session, int milliseconds) {
        sendCommand(session, "go movetime " + milliseconds);
    }

    public void uciGoMoveTime(int milliseconds) {
        uciGoMoveTime(model.getGameSession(), milliseconds);
    }

    public void uciGoInfinite(GameSession session) {
        sendCommand(session, "go infinite");
    }

    public void uciGoInfinite() {
        uciGoInfinite(model.getGameSession());
    }

    public void activateAnalysisMode() {
        activateAnalysisMode(model.getGameSession());
    }

    public void activateAnalysisMode(GameSession session) {
        if (session == null) {
            return;
        }
        stopEngine(session);
        Engine analysisEngine;
        if (session.getActiveEngine() != null && !session.getActiveEngine().isInternal() && !session.getActiveEngine().getName().equals(CONSTANTS.INTERNAL_ENGINE_NAME) && !(session.getActiveEngine() instanceof BotEngine)) {
            analysisEngine = session.getActiveEngine().makeCopy();
        } else {
            analysisEngine = model.selectedAnalysisEngine.makeCopy();
        }
        if (analysisEngine.supportsUciLimitStrength()) {
            analysisEngine.setUciLimitStrength(false);
        }
        session.setActiveEngine(analysisEngine);
        if (session == model.getGameSession()) {
            model.activeEngine = analysisEngine;
        }
        restartEngine(session, analysisEngine);
        setMultiPV(session, model.getMultiPv());
        session.setBlockGui(false);
        session.setMode(Model_JFXChess.MODE_ANALYSIS);
        String fen = session.getGame().getUciPositionString();
        sendNewPosition(session, fen);
        uciGoInfinite(session);
    }

    public ActionListener startAnalysisMode() {
        return e -> {
            activateAnalysisMode();
        };
    }

    public void activateEnterMovesMode() {
        activateEnterMovesMode(model.getGameSession());
    }

    public void activateEnterMovesMode(GameSession session) {
        if (session == null) {
            return;
        }
        stopEngine(session);
        session.setBlockGui(false);
        session.setMode(Model_JFXChess.MODE_ENTER_MOVES);
    }

    public ActionListener startEnterMovesMode() {
        return e -> {
            activateEnterMovesMode();
        };
    }

    public ActionListener toggleEngine(GameSession session) {
        return e -> {
            if (session == null) {
                return;
            }
            int currentMode = session.getMode();
            if (currentMode == Model_JFXChess.MODE_ANALYSIS
                    || currentMode == Model_JFXChess.MODE_PLAY_WHITE
                    || currentMode == Model_JFXChess.MODE_PLAY_BLACK
                    || currentMode == Model_JFXChess.MODE_PLAYOUT_POSITION
                    || currentMode == Model_JFXChess.MODE_GAME_ANALYSIS) {
                if (currentMode == Model_JFXChess.MODE_PLAY_WHITE || currentMode == Model_JFXChess.MODE_PLAY_BLACK) {
                    session.setPreviousPlayMode(currentMode);
                }
                activateEnterMovesMode(session);
            } else {
                int prevMode = session.getPreviousPlayMode();
                Engine eng = session.getActiveEngine();
                if (eng == null) {
                    eng = model.selectedPlayEngine;
                }
                if (prevMode == Model_JFXChess.MODE_PLAY_WHITE) {
                    activatePlayWhiteMode(session, eng);
                } else if (prevMode == Model_JFXChess.MODE_PLAY_BLACK) {
                    activatePlayBlackMode(session, eng);
                } else {
                    activateAnalysisMode(session);
                }
            }
        };
    }

    private void handleNewBoardPositionModeAnalysis() {
        handleNewBoardPositionModeAnalysis(model.getGameSession());
    }

    private void handleNewBoardPositionModeAnalysis(GameSession session) {
        if (session == null) {
            return;
        }
        String fen = session.getGame().getUciPositionString();
        sendNewPosition(session, fen);
        uciGoInfinite(session);
    }

    private void handleNewBoardPositionModeGameAnalysis() {

        boolean continueAnalysis = true;

        boolean parentIsRoot = (model.getGame().getCurrentNode().getParent() == model.getGame().getRootNode());
        if (!parentIsRoot) {
            // if the current position is in the opening book,
            // we stop the analysis
            long zobrist = model.getGame().getCurrentNode().getBoard().getZobrist();
            if (model.extBook.inBook(zobrist)) {
                model.getGame().getCurrentNode().setComment("last book move");
                continueAnalysis = false;
            } else {
                String fen = model.getGame().getUciPositionString();
                sendNewPosition(fen);
                uciGoMoveTime(model.getGameAnalysisThinkTimeSecs() * 1000);
            }
        } else {
            continueAnalysis = false;
        }

        if (!continueAnalysis) {
            // we are at the root or found a book move
            // should fire an event
            activateEnterMovesMode();
            JOptionPane.showMessageDialog(model.mainFrameRef, "Game Analysis Finished");
        }
    }

    public void handleNewBoardPositionModePlayout() {
        handleNewBoardPositionModePlayout(model.getGameSession());
    }

    public void handleNewBoardPositionModePlayout(GameSession session) {
        if (session == null) {
            return;
        }
        String fen = session.getGame().getUciPositionString();
        sendNewPosition(session, fen);
        uciGoMoveTime(session, session.getComputerThinkTimeSecs() * 1000);
    }

    public ActionListener startNewGame() {
        return e -> {
            model.setShortcutsEnabled(false);
            DialogNewGame dlg = new DialogNewGame(model.mainFrameRef, "New Game");
            dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dlg.setVisible(true);
            model.setShortcutsEnabled(true);
            int result = dlg.getSelection();
            if(result >= 0) {
                if(result == DialogNewGame.ENTER_ANALYSE) {
                    Game g = new Game();
                    Board b = new Board(true);
                    g.getRootNode().setBoard(b);
                    GameSession newSession = model.openGameInNewSession(g);
                    Engine analysisEngine = model.selectedAnalysisEngine.makeCopy();
                    newSession.setActiveEngine(analysisEngine);
                    newSession.setComputerThinkTimeSecs(3);
                    newSession.setFlipBoard(false);
                    newSession.goToNode(newSession.getGame().getRootNode().getId());
                    activateEnterMovesMode(newSession);
                }
                if(result == DialogNewGame.PLAY_BOT) {
                    DialogPlayBot dlgPlayBot = new DialogPlayBot(model.mainFrameRef, model.botEngines);
                    dlgPlayBot.setVisible(true);
                    if(dlgPlayBot.isConfirmed()) {
                        model.wasSaved = false;
                        Game g = new Game();
                        Board b;
                        if(dlgPlayBot.getPlayInitialPosition()) {
                            b = new Board(true);
                        } else {
                            b = model.getGame().getCurrentNode().getBoard().makeCopy();
                        }
                        g.getRootNode().setBoard(b);
                        BotEngine bot = model.botEngines.get(dlgPlayBot.getBotIndex()).makeCopy();
                        model.selectedPlayEngine = bot;
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
                        String formattedDate = LocalDate.now().format(formatter);
                        g.setHeader("Date", formattedDate);
                        g.setHeader("Event", "Training Game JFXChess");
                        if(dlgPlayBot.getPlayerColor() == CONSTANTS.WHITE) {
                            g.setHeader("White", "N.N.");
                            g.setHeader("Black", bot.getName());
                            g.setHeader("BlackElo", bot.getElo());
                            GameSession newSession = model.openGameInNewSession(g);
                            newSession.setActiveEngine(bot);
                            newSession.setComputerThinkTimeSecs(3);
                            newSession.setFlipBoard(false);
                            activatePlayWhiteMode(newSession, bot);
                        } else {
                            g.setHeader("Black", "N.N.");
                            g.setHeader("White", bot.getName());
                            g.setHeader("WhiteElo", bot.getElo());
                            GameSession newSession = model.openGameInNewSession(g);
                            newSession.setActiveEngine(bot);
                            newSession.setComputerThinkTimeSecs(3);
                            newSession.setFlipBoard(true);
                            activatePlayBlackMode(newSession, bot);
                        }
                    }
                }
                if(result == DialogNewGame.PLAY_UCI) {
                    DialogPlayEngine dlgUci = new DialogPlayEngine(model.mainFrameRef, model.engines);
                    dlgUci.setVisible(true);
                    boolean uciAccepted = dlgUci.isConfirmed();
                    if(uciAccepted) {
                        model.wasSaved = false;
                        Game g = new Game();
                        Board b;
                        if(dlgUci.getPlayInitialPosition()) {
                            b = new Board(true);
                        } else {
                            b = model.getGame().getCurrentNode().getBoard().makeCopy();
                        }
                        g.getRootNode().setBoard(b);
                        Engine uciEngine = model.engines.get(dlgUci.getSelectedEngineIdx()).makeCopy();
                        if(uciEngine.supportsUciLimitStrength()) {
                            int newElo = dlgUci.getElo();
                            if (newElo >= uciEngine.getMinUciElo()
                                    && newElo <= uciEngine.getMaxUciElo()) {
                                uciEngine.setUciElo(newElo);
                                uciEngine.setUciLimitStrength(true);
                            }
                        }
                        model.selectedPlayEngine = uciEngine;
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
                        String formattedDate = LocalDate.now().format(formatter);
                        g.setHeader("Date", formattedDate);
                        g.setHeader("Event", "Training Game JFXChess");
                        if(dlgUci.getPlayerColor() == CONSTANTS.WHITE) {
                            g.setHeader("White", "N.N.");
                            g.setHeader("Black", uciEngine.getName());
                            g.setHeader("BlackElo", String.valueOf(uciEngine.getUciElo()));
                            GameSession newSession = model.openGameInNewSession(g);
                            newSession.setActiveEngine(uciEngine);
                            newSession.setComputerThinkTimeSecs(3);
                            newSession.setFlipBoard(false);
                            activatePlayWhiteMode(newSession, uciEngine);
                        } else {
                            g.setHeader("Black", "N.N.");
                            g.setHeader("White", uciEngine.getName());
                            g.setHeader("WhiteElo", String.valueOf(uciEngine.getUciElo()));
                            GameSession newSession = model.openGameInNewSession(g);
                            newSession.setActiveEngine(uciEngine);
                            newSession.setComputerThinkTimeSecs(3);
                            newSession.setFlipBoard(true);
                            activatePlayBlackMode(newSession, uciEngine);
                        }
                    }
                }
            }
        };
    }

    public void activatePlayWhiteMode() {
        activatePlayWhiteMode(model.getGameSession(), model.selectedPlayEngine);
    }

    public void activatePlayWhiteMode(GameSession session, Engine engine) {
        if (session == null || engine == null) {
            return;
        }
        Engine activeEngine = engine.makeCopy();
        if (!(activeEngine instanceof BotEngine)) {
            if (activeEngine.supportsUciLimitStrength()) {
                activeEngine.setUciLimitStrength(true);
            }
        }
        session.setActiveEngine(activeEngine);
        if (session == model.getGameSession()) {
            model.activeEngine = activeEngine;
        }
        restartEngine(session, activeEngine);

        session.setFlipBoard(false);
        session.setHumanPlayerColor(CONSTANTS.WHITE);
        session.setMode(Model_JFXChess.MODE_PLAY_WHITE);
    }

    public void activatePlayBlackMode() {
        activatePlayBlackMode(model.getGameSession(), model.selectedPlayEngine);
    }

    public void activatePlayBlackMode(GameSession session, Engine engine) {
        if (session == null || engine == null) {
            return;
        }
        Engine activeEngine = engine.makeCopy();
        if (!(activeEngine instanceof BotEngine)) {
            if (activeEngine.supportsUciLimitStrength()) {
                activeEngine.setUciLimitStrength(true);
            }
        }
        session.setActiveEngine(activeEngine);
        if (session == model.getGameSession()) {
            model.activeEngine = activeEngine;
        }
        restartEngine(session, activeEngine);
        session.setFlipBoard(true);
        session.setHumanPlayerColor(CONSTANTS.BLACK);
        session.setMode(Model_JFXChess.MODE_PLAY_BLACK);
        session.setBlockGui(true);
        handleNewBoardPositionModePlay(session);
    }

    public void activatePlayoutPositionMode() {
        activatePlayoutPositionMode(model.getGameSession());
    }

    public void activatePlayoutPositionMode(GameSession session) {
        if (session == null) {
            return;
        }
        Engine activeEngine = model.selectedAnalysisEngine.makeCopy();
        if (activeEngine.supportsUciLimitStrength()) {
            activeEngine.setUciLimitStrength(false);
        }
        session.setActiveEngine(activeEngine);
        if (session == model.getGameSession()) {
            model.activeEngine = activeEngine;
        }
        restartEngine(session, activeEngine);
        session.setFlipBoard(false);
        session.setMode(Model_JFXChess.MODE_PLAYOUT_POSITION);
        handleNewBoardPositionModePlayout(session);
    }

    public void activateGameAnalysisMode() {

        model.getGame().removeAllComments();
        model.getGame().removeAllVariants();
        model.getGame().removeAllAnnotations();

        model.activeEngine = model.selectedAnalysisEngine;
        if (model.activeEngine.supportsUciLimitStrength()) {
            model.activeEngine.setUciLimitStrength(false);
        }
        if (model.activeEngine.supportsMultiPV()) {
            model.activeEngine.setMultiPV(1);
        }
        restartEngine(model.activeEngine);

        model.setFlipBoard(false);
        model.getGame().goToRoot();
        model.getGame().goToLeaf();
        if (model.getGame().getCurrentNode().getBoard().isCheckmate()) {
            model.currentIsMate = true;
            model.currentMateInMoves = 0;
        }
        model.setMode(Model_JFXChess.MODE_GAME_ANALYSIS);

        String fen = model.getGame().getUciPositionString();
        sendNewPosition(fen);
        uciGoMoveTime(model.getGameAnalysisThinkTimeSecs() * 1000);
    }

    public ActionListener startGameAnalysisMode() {
        return e -> {
            model.setShortcutsEnabled(false);
            DialogGameAnalysis dlg = new DialogGameAnalysis(model.mainFrameRef);
            dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dlg.setVisible(true);
            model.setShortcutsEnabled(true);

            if(dlg.isConfirmed()) {
                model.setGameAnalysisForPlayer(dlg.getSelectedPlayer());
                model.setGameAnalysisThinkTimeSecs(dlg.getSecondsPerMove());
                model.setGameAnalysisThreshold(dlg.getThreshold());
                activateGameAnalysisMode();
            }
        };
    }

    public ActionListener startPlayoutPositionMode() {
        return e -> {
            activatePlayoutPositionMode();
        };
    }


    public void handleNewBoardPositionModePlay() {
        handleNewBoardPositionModePlay(model.getGameSession());
    }

    public void handleNewBoardPositionModePlay(GameSession session) {
        if (session == null) {
            return;
        }
        // first check if we can apply a bookmove
        long zobrist = session.getGame().getCurrentNode().getBoard().getZobrist();
        boolean maxDepthReached = false;
        int currentDepth = session.getGame().getCurrentNode().getDepth();
        Engine activeEngine = session.getActiveEngine();
        int currentElo = 0;
        boolean limitElo = false;
        if (activeEngine != null) {
            currentElo = activeEngine.getUciElo();
            limitElo = activeEngine.getUciLimitStrength();
        }
        // engine supports setting ELO
        // let's limit book knowledge according to ELO
        if (limitElo && (currentElo > 0)) {
            if (currentElo <= 1200 && currentDepth > 10) {
                maxDepthReached = true;
            }
            if (currentElo > 1200 && currentElo < 1400 && currentDepth > 10) {
                maxDepthReached = true;
            }
            if (currentElo > 1400 && currentElo < 1600 && currentDepth > 12) {
                maxDepthReached = true;
            }
            if (currentElo > 1600 && currentElo < 1800 && currentDepth > 14) {
                maxDepthReached = true;
            }
            if (currentElo > 1800 && currentElo < 2000 && currentDepth > 16) {
                maxDepthReached = true;
            }
            if (currentElo > 2000 && currentElo < 2200 && currentDepth > 18) {
                maxDepthReached = true;
            }
        }

        String bookMove = model.extBook.getRandomMove(zobrist);
        if ((bookMove != null) && (!maxDepthReached)) {
            // don't execute the book move immediately; we want to
            // create the illusion of the computer thinking about his move
            CompletableFuture.delayedExecutor(
                    ((long) session.getComputerThinkTimeSecs() * 1000),
                    TimeUnit.MILLISECONDS
            ).execute(() -> handleBestMove(session, "BESTMOVE|" + bookMove + "|" + zobrist));

        } else {
            String fen = session.getGame().getUciPositionString();
            sendNewPosition(session, fen);
            uciGoMoveTime(session, session.getComputerThinkTimeSecs() * 1000);
        }
        if (session.getMode() == Model_JFXChess.MODE_PLAY_WHITE && session.getGame().getCurrentNode().getBoard().turn == CONSTANTS.BLACK) {
            session.setBlockGui(true);
            if (session == model.getGameSession()) {
                model.setBlockGUI(true);
            }
        }
        if (session.getMode() == Model_JFXChess.MODE_PLAY_BLACK && session.getGame().getCurrentNode().getBoard().turn == CONSTANTS.WHITE) {
            session.setBlockGui(true);
            if (session == model.getGameSession()) {
                model.setBlockGUI(true);
            }
        }
    }

    private void addBestPv(String[] uciMoves) {
        addBestPv(model.getGameSession(), uciMoves);
    }

    private void addBestPv(GameSession session, String[] uciMoves) {
        if (session == null) {
            return;
        }
        GameNode currentNode = session.getGame().getCurrentNode();

        for (String uciMove : uciMoves) {
            try {
                GameNode next = new GameNode();
                Board board = currentNode.getBoard().makeCopy();
                Move m = new Move(uciMove);
                if (!board.isLegal(m)) {
                    break;
                }
                board.apply(m);
                next.setMove(m);
                next.setBoard(board);
                next.setParent(currentNode);
                // to avoid bugs when incoherent information is
                // given/received by the engine, do not add lines that already exist
                if (currentNode.getVariations().size() > 0) {
                    String mUciChild0 = currentNode.getVariation(0).getMove().getUci();
                    if (mUciChild0.equals(uciMove)) {
                        break;
                    }
                }
                currentNode.addVariation(next);
                currentNode = next;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    public void handleBestMove(String bestmove) {
        handleBestMove(model.getGameSession(), bestmove);
    }

    public void handleBestMove(GameSession session, String bestmove) {
        if (session == null) {
            return;
        }
        int mode = session.getMode();
        if (mode == Model_JFXChess.MODE_ENTER_MOVES) {
            return;
        }
        String[] bestmoveItems = bestmove.split("\\|");

        String zobristString = bestmoveItems[bestmoveItems.length - 1];
        long zobrist = Long.parseLong(zobristString);

        if (zobrist != session.getGame().getCurrentNode().getBoard().getZobrist()) {
            // if this bestmove is for a different position than the current,
            // it is a relict from thread/gui synchronisation mismatch; we just dismiss it
            return;
        } else {
            // If not, this is a bestmove from either playing the engine
            // Then we need to unlock the GUI as
            // the user wants to potentially react to that
            session.setBlockGui(false);
            if (session == model.getGameSession()) {
                model.setBlockGUI(false);
            }
        }

        if (mode == Model_JFXChess.MODE_PLAY_WHITE ||
                mode == Model_JFXChess.MODE_PLAY_BLACK  ||
                mode == Model_JFXChess.MODE_PLAYOUT_POSITION) {

            // todo: catch Exceptions!
            String uci = bestmoveItems[1].split(" ")[0];
            Move m = new Move(uci);
            Board b = session.getGame().getCurrentNode().getBoard();
            if (b.isLegal(m)) {
                if (mode == Model_JFXChess.MODE_PLAY_WHITE && b.turn == CONSTANTS.BLACK) {
                    session.applyMove(m);
                    if (session == model.getGameSession()) {
                        model.notifySessionPositionChanged(session);
                    } else {
                        handleNewBoardPosition(session);
                    }
                } else if (mode == Model_JFXChess.MODE_PLAY_BLACK && b.turn == CONSTANTS.WHITE) {
                    session.applyMove(m);
                    if (session == model.getGameSession()) {
                        model.notifySessionPositionChanged(session);
                    } else {
                        handleNewBoardPosition(session);
                    }
                } else if (mode == Model_JFXChess.MODE_PLAYOUT_POSITION) {
                    session.applyMove(m);
                    if (session == model.getGameSession()) {
                        model.notifySessionPositionChanged(session);
                    } else {
                        handleNewBoardPosition(session);
                    }
                }
            }
        }

        if (mode == Model_JFXChess.MODE_GAME_ANALYSIS) {

            // first update information for current node
            model.childBestPv = model.currentBestPv;
            model.childBestEval = model.currentBestEval;
            model.childIsMate = model.currentIsMate;
            model.childMateInMoves = model.currentMateInMoves;

            model.currentBestPv = bestmoveItems[3];
            model.currentBestEval = Integer.parseInt(bestmoveItems[2]);
            model.currentIsMate = bestmoveItems[4].equals("true");
            model.currentMateInMoves = Integer.parseInt(bestmoveItems[5]);

            // double check, since some engines misreport / report not quickly enough
            if (session.getGame().getCurrentNode().getBoard().isCheckmate()) {
                model.currentIsMate = true;
            }

            // ignore leafs (game ended here)
            if (!session.getGame().getCurrentNode().isLeaf()) {

                // completely skip analysis for black or white, if
                // that option was chosen
                boolean turn = session.getGame().getCurrentNode().getBoard().turn;
                if ((model.getGameAnalysisForPlayer() == Model_JFXChess.BOTH_PLAYERS)
                        || (model.getGameAnalysisForPlayer() == CONSTANTS.IWHITE && turn == CONSTANTS.WHITE)
                        || (model.getGameAnalysisForPlayer() == CONSTANTS.IBLACK && turn == CONSTANTS.BLACK)) {

                    int centiPawnThreshold = (int) (model.getGameAnalysisThreshold() * 100.0);
                    // first case if there was simply a better move; i.e. no checkmate overseen or
                    // moved into a checkmate
                    if (!model.currentIsMate && !model.childIsMate) {
                        boolean wMistake = turn == CONSTANTS.WHITE && ((model.currentBestEval - model.childBestEval) >= centiPawnThreshold);
                        boolean bMistake = turn == CONSTANTS.BLACK && ((model.currentBestEval - model.childBestEval) <= -(centiPawnThreshold));

                        if (wMistake || bMistake) {
                            String uci = bestmoveItems[1].split(" ")[0];
                            String nextMove = session.getGame().getCurrentNode().getVariation(0).getMove().getUci();
                            String[] pvMoves = model.currentBestPv.split(" ");
                            // if the bestmove returned by the engine is different
                            // from the best suggested pv line, it means that e.g. the
                            // engine took a book move, but did not give a pv evaluation
                            // or there was some major async error between engine and gui
                            // in such a case, do not add the best pv line, as it is probably
                            // not a valid pv line for the current node
                            // we also do not want to add the same line as the child
                            if (!uci.equals(nextMove) && pvMoves.length > 0 && pvMoves[0].equals(uci)) {

                                addBestPv(session, pvMoves);

                                NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
                                DecimalFormat decim = (DecimalFormat) nf;
                                decim.applyPattern("0.00");
                                String sCurrentBest = decim.format(model.currentBestEval / 100.0);
                                String sChildBest = decim.format(model.childBestEval / 100.0);

                                ArrayList<GameNode> vars = session.getGame().getCurrentNode().getVariations();
                                if (vars != null && vars.size() > 1) {
                                    GameNode child0 = session.getGame().getCurrentNode().getVariation(0);
                                    child0.setComment(sChildBest);
                                    GameNode child1 = session.getGame().getCurrentNode().getVariation(1);
                                    child1.setComment(sCurrentBest);
                                }
                            }
                        }
                    }

                    if (model.currentIsMate && !model.childIsMate) {
                        // the current player missed a mate
                        String[] pvMoves = model.currentBestPv.split(" ");
                        addBestPv(session, pvMoves);

                        NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
                        DecimalFormat decim = (DecimalFormat) nf;
                        decim.applyPattern("0.00");
                        String sChildBest = decim.format(model.childBestEval / 100.0);

                        String sCurrentBest;
                        if (turn == CONSTANTS.WHITE) {
                            sCurrentBest = "#" + (Math.abs(model.currentMateInMoves));
                        } else {
                            sCurrentBest = "#-" + (Math.abs(model.currentMateInMoves));
                        }

                        ArrayList<GameNode> vars = session.getGame().getCurrentNode().getVariations();
                        if (vars != null && vars.size() > 1) {
                            GameNode child0 = session.getGame().getCurrentNode().getVariation(0);
                            child0.setComment(sChildBest);
                            GameNode child1 = session.getGame().getCurrentNode().getVariation(1);
                            child1.setComment(sCurrentBest);
                        }
                    }

                    if (!model.currentIsMate && model.childIsMate) {
                        // the current player  moved into a mate
                        String[] pvMoves = model.currentBestPv.split(" ");
                        addBestPv(session, pvMoves);

                        NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
                        DecimalFormat decim = (DecimalFormat) nf;
                        decim.applyPattern("0.00");
                        String sCurrentBest = decim.format(model.currentBestEval / 100.0);

                        String sChildBest;
                        if (turn == CONSTANTS.WHITE) {
                            sChildBest = "#-" + (Math.abs(model.childMateInMoves));
                        } else {
                            sChildBest = "#" + (Math.abs(model.childMateInMoves));
                        }

                        ArrayList<GameNode> vars = session.getGame().getCurrentNode().getVariations();
                        if (vars != null && vars.size() > 1) {
                            GameNode child0 = session.getGame().getCurrentNode().getVariation(0);
                            child0.setComment(sChildBest);
                            GameNode child1 = session.getGame().getCurrentNode().getVariation(1);
                            child1.setComment(sCurrentBest);
                        }
                    }

                    if (model.currentIsMate && model.childIsMate) {
                        // the current player had a mate, but instead of executing it, he moved into a mate,
                        // but we also want to skip the situation where the board position is checkmate
                        if ((model.currentMateInMoves >= 0 && model.childMateInMoves >= 0) &&
                                model.childMateInMoves != 0) {

                            String[] pvMoves = model.currentBestPv.split(" ");
                            addBestPv(session, pvMoves);

                            String sCurrentBest;
                            String sChildBest;
                            if (turn == CONSTANTS.WHITE) {
                                sCurrentBest = "#" + (Math.abs(model.currentMateInMoves));
                                sChildBest = "#-" + (Math.abs(model.childMateInMoves));
                            } else {
                                sCurrentBest = "#-" + (Math.abs(model.currentMateInMoves));
                                sChildBest = "#" + (Math.abs(model.childMateInMoves));
                            }

                            ArrayList<GameNode> vars = session.getGame().getCurrentNode().getVariations();
                            if (vars != null && vars.size() > 1) {
                                GameNode child0 = session.getGame().getCurrentNode().getVariation(0);
                                child0.setComment(sChildBest);
                                GameNode child1 = session.getGame().getCurrentNode().getVariation(1);
                                child1.setComment(sCurrentBest);
                            }
                        }
                    }
                }
            }
            session.markDirty();
            if (session == model.getGameSession()) {
                model.markTreeChange();
                model.goToParent();
            } else {
                session.getGame().goToParent();
            }
        }

    }

    public void handleNewEngineInfo(String s) {
        handleNewEngineInfo(model.getGameSession(), s);
    }

    public void handleNewEngineInfo(GameSession session, String s) {
        if (session == null) {
            return;
        }
        // we show info only during analysis, not when playing
        // against the bots/engine
        if (s.startsWith("INFO")) {
            if ((session.getMode() != Model_JFXChess.MODE_PLAY_BLACK) &&
                    (session.getMode() != Model_JFXChess.MODE_PLAY_WHITE)) {
                session.setCurrentEngineInfo(s);
                if (session == model.getGameSession()) {
                    model.setCurrentEngineInfo(s);
                }
            }
        }

        // if we get info from the engine but currently play against it,
        // just show some dummy info
        if ((session.getMode() == Model_JFXChess.MODE_PLAY_WHITE) || (session.getMode() == Model_JFXChess.MODE_PLAY_BLACK)) {
            if (session.getMode() == Model_JFXChess.MODE_PLAY_WHITE) {
                if (session.getGame().getCurrentNode().getBoard().turn == CONSTANTS.WHITE) {
                    String msg = playInfo.replace("MESSAGE", "Your turn - White to move");
                    session.setCurrentEngineInfo(msg);
                    if (session == model.getGameSession()) {
                        model.setCurrentEngineInfo(msg);
                    }
                } else {
                    String msg = playInfo.replace("MESSAGE", "thinking...");
                    session.setCurrentEngineInfo(msg);
                    if (session == model.getGameSession()) {
                        model.setCurrentEngineInfo(msg);
                    }
                }
            }
            if (session.getMode() == Model_JFXChess.MODE_PLAY_BLACK) {
                if (session.getGame().getCurrentNode().getBoard().turn == CONSTANTS.BLACK) {
                    String msg = playInfo.replace("MESSAGE", "Your turn - Black to move");
                    session.setCurrentEngineInfo(msg);
                    if (session == model.getGameSession()) {
                        model.setCurrentEngineInfo(msg);
                    }
                } else {
                    String msg = playInfo.replace("MESSAGE", "thinking...");
                    session.setCurrentEngineInfo(msg);
                    if (session == model.getGameSession()) {
                        model.setCurrentEngineInfo(msg);
                    }
                }
            }
        }
        if (s.startsWith("BESTMOVE")) {
            handleBestMove(session, s);
        }
    }

    public void handleNewBoardPosition() {
        handleNewBoardPosition(model.getGameSession());
    }

    public void handleNewBoardPosition(GameSession session) {
        if (session == null) {
            return;
        }
        int mode = session.getMode();
        Board board = session.getGame().getCurrentNode().getBoard();
        boolean turn = board.turn;

        boolean isCheckmate = board.isCheckmate();
        boolean isStalemate = board.isStalemate();
        boolean isThreefoldRepetition = session.getGame().isThreefoldRepetition();
        boolean isInsufficientMaterial = session.getGame().isInsufficientMaterial();

        boolean abort = false;

        // first we check if the game is finished due to checkmate, stalemate, three-fold repetition
        // or insufficient material
        // if that is the case
        //      we check if we play against the computer or are in MODE_PLAYOUT_POSITION
        //         if so, we are going to abort the game and switch to MODE_ENTER_MOVES
        //      then we check if we play against the computer
        //         if so, we are going to inform the user

        if ((isCheckmate || isStalemate || isThreefoldRepetition || isInsufficientMaterial)) {
            if (mode == Model_JFXChess.MODE_PLAY_WHITE || mode == Model_JFXChess.MODE_PLAY_BLACK || mode == Model_JFXChess.MODE_PLAYOUT_POSITION) {
                abort = true;
                // always display a result message, also if engines are playing against each other
                String message = "";
                if (isCheckmate) {
                    message = "Checkmate";
                }
                if (isStalemate) {
                    message = "Stalemate";
                }
                if (isThreefoldRepetition) {
                    message = "Draw (Threefold Repetition)";
                }
                if (isInsufficientMaterial) {
                    message = "Draw (Insufficient material for checkmate)";
                }
                if (model.mainFrameRef != null) {
                    JOptionPane.showMessageDialog(model.mainFrameRef, message);
                }
            }
        }
        if (abort) {
            activateEnterMovesMode(session);
            if (isCheckmate) {
                // white to move, but cannot: black checkmated
                if (board.turn == CONSTANTS.WHITE) {
                    session.getGame().setResult(CONSTANTS.RES_BLACK_WINS);
                } else {
                    session.getGame().setResult(CONSTANTS.RES_WHITE_WINS);
                }
            }
            if (isStalemate) {
                session.getGame().setResult(CONSTANTS.RES_DRAW);
            }
            if (isThreefoldRepetition) {
                session.getGame().setResult(CONSTANTS.RES_DRAW);
            }
            if (isInsufficientMaterial) {
                session.getGame().setResult(CONSTANTS.RES_DRAW);
            }
        } else {
            if (mode == Model_JFXChess.MODE_ANALYSIS) {
                handleNewBoardPositionModeAnalysis(session);
            }
            if (mode == Model_JFXChess.MODE_GAME_ANALYSIS) {
                handleNewBoardPositionModeGameAnalysis();
            }
            if (mode == Model_JFXChess.MODE_PLAYOUT_POSITION) {
                handleNewBoardPositionModePlayout(session);
            }
            if ((mode == Model_JFXChess.MODE_PLAY_WHITE || mode == Model_JFXChess.MODE_PLAY_BLACK)
                    && turn != session.getHumanPlayerColor()) {
                handleNewBoardPositionModePlay(session);
            }
        }
    }


    public ActionListener editEngines() {
        return e -> {
            activateEnterMovesMode();
            int idxActiveEngine = model.engines.indexOf(model.activeEngine);
            model.setShortcutsEnabled(false);
            DialogEngines dlgEngines = new DialogEngines(model.mainFrameRef, model.engines, idxActiveEngine);
            dlgEngines.setVisible(true);
            model.setShortcutsEnabled(true);
            if (dlgEngines.isConfirmed()) {
                model.engines = dlgEngines.getEngines();
                model.activeEngine = dlgEngines.getSelectedEngine();
                model.selectedAnalysisEngine = model.activeEngine;
                GameSession currentSession = model.getGameSession();
                if (currentSession != null) {
                    currentSession.setActiveEngine(model.activeEngine.makeCopy());
                }
            }
        };
    }


    // this is to get the engine info thread safe as a String
    // via the evt new value
    // but also to listen to position changes that occur in the model
    @Override
    public void propertyChange(PropertyChangeEvent evt) {

        if (evt.getPropertyName().equals("engineInfoFromThread")) {
            handleNewEngineInfo((String) evt.getNewValue());
        }
        if (evt.getPropertyName().equals("gameChanged")) {
            listenToActiveEngineSession();
        }
        if (evt.getPropertyName().equals("currentGameNodeChanged")) {
            handleNewBoardPosition();
        }
        if (evt.getPropertyName().equals("treeChanged")) {
            // do nothing - we should only react, if the current game
            // node changed. This is always done, if the tree changes
        }

    }

    public ActionListener changeNrThreads() {
        return changeNrThreads(model.getGameSession());
    }

    public ActionListener changeNrThreads(GameSession session) {
        return e -> {
            GameSession targetSession = session;
            if (targetSession == null) {
                targetSession = model.getGameSession();
            }
            if (targetSession == null) {
                return;
            }
            if (!targetSession.isBlockGui()) {
                Engine activeEngine = targetSession.getActiveEngine();
                if (activeEngine != null && activeEngine.supportsMultiThread()) {
                    activateEnterMovesMode(targetSession);
                    int currentNrThreads = activeEngine.getNrThreads();
                    int maxCpusAvail = model.maxCpus - 1;
                    if (maxCpusAvail <= 0) {
                        maxCpusAvail = 1;
                    }
                    int maxThreads = Math.min(maxCpusAvail, activeEngine.getMaxThreads());
                    model.setShortcutsEnabled(false);
                    DialogThreads dlgThreads = new DialogThreads(model.mainFrameRef, 1, maxThreads, currentNrThreads);
                    dlgThreads.setVisible(true);
                    model.setShortcutsEnabled(true);
                    if (dlgThreads.isConfirmed()) {
                        int newNrThreads = dlgThreads.getNrThreads();
                        if (newNrThreads != currentNrThreads) {
                            activeEngine.setThreads(newNrThreads);
                            if (targetSession == model.getGameSession()) {
                                model.setNrThreads(newNrThreads);
                            }
                        }
                    }
                }
            }
        };
    }

}
