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
import java.awt.event.ActionEvent;

public class DialogThreads extends JDialog {

    private boolean isConfirmed = false;
    private final JSpinner spinner;

    public DialogThreads(Frame parent, int minThreads, int maxThreads, int initialValue) {
        super(parent, "Select CPU Threads", true);

        // Spinner with bounds
        spinner = new JSpinner(new SpinnerNumberModel(initialValue, minThreads, maxThreads, 1));

        // Main panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Label
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(new JLabel("CPU Threads"), gbc);

        // Spinner
        gbc.gridx = 1;
        mainPanel.add(spinner, gbc);

        // Buttons panel (right aligned)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        // Button actions
        okButton.addActionListener((ActionEvent e) -> {
            isConfirmed = true;
            dispose();
        });

        cancelButton.addActionListener((ActionEvent e) -> {
            isConfirmed = false;
            dispose();
        });

        // Layout dialog
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(parent);
    }

    public boolean isConfirmed() {
        return isConfirmed;
    }

    public int getNrThreads() {
        return (Integer) spinner.getValue();
    }
}