package org.asdfjkl.jerryfx.gui;

import javafx.scene.control.Alert;
import org.asdfjkl.jerryfx.lib.Board;
import org.asdfjkl.jerryfx.lib.CONSTANTS;
import org.asdfjkl.jerryfx.lib.GameNode;
import org.asdfjkl.jerryfx.lib.Move;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ModeMenuController implements StateChangeListener {

    GameModel gameModel;
    EngineOutputView engineOutputView;
    EngineController engineController;

    public ModeMenuController(GameModel gameModel, EngineOutputView engineOutputView) {

        this.gameModel = gameModel;
        this.engineOutputView = engineOutputView;
    }

    public void handleEngineInfo(String s) {

        //System.out.println("MMM: got "+s);
        if(s.startsWith("INFO")) {
            //System.out.println(s);
            //"INFO |Stockfish 12 (Level MAX)|145.081 kn/s||(#0)  23. Be7#||||"
            String[] sSplit = s.split("\\|");
            if(gameModel.getGame().getCurrentNode().getBoard().isCheckmate() && sSplit.length > 1) {
                String sTemp = "|" + sSplit[1] + "|||(#0)|";
                this.engineOutputView.setText(sTemp);
            } else {
                this.engineOutputView.setText(s.substring(5));
            }
        }
        if(s.startsWith("BESTMOVE")) {
            handleBestMove(s);
        }
    }

    public void setEngineController(EngineController engineController) {
        this.engineController = engineController;
    }

    public void activateAnalysisMode() {

        gameModel.lastSeenBestmove = "";
        // first change gamestate and reset engine
        engineController.sendCommand("stop");
        engineController.sendCommand("quit");
        String cmdEngine = gameModel.activeEngine.getPath();
        //System.out.println("activateAnalysis: " + cmdEngine);
        engineController.sendCommand("start "+cmdEngine);
        engineController.sendCommand("uci");
        engineController.sendCommand("ucinewgame");
        for(EngineOption enOpt : gameModel.activeEngine.options) {
            if(enOpt.isNotDefault()) {
                engineController.sendCommand(enOpt.toUciCommand());
            }
        }
        if(gameModel.activeEngine.supportsMultiPV()) {
            engineController.sendCommand("setoption name MultiPV value " + gameModel.getMultiPv());
        }
        gameModel.setMode(GameModel.MODE_ANALYSIS);
        gameModel.triggerStateChange();
    }

    public void activateEnterMovesMode() {
        gameModel.lastSeenBestmove = "";
        engineController.sendCommand("stop");
        engineController.sendCommand("quit");
        gameModel.setMode(GameModel.MODE_ENTER_MOVES);
        gameModel.triggerStateChange();
    }

    private void handleStateChangeAnalysis() {

        String fen = gameModel.getGame().getCurrentNode().getBoard().fen();
        engineController.sendCommand("stop");
        engineController.sendCommand("position fen "+fen);
        engineController.sendCommand("go infinite");

    }

    private void handleStateChangeGameAnalysis() {

        //System.out.println("state change game analysis received");

        boolean continueAnalysis = true;

        //System.out.println(gameModel.getGame().getCurrentNode().getParent().getBoard().toString());
        //System.out.println(gameModel.getGame().getRootNode().getBoard().toString());
        boolean parentIsRoot = (gameModel.getGame().getCurrentNode().getParent() == gameModel.getGame().getRootNode());
        if(!parentIsRoot) {
            // if the current position is in the opening book,
            // we stop the analysis
            long zobrist = gameModel.getGame().getCurrentNode().getBoard().getZobrist();
            if(gameModel.book.inBook(zobrist)) {
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
                engineController.sendCommand("stop");
                engineController.sendCommand("position fen " + fen);
                engineController.sendCommand("go movetime " + (gameModel.getGameAnalysisThinkTime() * 1000));
            }
        } else {
            continueAnalysis = false;
        }

        if(!continueAnalysis) {
            // we are at the root or found a book move
            activateEnterMovesMode();
            //FlatAlert alert = new FlatAlert(Alert.AlertType.INFORMATION);
            DialogSimpleAlert dlg = new DialogSimpleAlert();
            dlg.show("     The analysis is finished.     ");
        }
    }

    public void activatePlayWhiteMode() {
        gameModel.lastSeenBestmove = "";
        // first change gamestate and reset engine
        engineController.sendCommand("stop");
        engineController.sendCommand("quit");
        String cmdEngine = gameModel.activeEngine.getPath();
        engineController.sendCommand("start "+cmdEngine);
        engineController.sendCommand("uci");
        engineController.sendCommand("ucinewgame");
        for(EngineOption enOpt : gameModel.activeEngine.options) {
            if(enOpt.isNotDefault()) {
                if(enOpt.type == EngineOption.EN_OPT_TYPE_SPIN) {
                    /*
                    System.out.println("en opt: " + enOpt.name);
                    System.out.println("en opt def: " + enOpt.spinDefault);
                    System.out.println("en opt val:" + enOpt.spinValue);
                    System.out.println("is not def: "+enOpt.isNotDefault());
                     */
                }
                engineController.sendCommand(enOpt.toUciCommand());
            }
        }
        if(gameModel.activeEngine.isInternal()) {
            engineController.sendCommand("setoption name Skill Level value "+gameModel.getEngineStrength());
        }
        // trigger statechange
        gameModel.setMode(GameModel.MODE_PLAY_WHITE);
        gameModel.setFlipBoard(false);
        gameModel.setHumanPlayerColor(CONSTANTS.WHITE);
        gameModel.triggerStateChange();
    }

    public void activatePlayBlackMode() {
        gameModel.lastSeenBestmove = "";
        // first change gamestate and reset engine
        engineController.sendCommand("stop");
        engineController.sendCommand("quit");
        String cmdEngine = gameModel.activeEngine.getPath();
        engineController.sendCommand("start "+cmdEngine);
        engineController.sendCommand("uci");
        engineController.sendCommand("ucinewgame");
        for(EngineOption enOpt : gameModel.activeEngine.options) {
            if(enOpt.isNotDefault()) {
                engineController.sendCommand(enOpt.toUciCommand());
            }
        }
        if(gameModel.activeEngine.isInternal()) {
            engineController.sendCommand("setoption name Skill Level value "+gameModel.getEngineStrength());
        }
        // trigger statechange
        gameModel.setMode(GameModel.MODE_PLAY_BLACK);
        gameModel.setFlipBoard(true);
        gameModel.setHumanPlayerColor(CONSTANTS.BLACK);
        gameModel.triggerStateChange();
    }

    public void activatePlayoutPositionMode() {
        gameModel.lastSeenBestmove = "";
        // first change gamestate and reset engine
        engineController.sendCommand("stop");
        engineController.sendCommand("quit");
        String cmdEngine = gameModel.activeEngine.getPath();
        engineController.sendCommand("start "+cmdEngine);
        engineController.sendCommand("uci");
        engineController.sendCommand("ucinewgame");
        for(EngineOption enOpt : gameModel.activeEngine.options) {
            if(enOpt.isNotDefault()) {
                engineController.sendCommand(enOpt.toUciCommand());
            }
        }
        // trigger statechange
        gameModel.setMode(GameModel.MODE_PLAYOUT_POSITION);
        gameModel.setFlipBoard(false);
        gameModel.triggerStateChange();
    }

    public void activateGameAnalysisMode() {

        gameModel.lastSeenBestmove = "";

        gameModel.getGame().removeAllComments();
        gameModel.getGame().removeAllVariants();
        gameModel.getGame().setTreeWasChanged(true);

        engineController.sendCommand("stop");
        engineController.sendCommand("quit");
        String cmdEngine = gameModel.activeEngine.getPath();
        engineController.sendCommand("start "+cmdEngine);
        engineController.sendCommand("uci");
        engineController.sendCommand("ucinewgame");
        if(gameModel.activeEngine.supportsMultiPV()) {
            engineController.sendCommand("setoption name MultiPV value 1");
        }
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
        //System.out.println(gameModel.getGame().getCurrentNode().getBoard().fen());
        //System.out.println(Long.toHexString(zobrist));
        //System.out.println(gameModel.book.readFile);
        ArrayList<String> uciMoves0 = gameModel.book.findMoves(zobrist);
        //System.out.println("found moves: "+(uciMoves0.size()));
        if(gameModel.book.inBook(zobrist)) {
            //System.out.println("position in book");
            ArrayList<String> uciMoves = gameModel.book.findMoves(zobrist);
            int idx = (int) (Math.random() * uciMoves.size());
            //System.out.println("random idx: "+idx);
            handleBestMove("BESTMOVE|"+uciMoves.get(idx));
        } else {
            //System.out.println("position not found in book");
            String fen = gameModel.getGame().getCurrentNode().getBoard().fen();
            engineController.sendCommand("stop");
            engineController.sendCommand("position fen "+fen);
            engineController.sendCommand("go movetime "+(gameModel.getComputerThinkTimeSecs()*1000));
        }
    }

    public void handleStateChangePlayoutPosition() {

        String fen = gameModel.getGame().getCurrentNode().getBoard().fen();
        engineController.sendCommand("stop");
        engineController.sendCommand("position fen "+fen);
        engineController.sendCommand("go movetime "+(gameModel.getComputerThinkTimeSecs()*1000));
    }

    private void addBestPv() {
        String[] uciMoves = gameModel.currentBestPv.split(" ");
        GameNode currentNode = gameModel.getGame().getCurrentNode();

        for (String uciMove : uciMoves) {
            try {
                GameNode next = new GameNode();
                Board board = currentNode.getBoard().makeCopy();
                //System.out.println("uciMove: "+uciMove);
                Move m = new Move(uciMove);
                board.apply(m);
                next.setMove(m);
                next.setBoard(board);
                next.setParent(currentNode);
                // to avoid bugs when incoherent information is
                // given/received by the engine, do not add lines that already exist
                if(currentNode.getVariations().size() > 0) {
                    String mUciChild0 = currentNode.getVariation(0).getMove().getUci();
                    if(mUciChild0.equals(uciMove)) {
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


    public void editEngines() {
        gameModel.setMode(GameModel.MODE_ENTER_MOVES);
        gameModel.triggerStateChange();
        DialogEngines dlg = new DialogEngines();
        ArrayList<Engine> enginesCopy = new ArrayList<>();
        for(Engine engine : gameModel.engines) {
            enginesCopy.add(engine);
        }
        int selectedIdx = gameModel.engines.indexOf(gameModel.activeEngine);
        if(selectedIdx < 0) {
            selectedIdx = 0;
        }
        boolean accepted = dlg.show(enginesCopy, selectedIdx);
        if(accepted) {
            //List<Engine> engineList = dlg.engineList
            ArrayList<Engine> engineList = new ArrayList<>(dlg.engineList);
            Engine selectedEngine = dlg.engineList.get(dlg.selectedIndex);
            gameModel.engines = engineList;
            gameModel.activeEngine = selectedEngine;
            gameModel.triggerStateChange();
        }
    }

    public void handleBestMove(String bestmove) {
        //System.out.println("handle bestmove, in: "+bestmove);

        int mode = gameModel.getMode();

        if(mode == GameModel.MODE_ENTER_MOVES) {
            return;
        }

        if(gameModel.lastSeenBestmove.equals(bestmove)) {
            return;
        }

        String[] bestmoveItems = bestmove.split("\\|");

        if (mode == GameModel.MODE_PLAY_WHITE || mode == GameModel.MODE_PLAY_BLACK  || mode == GameModel.MODE_PLAYOUT_POSITION) {

            gameModel.lastSeenBestmove = bestmove;
            // todo: catch Exceptions!
            String uci = bestmoveItems[1].split(" ")[0];
            Move m = new Move(uci);
            Board b = gameModel.getGame().getCurrentNode().getBoard();
            if (b.isLegal(m)) {
                if(mode == GameModel.MODE_PLAY_WHITE && b.turn == CONSTANTS.BLACK) {
                    gameModel.getGame().applyMove(m);
                    gameModel.triggerStateChange();
                }
                if(mode == GameModel.MODE_PLAY_BLACK && b.turn == CONSTANTS.WHITE) {
                    gameModel.getGame().applyMove(m);
                    gameModel.triggerStateChange();
                }
                if(mode == GameModel.MODE_PLAYOUT_POSITION) {
                    gameModel.getGame().applyMove(m);
                    gameModel.triggerStateChange();
                }
            }
        }
        if(mode == GameModel.MODE_GAME_ANALYSIS) {

            gameModel.lastSeenBestmove = bestmove;

            // first update information for current node
            gameModel.childBestPv = gameModel.currentBestPv;
            gameModel.childBestEval = gameModel.currentBestEval;
            gameModel.childIsMate = gameModel.currentIsMate;
            gameModel.childMateInMoves = gameModel.currentMateInMoves;

            gameModel.currentBestPv = bestmoveItems[3];
            gameModel.currentBestEval = Integer.parseInt(bestmoveItems[2]);
            gameModel.currentIsMate = bestmoveItems[4].equals("true");
            // some engines, like arasan report 0.00 in mate position with nullmove
            // thus check manually
            //if(gameModel.getGame().getCurrentNode().getBoard().isCheckmate()) {
            //    gameModel.currentIsMate = true;
            //}
            //System.out.println("handle State Change@"+bestmove);
            //System.out.println("handle State Change@"+gameModel.getGame().getCurrentNode().getMove().getUci()+": "+gameModel.currentIsMate);
            //System.out.println("handle State Change@"+gameModel.getGame().getCurrentNode().getBoard().toString());
            Board nodeboard = gameModel.getGame().getCurrentNode().getBoard();
            //System.out.println("------------------------");
            //System.out.println(nodeboard.toString());
            //System.out.println("bestmove: "+bestmove);

            gameModel.currentMateInMoves = Integer.parseInt(bestmoveItems[5]);


            // completely skip that for black or white, if
            // that was chosen in the analysis
            boolean turn = gameModel.getGame().getCurrentNode().getBoard().turn;
            if( (gameModel.getGameAnalysisForPlayer() == GameModel.BOTH_PLAYERS)
                || (gameModel.getGameAnalysisForPlayer() == CONSTANTS.IWHITE && turn == CONSTANTS.WHITE)
                    || (gameModel.getGameAnalysisForPlayer() == CONSTANTS.IBLACK && turn == CONSTANTS.BLACK)) {

                if(!gameModel.currentIsMate && !gameModel.childIsMate) {
                    boolean wMistake = turn == CONSTANTS.WHITE && ((gameModel.currentBestEval - gameModel.childBestEval) >= gameModel.getGameAnalysisThreshold());
                    boolean bMistake = turn == CONSTANTS.BLACK && ((gameModel.currentBestEval - gameModel.childBestEval) <= gameModel.getGameAnalysisThreshold());

                    if(wMistake || bMistake) {
                        String uci = bestmoveItems[1].split(" ")[0];
                        String nextMove = gameModel.getGame().getCurrentNode().getVariation(0).getMove().getUci();
                        if (!uci.equals(nextMove)) {
                            //System.out.println("played was: "+gameModel.getGame().getCurrentNode().getVariation(0).getSan());
                            //System.out.println("w/ "+gameModel.childBestEval);
                            //System.out.println("but: "+ gameModel.currentBestPv);
                            //System.out.println("gave: "+ gameModel.currentBestEval);

                            addBestPv();

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

                if(gameModel.currentIsMate && !gameModel.childIsMate) {
                    // the current player missed a mate
                    addBestPv();

                    NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
                    DecimalFormat decim = (DecimalFormat) nf;
                    decim.applyPattern("0.00");
                    String sChildBest = decim.format(gameModel.childBestEval / 100.0);

                    String sCurrentBest = "";
                    if(turn == CONSTANTS.WHITE) {
                        sCurrentBest = "#"+(Math.abs(gameModel.currentMateInMoves));
                    } else {
                        sCurrentBest = "#-"+(Math.abs(gameModel.currentMateInMoves));
                    }

                    ArrayList<GameNode> vars = gameModel.getGame().getCurrentNode().getVariations();
                    if (vars != null && vars.size() > 1) {
                        GameNode child0 = gameModel.getGame().getCurrentNode().getVariation(0);
                        child0.setComment(sChildBest);
                        GameNode child1 = gameModel.getGame().getCurrentNode().getVariation(1);
                        child1.setComment(sCurrentBest);
                    }
                }

                if(!gameModel.currentIsMate && gameModel.childIsMate) {
                    // the current player  moved into a mate
                    addBestPv();

                    NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
                    DecimalFormat decim = (DecimalFormat) nf;
                    decim.applyPattern("0.00");
                    String sCurrentBest = decim.format(gameModel.currentBestEval / 100.0);

                    String sChildBest = "";
                    if(turn == CONSTANTS.WHITE) {
                        sChildBest = "#-"+(Math.abs(gameModel.childMateInMoves));
                    } else {
                        sChildBest = "#"+(Math.abs(gameModel.childMateInMoves));
                    }

                    ArrayList<GameNode> vars = gameModel.getGame().getCurrentNode().getVariations();
                    if (vars != null && vars.size() > 1) {
                        GameNode child0 = gameModel.getGame().getCurrentNode().getVariation(0);
                        child0.setComment(sChildBest);
                        GameNode child1 = gameModel.getGame().getCurrentNode().getVariation(1);
                        child1.setComment(sCurrentBest);
                    }
                }

                if(gameModel.currentIsMate && gameModel.childIsMate) {
                    // the current player had a mate, but instead of executing it, he moved into a mate
                    // but we also want to skip the situation where the board position is checkmate
                    //System.out.println("currentMateinMove: "+gameModel.currentMateInMoves);
                    //System.out.println("childMateinMove: "+gameModel.childMateInMoves);
                    if ( (gameModel.currentMateInMoves >= 0 && gameModel.childMateInMoves >= 0) &&
                         gameModel.childMateInMoves != 0) {

                        addBestPv();

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
            gameModel.getGame().setTreeWasChanged(true);
            //System.out.println("current fen: "+gameModel.getGame().getCurrentNode().getBoard().fen());
            //System.out.println("triggering statechange");
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

        boolean abort = false;

        // if we change from e.g. play white to enter moves, the state change would trigger
        // the notification again in enter moves mode after the state change. thus,
        // also check if
        if((isCheckmate || isStalemate || isThreefoldRepetition) && !gameModel.doNotNotifyAboutResult) {

            String message = "";
            if(isCheckmate) {
                message = "     Checkmate.     ";
            }
            if(isStalemate) {
                message = "     Stalemate.     ";
            }
            if(isThreefoldRepetition) {
                message = "Draw (Threefold Repetition)";
            }
            if(mode != GameModel.MODE_GAME_ANALYSIS) {
                DialogSimpleAlert dlg = new DialogSimpleAlert();
                dlg.show(message);
            }

            if(mode == GameModel.MODE_PLAY_WHITE || mode == GameModel.MODE_PLAY_BLACK || mode == GameModel.MODE_PLAYOUT_POSITION) {
                abort = true;
            }
        }

        if(abort) {
            // if we change from e.g. play white to enter moves, the state change would trigger
            // the notification again in enter moves mode after the state change. thus
            // set a marker here, that for the next state change we ignore
            // the result notification
            gameModel.doNotNotifyAboutResult = true;
            gameModel.setMode(GameModel.MODE_ENTER_MOVES);
            gameModel.triggerStateChange();
        } else {
            gameModel.doNotNotifyAboutResult = false;
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

}
