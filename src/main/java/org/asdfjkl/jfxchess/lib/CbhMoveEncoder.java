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
import java.util.ArrayList;

/**
 * Encodes a {@link Game} tree into classic ChessBase `.cbg` (move stream) and
 * optional `.cba` (annotations) binary blocks.
 */
final class CbhMoveEncoder {

    static final class EncodeResult {
        final byte[] cbgBlock;
        final byte[] cbaBlock; // null when game has no annotations
        final int mainlinePlies;

        EncodeResult(byte[] cbgBlock, byte[] cbaBlock, int mainlinePlies) {
            this.cbgBlock = cbgBlock;
            this.cbaBlock = cbaBlock;
            this.mainlinePlies = mainlinePlies;
        }
    }

    private static final class EncodeCtx {
        final ByteArrayOutputStream cbgPayload = new ByteArrayOutputStream(256);
        final ByteArrayOutputStream cbaChunks = new ByteArrayOutputStream(128);
        int mc = 0;
        int moveOnlySeq = 0;
        boolean hasSeenPop = false;
        int mainlinePlies = 0;

        void emitControl(int op) {
            cbgPayload.write((op + mc) & 0xFF);
        }

        void emitMoveByte(int op) {
            cbgPayload.write((op + mc) & 0xFF);
            mc = (mc + 1) & 0xFF;
        }

        void emitEscape(int d1, int d2) {
            cbgPayload.write((0x29 + mc) & 0xFF);
            cbgPayload.write((d1 + mc) & 0xFF);
            cbgPayload.write((d2 + mc) & 0xFF);
            mc = (mc + 1) & 0xFF;
        }
    }

    private CbhMoveEncoder() {
    }

    static EncodeResult encode(Game game, int oneBasedGameNo) throws IOException {
        GameNode root = game.getRootNode();
        Board rootBoard = root.getBoard();
        if (rootBoard == null) {
            rootBoard = new Board(true);
        }

        EncodeCtx ctx = new EncodeCtx();
        CbhCrypto.RosterState state = new CbhCrypto.RosterState();
        boolean customSetup = CbCommon.isCustomStart(game);
        if (customSetup) {
            byte[] setup28 = CbhCrypto.encodeCustomSetup(rootBoard, state);
            ctx.cbgPayload.writeBytes(setup28);
        } else {
            state.resetStandard();
        }

        if (CbCommon.hasNodeAnnotations(root)) {
            writeNodeAnnotations(ctx.cbaChunks, 0, root);
        }

        encodeChildren(root, state, ctx, true);
        // Trailing POP acts as EOF sentinel for the move decoder.
        ctx.emitControl(0x0C);

        byte[] payloadBytes = ctx.cbgPayload.toByteArray();
        int totalCbgLen = 4 + payloadBytes.length;
        int hdr1 = totalCbgLen;
        if (customSetup) {
            hdr1 |= 0x40000000;
        }
        byte[] cbgBlock = new byte[totalCbgLen];
        CbCommon.writeI32BE(cbgBlock, 0, hdr1);
        System.arraycopy(payloadBytes, 0, cbgBlock, 4, payloadBytes.length);

        byte[] cbaBlock = null;
        if (ctx.cbaChunks.size() > 0) {
            byte[] chunkBytes = ctx.cbaChunks.toByteArray();
            int totalCbaLen = 14 + chunkBytes.length;
            cbaBlock = new byte[totalCbaLen];
            CbCommon.writeI32BE(cbaBlock, 0, Math.max(0, oneBasedGameNo));
            CbCommon.writeI32BE(cbaBlock, 10, totalCbaLen);
            System.arraycopy(chunkBytes, 0, cbaBlock, 14, chunkBytes.length);
        }

        return new EncodeResult(cbgBlock, cbaBlock, ctx.mainlinePlies);
    }

    private static void encodeChildren(
            GameNode parent,
            CbhCrypto.RosterState state,
            EncodeCtx ctx,
            boolean isMainline) throws IOException {
        ArrayList<GameNode> vars = parent.getVariations();
        int count = vars.size();
        if (count == 0) {
            return;
        }

        // CBG encodes sidelines in reverse order (wrapped in PUSH/POP) before the mainline
        // so that reverseVariationOrder(root) on decode restores [mainline, side1, side2, ...].
        for (int i = count - 1; i >= 1; i--) {
            GameNode side = vars.get(i);
            CbhCrypto.RosterState saved = new CbhCrypto.RosterState();
            saved.copyFrom(state);
            ctx.emitControl(0xDC); // OP_PUSH
            encodeMoveAndNode(side, state, ctx, false);
            encodeChildren(side, state, ctx, false);
            ctx.emitControl(0x0C); // OP_POP
            ctx.hasSeenPop = true;
            state.copyFrom(saved);
        }

        GameNode mainChild = vars.get(0);
        encodeMoveAndNode(mainChild, state, ctx, isMainline);
        encodeChildren(mainChild, state, ctx, isMainline);
    }

    private static void encodeMoveAndNode(
            GameNode node,
            CbhCrypto.RosterState state,
            EncodeCtx ctx,
            boolean isMainline) throws IOException {
        Move m = node.getMove();
        if (m == null || m.isNullMove) {
            ctx.emitMoveByte(0xAA); // OP_NULL
            state.toMove = 1 - state.toMove;
        } else {
            int fx = (m.from % 10) - 1;
            int fy = (m.from / 10) - 2;
            int tx = (m.to % 10) - 1;
            int ty = (m.to / 10) - 2;
            if (fx < 0 || fx > 7 || fy < 0 || fy > 7 || tx < 0 || tx > 7 || ty < 0 || ty > 7) {
                throw new IOException("Invalid move coordinates for CBH encoding: " + m.getUci());
            }
            int fromSq = (fy << 3) | fx;
            int toSq = (ty << 3) | tx;
            int movingPiece = state.board[fromSq];
            int movingType = movingPiece & 0x07;

            if (movingType == CONSTANTS.KING && fy == ty && Math.abs(tx - fx) == 2) {
                if (tx == 6) {
                    ctx.emitMoveByte(0x76); // OP_CASTLE_K
                    state.applyCastle(true);
                } else if (tx == 2) {
                    ctx.emitMoveByte(0xB5); // OP_CASTLE_Q
                    state.applyCastle(false);
                } else {
                    throw new IOException("Unsupported castling move for CBH encoding: " + m.getUci());
                }
            } else if (m.promotionPiece != CONSTANTS.EMPTY) {
                int promoIdx = CbhCrypto.constantToPromoCode(m.promotionPiece);
                if (promoIdx < 0) {
                    throw new IOException("Unsupported promotion piece for CBH encoding: " + m.promotionPiece);
                }
                int fsqCm = (fx << 3) | fy;
                int tsqCm = (tx << 3) | ty;
                int b1 = ((promoIdx & 0x03) << 4) | (tsqCm >>> 2);
                int b2 = ((tsqCm & 0x03) << 6) | fsqCm;
                int d1 = CbhCrypto.SBOX_FWD[b1];
                int d2 = CbhCrypto.SBOX_FWD[b2];
                if (d1 < 0 || d2 < 0) {
                    throw new IOException("Promotion move not representable in CBH S-box: " + m.getUci());
                }
                ctx.emitEscape(d1, d2);
                state.applyMove(fromSq, toSq, m.promotionPiece);
            } else {
                int rosterType = CbhCrypto.toRosterType(movingType);
                int slot = state.findPieceSlot(rosterType, fromSq);
                if (rosterType < 0 || slot < 0) {
                    throw new IOException("Piece not found in CBH roster for move: " + m.getUci());
                }
                int dxMod8;
                int dyMod8;
                if (state.toMove == 1 && rosterType == CbhCrypto.ROSTER_PAWN) {
                    dxMod8 = (fx - tx) & 7;
                    dyMod8 = (fy - ty) & 7;
                } else {
                    dxMod8 = (tx - fx) & 7;
                    dyMod8 = (ty - fy) & 7;
                }
                int op = CbhCrypto.encodeMoveOpcode(rosterType, slot, dxMod8, dyMod8);
                if (op < 0) {
                    throw new IOException("Move cannot be encoded in classic CBH 1-byte cipher: "
                            + m.getUci() + " (pieceType=" + rosterType + ", slot=" + slot + ")");
                }
                ctx.emitMoveByte(op);
                state.applyMove(fromSq, toSq, CONSTANTS.EMPTY);
            }
        }

        if (isMainline) {
            ctx.mainlinePlies++;
        }

        ctx.moveOnlySeq++;
        int cbaKey;
        if (ctx.hasSeenPop) {
            cbaKey = ctx.moveOnlySeq - 1;
        } else {
            cbaKey = ctx.moveOnlySeq;
        }
        if (cbaKey > 0 && CbCommon.hasNodeAnnotations(node)) {
            writeNodeAnnotations(ctx.cbaChunks, cbaKey, node);
        }
    }

    private static void writeNodeAnnotations(ByteArrayOutputStream out, int mvIdx, GameNode node) {
        String comment = node.getComment();
        if (comment != null && !comment.isEmpty()) {
            byte[] textBytes = comment.getBytes(CbCommon.WIN1252);
            int maxTextLen = 65535 - 8;
            int len = Math.min(textBytes.length, maxTextLen);
            int chunkLen = 8 + len;
            byte[] chunk = new byte[chunkLen];
            CbCommon.writeU24BE(chunk, 0, mvIdx);
            chunk[3] = 0x02;
            CbCommon.writeU16BE(chunk, 4, chunkLen);
            chunk[6] = 0x00;
            chunk[7] = 0x00; // Universal language
            System.arraycopy(textBytes, 0, chunk, 8, len);
            out.writeBytes(chunk);
        }

        int[] nags = CbCommon.extractNags(node.getNags());
        if (nags[0] != 0 || nags[1] != 0 || nags[2] != 0) {
            byte[] chunk = new byte[9];
            CbCommon.writeU24BE(chunk, 0, mvIdx);
            chunk[3] = 0x03;
            CbCommon.writeU16BE(chunk, 4, 9);
            chunk[6] = (byte) nags[0];
            chunk[7] = (byte) nags[1];
            chunk[8] = (byte) nags[2];
            out.writeBytes(chunk);
        }

        ArrayList<ColoredField> fields = node.getColoredFields();
        if (!fields.isEmpty()) {
            int count = Math.min(fields.size(), (65535 - 6) / 2);
            int chunkLen = 6 + count * 2;
            byte[] chunk = new byte[chunkLen];
            CbCommon.writeU24BE(chunk, 0, mvIdx);
            chunk[3] = 0x04;
            CbCommon.writeU16BE(chunk, 4, chunkLen);
            int q = 6;
            for (int i = 0; i < count; i++) {
                ColoredField cf = fields.get(i);
                chunk[q++] = 2; // green
                chunk[q++] = (byte) ((cf.x * 8) + cf.y + 1);
            }
            out.writeBytes(chunk);
        }

        ArrayList<Arrow> arrows = node.getArrows();
        if (!arrows.isEmpty()) {
            int count = Math.min(arrows.size(), (65535 - 6) / 3);
            int chunkLen = 6 + count * 3;
            byte[] chunk = new byte[chunkLen];
            CbCommon.writeU24BE(chunk, 0, mvIdx);
            chunk[3] = 0x05;
            CbCommon.writeU16BE(chunk, 4, chunkLen);
            int q = 6;
            for (int i = 0; i < count; i++) {
                Arrow a = arrows.get(i);
                chunk[q++] = 2; // green
                chunk[q++] = (byte) ((a.xFrom * 8) + a.yFrom + 1);
                chunk[q++] = (byte) ((a.xTo * 8) + a.yTo + 1);
            }
            out.writeBytes(chunk);
        }
    }
}
