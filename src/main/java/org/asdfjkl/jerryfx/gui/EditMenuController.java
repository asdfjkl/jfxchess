package org.asdfjkl.jerryfx.gui;

import org.asdfjkl.jerryfx.lib.Board;
import org.asdfjkl.jerryfx.lib.Game;
import org.asdfjkl.jerryfx.lib.PgnPrinter;
import org.asdfjkl.jerryfx.lib.PgnReader;

public class EditMenuController {

    GameModel gameModel;

    public EditMenuController(GameModel gameModel) {
        this.gameModel = gameModel;
    }

    public void paste(String s) {

        try {
            Board board = new Board(s);
            if(board.isConsistent()) {
                Game g = new Game();
                g.getRootNode().setBoard(board);
                gameModel.setGame(g);
                gameModel.getGame().setTreeWasChanged(true);
                gameModel.triggerStateChange();
            }
        } catch(IllegalArgumentException e) {
            // if not a fen string, maybee it's a full game
            PgnReader reader = new PgnReader();
            Game g = reader.readGame(s);
            PgnPrinter prn = new PgnPrinter();
            System.out.println(prn);
            // as a heuristic we assume it's really a pasted game string if either there is at least
            // two game nodes, or if there is a fen string for the root board
            if(g.getRootNode().hasChild() || !g.getRootNode().getBoard().isInitialPosition()) {
                gameModel.setGame(g);
                gameModel.getGame().setTreeWasChanged(true);
                gameModel.triggerStateChange();
            }

        }
    }

}
