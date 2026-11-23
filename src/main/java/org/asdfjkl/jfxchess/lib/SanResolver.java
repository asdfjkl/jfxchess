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

/**
 * Fast, targeted Standard Algebraic Notation (SAN) move resolver.
 * Disambiguates and validates moves by directly inspecting piece lists and
 * mailbox ray geometry, avoiding full move generation while guaranteeing
 * crash-safe error handling for ill-formatted PGN tokens.
 */
public final class SanResolver {

    private SanResolver() {
    }

    /**
     * Resolves a SAN move string into a legal {@link Move} on the given board.
     *
     * @param board current board state
     * @param rawSan raw SAN string (e.g. "e4", "Nf3", "Rad1", "Qh4e1", "exd5", "e8=Q", "0-0")
     * @return matching legal Move, or null if the move is illegal, ambiguous, or malformed
     */
    public static Move parseSan(Board board, String rawSan) {
        if (board == null || rawSan == null) {
            return null;
        }
        int len = rawSan.length();
        if (len == 0) {
            return null;
        }
        byte[] bytes = new byte[len];
        for (int i = 0; i < len; i++) {
            char c = rawSan.charAt(i);
            bytes[i] = (byte) (c < 128 ? c : '?');
        }
        return parseSan(board, bytes, 0, len);
    }

    /**
     * Resolves a SAN move directly from a raw ASCII byte slice into a legal {@link Move}.
     * Eliminates intermediate String and byte array heap allocations.
     *
     * @param board current board state
     * @param buf byte buffer containing the token
     * @param off start offset in buffer
     * @param len length in buffer
     * @return matching legal Move, or null if the move is illegal, ambiguous, or malformed
     */
    public static Move parseSan(Board board, byte[] buf, int off, int len) {
        if (board == null || buf == null || len <= 0 || off < 0 || off + len > buf.length) {
            return null;
        }

        // 1. Trim leading whitespace
        while (len > 0 && buf[off] <= ' ') {
            off++;
            len--;
        }

        // 2. Trim trailing whitespace, check/mate, annotations, trailing dots
        while (len > 0) {
            byte b = buf[off + len - 1];
            if (b <= ' ' || b == '+' || b == '#' || b == '!' || b == '?' || b == '.') {
                len--;
            } else {
                break;
            }
        }

        // 3. Strip trailing "e.p." or "ep" if present
        if (len >= 4 && (buf[off + len - 4] == 'e' || buf[off + len - 4] == 'E') &&
                buf[off + len - 3] == '.' &&
                (buf[off + len - 2] == 'p' || buf[off + len - 2] == 'P') &&
                buf[off + len - 1] == '.') {
            len -= 4;
        } else if (len >= 2 && (buf[off + len - 2] == 'e' || buf[off + len - 2] == 'E') &&
                (buf[off + len - 1] == 'p' || buf[off + len - 1] == 'P')) {
            len -= 2;
        }

        while (len > 0 && buf[off + len - 1] <= ' ') {
            len--;
        }

        if (len == 0) {
            return null;
        }

        try {
            // 4. Castling moves
            if (isKingsideCastle(buf, off, len)) {
                return resolveCastleKingside(board);
            }
            if (isQueensideCastle(buf, off, len)) {
                return resolveCastleQueenside(board);
            }

            // 5. Null moves
            if (isNullMove(buf, off, len)) {
                Move m = new Move();
                m.isNullMove = true;
                return m;
            }

            // 6. Long Algebraic Notation / Coordinate moves (e.g. "e2e4", "g1f3", "e7e8q")
            if (isCoordinateMove(buf, off, len)) {
                return resolveCoordinateMove(board, buf, off, len);
            }

            // 7. Pawn moves (starts with 'a' through 'h')
            byte firstByte = buf[off];
            if (firstByte >= 'a' && firstByte <= 'h') {
                return resolvePawnMove(board, buf, off, len);
            }

            // 8. Piece moves ('N', 'B', 'R', 'Q', 'K')
            int pieceType = decodePieceType((char) firstByte);
            if (pieceType != CONSTANTS.EMPTY) {
                return resolvePieceMove(board, buf, off, len, pieceType);
            }

            return null;
        } catch (Exception e) {
            // Defensive recovery: malformed PGN tokens must never crash the caller
            return null;
        }
    }

    private static boolean isKingsideCastle(byte[] buf, int off, int len) {
        if (len == 3) {
            byte b0 = buf[off];
            byte b1 = buf[off + 1];
            byte b2 = buf[off + 2];
            return (b1 == '-') &&
                    ((b0 == 'O' && b2 == 'O') || (b0 == '0' && b2 == '0') || (b0 == 'o' && b2 == 'o'));
        }
        return false;
    }

    private static boolean isQueensideCastle(byte[] buf, int off, int len) {
        if (len == 5) {
            byte b0 = buf[off];
            byte b1 = buf[off + 1];
            byte b2 = buf[off + 2];
            byte b3 = buf[off + 3];
            byte b4 = buf[off + 4];
            return (b1 == '-' && b3 == '-') &&
                    ((b0 == 'O' && b2 == 'O' && b4 == 'O') ||
                     (b0 == '0' && b2 == '0' && b4 == '0') ||
                     (b0 == 'o' && b2 == 'o' && b4 == 'o'));
        }
        return false;
    }

    private static boolean isNullMove(byte[] buf, int off, int len) {
        if (len == 2) {
            if (buf[off] == '-' && buf[off + 1] == '-') return true;
            if ((buf[off] == 'Z' || buf[off] == 'z') && buf[off + 1] == '0') return true;
        } else if (len == 4) {
            if (buf[off] == '0' && buf[off + 1] == '0' && buf[off + 2] == '0' && buf[off + 3] == '0') return true;
            if ((buf[off] == 'n' || buf[off] == 'N') &&
                (buf[off + 1] == 'u' || buf[off + 1] == 'U') &&
                (buf[off + 2] == 'l' || buf[off + 2] == 'L') &&
                (buf[off + 3] == 'l' || buf[off + 3] == 'L')) return true;
        }
        return false;
    }

    private static Move resolveCastleKingside(Board board) {
        if (board.turn == CONSTANTS.WHITE) {
            if (!board.canCastleWhiteKing()) {
                return null;
            }
            if (board.getInternalPieceAt(CONSTANTS.E1) != CONSTANTS.WHITE_KING ||
                    board.getInternalPieceAt(CONSTANTS.H1) != CONSTANTS.WHITE_ROOK ||
                    board.getInternalPieceAt(CONSTANTS.F1) != CONSTANTS.EMPTY ||
                    board.getInternalPieceAt(CONSTANTS.G1) != CONSTANTS.EMPTY) {
                return null;
            }
            Move m = new Move(CONSTANTS.E1, CONSTANTS.G1);
            return board.isPseudoALegal(m) ? m : null;
        } else {
            if (!board.canCastleBlackKing()) {
                return null;
            }
            if (board.getInternalPieceAt(CONSTANTS.E8) != CONSTANTS.BLACK_KING ||
                    board.getInternalPieceAt(CONSTANTS.H8) != CONSTANTS.BLACK_ROOK ||
                    board.getInternalPieceAt(CONSTANTS.F8) != CONSTANTS.EMPTY ||
                    board.getInternalPieceAt(CONSTANTS.G8) != CONSTANTS.EMPTY) {
                return null;
            }
            Move m = new Move(CONSTANTS.E8, CONSTANTS.G8);
            return board.isPseudoALegal(m) ? m : null;
        }
    }

    private static Move resolveCastleQueenside(Board board) {
        if (board.turn == CONSTANTS.WHITE) {
            if (!board.canCastleWhiteQueen()) {
                return null;
            }
            if (board.getInternalPieceAt(CONSTANTS.E1) != CONSTANTS.WHITE_KING ||
                    board.getInternalPieceAt(CONSTANTS.A1) != CONSTANTS.WHITE_ROOK ||
                    board.getInternalPieceAt(CONSTANTS.D1) != CONSTANTS.EMPTY ||
                    board.getInternalPieceAt(CONSTANTS.C1) != CONSTANTS.EMPTY ||
                    board.getInternalPieceAt(CONSTANTS.B1) != CONSTANTS.EMPTY) {
                return null;
            }
            Move m = new Move(CONSTANTS.E1, CONSTANTS.C1);
            return board.isPseudoALegal(m) ? m : null;
        } else {
            if (!board.canCastleBlackQueen()) {
                return null;
            }
            if (board.getInternalPieceAt(CONSTANTS.E8) != CONSTANTS.BLACK_KING ||
                    board.getInternalPieceAt(CONSTANTS.A8) != CONSTANTS.BLACK_ROOK ||
                    board.getInternalPieceAt(CONSTANTS.D8) != CONSTANTS.EMPTY ||
                    board.getInternalPieceAt(CONSTANTS.C8) != CONSTANTS.EMPTY ||
                    board.getInternalPieceAt(CONSTANTS.B8) != CONSTANTS.EMPTY) {
                return null;
            }
            Move m = new Move(CONSTANTS.E8, CONSTANTS.C8);
            return board.isPseudoALegal(m) ? m : null;
        }
    }

    private static boolean isCoordinateMove(byte[] buf, int off, int len) {
        int count = 0;
        byte f1 = 0, r1 = 0, f2 = 0, r2 = 0;
        for (int i = 0; i < len && count < 4; i++) {
            byte b = buf[off + i];
            if (b != '-' && b != 'x') {
                if (count == 0) f1 = b;
                else if (count == 1) r1 = b;
                else if (count == 2) f2 = b;
                else if (count == 3) r2 = b;
                count++;
            }
        }
        return (count == 4 && f1 >= 'a' && f1 <= 'h' && r1 >= '1' && r1 <= '8'
                && f2 >= 'a' && f2 <= 'h' && r2 >= '1' && r2 <= '8');
    }

    private static Move resolveCoordinateMove(Board board, byte[] buf, int off, int len) {
        int count = 0;
        int fromCol = -1, fromRow = -1, toCol = -1, toRow = -1;
        int promo = CONSTANTS.EMPTY;
        for (int i = 0; i < len; i++) {
            byte b = buf[off + i];
            if (b != '-' && b != 'x') {
                if (count == 0) fromCol = b - 'a';
                else if (count == 1) fromRow = b - '1';
                else if (count == 2) toCol = b - 'a';
                else if (count == 3) toRow = b - '1';
                else if (count == 4) promo = decodePromotionPiece((char) b);
                count++;
            }
        }
        if (fromCol < 0 || fromRow < 0 || toCol < 0 || toRow < 0) {
            return null;
        }

        int fromSq = Board.xyToInternal(fromCol, fromRow);
        int toSq = Board.xyToInternal(toCol, toRow);

        int piece = board.getInternalPieceAt(fromSq);
        if (piece == CONSTANTS.EMPTY || piece == CONSTANTS.FRINGE) {
            return null;
        }
        if (board.getPieceColorAt(fromSq) != board.turn) {
            return null;
        }

        Move m = (promo != CONSTANTS.EMPTY)
                ? new Move(fromSq, toSq, promo)
                : new Move(fromSq, toSq);

        return board.isPseudoALegal(m) ? m : null;
    }

    private static int decodePieceType(char c) {
        return switch (c) {
            case 'N' -> CONSTANTS.KNIGHT;
            case 'B' -> CONSTANTS.BISHOP;
            case 'R' -> CONSTANTS.ROOK;
            case 'Q' -> CONSTANTS.QUEEN;
            case 'K' -> CONSTANTS.KING;
            default -> CONSTANTS.EMPTY;
        };
    }

    private static int decodePromotionPiece(char c) {
        return switch (Character.toUpperCase(c)) {
            case 'Q' -> CONSTANTS.QUEEN;
            case 'R' -> CONSTANTS.ROOK;
            case 'B' -> CONSTANTS.BISHOP;
            case 'N' -> CONSTANTS.KNIGHT;
            default -> CONSTANTS.EMPTY;
        };
    }

    private static Move resolvePawnMove(Board board, byte[] buf, int off, int len) {
        int promoPiece = CONSTANTS.EMPTY;

        // Extract promotion piece: e8=Q, e8(Q), e8/Q, e8Q
        int equalIdx = -1;
        for (int i = 0; i < len; i++) {
            if (buf[off + i] == '=') {
                equalIdx = i;
                break;
            }
        }
        if (equalIdx >= 0 && equalIdx + 1 < len) {
            promoPiece = decodePromotionPiece((char) buf[off + equalIdx + 1]);
            len = equalIdx;
        } else if (len >= 4 && buf[off + len - 1] == ')' && buf[off + len - 3] == '(') {
            promoPiece = decodePromotionPiece((char) buf[off + len - 2]);
            len -= 3;
        } else {
            int slashIdx = -1;
            for (int i = 0; i < len; i++) {
                if (buf[off + i] == '/') {
                    slashIdx = i;
                    break;
                }
            }
            if (slashIdx >= 0 && slashIdx + 1 < len) {
                promoPiece = decodePromotionPiece((char) buf[off + slashIdx + 1]);
                len = slashIdx;
            } else if (len >= 3) {
                byte last = buf[off + len - 1];
                if (last == 'Q' || last == 'R' || last == 'B' || last == 'N' ||
                        last == 'q' || last == 'r' || last == 'b' || last == 'n') {
                    promoPiece = decodePromotionPiece((char) last);
                    len--;
                }
            }
        }

        // Case 1: Simple pawn push, e.g. "e4"
        if (len == 2 && buf[off] >= 'a' && buf[off] <= 'h'
                && buf[off + 1] >= '1' && buf[off + 1] <= '8') {
            int toCol = buf[off] - 'a';
            int toRow = buf[off + 1] - '1';
            return resolvePawnPush(board, toCol, toRow, promoPiece);
        }

        // Case 2: Pawn capture, e.g. "exd5", "ed4", "exf6", "ed"
        int fromCol = buf[off] - 'a';
        int toCol = -1;
        int toRow = -1;

        if (len == 4 && buf[off + 1] == 'x'
                && buf[off + 2] >= 'a' && buf[off + 2] <= 'h'
                && buf[off + 3] >= '1' && buf[off + 3] <= '8') {
            toCol = buf[off + 2] - 'a';
            toRow = buf[off + 3] - '1';
        } else if (len == 3 && buf[off + 1] >= 'a' && buf[off + 1] <= 'h'
                && buf[off + 2] >= '1' && buf[off + 2] <= '8') {
            toCol = buf[off + 1] - 'a';
            toRow = buf[off + 2] - '1';
        } else if (len == 2 && buf[off + 1] >= 'a' && buf[off + 1] <= 'h') {
            toCol = buf[off + 1] - 'a';
            return resolveRanklessPawnCapture(board, fromCol, toCol, promoPiece);
        }

        if (fromCol >= 0 && fromCol <= 7 && toCol >= 0 && toCol <= 7 && toRow >= 0 && toRow <= 7) {
            return resolvePawnCapture(board, fromCol, toCol, toRow, promoPiece);
        }

        return null;
    }

    private static Move resolvePawnPush(Board board, int toCol, int toRow, int promoPiece) {
        int toSq = Board.xyToInternal(toCol, toRow);
        if (board.getInternalPieceAt(toSq) != CONSTANTS.EMPTY) {
            return null; // pawn push target must be empty
        }

        if (board.turn == CONSTANTS.WHITE) {
            // 1 step back
            if (toRow >= 1) {
                int fromSq1 = Board.xyToInternal(toCol, toRow - 1);
                if (board.getInternalPieceAt(fromSq1) == CONSTANTS.WHITE_PAWN) {
                    int p = (toRow == 7 && promoPiece == CONSTANTS.EMPTY) ? CONSTANTS.QUEEN : promoPiece;
                    Move m = new Move(fromSq1, toSq, p);
                    if (board.isPseudoALegal(m)) {
                        return m;
                    }
                }
            }
            // 2 steps back from rank 2 to rank 4
            if (toRow == 3) {
                int intermediateSq = Board.xyToInternal(toCol, 2);
                int fromSq2 = Board.xyToInternal(toCol, 1);
                if (board.getInternalPieceAt(intermediateSq) == CONSTANTS.EMPTY &&
                        board.getInternalPieceAt(fromSq2) == CONSTANTS.WHITE_PAWN) {
                    Move m = new Move(fromSq2, toSq);
                    if (board.isPseudoALegal(m)) {
                        return m;
                    }
                }
            }
        } else {
            // Black pawn push
            // 1 step back
            if (toRow <= 6) {
                int fromSq1 = Board.xyToInternal(toCol, toRow + 1);
                if (board.getInternalPieceAt(fromSq1) == CONSTANTS.BLACK_PAWN) {
                    int p = (toRow == 0 && promoPiece == CONSTANTS.EMPTY) ? CONSTANTS.QUEEN : promoPiece;
                    Move m = new Move(fromSq1, toSq, p);
                    if (board.isPseudoALegal(m)) {
                        return m;
                    }
                }
            }
            // 2 steps back from rank 7 to rank 5
            if (toRow == 4) {
                int intermediateSq = Board.xyToInternal(toCol, 5);
                int fromSq2 = Board.xyToInternal(toCol, 6);
                if (board.getInternalPieceAt(intermediateSq) == CONSTANTS.EMPTY &&
                        board.getInternalPieceAt(fromSq2) == CONSTANTS.BLACK_PAWN) {
                    Move m = new Move(fromSq2, toSq);
                    if (board.isPseudoALegal(m)) {
                        return m;
                    }
                }
            }
        }

        return null;
    }

    private static Move resolvePawnCapture(Board board, int fromCol, int toCol, int toRow, int promoPiece) {
        if (Math.abs(toCol - fromCol) != 1) {
            return null; // pawns can only capture on adjacent columns
        }

        int fromRow = (board.turn == CONSTANTS.WHITE) ? toRow - 1 : toRow + 1;
        if (fromRow < 0 || fromRow > 7) {
            return null;
        }

        int fromSq = Board.xyToInternal(fromCol, fromRow);
        int toSq = Board.xyToInternal(toCol, toRow);

        int expectedPawn = (board.turn == CONSTANTS.WHITE) ? CONSTANTS.WHITE_PAWN : CONSTANTS.BLACK_PAWN;
        if (board.getInternalPieceAt(fromSq) != expectedPawn) {
            return null;
        }

        int targetPiece = board.getInternalPieceAt(toSq);
        boolean isEp = (toSq == board.getEpTarget());

        if (targetPiece == CONSTANTS.EMPTY && !isEp) {
            return null; // capture target must be non-empty or ep
        }
        if (targetPiece != CONSTANTS.EMPTY && board.getPieceColorAt(toSq) == board.turn) {
            return null; // cannot capture own piece
        }

        int p = promoPiece;
        if ((board.turn == CONSTANTS.WHITE && toRow == 7) || (board.turn == CONSTANTS.BLACK && toRow == 0)) {
            if (p == CONSTANTS.EMPTY) {
                p = CONSTANTS.QUEEN;
            }
        }

        Move m = new Move(fromSq, toSq, p);
        return board.isPseudoALegal(m) ? m : null;
    }

    private static Move resolveRanklessPawnCapture(Board board, int fromCol, int toCol, int promoPiece) {
        if (Math.abs(toCol - fromCol) != 1) {
            return null;
        }
        Move matched = null;
        int count = 0;
        for (int r = 0; r < 8; r++) {
            Move m = resolvePawnCapture(board, fromCol, toCol, r, promoPiece);
            if (m != null) {
                count++;
                matched = m;
            }
        }
        return (count == 1) ? matched : null;
    }

    private static Move resolvePieceMove(Board board, byte[] buf, int off, int len, int pieceType) {
        if (len < 3) {
            return null;
        }

        // The destination square is always the last 2 characters
        byte toColByte = buf[off + len - 2];
        byte toRowByte = buf[off + len - 1];
        if (toColByte < 'a' || toColByte > 'h' || toRowByte < '1' || toRowByte > '8') {
            return null;
        }

        int toCol = toColByte - 'a';
        int toRow = toRowByte - '1';
        int toSq = Board.xyToInternal(toCol, toRow);

        // Target square cannot contain own piece
        int targetPiece = board.getInternalPieceAt(toSq);
        if (targetPiece != CONSTANTS.EMPTY && board.getPieceColorAt(toSq) == board.turn) {
            return null;
        }

        // Remainder between piece letter and destination square holds disambiguation and optional 'x' / '-'
        int disambigCol = -1;
        int disambigRow = -1;
        int dCount = 0;
        byte d1 = 0, d2 = 0;

        for (int i = off + 1; i < off + len - 2; i++) {
            byte b = buf[i];
            if (b != 'x' && b != '-') {
                if (dCount == 0) d1 = b;
                else if (dCount == 1) d2 = b;
                dCount++;
            }
        }

        if (dCount == 1) {
            if (d1 >= 'a' && d1 <= 'h') {
                disambigCol = d1 - 'a';
            } else if (d1 >= '1' && d1 <= '8') {
                disambigRow = d1 - '1';
            } else {
                return null;
            }
        } else if (dCount == 2) {
            if (d1 >= 'a' && d1 <= 'h' && d2 >= '1' && d2 <= '8') {
                disambigCol = d1 - 'a';
                disambigRow = d2 - '1';
            } else {
                return null;
            }
        } else if (dCount > 2) {
            return null; // invalid disambiguation syntax
        }

        // Search the player's piece list for the specified piece type
        int color = board.turn ? CONSTANTS.IBLACK : CONSTANTS.IWHITE;
        int[][][] pieceList = board.getPieceList();
        if (pieceList == null) {
            return null;
        }

        Move matchedMove = null;
        int matchCount = 0;

        for (int i = 0; i < 10; i++) {
            int fromSq = pieceList[color][pieceType][i];
            if (fromSq == CONSTANTS.EMPTY) {
                break;
            }

            int fromCol = (fromSq % 10) - 1;
            int fromRow = (fromSq / 10) - 2;

            if (disambigCol != -1 && fromCol != disambigCol) {
                continue;
            }
            if (disambigRow != -1 && fromRow != disambigRow) {
                continue;
            }

            if (!canPieceReach(board, pieceType, fromSq, toSq, fromCol, fromRow, toCol, toRow)) {
                continue;
            }

            Move candidate = new Move(fromSq, toSq);
            if (!board.isPseudoALegal(candidate)) {
                continue;
            }

            matchCount++;
            matchedMove = candidate;
        }

        return (matchCount == 1) ? matchedMove : null;
    }

    private static boolean canPieceReach(Board board, int pieceType, int fromSq, int toSq,
                                         int fromCol, int fromRow, int toCol, int toRow) {
        if (fromSq == toSq) {
            return false;
        }
        int colDiff = toCol - fromCol;
        int rowDiff = toRow - fromRow;
        int absCol = Math.abs(colDiff);
        int absRow = Math.abs(rowDiff);

        switch (pieceType) {
            case CONSTANTS.KNIGHT:
                return (absCol == 1 && absRow == 2) || (absCol == 2 && absRow == 1);

            case CONSTANTS.KING:
                return absCol <= 1 && absRow <= 1;

            case CONSTANTS.BISHOP:
                if (absCol != absRow) {
                    return false;
                }
                int bStep = (rowDiff > 0 ? 10 : -10) + (colDiff > 0 ? 1 : -1);
                return isRayClear(board, fromSq, toSq, bStep);

            case CONSTANTS.ROOK:
                if (absCol != 0 && absRow != 0) {
                    return false;
                }
                int rStep = (absCol == 0) ? (rowDiff > 0 ? 10 : -10) : (colDiff > 0 ? 1 : -1);
                return isRayClear(board, fromSq, toSq, rStep);

            case CONSTANTS.QUEEN:
                if (absCol == absRow) {
                    int qBStep = (rowDiff > 0 ? 10 : -10) + (colDiff > 0 ? 1 : -1);
                    return isRayClear(board, fromSq, toSq, qBStep);
                } else if (absCol == 0 || absRow == 0) {
                    int qRStep = (absCol == 0) ? (rowDiff > 0 ? 10 : -10) : (colDiff > 0 ? 1 : -1);
                    return isRayClear(board, fromSq, toSq, qRStep);
                }
                return false;

            default:
                return false;
        }
    }

    private static boolean isRayClear(Board board, int fromSq, int toSq, int step) {
        for (int sq = fromSq + step; sq != toSq; sq += step) {
            if (board.getInternalPieceAt(sq) != CONSTANTS.EMPTY) {
                return false;
            }
        }
        return true;
    }
}
