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
 * Static 45,356-entry move index table and custom position piece decoder for
 * ChessBase `.2cbg` streams.
 */
final class TwoCbhMoveTable {

    static final int TABLE_SIZE = 45356;

    static final int CASTLE_WQ = 45353;
    static final int CASTLE_WK = 45354;
    static final int CASTLE_BQ = 45355;
    static final int CASTLE_BK = 45356;

    /**
     * Packed move word entry at index {@code 1..45356}:
     * bits 0..5 = fromSq64, bits 6..11 = toSq64, bits 12..15 = promo ({@link CONSTANTS}).
     */
    static final int[] MOVES = new int[TABLE_SIZE + 1];

    // Reverse O(1) lookup tables:
    // PIECE_WORD[color 0=W,1=B][pieceIdx 0=K,1=Q,2=N,3=B,4=R,5=P][(fromSq64 << 6) | toSq64]
    private static final int[][][] PIECE_WORD = new int[2][6][4096];
    // PROMO_WORD[promoIdx 0=Q,1=R,2=B,3=N][(fromSq64 << 6) | toSq64]
    private static final int[][] PROMO_WORD = new int[4][4096];

    private static final int[] K_DX = {-1, -1, -1, 0, 0, 1, 1, 1};
    private static final int[] K_DY = {-1, 0, 1, -1, 1, -1, 0, 1};

    private static final int[] N_DX = {-2, -2, 2, 2, -1, -1, 1, 1};
    private static final int[] N_DY = {-1, 1, -1, 1, -2, 2, -2, 2};

    private static final int[] DIAG_DX = {-1, 1, 1, -1};
    private static final int[] DIAG_DY = {-1, -1, 1, 1};

    private static final int[] ORTHO_DX = {-1, 0, 1, 0};
    private static final int[] ORTHO_DY = {0, -1, 0, 1};

    private static final int[] Q_DX = {-1, 1, 1, -1, -1, 0, 1, 0};
    private static final int[] Q_DY = {-1, -1, 1, 1, 0, -1, 0, 1};

    private static final int[] PROMO_ORDER = {
        CONSTANTS.QUEEN, CONSTANTS.ROOK, CONSTANTS.BISHOP, CONSTANTS.KNIGHT
    };

    static {
        int idx = 1;

        // 1. Pieces for White then Black
        for (int color = 0; color < 2; color++) {
            // King
            for (int fx = 0; fx < 8; fx++) {
                for (int fy = 0; fy < 8; fy++) {
                    for (int d = 0; d < 8; d++) {
                        int tx = fx + K_DX[d];
                        int ty = fy + K_DY[d];
                        if (tx >= 0 && tx < 8 && ty >= 0 && ty < 8) {
                            int packed = packMove(fx, fy, tx, ty, CONSTANTS.EMPTY);
                            regPieceWord(color, 0, packed, idx);
                            for (int r = 0; r < 6; r++) {
                                MOVES[idx++] = packed;
                            }
                        }
                    }
                }
            }
            // Queen
            for (int fx = 0; fx < 8; fx++) {
                for (int fy = 0; fy < 8; fy++) {
                    for (int d = 0; d < 8; d++) {
                        for (int step = 1; step <= 7; step++) {
                            int tx = fx + Q_DX[d] * step;
                            int ty = fy + Q_DY[d] * step;
                            if (tx >= 0 && tx < 8 && ty >= 0 && ty < 8) {
                                int packed = packMove(fx, fy, tx, ty, CONSTANTS.EMPTY);
                                regPieceWord(color, 1, packed, idx);
                                for (int r = 0; r < 6; r++) {
                                    MOVES[idx++] = packed;
                                }
                            }
                        }
                    }
                }
            }
            // Knight
            for (int fx = 0; fx < 8; fx++) {
                for (int fy = 0; fy < 8; fy++) {
                    for (int d = 0; d < 8; d++) {
                        int tx = fx + N_DX[d];
                        int ty = fy + N_DY[d];
                        if (tx >= 0 && tx < 8 && ty >= 0 && ty < 8) {
                            int packed = packMove(fx, fy, tx, ty, CONSTANTS.EMPTY);
                            regPieceWord(color, 2, packed, idx);
                            for (int r = 0; r < 6; r++) {
                                MOVES[idx++] = packed;
                            }
                        }
                    }
                }
            }
            // Bishop
            for (int fx = 0; fx < 8; fx++) {
                for (int fy = 0; fy < 8; fy++) {
                    for (int d = 0; d < 4; d++) {
                        for (int step = 1; step <= 7; step++) {
                            int tx = fx + DIAG_DX[d] * step;
                            int ty = fy + DIAG_DY[d] * step;
                            if (tx >= 0 && tx < 8 && ty >= 0 && ty < 8) {
                                int packed = packMove(fx, fy, tx, ty, CONSTANTS.EMPTY);
                                regPieceWord(color, 3, packed, idx);
                                for (int r = 0; r < 6; r++) {
                                    MOVES[idx++] = packed;
                                }
                            }
                        }
                    }
                }
            }
            // Rook
            for (int fx = 0; fx < 8; fx++) {
                for (int fy = 0; fy < 8; fy++) {
                    for (int d = 0; d < 4; d++) {
                        for (int step = 1; step <= 7; step++) {
                            int tx = fx + ORTHO_DX[d] * step;
                            int ty = fy + ORTHO_DY[d] * step;
                            if (tx >= 0 && tx < 8 && ty >= 0 && ty < 8) {
                                int packed = packMove(fx, fy, tx, ty, CONSTANTS.EMPTY);
                                regPieceWord(color, 4, packed, idx);
                                for (int r = 0; r < 6; r++) {
                                    MOVES[idx++] = packed;
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. White Pawns (fy = 1..6)
        for (int fx = 0; fx < 8; fx++) {
            for (int fy = 1; fy <= 6; fy++) {
                if (fy == 1) {
                    int p2 = packMove(fx, fy, fx, fy + 2, CONSTANTS.EMPTY);
                    regPieceWord(0, 5, p2, idx);
                    MOVES[idx++] = p2;
                    int p1 = packMove(fx, fy, fx, fy + 1, CONSTANTS.EMPTY);
                    regPieceWord(0, 5, p1, idx);
                    MOVES[idx++] = p1;
                    if (fx > 0) {
                        int capL = packMove(fx, fy, fx - 1, fy + 1, CONSTANTS.EMPTY);
                        regPieceWord(0, 5, capL, idx);
                        for (int r = 0; r < 5; r++) {
                            MOVES[idx++] = capL;
                        }
                    }
                    if (fx < 7) {
                        int capR = packMove(fx, fy, fx + 1, fy + 1, CONSTANTS.EMPTY);
                        regPieceWord(0, 5, capR, idx);
                        for (int r = 0; r < 5; r++) {
                            MOVES[idx++] = capR;
                        }
                    }
                } else if (fy <= 5) {
                    int p1 = packMove(fx, fy, fx, fy + 1, CONSTANTS.EMPTY);
                    regPieceWord(0, 5, p1, idx);
                    MOVES[idx++] = p1;
                    int caps;
                    if (fy == 4) {
                        caps = 6;
                    } else {
                        caps = 5;
                    }
                    if (fx > 0) {
                        int capL = packMove(fx, fy, fx - 1, fy + 1, CONSTANTS.EMPTY);
                        regPieceWord(0, 5, capL, idx);
                        for (int r = 0; r < caps; r++) {
                            MOVES[idx++] = capL;
                        }
                    }
                    if (fx < 7) {
                        int capR = packMove(fx, fy, fx + 1, fy + 1, CONSTANTS.EMPTY);
                        regPieceWord(0, 5, capR, idx);
                        for (int r = 0; r < caps; r++) {
                            MOVES[idx++] = capR;
                        }
                    }
                } else {
                    for (int pi = 0; pi < PROMO_ORDER.length; pi++) {
                        int pr = PROMO_ORDER[pi];
                        int pm = packMove(fx, fy, fx, fy + 1, pr);
                        regPromoWord(pi, pm, idx);
                        MOVES[idx++] = pm;
                    }
                    if (fx > 0) {
                        for (int cap = 0; cap < 4; cap++) {
                            for (int pi = 0; pi < PROMO_ORDER.length; pi++) {
                                int pr = PROMO_ORDER[pi];
                                int pm = packMove(fx, fy, fx - 1, fy + 1, pr);
                                regPromoWord(pi, pm, idx);
                                MOVES[idx++] = pm;
                            }
                        }
                    }
                    if (fx < 7) {
                        for (int cap = 0; cap < 4; cap++) {
                            for (int pi = 0; pi < PROMO_ORDER.length; pi++) {
                                int pr = PROMO_ORDER[pi];
                                int pm = packMove(fx, fy, fx + 1, fy + 1, pr);
                                regPromoWord(pi, pm, idx);
                                MOVES[idx++] = pm;
                            }
                        }
                    }
                }
            }
        }

        // 3. Black Pawns (fy = 1..6)
        for (int fx = 0; fx < 8; fx++) {
            for (int fy = 1; fy <= 6; fy++) {
                if (fy == 6) {
                    int p2 = packMove(fx, fy, fx, fy - 2, CONSTANTS.EMPTY);
                    regPieceWord(1, 5, p2, idx);
                    MOVES[idx++] = p2;
                    int p1 = packMove(fx, fy, fx, fy - 1, CONSTANTS.EMPTY);
                    regPieceWord(1, 5, p1, idx);
                    MOVES[idx++] = p1;
                    if (fx > 0) {
                        int capL = packMove(fx, fy, fx - 1, fy - 1, CONSTANTS.EMPTY);
                        regPieceWord(1, 5, capL, idx);
                        for (int r = 0; r < 5; r++) {
                            MOVES[idx++] = capL;
                        }
                    }
                    if (fx < 7) {
                        int capR = packMove(fx, fy, fx + 1, fy - 1, CONSTANTS.EMPTY);
                        regPieceWord(1, 5, capR, idx);
                        for (int r = 0; r < 5; r++) {
                            MOVES[idx++] = capR;
                        }
                    }
                } else if (fy >= 2) {
                    int p1 = packMove(fx, fy, fx, fy - 1, CONSTANTS.EMPTY);
                    regPieceWord(1, 5, p1, idx);
                    MOVES[idx++] = p1;
                    int caps;
                    if (fy == 3) {
                        caps = 6;
                    } else {
                        caps = 5;
                    }
                    if (fx > 0) {
                        int capL = packMove(fx, fy, fx - 1, fy - 1, CONSTANTS.EMPTY);
                        regPieceWord(1, 5, capL, idx);
                        for (int r = 0; r < caps; r++) {
                            MOVES[idx++] = capL;
                        }
                    }
                    if (fx < 7) {
                        int capR = packMove(fx, fy, fx + 1, fy - 1, CONSTANTS.EMPTY);
                        regPieceWord(1, 5, capR, idx);
                        for (int r = 0; r < caps; r++) {
                            MOVES[idx++] = capR;
                        }
                    }
                } else {
                    for (int pi = 0; pi < PROMO_ORDER.length; pi++) {
                        int pr = PROMO_ORDER[pi];
                        int pm = packMove(fx, fy, fx, fy - 1, pr);
                        regPromoWord(pi, pm, idx);
                        MOVES[idx++] = pm;
                    }
                    if (fx > 0) {
                        for (int cap = 0; cap < 4; cap++) {
                            for (int pi = 0; pi < PROMO_ORDER.length; pi++) {
                                int pr = PROMO_ORDER[pi];
                                int pm = packMove(fx, fy, fx - 1, fy - 1, pr);
                                regPromoWord(pi, pm, idx);
                                MOVES[idx++] = pm;
                            }
                        }
                    }
                    if (fx < 7) {
                        for (int cap = 0; cap < 4; cap++) {
                            for (int pi = 0; pi < PROMO_ORDER.length; pi++) {
                                int pr = PROMO_ORDER[pi];
                                int pm = packMove(fx, fy, fx + 1, fy - 1, pr);
                                regPromoWord(pi, pm, idx);
                                MOVES[idx++] = pm;
                            }
                        }
                    }
                }
            }
        }

        // 4. Castling tail (45353..45356)
        MOVES[idx++] = packMove(4, 0, 2, 0, CONSTANTS.EMPTY);
        MOVES[idx++] = packMove(4, 0, 6, 0, CONSTANTS.EMPTY);
        MOVES[idx++] = packMove(4, 7, 2, 7, CONSTANTS.EMPTY);
        MOVES[idx++] = packMove(4, 7, 6, 7, CONSTANTS.EMPTY);

        if (idx != TABLE_SIZE + 1) {
            throw new IllegalStateException("Unexpected 2CBG move table size: " + (idx - 1));
        }
    }

    private TwoCbhMoveTable() {
    }

    private static void regPieceWord(int color, int pieceIdx, int packed, int wordIdx) {
        int key = (fromSq64(packed) << 6) | toSq64(packed);
        if (PIECE_WORD[color][pieceIdx][key] == 0) {
            PIECE_WORD[color][pieceIdx][key] = wordIdx;
        }
    }

    private static void regPromoWord(int promoIdx, int packed, int wordIdx) {
        int key = (fromSq64(packed) << 6) | toSq64(packed);
        if (PROMO_WORD[promoIdx][key] == 0) {
            PROMO_WORD[promoIdx][key] = wordIdx;
        }
    }

    static int encodeMoveWord(
            int color,
            int constPieceType,
            int fromSq64,
            int toSq64,
            int promoConst,
            boolean isCastleK,
            boolean isCastleQ) {
        if (isCastleK) {
            return (color == 0) ? CASTLE_WK : CASTLE_BK;
        }
        if (isCastleQ) {
            return (color == 0) ? CASTLE_WQ : CASTLE_BQ;
        }
        int key = ((fromSq64 & 0x3F) << 6) | (toSq64 & 0x3F);
        if (promoConst != CONSTANTS.EMPTY) {
            int promoIdx = -1;
            if (promoConst == CONSTANTS.QUEEN) {
                promoIdx = 0;
            } else if (promoConst == CONSTANTS.ROOK) {
                promoIdx = 1;
            } else if (promoConst == CONSTANTS.BISHOP) {
                promoIdx = 2;
            } else if (promoConst == CONSTANTS.KNIGHT) {
                promoIdx = 3;
            }
            if (promoIdx >= 0) {
                int w = PROMO_WORD[promoIdx][key];
                if (w > 0) {
                    return w;
                }
            }
        } else {
            int pieceIdx = constPieceTypeToTableIdx(constPieceType);
            if (color >= 0 && color <= 1 && pieceIdx >= 0) {
                int w = PIECE_WORD[color][pieceIdx][key];
                if (w > 0) {
                    return w;
                }
            }
        }
        // Fallback linear scan if board state didn't match standard piece slot
        int wantPacked = (fromSq64 & 0x3F) | ((toSq64 & 0x3F) << 6) | ((promoConst & 0x0F) << 12);
        for (int i = 1; i <= TABLE_SIZE; i++) {
            if (MOVES[i] == wantPacked) {
                return i;
            }
        }
        return -1;
    }

    private static int constPieceTypeToTableIdx(int constPieceType) {
        if (constPieceType == CONSTANTS.KING) {
            return 0;
        } else if (constPieceType == CONSTANTS.QUEEN) {
            return 1;
        } else if (constPieceType == CONSTANTS.KNIGHT) {
            return 2;
        } else if (constPieceType == CONSTANTS.BISHOP) {
            return 3;
        } else if (constPieceType == CONSTANTS.ROOK) {
            return 4;
        } else if (constPieceType == CONSTANTS.PAWN) {
            return 5;
        }
        return -1;
    }

    /**
     * Encodes {@code rootBoard} into `.2cbg` custom setup 16-bit words starting with
     * {@code 0x0001, 0xFFFB} and ending with {@code 0xFFFC}.
     */
    static int[] encodeSetup(Board rootBoard) {
        int[] tmp = new int[72];
        int n = 0;
        tmp[n++] = 0x0001;
        tmp[n++] = 0xFFFB;
        int fullmove = rootBoard.fullmoveNumber;
        if (fullmove <= 0) {
            fullmove = 1;
        } else if (fullmove > 65535) {
            fullmove = 65535;
        }
        tmp[n++] = fullmove;
        int sideToMove = (rootBoard.turn == CONSTANTS.BLACK) ? 1 : 0;
        int castlingMask = CbCommon.extractCastlingMask(rootBoard) & 0x0F;
        tmp[n++] = (castlingMask << 8) | sideToMove;
        tmp[n++] = CbCommon.parseEpFile(rootBoard);

        for (int file = 0; file < 8; file++) {
            for (int rank = 0; rank < 8; rank++) {
                int p = rootBoard.getPieceAt(file, rank);
                if (p == CONSTANTS.EMPTY || p == CONSTANTS.FRINGE) {
                    continue;
                }
                if (p == CONSTANTS.WHITE_PAWN) {
                    if (rank >= 1 && rank <= 6) {
                        tmp[n++] = 0xC000 + 685 + (file * 6) + (rank - 1);
                    }
                } else if (p == CONSTANTS.BLACK_PAWN) {
                    if (rank >= 1 && rank <= 6) {
                        tmp[n++] = 0xC000 + 733 + (file * 6) + (rank - 1);
                    }
                } else {
                    int typeIdx = encodePieceType(p);
                    if (typeIdx >= 0) {
                        tmp[n++] = 0xC000 + 45 + (typeIdx * 64) + (file * 8 + rank);
                    }
                }
            }
        }
        tmp[n++] = 0xFFFC;
        return Arrays.copyOf(tmp, n);
    }

    private static int encodePieceType(int piece) {
        if (piece == CONSTANTS.WHITE_KING) {
            return 0;
        } else if (piece == CONSTANTS.WHITE_QUEEN) {
            return 1;
        } else if (piece == CONSTANTS.WHITE_KNIGHT) {
            return 2;
        } else if (piece == CONSTANTS.WHITE_BISHOP) {
            return 3;
        } else if (piece == CONSTANTS.WHITE_ROOK) {
            return 4;
        } else if (piece == CONSTANTS.BLACK_KING) {
            return 5;
        } else if (piece == CONSTANTS.BLACK_QUEEN) {
            return 6;
        } else if (piece == CONSTANTS.BLACK_KNIGHT) {
            return 7;
        } else if (piece == CONSTANTS.BLACK_BISHOP) {
            return 8;
        } else if (piece == CONSTANTS.BLACK_ROOK) {
            return 9;
        }
        return -1;
    }

    private static int packMove(int fx, int fy, int tx, int ty, int promo) {
        int from = (fy << 3) | fx;
        int to = (ty << 3) | tx;
        return from | (to << 6) | ((promo & 0x0F) << 12);
    }

    static int fromSq64(int packed) {
        return packed & 0x3F;
    }

    static int toSq64(int packed) {
        return (packed >>> 6) & 0x3F;
    }

    static int promoConst(int packed) {
        return (packed >>> 12) & 0x0F;
    }

    /**
     * Decodes a `.2cbg` custom setup position starting at word index 2 (`0xFFFB`
     * already observed at word[1]). Returns the next word index right after the
     * closing `0xFFFC` marker (i.e. start of move words), and fills {@code outFen[0]}.
     */
    static int decodeSetup(byte[] payload, int payloadLen, String[] outFen) {
        int wordCount = payloadLen / 2;
        if (wordCount < 5) {
            outFen[0] = null;
            return wordCount;
        }
        int fullmove = CbCommon.readU16LE(payload, 4);
        if (fullmove <= 0) {
            fullmove = 1;
        }
        int turnCastles = CbCommon.readU16LE(payload, 6);
        int sideToMove = turnCastles & 0xFF;
        int castlingMask = (turnCastles >>> 8) & 0xFF;
        int epWord = CbCommon.readU16LE(payload, 8);

        int[] board = new int[64];
        Arrays.fill(board, CONSTANTS.EMPTY);

        int wIdx = 5;
        while (wIdx < wordCount) {
            int w = CbCommon.readU16LE(payload, wIdx * 2);
            wIdx++;
            if (w == 0xFFFC) {
                break;
            }
            if (w >= 0xC02D && w <= 0xC30C) {
                int id = w - 0xC000;
                if (id < 685) {
                    int typeIdx = (id - 45) / 64;
                    int cmSq = (id - 45) % 64;
                    int file = cmSq >> 3;
                    int rank = cmSq & 7;
                    int piece = decodePieceType(typeIdx);
                    if (piece != CONSTANTS.EMPTY) {
                        board[(rank << 3) | file] = piece;
                    }
                } else if (id < 733) {
                    int off = id - 685;
                    int file = off / 6;
                    int rank = (off % 6) + 1;
                    board[(rank << 3) | file] = CONSTANTS.WHITE_PAWN;
                } else {
                    int off = id - 733;
                    int file = off / 6;
                    int rank = (off % 6) + 1;
                    board[(rank << 3) | file] = CONSTANTS.BLACK_PAWN;
                }
            }
        }

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
                    sb.append(CbhCrypto.pieceToFenChar(p));
                }
            }
            if (empty > 0) {
                sb.append((char) ('0' + empty));
            }
            if (rank > 0) {
                sb.append('/');
            }
        }
        if (sideToMove == 1) {
            sb.append(" b ");
        } else {
            sb.append(" w ");
        }
        StringBuilder castles = new StringBuilder(4);
        if ((castlingMask & 0x02) != 0 && board[4] == CONSTANTS.WHITE_KING && board[7] == CONSTANTS.WHITE_ROOK) {
            castles.append('K');
        }
        if ((castlingMask & 0x01) != 0 && board[4] == CONSTANTS.WHITE_KING && board[0] == CONSTANTS.WHITE_ROOK) {
            castles.append('Q');
        }
        if ((castlingMask & 0x08) != 0 && board[60] == CONSTANTS.BLACK_KING && board[63] == CONSTANTS.BLACK_ROOK) {
            castles.append('k');
        }
        if ((castlingMask & 0x04) != 0 && board[60] == CONSTANTS.BLACK_KING && board[56] == CONSTANTS.BLACK_ROOK) {
            castles.append('q');
        }
        if (castles.isEmpty()) {
            sb.append('-');
        } else {
            sb.append(castles);
        }
        sb.append(' ');
        if (epWord >= 1 && epWord <= 8) {
            char epCol = (char) ('a' + (epWord - 1));
            char epRank;
            if (sideToMove == 1) {
                epRank = '3';
            } else {
                epRank = '6';
            }
            sb.append(epCol).append(epRank);
        } else {
            sb.append('-');
        }
        sb.append(" 0 ").append(fullmove);
        outFen[0] = sb.toString();
        return wIdx;
    }

    private static int decodePieceType(int typeIdx) {
        if (typeIdx == 0) {
            return CONSTANTS.WHITE_KING;
        } else if (typeIdx == 1) {
            return CONSTANTS.WHITE_QUEEN;
        } else if (typeIdx == 2) {
            return CONSTANTS.WHITE_KNIGHT;
        } else if (typeIdx == 3) {
            return CONSTANTS.WHITE_BISHOP;
        } else if (typeIdx == 4) {
            return CONSTANTS.WHITE_ROOK;
        } else if (typeIdx == 5) {
            return CONSTANTS.BLACK_KING;
        } else if (typeIdx == 6) {
            return CONSTANTS.BLACK_QUEEN;
        } else if (typeIdx == 7) {
            return CONSTANTS.BLACK_KNIGHT;
        } else if (typeIdx == 8) {
            return CONSTANTS.BLACK_BISHOP;
        } else if (typeIdx == 9) {
            return CONSTANTS.BLACK_ROOK;
        }
        return CONSTANTS.EMPTY;
    }
}
