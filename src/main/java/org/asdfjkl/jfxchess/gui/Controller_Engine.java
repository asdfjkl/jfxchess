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
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Controller_Engine implements PropertyChangeListener {

    private final Model_JFXChess model;
    final EngineThread engineThread;
    final BlockingQueue<String> cmdQueue = new LinkedBlockingQueue<>();
    Engine currentEngine = null;
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

        engineThread = new EngineThread(cmdQueue);
        engineThread.addPropertyChangeListener(this);
        engineThread.start();
    }

    public void sendCommand(String cmd) {
        try {
            cmdQueue.put(cmd);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stopEngine() {
        sendCommand("stop");
        sendCommand("quit");
    }

    public void restartEngine(Engine activeEngine) {
        currentEngine = activeEngine;
        // It's OK to send stop and quit even if the engine process
        // inside the engine thread is not running. These commands will
        // just be consumed by the engine thread in that case.
        stopEngine();
        // Restart engine.
        sendCommand("start " + activeEngine.getPath());
        int countMs = 0;
        do {
            try {
                Thread.sleep(10);
                countMs += 10;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (!engineThread.engineIsOn() && countMs < 1500);


        // Since the engine is either internal, or we have
        // been able to set up the engine in the
        // Mode->Engines... dialog, we know it's a UCI-engine.
        // But since some engines won't accept commands prior
        // to uci, so:
        sendCommand("uci");
        // Here the engine thread will wait for uciok from engine,
        // which is according to the UCI-protocol, but meanwhile we
        // continue with pumping setoption commands into the cmdQueue.
        for (EngineOption enOpt : activeEngine.options) {
            if (enOpt.isNotDefault()) {
                sendCommand(enOpt.toUciCommand());
            }
        }
        // The engine thread requires an isready
        // before any other commands can be sent,
        // except for uci, setoption and quit.
        sendCommand("isready");
        sendCommand("ucinewgame");
        // An isready should be sent after ucinewgame.
        sendCommand("isready");
    }

    public void setMultiPV(int n) {
        if (currentEngine != null && currentEngine.supportsMultiPV()) {
            sendCommand("setoption name MultiPV value " + n);
        }
        // in case the engine hasn't been started yet:
        engineThread.engineInfoSetPVLines(n);
    }

    public ActionListener incMultiPV() {
        return e -> {
            // we allow changing pv only in analysis mode, otherwise ignore
            if(model.getMode() == Model_JFXChess.MODE_ANALYSIS) {
                int currentMultiPv = model.getMultiPv();
                if (currentMultiPv < model.activeEngine.getMaxMultiPV() &&
                        currentMultiPv < Model_JFXChess.MAX_PV) {
                    currentMultiPv++;
                    model.setMultiPv(currentMultiPv);
                    sendCommand("stop");
                    sendCommand("isready");
                    sendCommand("setoption name MultiPV value " + currentMultiPv);
                    sendCommand("isready");
                    sendCommand("go infinite");
                }
            }
        };
    }

    public ActionListener decMultiPV() {
        return e -> {
            // we allow changing pv only in analysis mode, otherwise ignore
            if(model.getMode() == Model_JFXChess.MODE_ANALYSIS) {
                int currentMultiPv = model.getMultiPv();
                if (currentMultiPv > 1) {
                    currentMultiPv--;
                    model.setMultiPv(currentMultiPv);
                    sendCommand("stop");
                    sendCommand("setoption name MultiPV value " + currentMultiPv);
                    sendCommand("go infinite");
                }
            }
        };
    }

    public void sendNewPosition(String s) {
        sendCommand("stop");
        sendCommand(s);
    }

    public void uciGoMoveTime(int milliseconds) {
        sendCommand("go movetime " + milliseconds);
    }

    public void uciGoInfinite() {
        sendCommand("go infinite");
    }

    public void activateAnalysisMode() {
        stopEngine();
        model.activeEngine = model.selectedAnalysisEngine;
        if (model.activeEngine.supportsUciLimitStrength()) {
            model.activeEngine.setUciLimitStrength(false);
        }
        restartEngine(model.activeEngine);
        setMultiPV(model.getMultiPv());
        model.setBlockGUI(false);
        model.setMode(Model_JFXChess.MODE_ANALYSIS);
        String fen = model.getGame().getUciPositionString();
        sendNewPosition(fen);
        uciGoInfinite();
    }

    public ActionListener startAnalysisMode() {
        return e -> {
            activateAnalysisMode();
        };
    }

    public void activateEnterMovesMode() {
        stopEngine();
        model.activeEngine = model.selectedAnalysisEngine;
        if (model.activeEngine.supportsUciLimitStrength()) {
            model.activeEngine.setUciLimitStrength(false);
        }
        model.setBlockGUI(false);
        model.setMode(Model_JFXChess.MODE_ENTER_MOVES);
    }

    public ActionListener startEnterMovesMode() {
        return e -> {
            activateEnterMovesMode();
        };
    }

    private void handleNewBoardPositionModeAnalysis() {
        String fen = model.getGame().getUciPositionString();
        sendNewPosition(fen);
        uciGoInfinite();
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

        String fen = model.getGame().getUciPositionString();
        sendNewPosition(fen);
        uciGoMoveTime(model.getComputerThinkTimeSecs()*1000);
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
                    // clean up current game, but otherwise not much to do
                    model.getPgnDatabase().setIdxOfCurrentlyOpenedGame(-1);
                    model.setComputerThinkTimeSecs(3);
                    Game g = new Game();
                    Board b = new Board(true);
                    g.getRootNode().setBoard(b);
                    model.setGame(g);
                    model.goToNode(g.getRootNode().getId());
                    activateEnterMovesMode();
                }
                if(result == DialogNewGame.PLAY_BOT) {
                    DialogPlayBot dlgPlayBot = new DialogPlayBot(model.mainFrameRef, model.botEngines);
                    dlgPlayBot.setVisible(true);
                    if(dlgPlayBot.isConfirmed()) {
                        model.wasSaved = false;
                        model.getPgnDatabase().setIdxOfCurrentlyOpenedGame(-1);
                        model.setComputerThinkTimeSecs(3);
                        Game g = new Game();
                        Board b;
                        if(dlgPlayBot.getPlayInitialPosition()) {
                            b = new Board(true);
                        } else {
                            b = model.getGame().getCurrentNode().getBoard().makeCopy();
                        }
                        g.getRootNode().setBoard(b);
                        model.setGame(g);
                        model.getGame().setTreeWasChanged(true);
                        model.getGame().setHeaderWasChanged(true);
                        model.selectedPlayEngine = model.botEngines.get(dlgPlayBot.getBotIndex());
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
                        String formattedDate = LocalDate.now().format(formatter);
                        g.setHeader("Date", formattedDate);
                        g.setHeader("Event", "Training Game JFXChess");
                        if(dlgPlayBot.getPlayerColor() == CONSTANTS.WHITE) {
                            g.setHeader("White", "N.N.");
                            g.setHeader("Black", model.selectedPlayEngine.getName());
                            g.setHeader("BlackElo", ((BotEngine) model.selectedPlayEngine).getElo());
                            model.setFlipBoard(false);
                            activatePlayWhiteMode();
                        } else {
                            g.setHeader("Black", "N.N.");
                            g.setHeader("White", model.selectedPlayEngine.getName());
                            g.setHeader("WhiteElo", ((BotEngine) model.selectedPlayEngine).getElo());
                            model.setFlipBoard(true);
                            activatePlayBlackMode();
                        }
                    }
                }
                if(result == DialogNewGame.PLAY_UCI) {
                    DialogPlayEngine dlgUci = new DialogPlayEngine(model.mainFrameRef, model.engines);
                    dlgUci.setVisible(true);
                    boolean uciAccepted = dlgUci.isConfirmed();
                    if(uciAccepted) {
                        model.wasSaved = false;
                        model.getPgnDatabase().setIdxOfCurrentlyOpenedGame(-1);
                        model.setComputerThinkTimeSecs(3);
                        Game g = new Game();
                        Board b;
                        if(dlgUci.getPlayInitialPosition()) {
                            b = new Board(true);
                        } else {
                            b = model.getGame().getCurrentNode().getBoard().makeCopy();
                        }
                        g.getRootNode().setBoard(b);
                        model.setGame(g);
                        model.getGame().setTreeWasChanged(true);
                        model.getGame().setHeaderWasChanged(true);
                        model.selectedPlayEngine = model.engines.get(dlgUci.getSelectedEngineIdx());
                        if(model.selectedPlayEngine.supportsUciLimitStrength()) {
                            int newElo = dlgUci.getElo();
                            if (newElo >= model.selectedPlayEngine.getMinUciElo()
                                    && newElo <= model.selectedPlayEngine.getMaxUciElo()) {
                                model.selectedPlayEngine.setUciElo(newElo);
                            }
                        }
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
                        String formattedDate = LocalDate.now().format(formatter);
                        g.setHeader("Date", formattedDate);
                        g.setHeader("Event", "Training Game JFXChess");
                        if(dlgUci.getPlayerColor() == CONSTANTS.WHITE) {
                            g.setHeader("White", "N.N.");
                            g.setHeader("Black", model.selectedPlayEngine.getName());
                            g.setHeader("BlackElo", String.valueOf(model.selectedPlayEngine.getUciElo()));
                            model.setFlipBoard(false);
                            activatePlayWhiteMode();
                        } else {
                            g.setHeader("Black", "N.N.");
                            g.setHeader("White", model.selectedPlayEngine.getName());
                            g.setHeader("WhiteElo", String.valueOf(model.selectedPlayEngine.getUciElo()));
                            model.setFlipBoard(true);
                            activatePlayBlackMode();
                        }
                    }
                }
            }
        };
    }

    public void activatePlayWhiteMode() {
        // restart engine
        model.activeEngine = model.selectedPlayEngine;
        if (!(model.activeEngine instanceof BotEngine)) {
            if (model.activeEngine.supportsUciLimitStrength()) {
                model.activeEngine.setUciLimitStrength(true);
            }
        }
        // restart
        restartEngine(model.activeEngine);

        // change game mode, trigger statechange
        model.setFlipBoard(false);
        model.setHumanPlayerColor(CONSTANTS.WHITE);
        model.setMode(Model_JFXChess.MODE_PLAY_WHITE);
    }

    public void activatePlayBlackMode() {
        // restart engine
        model.activeEngine = model.selectedPlayEngine;
        if (!(model.activeEngine instanceof BotEngine)) {
            if (model.activeEngine.supportsUciLimitStrength()) {
                model.activeEngine.setUciLimitStrength(true);
            }
        }
        // restart
        restartEngine(model.activeEngine);
        model.setFlipBoard(true);
        model.setHumanPlayerColor(CONSTANTS.BLACK);
        model.setMode(Model_JFXChess.MODE_PLAY_BLACK);
        model.setBlockGUI(true);
        handleNewBoardPositionModePlay();
    }

    public void activatePlayoutPositionMode() {
        // first change gamestate and reset engine
        model.activeEngine = model.selectedAnalysisEngine;
        if (model.activeEngine.supportsUciLimitStrength()) {
            model.activeEngine.setUciLimitStrength(false);
        }
        restartEngine(model.activeEngine);
        model.setFlipBoard(false);
        model.setMode(Model_JFXChess.MODE_PLAYOUT_POSITION);
        handleNewBoardPositionModePlayout();
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

        // first check if we can apply a bookmove
        long zobrist = model.getGame().getCurrentNode().getBoard().getZobrist();
        boolean maxDepthReached = false;
        int currentDepth = model.getGame().getCurrentNode().getDepth();
        int currentElo = model.activeEngine.getUciElo();
        boolean limitElo = model.activeEngine.getUciLimitStrength();
        // engine supports setting ELO
        // let's limit book knowledge according to ELO
        if(limitElo && (currentElo > 0)) {
            if(currentElo <= 1200 && currentDepth > 10) {
                maxDepthReached = true;
            }
            if(currentElo > 1200 && currentElo < 1400 && currentDepth > 10) {
                maxDepthReached = true;
            }
            if(currentElo > 1400 && currentElo < 1600 && currentDepth > 12) {
                maxDepthReached = true;
            }
            if(currentElo > 1600 && currentElo < 1800 && currentDepth > 14) {
                maxDepthReached = true;
            }
            if(currentElo > 1800 && currentElo < 2000 && currentDepth > 16) {
                maxDepthReached = true;
            }
            if(currentElo > 2000 && currentElo < 2200 && currentDepth > 18) {
                maxDepthReached = true;
            }
        }

        String bookMove = model.extBook.getRandomMove(zobrist);
        if((bookMove != null) && (!maxDepthReached)) {
            // don't execute the book move immediately; we want to
            // create the illusion of the computer thinking about his move
            CompletableFuture.delayedExecutor(
                    ((long) model.getComputerThinkTimeSecs() * 1000),
                    TimeUnit.MILLISECONDS
            ).execute(() -> handleBestMove("BESTMOVE|" + bookMove + "|" + zobrist));

        } else {
            String fen = model.getGame().getUciPositionString();
            sendNewPosition(fen);
            uciGoMoveTime(model.getComputerThinkTimeSecs()*1000);
        }
        if(model.getMode() == Model_JFXChess.MODE_PLAY_WHITE && model.getGame().getCurrentNode().getBoard().turn == CONSTANTS.BLACK) {
            model.setBlockGUI(true);
        }
        if(model.getMode() == Model_JFXChess.MODE_PLAY_BLACK && model.getGame().getCurrentNode().getBoard().turn == CONSTANTS.WHITE) {
            model.setBlockGUI(true);
        }
    }

    private void addBestPv(String[] uciMoves) {
        GameNode currentNode = model.getGame().getCurrentNode();

        for (String uciMove : uciMoves) {
            try {
                GameNode next = new GameNode();
                Board board = currentNode.getBoard().makeCopy();
                Move m = new Move(uciMove);
                if(!board.isLegal(m)) {
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

        int mode = model.getMode();
        if(mode == Model_JFXChess.MODE_ENTER_MOVES) {
            return;
        }
        String[] bestmoveItems = bestmove.split("\\|");

        String zobristString = bestmoveItems[bestmoveItems.length-1];
        long zobrist = Long.parseLong(zobristString);

        if(zobrist != model.getGame().getCurrentNode().getBoard().getZobrist()) {
            // if this bestmove is for a different position than the current,
            // it is a relict from thread/gui synchronisation mismatch; we just dismiss it
            return;
        } else {
            // If not, this is a bestmove from either playing the engine
            // Then we need to unlock the GUI as
            // the user wants to potentially react to that
            model.setBlockGUI(false);
        }

        if (mode == Model_JFXChess.MODE_PLAY_WHITE ||
                mode == Model_JFXChess.MODE_PLAY_BLACK  ||
                mode == Model_JFXChess.MODE_PLAYOUT_POSITION) {

            // todo: catch Exceptions!
            String uci = bestmoveItems[1].split(" ")[0];
            Move m = new Move(uci);
            Board b = model.getGame().getCurrentNode().getBoard();
            if (b.isLegal(m)) {
                if(mode == Model_JFXChess.MODE_PLAY_WHITE && b.turn == CONSTANTS.BLACK) {
                    model.applyMove(m);
                }
                if(mode == Model_JFXChess.MODE_PLAY_BLACK && b.turn == CONSTANTS.WHITE) {
                    model.applyMove(m);
                }
                if(mode == Model_JFXChess.MODE_PLAYOUT_POSITION) {
                    model.applyMove(m);
                }
            }
        }

        if(mode == Model_JFXChess.MODE_GAME_ANALYSIS) {

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
            if(model.getGame().getCurrentNode().getBoard().isCheckmate()){
                model.currentIsMate = true;
            }

            // ignore leafs (game ended here)
            if(!model.getGame().getCurrentNode().isLeaf()) {

                // completely skip analysis for black or white, if
                // that option was chosen
                boolean turn = model.getGame().getCurrentNode().getBoard().turn;
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
                            String nextMove = model.getGame().getCurrentNode().getVariation(0).getMove().getUci();
                            String[] pvMoves = model.currentBestPv.split(" ");
                            // if the bestmove returned by the engine is different
                            // from the best suggested pv line, it means that e.g. the
                            // engine took a book move, but did not give a pv evaluation
                            // or there was some major async error between engine and gui
                            // in such a case, do not add the best pv line, as it is probably
                            // not a valid pv line for the current node
                            // we also do not want to add the same line as the child
                            if (!uci.equals(nextMove) && pvMoves.length > 0 && pvMoves[0].equals(uci)) {

                                addBestPv(pvMoves);

                                NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
                                DecimalFormat decim = (DecimalFormat) nf;
                                decim.applyPattern("0.00");
                                String sCurrentBest = decim.format(model.currentBestEval / 100.0);
                                String sChildBest = decim.format(model.childBestEval / 100.0);

                                ArrayList<GameNode> vars = model.getGame().getCurrentNode().getVariations();
                                if (vars != null && vars.size() > 1) {
                                    GameNode child0 = model.getGame().getCurrentNode().getVariation(0);
                                    child0.setComment(sChildBest);
                                    GameNode child1 = model.getGame().getCurrentNode().getVariation(1);
                                    child1.setComment(sCurrentBest);
                                }
                            }
                        }
                    }

                    if (model.currentIsMate && !model.childIsMate) {
                        // the current player missed a mate
                        String[] pvMoves = model.currentBestPv.split(" ");
                        addBestPv(pvMoves);

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

                        ArrayList<GameNode> vars = model.getGame().getCurrentNode().getVariations();
                        if (vars != null && vars.size() > 1) {
                            GameNode child0 = model.getGame().getCurrentNode().getVariation(0);
                            child0.setComment(sChildBest);
                            GameNode child1 = model.getGame().getCurrentNode().getVariation(1);
                            child1.setComment(sCurrentBest);
                        }
                    }

                    if (!model.currentIsMate && model.childIsMate) {
                        // the current player  moved into a mate
                        String[] pvMoves = model.currentBestPv.split(" ");
                        addBestPv(pvMoves);

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

                        ArrayList<GameNode> vars = model.getGame().getCurrentNode().getVariations();
                        if (vars != null && vars.size() > 1) {
                            GameNode child0 = model.getGame().getCurrentNode().getVariation(0);
                            child0.setComment(sChildBest);
                            GameNode child1 = model.getGame().getCurrentNode().getVariation(1);
                            child1.setComment(sCurrentBest);
                        }
                    }

                    if (model.currentIsMate && model.childIsMate) {
                        // the current player had a mate, but instead of executing it, he moved into a mate,
                        // but we also want to skip the situation where the board position is checkmate
                        if ((model.currentMateInMoves >= 0 && model.childMateInMoves >= 0) &&
                                model.childMateInMoves != 0) {

                            String[] pvMoves = model.currentBestPv.split(" ");
                            addBestPv(pvMoves);

                            String sCurrentBest;
                            String sChildBest;
                            if (turn == CONSTANTS.WHITE) {
                                sCurrentBest = "#" + (Math.abs(model.currentMateInMoves));
                                sChildBest = "#-" + (Math.abs(model.childMateInMoves));
                            } else {
                                sCurrentBest = "#-" + (Math.abs(model.currentMateInMoves));
                                sChildBest = "#" + (Math.abs(model.childMateInMoves));
                            }

                            ArrayList<GameNode> vars = model.getGame().getCurrentNode().getVariations();
                            if (vars != null && vars.size() > 1) {
                                GameNode child0 = model.getGame().getCurrentNode().getVariation(0);
                                child0.setComment(sChildBest);
                                GameNode child1 = model.getGame().getCurrentNode().getVariation(1);
                                child1.setComment(sCurrentBest);
                            }
                        }
                    }
                }
            }
            model.markTreeChange();
            model.goToParent();
        }

    }

    public void handleNewEngineInfo(String s) {
        // we show info only during analysis, not when playing
        // against the bots/engine
        if (s.startsWith("INFO")) {
            if ((model.getMode() != Model_JFXChess.MODE_PLAY_BLACK) &&
                    (model.getMode() != Model_JFXChess.MODE_PLAY_WHITE)) {
                model.setCurrentEngineInfo(s);
            }
        }

        // if we get info from the engine but currently play against it,
        // just show some dummy info
        if ((model.getMode() == Model_JFXChess.MODE_PLAY_WHITE) || (model.getMode() == Model_JFXChess.MODE_PLAY_BLACK)) {
            if (model.getMode() == Model_JFXChess.MODE_PLAY_WHITE) {
                if (model.getGame().getCurrentNode().getBoard().turn == CONSTANTS.WHITE) {
                    String msg = playInfo.replace("MESSAGE", "Your turn - White to move");
                    model.setCurrentEngineInfo(msg);
                } else {
                    String msg = playInfo.replace("MESSAGE", "thinking...");
                    model.setCurrentEngineInfo(msg);
                }
            }
            if (model.getMode() == Model_JFXChess.MODE_PLAY_BLACK) {
                if (model.getGame().getCurrentNode().getBoard().turn == CONSTANTS.BLACK) {
                    String msg = playInfo.replace("MESSAGE", "Your turn - Black to move");
                    model.setCurrentEngineInfo(msg);
                } else {
                    String msg = playInfo.replace("MESSAGE", "thinking...");
                    model.setCurrentEngineInfo(msg);
                }
            }
        }
        if (s.startsWith("BESTMOVE")) {
            handleBestMove(s);
        }
    }

    public void handleNewBoardPosition() {
        int mode = model.getMode();
        Board board = model.getGame().getCurrentNode().getBoard();
        boolean turn = board.turn;

        boolean isCheckmate = board.isCheckmate();
        boolean isStalemate = board.isStalemate();
        boolean isThreefoldRepetition = model.getGame().isThreefoldRepetition();
        boolean isInsufficientMaterial = model.getGame().isInsufficientMaterial();

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
                JOptionPane.showMessageDialog(model.mainFrameRef, message);
            }
        }
        if(abort) {
            activateEnterMovesMode();
            if (isCheckmate) {
                // white to move, but cannot: black checkmated
                if(board.turn == CONSTANTS.WHITE) {
                    model.game.setResult(CONSTANTS.RES_BLACK_WINS);
                } else {
                    model.game.setResult(CONSTANTS.RES_WHITE_WINS);
                }
            }
            if (isStalemate) {
                model.game.setResult(CONSTANTS.RES_DRAW);
            }
            if (isThreefoldRepetition) {
                model.game.setResult(CONSTANTS.RES_DRAW);
            }
            if (isInsufficientMaterial) {
                model.game.setResult(CONSTANTS.RES_DRAW);
            }
        } else {
            if (mode == Model_JFXChess.MODE_ANALYSIS) {
                handleNewBoardPositionModeAnalysis();
            }
            if (mode == Model_JFXChess.MODE_GAME_ANALYSIS) {
                handleNewBoardPositionModeGameAnalysis();
            }
            if (mode == Model_JFXChess.MODE_PLAYOUT_POSITION) {
                handleNewBoardPositionModePlayout();
            }
            if ((mode == Model_JFXChess.MODE_PLAY_WHITE || mode == Model_JFXChess.MODE_PLAY_BLACK)
                    && turn != model.getHumanPlayerColor()) {
                handleNewBoardPositionModePlay();
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
            if(dlgEngines.isConfirmed()) {
                model.engines = dlgEngines.getEngines();
                model.activeEngine = dlgEngines.getSelectedEngine();
                model.selectedAnalysisEngine = model.activeEngine;
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
        if (evt.getPropertyName().equals("currentGameNodeChanged")) {
            handleNewBoardPosition();
        }
        if (evt.getPropertyName().equals("treeChanged")) {
            // do nothing - we should only react, if the current game
            // node changed. This is always done, if the tree changes
        }

    }

    public ActionListener changeNrThreads() {
        return e -> {
            if(!model.isBlockGUI()) {
                if(model.activeEngine.supportsMultiThread()) {
                    activateEnterMovesMode();
                    int currentNrThreads = model.activeEngine.getNrThreads();
                    int maxCpusAvail = model.maxCpus - 1;
                    if(maxCpusAvail <= 0) {
                        maxCpusAvail = 1;
                    }
                    int maxThreads = Math.min(maxCpusAvail,  model.activeEngine.getMaxThreads());
                    model.setShortcutsEnabled(false);
                    DialogThreads dlgThreads = new DialogThreads(model.mainFrameRef, 1, maxThreads, currentNrThreads);
                    dlgThreads.setVisible(true);
                    model.setShortcutsEnabled(true);
                    if(dlgThreads.isConfirmed()) {
                        int newNrThreads = dlgThreads.getNrThreads();
                        if(newNrThreads != currentNrThreads) {
                            model.setNrThreads(newNrThreads);
                        }
                    }
                }
            }
        };
    }

}
