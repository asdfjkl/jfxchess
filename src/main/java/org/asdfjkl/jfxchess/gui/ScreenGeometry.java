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

public class ScreenGeometry {

    double xOffset;
    double yOffset;
    double width;
    double height;
    double moveDividerRatio;
    double mainDividerRatio;
    final static double DEFAULT_MOVE_DIVIDER_RATIO = 0.5;
    final static double DEFAULT_MAIN_DIVIDER_RATIO = 0.7;
    final static double DEFAULT_WIDTH_RATIO = 0.8;
    final static double DEFAULT_HEIGHT_RATIO = 0.8;

    public ScreenGeometry(double xOffset, double yOffset, double width, double height, double moveDividerRatio, double mainDividerRatio) {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.width = width;
        this.height = height;
        this.moveDividerRatio = moveDividerRatio;
        this.mainDividerRatio = mainDividerRatio;
    }

    public boolean isValid() {
        return (xOffset >= 0.0 && yOffset >= 0.0 && width > 100 && height > 100 && moveDividerRatio > 0.1 && mainDividerRatio > 0.1);
    }

}
