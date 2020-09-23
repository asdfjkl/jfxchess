package org.asdfjkl.jerryfx.gui;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.asdfjkl.jerryfx.lib.Board;
import org.asdfjkl.jerryfx.lib.Game;
import org.asdfjkl.jerryfx.lib.PgnPrinter;
import org.asdfjkl.jerryfx.lib.PgnReader;

public class EditMenuController {

    GameModel gameModel;
    PgnPrinter pgnPrinter;

    public EditMenuController(GameModel gameModel) {

        this.gameModel = gameModel;
        pgnPrinter = new PgnPrinter();
    }

    public void copyPosition() {

        String fen = gameModel.getGame().getCurrentNode().getBoard().fen();
        Clipboard systemClipboard = Clipboard.getSystemClipboard();
        systemClipboard.clear();
        ClipboardContent content = new ClipboardContent();
        content.putString(fen);
        systemClipboard.setContent(content);

    }

    public void copyGame() {

        String pgn = pgnPrinter.printGame(gameModel.getGame());
        Clipboard systemClipboard = Clipboard.getSystemClipboard();
        systemClipboard.clear();
        ClipboardContent content = new ClipboardContent();
        content.putString(pgn);
        systemClipboard.setContent(content);

    }

    public void enterPosition(double dialogHeight, BoardStyle style) {

        Board board = gameModel.getGame().getCurrentNode().getBoard();
        DialogEnterPosition dlg = new DialogEnterPosition();
        double width = dialogHeight * 1.6;
        boolean accepted = dlg.show(board, style, width, dialogHeight);
        if(accepted) {
            Board newBoard = dlg.currentBoard;
            if(newBoard.isConsistent()) {
                Game g = new Game();
                g.getRootNode().setBoard(newBoard);
                gameModel.setGame(g);
                gameModel.getGame().setTreeWasChanged(true);
                gameModel.triggerStateChange();
            }
        }
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
