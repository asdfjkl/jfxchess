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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class DialogNextMove extends JDialog {

    private int selectedMove = -1;
    private final JList<String> list;

    public DialogNextMove(Window owner, ArrayList<String> possibleMoves) {
        super(owner, "Select Next Move", ModalityType.APPLICATION_MODAL);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Model
        DefaultListModel<String> model = new DefaultListModel<>();
        for (String move : possibleMoves) {
            model.addElement(move);
        }

        // List
        list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setVisibleRowCount(model.size());

        JScrollPane scrollPane = new JScrollPane(list);

        // Buttons
        JButton btnOk = new JButton("OK");
        JButton btnCancel = new JButton("Cancel");

        btnOk.addActionListener(e -> onOk());
        btnCancel.addActionListener(e -> onCancel());

        // Double-click support
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onOk();
                }
            }
        });

        // Key bindings
        InputMap im = list.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = list.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "ok");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "ok");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "cancel");

        am.put("ok", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOk();
            }
        });

        am.put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // Layout
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(btnOk);
        buttonPanel.add(btnCancel);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);

        // Optional icon
        //setIconImage(new ImageIcon("icons/app_icon.png").getImage());

        setSize(300, 200);
        setLocationRelativeTo(owner);
    }

    private void onOk() {
        selectedMove = list.getSelectedIndex();
        dispose();
    }

    private void onCancel() {
        selectedMove = -1;
        dispose();
    }

    public int getSelectedMove() {
        return selectedMove;
    }

    // Convenience method (optional)
    /*
    public static int showDialog(Window owner, List<String> moves) {
        DialogNextMove dialog = new DialogNextMove(owner, moves);
        dialog.setVisible(true);
        return dialog.getSelectedMove();
    }*/
}