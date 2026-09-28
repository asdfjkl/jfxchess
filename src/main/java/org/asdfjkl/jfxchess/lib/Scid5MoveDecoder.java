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

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.asdfjkl.jfxchess.lib.ScidPosition.*;

/**
 * Decodes game data blobs from SCID 5 (.sg5) files into {@link Game} instances.
 */
public class Scid5MoveDecoder {

    // Common tags table (codes 241..250)
    private static final String[] COMMON_TAGS = {
            "WhiteCountry", "BlackCountry", "Annotator", "PlyCount",
            "EventDate", "Opening", "Variation", "Setup", "Source", "SetUp"
    };

    /**
     * Decodes a SCID5 game blob into a Game object.
     *
     * @param sg5      the byte array containing game data
     * @param offset   start offset of the game blob
     * @param dataSize length of the game blob in bytes
     * @param info     metadata from index/namebase, or null
     * @return the fully reconstructed Game object
     */
    public static Game decode(byte[] sg5, int offset, int dataSize, Scid5GameInfo info) {
        Game game = new Game();
        int pos = offset;
        int end = offset + dataSize;

        // 1. Populate standard PGN headers from GameInfo if available
        if (info != null) {
            game.setHeader("Event", info.getEvent());
            game.setHeader("Site", info.getSite());
            game.setHeader("Date", info.getDate());
            game.setHeader("Round", info.getRound());
            game.setHeader("White", info.getWhite());
            game.setHeader("Black", info.getBlack());
            game.setHeader("Result", info.getResult());
            if (info.getEco() != null && !info.getEco().isEmpty()) {
                game.setHeader("ECO", info.getEco());
            }
            if (info.getWhiteElo() != null && !info.getWhiteElo().isEmpty()) {
                game.setHeader("WhiteElo", info.getWhiteElo());
            }
            if (info.getBlackElo() != null && !info.getBlackElo().isEmpty()) {
                game.setHeader("BlackElo", info.getBlackElo());
            }

            if ("1-0".equals(info.getResult())) {
                game.setResult(CONSTANTS.RES_WHITE_WINS);
            } else if ("0-1".equals(info.getResult())) {
                game.setResult(CONSTANTS.RES_BLACK_WINS);
            } else if ("1/2-1/2".equals(info.getResult())) {
                game.setResult(CONSTANTS.RES_DRAW);
            } else {
                game.setResult(CONSTANTS.RES_UNDEF);
            }
        }

        // 2. Extra tags section
        while (pos < end) {
            int b = sg5[pos++] & 0xFF;
            if (b == 0) {
                break; // End of extra tags section
            }
            if (b >= 241 && b <= 250) {
                String tagName = COMMON_TAGS[b - 241];
                int valLen = sg5[pos++] & 0xFF;
                String val = new String(sg5, pos, valLen, StandardCharsets.UTF_8);
                pos += valLen;
                game.setHeader(tagName, val);
            } else {
                int nameLen = b;
                String tagName = new String(sg5, pos, nameLen, StandardCharsets.UTF_8);
                pos += nameLen;
                int valLen = sg5[pos++] & 0xFF;
                String val = new String(sg5, pos, valLen, StandardCharsets.UTF_8);
                pos += valLen;
                game.setHeader(tagName, val);
            }
        }

        // 3. Start board section
        int startFlags = sg5[pos++] & 0xFF;
        ScidPosition scidPos = new ScidPosition();
        Board rootBoard;

        if ((startFlags & 1) != 0) {
            int fenStart = pos;
            while (pos < end && sg5[pos] != 0) {
                pos++;
            }
            String fen = new String(sg5, fenStart, pos - fenStart, StandardCharsets.US_ASCII);
            pos++; // skip null char
            game.setHeader("SetUp", "1");
            rootBoard = new Board(fen);
            scidPos.setupFen(fen);
        } else {
            rootBoard = new Board(true);
            scidPos.stdStart();
        }
        game.getRootNode().setBoard(rootBoard);

        // 4. Move stream & Comments
        GameNode currentNode = game.getRootNode();
        Map<GameNode, ScidPosition> nodePositions = new HashMap<>();
        nodePositions.put(currentNode, scidPos.copy());
        Stack<Scid5DecoderState> varStack = new Stack<>();
        List<GameNode> commentNodes = new ArrayList<>();

        while (pos < end) {
            int b = sg5[pos++] & 0xFF;
            if (b == 0x0F) { // ENCODE_END_GAME
                break;
            }

            int pieceNum = (b >> 4) & 0x0F;
            int moveCode = b & 0x0F;

            if (pieceNum == 0) {
                // King moves and special tokens
                if (moveCode == 0) {
                    // Null move
                    Move m = new Move();
                    m.isNullMove = true;
                    currentNode = applyDecodedMove(currentNode, m);
                    scidPos.toMove = 1 - scidPos.toMove;
                    nodePositions.put(currentNode, scidPos.copy());
                } else if (moveCode <= 8) {
                    // King step
                    int from = scidPos.list[scidPos.toMove][0];
                    int[] diffs = {0, -9, -8, -7, -1, 1, 7, 8, 9};
                    int to = from + diffs[moveCode];
                    Move m = createMove(from, to, EMPTY);
                    currentNode = applyDecodedMove(currentNode, m);
                    scidPos.doMove(from, to, EMPTY, false, false);
                    nodePositions.put(currentNode, scidPos.copy());
                } else if (moveCode == 9) {
                    // Queenside Castle (O-O-O)
                    int from = scidPos.list[scidPos.toMove][0];
                    int to = (scidPos.toMove == WHITE) ? 2 : 58;
                    Move m = createMove(from, to, EMPTY);
                    currentNode = applyDecodedMove(currentNode, m);
                    scidPos.doMove(from, to, EMPTY, false, true);
                    nodePositions.put(currentNode, scidPos.copy());
                } else if (moveCode == 10) {
                    // Kingside Castle (O-O)
                    int from = scidPos.list[scidPos.toMove][0];
                    int to = (scidPos.toMove == WHITE) ? 6 : 62;
                    Move m = createMove(from, to, EMPTY);
                    currentNode = applyDecodedMove(currentNode, m);
                    scidPos.doMove(from, to, EMPTY, true, false);
                    nodePositions.put(currentNode, scidPos.copy());
                } else if (moveCode == 11) {
                    // NAG annotation
                    int nag = sg5[pos++] & 0xFF;
                    currentNode.addNag(nag);
                } else if (moveCode == 12) {
                    // Comment marker
                    commentNodes.add(currentNode);
                } else if (moveCode == 13) {
                    // Start Variation '('
                    varStack.push(new Scid5DecoderState(currentNode, scidPos.copy()));
                    GameNode parent = currentNode.getParent();
                    if (parent != null) {
                        currentNode = parent;
                        ScidPosition parentPos = nodePositions.get(parent);
                        if (parentPos != null) {
                            scidPos = parentPos.copy();
                        }
                    }
                } else if (moveCode == 14) {
                    // End Variation ')'
                    if (!varStack.isEmpty()) {
                        Scid5DecoderState state = varStack.pop();
                        currentNode = state.node;
                        scidPos = state.pos;
                    }
                }
                continue;
            }

            // Normal piece moves (pieceNum >= 1)
            int from = scidPos.list[scidPos.toMove][pieceNum];
            int pieceType = scidPos.board[from];
            int to = -1;
            int promo = EMPTY;

            switch (pieceType) {
                case PAWN -> {
                    int[] sqdiff = {7, 8, 9, 7, 8, 9, 7, 8, 9, 7, 8, 9, 7, 8, 9, 16};
                    int[] promoPiece = {
                            EMPTY, EMPTY, EMPTY, QUEEN, QUEEN, QUEEN, ROOK, ROOK, ROOK,
                            BISHOP, BISHOP, BISHOP, KNIGHT, KNIGHT, KNIGHT, EMPTY
                    };
                    to = (scidPos.toMove == WHITE) ? (from + sqdiff[moveCode]) : (from - sqdiff[moveCode]);
                    promo = promoPiece[moveCode];
                }
                case BISHOP -> {
                    int targetFile = (moveCode >= 8) ? (moveCode - 8) : moveCode;
                    int fylediff = targetFile - (from & 7);
                    to = (moveCode >= 8) ? (from - 7 * fylediff) : (from + 9 * fylediff);
                }
                case KNIGHT -> {
                    int[] sqdiff = {0, -17, -15, -10, -6, 6, 10, 15, 17, 0, 0, 0, 0, 0, 0, 0};
                    to = from + sqdiff[moveCode];
                }
                case QUEEN -> {
                    if (moveCode == (from & 7)) { // 2-byte diagonal move
                        int b2 = sg5[pos++] & 0xFF;
                        to = b2 - 64;
                    } else if (moveCode >= 8) {
                        to = ((moveCode - 8) << 3) | (from & 7);
                    } else {
                        to = (from & ~7) | moveCode;
                    }
                }
                case ROOK -> {
                    if (moveCode >= 8) {
                        to = ((moveCode - 8) << 3) | (from & 7);
                    } else {
                        to = (from & ~7) | moveCode;
                    }
                }
            }

            Move m = createMove(from, to, promo);
            currentNode = applyDecodedMove(currentNode, m);
            scidPos.doMove(from, to, promo, false, false);
            nodePositions.put(currentNode, scidPos.copy());
        }

        // 5. Read Section 4 Comments and associate them with commentNodes
        List<String> comments = new ArrayList<>();
        while (pos < end) {
            int commentStart = pos;
            while (pos < end && sg5[pos] != 0) {
                pos++;
            }
            comments.add(new String(sg5, commentStart, pos - commentStart, StandardCharsets.UTF_8));
            pos++; // skip null terminator
        }

        for (int i = 0; i < commentNodes.size() && i < comments.size(); i++) {
            GameNode node = commentNodes.get(i);
            String comment = comments.get(i);
            if (node.getComment() == null || node.getComment().isEmpty()) {
                node.setComment(comment);
            } else {
                node.setComment(node.getComment() + " " + comment);
            }
        }

        return game;
    }

    private static Move createMove(int fromSq, int toSq, int promo) {
        int fromCol = fromSq & 7;
        int fromRow = fromSq >> 3;
        int toCol = toSq & 7;
        int toRow = toSq >> 3;

        if (promo == EMPTY) {
            return new Move(fromCol, fromRow, toCol, toRow);
        }

        int fromInternal = ((fromRow + 2) * 10) + (fromCol + 1);
        int toInternal = ((toRow + 2) * 10) + (toCol + 1);
        int promoConstant = switch (promo) {
            case QUEEN -> CONSTANTS.QUEEN;
            case ROOK -> CONSTANTS.ROOK;
            case BISHOP -> CONSTANTS.BISHOP;
            case KNIGHT -> CONSTANTS.KNIGHT;
            default -> 0;
        };
        return new Move(fromInternal, toInternal, promoConstant);
    }

    private static GameNode applyDecodedMove(GameNode currentNode, Move m) {
        GameNode nextNode = new GameNode();
        Board childBoard = currentNode.getBoard().makeCopy();
        childBoard.apply(m);
        nextNode.setBoard(childBoard);
        nextNode.setMove(m);
        nextNode.setParent(currentNode);
        currentNode.addVariation(nextNode);
        return nextNode;
    }
}
