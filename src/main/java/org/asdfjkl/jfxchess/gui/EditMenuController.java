/* JerryFX - A Chess Graphical User Interface
 * Copyright (C) 2020 Dominik Klein
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

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.asdfjkl.jfxchess.lib.Board;
import org.asdfjkl.jfxchess.lib.Game;
import org.asdfjkl.jfxchess.lib.PgnPrinter;
import org.asdfjkl.jfxchess.lib.PgnReader;
import java.util.Map;

public class EditMenuController {

    final GameModel gameModel;
    final PgnPrinter pgnPrinter;

    public EditMenuController(GameModel gameModel) {

        this.gameModel = gameModel;
        pgnPrinter = new PgnPrinter();
    }

    public void copyPosition() {

        String fen = gameModel.getGame().getCurrentNode().getBoard().fen();
        Clipboard systemClipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(fen);
        systemClipboard.setContent(content);

    }

    public void copyGame() {

        String pgn = pgnPrinter.printGame(gameModel.getGame());
        Clipboard systemClipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(pgn);
        systemClipboard.setContent(content);

    }

    public void enterPosition(double dialogHeight, BoardStyle style) {

        Board board = gameModel.getGame().getCurrentNode().getBoard();
        DialogEnterPosition dlg = new DialogEnterPosition();
        double width = dialogHeight * 1.6;
        //double width = dialogHeight * 1.7;
        boolean accepted = dlg.show(board, style, width, dialogHeight, gameModel.THEME);
        if(accepted) {
            Board newBoard = dlg.getCurrentBoard();
            if(newBoard.isConsistent()) {
                Game g = new Game();
                g.getRootNode().setBoard(newBoard);
                gameModel.setGame(g);
                gameModel.getGame().setTreeWasChanged(true);
                gameModel.getGame().setHeaderWasChanged(true);
                gameModel.triggerStateChange();
            }
        }
    }

    public void editGameData() {
        DialogEditGameData dlg = new DialogEditGameData();
        boolean accteped = dlg.show(gameModel.getGame().getPgnHeaders(),
                gameModel.getGame().getResult(),
                gameModel.THEME);
        if(accteped) {
            for (Map.Entry<String, String> entry : dlg.pgnHeaders.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                gameModel.getGame().setHeader(key, value);
            }
            gameModel.getGame().setResult(dlg.gameResult);
            gameModel.getGame().setHeaderWasChanged(true);
            gameModel.triggerStateChange();
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
            // if not a fen string, maybe it's a full game
            PgnReader reader = new PgnReader();
            Game g = reader.readGame(s);
            // PgnPrinter prn = new PgnPrinter();
            // as a heuristic we assume it's really a pasted game string if either there is at least
            // two game nodes, or if there is a fen string for the root board
            if(g.getRootNode().hasChild() || !g.getRootNode().getBoard().isInitialPosition()) {
                gameModel.setGame(g);
                gameModel.getGame().setTreeWasChanged(true);
                gameModel.getGame().setHeaderWasChanged(true);
                gameModel.triggerStateChange();
            }

        }
    }

}
