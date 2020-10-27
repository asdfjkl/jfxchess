/* JerryFX - A Chess Graphical User Interface
 * Copyright (C) 2020 Dominik Klein
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

package org.asdfjkl.jerryfx.lib;

public class Move {

    int from;
    int to;
    int promotionPiece;
    boolean isNullMove;

    public Move(int from, int to) {

        this.from = from;
        this.to = to;
        this.promotionPiece = 0;
        this.isNullMove = false;

    }

    public Move(int fromColumn, int fromRow, int toColumn, int toRow) {

        this.from = ((fromRow + 2) * 10) + (fromColumn + 1);
        this.to = ((toRow + 2) * 10) + (toColumn + 1);
        this.promotionPiece = 0;
        this.isNullMove = false;

    }

    public Move() {

        this.from = 0;
        this.to = 0;
        this.promotionPiece = 0;
        this.isNullMove = true;

    }

    public Move(int from, int to, int promotionPiece) {

        this.from = from;
        this.to = to;
        this.promotionPiece = promotionPiece;

    }

    public Move(int fromColumn, int fromRow, int toColumn, int toRow, char promotionPiece) {

        //System.out.println("from col: "+fromColumn + " tocol: "+toColumn);
        //System.out.println("from row: "+fromRow + " torow: "+toRow);
        this.from = ((fromRow + 2) * 10) + (fromColumn + 1);
        this.to = ((toRow + 2) * 10) + (toColumn + 1);

        //System.out.println("intfrom: "+this.from);
        //System.out.println("intto: "+this.to);


        this.promotionPiece = -1;
        if (promotionPiece == 'N') {
            this.promotionPiece = CONSTANTS.KNIGHT;
        }
        if (promotionPiece == 'B') {
            this.promotionPiece = CONSTANTS.BISHOP;
        }
        if (promotionPiece == 'R') {
            this.promotionPiece = CONSTANTS.ROOK;
        }
        if (promotionPiece == 'Q') {
            this.promotionPiece = CONSTANTS.QUEEN;
        }
        if (this.promotionPiece < 0) {
            throw new IllegalArgumentException("Illegal Promotion Piece: " + promotionPiece);
        }
        this.isNullMove = false;

    }

    public void setPromotionPiece(int promotionPiece) {
        this.promotionPiece = promotionPiece;
    }

    private int alphaToPos(char alpha) {
        if (alpha == 'A') {
            return 1;
        } else if (alpha == 'B') {
            return 2;
        } else if (alpha == 'C') {
            return 3;
        } else if (alpha == 'D') {
            return 4;
        } else if (alpha == 'E') {
            return 5;
        } else if (alpha == 'F') {
            return 6;
        } else if (alpha == 'G') {
            return 7;
        } else if (alpha == 'H') {
            return 8;
        }
        throw new IllegalArgumentException("alpha to pos called with: " + alpha);
    }


    public Move(String uci) {

        if (!(uci.length() == 4 || uci.length() == 5)) {
            throw new IllegalArgumentException("Illegal Uci String: " + uci);
        }
        String uciUpper = uci.toUpperCase();

        int fromColumn = this.alphaToPos(uciUpper.charAt(0));
        // -49 is for ascii(1) -> int 0, * 10 + 20 is to get internal board coordinate
        int fromRow = ((((int) uciUpper.charAt(1)) - 49) * 10) + 20;
        this.from = fromRow + fromColumn;

        int toColumn = this.alphaToPos(uciUpper.charAt(2));
        int toRow = ((((int) uciUpper.charAt(3)) - 49) * 10) + 20;
        this.to = toRow + toColumn;

        if (uciUpper.length() == 5) {
            char promPiece = uciUpper.charAt(4);
            this.promotionPiece = -1;
            if (promPiece == 'N') {
                this.promotionPiece = CONSTANTS.KNIGHT;
            }
            if (promPiece == 'B') {
                this.promotionPiece = CONSTANTS.BISHOP;
            }
            if (promPiece == 'R') {
                this.promotionPiece = CONSTANTS.ROOK;
            }
            if (promPiece == 'Q') {
                this.promotionPiece = CONSTANTS.QUEEN;
            }
            if (promotionPiece < 0) {
                throw new IllegalArgumentException("illegal uci string: " + uci);
            }
        }
        this.isNullMove = false;

    }

    public String getUci() {

        if (this.isNullMove) {
            return "0000";
        } else {
            char colFrom = (char) ((this.from % 10) + 96);
            char rowFrom = (char) ((this.from / 10) + 47);

            //System.out.println(("from: "+this.from));

            char colTo = (char) ((this.to % 10) + 96);
            char rowTo = (char) ((this.to / 10) + 47);

            //System.out.println(("to: "+this.to));

            String uci = "";
            uci += colFrom;
            uci += rowFrom;
            uci += colTo;
            uci += rowTo;
            if (this.promotionPiece == CONSTANTS.KNIGHT) {
                uci += "N";
            }
            if (this.promotionPiece == CONSTANTS.ROOK) {
                uci += "R";
            }
            if (this.promotionPiece == CONSTANTS.QUEEN) {
                uci += "Q";
            }
            if (this.promotionPiece == CONSTANTS.BISHOP) {
                uci += "B";
            }
            return uci;
        }
    }

    @Override
    public String toString() {
        return this.getUci();
    }


}