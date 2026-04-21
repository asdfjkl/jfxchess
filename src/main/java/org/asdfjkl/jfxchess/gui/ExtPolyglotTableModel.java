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

import org.asdfjkl.jfxchess.lib.PolyglotExtEntry;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class ExtPolyglotTableModel extends AbstractTableModel {

    private final String[] columns = {
            "Move", "Count", "Win/Draw/Loss", "Elo"
    };

    private List<PolyglotExtEntry> data;

    public ExtPolyglotTableModel(List<PolyglotExtEntry> data) {
        this.data = data;
    }

    public void setData(List<PolyglotExtEntry> data) {
        this.data = data;
        fireTableDataChanged();
    }

    public PolyglotExtEntry getEntry(int row) {
        return data.get(row);
    }

    @Override
    public int getRowCount() {
        return data == null ? 0 : data.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case 0 -> String.class;   // Move
            case 1 -> Long.class;     // Count
            //case 2 -> Integer.class; // percentages
            case 3 -> Integer.class;  // Elo
            default -> Object.class;
        };
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        PolyglotExtEntry entry = data.get(rowIndex);

        return switch (columnIndex) {
            case 0 -> entry.getMove();
            case 1 -> entry.getPosCount();
            case 2 -> entry;
            case 3 -> entry.getAvgELO();
            default -> null;
        };
    }
}