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

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import java.awt.*;

public class DialogNewGame extends JDialog {

    public static int ENTER_ANALYSE = 0;
    public static int PLAY_BOT = 1;
    public static int PLAY_UCI = 2;

    private int selection = -1;

    public DialogNewGame(JFrame parent, String title) {
        super(parent, title, true);

        setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        double scale = HighDPIHelper.getUIScaleFactor();
        int squareSize = 64;

        JButton btnEnterAnalyse = new JButton();
        btnEnterAnalyse.setIcon(new FlatSVGIcon("icons/edit_square.svg"));
        btnEnterAnalyse.setToolTipText("Enter & Analyse");
        btnEnterAnalyse.setText("Enter & Analyse");
        btnEnterAnalyse.setHorizontalTextPosition(SwingConstants.CENTER);
        btnEnterAnalyse.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnEnterAnalyse.setFocusable(false);

        JButton btnPlayBot = new JButton();
        btnPlayBot.setIcon(new FlatSVGIcon("icons/smart_toy.svg"));
        btnPlayBot.setToolTipText("Play Bot");
        btnPlayBot.setText("Play Bot");
        btnPlayBot.setHorizontalTextPosition(SwingConstants.CENTER);
        btnPlayBot.setVerticalTextPosition(SwingConstants.BOTTOM);
        //btnPlayBot.putClientProperty("JButton.buttonType", "toolBarButton");
        btnPlayBot.setFocusable(false);

        JButton btnPlayUci = new JButton();
        btnPlayUci.setIcon(new FlatSVGIcon("icons/memory.svg"));
        btnPlayUci.setToolTipText("Play Engine");
        btnPlayUci.setText("Play Engine");
        btnPlayUci.setHorizontalTextPosition(SwingConstants.CENTER);
        btnPlayUci.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnPlayUci.setFocusable(false);

        // set width of all buttons to that one of
        // enter & analyze (widest one)
        Dimension d = btnEnterAnalyse.getPreferredSize();
        btnPlayBot.setPreferredSize(d);
        btnPlayUci.setPreferredSize(d);

        btnEnterAnalyse.addActionListener(e -> {
            selection = ENTER_ANALYSE;
            dispose();
        });
        add(btnEnterAnalyse);

        btnPlayBot.addActionListener(e -> {
            selection = PLAY_BOT;
            dispose();
        });
        add(btnPlayBot);

        btnPlayUci.addActionListener(e -> {
            selection = PLAY_UCI;
            dispose();
        });
        add(btnPlayUci);

        pack();
        setLocationRelativeTo(parent);

    }

    public int getSelection() {
        return selection;
    }

}


