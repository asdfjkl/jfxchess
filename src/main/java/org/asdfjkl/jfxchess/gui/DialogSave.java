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

public class DialogSave extends JDialog {

    public static final int CANCEL = 0;
    public static final int SAVE_REPLACE = 1;
    public static final int SAVE_NEW_APPEND = 2;

    // Backward-compatibility aliases
    public static final int REPLACE_CURRENT = SAVE_REPLACE;
    public static final int APPEND_CURRENT = SAVE_NEW_APPEND;
    public static final int SAVE_NEW = SAVE_NEW_APPEND;
    public static final int APPEND_OTHER = SAVE_NEW_APPEND;

    private int result = CANCEL;

    public DialogSave(Frame parent, boolean replaceEnabled) {
        super(parent, "Save Game", true);

        setLayout(new GridLayout(3, 1, 5, 5));
        ((JComponent) getContentPane()).setBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        );

        JButton btnReplace = new JButton("Save (Replace)");
        JButton btnAppend = new JButton("Save as New (Append)");
        JButton btnCancel = new JButton("Cancel");

        btnReplace.setEnabled(replaceEnabled);

        Dimension maxSize = getMaxButtonSize(btnReplace, btnAppend, btnCancel);
        for (JButton b : new JButton[]{btnReplace, btnAppend, btnCancel}) {
            b.setPreferredSize(maxSize);
        }

        btnReplace.addActionListener(e -> { result = SAVE_REPLACE; dispose(); });
        btnAppend.addActionListener(e -> { result = SAVE_NEW_APPEND; dispose(); });
        btnCancel.addActionListener(e -> { result = CANCEL; dispose(); });

        add(btnReplace);
        add(btnAppend);
        add(btnCancel);

        pack();
        setLocationRelativeTo(parent);
    }

    public DialogSave(Frame parent,
                      boolean appendCurrentEnabled,
                      boolean replaceCurrentEnabled) {
        this(parent, replaceCurrentEnabled);
    }

    public int getResult() {
        return result;
    }

    private Dimension getMaxButtonSize(JButton... buttons) {
        int maxWidth = 0;
        int maxHeight = 0;

        for (JButton b : buttons) {
            Dimension d = b.getPreferredSize();
            maxWidth = Math.max(maxWidth, d.width);
            maxHeight = Math.max(maxHeight, d.height);
        }

        return new Dimension(maxWidth, maxHeight);
    }

}