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

import org.asdfjkl.jfxchess.lib.ChessDatabase;
import org.asdfjkl.jfxchess.lib.GameInfo;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DialogImportGames extends JDialog {

    private JTable table;
    private final GameTableModel tableModel;

    private final ChessDatabase sourceDatabase;
    private final ChessDatabase targetDatabase;

    private boolean isConfirmed = false;
    private final ArrayList<GameInfo> selectedGames = new ArrayList<>();

    public DialogImportGames(Frame owner,
                             ChessDatabase sourceDatabase,
                             ChessDatabase targetDatabase) {
        super(owner, getDialogTitle(sourceDatabase), true);

        this.sourceDatabase = sourceDatabase;
        this.targetDatabase = targetDatabase;

        if (sourceDatabase != null) {
            this.tableModel = new GameTableModel(sourceDatabase.getIndex());
        } else {
            this.tableModel = new GameTableModel(new ArrayList<>());
        }

        initUI();

        setSize(900, 600);
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // ===== TABLE =====
        table = new JTable(tableModel);
        table.setAutoCreateRowSorter(false);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // ===== BUTTONS =====
        JPanel bottomPanel = new JPanel(new BorderLayout());

        // Left buttons
        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnSelectAll = new JButton("Select All");
        JButton btnClearSelection = new JButton("Clear Selection");

        leftButtons.add(btnSelectAll);
        leftButtons.add(btnClearSelection);

        // Right buttons
        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnImport = new JButton("Import");
        JButton btnCancel = new JButton("Cancel");

        rightButtons.add(btnImport);
        rightButtons.add(btnCancel);

        bottomPanel.add(leftButtons, BorderLayout.WEST);
        bottomPanel.add(rightButtons, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        btnImport.setEnabled(false);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = table.getSelectedRowCount() > 0;
                btnImport.setEnabled(hasSelection);
            }
        });

        btnSelectAll.addActionListener(e -> {
            if (table.getRowCount() > 0) {
                table.selectAll();
            }
        });

        btnClearSelection.addActionListener(e -> {
            table.clearSelection();
        });

        btnCancel.addActionListener(e -> {
            isConfirmed = false;
            dispose();
        });

        btnImport.addActionListener(e -> {
            onBtnImport();
        });
    }

    private void onBtnImport() {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length == 0) {
            return;
        }

        int selectedCount = selectedRows.length;
        int totalCount = tableModel.getRowCount();
        String sourceName = extractFileName(sourceDatabase);
        String targetName = extractFileName(targetDatabase);

        String message = "Importing " + selectedCount + "/" + totalCount +
                " games from " + sourceName + " to " + targetName;

        int result = JOptionPane.showConfirmDialog(
                this,
                message,
                "Confirm Import",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (result == JOptionPane.OK_OPTION) {
            selectedGames.clear();
            for (int viewRow : selectedRows) {
                int modelRow = table.convertRowIndexToModel(viewRow);
                selectedGames.add(tableModel.getGameAt(modelRow));
            }
            isConfirmed = true;
            dispose();
        }
    }

    public boolean isConfirmed() {
        return isConfirmed;
    }

    public ArrayList<GameInfo> getSelectedGames() {
        return new ArrayList<>(selectedGames);
    }

    private static String extractFileName(ChessDatabase database) {
        if (database != null && database.getFilename() != null && !database.getFilename().isBlank()) {
            return new File(database.getFilename()).getName();
        }
        return "";
    }

    private static String getDialogTitle(ChessDatabase database) {
        String name = extractFileName(database);
        if (!name.isEmpty()) {
            return "Import Games - " + name;
        }
        return "Import Games";
    }

    // ===== TABLE MODEL =====
    private static class GameTableModel extends AbstractTableModel {

        private final String[] columns = {
                "No", "White", "Elo", "Black", "Elo", "Result", "Event", "Date"
        };

        private final List<GameInfo> games;

        public GameTableModel(List<GameInfo> games) {
            if (games != null) {
                this.games = new ArrayList<>(games);
            } else {
                this.games = new ArrayList<>();
            }
        }

        @Override
        public int getRowCount() {
            return games.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int col) {
            return columns[col];
        }

        @Override
        public Object getValueAt(int row, int col) {
            GameInfo g = games.get(row);

            switch (col) {
                case 0: {
                    return row + 1;
                }
                case 1: {
                    return g.getWhite();
                }
                case 2: {
                    return g.getWhiteElo();
                }
                case 3: {
                    return g.getBlack();
                }
                case 4: {
                    return g.getBlackElo();
                }
                case 5: {
                    return g.getResult();
                }
                case 6: {
                    return g.getEvent();
                }
                case 7: {
                    return g.getDate();
                }
                default: {
                    return "";
                }
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0: {
                    return Integer.class;
                }
                default: {
                    return String.class;
                }
            }
        }

        public GameInfo getGameAt(int row) {
            return games.get(row);
        }
    }
}
