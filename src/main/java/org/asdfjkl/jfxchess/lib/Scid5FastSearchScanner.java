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

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

/**
 * Fast, allocation-free move stream decoder and board comparator for SCID5 databases.
 * Directly decodes compressed move streams from .sg5 into a reusable ScidPosition,
 * checking target position matches in microseconds without allocating Game/Node/Move objects.
 */
public class Scid5FastSearchScanner {

    private final int[] targetPieces = new int[64];
    private final int searchTurn;
    private final boolean searchPosition;
    private final boolean hasWildcards;

    private final int targetWhiteHomePawns;
    private final int targetBlackHomePawns;
    private final int targetWhitePieces;
    private final int targetBlackPieces;
    private final boolean canCheckHomePawns;

    private static final int[] KING_DIFFS = {0, -9, -8, -7, -1, 1, 7, 8, 9};
    private static final int[] KNIGHT_SQDIFF = {0, -17, -15, -10, -6, 6, 10, 15, 17, 0, 0, 0, 0, 0, 0, 0};
    private static final int[] PAWN_SQDIFF = {7, 8, 9, 7, 8, 9, 7, 8, 9, 7, 8, 9, 7, 8, 9, 16};
    private static final int[] PAWN_PROMO = {
        ScidPosition.EMPTY, ScidPosition.EMPTY, ScidPosition.EMPTY,
        ScidPosition.QUEEN, ScidPosition.QUEEN, ScidPosition.QUEEN,
        ScidPosition.ROOK, ScidPosition.ROOK, ScidPosition.ROOK,
        ScidPosition.BISHOP, ScidPosition.BISHOP, ScidPosition.BISHOP,
        ScidPosition.KNIGHT, ScidPosition.KNIGHT, ScidPosition.KNIGHT,
        ScidPosition.EMPTY
    };

    private static final int[] SCID_TO_JFX = {
        CONSTANTS.EMPTY,        // 0 = EMPTY
        CONSTANTS.WHITE_KING,   // 1 = KING (0x06)
        CONSTANTS.WHITE_QUEEN,  // 2 = QUEEN (0x05)
        CONSTANTS.WHITE_ROOK,   // 3 = ROOK (0x04)
        CONSTANTS.WHITE_BISHOP, // 4 = BISHOP (0x03)
        CONSTANTS.WHITE_KNIGHT, // 5 = KNIGHT (0x02)
        CONSTANTS.WHITE_PAWN    // 6 = PAWN (0x01)
    };

    private final ScidPosition scidPos = new ScidPosition();
    private final ScidPosition[] posHistory = new ScidPosition[512];
    private final int[] varStack = new int[64];
    private byte[] buffer = new byte[65536];

    public Scid5FastSearchScanner(SearchPattern pattern) {
        this.searchPosition = pattern.isSearchPosition();
        this.searchTurn = pattern.getSearchTurn();
        Board searchBoard = pattern.getSearchBoard();

        boolean wildcards = false;
        int wHome = 0;
        int bHome = 0;
        int wCount = 0;
        int bCount = 0;
        boolean disableHomePawn = false;

        if (searchBoard != null) {
            for (int sq = 0; sq < 64; sq++) {
                int p = searchBoard.getPieceAt(sq & 7, sq >> 3);
                targetPieces[sq] = p;
                if (p == CONSTANTS.WHITE_ANY_PIECE || p == CONSTANTS.BLACK_ANY_PIECE) {
                    wildcards = true;
                }
                if (p >= CONSTANTS.WHITE_PAWN && p <= CONSTANTS.WHITE_KING) wCount++;
                if (p >= CONSTANTS.BLACK_PAWN && p <= CONSTANTS.BLACK_KING) bCount++;
            }

            for (int col = 0; col < 8; col++) {
                int wp = searchBoard.getPieceAt(col, 1);
                if (wp == CONSTANTS.WHITE_ANY_PIECE) {
                    disableHomePawn = true;
                } else if (wp == CONSTANTS.WHITE_PAWN) {
                    wHome |= (1 << col);
                }
                int bp = searchBoard.getPieceAt(col, 6);
                if (bp == CONSTANTS.BLACK_ANY_PIECE) {
                    disableHomePawn = true;
                } else if (bp == CONSTANTS.BLACK_PAWN) {
                    bHome |= (1 << col);
                }
            }
        }

        this.hasWildcards = wildcards;
        this.targetWhiteHomePawns = wHome;
        this.targetBlackHomePawns = bHome;
        this.targetWhitePieces = wCount;
        this.targetBlackPieces = bCount;
        this.canCheckHomePawns = !disableHomePawn;

        for (int i = 0; i < posHistory.length; i++) {
            posHistory[i] = new ScidPosition();
        }
    }

    public boolean matches(ScidPosition pos) {
        if (!searchPosition) {
            return true;
        }
        if (searchTurn == SearchPattern.TURN_WHITE && pos.toMove != ScidPosition.WHITE) {
            return false;
        }
        if (searchTurn == SearchPattern.TURN_BLACK && pos.toMove != ScidPosition.BLACK) {
            return false;
        }
        for (int sq = 0; sq < 64; sq++) {
            int tgt = targetPieces[sq];
            int scidType = pos.board[sq];
            if (tgt == CONSTANTS.EMPTY) {
                if (scidType != ScidPosition.EMPTY) return false;
            } else if (tgt == CONSTANTS.WHITE_ANY_PIECE) {
                if (scidType == ScidPosition.EMPTY || pos.color[sq] != ScidPosition.WHITE) return false;
            } else if (tgt == CONSTANTS.BLACK_ANY_PIECE) {
                if (scidType == ScidPosition.EMPTY || pos.color[sq] != ScidPosition.BLACK) return false;
            } else {
                if (scidType == ScidPosition.EMPTY) return false;
                int jfxPiece = (pos.color[sq] == ScidPosition.WHITE) ? SCID_TO_JFX[scidType] : (SCID_TO_JFX[scidType] | 0x80);
                if (jfxPiece != tgt) return false;
            }
        }
        return true;
    }

    public boolean scanGame(FileChannel channel, Scid5GameInfo info) {
        int length = info.getSg5Length();
        if (length <= 0) return false;
        long offset = info.getSg5Offset();

        if (buffer.length < length) {
            buffer = new byte[Math.max(length, buffer.length * 2)];
        }
        ByteBuffer bb = ByteBuffer.wrap(buffer, 0, length);
        try {
            int read = channel.read(bb, offset);
            if (read < length) return false;
        } catch (Exception e) {
            return false;
        }

        return scan(buffer, 0, length);
    }

    public boolean scan(byte[] sg5, int offset, int dataSize) {
        int pos = offset;
        int end = offset + dataSize;

        // 1. Extra tags section
        while (pos < end) {
            int b = sg5[pos++] & 0xFF;
            if (b == 0) {
                break;
            }
            if (b >= 241 && b <= 250) {
                int valLen = sg5[pos++] & 0xFF;
                pos += valLen;
            } else {
                int nameLen = b;
                pos += nameLen;
                int valLen = sg5[pos++] & 0xFF;
                pos += valLen;
            }
        }

        // 2. Start board section
        if (pos >= end) return false;
        int startFlags = sg5[pos++] & 0xFF;
        boolean standardStart = (startFlags & 1) == 0;
        int curWhiteHomePawns = 0xFF;
        int curBlackHomePawns = 0xFF;
        boolean checkHomePawns = standardStart && canCheckHomePawns;

        if (!standardStart) {
            int fenStart = pos;
            while (pos < end && sg5[pos] != 0) {
                pos++;
            }
            String fen = new String(sg5, fenStart, pos - fenStart, StandardCharsets.US_ASCII);
            pos++; // skip null char
            scidPos.setupFen(fen);
        } else {
            scidPos.stdStart();
        }

        if (matches(scidPos)) {
            return true;
        }

        int ply = 0;
        posHistory[ply++].copyFrom(scidPos);
        int varDepth = 0;

        // 3. Move stream
        while (pos < end) {
            int b = sg5[pos++] & 0xFF;
            if (b == 0x0F) {
                break;
            }

            int pieceNum = (b >> 4) & 0x0F;
            int moveCode = b & 0x0F;

            int from = -1;
            int to = -1;

            if (pieceNum == 0) {
                if (moveCode == 0) {
                    // Null move
                    scidPos.toMove = 1 - scidPos.toMove;
                } else if (moveCode <= 8) {
                    // King step
                    from = scidPos.list[scidPos.toMove][0];
                    to = from + KING_DIFFS[moveCode];
                    scidPos.doMove(from, to, ScidPosition.EMPTY, false, false);
                } else if (moveCode == 9) {
                    // Queenside Castle (O-O-O)
                    from = scidPos.list[scidPos.toMove][0];
                    to = (scidPos.toMove == ScidPosition.WHITE) ? 2 : 58;
                    scidPos.doMove(from, to, ScidPosition.EMPTY, false, true);
                } else if (moveCode == 10) {
                    // Kingside Castle (O-O)
                    from = scidPos.list[scidPos.toMove][0];
                    to = (scidPos.toMove == ScidPosition.WHITE) ? 6 : 62;
                    scidPos.doMove(from, to, ScidPosition.EMPTY, true, false);
                } else if (moveCode == 11) {
                    pos++; // skip NAG byte
                    continue;
                } else if (moveCode == 12) {
                    continue; // comment
                } else if (moveCode == 13) {
                    // Start Variation '('
                    if (varDepth < varStack.length) {
                        varStack[varDepth++] = ply;
                    }
                    if (ply >= 2) {
                        scidPos.copyFrom(posHistory[ply - 2]);
                    }
                    continue;
                } else if (moveCode == 14) {
                    // End Variation ')'
                    if (varDepth > 0) {
                        ply = varStack[--varDepth];
                        scidPos.copyFrom(posHistory[ply - 1]);
                    }
                    continue;
                }
            } else {
                from = scidPos.list[scidPos.toMove][pieceNum];
                int pieceType = scidPos.board[from];
                int promo = ScidPosition.EMPTY;

                switch (pieceType) {
                    case ScidPosition.PAWN -> {
                        to = (scidPos.toMove == ScidPosition.WHITE) ? (from + PAWN_SQDIFF[moveCode]) : (from - PAWN_SQDIFF[moveCode]);
                        promo = PAWN_PROMO[moveCode];
                    }
                    case ScidPosition.BISHOP -> {
                        int targetFile = (moveCode >= 8) ? (moveCode - 8) : moveCode;
                        int fylediff = targetFile - (from & 7);
                        to = (moveCode >= 8) ? (from - 7 * fylediff) : (from + 9 * fylediff);
                    }
                    case ScidPosition.KNIGHT -> {
                        to = from + KNIGHT_SQDIFF[moveCode];
                    }
                    case ScidPosition.QUEEN -> {
                        if (moveCode == (from & 7)) {
                            int b2 = sg5[pos++] & 0xFF;
                            to = b2 - 64;
                        } else if (moveCode >= 8) {
                            to = ((moveCode - 8) << 3) | (from & 7);
                        } else {
                            to = (from & ~7) | moveCode;
                        }
                    }
                    case ScidPosition.ROOK -> {
                        if (moveCode >= 8) {
                            to = ((moveCode - 8) << 3) | (from & 7);
                        } else {
                            to = (from & ~7) | moveCode;
                        }
                    }
                }

                scidPos.doMove(from, to, promo, false, false);
            }

            if (matches(scidPos)) {
                return true;
            }

            // Mid-game pruning:
            if (checkHomePawns && varDepth == 0) {
                if (from >= 8 && from <= 15) {
                    curWhiteHomePawns &= ~(1 << (from - 8));
                    if ((curWhiteHomePawns & targetWhiteHomePawns) != targetWhiteHomePawns) {
                        break; // A required white home pawn left its rank!
                    }
                }
                if (from >= 48 && from <= 55) {
                    curBlackHomePawns &= ~(1 << (from - 48));
                    if ((curBlackHomePawns & targetBlackHomePawns) != targetBlackHomePawns) {
                        break; // A required black home pawn left its rank!
                    }
                }
                if (to >= 8 && to <= 15 && (targetWhiteHomePawns & (1 << (to - 8))) != 0) {
                    curWhiteHomePawns &= ~(1 << (to - 8));
                    break; // Required white home pawn captured!
                }
                if (to >= 48 && to <= 55 && (targetBlackHomePawns & (1 << (to - 48))) != 0) {
                    curBlackHomePawns &= ~(1 << (to - 48));
                    break; // Required black home pawn captured!
                }
            }

            if (!hasWildcards && varDepth == 0) {
                if (scidPos.count[ScidPosition.WHITE] < targetWhitePieces ||
                    scidPos.count[ScidPosition.BLACK] < targetBlackPieces) {
                    break; // Insufficient material to reach target!
                }
            }

            if (ply < posHistory.length) {
                posHistory[ply++].copyFrom(scidPos);
            }
        }

        return false;
    }
}
