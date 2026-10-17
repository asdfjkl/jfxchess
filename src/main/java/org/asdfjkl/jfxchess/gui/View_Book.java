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
import javax.swing.table.TableColumn;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

public class View_Book extends JTable implements PropertyChangeListener {

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

        getColumnModel().getColumn(2).setCellRenderer(new WinDrawLossRenderer());

        double scale = HighDPIHelper.getUIScaleFactor();
        setRowHeight((int) Math.round(28 * scale));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        // total count is centered
        getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        getColumnModel().getColumn(1).setHeaderRenderer(centerRenderer);
        // average elo centered
        getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        getColumnModel().getColumn(3).setHeaderRenderer(centerRenderer);

        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        // Move column: compact minimum and preferred width, does not expand further
        TableColumn colMove = getColumnModel().getColumn(0);
        colMove.setMinWidth((int) Math.round(50 * scale));
        colMove.setPreferredWidth((int) Math.round(65 * scale));
        colMove.setMaxWidth((int) Math.round(65 * scale));

        // Count column: compact minimum and preferred width, does not expand further
        TableColumn colCount = getColumnModel().getColumn(1);
        colCount.setMinWidth((int) Math.round(50 * scale));
        colCount.setPreferredWidth((int) Math.round(65 * scale));
        colCount.setMaxWidth((int) Math.round(65 * scale));

        // Win/Draw/Loss column: expands to absorb remaining table width
        TableColumn colWdl = getColumnModel().getColumn(2);
        colWdl.setMinWidth((int) Math.round(140 * scale));
        colWdl.setPreferredWidth((int) Math.round(260 * scale));
        colWdl.setMaxWidth(Integer.MAX_VALUE);

        // Elo column: compact minimum and preferred width, does not expand further
        TableColumn colElo = getColumnModel().getColumn(3);
        colElo.setMinWidth((int) Math.round(50 * scale));
        colElo.setPreferredWidth((int) Math.round(60 * scale));
        colElo.setMaxWidth((int) Math.round(60 * scale));


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

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("currentGameNodeChanged".equals(evt.getPropertyName())
                || "gameChanged".equals(evt.getPropertyName())
                || "bookChanged".equals(evt.getPropertyName()))
        {
            ArrayList<PolyglotExtEntry> moves = model.extBook.findEntries(
                    gameSession.getGame().getCurrentNode().getBoard());
            tableModel.setData(moves);

        }
    }
}