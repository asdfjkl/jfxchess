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

import org.asdfjkl.jfxchess.lib.SearchPattern;

import javax.swing.*;
import java.awt.*;

public class DialogSearchGames extends JDialog {

    private final TabSearchGameData tabSearchGameData;
    private final TabSearchPosition tabSearchPosition;

    private final JCheckBox cbSearchGameData;
    private final JCheckBox cbSearchPosition;

    private final JButton okButton;
    private final JButton cancelButton;

    private SearchPattern pattern;
    private boolean isConfirmed = false;

    public DialogSearchGames(Window parent, SearchPattern pattern) {
        this(parent, pattern, null);
    }

    public DialogSearchGames(Window parent, SearchPattern pattern, Model_JFXChess model) {
        super(parent, "Search Games", ModalityType.APPLICATION_MODAL);
        this.pattern = (pattern != null) ? pattern : new SearchPattern();

        setLayout(new BorderLayout(5, 5));
        setSize(860, 580);
        setLocationRelativeTo(parent);

        // --- Tabs ---
        JTabbedPane tabbedPane = new JTabbedPane();
        tabSearchGameData = new TabSearchGameData();
        tabSearchPosition = new TabSearchPosition(model);

        tabbedPane.addTab("Game Data", tabSearchGameData);
        tabbedPane.addTab("Position", tabSearchPosition);

        add(tabbedPane, BorderLayout.CENTER);

        // --- Bottom Controls ---
        JPanel bottomContainer = new JPanel(new BorderLayout(5, 5));
        bottomContainer.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        // Filter Selection Checkboxes (horizontally next to each other)
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        cbSearchGameData = new JCheckBox("Search for Game Data");
        cbSearchPosition = new JCheckBox("Search Position");

        filterPanel.add(cbSearchGameData);
        filterPanel.add(cbSearchPosition);
        bottomContainer.add(filterPanel, BorderLayout.WEST);

        // Action Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        okButton = new JButton("Search");
        cancelButton = new JButton("Cancel");

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        bottomContainer.add(buttonPanel, BorderLayout.EAST);

        add(bottomContainer, BorderLayout.SOUTH);

        // Checkbox toggle listener to enable/disable Search button
        cbSearchGameData.addActionListener(e -> updateSearchButtonState());
        cbSearchPosition.addActionListener(e -> updateSearchButtonState());

        // Button behavior
        okButton.addActionListener(e -> {
            isConfirmed = true;
            applyToSearchPattern();
            dispose();
        });
        cancelButton.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(okButton);

        setSearchPattern(this.pattern);
    }

    private void updateSearchButtonState() {
        okButton.setEnabled(cbSearchGameData.isSelected() || cbSearchPosition.isSelected());
    }

    private void applyToSearchPattern() {
        if (pattern == null) {
            pattern = new SearchPattern();
        }
        pattern.setSearchGameData(cbSearchGameData.isSelected());
        pattern.setSearchPosition(cbSearchPosition.isSelected());
        tabSearchGameData.applyToPattern(pattern);
        tabSearchPosition.applyToPattern(pattern);
    }

    public SearchPattern getSearchPattern() {
        applyToSearchPattern();
        return pattern;
    }

    public void setSearchPattern(SearchPattern pattern) {
        this.pattern = (pattern != null) ? pattern : new SearchPattern();
        cbSearchGameData.setSelected(this.pattern.isSearchGameData());
        cbSearchPosition.setSelected(this.pattern.isSearchPosition());
        tabSearchGameData.setFromPattern(this.pattern);
        tabSearchPosition.setFromPattern(this.pattern);
        updateSearchButtonState();
    }

    public boolean isConfirmed() {
        return isConfirmed;
    }
}
