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

public class DialogProgress extends JDialog {

    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final JButton cancelButton = new JButton("Cancel");

    public DialogProgress(JFrame parent, SwingWorker worker, String windowTitle) {
        super(parent, windowTitle, true);

        initUI();
        bindWorker(worker);

        cancelButton.addActionListener(e -> {
            worker.cancel(true);
            dispose();
        });
    }

    public DialogProgress(Window parent, SwingWorker worker, String windowTitle) {
        super(parent, windowTitle, ModalityType.APPLICATION_MODAL);

        initUI();
        bindWorker(worker);

        cancelButton.addActionListener(e -> {
            worker.cancel(true);
            dispose();
        });
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // ---- Main content panel with padding ----
        JPanel content = new JPanel();
        content.setLayout(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));

        // ---- Progress bar ----
        progressBar.setStringPainted(true);
        progressBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));

        // Stack label + progress bar
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.add(progressBar, BorderLayout.CENTER);

        content.add(centerPanel, BorderLayout.CENTER);

        // ---- Button panel (Windows-style centered button) ----
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        cancelButton.setPreferredSize(new Dimension(90, 26)); // small fixed width
        buttonPanel.add(cancelButton);


        // subtle top separator like native dialogs
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(10, 0, 5, 0)
        ));

        add(content, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setResizable(false);
        setLocationRelativeTo(getParent());
    }

    private void bindWorker(SwingWorker worker) {
        worker.addPropertyChangeListener(evt -> {
            switch (evt.getPropertyName()) {
                case "progress":
                    progressBar.setValue((Integer) evt.getNewValue());
                    break;

                case "state":
                    if (evt.getNewValue() == SwingWorker.StateValue.DONE) {
                        dispose();
                    }
                    break;
            }
        });
    }
}