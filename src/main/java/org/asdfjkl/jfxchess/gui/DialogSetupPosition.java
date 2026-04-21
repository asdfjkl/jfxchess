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

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DialogSetupPosition extends JDialog implements SetupPositionListener {

    Model_JFXChess model;
    private JButton btnOk;
    private JButton btnCancel;
    private View_SetupPosition viewSetupPosition;
    private JRadioButton rbWhite;
    private JRadioButton rbBlack;
    private JCheckBox jcbWhite00;
    private JCheckBox jcbWhite000;
    private JCheckBox jcbBlack00;
    private JCheckBox jcbBlack000;
    private boolean confirmed;

    public DialogSetupPosition(Frame parent, Model_JFXChess model) {
        super(parent, "Enter Position", true);

        this.model = model;

        setLayout(new BorderLayout(10,10));

        add(createMainPanel(), BorderLayout.CENTER);
        add(createBottomButtons(), BorderLayout.SOUTH);

        setSize(800, 500);
        setLocationRelativeTo(parent);
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        // --- Chessboard (grows) ---
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5,5,5,5);
        viewSetupPosition = new View_SetupPosition(model);
        viewSetupPosition.addListener(this);
        panel.add(viewSetupPosition, c);

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
        panel.setPreferredSize(new Dimension(220, 0));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5,5,5,5);

        int y = 0;

        c.gridy = y++;
        panel.add(createCastlingPanel(), c);

        c.gridy = y++;
        panel.add(createEnPassantPanel(), c);

        c.gridy = y++;
        panel.add(createTurnPanel(), c);

        c.gridy = y++;
        panel.add(createActionButtons(), c);

        c.gridy = y++;
        c.weighty = 1;
        panel.add(Box.createVerticalGlue(), c);

        return panel;
    }

    private JPanel createCastlingPanel() {
        JPanel panel = new JPanel(new GridLayout(4,1));
        panel.setBorder(new TitledBorder("Castling Rights"));

        jcbWhite00 = new JCheckBox("White 0-0");
        jcbWhite000 = new JCheckBox("White 0-0-0");
        jcbBlack00 = new JCheckBox("Black 0-0");
        jcbBlack000 = new JCheckBox("Black 0-0");

        panel.add(jcbWhite00);
        panel.add(jcbWhite000);
        panel.add(jcbBlack00);
        panel.add(jcbBlack000);

        jcbWhite00.setSelected(model.getGame().getCurrentNode().getBoard().canCastleWhiteKing());
        jcbWhite000.setSelected(model.getGame().getCurrentNode().getBoard().canCastleWhiteQueen());
        jcbBlack00.setSelected(model.getGame().getCurrentNode().getBoard().canCastleBlackKing());
        jcbBlack000.setSelected(model.getGame().getCurrentNode().getBoard().canCastleBlackQueen());

        jcbWhite00.addActionListener(e -> {
            viewSetupPosition.setCastleWKing(jcbWhite00.isSelected());
            viewSetupPosition.repaint();
            btnOk.setEnabled(viewSetupPosition.isConsistent());
        });
        jcbWhite000.addActionListener(e -> {
            viewSetupPosition.setCastleWQueen(jcbWhite000.isSelected());
            viewSetupPosition.repaint();
            btnOk.setEnabled(viewSetupPosition.isConsistent());
        });
        jcbBlack00.addActionListener(e -> {
            viewSetupPosition.setCastleWKing(jcbBlack00.isSelected());
            viewSetupPosition.repaint();
            btnOk.setEnabled(viewSetupPosition.isConsistent());
        });
        jcbBlack000.addActionListener(e -> {
            viewSetupPosition.setCastleWKing(jcbBlack000.isSelected());
            viewSetupPosition.repaint();
            btnOk.setEnabled(viewSetupPosition.isConsistent());
        });

        return panel;
    }

    private JPanel createEnPassantPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(new TitledBorder("En Passant Square"));

        JComboBox<String> combo = new JComboBox<>(createEnPassantOptions());
        panel.add(combo);

        combo.addActionListener(e -> {
            try {
                String epSquare = combo.getSelectedItem().toString();
                viewSetupPosition.setEnPassantSquare(epSquare);
                btnOk.setEnabled(viewSetupPosition.isConsistent());
            } catch(NullPointerException ignored) {
            }
        });
        return panel;
    }

    private String[] createEnPassantOptions() {
        List<String> squares = new ArrayList<>();
        squares.add("-");

        for (char f='a'; f<='h'; f++)
            squares.add(f + "3");

        for (char f='a'; f<='h'; f++)
            squares.add(f + "6");

        return squares.toArray(new String[0]);
    }

    private JPanel createTurnPanel() {
        JPanel panel = new JPanel(new GridLayout(2,1));
        panel.setBorder(new TitledBorder("Turn"));

        rbWhite = new JRadioButton("White", true);
        rbBlack = new JRadioButton("Black");

        ButtonGroup group = new ButtonGroup();
        group.add(rbWhite);
        group.add(rbBlack);

        panel.add(rbWhite);
        panel.add(rbBlack);

        if(model.getGame().getCurrentNode().getBoard().turn == CONSTANTS.WHITE) {
            rbWhite.setSelected(true);
        } else {
            rbBlack.setSelected(true);
        }

        rbWhite.addActionListener(e -> {
            viewSetupPosition.setTurn(CONSTANTS.WHITE);
            viewSetupPosition.repaint();
            btnOk.setEnabled(viewSetupPosition.isConsistent());
        });
        rbBlack.addActionListener(e -> {
            viewSetupPosition.setTurn(CONSTANTS.BLACK);
            viewSetupPosition.repaint();
            btnOk.setEnabled(viewSetupPosition.isConsistent());
        });

        return panel;
    }

    private JPanel createActionButtons() {
        JPanel panel = new JPanel(new GridLayout(4,1,0,5));

        JButton btnFlipBoard = new JButton("Flip Board");
        JButton btnInitialPosition = new JButton("Initial Position");
        JButton btnClearBoard = new JButton("Clear Board");
        JButton btnCurrentPosition = new JButton("Current Position");

        panel.add(btnFlipBoard);
        panel.add(btnInitialPosition);
        panel.add(btnClearBoard);
        panel.add(btnCurrentPosition);

        btnFlipBoard.addActionListener(e -> {
            viewSetupPosition.flipBoard = !viewSetupPosition.flipBoard;
            viewSetupPosition.repaint();
        });

        btnInitialPosition.addActionListener(e -> {
            viewSetupPosition.resetToStartingPosition();
            rbWhite.setSelected(true);
            jcbWhite00.setSelected(true);
            jcbWhite000.setSelected(true);
            jcbBlack00.setSelected(true);
            jcbBlack000.setSelected(true);
            btnOk.setEnabled(true);
        });

        btnClearBoard.addActionListener(e -> {
            viewSetupPosition.clearBoard();
            rbWhite.setSelected(true);
            jcbWhite00.setSelected(false);
            jcbWhite000.setSelected(false);
            jcbBlack00.setSelected(false);
            jcbBlack000.setSelected(false);
            btnOk.setEnabled(true);
        });

        btnCurrentPosition.addActionListener(e -> {
            Board b = model.getGame().getCurrentNode().getBoard();
            viewSetupPosition.copyBoard(b);
            jcbWhite00.setSelected(b.canCastleWhiteKing());
            jcbWhite000.setSelected(b.canCastleWhiteQueen());
            jcbBlack00.setSelected(b.canCastleBlackKing());
            jcbBlack000.setSelected(b.canCastleBlackQueen());
            viewSetupPosition.repaint();
            btnOk.setEnabled(viewSetupPosition.isConsistent());
        });

        return panel;
    }

    private JPanel createBottomButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        btnOk = new JButton("OK");
        btnCancel = new JButton("Cancel");
        panel.add(btnOk);
        panel.add(btnCancel);

        btnOk.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        btnCancel.addActionListener(e -> dispose());

        return panel;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public Board getCurrentBoard() {
        return viewSetupPosition.makeBoardCopy();
    }

    public void boardChanged() {
        btnOk.setEnabled((viewSetupPosition.isBoardConsistent()));
        repaint();
    }
}