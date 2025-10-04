/* JFXChess - A Chess Graphical User Interface
 * Copyright (C) 2020-2025 Dominik Klein
 * Copyright (C) 2025 Torsten Torell
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

import javafx.animation.PauseTransition;
import javafx.scene.control.Alert;
import javafx.util.Duration;
import org.asdfjkl.jfxchess.lib.*;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ModeMenuController implements StateChangeListener {

    final GameModel gameModel;
    final EngineOutputView engineOutputView;
    private final EngineController engineController;

    public ModeMenuController(GameModel gameModel, EngineOutputView engineOutputView) {

        this.gameModel = gameModel;
        engineController = new EngineController(this);
        this.engineOutputView = engineOutputView;
    }

    private void notifyUserDuringPlay() {
        if(gameModel.getMode() == GameModel.MODE_PLAY_WHITE) {
            if(gameModel.getGame().getCurrentNode().getBoard().turn == CONSTANTS.WHITE) {
                engineOutputView.setText("|||||||Your turn - White to move");
            } else {
                engineOutputView.setText("|||||||...thinking...");
            }
        }
        if(gameModel.getMode() == GameModel.MODE_PLAY_BLACK) {
            if(gameModel.getGame().getCurrentNode().getBoard().turn == CONSTANTS.BLACK) {
                engineOutputView.setText("|||||||Your turn - Black to move");
            } else {
                engineOutputView.setText("|||||||...thinking...");
            }
        }

    }

    public void handleEngineInfo(String s) {

        // we show info only during analysis, not when playing
        // against the bots/engine
        if(s.startsWith("INFO")) {
            if((gameModel.getMode() != GameModel.MODE_PLAY_BLACK) &&
                (gameModel.getMode() != GameModel.MODE_PLAY_WHITE)) {
                //"INFO |Stockfish 12 (Level MAX)|zobrist|145.081 kn/s||(#0)  23. Be7#||||"
                String[] sSplit = s.split("\\|");
                if(gameModel.getGame().getCurrentNode().getBoard().isCheckmate() && sSplit.length > 1) {
                    String sTemp = "|" + sSplit[1] + "||||(#0)|";
                    this.engineOutputView.setText(sTemp);
                } else {
                    this.engineOutputView.setText(s.substring(5));
                }
            }
        }
        // if we get info from the engine but currently play against it,
        // just show some dummy info
        if((gameModel.getMode() == GameModel.MODE_PLAY_WHITE) ||
                (gameModel.getMode() == GameModel.MODE_PLAY_BLACK)) {
            notifyUserDuringPlay();
        }
        if(s.startsWith("BESTMOVE")) {
            handleBestMove(s);
        }
    }

    public void activateAnalysisMode() {
        engineController.stopEngine();
        gameModel.activeEngine = gameModel.selectedAnalysisEngine;
        if(gameModel.activeEngine.supportsUciLimitStrength()) {
            gameModel.activeEngine.setUciLimitStrength(false);
        }
        setEngineNameAndInfoToOuptput();
        System.out.println("current engine path: "+gameModel.activeEngine.getPath());
        engineController.restartEngine(gameModel.activeEngine);
        engineController.setMultiPV(gameModel.getMultiPv());
        gameModel.setMode(GameModel.MODE_ANALYSIS);
        gameModel.blockGUI = false;
        gameModel.triggerStateChange();
    }

    public void activateEnterMovesMode() {
        engineController.stopEngine();
        gameModel.activeEngine = gameModel.selectedAnalysisEngine;
        if(gameModel.activeEngine.supportsUciLimitStrength()) {
            gameModel.activeEngine.setUciLimitStrength(false);
        }
        setEngineNameAndInfoToOuptput();

        gameModel.setMode(GameModel.MODE_ENTER_MOVES);
        gameModel.blockGUI = false;
        gameModel.triggerStateChange();
    }

    private void handleStateChangeAnalysis() {
        String fen = gameModel.getGame().getCurrentNode().getBoard().fen();
        engineController.sendNewPosition(fen);
        engineController.uciGoInfinite();
    }

    private void handleStateChangeGameAnalysis() {

        boolean continueAnalysis = true;

        boolean parentIsRoot = (gameModel.getGame().getCurrentNode().getParent() == gameModel.getGame().getRootNode());
        if(!parentIsRoot) {
            // if the current position is in the opening book,
            // we stop the analysis
            long zobrist = gameModel.getGame().getCurrentNode().getBoard().getZobrist();
            if(gameModel.extBook.inBook(zobrist)) {
                gameModel.getGame().getCurrentNode().setComment("last book move");
                continueAnalysis = false;
            } else {
                // otherwise continue the analysis
                if (gameModel.getGameAnalysisJustStarted()) {
                    gameModel.setGameAnalysisJustStarted(false);
                } else {
                    gameModel.getGame().goToParent();
                }
                String fen = gameModel.getGame().getCurrentNode().getBoard().fen();
                engineController.sendNewPosition(fen);
                engineController.uciGoMoveTime(gameModel.getGameAnalysisThinkTimeSecs() * 1000);
            }
        } else {
            continueAnalysis = false;
        }

        if(!continueAnalysis) {
            // we are at the root or found a book move
            gameModel.getGame().setTreeWasChanged(true);
            activateEnterMovesMode();

            DialogSimpleAlert dlgAlert = new DialogSimpleAlert(
                    gameModel.getStageRef(), Alert.AlertType.INFORMATION,
                    "Analysis Finished", "Game Analysis Finished");
            dlgAlert.showAndWait();
        }
    }

    public void activatePlayWhiteMode() {
        // restart engine
        gameModel.activeEngine = gameModel.selectedPlayEngine;
        if(!(gameModel.activeEngine instanceof BotEngine)) {
            if(gameModel.activeEngine.supportsUciLimitStrength()) {
                    gameModel.activeEngine.setUciLimitStrength(true);
            }
        }
        // restart
        engineController.restartEngine(gameModel.activeEngine);

        // change game mode, trigger statechange
        setEngineNameAndInfoToOuptput();
	    gameModel.setMode(GameModel.MODE_PLAY_WHITE);
        gameModel.setFlipBoard(false);
        gameModel.setHumanPlayerColor(CONSTANTS.WHITE);
        gameModel.triggerStateChange();
    }

    public void activatePlayBlackMode() {
        // restart engine
        gameModel.activeEngine = gameModel.selectedPlayEngine;
        if(!(gameModel.activeEngine instanceof BotEngine)) {
            if(gameModel.activeEngine.supportsUciLimitStrength()) {
                gameModel.activeEngine.setUciLimitStrength(true);
            }
        }
        // restart
        engineController.restartEngine(gameModel.activeEngine);
        // trigger statechange
        setEngineNameAndInfoToOuptput();
        gameModel.setMode(GameModel.MODE_PLAY_BLACK);
        gameModel.setFlipBoard(true);
        gameModel.setHumanPlayerColor(CONSTANTS.BLACK);
        gameModel.triggerStateChange();
    }

    public void activatePlayoutPositionMode() {
        // first change gamestate and reset engine
        gameModel.activeEngine = gameModel.selectedAnalysisEngine;
        if(gameModel.activeEngine.supportsUciLimitStrength()) {
            gameModel.activeEngine.setUciLimitStrength(false);
        }
        engineController.restartEngine(gameModel.activeEngine);
        setEngineNameAndInfoToOuptput();
        gameModel.setMode(GameModel.MODE_PLAYOUT_POSITION);
        gameModel.setFlipBoard(false);
        gameModel.triggerStateChange();
    }

    public void activateGameAnalysisMode() {

        gameModel.getGame().removeAllComments();
        gameModel.getGame().removeAllVariants();
        gameModel.getGame().removeAllAnnotations();
        gameModel.getGame().setTreeWasChanged(true);

        gameModel.activeEngine = gameModel.selectedAnalysisEngine;
        if(gameModel.activeEngine.supportsUciLimitStrength()) {
            gameModel.activeEngine.setUciLimitStrength(false);
        }
        if(gameModel.activeEngine.supportsMultiPV()) {
            gameModel.activeEngine.setMultiPV(1);
        }
        setEngineNameAndInfoToOuptput();
        engineController.restartEngine(gameModel.activeEngine);

        gameModel.setFlipBoard(false);
        gameModel.getGame().goToRoot();
        gameModel.getGame().goToLeaf();
        if(gameModel.getGame().getCurrentNode().getBoard().isCheckmate()) {
            gameModel.currentIsMate = true;
            gameModel.currentMateInMoves = 0;
        }

        gameModel.setGameAnalysisJustStarted(true);
        gameModel.triggerStateChange();

    }

    public void handleStateChangePlayWhiteOrBlack() {

        // first check if we can apply a bookmove
        long zobrist = gameModel.getGame().getCurrentNode().getBoard().getZobrist();
        boolean maxDepthReached = false;
        int currentDepth = gameModel.getGame().getCurrentNode().getDepth();
        int currentElo = gameModel.activeEngine.getUciElo();
        boolean limitElo = gameModel.activeEngine.getUciLimitStrength();
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
        String bookMove = gameModel.extBook.getRandomMove(zobrist);
        if((bookMove != null) && (!maxDepthReached)) {
            // don't execute the book move immediately; we want to
            // create the illusion of the computer thinking about his move
            // inside your event handler or wherever:
            PauseTransition delay = new PauseTransition(Duration.seconds(gameModel.getComputerThinkTimeSecs()));
            delay.setOnFinished(event -> {
                handleBestMove("BESTMOVE|"+bookMove+"|"+zobrist);
            });
            delay.play();
        } else {
            String fen = gameModel.getGame().getCurrentNode().getBoard().fen();
            engineController.sendNewPosition(fen);
            engineController.uciGoMoveTime(gameModel.getComputerThinkTimeSecs()*1000);
        }
    }

    public void handleStateChangePlayoutPosition() {

        String fen = gameModel.getGame().getCurrentNode().getBoard().fen();
        engineController.sendNewPosition(fen);
        engineController.uciGoMoveTime(gameModel.getComputerThinkTimeSecs()*1000);
    }

    private void addBestPv(String[] uciMoves) {
        GameNode currentNode = gameModel.getGame().getCurrentNode();

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

    // This is a method to directly after editing engines or at startup of
    // the program, show in the outputview the engineID, the Elo strength
    // (if UCILimitStrength option is true) and the empty Pv-lines of the active engine.
    // When the engine starts, the elo will be shown in the normal way
    // via the EngineThread and EngineInfo when the corresponding commands
    // are being sent to the engine.
    public void setEngineNameAndInfoToOuptput() {

        // | id (Level MAX) | zobrist  |  nps | hashfull | tbhits | current Move + depth | eval+line pv1 | .. pv2 | ...pv3 | ...pv4 | ... | ...pv64 |
        String newInfo = "|||||||||||";
        engineOutputView.setId(gameModel.activeEngine.getNameWithElo());
        engineOutputView.setText("|||||||||||");

    }

    public void editEngines() {

        // The following call stops the engine-process, set the ENTER_MOVES_MODE
        // and calls GameModel.triggerChangeState(). Previously the engine was
        // not stopped here.
        activateEnterMovesMode();
        DialogEngines dlg = new DialogEngines();
        ArrayList<Engine> enginesCopy = new ArrayList<>();
        for(Engine engine : gameModel.engines) {
            enginesCopy.add(engine);
        }
        int selectedIdx = gameModel.engines.indexOf(gameModel.activeEngine);
        if(selectedIdx < 0) {
            selectedIdx = 0;
        }
        boolean accepted = dlg.show(gameModel.getStageRef(), enginesCopy, selectedIdx);
        if(accepted) {
            ArrayList<Engine> engineList = new ArrayList<>(dlg.engineList);
            Engine selectedEngine = dlg.engineList.get(dlg.selectedIndex);
            gameModel.engines = engineList;
            gameModel.activeEngine = selectedEngine;
            // Change the engine-info in the bottom panel immediately on OK
            // being pressed. Previously it didn't change until we started
            // playing.
            setEngineNameAndInfoToOuptput();
            // // reset pv line to 1 for new engine
            // gameModel.setMultiPv(1);

            gameModel.setMultiPvChange(true); // Not important anymore.            
            gameModel.triggerStateChange(); // Important.
        }
    }

    public void handleBestMove(String bestmove) {

        int mode = gameModel.getMode();

        if(mode == GameModel.MODE_ENTER_MOVES) {
            return;
        }

        String[] bestmoveItems = bestmove.split("\\|");

        String zobristString = bestmoveItems[bestmoveItems.length-1];
        long zobrist = Long.parseLong(zobristString);

        if(zobrist != gameModel.getGame().getCurrentNode().getBoard().getZobrist()) {
            // if this bestmove is for a different position than the current,
            // it is a relict from thread/gui synchronisation mismatch; we just dismiss it
            return;
        } else {
            // If not, this is a bestmove from either playing the engine
            // Then we need to unlock the GUI as
            // the user wants to potentially react to that
            gameModel.blockGUI = false;
        }

        if (mode == GameModel.MODE_PLAY_WHITE || mode == GameModel.MODE_PLAY_BLACK  || mode == GameModel.MODE_PLAYOUT_POSITION) {

            // todo: catch Exceptions!
            String uci = bestmoveItems[1].split(" ")[0];
            Move m = new Move(uci);
            Board b = gameModel.getGame().getCurrentNode().getBoard();
            if (b.isLegal(m)) {
                if(mode == GameModel.MODE_PLAY_WHITE && b.turn == CONSTANTS.BLACK) {
                    gameModel.getGame().applyMove(m);
                    notifyUserDuringPlay();
                    gameModel.triggerStateChange();
                }
                if(mode == GameModel.MODE_PLAY_BLACK && b.turn == CONSTANTS.WHITE) {
                    gameModel.getGame().applyMove(m);
                    notifyUserDuringPlay();
                    gameModel.triggerStateChange();
                }
                if(mode == GameModel.MODE_PLAYOUT_POSITION) {
                    gameModel.getGame().applyMove(m);
                    gameModel.triggerStateChange();
                }
            }
        }

        if(mode == GameModel.MODE_GAME_ANALYSIS) {

            // first update information for current node
            gameModel.childBestPv = gameModel.currentBestPv;
            gameModel.childBestEval = gameModel.currentBestEval;
            gameModel.childIsMate = gameModel.currentIsMate;
            gameModel.childMateInMoves = gameModel.currentMateInMoves;

            gameModel.currentBestPv = bestmoveItems[3];
            gameModel.currentBestEval = Integer.parseInt(bestmoveItems[2]);
            gameModel.currentIsMate = bestmoveItems[4].equals("true");
            gameModel.currentMateInMoves = Integer.parseInt(bestmoveItems[5]);

            // double check, since some engines misreport / report not quickly enough
            if(gameModel.getGame().getCurrentNode().getBoard().isCheckmate()){
                gameModel.currentIsMate = true;
            }

            // ignore leafs (game ended here)
            if(!gameModel.getGame().getCurrentNode().isLeaf()) {

                // completely skip analysis for black or white, if
                // that option was chosen
                boolean turn = gameModel.getGame().getCurrentNode().getBoard().turn;
                if ((gameModel.getGameAnalysisForPlayer() == GameModel.BOTH_PLAYERS)
                        || (gameModel.getGameAnalysisForPlayer() == CONSTANTS.IWHITE && turn == CONSTANTS.WHITE)
                        || (gameModel.getGameAnalysisForPlayer() == CONSTANTS.IBLACK && turn == CONSTANTS.BLACK)) {

                    int centiPawnThreshold = (int) (gameModel.getGameAnalysisThreshold() * 100.0);
                    // first case if there was simply a better move; i.e. no checkmate overseen or
                    // moved into a checkmate
                    if (!gameModel.currentIsMate && !gameModel.childIsMate) {
                        boolean wMistake = turn == CONSTANTS.WHITE && ((gameModel.currentBestEval - gameModel.childBestEval) >= centiPawnThreshold);
                        boolean bMistake = turn == CONSTANTS.BLACK && ((gameModel.currentBestEval - gameModel.childBestEval) <= -(centiPawnThreshold));

                        if (wMistake || bMistake) {
                            String uci = bestmoveItems[1].split(" ")[0];
                            String nextMove = gameModel.getGame().getCurrentNode().getVariation(0).getMove().getUci();
                            String[] pvMoves = gameModel.currentBestPv.split(" ");
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
                                String sCurrentBest = decim.format(gameModel.currentBestEval / 100.0);
                                String sChildBest = decim.format(gameModel.childBestEval / 100.0);

                                ArrayList<GameNode> vars = gameModel.getGame().getCurrentNode().getVariations();
                                if (vars != null && vars.size() > 1) {
                                    GameNode child0 = gameModel.getGame().getCurrentNode().getVariation(0);
                                    child0.setComment(sChildBest);
                                    GameNode child1 = gameModel.getGame().getCurrentNode().getVariation(1);
                                    child1.setComment(sCurrentBest);
                                }
                            }
                        }
                    }

                    if (gameModel.currentIsMate && !gameModel.childIsMate) {
                        // the current player missed a mate
                        String[] pvMoves = gameModel.currentBestPv.split(" ");
                        addBestPv(pvMoves);

                        NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
                        DecimalFormat decim = (DecimalFormat) nf;
                        decim.applyPattern("0.00");
                        String sChildBest = decim.format(gameModel.childBestEval / 100.0);

                        String sCurrentBest = "";
                        if (turn == CONSTANTS.WHITE) {
                            sCurrentBest = "#" + (Math.abs(gameModel.currentMateInMoves));
                        } else {
                            sCurrentBest = "#-" + (Math.abs(gameModel.currentMateInMoves));
                        }

                        ArrayList<GameNode> vars = gameModel.getGame().getCurrentNode().getVariations();
                        if (vars != null && vars.size() > 1) {
                            GameNode child0 = gameModel.getGame().getCurrentNode().getVariation(0);
                            child0.setComment(sChildBest);
                            GameNode child1 = gameModel.getGame().getCurrentNode().getVariation(1);
                            child1.setComment(sCurrentBest);
                        }
                    }

                    if (!gameModel.currentIsMate && gameModel.childIsMate) {
                        // the current player  moved into a mate
                        String[] pvMoves = gameModel.currentBestPv.split(" ");
                        addBestPv(pvMoves);

                        NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
                        DecimalFormat decim = (DecimalFormat) nf;
                        decim.applyPattern("0.00");
                        String sCurrentBest = decim.format(gameModel.currentBestEval / 100.0);

                        String sChildBest = "";
                        if (turn == CONSTANTS.WHITE) {
                            sChildBest = "#-" + (Math.abs(gameModel.childMateInMoves));
                        } else {
                            sChildBest = "#" + (Math.abs(gameModel.childMateInMoves));
                        }

                        ArrayList<GameNode> vars = gameModel.getGame().getCurrentNode().getVariations();
                        if (vars != null && vars.size() > 1) {
                            GameNode child0 = gameModel.getGame().getCurrentNode().getVariation(0);
                            child0.setComment(sChildBest);
                            GameNode child1 = gameModel.getGame().getCurrentNode().getVariation(1);
                            child1.setComment(sCurrentBest);
                        }
                    }

                    if (gameModel.currentIsMate && gameModel.childIsMate) {
                        // the current player had a mate, but instead of executing it, he moved into a mate,
                        // but we also want to skip the situation where the board position is checkmate
                        if ((gameModel.currentMateInMoves >= 0 && gameModel.childMateInMoves >= 0) &&
                                gameModel.childMateInMoves != 0) {

                            String[] pvMoves = gameModel.currentBestPv.split(" ");
                            addBestPv(pvMoves);

                            String sCurrentBest = "";
                            String sChildBest = "";
                            if (turn == CONSTANTS.WHITE) {
                                sCurrentBest = "#" + (Math.abs(gameModel.currentMateInMoves));
                                sChildBest = "#-" + (Math.abs(gameModel.childMateInMoves));
                            } else {
                                sCurrentBest = "#-" + (Math.abs(gameModel.currentMateInMoves));
                                sChildBest = "#" + (Math.abs(gameModel.childMateInMoves));
                            }

                            ArrayList<GameNode> vars = gameModel.getGame().getCurrentNode().getVariations();
                            if (vars != null && vars.size() > 1) {
                                GameNode child0 = gameModel.getGame().getCurrentNode().getVariation(0);
                                child0.setComment(sChildBest);
                                GameNode child1 = gameModel.getGame().getCurrentNode().getVariation(1);
                                child1.setComment(sCurrentBest);
                            }
                        }
                    }
                }
            }
            gameModel.getGame().setTreeWasChanged(true);
            gameModel.triggerStateChange();
        }

    }

    @Override
    public void stateChange() {
        int mode = gameModel.getMode();
        Board board = gameModel.getGame().getCurrentNode().getBoard();
        boolean turn = board.turn;

        boolean isCheckmate = board.isCheckmate();
        boolean isStalemate = board.isStalemate();
        boolean isThreefoldRepetition = gameModel.getGame().isThreefoldRepetition();
        boolean isInsufficientMaterial = gameModel.getGame().isInsufficientMaterial();

        boolean abort = false;

        // first we check if the game is finished due to checkmate, stalemate, three-fold repetition
        // or insufficient material
        // if that is the case
        //      we check if we play against the computer or are in analysis or playoutposition mode
        //         if so, we are going to abort the game and switch to entermovesmode
        //      then we check if we play against the computer
        //         if so, we are going to inform the user

        // if we change from e.g. play white to enter moves, the state change would trigger
        // the notification again in enter moves mode after the state change. thus,
        // also check if
        if ((isCheckmate || isStalemate || isThreefoldRepetition || isInsufficientMaterial)) {
            if (mode == GameModel.MODE_PLAY_WHITE || mode == GameModel.MODE_PLAY_BLACK || mode == GameModel.MODE_PLAYOUT_POSITION) {
                abort = true;
            }

            if (mode == GameModel.MODE_PLAY_WHITE || mode == GameModel.MODE_PLAY_BLACK) {

                String message = "";
                if (isCheckmate) {
                    message = "     Checkmate.     ";
                }
                if (isStalemate) {
                    message = "     Stalemate.     ";
                }
                if (isThreefoldRepetition) {
                    message = "Draw (Threefold Repetition)";
                }
                if (isInsufficientMaterial) {
                    message = "Draw (Insufficient material for checkmate)";
                }
                DialogSimpleAlert dlgAlert = new DialogSimpleAlert(
                        gameModel.getStageRef(), Alert.AlertType.INFORMATION,
                        "Game Finished", message);
                dlgAlert.showAndWait();
            }
        }

        if(abort) {
            activateEnterMovesMode();
        } else {
            if (mode == GameModel.MODE_ANALYSIS) {
                handleStateChangeAnalysis();
            }
            if (mode == GameModel.MODE_GAME_ANALYSIS) {
                handleStateChangeGameAnalysis();
            }
            if (mode == GameModel.MODE_PLAYOUT_POSITION) {
                handleStateChangePlayoutPosition();
            }
            if ((mode == GameModel.MODE_PLAY_WHITE || mode == GameModel.MODE_PLAY_BLACK)
                    && turn != gameModel.getHumanPlayerColor()) {
                handleStateChangePlayWhiteOrBlack();
            }
        }
    }

    // Just a few wrapper-methods, so I could remove Enginecontroller
    // completely from App.java.
    public void stopEngine() {
        engineController.stopEngine();
    }
    
    public void engineSetOptionMultiPV(int value) {
        engineController.setMultiPV(value);     
    }

    public void engineSetThreads(int value) {
        engineController.setNrThreads(value);
    }
}
