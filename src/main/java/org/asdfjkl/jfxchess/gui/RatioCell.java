/* JFXChess - A Chess Graphical User Interface
 * Copyright (C) 2020-2025 Dominik Klein
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

import javafx.scene.control.TableCell;
import org.asdfjkl.jfxchess.lib.PolyglotExtEntry;

public class RatioCell extends TableCell<PolyglotExtEntry, PolyglotExtEntry> {

    BarchartWDL barchartWDL = null;


    public RatioCell(int theme) {
        super();
        barchartWDL = new BarchartWDL();
        barchartWDL.setTheme(theme);
    }

    @Override
    protected void updateItem(PolyglotExtEntry item, boolean empty) {
        super.updateItem(item, empty);

        if(empty || (item == null)) {
            setText(null);
            setGraphic(null);
        } else {
            barchartWDL.setWDLRatio(item);
            setGraphic(barchartWDL);
        }
    }

}