package org.asdfjkl.jerryfx.gui;

public class ModeMenuController implements StateChangeListener {

    GameModel gameModel;
    EngineOutputView engineOutputView;
    EngineController engineController;

    public ModeMenuController(GameModel gameModel, EngineOutputView engineOutputView) {

        this.gameModel = gameModel;
        this.engineOutputView = engineOutputView;
    }

    public void handleEngineInfo(String s) {

        this.engineOutputView.setText(s);
    }

    public void setEngineController(EngineController engineController) {
        this.engineController = engineController;
    }

    public void activateAnalysisMode() {

        // first change gamestate and reset engine
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

    @Override
    public void stateChange() {
        if(gameModel.getMode() == GameModel.MODE_ANALYSIS) {
            handleStateChangeAnalysis();
        }

        //
    }

}
