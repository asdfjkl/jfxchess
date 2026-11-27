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

import org.asdfjkl.jfxchess.lib.Move;
import org.asdfjkl.jfxchess.lib.PolyglotExtEntry;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

public class View_Book extends JTable implements PropertyChangeListener {

    private static final String EVT_NODE_CHANGED = "currentGameNodeChanged";
    private static final String EVT_GAME_CHANGED = "gameChanged";
    private static final String EVT_BOOK_CHANGED = "bookChanged";

    private static final int COL_MOVE = 0;
    private static final int COL_COUNT = 1;
    private static final int COL_WDL = 2;
    private static final int COL_ELO = 3;
    private static final int COLUMN_COUNT = 4;

    private static final int BASE_ROW_HEIGHT = 28;
    private static final int ROW_V_PADDING = 8;

    private static final int MOVE_MIN_W = 55;
    private static final int MOVE_PREF_W = 72;
    private static final int MOVE_MAX_W = 160;

    private static final int COUNT_MIN_W = 55;
    private static final int COUNT_PREF_W = 75;
    private static final int COUNT_MAX_W = 160;

    private static final int WDL_MIN_W = 140;
    private static final int WDL_PREF_W = 260;

    private static final int ELO_MIN_W = 50;
    private static final int ELO_PREF_W = 65;
    private static final int ELO_MAX_W = 140;

    private final ExtPolyglotTableModel tableModel;
    private final Model_JFXChess model;
    private final GameSession gameSession;
    private final Controller_Board controller_Board;

    public View_Book(Model_JFXChess model,
                     GameSession gameSession,
                     Controller_Board controller_Board) {
        super();

        this.model = model;
        this.gameSession = gameSession;
        this.controller_Board = controller_Board;
        gameSession.addPropertyChangeListener(this);
        model.addListener(this);
        ArrayList<PolyglotExtEntry> data = new ArrayList<>();
        // Set model
        tableModel = new ExtPolyglotTableModel(data);

        setModel(tableModel);

        // Disable sorting
        setRowSorter(null);
        getTableHeader().setReorderingAllowed(false);

        // Appearance
        setFillsViewportHeight(true);

        // Optional: hand cursor
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        // Add click handling
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleClick(e);
            }
        });

        getColumnModel().getColumn(COL_WDL).setCellRenderer(new WinDrawLossRenderer());

        DefaultTableCellRenderer centerCellRenderer = new DefaultTableCellRenderer();
        centerCellRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        getColumnModel().getColumn(COL_COUNT).setCellRenderer(centerCellRenderer);
        getColumnModel().getColumn(COL_ELO).setCellRenderer(centerCellRenderer);

        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        applyHeaderRenderers();
        applyScaledLayout();

        refresh();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        applyScaledLayout();
    }

    // Wrap the LaF default header renderer so alignment never drops FlatLaf header styling.
    private void applyHeaderRenderers() {
        if (getColumnModel() == null || getColumnModel().getColumnCount() < COLUMN_COUNT) {
            return;
        }

        TableCellRenderer leadingHeader = buildHeaderRenderer(SwingConstants.LEADING);
        TableCellRenderer centerHeader = buildHeaderRenderer(SwingConstants.CENTER);

        getColumnModel().getColumn(COL_MOVE).setHeaderRenderer(leadingHeader);
        getColumnModel().getColumn(COL_COUNT).setHeaderRenderer(centerHeader);
        getColumnModel().getColumn(COL_WDL).setHeaderRenderer(leadingHeader);
        getColumnModel().getColumn(COL_ELO).setHeaderRenderer(centerHeader);
    }

    private TableCellRenderer buildHeaderRenderer(int alignment) {
        return (table, value, isSelected, hasFocus, row, column) -> {
            TableCellRenderer base = table.getTableHeader().getDefaultRenderer();
            Component comp = base.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column
            );
            if (comp instanceof JLabel label) {
                label.setHorizontalAlignment(alignment);
            }
            return comp;
        };
    }

    // Scale row height and column bounds with FlatLaf logical UI scale and font metrics.
    private void applyScaledLayout() {
        int scaledRow = HighDPIHelper.scaleUI(BASE_ROW_HEIGHT);
        if (getFont() != null) {
            int fontRow = getFontMetrics(getFont()).getHeight() + HighDPIHelper.scaleUI(ROW_V_PADDING);
            scaledRow = Math.max(scaledRow, fontRow);
        }
        setRowHeight(scaledRow);

        if (getColumnModel() == null || getColumnModel().getColumnCount() < COLUMN_COUNT) {
            return;
        }

        setColBounds(
                getColumnModel().getColumn(COL_MOVE),
                HighDPIHelper.scaleUI(MOVE_MIN_W),
                HighDPIHelper.scaleUI(MOVE_PREF_W),
                HighDPIHelper.scaleUI(MOVE_MAX_W)
        );
        setColBounds(
                getColumnModel().getColumn(COL_COUNT),
                HighDPIHelper.scaleUI(COUNT_MIN_W),
                HighDPIHelper.scaleUI(COUNT_PREF_W),
                HighDPIHelper.scaleUI(COUNT_MAX_W)
        );
        setColBounds(
                getColumnModel().getColumn(COL_WDL),
                HighDPIHelper.scaleUI(WDL_MIN_W),
                HighDPIHelper.scaleUI(WDL_PREF_W),
                Integer.MAX_VALUE
        );
        setColBounds(
                getColumnModel().getColumn(COL_ELO),
                HighDPIHelper.scaleUI(ELO_MIN_W),
                HighDPIHelper.scaleUI(ELO_PREF_W),
                HighDPIHelper.scaleUI(ELO_MAX_W)
        );
    }

    // Lift maxWidth first so resizing across scale changes never clamps min/preferred widths.
    private void setColBounds(TableColumn col, int minW, int prefW, int maxW) {
        col.setMaxWidth(Integer.MAX_VALUE);
        col.setMinWidth(minW);
        col.setPreferredWidth(prefW);
        col.setMaxWidth(maxW);
    }

    private void handleClick(MouseEvent e) {
        // Double-click (change to 1 if you prefer single-click)
        if (e.getClickCount() == 2) {

            int row = getSelectedRow();
            if (row >= 0) {

                int modelRow = convertRowIndexToModel(row);
                PolyglotExtEntry entry = tableModel.getEntry(modelRow);

                applyMove(entry.getMove());
            }
        }
    }

    private void applyMove(String move) {
        Move m = new Move(move);
        controller_Board.applyMove(m);
    }

    // Optional: access selected entry
    public PolyglotExtEntry getSelectedEntry() {
        int row = getSelectedRow();
        if (row >= 0) {
            return tableModel.getEntry(convertRowIndexToModel(row));
        }
        return null;
    }

    public void refresh() {
        tableModel.setData(model.findBookEntries(gameSession));
    }

    // Filter out model-level position events that belong to other tabs.
    private boolean isRelevantEvent(PropertyChangeEvent evt) {
        String name = evt.getPropertyName();

        if (EVT_BOOK_CHANGED.equals(name)) {
            return true;
        }

        boolean isPosEvent = EVT_NODE_CHANGED.equals(name)
                || EVT_GAME_CHANGED.equals(name);
        if (!isPosEvent) {
            return false;
        }

        if (evt.getSource() == model && model.getGameSession() != gameSession) {
            return false;
        }

        return true;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (!isRelevantEvent(evt)) {
            return;
        }

        refresh();
    }
}