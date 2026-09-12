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

import org.asdfjkl.jfxchess.lib.Arrow;
import org.asdfjkl.jfxchess.lib.ColoredField;
import org.asdfjkl.jfxchess.lib.GameNode;
import org.asdfjkl.jfxchess.lib.Move;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class Controller_Board {

    private final Model_JFXChess model;
    private final GameSession gameSession;

    public Controller_Board(Model_JFXChess model, GameSession gameSession) {

        this.model = model;
        this.gameSession = gameSession;
    }

    public void applyMove(Move m) {

        gameSession.applyMove(m);
        model.notifySessionPositionChanged(gameSession);
    }

    public void addOrRemoveArrow(Arrow a) {

        gameSession.addOrRemoveArrow(a);
    }

    public void addOrRemoveColoredField(ColoredField c) {

        gameSession.addOrRemoveColoredField(c);
    }

    public ActionListener moveForward() {
        return e -> {
            ArrayList<GameNode> variations = gameSession.getGame().getCurrentNode().getVariations();
            if (variations.size() > 1) {
                ArrayList<String> nextMoves = new ArrayList<>();
                for (GameNode varI : variations) {
                    nextMoves.add(varI.getSan());
                }
                model.setShortcutsEnabled(false);
                DialogNextMove dlgNextMove = new DialogNextMove(dialogOwner(e), nextMoves);
                dlgNextMove.setVisible(true);
                model.setShortcutsEnabled(true);
                int selectedMove = dlgNextMove.getSelectedMove();
                // if selectedMove == -1, user aborted. Don't change anything.
                if (selectedMove >= 0) {
                    gameSession.goToChild(selectedMove);
                    model.notifySessionPositionChanged(gameSession);
                }
            } else { // only one move -> go to child
                gameSession.goToChild(0);
                model.notifySessionPositionChanged(gameSession);
            }
        };
    }

    public ActionListener moveBack() {
        return e -> {
            gameSession.goToParent();
            model.notifySessionPositionChanged(gameSession);
        };
    }

    public ActionListener seekToEnd() {
        return e -> {
            gameSession.seekToEnd();
            model.notifySessionPositionChanged(gameSession);
        };
    }

    public ActionListener seekToBeginning() {
        return e -> {
            gameSession.seekToBeginning();
            model.notifySessionPositionChanged(gameSession);
        };
    }

    public void goToNode(int node) {
        gameSession.goToNode(node);
        model.notifySessionPositionChanged(gameSession);
    }

    private Window dialogOwner(java.awt.event.ActionEvent event) {
        return model.mainFrameRef;
    }

}
