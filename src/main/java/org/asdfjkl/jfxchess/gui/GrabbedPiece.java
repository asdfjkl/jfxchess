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

import java.awt.Point;
import org.asdfjkl.jfxchess.lib.CONSTANTS;

public class GrabbedPiece {
    public static enum GrabbedFrom {
        Board, PieceSelector
    };

    private final Point sourceSquare;
    private double currentXLocation;
    private double currentYLocation;
    private int pieceType;
    private int rightClickPieceType;
    private boolean drawImage;
    GrabbedFrom grabbedFrom;

    public GrabbedPiece() {
        sourceSquare = new Point(-1, -1);
        pieceType = CONSTANTS.EMPTY;
        rightClickPieceType = CONSTANTS.EMPTY;
        drawImage = false;
        grabbedFrom = GrabbedFrom.Board;
    }

    // Piece is grabbed in the board
    void grab(int boardX,
              int boardY,
              double currentXLocation,
              double currentYLocation,
              int pieceType) {
        sourceSquare.x = (boardX);
        sourceSquare.y = boardY;
        this.currentXLocation = currentXLocation;
        this.currentYLocation = currentYLocation;
        this.pieceType = pieceType;
        rightClickPieceType = pieceType;
        drawImage = true;
        grabbedFrom = GrabbedFrom.Board;
    }

    // Piece is grabbed outside board, e.g. from the
    // pieceSelector-area of the DialogEnterPosition.
    void grab(double currentXLocation, double currentYLocation, int pieceType) {
        sourceSquare.x = -1;
        sourceSquare.y = -1;
        this.currentXLocation = currentXLocation;
        this.currentYLocation = currentYLocation;
        this.pieceType = pieceType;
        // selectionPieceType = pieceType;
        rightClickPieceType = pieceType;
        drawImage = true;
        grabbedFrom = GrabbedFrom.PieceSelector;
    }

    void reset() {
        sourceSquare.x = -1;
        sourceSquare.y = -1;
        pieceType = CONSTANTS.EMPTY;
        // Note that rightClickPieceType keeps its value.
        drawImage = false;
        grabbedFrom = GrabbedFrom.Board;
    }

    // Also resets rightClickPieceType.
    public void full_reset() {
        reset();
        rightClickPieceType = CONSTANTS.EMPTY;
    }

    public boolean isGrabbed() {
        return pieceType != CONSTANTS.EMPTY;
    }

    int sourceSquareX() {
        return sourceSquare.x;
    }

    int sourceSquareY() {
        return sourceSquare.y;
    }

    int rightClickPiece() {
        return rightClickPieceType;
    }

    boolean getDrawImage() {
        return drawImage;
    }

    public void setDrawImage(boolean b) {
        drawImage = b;
    }

    public void setCurrentXLocation(double currentXLocation) {
        this.currentXLocation = currentXLocation;
    }

    public void setCurrentYLocation(double currentYLocation) {
        this.currentYLocation = currentYLocation;
    }

    public void setRightClickPiece(int rCPieceType) {
        rightClickPieceType = rCPieceType;
    }

    public void setPiece(int piece) {
        this.pieceType = piece;
    }

    public int getPiece() {
        return this.pieceType;
    }

    public double getCurrentXLocation() {
        return this.currentXLocation;
    }

    public double getCurrentYLocation() {
        return this.currentYLocation;
    }
}
