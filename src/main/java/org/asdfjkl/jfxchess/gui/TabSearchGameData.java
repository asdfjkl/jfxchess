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

public class TabSearchGameData extends JPanel {

    private static final int INPUT_WIDTH = 260;

    private final JTextField txtWhite;
    private final JTextField txtBlack;
    private final JTextField txtEvent;
    private final JTextField txtSite;
    private final JCheckBox cbIgnoreColors;
    private final JCheckBox cbElo;
    private final JCheckBox cbYear;
    private JSpinner spMinElo;
    private JSpinner spMaxElo;
    private JSpinner spMinYear;
    private JSpinner spMaxYear;
    private JRadioButton rbEloOne;
    private JRadioButton rbEloBoth;
    private JRadioButton rbEloAverage;
    private JCheckBox cbWhiteWins;
    private JCheckBox cbBlackWins;
    private JCheckBox cbResUnknown;
    private JCheckBox cbResDraw;

    public TabSearchGameData() {
        super(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        int y = 0;

        // --- Text fields ---
        txtWhite = addAlignedField(this, gbc, y++, "White:");
        txtBlack = addAlignedField(this, gbc, y++, "Black:");

        // Ignore Colors
        gbc.gridx = 1;
        gbc.gridy = y++;
        cbIgnoreColors = new JCheckBox("Ignore Colors");
        add(cbIgnoreColors, gbc);

        txtEvent = addAlignedField(this, gbc, y++, "Event:");
        txtSite = addAlignedField(this, gbc, y++, "Site:");

        // --- Year ---
        gbc.gridx = 0;
        gbc.gridy = y;
        cbYear = new JCheckBox("Year:");
        add(cbYear, gbc);

        gbc.gridx = 1;
        add(createYearPanel(), gbc);
        y++;

        // --- Elo ---
        gbc.gridx = 0;
        gbc.gridy = y;
        cbElo = new JCheckBox("Elo:");
        add(cbElo, gbc);

        gbc.gridx = 1;
        add(createEloRangePanel(), gbc);
        y++;

        // Elo radio buttons
        gbc.gridx = 1;
        gbc.gridy = y++;
        add(createRadioGroup(), gbc);

        // --- Result ---
        gbc.gridx = 0;
        gbc.gridy = y;
        add(new JLabel("Result:"), gbc);

        gbc.gridx = 1;
        add(createResultPanel(), gbc);
        y++;

        // Filler column on the right (gridx = 2) to push content to the left
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel fillerX = new JPanel();
        fillerX.setOpaque(false);
        add(fillerX, gbc);

        // Filler row at the bottom to push content to the top
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 3;
        gbc.weightx = 0.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        JPanel fillerY = new JPanel();
        fillerY.setOpaque(false);
        add(fillerY, gbc);
    }

    private JTextField addAlignedField(JPanel panel, GridBagConstraints gbc, int y, String labelText) {
        gbc.gridx = 0;
        gbc.gridy = y;
        panel.add(new JLabel(labelText), gbc);

        JTextField field = new JTextField(20);
        field.setPreferredSize(new Dimension(INPUT_WIDTH, field.getPreferredSize().height));

        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrapper.setOpaque(false);
        wrapper.setPreferredSize(new Dimension(INPUT_WIDTH, field.getPreferredSize().height));
        wrapper.add(field);

        gbc.gridx = 1;
        panel.add(wrapper, gbc);

        return field;
    }

    private JPanel createYearPanel() {
        JPanel inner = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        inner.setOpaque(false);
        spMinYear = new JSpinner(new SpinnerNumberModel(500, 0, 3000, 1));
        inner.add(spMinYear);
        inner.add(new JLabel(" to "));
        spMaxYear = new JSpinner(new SpinnerNumberModel(2100, 0, 3000, 1));
        inner.add(spMaxYear);
        return wrap(inner);
    }

    private JPanel createEloRangePanel() {
        JPanel inner = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        inner.setOpaque(false);
        spMinElo = new JSpinner(new SpinnerNumberModel(1000, 0, 4000, 1));
        inner.add(spMinElo);
        inner.add(new JLabel(" to "));
        spMaxElo = new JSpinner(new SpinnerNumberModel(3000, 0, 4000, 1));
        inner.add(spMaxElo);
        return wrap(inner);
    }

    private JPanel wrap(JPanel inner) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrapper.setOpaque(false);
        wrapper.setPreferredSize(new Dimension(INPUT_WIDTH, inner.getPreferredSize().height));
        wrapper.add(inner);
        return wrapper;
    }

    private JPanel createRadioGroup() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        panel.setOpaque(false);

        rbEloOne = new JRadioButton("One");
        rbEloBoth = new JRadioButton("Both");
        rbEloAverage = new JRadioButton("Average");

        ButtonGroup group = new ButtonGroup();
        group.add(rbEloOne);
        group.add(rbEloBoth);
        group.add(rbEloAverage);

        panel.add(rbEloOne);
        panel.add(rbEloBoth);
        panel.add(rbEloAverage);

        return panel;
    }

    private JPanel createResultPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);

        cbWhiteWins = new JCheckBox("1-0");
        cbBlackWins = new JCheckBox("0-1");
        cbResUnknown = new JCheckBox("*");
        cbResDraw = new JCheckBox("1/2-1/2");
        panel.add(cbWhiteWins);
        panel.add(cbBlackWins);
        panel.add(cbResUnknown);
        panel.add(cbResDraw);

        return panel;
    }

    public void applyToPattern(SearchPattern pattern) {
        if (pattern == null) {
            return;
        }
        pattern.setWhiteName(txtWhite.getText());
        pattern.setBlackName(txtBlack.getText());
        pattern.setIgnoreNameColor(cbIgnoreColors.isSelected());
        pattern.setEvent(txtEvent.getText());
        pattern.setSite(txtSite.getText());
        if (cbElo.isSelected()) {
            if (rbEloAverage.isSelected()) {
                pattern.setCheckElo(SearchPattern.SEARCH_AVG_ELO);
            } else if (rbEloBoth.isSelected()) {
                pattern.setCheckElo(SearchPattern.SEARCH_BOTH_ELO);
            } else {
                pattern.setCheckElo(SearchPattern.SEARCH_ONE_ELO);
            }
        } else {
            pattern.setCheckElo(SearchPattern.SEARCH_IGNORE_ELO);
        }
        pattern.setMinElo((Integer) spMinElo.getValue());
        pattern.setMaxElo((Integer) spMaxElo.getValue());

        pattern.setCheckYear(cbYear.isSelected());
        pattern.setMinYear((Integer) spMinYear.getValue());
        pattern.setMaxYear((Integer) spMaxYear.getValue());

        pattern.setResultWhiteWins(cbWhiteWins.isSelected());
        pattern.setResultBlackWins(cbBlackWins.isSelected());
        pattern.setResultUndef(cbResUnknown.isSelected());
        pattern.setResultDraw(cbResDraw.isSelected());
    }

    public void setFromPattern(SearchPattern pattern) {
        if (pattern == null) {
            return;
        }
        txtWhite.setText(pattern.getWhiteName());
        txtBlack.setText(pattern.getBlackName());
        cbIgnoreColors.setSelected(pattern.isIgnoreNameColor());
        txtEvent.setText(pattern.getEvent());
        txtSite.setText(pattern.getSite());
        if (pattern.getCheckElo() == SearchPattern.SEARCH_IGNORE_ELO) {
            cbElo.setSelected(false);
            rbEloOne.setSelected(true);
        } else {
            cbElo.setSelected(true);
            if (pattern.getCheckElo() == SearchPattern.SEARCH_BOTH_ELO) {
                rbEloBoth.setSelected(true);
            } else if (pattern.getCheckElo() == SearchPattern.SEARCH_AVG_ELO) {
                rbEloAverage.setSelected(true);
            } else {
                rbEloOne.setSelected(true);
            }
        }
        spMinElo.setValue(pattern.getMinElo());
        spMaxElo.setValue(pattern.getMaxElo());

        cbYear.setSelected(pattern.isCheckYear());
        spMinYear.setValue(pattern.getMinYear());
        spMaxYear.setValue(pattern.getMaxYear());

        cbWhiteWins.setSelected(pattern.isResultWhiteWins());
        cbBlackWins.setSelected(pattern.isResultBlackWins());
        cbResUnknown.setSelected(pattern.isResultUndef());
        cbResDraw.setSelected(pattern.isResultDraw());
    }
}
