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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Stack;

/**
 * Fast PGN reader powered by {@link PgnByteLexer} and {@link SanResolver}.
 * Bypasses line-by-line String decoding and regex, operating directly on
 * sliding 64 KB byte blocks.
 */
public class PgnReader {

    private static final Board STANDARD_ROOT = new Board(true);

    private static final class VariationFrame {
        final Board savedCurrent = new Board();
        final Board savedPrev = new Board();
        int savedPly;
    }

    private GameNode currentNode = null;
    private final Stack<GameNode> gameStack;
    private final VariationFrame[] varStack;
    private final Board scanCurrentBoard;
    private final Board scanPrevBoard;

    public PgnReader() {
        gameStack = new Stack<>();
        varStack = new VariationFrame[64];
        for (int i = 0; i < varStack.length; i++) {
            varStack[i] = new VariationFrame();
        }
        scanCurrentBoard = new Board();
        scanPrevBoard = new Board();
    }

    public HashMap<String, String> readSingleHeader(String filename, long offset) {
        try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
            return readSingleHeader(raf, offset);
        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public HashMap<String, String> readSingleHeader(RandomAccessFile raf, long offset) {
        HashMap<String, String> header = new HashMap<>();
        try {
            PgnByteLexer lexer = new PgnByteLexer(raf);
            lexer.seek(offset);
            int tkn;
            while ((tkn = lexer.nextToken()) == PgnByteLexer.TOKEN_TAG) {
                header.put(lexer.getTagName(), lexer.getTagValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return header;
    }

    private void addMove(Move m) {
        GameNode next = new GameNode();
        Board currentBoard = this.currentNode.getBoard();
        try {
            Board childBoard = currentBoard.makeCopy();
            childBoard.apply(m);
            next.setMove(m);
            next.setBoard(childBoard);
            next.setParent(this.currentNode);
            this.currentNode.addVariation(next);
            this.currentNode = next;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private static int extractNagFromMoveToken(byte[] buf, int off, int len) {
        boolean hasDoubleQ = false;
        boolean hasDoubleE = false;
        boolean hasQE = false;
        boolean hasEQ = false;
        boolean hasQ = false;
        boolean hasE = false;

        for (int i = 0; i < len; i++) {
            byte b = buf[off + i];
            if (b == '?' || b == '!') {
                if (i + 1 < len) {
                    byte b2 = buf[off + i + 1];
                    if (b == '?' && b2 == '?') { hasDoubleQ = true; break; }
                    if (b == '!' && b2 == '!') { hasDoubleE = true; break; }
                    if (b == '!' && b2 == '?') { hasEQ = true; break; }
                    if (b == '?' && b2 == '!') { hasQE = true; break; }
                }
                if (b == '?') hasQ = true;
                if (b == '!') hasE = true;
            }
        }

        if (hasDoubleQ) {
            return CONSTANTS.NAG_BLUNDER;
        } else if (hasDoubleE) {
            return CONSTANTS.NAG_BRILLIANT_MOVE;
        } else if (hasEQ) {
            return CONSTANTS.NAG_SPECULATIVE_MOVE;
        } else if (hasQE) {
            return CONSTANTS.NAG_DUBIOUS_MOVE;
        } else if (hasQ) {
            return CONSTANTS.NAG_MISTAKE;
        } else if (hasE) {
            return CONSTANTS.NAG_GOOD_MOVE;
        }
        return 0;
    }

    private void extractTrailingNag(byte[] buf, int off, int len) {
        int nag = extractNagFromMoveToken(buf, off, len);
        if (nag != 0) {
            currentNode.addNag(nag);
        }
    }

    private void parseMoveToken(PgnByteLexer lexer) {
        byte[] buf = lexer.getBuffer();
        int off = lexer.getTokenOffset();
        int len = lexer.getTokenLength();
        Move m = SanResolver.parseSan(currentNode.getBoard(), buf, off, len);
        if (m != null) {
            addMove(m);
            extractTrailingNag(buf, off, len);
        }
    }

    /**
     * Reads a game from a streaming {@link PgnByteLexer}.
     *
     * @param lexer the byte lexer
     * @return the parsed Game, or null if EOF was reached without content
     */
    public Game readGame(PgnByteLexer lexer) throws IOException {
        Game g = new Game();
        gameStack.clear();
        gameStack.push(g.getRootNode());
        currentNode = g.getRootNode();
        Board rootBoard = new Board(true);
        currentNode.setBoard(rootBoard);

        String startingFen = "";
        int tkn;
        boolean hasContent = false;

        // 1. Parse tag headers
        while ((tkn = lexer.nextToken()) == PgnByteLexer.TOKEN_TAG) {
            hasContent = true;
            String tag = lexer.getTagName();
            String value = lexer.getTagValue();
            if ("FEN".equals(tag)) {
                startingFen = value;
            } else {
                g.setHeader(tag, value);
            }
        }

        // Set starting FEN if present
        if (!startingFen.isEmpty()) {
            try {
                Board boardFen = new Board(startingFen);
                if (!boardFen.isConsistent()) {
                    return g;
                } else {
                    currentNode.setBoard(boardFen);
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }

        // 2. Movetext loop
        while (tkn != PgnByteLexer.TOKEN_EOF) {
            hasContent = true;
            if (tkn == PgnByteLexer.TOKEN_TAG) {
                // Next game encountered!
                lexer.pushBackToken();
                break;
            } else if (tkn == PgnByteLexer.TOKEN_RESULT) {
                g.setResult(lexer.getResult());
                // Look ahead for optional trailing comment or next game
                while ((tkn = lexer.nextToken()) != PgnByteLexer.TOKEN_EOF) {
                    if (tkn == PgnByteLexer.TOKEN_COMMENT) {
                        currentNode.setComment(lexer.getComment());
                    } else if (tkn == PgnByteLexer.TOKEN_TAG) {
                        lexer.pushBackToken();
                        break;
                    } else {
                        lexer.pushBackToken();
                        break;
                    }
                }
                break;
            } else if (tkn == PgnByteLexer.TOKEN_MOVE) {
                parseMoveToken(lexer);
            } else if (tkn == PgnByteLexer.TOKEN_NULL_MOVE) {
                Move m = new Move();
                m.isNullMove = true;
                addMove(m);
            } else if (tkn == PgnByteLexer.TOKEN_OPEN_VARIATION) {
                if (currentNode != g.getRootNode()) {
                    gameStack.push(currentNode);
                    currentNode = currentNode.getParent();
                }
            } else if (tkn == PgnByteLexer.TOKEN_CLOSE_VARIATION) {
                if (gameStack.size() > 1) {
                    currentNode = gameStack.pop();
                }
            } else if (tkn == PgnByteLexer.TOKEN_NAG) {
                currentNode.addNag(lexer.getNag());
            } else if (tkn == PgnByteLexer.TOKEN_COMMENT) {
                currentNode.setComment(lexer.getComment());
            }

            tkn = lexer.nextToken();
        }

        if (!hasContent && tkn == PgnByteLexer.TOKEN_EOF) {
            return null;
        }
        return g;
    }

    public Game readGame(RandomAccessFile raf) {
        try {
            return readGame(new PgnByteLexer(raf));
        } catch (IOException e) {
            e.printStackTrace();
            return new Game();
        }
    }

    public Game readGame(File file) {
        try (PgnByteLexer lexer = new PgnByteLexer(file)) {
            return readGame(lexer);
        } catch (IOException e) {
            e.printStackTrace();
            return new Game();
        }
    }

    public Game readGame(String gameString) {
        if (gameString == null || gameString.isEmpty()) {
            return new Game();
        }
        try {
            PgnByteLexer lexer = new PgnByteLexer(gameString.getBytes(StandardCharsets.UTF_8));
            Game g = readGame(lexer);
            return g != null ? g : new Game();
        } catch (IOException e) {
            e.printStackTrace();
            return new Game();
        }
    }

    /**
     * Streams a single PGN game to a {@link PgnGameConsumer} with zero AST allocations
     * and in-place {@link Board} mutation.
     *
     * @param lexer the byte lexer
     * @param consumer the game event consumer
     * @return true if a game was processed (or aborted early), false if EOF reached without content
     */
    public boolean scanGame(PgnByteLexer lexer, PgnGameConsumer consumer) throws IOException {
        int tkn;
        boolean hasContent = false;
        String startingFen = "";

        // 1. Parse tag headers
        while ((tkn = lexer.nextToken()) == PgnByteLexer.TOKEN_TAG) {
            hasContent = true;
            String tag = lexer.getTagName();
            String value = lexer.getTagValue();
            if ("FEN".equals(tag)) {
                startingFen = value;
            }
            consumer.onTag(tag, value);
        }

        // Set starting board
        if (!startingFen.isEmpty()) {
            try {
                Board boardFen = new Board(startingFen);
                if (!boardFen.isConsistent()) {
                    return hasContent;
                }
                scanCurrentBoard.copy(boardFen);
            } catch (IllegalArgumentException e) {
                scanCurrentBoard.copy(STANDARD_ROOT);
            }
        } else {
            scanCurrentBoard.copy(STANDARD_ROOT);
        }
        scanPrevBoard.copy(scanCurrentBoard);

        // Notify game start with root board. If consumer returns false, early exit immediately!
        if (!consumer.onGameStart(scanCurrentBoard)) {
            int next = lexer.skipToNextTag();
            if (next == PgnByteLexer.TOKEN_TAG) {
                lexer.pushBackToken();
            }
            return true;
        }

        int varDepth = 0;
        int ply = 0;

        // 2. Movetext loop
        while (tkn != PgnByteLexer.TOKEN_EOF) {
            hasContent = true;
            if (tkn == PgnByteLexer.TOKEN_TAG) {
                // Next game encountered!
                lexer.pushBackToken();
                break;
            } else if (tkn == PgnByteLexer.TOKEN_RESULT) {
                consumer.onResult(lexer.getResult());
                // Look ahead for optional trailing comment or next game
                while ((tkn = lexer.nextToken()) != PgnByteLexer.TOKEN_EOF) {
                    if (tkn == PgnByteLexer.TOKEN_COMMENT) {
                        consumer.onComment(lexer.getComment());
                    } else if (tkn == PgnByteLexer.TOKEN_TAG) {
                        lexer.pushBackToken();
                        break;
                    } else {
                        lexer.pushBackToken();
                        break;
                    }
                }
                break;
            } else if (tkn == PgnByteLexer.TOKEN_MOVE) {
                byte[] buf = lexer.getBuffer();
                int off = lexer.getTokenOffset();
                int len = lexer.getTokenLength();
                Move m = SanResolver.parseSan(scanCurrentBoard, buf, off, len);
                if (m != null) {
                    scanPrevBoard.copy(scanCurrentBoard);
                    scanCurrentBoard.apply(m);
                    ply++;
                    boolean isMainline = (varDepth == 0);
                    if (!consumer.onMove(scanCurrentBoard, m, ply, isMainline)) {
                        // Early exit requested!
                        int next = lexer.skipToNextTag();
                        if (next == PgnByteLexer.TOKEN_TAG) {
                            lexer.pushBackToken();
                        }
                        return true;
                    }
                    int nag = extractNagFromMoveToken(buf, off, len);
                    if (nag != 0) {
                        consumer.onNag(nag);
                    }
                }
            } else if (tkn == PgnByteLexer.TOKEN_NULL_MOVE) {
                Move m = new Move();
                m.isNullMove = true;
                scanPrevBoard.copy(scanCurrentBoard);
                scanCurrentBoard.apply(m);
                ply++;
                boolean isMainline = (varDepth == 0);
                if (!consumer.onMove(scanCurrentBoard, m, ply, isMainline)) {
                    int next = lexer.skipToNextTag();
                    if (next == PgnByteLexer.TOKEN_TAG) {
                        lexer.pushBackToken();
                    }
                    return true;
                }
            } else if (tkn == PgnByteLexer.TOKEN_OPEN_VARIATION) {
                if (varDepth < varStack.length) {
                    varStack[varDepth].savedCurrent.copy(scanCurrentBoard);
                    varStack[varDepth].savedPrev.copy(scanPrevBoard);
                    varStack[varDepth].savedPly = ply;
                    varDepth++;
                }
                scanCurrentBoard.copy(scanPrevBoard);
                ply = (varDepth > 0) ? varStack[varDepth - 1].savedPly - 1 : ply;
                consumer.onStartVariation();
            } else if (tkn == PgnByteLexer.TOKEN_CLOSE_VARIATION) {
                if (varDepth > 0) {
                    varDepth--;
                    scanCurrentBoard.copy(varStack[varDepth].savedCurrent);
                    scanPrevBoard.copy(varStack[varDepth].savedPrev);
                    ply = varStack[varDepth].savedPly;
                }
                consumer.onEndVariation();
            } else if (tkn == PgnByteLexer.TOKEN_NAG) {
                consumer.onNag(lexer.getNag());
            } else if (tkn == PgnByteLexer.TOKEN_COMMENT) {
                consumer.onComment(lexer.getComment());
            }

            tkn = lexer.nextToken();
        }

        if (!hasContent && tkn == PgnByteLexer.TOKEN_EOF) {
            return false;
        }

        consumer.onGameEnd();
        return true;
    }

    public void scanAllGames(PgnByteLexer lexer, PgnGameConsumer consumer) throws IOException {
        while (scanGame(lexer, consumer)) {
            // Continues until EOF
        }
    }

    public void scanAllGames(File file, PgnGameConsumer consumer) throws IOException {
        try (PgnByteLexer lexer = new PgnByteLexer(file)) {
            scanAllGames(lexer, consumer);
        }
    }
}
