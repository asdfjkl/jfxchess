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
import java.util.*;

public class DialogPlayBot extends JDialog {

    private JList<String> botList;
    private JLabel headerLabel;
    private JTextArea descriptionArea;
    private JLabel imageLabel;

    ArrayList<BotEngine> botEngines;

    private boolean playerColor = CONSTANTS.WHITE;
    private boolean playInitialPosition = true;
    private int botIndex = 0;
    private boolean confirmed = false;

    public DialogPlayBot(Frame parent, ArrayList<BotEngine> botEngines) {
        super(parent, "Choose Opponent", true);

        this.botEngines = botEngines;
        buildUI();

        setSize(870, 600);
        setLocationRelativeTo(parent);
    }

    private void buildUI() {

        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        // -------- LEFT COLUMN (JList) --------

        DefaultListModel<String> model = new DefaultListModel<>();
        for (BotEngine botEngine : botEngines) {
            model.addElement(botEngine.getName());
        }

        botList = new JList<>(model);
        botList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        botList.setSelectedIndex(0);

        JScrollPane listScroll = new JScrollPane(botList);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.2;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;

        // wider list view for even columns
        botList.setFixedCellWidth(200);
        listScroll.setPreferredSize(new Dimension(220, 0));
        listScroll.setMinimumSize(new Dimension(220, 0));

        mainPanel.add(listScroll, gbc);

        // -------- MIDDLE COLUMN --------

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

        middlePanel.add(radioPanel, BorderLayout.NORTH);

        // Lower part (description)
        JPanel descriptionPanel = new JPanel(new BorderLayout(5,5));

        headerLabel = new JLabel();
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 16f));

        descriptionArea = new JTextArea();
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setEditable(false);
        descriptionArea.setOpaque(false);
        descriptionArea.setFont(UIManager.getFont("Label.font"));

        descriptionPanel.add(headerLabel, BorderLayout.NORTH);
        descriptionPanel.add(descriptionArea, BorderLayout.CENTER);

        middlePanel.add(descriptionPanel, BorderLayout.CENTER);

        gbc.gridx = 1;
        gbc.weightx = 0.4;

        mainPanel.add(middlePanel, gbc);

        // -------- RIGHT COLUMN (IMAGE) --------

        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setPreferredSize(new Dimension(333, 500));

        gbc.gridx = 2;
        gbc.weightx = 0.4;

        mainPanel.add(imageLabel, gbc);

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

        botList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateOpponent(botList.getSelectedIndex());
                botIndex = botList.getSelectedIndex();
            }
        });

        updateOpponent(botList.getSelectedIndex());
    }

    private void updateOpponent(int index) {

        String name =  botEngines.get(index).getName();
        String elo = botEngines.get(index).getElo();
        String bio = botEngines.get(index).getBio();

        headerLabel.setText(name + " (" + elo + ")");
        descriptionArea.setText(bio);

        ImageIcon icon = new ImageIcon(botEngines.get(index).getImage());
        Image scaled = icon.getImage().getScaledInstance(333, 500, Image.SCALE_SMOOTH);
        imageLabel.setIcon(new ImageIcon(scaled));
    }

    public boolean getPlayerColor() {
        return playerColor;
    }

    public boolean getPlayInitialPosition() {
        return playInitialPosition;
    }

    public int getBotIndex() {
        return botIndex;
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}