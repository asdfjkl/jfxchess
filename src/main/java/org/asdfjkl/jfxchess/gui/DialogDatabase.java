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
import org.asdfjkl.jfxchess.lib.SearchPattern;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DialogDatabase extends JDialog {

    private JTable table;
    private final GameTableModel tableModel;

    private final Controller_Pgn controller_Pgn;
    private final ChessDatabase database;

    private JButton btnReset;
    private boolean isConfirmed = false;

    private SearchPattern pattern = new SearchPattern();

    public DialogDatabase(Frame owner,
                          ChessDatabase database,
                          Controller_Pgn controller) {
        this(owner, database, controller, null);
    }

    public DialogDatabase(Frame owner,
                          ChessDatabase database,
                          Controller_Pgn controller,
                          GameInfo activeGame) {
        super(owner, getDatabaseTitle(database), true);

        this.controller_Pgn = controller;
        this.database = database;

        if (database != null && database.isSearchActive()) {
            this.tableModel = new GameTableModel(database.getSearchResults());
        } else if (database != null) {
            this.tableModel = new GameTableModel(database.getIndex());
        } else {
            this.tableModel = new GameTableModel(new ArrayList<>());
        }

        initUI();

        if (database != null && !database.isSearchActive() && activeGame != null) {
            int idx = database.indexOf(activeGame);
            if (idx >= 0 && idx < database.getIndex().size()) {
                this.table.setRowSelectionInterval(idx, idx);
            }
        }

        setSize(900, 600);
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // ===== TABLE =====
        table = new JTable(tableModel);
        table.setAutoCreateRowSorter(false);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // ===== BUTTONS =====
        JPanel bottomPanel = new JPanel(new BorderLayout());

        // Left buttons
        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnSearch = new JButton("Search");
        btnReset = new JButton("Reset Search");
        JButton btnDelete = new JButton("Delete Game");

        leftButtons.add(btnSearch);
        leftButtons.add(btnReset);
        leftButtons.add(btnDelete);

        // Right buttons
        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnOpen = new JButton("Open Game");
        JButton btnCancel = new JButton("Cancel");

        rightButtons.add(btnOpen);
        rightButtons.add(btnCancel);

        bottomPanel.add(leftButtons, BorderLayout.WEST);
        bottomPanel.add(rightButtons, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        btnCancel.addActionListener(e -> dispose());
        btnOpen.addActionListener(e -> {
            isConfirmed = true;
            dispose();
        });

        btnOpen.setEnabled(false);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean rowSelected = table.getSelectedRow() != -1;
                btnOpen.setEnabled(rowSelected);
            }
        });

        if (database != null && database.isSearchActive()) {
            btnReset.setEnabled(true);
        } else {
            btnReset.setEnabled(false);
        }

        btnDelete.addActionListener(e -> deleteSelectedGame());
        btnReset.addActionListener(e -> resetSearch());
        btnSearch.addActionListener(e -> onBtnSearch());
    }

    // ===== TABLE MODEL =====
    private static class GameTableModel extends AbstractTableModel {

        private final String[] columns = {
                "No", "White", "Elo", "Black", "Elo", "Result", "Event", "Date"
        };

        private List<GameInfo> games;

        public GameTableModel(List<GameInfo> games) {
            this.games = games != null ? new ArrayList<>(games) : new ArrayList<>();
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
                case 0: return row + 1;
                case 1: return g.getWhite();
                case 2: return g.getWhiteElo();
                case 3: return g.getBlack();
                case 4: return g.getBlackElo();
                case 5: return g.getResult();
                case 6: return g.getEvent();
                case 7: return g.getDate();
                default: return "";
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return Integer.class;
                default:
                    return String.class;
            }
        }

        public GameInfo getGameAt(int row) {
            return games.get(row);
        }

        public void removeRow(int row) {
            games.remove(row);
            fireTableRowsDeleted(row, row);
        }

        public void setData(List<GameInfo> newEntries) {
            this.games = newEntries != null ? new ArrayList<>(newEntries) : new ArrayList<>();
            fireTableDataChanged();
        }
    }

    // ===== ACCESS HELPERS =====
    public GameInfo getSelectedGame() {
        int row = table.getSelectedRow();
        if (row < 0) return null;

        int modelRow = table.convertRowIndexToModel(row);
        return tableModel.getGameAt(modelRow);
    }

    public int getIndexOfSelectedGame() {
        int row = table.getSelectedRow();
        if (row < 0) return -1;

        return table.convertRowIndexToModel(row);
    }

    public void deleteSelectedGame() {
        int row = table.getSelectedRow();
        if (row < 0) return;

        int modelRow = table.convertRowIndexToModel(row);
        GameInfo gameInfo = tableModel.getGameAt(modelRow);
        int result = JOptionPane.showConfirmDialog(this,
                "Deleting '" +
                        gameInfo.getWhite() + " vs. " + gameInfo.getBlack() +
                        "', please confirm",
                "Confirm Deletion",
                JOptionPane.OK_CANCEL_OPTION
        );
        if (result == JOptionPane.OK_OPTION) {
            controller_Pgn.deleteGame(gameInfo);
            tableModel.removeRow(modelRow);
        }
    }

    private void onBtnSearch() {
        DialogSearchGames dlgSearch = new DialogSearchGames(this, pattern);
        dlgSearch.setVisible(true);
        pattern = dlgSearch.getSearchPattern();
        if (dlgSearch.isConfirmed()) {
            searchGames(pattern);
        }
    }

    private void searchGames(SearchPattern pattern) {
        if (database == null) {
            return;
        }
        PgnSearchWorker worker = new PgnSearchWorker(database, pattern,
                entriesFromWorker -> {
                    tableModel.setData(database.getSearchResults());
                    btnReset.setEnabled(true);
                    invalidate();
                }
        );
        DialogProgress dlgProgress = new DialogProgress(this, worker, "Searching Games");
        worker.execute();
        dlgProgress.setVisible(true);
    }

    private void resetSearch() {
        if (database == null) {
            return;
        }
        database.resetSearch();
        tableModel.setData(database.getIndex());
        btnReset.setEnabled(false);
    }

    public boolean isConfirmed() {
        return isConfirmed;
    }

    private static String getDatabaseTitle(ChessDatabase database) {
        if (database != null && database.getFilename() != null && !database.getFilename().isBlank()) {
            return "Database - " + new java.io.File(database.getFilename()).getName();
        }
        return "Database";
    }
}
