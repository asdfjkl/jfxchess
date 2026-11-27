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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Encodes a {@link Game} tree into modern ChessBase `.2cbg` (16-bit move word stream)
 * and optional `.2cba` (annotations indexed in `C_rev` pre-order DFS) binary blocks.
 */
final class TwoCbhMoveEncoder {

    static final class EncodeResult {
        final byte[] twoCbgBlock;
        final byte[] twoCbaBlock; // null when game has no annotations
        final int mainlinePlies;

        EncodeResult(byte[] twoCbgBlock, byte[] twoCbaBlock, int mainlinePlies) {
            this.twoCbgBlock = twoCbgBlock;
            this.twoCbaBlock = twoCbaBlock;
            this.mainlinePlies = mainlinePlies;
        }
    }

    private static final class WordList {
        int[] data = new int[256];
        int size = 0;

        void add(int w) {
            if (size >= data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
            data[size++] = w & 0xFFFF;
        }

        byte[] toLittleEndianBytes() {
            byte[] out = new byte[size * 2];
            for (int i = 0; i < size; i++) {
                int w = data[i];
                out[i * 2] = (byte) (w & 0xFF);
                out[i * 2 + 1] = (byte) ((w >>> 8) & 0xFF);
            }
            return out;
        }
    }

    private TwoCbhMoveEncoder() {
    }

    static EncodeResult encode(Game game) throws IOException {
        GameNode root = game.getRootNode();
        Board rootBoard = root.getBoard();
        if (rootBoard == null) {
            rootBoard = new Board(true);
        }

        WordList words = new WordList();
        boolean customSetup = CbCommon.isCustomStart(game);
        if (customSetup) {
            int[] setupWords = TwoCbhMoveTable.encodeSetup(rootBoard);
            for (int w : setupWords) {
                words.add(w);
            }
        } else {
            words.add(0x0001);
            words.add(0xFFFC);
        }

        int[] mainlinePlies = new int[1];
        encodeChildren(root, rootBoard, words, mainlinePlies, true);
        words.add(0xFFFF); // EOF terminator

        byte[] twoCbgBlock = wrapMagicBlock(words.toLittleEndianBytes());
        byte[] twoCbaBlock = encodeTwoCba(root);

        return new EncodeResult(twoCbgBlock, twoCbaBlock, mainlinePlies[0]);
    }

    private static void encodeChildren(
            GameNode parent,
            Board parentBoard,
            WordList words,
            int[] mainlinePlies,
            boolean isMainline) throws IOException {
        ArrayList<GameNode> vars = parent.getVariations();
        int count = vars.size();
        if (count == 0) {
            return;
        }

        // 2CBG encodes sidelines in reverse order before the mainline.
        // For each sideline, 0xFFFD is emitted immediately AFTER the sideline's first move
        // so the decoder pushes (parent, parentBoard) onto varStack before descending into the sideline.
        for (int i = count - 1; i >= 1; i--) {
            GameNode side = vars.get(i);
            int w = encodeMoveWord(parentBoard, side.getMove());
            words.add(w);
            words.add(0xFFFD);
            Board sideBoard = resolveChildBoard(parentBoard, side);
            encodeChildren(side, sideBoard, words, mainlinePlies, false);
            words.add(0xFFFF);
        }

        GameNode mainChild = vars.get(0);
        int mainWord = encodeMoveWord(parentBoard, mainChild.getMove());
        words.add(mainWord);
        if (isMainline) {
            mainlinePlies[0]++;
        }
        Board mainBoard = resolveChildBoard(parentBoard, mainChild);
        encodeChildren(mainChild, mainBoard, words, mainlinePlies, isMainline);
    }

    private static Board resolveChildBoard(Board parentBoard, GameNode child) {
        Board b = child.getBoard();
        if (b != null) {
            return b;
        }
        Board copy = parentBoard.makeCopy();
        if (child.getMove() != null) {
            copy.apply(child.getMove());
        }
        return copy;
    }

    private static int encodeMoveWord(Board board, Move m) throws IOException {
        if (m == null || m.isNullMove) {
            return 0xFFFA;
        }
        int fx = (m.from % 10) - 1;
        int fy = (m.from / 10) - 2;
        int tx = (m.to % 10) - 1;
        int ty = (m.to / 10) - 2;
        if (fx < 0 || fx > 7 || fy < 0 || fy > 7 || tx < 0 || tx > 7 || ty < 0 || ty > 7) {
            throw new IOException("Invalid move coordinates for 2CBH encoding: " + m.getUci());
        }
        int fromSq64 = (fy << 3) | fx;
        int toSq64 = (ty << 3) | tx;
        int piece = board.getPieceAt(fx, fy);
        int pieceType = piece & 0x07;
        int color = (board.turn == CONSTANTS.BLACK) ? 1 : 0;
        boolean isCastleK = (pieceType == CONSTANTS.KING) && (fy == ty) && (fx == 4 && tx == 6);
        boolean isCastleQ = (pieceType == CONSTANTS.KING) && (fy == ty) && (fx == 4 && tx == 2);

        int word = TwoCbhMoveTable.encodeMoveWord(
                color, pieceType, fromSq64, toSq64, m.promotionPiece, isCastleK, isCastleQ);
        if (word <= 0) {
            throw new IOException("Move cannot be encoded in 2CBH move table: " + m.getUci());
        }
        return word;
    }

    private static byte[] encodeTwoCba(GameNode root) {
        ArrayList<GameNode> cRevList = new ArrayList<>();
        TwoCbhChessDatabase.buildCRevNodeList(root, cRevList);

        boolean any = false;
        for (int i = 0; i < cRevList.size(); i++) {
            if (CbCommon.hasNodeAnnotations(cRevList.get(i))) {
                any = true;
                break;
            }
        }
        if (!any) {
            return null;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream(256);
        writeU16LE(out, 0x2000);

        for (int mvIdx = 0; mvIdx < cRevList.size(); mvIdx++) {
            GameNode node = cRevList.get(mvIdx);
            if (!CbCommon.hasNodeAnnotations(node)) {
                continue;
            }
            String comment = node.getComment();
            boolean hasComment = comment != null && !comment.isEmpty();
            int[] nags = CbCommon.extractNags(node.getNags());
            boolean hasNags = nags[0] != 0 || nags[1] != 0 || nags[2] != 0;
            ArrayList<ColoredField> fields = node.getColoredFields();
            boolean hasFields = !fields.isEmpty();
            ArrayList<Arrow> arrows = node.getArrows();
            boolean hasArrows = !arrows.isEmpty();

            int itemCount = (hasComment ? 1 : 0)
                    + (hasNags ? 1 : 0)
                    + (hasFields ? 1 : 0)
                    + (hasArrows ? 1 : 0);
            if (itemCount == 0) {
                continue;
            }

            writeI32LE(out, mvIdx);
            writeI32LE(out, itemCount);

            if (hasComment) {
                byte[] utf8 = comment.getBytes(StandardCharsets.UTF_8);
                writeU16LE(out, 0x0002);
                writeU16LE(out, 0);    // flags
                writeU16LE(out, 0);    // lang 0 (Universal/English highest priority)
                writeI32LE(out, utf8.length);
                out.writeBytes(utf8);
            }
            if (hasNags) {
                writeU16LE(out, 0x0003);
                out.write(nags[0] & 0xFF);
                out.write(nags[1] & 0xFF);
                out.write(nags[2] & 0xFF);
            }
            if (hasFields) {
                writeU16LE(out, 0x0004);
                writeI32LE(out, fields.size() * 2);
                for (int i = 0; i < fields.size(); i++) {
                    ColoredField cf = fields.get(i);
                    out.write(2); // green
                    out.write((cf.x * 8 + cf.y + 1) & 0xFF);
                }
            }
            if (hasArrows) {
                writeU16LE(out, 0x0005);
                writeI32LE(out, arrows.size() * 3);
                for (int i = 0; i < arrows.size(); i++) {
                    Arrow a = arrows.get(i);
                    out.write(2); // green
                    out.write((a.xFrom * 8 + a.yFrom + 1) & 0xFF);
                    out.write((a.xTo * 8 + a.yTo + 1) & 0xFF);
                }
            }
        }

        writeU16LE(out, 0xFFFF);
        return wrapMagicBlock(out.toByteArray());
    }

    static byte[] wrapMagicBlock(byte[] payload) {
        byte[] out = new byte[24 + payload.length];
        CbCommon.writeI64LE(out, 0, TwoCbhChessDatabase.TWO_CBG_MAGIC);
        CbCommon.writeI32LE(out, 8, payload.length);
        CbCommon.writeI32LE(out, 12, 100);
        System.arraycopy(payload, 0, out, 24, payload.length);
        return out;
    }

    private static void writeU16LE(ByteArrayOutputStream out, int v) {
        out.write(v & 0xFF);
        out.write((v >>> 8) & 0xFF);
    }

    private static void writeI32LE(ByteArrayOutputStream out, int v) {
        out.write(v & 0xFF);
        out.write((v >>> 8) & 0xFF);
        out.write((v >>> 16) & 0xFF);
        out.write((v >>> 24) & 0xFF);
    }
}
