/* JFXChess - A Chess Graphical User Interface
 * Copyright (C) 2026 Dominik Klein
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

package org.asdfjkl.jfxchess.lib;

import java.util.Arrays;

/**
 * Internal board representation for tracking dynamic piece lists during
 * SCID 5 move decoding and encoding.
 */
public class ScidPosition {

    // Piece type constants used in SCID5 encoding
    public static final int EMPTY = 0;
    public static final int KING = 1;
    public static final int QUEEN = 2;
    public static final int ROOK = 3;
    public static final int BISHOP = 4;
    public static final int KNIGHT = 5;
    public static final int PAWN = 6;

    public static final int WHITE = 0;
    public static final int BLACK = 1;

    public int[] board = new int[64];
    public int[] color = new int[64];
    public int[][] list = new int[2][16];
    public int[] listPos = new int[64];
    public int[] count = new int[2];
    public int toMove = WHITE;

    public ScidPosition() {
        Arrays.fill(board, EMPTY);
        Arrays.fill(color, -1);
        Arrays.fill(listPos, -1);
    }

    public void stdStart() {
        Arrays.fill(board, EMPTY);
        Arrays.fill(color, -1);
        Arrays.fill(listPos, -1);
        count[WHITE] = 0;
        count[BLACK] = 0;

        addPiece(KING, 4, WHITE);
        addPiece(ROOK, 0, WHITE);
        addPiece(KNIGHT, 1, WHITE);
        addPiece(BISHOP, 2, WHITE);
        addPiece(QUEEN, 3, WHITE);
        addPiece(BISHOP, 5, WHITE);
        addPiece(KNIGHT, 6, WHITE);
        addPiece(ROOK, 7, WHITE);
        for (int i = 0; i < 8; i++) {
            addPiece(PAWN, 8 + i, WHITE);
        }

        addPiece(KING, 60, BLACK);
        addPiece(ROOK, 56, BLACK);
        addPiece(KNIGHT, 57, BLACK);
        addPiece(BISHOP, 58, BLACK);
        addPiece(QUEEN, 59, BLACK);
        addPiece(BISHOP, 61, BLACK);
        addPiece(KNIGHT, 62, BLACK);
        addPiece(ROOK, 63, BLACK);
        for (int i = 0; i < 8; i++) {
            addPiece(PAWN, 48 + i, BLACK);
        }
        toMove = WHITE;
    }

    public void addPiece(int type, int sq, int c) {
        if (type == KING) {
            if (count[c] > 0) {
                int oldSq = list[c][0];
                list[c][count[c]] = oldSq;
                listPos[oldSq] = count[c];
            }
            list[c][0] = sq;
            listPos[sq] = 0;
        } else {
            listPos[sq] = count[c];
            list[c][count[c]] = sq;
        }
        board[sq] = type;
        color[sq] = c;
        count[c]++;
    }

    public void setupFen(String fen) {
        Arrays.fill(board, EMPTY);
        Arrays.fill(color, -1);
        Arrays.fill(listPos, -1);
        count[WHITE] = 0;
        count[BLACK] = 0;

        String[] parts = fen.split(" ");
        String ranks = parts[0];
        int r = 7;
        int f = 0;
        for (char c : ranks.toCharArray()) {
            if (c == '/') {
                r--;
                f = 0;
            } else if (Character.isDigit(c)) {
                f += (c - '0');
            } else {
                int sq = (r << 3) | f;
                int col = Character.isUpperCase(c) ? WHITE : BLACK;
                char lower = Character.toLowerCase(c);
                int type = switch (lower) {
                    case 'k' -> KING;
                    case 'q' -> QUEEN;
                    case 'r' -> ROOK;
                    case 'b' -> BISHOP;
                    case 'n' -> KNIGHT;
                    case 'p' -> PAWN;
                    default -> EMPTY;
                };
                addPiece(type, sq, col);
                f++;
            }
        }
        toMove = (parts.length > 1 && "b".equalsIgnoreCase(parts[1])) ? BLACK : WHITE;
    }

    public void doMove(int from, int to, int promo, boolean isCastleK, boolean isCastleQ) {
        int movingPiece = board[from];
        int c = toMove;
        int enemy = 1 - c;
        int pieceNum = listPos[from];

        if (isCastleK) {
            int rookFrom = (c == WHITE) ? 7 : 63;
            int rookTo = (c == WHITE) ? 5 : 61;
            int rookIdx = listPos[rookFrom];
            board[from] = EMPTY; color[from] = -1; listPos[from] = -1;
            board[rookFrom] = EMPTY; color[rookFrom] = -1; listPos[rookFrom] = -1;

            board[to] = KING; color[to] = c; list[c][0] = to; listPos[to] = 0;
            board[rookTo] = ROOK; color[rookTo] = c; list[c][rookIdx] = rookTo; listPos[rookTo] = rookIdx;
            toMove = enemy;
            return;
        }

        if (isCastleQ) {
            int rookFrom = (c == WHITE) ? 0 : 56;
            int rookTo = (c == WHITE) ? 3 : 59;
            int rookIdx = listPos[rookFrom];
            board[from] = EMPTY; color[from] = -1; listPos[from] = -1;
            board[rookFrom] = EMPTY; color[rookFrom] = -1; listPos[rookFrom] = -1;

            board[to] = KING; color[to] = c; list[c][0] = to; listPos[to] = 0;
            board[rookTo] = ROOK; color[rookTo] = c; list[c][rookIdx] = rookTo; listPos[rookTo] = rookIdx;
            toMove = enemy;
            return;
        }

        // Check capture
        int capturedSq = to;
        if (movingPiece == PAWN && board[to] == EMPTY && (from & 7) != (to & 7)) {
            // En passant capture
            capturedSq = (c == WHITE) ? (to - 8) : (to + 8);
        }

        if (board[capturedSq] != EMPTY) {
            int capturedNum = listPos[capturedSq];
            count[enemy]--;
            int lastSq = list[enemy][count[enemy]];
            listPos[lastSq] = capturedNum;
            list[enemy][capturedNum] = lastSq;
            board[capturedSq] = EMPTY;
            color[capturedSq] = -1;
            listPos[capturedSq] = -1;
        }

        board[from] = EMPTY;
        color[from] = -1;
        listPos[from] = -1;

        int newType = (promo != EMPTY) ? promo : movingPiece;
        board[to] = newType;
        color[to] = c;
        list[c][pieceNum] = to;
        listPos[to] = pieceNum;

        toMove = enemy;
    }

    public ScidPosition copy() {
        ScidPosition cp = new ScidPosition();
        System.arraycopy(this.board, 0, cp.board, 0, 64);
        System.arraycopy(this.color, 0, cp.color, 0, 64);
        System.arraycopy(this.listPos, 0, cp.listPos, 0, 64);
        System.arraycopy(this.list[0], 0, cp.list[0], 0, 16);
        System.arraycopy(this.list[1], 0, cp.list[1], 0, 16);
        cp.count[0] = this.count[0];
        cp.count[1] = this.count[1];
        cp.toMove = this.toMove;
        return cp;
    }
}
