package org.asdfjkl.jerryfx.gui;

import org.asdfjkl.jerryfx.lib.Board;
import org.asdfjkl.jerryfx.lib.CONSTANTS;
import org.asdfjkl.jerryfx.lib.Move;

public class ModeMenuController implements StateChangeListener {

    GameModel gameModel;
    EngineOutputView engineOutputView;
    EngineController engineController;

    public ModeMenuController(GameModel gameModel, EngineOutputView engineOutputView) {

        this.gameModel = gameModel;
        this.engineOutputView = engineOutputView;
    }

    public void handleEngineInfo(String s) {

        if(s.startsWith("INFO")) {
            this.engineOutputView.setText(s.substring(5));
        }
        if(s.startsWith("BESTMOVE")) {

            System.out.println("got bestmove: "+s.substring(9));
            System.out.println("got bestmove uci: "+s.substring(9).split(" ")[0]);
            String uciMove = s.substring(9).split(" ")[0];
            handleBestMove(uciMove);
        }
    }

    public void setEngineController(EngineController engineController) {
        this.engineController = engineController;
    }

    public void activateAnalysisMode() {

        // first change gamestate and reset engine
        engineController.sendCommand("stop");
        engineController.sendCommand("quit");
        String cmdStockfish = "C:\\Program Files (x86)\\Jerry_Chess\\engine\\stockfish.exe";
        engineController.sendCommand("start "+cmdStockfish);
        engineController.sendCommand("ucinewgame");
        engineController.sendCommand("uci");
        engineController.sendCommand("setoption name MultiPV value "+gameModel.getMultiPv());
        // set engine strength to MAX
        // since we use stockfish, this is 20
        // will be just ignored by other engines
        // internal engine is always at idx 0.
        // for 0, i.e. INTERNAL_ENGINE_IDX
        // todo
        // set all engine options
        // todo
        // also send multi pv command according to current selection
        // todo
        // then trigger state change
        gameModel.setMode(GameModel.MODE_ANALYSIS);
        gameModel.triggerStateChange();
    }

    public void activateEnterMovesMode() {
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

    public void activatePlayWhiteMode() {
        // first change gamestate and reset engine
        engineController.sendCommand("stop");
        engineController.sendCommand("quit");
        String cmdStockfish = "C:\\Program Files (x86)\\Jerry_Chess\\engine\\stockfish.exe";
        engineController.sendCommand("start "+cmdStockfish);
        engineController.sendCommand("ucinewgame");
        engineController.sendCommand("uci");
        // todo: send engine options, esp. strength
        // trigger statechange
        gameModel.setMode(GameModel.MODE_PLAY_WHITE);
        gameModel.setFlipBoard(false);
        gameModel.setHumanPlayerColor(CONSTANTS.WHITE);
        gameModel.triggerStateChange();
    }

    public void activatePlayBlackMode() {
        // first change gamestate and reset engine
        engineController.sendCommand("stop");
        engineController.sendCommand("quit");
        String cmdStockfish = "C:\\Program Files (x86)\\Jerry_Chess\\engine\\stockfish.exe";
        engineController.sendCommand("start "+cmdStockfish);
        engineController.sendCommand("ucinewgame");
        engineController.sendCommand("uci");
        // todo: send engine options, esp. strength
        // trigger statechange
        gameModel.setMode(GameModel.MODE_PLAY_BLACK);
        gameModel.setFlipBoard(true);
        gameModel.setHumanPlayerColor(CONSTANTS.BLACK);
        gameModel.triggerStateChange();
    }

    public void handleStateChangePlayWhiteOrBlack() {
        // todo:
        // first check if we can apply a bookmove
        /*
        chess::GameNode *current = this->gameModel->getGame()->getCurrentNode();
        bool usedBook = false;
        QString uci = QString("");
        if(this->gameModel->canAndMayUseBook(current)) {
            QVector<chess::Move> mvs = this->gameModel->getBookMoves(current);
            if(mvs.size() > 0) {
                int sel = (rand() % (int)(mvs.size()));
                chess::Move mi = mvs.at(sel);
                uci = mi.uci();
                usedBook = true;
            }
        }
        if(!usedBook) {
            QString fen = this->gameModel->getGame()->getCurrentNode()->getBoard().fen();
            this->uci_controller->uciSendFen(fen);
            QString position = QString("position fen ").append(fen);
            this->uci_controller->uciSendPosition(position);
            this->uci_controller->uciGoMovetime(this->gameModel->engineThinkTimeMs);
        } else {
            this->onBestMove(uci);
        }*/

        String fen = gameModel.getGame().getCurrentNode().getBoard().fen();
        System.out.println("sending fen "+fen);
        engineController.sendCommand("stop");
        engineController.sendCommand("position fen "+fen);
        engineController.sendCommand("go movetime "+gameModel.getComputerThinkTimeMs());
    }

    public void handleBestMove(String bestmove) {
        int mode = gameModel.getMode();
        if (mode == GameModel.MODE_PLAY_WHITE || mode == GameModel.MODE_PLAY_BLACK) {
            // todo: catch Exceptions!
            String uci = bestmove;
            Move m = new Move(uci);
            Board b = gameModel.getGame().getCurrentNode().getBoard();
            if (b.isLegal(m)) {
                gameModel.getGame().applyMove(m);
                gameModel.triggerStateChange();
            }
        }
    }

    @Override
    public void stateChange() {
        int mode = gameModel.getMode();
        boolean turn = gameModel.getGame().getCurrentNode().getBoard().turn;

        System.out.println("mode: "+mode);
        System.out.println("turn: "+(turn == gameModel.getHumanPlayerColor()));

        if(mode == GameModel.MODE_ANALYSIS) {
            handleStateChangeAnalysis();
        }
        if((mode == GameModel.MODE_PLAY_WHITE || mode == GameModel.MODE_PLAY_BLACK)
            && turn != gameModel.getHumanPlayerColor()) {
            System.out.println("handle state change black or white");
            handleStateChangePlayWhiteOrBlack();
        }
        // todo: playout pos. game analysis, checkmate, three-fold repitition, stalemate, 50-move rule
    }

}
