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

import java.awt.event.ActionListener;
import java.util.ArrayList;

public class Controller_Board {

    private final Model_JFXChess model;

    public Controller_Board(Model_JFXChess model) {

        this.model = model;
    }

    public void applyMove(Move m) {

        model.applyMove(m);
    }

    public void addOrRemoveArrow(Arrow a) {

        model.getGame().getCurrentNode().addOrRemoveArrow(a);
    }

    public void addOrRemoveColoredField(ColoredField c) {

        model.getGame().getCurrentNode().addOrRemoveColoredField(c);
    }

    public ActionListener moveForward() {
        return e -> {
            ArrayList<GameNode> variations = model.getGame().getCurrentNode().getVariations();
            if (variations.size() > 1) {
                ArrayList<String> nextMoves = new ArrayList<>();
                for (GameNode varI : variations) {
                    nextMoves.add(varI.getSan());
                }
                model.setShortcutsEnabled(false);
                DialogNextMove dlgNextMove = new DialogNextMove(model.mainFrameRef, nextMoves);
                dlgNextMove.setVisible(true);
                model.setShortcutsEnabled(true);
                int selectedMove = dlgNextMove.getSelectedMove();
                // if selectedMove == -1, user aborted. Don't change anything.
                if (selectedMove >= 0) {
                    model.goToChild(selectedMove);
                }
            } else { // only one move -> go to child
                model.goToChild(0);
            }
        };
    }

    public ActionListener moveBack() {
        return e -> {
            model.goToParent();
        };
    }

    public ActionListener seekToEnd() {
        return e -> {
            model.seekToEnd();
        };
    }

    public ActionListener seekToBeginning() {
        return e -> {
            model.seekToBeginning();
        };
    }

    public void goToNode(int node) {
        model.goToNode(node);
    }

}
