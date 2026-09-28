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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.asdfjkl.jfxchess.lib.ScidPosition.*;

/**
 * Encodes {@link Game} objects into SCID 5 (.sg5) binary blobs and calculates
 * the required metadata for index (.si5) records.
 */
public class Scid5MoveEncoder {

    // Common tags table (codes 241..250)
    private static final String[] COMMON_TAGS = {
            "WhiteCountry", "BlackCountry", "Annotator", "PlyCount",
            "EventDate", "Opening", "Variation", "Setup", "Source", "SetUp"
    };

    private static final Map<String, Integer> COMMON_TAG_MAP = new HashMap<>();
    static {
        for (int i = 0; i < COMMON_TAGS.length; i++) {
            COMMON_TAG_MAP.put(COMMON_TAGS[i], 241 + i);
        }
    }

    // Tags that are excluded from Section 1 (stored in .si5, .sn5, or Section 2)
    private static final Set<String> EXCLUDED_TAGS = new HashSet<>(Arrays.asList(
            "Event", "Site", "Date", "Round", "White", "Black", "Result",
            "ECO", "WhiteElo", "BlackElo", "FEN", "SetUp", "Setup"
    ));

    // Rating threshold table for non-linear count rating codes (0..15)
    private static final int[] RATING_THRESHOLDS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 30, 40, 50
    };

    // Home pawn starting square to 4-bit nibble mapping (0..15)
    // -1 indicates not a home pawn square
    private static final int[] SQ_TO_HP_NIBBLE = new int[64];
    static {
        Arrays.fill(SQ_TO_HP_NIBBLE, -1);
        // Black pawns on rank 7 (squares 48..55)
        SQ_TO_HP_NIBBLE[(6 << 3) | 7] = 0; // h7
        SQ_TO_HP_NIBBLE[(6 << 3) | 6] = 1; // g7
        SQ_TO_HP_NIBBLE[(6 << 3) | 5] = 2; // f7
        SQ_TO_HP_NIBBLE[(6 << 3) | 4] = 3; // e7
        SQ_TO_HP_NIBBLE[(6 << 3) | 3] = 4; // d7
        SQ_TO_HP_NIBBLE[(6 << 3) | 2] = 5; // c7
        SQ_TO_HP_NIBBLE[(6 << 3) | 1] = 6; // b7
        SQ_TO_HP_NIBBLE[(6 << 3) | 0] = 7; // a7

        // White pawns on rank 2 (squares 8..15)
        SQ_TO_HP_NIBBLE[(1 << 3) | 7] = 8;  // h2
        SQ_TO_HP_NIBBLE[(1 << 3) | 6] = 9;  // g2
        SQ_TO_HP_NIBBLE[(1 << 3) | 5] = 10; // f2
        SQ_TO_HP_NIBBLE[(1 << 3) | 4] = 11; // e2
        SQ_TO_HP_NIBBLE[(1 << 3) | 3] = 12; // d2
        SQ_TO_HP_NIBBLE[(1 << 3) | 2] = 13; // c2
        SQ_TO_HP_NIBBLE[(1 << 3) | 1] = 14; // b2
        SQ_TO_HP_NIBBLE[(1 << 3) | 0] = 15; // a2
    }

    /**
     * Encodes a {@link Game} object into SCID 5 binary blob format (.sg5) and
     * computes the associated metadata fields for the index (.si5).
     *
     * @param game the game to encode
     * @return the result containing serialized bytes and index metadata
     */
    public static Scid5EncodeResult encode(Game game) {
        Scid5EncodeResult result = new Scid5EncodeResult();

        // 1. Encode Section 1: Extra tags
        ByteArrayOutputStream tagsStream = new ByteArrayOutputStream();
        encodeExtraTags(game, tagsStream);

        // 2. Check starting position
        boolean hasCustomStart = false;
        String customFen = null;
        Board rootBoard = game.getRootNode().getBoard();
        if (rootBoard != null && !rootBoard.isInitialPosition()) {
            hasCustomStart = true;
            customFen = rootBoard.fen();
        } else {
            String fenHeader = game.getHeader("FEN");
            if (fenHeader != null && !fenHeader.isBlank()) {
                if (!fenHeader.trim().startsWith("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq")) {
                    hasCustomStart = true;
                    customFen = fenHeader.trim();
                }
            }
            if (!hasCustomStart && ("1".equals(game.getHeader("SetUp")) || "1".equals(game.getHeader("Setup")))) {
                hasCustomStart = true;
                customFen = (rootBoard != null) ? rootBoard.fen() : null;
            }
        }

        ScidPosition scidPos = new ScidPosition();
        if (hasCustomStart) {
            result.flags |= Scid5GameInfo.FLAG_CUSTOM_START;
            scidPos.setupFen(customFen);
        } else {
            scidPos.stdStart();
        }

        // 3. Encode Section 3: Move stream & collect comments
        ByteArrayOutputStream movesStream = new ByteArrayOutputStream();
        List<String> comments = new ArrayList<>();
        List<Integer> hpDepartures = new ArrayList<>();
        int[] stats = new int[4]; // [0]=commentCount, [1]=variationCount, [2]=nagCount, [3]=mainlinePlies
        int[] flagsHolder = new int[]{result.flags};
        int[] homePawnMask = new int[]{0xFFFF};

        // Root node comments and NAGs
        GameNode root = game.getRootNode();
        if (root.getComment() != null && !root.getComment().isEmpty()) {
            movesStream.write(0x0C);
            comments.add(root.getComment());
            stats[0]++;
        }
        for (int nag : root.getNags()) {
            movesStream.write(0x0B);
            movesStream.write(nag & 0xFF);
            stats[2]++;
        }

        // Recursively encode move tree
        ScidPosition endPos = encodeNode(
                root, scidPos, movesStream, comments, hpDepartures,
                homePawnMask, stats, flagsHolder, true
        );

        // End of game marker (0x0F)
        movesStream.write(0x0F);

        // 4. Encode Section 2: Start board (flags + optional FEN)
        result.flags = flagsHolder[0];
        ByteArrayOutputStream startBoardStream = new ByteArrayOutputStream();
        int startFlags = 0;
        if (hasCustomStart) startFlags |= 1;
        if ((result.flags & Scid5GameInfo.FLAG_PROMO) != 0) startFlags |= 2;
        if ((result.flags & Scid5GameInfo.FLAG_UNDER_PROMO) != 0) startFlags |= 4;
        startBoardStream.write(startFlags);
        if (hasCustomStart && customFen != null) {
            startBoardStream.writeBytes(customFen.getBytes(StandardCharsets.US_ASCII));
            startBoardStream.write(0x00);
        }

        // 5. Encode Section 4: Comments
        ByteArrayOutputStream commentsStream = new ByteArrayOutputStream();
        for (String c : comments) {
            commentsStream.writeBytes(c.getBytes(StandardCharsets.UTF_8));
            commentsStream.write(0x00);
        }

        // Combine all sections into final data blob
        ByteArrayOutputStream fullBlob = new ByteArrayOutputStream();
        fullBlob.writeBytes(tagsStream.toByteArray());
        fullBlob.writeBytes(startBoardStream.toByteArray());
        fullBlob.writeBytes(movesStream.toByteArray());
        fullBlob.writeBytes(commentsStream.toByteArray());

        result.data = fullBlob.toByteArray();
        result.dataSize = result.data.length;

        // 6. Compute metadata for .si5
        result.halfMoves = Math.min(stats[3], 1023);
        result.commentRating = encodeRating(stats[0]);
        result.variationRating = encodeRating(stats[1]);
        result.nagRating = encodeRating(stats[2]);

        // Home pawn departures
        result.homePawnCount = Math.min(hpDepartures.size(), 16);
        for (int i = 0; i < result.homePawnCount; i++) {
            int byteIdx = i / 2;
            int nibble = hpDepartures.get(i);
            if (i % 2 == 0) {
                result.homePawnData[byteIdx] |= (byte) ((nibble & 0x0F) << 4);
            } else {
                result.homePawnData[byteIdx] |= (byte) (nibble & 0x0F);
            }
        }

        // Final material signature
        result.finalMatSig = computeFinalMatSig(endPos != null ? endPos : scidPos);

        return result;
    }

    private static void encodeExtraTags(Game game, ByteArrayOutputStream out) {
        HashMap<String, String> headers = game.getPgnHeaders();
        if (headers == null || headers.isEmpty()) {
            out.write(0x00);
            return;
        }

        // Sort keys for deterministic output
        List<String> keys = new ArrayList<>(headers.keySet());
        Collections.sort(keys);

        for (String key : keys) {
            if (EXCLUDED_TAGS.contains(key)) {
                continue;
            }
            String val = headers.get(key);
            if (val == null || val.isEmpty()) {
                continue;
            }

            Integer commonCode = COMMON_TAG_MAP.get(key);
            byte[] valBytes = val.getBytes(StandardCharsets.UTF_8);
            if (valBytes.length > 255) {
                valBytes = Arrays.copyOf(valBytes, 255);
            }

            if (commonCode != null) {
                out.write(commonCode);
                out.write(valBytes.length & 0xFF);
                out.writeBytes(valBytes);
            } else {
                byte[] nameBytes = key.getBytes(StandardCharsets.UTF_8);
                if (nameBytes.length == 0 || nameBytes.length > 240) {
                    continue;
                }
                out.write(nameBytes.length & 0xFF);
                out.writeBytes(nameBytes);
                out.write(valBytes.length & 0xFF);
                out.writeBytes(valBytes);
            }
        }
        out.write(0x00); // Delimiter ending extra tags
    }

    private static ScidPosition encodeNode(
            GameNode current,
            ScidPosition scidPos,
            ByteArrayOutputStream movesStream,
            List<String> comments,
            List<Integer> hpDepartures,
            int[] homePawnMask,
            int[] stats,
            int[] flagsHolder,
            boolean isMainline) {

        int numChildren = current.getVariations().size();
        if (numChildren == 0) {
            return scidPos;
        }

        GameNode mainChild = current.getVariation(0);
        ScidPosition posAtCurrent = scidPos.copy();

        // 1. Encode mainline move
        encodeSingleMove(mainChild, scidPos, movesStream, comments, hpDepartures,
                homePawnMask, stats, flagsHolder, isMainline);
        if (isMainline) {
            stats[3]++; // mainline plies
        }

        // 2. Encode sideline variations from current node
        for (int i = 1; i < numChildren; i++) {
            GameNode sideChild = current.getVariation(i);
            stats[1]++; // variation count
            movesStream.write(0x0D); // Start Variation '('

            ScidPosition sidePos = posAtCurrent.copy();
            encodeSingleMove(sideChild, sidePos, movesStream, comments, hpDepartures,
                    homePawnMask, stats, flagsHolder, false);

            encodeNode(sideChild, sidePos, movesStream, comments, hpDepartures,
                    homePawnMask, stats, flagsHolder, false);

            movesStream.write(0x0E); // End Variation ')'
        }

        // 3. Recurse down mainline child
        return encodeNode(mainChild, scidPos, movesStream, comments, hpDepartures,
                homePawnMask, stats, flagsHolder, isMainline);
    }

    private static void encodeSingleMove(
            GameNode node,
            ScidPosition scidPos,
            ByteArrayOutputStream movesStream,
            List<String> comments,
            List<Integer> hpDepartures,
            int[] homePawnMask,
            int[] stats,
            int[] flagsHolder,
            boolean isMainline) {

        Move m = node.getMove();
        if (m == null || m.isNullMove) {
            movesStream.write(0x00);
            scidPos.toMove = 1 - scidPos.toMove;
        } else {
            int from = internalToSq64(m.getMoveSourceSquare());
            int to = internalToSq64(m.getMoveTargetSquare());
            int promo = promoToScidPiece(m.promotionPiece);

            if (promo == QUEEN) {
                flagsHolder[0] |= Scid5GameInfo.FLAG_PROMO;
            } else if (promo != EMPTY) {
                flagsHolder[0] |= Scid5GameInfo.FLAG_UNDER_PROMO;
            }

            int pieceNum = scidPos.listPos[from];
            int pieceType = scidPos.board[from];

            if (pieceNum == 0) {
                // King move or castling
                boolean isCastleK = (scidPos.toMove == WHITE)
                        ? (from == 4 && to == 6) : (from == 60 && to == 62);
                boolean isCastleQ = (scidPos.toMove == WHITE)
                        ? (from == 4 && to == 2) : (from == 60 && to == 58);

                if (isCastleK) {
                    movesStream.write(0x0A); // 10: O-O
                    scidPos.doMove(from, to, EMPTY, true, false);
                } else if (isCastleQ) {
                    movesStream.write(0x09); // 9: O-O-O
                    scidPos.doMove(from, to, EMPTY, false, true);
                } else {
                    int diff = to - from;
                    int moveCode = switch (diff) {
                        case -9 -> 1;
                        case -8 -> 2;
                        case -7 -> 3;
                        case -1 -> 4;
                        case 1 -> 5;
                        case 7 -> 6;
                        case 8 -> 7;
                        case 9 -> 8;
                        default -> throw new IllegalStateException("Illegal king step diff: " + diff);
                    };
                    movesStream.write(moveCode);
                    scidPos.doMove(from, to, EMPTY, false, false);
                }
            } else {
                // Non-King pieces
                switch (pieceType) {
                    case PAWN -> {
                        int step = (scidPos.toMove == WHITE) ? (to - from) : (from - to);
                        int moveCode;
                        if (promo != EMPTY) {
                            int base = switch (step) {
                                case 7 -> 0; // capture left
                                case 8 -> 1; // forward
                                case 9 -> 2; // capture right
                                default -> throw new IllegalStateException("Illegal pawn promo step: " + step);
                            };
                            int promoOffset = switch (promo) {
                                case QUEEN -> 0;
                                case ROOK -> 1;
                                case BISHOP -> 2;
                                case KNIGHT -> 3;
                                default -> 0;
                            };
                            moveCode = 3 + (promoOffset * 3) + base;
                        } else {
                            moveCode = switch (step) {
                                case 16 -> 15; // 2-square push
                                case 7 -> 0;  // capture left
                                case 8 -> 1;  // 1-square push
                                case 9 -> 2;  // capture right
                                default -> throw new IllegalStateException("Illegal pawn step: " + step);
                            };
                        }
                        movesStream.write((pieceNum << 4) | moveCode);
                        scidPos.doMove(from, to, promo, false, false);
                    }
                    case KNIGHT -> {
                        int diff = to - from;
                        int moveCode = switch (diff) {
                            case -17 -> 1;
                            case -15 -> 2;
                            case -10 -> 3;
                            case -6 -> 4;
                            case 6 -> 5;
                            case 10 -> 6;
                            case 15 -> 7;
                            case 17 -> 8;
                            default -> throw new IllegalStateException("Illegal knight diff: " + diff);
                        };
                        movesStream.write((pieceNum << 4) | moveCode);
                        scidPos.doMove(from, to, EMPTY, false, false);
                    }
                    case BISHOP -> {
                        int fromCol = from & 7;
                        int fromRow = from >> 3;
                        int toCol = to & 7;
                        int toRow = to >> 3;
                        int moveCode;
                        if ((toCol - fromCol) == (toRow - fromRow)) {
                            // Main diagonal
                            moveCode = toCol;
                        } else {
                            // Anti-diagonal
                            moveCode = 8 + toCol;
                        }
                        movesStream.write((pieceNum << 4) | moveCode);
                        scidPos.doMove(from, to, EMPTY, false, false);
                    }
                    case ROOK -> {
                        int fromCol = from & 7;
                        int fromRow = from >> 3;
                        int toCol = to & 7;
                        int toRow = to >> 3;
                        int moveCode;
                        if (toRow == fromRow) {
                            // Horizontal
                            moveCode = toCol;
                        } else {
                            // Vertical
                            moveCode = 8 + toRow;
                        }
                        movesStream.write((pieceNum << 4) | moveCode);
                        scidPos.doMove(from, to, EMPTY, false, false);
                    }
                    case QUEEN -> {
                        int fromCol = from & 7;
                        int fromRow = from >> 3;
                        int toCol = to & 7;
                        int toRow = to >> 3;
                        if (toRow == fromRow) {
                            // Horizontal rook-like move
                            movesStream.write((pieceNum << 4) | toCol);
                        } else if (toCol == fromCol) {
                            // Vertical rook-like move
                            movesStream.write((pieceNum << 4) | (8 + toRow));
                        } else {
                            // Diagonal move: 2 bytes
                            movesStream.write((pieceNum << 4) | fromCol);
                            movesStream.write(to + 64);
                        }
                        scidPos.doMove(from, to, EMPTY, false, false);
                    }
                    default -> throw new IllegalStateException("Unknown piece type on square " + from + ": " + pieceType);
                }
            }

            // Track home pawn departures for mainline moves
            if (isMainline) {
                int fromNibble = SQ_TO_HP_NIBBLE[from];
                if (fromNibble != -1 && (homePawnMask[0] & (1 << fromNibble)) != 0) {
                    homePawnMask[0] &= ~(1 << fromNibble);
                    hpDepartures.add(fromNibble);
                }
                int toNibble = SQ_TO_HP_NIBBLE[to];
                if (toNibble != -1 && (homePawnMask[0] & (1 << toNibble)) != 0) {
                    homePawnMask[0] &= ~(1 << toNibble);
                    hpDepartures.add(toNibble);
                }
            }
        }

        // NAGs on this node
        for (int nag : node.getNags()) {
            movesStream.write(0x0B);
            movesStream.write(nag & 0xFF);
            stats[2]++; // nag count
        }

        // Comment on this node
        if (node.getComment() != null && !node.getComment().isEmpty()) {
            movesStream.write(0x0C);
            comments.add(node.getComment());
            stats[0]++; // comment count
        }
    }

    private static int internalToSq64(int internal) {
        int row = (internal / 10) - 2;
        int col = (internal % 10) - 1;
        return (row << 3) | col;
    }

    private static int promoToScidPiece(int promotionPiece) {
        return switch (promotionPiece) {
            case CONSTANTS.QUEEN -> QUEEN;
            case CONSTANTS.ROOK -> ROOK;
            case CONSTANTS.BISHOP -> BISHOP;
            case CONSTANTS.KNIGHT -> KNIGHT;
            default -> EMPTY;
        };
    }

    public static int encodeRating(int count) {
        for (int i = RATING_THRESHOLDS.length - 1; i >= 0; i--) {
            if (count >= RATING_THRESHOLDS[i]) {
                return i;
            }
        }
        return 0;
    }

    private static int computeFinalMatSig(ScidPosition pos) {
        int wp = 0, wn = 0, wb = 0, wr = 0, wq = 0;
        int bp = 0, bn = 0, bb = 0, br = 0, bq = 0;

        for (int sq = 0; sq < 64; sq++) {
            int type = pos.board[sq];
            int col = pos.color[sq];
            if (col == WHITE) {
                switch (type) {
                    case PAWN -> wp++;
                    case KNIGHT -> wn++;
                    case BISHOP -> wb++;
                    case ROOK -> wr++;
                    case QUEEN -> wq++;
                }
            } else if (col == BLACK) {
                switch (type) {
                    case PAWN -> bp++;
                    case KNIGHT -> bn++;
                    case BISHOP -> bb++;
                    case ROOK -> br++;
                    case QUEEN -> bq++;
                }
            }
        }

        return (Math.min(bp, 15) & 0x0F) |
                ((Math.min(bn, 3) & 0x03) << 4) |
                ((Math.min(bb, 3) & 0x03) << 6) |
                ((Math.min(br, 3) & 0x03) << 8) |
                ((Math.min(bq, 3) & 0x03) << 10) |
                ((Math.min(wp, 15) & 0x0F) << 12) |
                ((Math.min(wn, 3) & 0x03) << 16) |
                ((Math.min(wb, 3) & 0x03) << 18) |
                ((Math.min(wr, 3) & 0x03) << 20) |
                ((Math.min(wq, 3) & 0x03) << 22);
    }
}
