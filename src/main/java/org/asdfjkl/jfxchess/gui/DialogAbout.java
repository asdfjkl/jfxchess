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
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;

public class DialogAbout extends JDialog {

    public DialogAbout(JFrame parent, String version) {

        super(parent, "About", true);

        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Use JTextPane for proper centering
        JTextPane textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setOpaque(false);
        textPane.setFocusable(false);
        textPane.setCursor(null);

        // Use standard Swing font
        Font uiFont = UIManager.getFont("Label.font");
        textPane.setFont(uiFont);

        // Center alignment
        StyledDocument doc = textPane.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);

        // Text content
        textPane.setText(
                "JFXChess " + version + "\n\n" +
                "Copyright © 2014-2026\n" +
                "Dominik Klein\n" +
                "and contributors\n" +
                "licensed under GNU GPL 2" + "\n\n" +
                "Contributors\n" +
                "TTorell, mipper\n\n"+
                "Stockfish Chess Engine\n" +
                "by the Stockfish-Team\n\n" +
                "Piece Images\n" +
                "from Raptor Chess Interface\n\n" +
                "        all licensed under GNU GPL 2        \n\n" +
                "Thanks to all who provided\n"+
                "feedback and/or bug-reports!"
        );

        // Wrap in scroll pane (no borders)
        JScrollPane scroll = new JScrollPane(textPane);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        // OK button
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton);

        main.add(scroll, BorderLayout.CENTER);
        main.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(main);
        setSize(300, 450);
        setLocationRelativeTo(parent);
        setResizable(false);

    }
}