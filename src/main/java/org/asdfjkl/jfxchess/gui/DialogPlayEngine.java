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

import org.asdfjkl.jfxchess.lib.CONSTANTS;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class DialogPlayEngine extends JDialog {

    private JList<Engine> engineList;
    private JSlider eloSlider;

    ArrayList<Engine> uciEngines;

    private boolean playerColor = CONSTANTS.WHITE;
    private boolean playInitialPosition = true;
    private int selectedEngineIdx = 0;
    private boolean confirmed = false;
    private int elo = -1;

    public DialogPlayEngine(Frame parent, ArrayList<Engine> engines) {
        super(parent, "Choose Engine", true);

        this.uciEngines = engines;
        buildUI();

        setSize(480, 500);
        setLocationRelativeTo(parent);
    }

    private void buildUI() {

        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        // -------- LEFT COLUMN (JList) --------

        DefaultListModel<Engine> model = new DefaultListModel<>();
        for (Engine engine : uciEngines) {
            model.addElement(engine);
        }

        engineList = new JList<>(model);
        engineList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        engineList.setSelectedIndex(0);

        JScrollPane listScroll = new JScrollPane(engineList);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;

        // wider list view for even columns
        engineList.setFixedCellWidth(160);
        listScroll.setPreferredSize(new Dimension(180, 0));
        listScroll.setMinimumSize(new Dimension(180, 0));

        mainPanel.add(listScroll, gbc);

        // -------- RIGHT COLUMN --------

        JPanel middlePanel = new JPanel(new BorderLayout(10,10));

        // Upper part (radio buttons)
        JPanel radioPanel = new JPanel();
        radioPanel.setLayout(new BoxLayout(radioPanel, BoxLayout.Y_AXIS));

        // Group 1
        JPanel sidePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sidePanel.setBorder(BorderFactory.createTitledBorder("Choose your side"));

        JRadioButton white = new JRadioButton("White", true);
        JRadioButton black = new JRadioButton("Black");

        white.addActionListener(e -> {
            playerColor = CONSTANTS.WHITE;
        });
        black.addActionListener(e -> {
            playerColor = CONSTANTS.BLACK;
        });

        ButtonGroup sideGroup = new ButtonGroup();
        sideGroup.add(white);
        sideGroup.add(black);

        sidePanel.add(white);
        sidePanel.add(black);

        // Group 2
        JPanel startPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startPanel.setBorder(BorderFactory.createTitledBorder("Start from"));

        JRadioButton initial = new JRadioButton("Initial Position", true);
        JRadioButton current = new JRadioButton("Current Position");

        initial.addActionListener(e -> {
            playInitialPosition = true;
        });
        current.addActionListener(e -> {
            playInitialPosition = false;
        });

        ButtonGroup startGroup = new ButtonGroup();
        startGroup.add(initial);
        startGroup.add(current);

        startPanel.add(initial);
        startPanel.add(current);

        radioPanel.add(sidePanel);
        radioPanel.add(startPanel);

        // Create slider
        Engine internalEngine = uciEngines.getFirst();
        eloSlider = new JSlider(JSlider.HORIZONTAL, internalEngine.getMinUciElo(),
                internalEngine.getMaxUciElo(), internalEngine.getUciElo());
        eloSlider.setMajorTickSpacing(200);
        eloSlider.setMinorTickSpacing(10);
        eloSlider.setPaintTicks(true);
        eloSlider.setPaintLabels(false);
        eloSlider.setSnapToTicks(true);
        JLabel eloValueLabel = new JLabel("Elo " + internalEngine.getUciElo());
        eloValueLabel.setPreferredSize(new Dimension(50, 20));
        // Listen for slider changes
        eloSlider.addChangeListener(e -> {
            int value = eloSlider.getValue();
            eloValueLabel.setText("Elo "+value);
            elo = value;
        });

        // Panel to visually group components ("UCI ELO")
        JPanel uciPanel = new JPanel();
        uciPanel.setBorder(BorderFactory.createTitledBorder("Strength"));
        uciPanel.setLayout(new BorderLayout());

        uciPanel.add(eloSlider, BorderLayout.CENTER);
        uciPanel.add(eloValueLabel, BorderLayout.EAST);

        radioPanel.add(uciPanel, BorderLayout.NORTH);

        middlePanel.add(radioPanel, BorderLayout.NORTH);

        gbc.gridx = 1;
        gbc.weightx = 0.9;

        mainPanel.add(middlePanel, gbc);

        add(mainPanel, BorderLayout.CENTER);

        // -------- BUTTONS --------

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");

        ok.addActionListener(e -> {
            confirmed = true;
        });

        cancel.addActionListener(e -> dispose());
        ok.addActionListener(e -> {
            dispose();
        });

        buttonPanel.add(ok);
        buttonPanel.add(cancel);

        add(buttonPanel, BorderLayout.SOUTH);

        // -------- LISTENER --------

        engineList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateOpponent(engineList.getSelectedIndex());

            }
        });

        updateOpponent(engineList.getSelectedIndex());

        // select default engine
        engineList.setSelectedIndex(0);
    }

    private void updateOpponent(int index) {

        selectedEngineIdx = index;
        Engine selectedEngine = engineList.getSelectedValue();
        if(!selectedEngine.supportsMultiPV()) {
            eloSlider.setEnabled(false);
        } else {
            eloSlider.setEnabled(true);
            eloSlider.setMinimum(selectedEngine.getMinUciElo());
            eloSlider.setMaximum(selectedEngine.getMaxUciElo());
            eloSlider.setValue(selectedEngine.getUciElo());
        }
    }

    public boolean getPlayerColor() {
        return playerColor;
    }

    public boolean getPlayInitialPosition() {
        return playInitialPosition;
    }

    public int getSelectedEngineIdx() {
        return selectedEngineIdx;
    }

    public int getElo() {
        return elo;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

}
