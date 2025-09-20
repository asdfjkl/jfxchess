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

import javafx.scene.paint.Color;

public class BoardStyle {

    public static final int STYLE_BLUE = 0;
    public static final int STYLE_BROWN = 1;
    public static final int STYLE_GREEN = 2;

    public static final int PIECE_STYLE_MERIDA = 0;
    public static final int PIECE_STYLE_OLD = 1;
    public static final int PIECE_STYLE_USCF = 2;

    public static final Color BORDER_BLUE = Color.rgb(56,66,91);
    public static final Color DARK_SQUARE_BLUE = Color.rgb(90,106,173);
    public static final Color LIGHT_SQUARE_BLUE = Color.rgb(166,188,231);
    public static final Color COORDINATE_COLOR_BLUE = Color.rgb(239,239,239);

    public static final Color BORDER_GREEN = Color.rgb(72,81,63);
    public static final Color DARK_SQUARE_GREEN = Color.rgb(118,150,86);
    public static final Color LIGHT_SQUARE_GREEN = Color.rgb(238,238,210);
    public static final Color COORDINATE_COLOR_GREEN = Color.rgb(239,239,239);

    public static final Color BORDER_BROWN = Color.rgb(107,80,57);
    public static final Color DARK_SQUARE_BROWN = Color.rgb(181,136,99);
    public static final Color LIGHT_SQUARE_BROWN = Color.rgb(240,217,181);
    public static final Color COORDINATE_COLOR_BROWN = Color.rgb(239,239,239);

    private Color borderColor;
    private Color coordinateColor;
    private Color darkSquareColor;
    private Color lightSquareColor;

    private int pieceStyle;

    public void setColorStyle(int colorStyle) {

        if(colorStyle == STYLE_BLUE) {
            borderColor = BORDER_BLUE;
            coordinateColor = COORDINATE_COLOR_BLUE;
            darkSquareColor = DARK_SQUARE_BLUE;
            lightSquareColor = LIGHT_SQUARE_BLUE;
        }
        if(colorStyle == STYLE_GREEN) {
            borderColor = BORDER_GREEN;
            coordinateColor = COORDINATE_COLOR_GREEN;
            darkSquareColor = DARK_SQUARE_GREEN;
            lightSquareColor = LIGHT_SQUARE_GREEN;
        }
        if(colorStyle == STYLE_BROWN) {
            borderColor = BORDER_BROWN;
            coordinateColor = COORDINATE_COLOR_BROWN;
            darkSquareColor = DARK_SQUARE_BROWN;
            lightSquareColor = LIGHT_SQUARE_BROWN;
        }

    }

    public void setPieceStyle(int pieceStyle) {

        if(pieceStyle >= 0 && pieceStyle <= 2) {
            this.pieceStyle = pieceStyle;
        }
    }

    public BoardStyle() {

        borderColor = BORDER_BLUE;
        coordinateColor = COORDINATE_COLOR_BLUE;
        darkSquareColor = DARK_SQUARE_BLUE;
        lightSquareColor = LIGHT_SQUARE_BLUE;

        pieceStyle = PIECE_STYLE_MERIDA;

    }

    public Color getBorderColor() {
        return this.borderColor;
    }

    public Color getCoordinateColor() {
        return this.coordinateColor;
    }

    public Color getDarkSquareColor() {
        return this.darkSquareColor;
    }

    public Color getLightSquareColor() {
        return this.lightSquareColor;
    }

    public int getPieceStyle() { return this.pieceStyle; }

    public int getColorStyle() {

        if (borderColor == BORDER_BLUE) {
            return STYLE_BLUE;
        }
        if (borderColor == BORDER_BROWN) {
            return STYLE_BROWN;
        }
        return STYLE_GREEN;
    }

}
