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

import org.asdfjkl.jfxchess.lib.Board;
import org.asdfjkl.jfxchess.lib.CONSTANTS;
import org.asdfjkl.jfxchess.lib.SearchPattern;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class TabSearchPosition extends JPanel {

    private final Model_JFXChess model;
    private View_SearchPositionBoard viewSearchPositionBoard;
    private JRadioButton rbWhite;
    private JRadioButton rbBlack;
    private JRadioButton rbAny;

    public TabSearchPosition() {
        this(null);
    }

    public TabSearchPosition(Model_JFXChess model) {
        super(new BorderLayout(10, 10));
        this.model = model;

        add(createMainPanel(), BorderLayout.CENTER);
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        // --- Chessboard & Piece Selector (grows) ---
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 5, 5, 5);
        viewSearchPositionBoard = new View_SearchPositionBoard(model);
        panel.add(viewSearchPositionBoard, c);

        // --- Options panel (fixed width) ---
        c.gridx = 1;
        c.weightx = 0;
        c.weighty = 1;
        c.fill = GridBagConstraints.VERTICAL;
        panel.add(createOptionsPanel(), c);

        return panel;
    }

    private JPanel createOptionsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(200, 0));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 5, 5, 5);

        int y = 0;

        c.gridy = y++;
        panel.add(createTurnPanel(), c);

        c.gridy = y++;
        panel.add(createActionButtons(), c);

        // Filler to keep controls pinned to the top
        c.gridy = y;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        panel.add(new JPanel(), c);

        return panel;
    }

    private JPanel createTurnPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1));
        panel.setBorder(new TitledBorder("Turn"));

        rbWhite = new JRadioButton("White");
        rbBlack = new JRadioButton("Black");
        rbAny = new JRadioButton("Any Side", true);

        ButtonGroup group = new ButtonGroup();
        group.add(rbWhite);
        group.add(rbBlack);
        group.add(rbAny);

        panel.add(rbWhite);
        panel.add(rbBlack);
        panel.add(rbAny);

        rbWhite.addActionListener(e -> {
            viewSearchPositionBoard.setTurn(CONSTANTS.WHITE);
            viewSearchPositionBoard.repaint();
        });
        rbBlack.addActionListener(e -> {
            viewSearchPositionBoard.setTurn(CONSTANTS.BLACK);
            viewSearchPositionBoard.repaint();
        });

        return panel;
    }

    private JPanel createActionButtons() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 0, 5));

        JButton btnFlipBoard = new JButton("Flip Board");
        JButton btnInitialPosition = new JButton("Initial Position");
        JButton btnClearBoard = new JButton("Clear Board");
        JButton btnCurrentPosition = new JButton("Current Position");

        panel.add(btnFlipBoard);
        panel.add(btnInitialPosition);
        panel.add(btnClearBoard);
        panel.add(btnCurrentPosition);

        btnFlipBoard.addActionListener(e -> {
            viewSearchPositionBoard.flipBoard = !viewSearchPositionBoard.flipBoard;
            viewSearchPositionBoard.repaint();
        });

        btnInitialPosition.addActionListener(e -> {
            viewSearchPositionBoard.resetToStartingPosition();
            rbWhite.setSelected(true);
        });

        btnClearBoard.addActionListener(e -> {
            viewSearchPositionBoard.clearBoard();
            rbAny.setSelected(true);
        });

        btnCurrentPosition.addActionListener(e -> {
            if (model != null && model.getGame() != null && model.getGame().getCurrentNode() != null) {
                Board b = model.getGame().getCurrentNode().getBoard();
                if (b != null) {
                    viewSearchPositionBoard.copyBoard(b);
                    if (b.turn == CONSTANTS.WHITE) {
                        rbWhite.setSelected(true);
                    } else {
                        rbBlack.setSelected(true);
                    }
                    viewSearchPositionBoard.repaint();
                }
            }
        });

        return panel;
    }

    public void applyToPattern(SearchPattern pattern) {
        if (pattern == null) {
            return;
        }
        pattern.setSearchBoard(viewSearchPositionBoard.makeBoardCopy());
        if (rbWhite.isSelected()) {
            pattern.setSearchTurn(SearchPattern.TURN_WHITE);
        } else if (rbBlack.isSelected()) {
            pattern.setSearchTurn(SearchPattern.TURN_BLACK);
        } else {
            pattern.setSearchTurn(SearchPattern.TURN_ANY);
        }
    }

    public void setFromPattern(SearchPattern pattern) {
        if (pattern == null) {
            return;
        }
        if (pattern.getSearchBoard() != null) {
            viewSearchPositionBoard.copyBoard(pattern.getSearchBoard());
        }
        int turn = pattern.getSearchTurn();
        if (turn == SearchPattern.TURN_WHITE) {
            rbWhite.setSelected(true);
        } else if (turn == SearchPattern.TURN_BLACK) {
            rbBlack.setSelected(true);
        } else {
            rbAny.setSelected(true);
        }
    }

    public View_SearchPositionBoard getViewSearchPositionBoard() {
        return viewSearchPositionBoard;
    }
}
