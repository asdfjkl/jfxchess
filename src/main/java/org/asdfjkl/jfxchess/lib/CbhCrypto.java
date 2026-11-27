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

package org.asdfjkl.jfxchess.lib;

import java.util.Arrays;

/**
 * ChessBase classic `.cbg` cipher opcode tables, inverse S-box, piece roster
 * tracking, and 28-byte custom setup decoder.
 */
final class CbhCrypto {

    static final int OP_UNMAPPED = 0;
    static final int OP_PUSH = 1;
    static final int OP_POP = 2;
    static final int OP_SKIP = 3;
    static final int OP_ESCAPE = 4;
    static final int OP_NULL = 5;
    static final int OP_CASTLE_K = 6;
    static final int OP_CASTLE_Q = 7;
    static final int OP_MOVE = 8;

    static final int ROSTER_KING = 0;
    static final int ROSTER_QUEEN = 1;
    static final int ROSTER_ROOK = 2;
    static final int ROSTER_BISHOP = 3;
    static final int ROSTER_KNIGHT = 4;
    static final int ROSTER_PAWN = 5;

    static final int[] OP_KIND = new int[256];
    static final int[] OP_PIECE_TYPE = new int[256];
    static final int[] OP_SLOT = new int[256];
    static final int[] OP_DX = new int[256];
    static final int[] OP_DY = new int[256];

    static final int[] SBOX_INV = new int[256];
    static final int[] SBOX_FWD = new int[256];
    private static final int[][][][] MOVE_ENCODE_TABLE = new int[6][8][8][8];

    private static final int[] KNIGHT_DELTAS = {
        2, 1, 1, 2, -1, 2, -2, 1, -2, -1, -1, -2, 1, -2, 2, -1
    };

    static {
        Arrays.fill(OP_KIND, OP_UNMAPPED);
        Arrays.fill(SBOX_INV, -1);
        Arrays.fill(SBOX_FWD, -1);
        for (int t = 0; t < 6; t++) {
            for (int s = 0; s < 8; s++) {
                for (int dx = 0; dx < 8; dx++) {
                    Arrays.fill(MOVE_ENCODE_TABLE[t][s][dx], -1);
                }
            }
        }

        OP_KIND[0xDC] = OP_PUSH;
        OP_KIND[0x0C] = OP_POP;
        OP_KIND[0x9F] = OP_SKIP;
        OP_KIND[0x29] = OP_ESCAPE;
        OP_KIND[0xAA] = OP_NULL;
        OP_KIND[0x76] = OP_CASTLE_K;
        OP_KIND[0xB5] = OP_CASTLE_Q;

        // King steps (8)
        int[] kOps = {0x49, 0x39, 0xD8, 0x5D, 0xC2, 0xB1, 0xB2, 0x47};
        int[] kDx = {0, 1, 1, 1, 0, -1, -1, -1};
        int[] kDy = {1, 1, 0, -1, -1, -1, 0, 1};
        for (int i = 0; i < 8; i++) {
            reg(kOps[i], ROSTER_KING, 0, kDx[i], kDy[i]);
        }

        // Q1 (28)
        int[] q1Up = {0xA5, 0xB8, 0xCB, 0x53, 0x7F, 0x6B, 0x8D};
        int[] q1Right = {0x79, 0xBE, 0xEB, 0x21, 0x99, 0xD2, 0x57};
        int[] q1DiagUp = {0x4D, 0xB4, 0xBF, 0x62, 0xBD, 0x24, 0x96};
        int[] q1DiagDn = {0xA7, 0x48, 0x28, 0x6E, 0x2F, 0x5A, 0x18};
        for (int s = 1; s <= 7; s++) {
            reg(q1Up[s - 1], ROSTER_QUEEN, 0, 0, s);
            reg(q1Right[s - 1], ROSTER_QUEEN, 0, s, 0);
            reg(q1DiagUp[s - 1], ROSTER_QUEEN, 0, s, s);
            reg(q1DiagDn[s - 1], ROSTER_QUEEN, 0, s, -s);
        }

        // Q2 (27 unique mod-8 wrapped vectors)
        int[] q2Up = {0xE5, 0x94, 0x50, 0x11, 0xEA, 0x31, 0x01};
        int[] q2Right = {0x5C, 0x95, 0xCA, 0xD3, 0x1D, 0x7E, 0xEF};
        int[] q2DiagUp = {0x44, 0x80, 0xA0, 0x1F, 0x83, 0x00, 0x4B};
        int[] q2DiagDn = {0x67, 0x20, 0x5B, 0x1F, 0x92, 0xB6, 0x60};
        for (int s = 1; s <= 7; s++) {
            reg(q2Up[s - 1], ROSTER_QUEEN, 1, 0, s);
            reg(q2Right[s - 1], ROSTER_QUEEN, 1, s, 0);
            reg(q2DiagUp[s - 1], ROSTER_QUEEN, 1, s, s);
            if (s != 4) {
                reg(q2DiagDn[s - 1], ROSTER_QUEEN, 1, s, -s);
            }
        }

        // R1 & R2 (14 each)
        int[] r1Up = {0x4E, 0xF8, 0x43, 0xD7, 0x63, 0x9C, 0xE6};
        int[] r1Right = {0x2E, 0xC6, 0x26, 0x88, 0x30, 0x61, 0x6F};
        int[] r2Up = {0x14, 0xA9, 0x68, 0xEE, 0xFB, 0x77, 0xE2};
        int[] r2Right = {0xA6, 0x05, 0x8B, 0xA1, 0x98, 0x32, 0x52};
        for (int s = 1; s <= 7; s++) {
            reg(r1Up[s - 1], ROSTER_ROOK, 0, 0, s);
            reg(r1Right[s - 1], ROSTER_ROOK, 0, s, 0);
            reg(r2Up[s - 1], ROSTER_ROOK, 1, 0, s);
            reg(r2Right[s - 1], ROSTER_ROOK, 1, s, 0);
        }

        // B1 & B2 (14 each)
        int[] b1Up = {0x02, 0x97, 0xE1, 0x41, 0xC3, 0x7C, 0xE4};
        int[] b1Dn = {0x06, 0xB7, 0x55, 0xD9, 0x2C, 0xAE, 0x37};
        int[] b2Up = {0xF6, 0x3F, 0x08, 0x93, 0x73, 0x5E, 0x78};
        int[] b2Dn = {0x35, 0xF2, 0x6D, 0x71, 0xA2, 0xF3, 0x16};
        for (int s = 1; s <= 7; s++) {
            reg(b1Up[s - 1], ROSTER_BISHOP, 0, s, s);
            reg(b1Dn[s - 1], ROSTER_BISHOP, 0, s, -s);
            reg(b2Up[s - 1], ROSTER_BISHOP, 1, s, s);
            reg(b2Dn[s - 1], ROSTER_BISHOP, 1, s, -s);
        }

        // N1, N2, N3 (8 each)
        int[] n1Ops = {0x58, 0x3D, 0xFA, 0xE9, 0xBA, 0xD4, 0xDD, 0x4A};
        int[] n2Ops = {0xC4, 0x0E, 0xFE, 0x5F, 0x75, 0x07, 0x89, 0x34};
        int[] n3Ops = {0x9B, 0xC0, 0xE3, 0xA3, 0xAC, 0xC9, 0xEC, 0x27};
        for (int i = 0; i < 8; i++) {
            int dx = KNIGHT_DELTAS[i * 2];
            int dy = KNIGHT_DELTAS[i * 2 + 1];
            reg(n1Ops[i], ROSTER_KNIGHT, 0, dx, dy);
            reg(n2Ops[i], ROSTER_KNIGHT, 1, dx, dy);
            reg(n3Ops[i], ROSTER_KNIGHT, 2, dx, dy);
        }

        // Pawns Pa..Ph (slot 0..7, 4 moves each)
        int[][] pawnOps = {
            {0x2D, 0xC1, 0x8E, 0xF5},
            {0x64, 0x17, 0x70, 0xA4},
            {0x7B, 0xDA, 0xE0, 0x85},
            {0xC5, 0x0B, 0x90, 0xF9},
            {0x84, 0xFF, 0x15, 0x36},
            {0x09, 0x9E, 0x7D, 0xDE},
            {0xBB, 0xDF, 0xBC, 0x3A},
            {0x12, 0x33, 0x13, 0x19}
        };
        int[] pDx = {0, 0, 1, -1};
        int[] pDy = {1, 2, 1, 1};
        for (int slot = 0; slot < 8; slot++) {
            for (int m = 0; m < 4; m++) {
                reg(pawnOps[slot][m], ROSTER_PAWN, slot, pDx[m], pDy[m]);
            }
        }

        // S-Box entries 0..63 (canonical order: NULL, K, O-O, O-O-O, Q1, R1, R2[0..10])
        int[] sbox0To63 = {
            0xAA,
            0x49, 0x39, 0xD8, 0x5D, 0xC2, 0xB1, 0xB2, 0x47,
            0x76, 0xB5,
            0xA5, 0xB8, 0xCB, 0x53, 0x7F, 0x6B, 0x8D,
            0x79, 0xBE, 0xEB, 0x21, 0x99, 0xD2, 0x57,
            0x4D, 0xB4, 0xBF, 0x62, 0xBD, 0x24, 0x96,
            0xA7, 0x48, 0x28, 0x6E, 0x2F, 0x5A, 0x18,
            0x4E, 0xF8, 0x43, 0xD7, 0x63, 0x9C, 0xE6,
            0x2E, 0xC6, 0x26, 0x88, 0x30, 0x61, 0x6F,
            0x14, 0xA9, 0x68, 0xEE, 0xFB, 0x77, 0xE2,
            0xA6, 0x05, 0x8B, 0xA1
        };
        for (int i = 0; i < sbox0To63.length; i++) {
            SBOX_INV[sbox0To63[i]] = i;
        }
        SBOX_INV[0xDB] = 198;
        SBOX_INV[0x8F] = 206;
        SBOX_INV[0xB9] = 214;
        SBOX_INV[0x3E] = 222;
        SBOX_INV[0xA3] = 230;
        SBOX_INV[0xC7] = 238;
        SBOX_INV[0x8A] = 246;
        SBOX_INV[0xDC] = 254;

        for (int d = 0; d < 256; d++) {
            int val = SBOX_INV[d];
            if (val >= 0 && val < 256) {
                SBOX_FWD[val] = d;
            }
        }
    }

    private CbhCrypto() {
    }

    private static void reg(int op, int type, int slot, int dx, int dy) {
        if (OP_KIND[op] != OP_UNMAPPED) {
            throw new IllegalStateException("Duplicate CBG opcode: 0x" + Integer.toHexString(op));
        }
        OP_KIND[op] = OP_MOVE;
        OP_PIECE_TYPE[op] = type;
        OP_SLOT[op] = slot;
        OP_DX[op] = dx;
        OP_DY[op] = dy;
        MOVE_ENCODE_TABLE[type][slot][dx & 7][dy & 7] = op;
    }

    static int encodeMoveOpcode(int pieceType, int slot, int dxMod8, int dyMod8) {
        if (pieceType < 0 || pieceType >= 6 || slot < 0 || slot >= 8) {
            return -1;
        }
        return MOVE_ENCODE_TABLE[pieceType][slot][dxMod8 & 7][dyMod8 & 7];
    }

    static int promoCodeToConstant(int promoIdx) {
        if (promoIdx == 0) {
            return CONSTANTS.QUEEN;
        } else if (promoIdx == 1) {
            return CONSTANTS.KNIGHT;
        } else if (promoIdx == 2) {
            return CONSTANTS.BISHOP;
        } else if (promoIdx == 3) {
            return CONSTANTS.ROOK;
        }
        return CONSTANTS.EMPTY;
    }

    static int constantToPromoCode(int promoConst) {
        if (promoConst == CONSTANTS.QUEEN) {
            return 0;
        } else if (promoConst == CONSTANTS.KNIGHT) {
            return 1;
        } else if (promoConst == CONSTANTS.BISHOP) {
            return 2;
        } else if (promoConst == CONSTANTS.ROOK) {
            return 3;
        }
        return -1;
    }

    static int toRosterType(int constPieceType) {
        if (constPieceType == CONSTANTS.KING) {
            return ROSTER_KING;
        } else if (constPieceType == CONSTANTS.QUEEN) {
            return ROSTER_QUEEN;
        } else if (constPieceType == CONSTANTS.ROOK) {
            return ROSTER_ROOK;
        } else if (constPieceType == CONSTANTS.BISHOP) {
            return ROSTER_BISHOP;
        } else if (constPieceType == CONSTANTS.KNIGHT) {
            return ROSTER_KNIGHT;
        } else if (constPieceType == CONSTANTS.PAWN) {
            return ROSTER_PAWN;
        }
        return -1;
    }

    /**
     * Mutable roster state tracking square indices ({@code (rank << 3) | file}, -1 if absent)
     * in exact ChessBase column-major slot order.
     */
    static final class RosterState {
        final int[] board = new int[64]; // CONSTANTS.WHITE_* / BLACK_* / EMPTY
        final int[] kingSq = new int[2];
        final int[][] queens = new int[2][16];
        final int[] queenCount = new int[2];
        final int[][] rooks = new int[2][16];
        final int[] rookCount = new int[2];
        final int[][] bishops = new int[2][16];
        final int[] bishopCount = new int[2];
        final int[][] knights = new int[2][16];
        final int[] knightCount = new int[2];
        final int[][] pawns = new int[2][16];
        final int[] pawnCount = new int[2];
        int toMove; // 0 = white, 1 = black

        void resetStandard() {
            Arrays.fill(board, CONSTANTS.EMPTY);
            for (int f = 0; f < 8; f++) {
                board[f] = initialBackRank(f, false);
                board[8 + f] = CONSTANTS.WHITE_PAWN;
                board[48 + f] = CONSTANTS.BLACK_PAWN;
                board[56 + f] = initialBackRank(f, true);
            }
            toMove = 0;
            rebuildRostersColumnMajor();
        }

        void initFromBoard(Board b) {
            Arrays.fill(board, CONSTANTS.EMPTY);
            for (int rank = 0; rank < 8; rank++) {
                for (int file = 0; file < 8; file++) {
                    int p = b.getPieceAt(file, rank);
                    if (p != CONSTANTS.EMPTY && p != CONSTANTS.FRINGE) {
                        board[(rank << 3) | file] = p;
                    }
                }
            }
            if (b.turn == CONSTANTS.BLACK) {
                toMove = 1;
            } else {
                toMove = 0;
            }
            rebuildRostersColumnMajor();
        }

        int findPieceSlot(int pieceType, int sq) {
            if (pieceType == ROSTER_KING) {
                if (kingSq[toMove] == sq) {
                    return 0;
                }
                return -1;
            } else if (pieceType == ROSTER_QUEEN) {
                return findInList(queens[toMove], queenCount[toMove], sq);
            } else if (pieceType == ROSTER_ROOK) {
                return findInList(rooks[toMove], rookCount[toMove], sq);
            } else if (pieceType == ROSTER_BISHOP) {
                return findInList(bishops[toMove], bishopCount[toMove], sq);
            } else if (pieceType == ROSTER_KNIGHT) {
                return findInList(knights[toMove], knightCount[toMove], sq);
            } else if (pieceType == ROSTER_PAWN) {
                return findInList(pawns[toMove], pawnCount[toMove], sq);
            }
            return -1;
        }

        private static int findInList(int[] list, int count, int sq) {
            for (int i = 0; i < count; i++) {
                if (list[i] == sq) {
                    return i;
                }
            }
            return -1;
        }

        private static int initialBackRank(int f, boolean black) {
            int p;
            if (f == 0 || f == 7) {
                p = CONSTANTS.ROOK;
            } else if (f == 1 || f == 6) {
                p = CONSTANTS.KNIGHT;
            } else if (f == 2 || f == 5) {
                p = CONSTANTS.BISHOP;
            } else if (f == 3) {
                p = CONSTANTS.QUEEN;
            } else {
                p = CONSTANTS.KING;
            }
            if (black) {
                return p | 0x80;
            }
            return p;
        }

        void rebuildRostersColumnMajor() {
            kingSq[0] = -1;
            kingSq[1] = -1;
            queenCount[0] = 0;
            queenCount[1] = 0;
            rookCount[0] = 0;
            rookCount[1] = 0;
            bishopCount[0] = 0;
            bishopCount[1] = 0;
            knightCount[0] = 0;
            knightCount[1] = 0;
            pawnCount[0] = 0;
            pawnCount[1] = 0;
            Arrays.fill(queens[0], -1);
            Arrays.fill(queens[1], -1);
            Arrays.fill(rooks[0], -1);
            Arrays.fill(rooks[1], -1);
            Arrays.fill(bishops[0], -1);
            Arrays.fill(bishops[1], -1);
            Arrays.fill(knights[0], -1);
            Arrays.fill(knights[1], -1);
            Arrays.fill(pawns[0], -1);
            Arrays.fill(pawns[1], -1);

            for (int file = 0; file < 8; file++) {
                for (int rank = 0; rank < 8; rank++) {
                    int sq = (rank << 3) | file;
                    int piece = board[sq];
                    if (piece == CONSTANTS.EMPTY) {
                        continue;
                    }
                    int side;
                    if ((piece & 0x80) != 0) {
                        side = 1;
                    } else {
                        side = 0;
                    }
                    int type = piece & 0x07;
                    if (type == CONSTANTS.KING) {
                        kingSq[side] = sq;
                    } else if (type == CONSTANTS.QUEEN) {
                        if (queenCount[side] < 16) {
                            queens[side][queenCount[side]++] = sq;
                        }
                    } else if (type == CONSTANTS.ROOK) {
                        if (rookCount[side] < 16) {
                            rooks[side][rookCount[side]++] = sq;
                        }
                    } else if (type == CONSTANTS.BISHOP) {
                        if (bishopCount[side] < 16) {
                            bishops[side][bishopCount[side]++] = sq;
                        }
                    } else if (type == CONSTANTS.KNIGHT) {
                        if (knightCount[side] < 16) {
                            knights[side][knightCount[side]++] = sq;
                        }
                    } else if (type == CONSTANTS.PAWN) {
                        if (pawnCount[side] < 16) {
                            pawns[side][pawnCount[side]++] = sq;
                        }
                    }
                }
            }
        }

        void copyFrom(RosterState other) {
            System.arraycopy(other.board, 0, board, 0, 64);
            kingSq[0] = other.kingSq[0];
            kingSq[1] = other.kingSq[1];
            queenCount[0] = other.queenCount[0];
            queenCount[1] = other.queenCount[1];
            rookCount[0] = other.rookCount[0];
            rookCount[1] = other.rookCount[1];
            bishopCount[0] = other.bishopCount[0];
            bishopCount[1] = other.bishopCount[1];
            knightCount[0] = other.knightCount[0];
            knightCount[1] = other.knightCount[1];
            pawnCount[0] = other.pawnCount[0];
            pawnCount[1] = other.pawnCount[1];
            System.arraycopy(other.queens[0], 0, queens[0], 0, 16);
            System.arraycopy(other.queens[1], 0, queens[1], 0, 16);
            System.arraycopy(other.rooks[0], 0, rooks[0], 0, 16);
            System.arraycopy(other.rooks[1], 0, rooks[1], 0, 16);
            System.arraycopy(other.bishops[0], 0, bishops[0], 0, 16);
            System.arraycopy(other.bishops[1], 0, bishops[1], 0, 16);
            System.arraycopy(other.knights[0], 0, knights[0], 0, 16);
            System.arraycopy(other.knights[1], 0, knights[1], 0, 16);
            System.arraycopy(other.pawns[0], 0, pawns[0], 0, 16);
            System.arraycopy(other.pawns[1], 0, pawns[1], 0, 16);
            toMove = other.toMove;
        }

        int getPieceSquare(int pieceType, int slot) {
            if (pieceType == ROSTER_KING) {
                return kingSq[toMove];
            } else if (pieceType == ROSTER_QUEEN) {
                if (slot >= 0 && slot < queenCount[toMove]) {
                    return queens[toMove][slot];
                }
                return -1;
            } else if (pieceType == ROSTER_ROOK) {
                if (slot >= 0 && slot < rookCount[toMove]) {
                    return rooks[toMove][slot];
                }
                return -1;
            } else if (pieceType == ROSTER_BISHOP) {
                if (slot >= 0 && slot < bishopCount[toMove]) {
                    return bishops[toMove][slot];
                }
                return -1;
            } else if (pieceType == ROSTER_KNIGHT) {
                if (slot >= 0 && slot < knightCount[toMove]) {
                    return knights[toMove][slot];
                }
                return -1;
            } else if (pieceType == ROSTER_PAWN) {
                if (slot >= 0 && slot < pawnCount[toMove]) {
                    return pawns[toMove][slot];
                }
                return -1;
            }
            return -1;
        }

        void applyCastle(boolean kingside) {
            int rankOffset;
            if (toMove == 0) {
                rankOffset = 0;
            } else {
                rankOffset = 56;
            }
            int kFrom = rankOffset + 4;
            int kTo;
            int rFrom;
            int rTo;
            if (kingside) {
                kTo = rankOffset + 6;
                rFrom = rankOffset + 7;
                rTo = rankOffset + 5;
            } else {
                kTo = rankOffset + 2;
                rFrom = rankOffset;
                rTo = rankOffset + 3;
            }
            int kPiece = board[kFrom];
            int rPiece = board[rFrom];
            board[kFrom] = CONSTANTS.EMPTY;
            board[rFrom] = CONSTANTS.EMPTY;
            board[kTo] = kPiece;
            board[rTo] = rPiece;
            kingSq[toMove] = kTo;
            for (int i = 0; i < rookCount[toMove]; i++) {
                if (rooks[toMove][i] == rFrom) {
                    rooks[toMove][i] = rTo;
                    break;
                }
            }
            toMove = 1 - toMove;
        }

        void applyMove(int fromSq, int toSq, int promoConst) {
            int moving = board[fromSq];
            int enemy = 1 - toMove;
            int movingType = moving & 0x07;

            // Check en passant capture
            int capturedSq = toSq;
            if (movingType == CONSTANTS.PAWN && board[toSq] == CONSTANTS.EMPTY && (fromSq & 7) != (toSq & 7)) {
                int epSq = (fromSq & ~7) | (toSq & 7);
                if (board[epSq] != CONSTANTS.EMPTY) {
                    capturedSq = epSq;
                }
            }

            if (board[capturedSq] != CONSTANTS.EMPTY) {
                removePieceFromRoster(enemy, board[capturedSq] & 0x07, capturedSq);
                board[capturedSq] = CONSTANTS.EMPTY;
            }

            board[fromSq] = CONSTANTS.EMPTY;

            if (promoConst != CONSTANTS.EMPTY) {
                int promotedPiece;
                if (toMove == 1) {
                    promotedPiece = promoConst | 0x80;
                } else {
                    promotedPiece = promoConst;
                }
                board[toSq] = promotedPiece;
                // Pawns clear slot without shifting remaining pawn indices
                clearPawnSlot(toMove, fromSq);
                addPromotedPiece(toMove, promoConst, toSq);
            } else {
                board[toSq] = moving;
                updatePieceSquare(toMove, movingType, fromSq, toSq);
            }
            toMove = enemy;
        }

        private void clearPawnSlot(int side, int sq) {
            for (int i = 0; i < pawnCount[side]; i++) {
                if (pawns[side][i] == sq) {
                    pawns[side][i] = -1;
                    break;
                }
            }
        }

        private void addPromotedPiece(int side, int type, int sq) {
            if (type == CONSTANTS.QUEEN && queenCount[side] < 16) {
                queens[side][queenCount[side]++] = sq;
            } else if (type == CONSTANTS.ROOK && rookCount[side] < 16) {
                rooks[side][rookCount[side]++] = sq;
            } else if (type == CONSTANTS.BISHOP && bishopCount[side] < 16) {
                bishops[side][bishopCount[side]++] = sq;
            } else if (type == CONSTANTS.KNIGHT && knightCount[side] < 16) {
                knights[side][knightCount[side]++] = sq;
            }
        }

        private void updatePieceSquare(int side, int type, int oldSq, int newSq) {
            if (type == CONSTANTS.KING) {
                kingSq[side] = newSq;
            } else if (type == CONSTANTS.QUEEN) {
                replaceInList(queens[side], queenCount[side], oldSq, newSq);
            } else if (type == CONSTANTS.ROOK) {
                replaceInList(rooks[side], rookCount[side], oldSq, newSq);
            } else if (type == CONSTANTS.BISHOP) {
                replaceInList(bishops[side], bishopCount[side], oldSq, newSq);
            } else if (type == CONSTANTS.KNIGHT) {
                replaceInList(knights[side], knightCount[side], oldSq, newSq);
            } else if (type == CONSTANTS.PAWN) {
                replaceInList(pawns[side], pawnCount[side], oldSq, newSq);
            }
        }

        private static void replaceInList(int[] list, int count, int oldSq, int newSq) {
            for (int i = 0; i < count; i++) {
                if (list[i] == oldSq) {
                    list[i] = newSq;
                    return;
                }
            }
        }

        private void removePieceFromRoster(int side, int type, int sq) {
            if (type == CONSTANTS.PAWN) {
                clearPawnSlot(side, sq);
            } else if (type == CONSTANTS.QUEEN) {
                queenCount[side] = shiftRemove(queens[side], queenCount[side], sq);
            } else if (type == CONSTANTS.ROOK) {
                rookCount[side] = shiftRemove(rooks[side], rookCount[side], sq);
            } else if (type == CONSTANTS.BISHOP) {
                bishopCount[side] = shiftRemove(bishops[side], bishopCount[side], sq);
            } else if (type == CONSTANTS.KNIGHT) {
                knightCount[side] = shiftRemove(knights[side], knightCount[side], sq);
            } else if (type == CONSTANTS.KING) {
                kingSq[side] = -1;
            }
        }

        private static int shiftRemove(int[] list, int count, int sq) {
            for (int i = 0; i < count; i++) {
                if (list[i] == sq) {
                    for (int j = i + 1; j < count; j++) {
                        list[j - 1] = list[j];
                    }
                    list[count - 1] = -1;
                    return count - 1;
                }
            }
            return count;
        }

        String buildFen(int epFile, int castlingMask, int fullmove) {
            StringBuilder sb = new StringBuilder(80);
            for (int rank = 7; rank >= 0; rank--) {
                int empty = 0;
                for (int file = 0; file < 8; file++) {
                    int p = board[(rank << 3) | file];
                    if (p == CONSTANTS.EMPTY) {
                        empty++;
                    } else {
                        if (empty > 0) {
                            sb.append((char) ('0' + empty));
                            empty = 0;
                        }
                        sb.append(pieceToFenChar(p));
                    }
                }
                if (empty > 0) {
                    sb.append((char) ('0' + empty));
                }
                if (rank > 0) {
                    sb.append('/');
                }
            }
            if (toMove == 0) {
                sb.append(" w ");
            } else {
                sb.append(" b ");
            }
            StringBuilder castles = new StringBuilder(4);
            if ((castlingMask & 0x02) != 0 && kingSq[0] == 4 && board[7] == CONSTANTS.WHITE_ROOK) {
                castles.append('K');
            }
            if ((castlingMask & 0x01) != 0 && kingSq[0] == 4 && board[0] == CONSTANTS.WHITE_ROOK) {
                castles.append('Q');
            }
            if ((castlingMask & 0x08) != 0 && kingSq[1] == 60 && board[63] == CONSTANTS.BLACK_ROOK) {
                castles.append('k');
            }
            if ((castlingMask & 0x04) != 0 && kingSq[1] == 60 && board[56] == CONSTANTS.BLACK_ROOK) {
                castles.append('q');
            }
            if (castles.isEmpty()) {
                sb.append('-');
            } else {
                sb.append(castles);
            }
            sb.append(' ');
            if (epFile >= 1 && epFile <= 8) {
                char epCol = (char) ('a' + (epFile - 1));
                char epRank;
                if (toMove == 0) {
                    epRank = '6';
                } else {
                    epRank = '3';
                }
                sb.append(epCol).append(epRank);
            } else {
                sb.append('-');
            }
            sb.append(" 0 ");
            if (fullmove <= 0) {
                sb.append('1');
            } else {
                sb.append(fullmove);
            }
            return sb.toString();
        }
    }

    static char pieceToFenChar(int piece) {
        if (piece == CONSTANTS.WHITE_KING) {
            return 'K';
        } else if (piece == CONSTANTS.WHITE_QUEEN) {
            return 'Q';
        } else if (piece == CONSTANTS.WHITE_ROOK) {
            return 'R';
        } else if (piece == CONSTANTS.WHITE_BISHOP) {
            return 'B';
        } else if (piece == CONSTANTS.WHITE_KNIGHT) {
            return 'N';
        } else if (piece == CONSTANTS.WHITE_PAWN) {
            return 'P';
        } else if (piece == CONSTANTS.BLACK_KING) {
            return 'k';
        } else if (piece == CONSTANTS.BLACK_QUEEN) {
            return 'q';
        } else if (piece == CONSTANTS.BLACK_ROOK) {
            return 'r';
        } else if (piece == CONSTANTS.BLACK_BISHOP) {
            return 'b';
        } else if (piece == CONSTANTS.BLACK_KNIGHT) {
            return 'n';
        } else if (piece == CONSTANTS.BLACK_PAWN) {
            return 'p';
        }
        return '?';
    }

    /**
     * Decodes a 28-byte classic CBH custom setup block into {@code state} and returns its FEN.
     */
    static String decodeCustomSetup(byte[] buf, int off, RosterState state) {
        Arrays.fill(state.board, CONSTANTS.EMPTY);
        int flagsByte = buf[off + 1] & 0xFF;
        int epFile = flagsByte & 0x0F;
        if ((flagsByte & 0x10) != 0) {
            state.toMove = 1;
        } else {
            state.toMove = 0;
        }
        int castlingMask = buf[off + 2] & 0x0F;
        int fullmove = buf[off + 3] & 0xFF;
        if (fullmove <= 0) {
            fullmove = 1;
        }

        int bitOff = 0;
        int base = off + 4;
        for (int cmSq = 0; cmSq < 64; cmSq++) {
            int firstBit = readBit(buf, base, bitOff);
            bitOff++;
            if (firstBit == 0) {
                continue;
            }
            int code = (readBit(buf, base, bitOff) << 3)
                    | (readBit(buf, base, bitOff + 1) << 2)
                    | (readBit(buf, base, bitOff + 2) << 1)
                    | readBit(buf, base, bitOff + 3);
            bitOff += 4;
            int file = cmSq >> 3;
            int rank = cmSq & 7;
            int piece = decodeSetupCode(code);
            if (piece != CONSTANTS.EMPTY) {
                state.board[(rank << 3) | file] = piece;
            }
        }
        state.rebuildRostersColumnMajor();
        return state.buildFen(epFile, castlingMask, fullmove);
    }

    private static int readBit(byte[] buf, int baseOff, int bitIdx) {
        int byteIdx = baseOff + (bitIdx >>> 3);
        if (byteIdx >= buf.length) {
            return 0;
        }
        int shift = 7 - (bitIdx & 7);
        return (buf[byteIdx] >>> shift) & 1;
    }

    private static int decodeSetupCode(int low4Bits) {
        boolean black = (low4Bits & 0x08) != 0;
        int p = low4Bits & 0x07;
        int type;
        if (p == 1) {
            type = CONSTANTS.KING;
        } else if (p == 2) {
            type = CONSTANTS.QUEEN;
        } else if (p == 3) {
            type = CONSTANTS.KNIGHT;
        } else if (p == 4) {
            type = CONSTANTS.BISHOP;
        } else if (p == 5) {
            type = CONSTANTS.ROOK;
        } else if (p == 6) {
            type = CONSTANTS.PAWN;
        } else {
            return CONSTANTS.EMPTY;
        }
        if (black) {
            return type | 0x80;
        }
        return type;
    }

    /**
     * Encodes {@code rootBoard} into a 28-byte classic CBH custom setup block and
     * initializes {@code state} to match.
     */
    static byte[] encodeCustomSetup(Board rootBoard, RosterState state) {
        state.initFromBoard(rootBoard);
        byte[] buf = new byte[28];
        buf[0] = 0x01;
        int epFile = CbCommon.parseEpFile(rootBoard);
        int flagsByte = epFile & 0x0F;
        if (rootBoard.turn == CONSTANTS.BLACK) {
            flagsByte |= 0x10;
        }
        buf[1] = (byte) flagsByte;
        buf[2] = (byte) (CbCommon.extractCastlingMask(rootBoard) & 0x0F);
        int fullmove = rootBoard.fullmoveNumber;
        if (fullmove <= 0) {
            fullmove = 1;
        } else if (fullmove > 255) {
            fullmove = 255;
        }
        buf[3] = (byte) fullmove;

        int bitOff = 0;
        for (int cmSq = 0; cmSq < 64; cmSq++) {
            int file = cmSq >> 3;
            int rank = cmSq & 7;
            int piece = state.board[(rank << 3) | file];
            int code = encodeSetupCode(piece);
            if (code == 0) {
                bitOff++;
            } else {
                writeBit(buf, 4, bitOff++, 1);
                writeBit(buf, 4, bitOff++, (code >>> 3) & 1);
                writeBit(buf, 4, bitOff++, (code >>> 2) & 1);
                writeBit(buf, 4, bitOff++, (code >>> 1) & 1);
                writeBit(buf, 4, bitOff++, code & 1);
            }
        }
        return buf;
    }

    private static void writeBit(byte[] buf, int baseOff, int bitIdx, int val) {
        if (val == 0) {
            return;
        }
        int byteIdx = baseOff + (bitIdx >>> 3);
        if (byteIdx >= buf.length) {
            return;
        }
        int shift = 7 - (bitIdx & 7);
        buf[byteIdx] |= (byte) (1 << shift);
    }

    private static int encodeSetupCode(int piece) {
        if (piece == CONSTANTS.EMPTY) {
            return 0;
        }
        boolean black = (piece & 0x80) != 0;
        int type = piece & 0x07;
        int p;
        if (type == CONSTANTS.KING) {
            p = 1;
        } else if (type == CONSTANTS.QUEEN) {
            p = 2;
        } else if (type == CONSTANTS.KNIGHT) {
            p = 3;
        } else if (type == CONSTANTS.BISHOP) {
            p = 4;
        } else if (type == CONSTANTS.ROOK) {
            p = 5;
        } else if (type == CONSTANTS.PAWN) {
            p = 6;
        } else {
            return 0;
        }
        if (black) {
            return p | 0x08;
        }
        return p;
    }
}
