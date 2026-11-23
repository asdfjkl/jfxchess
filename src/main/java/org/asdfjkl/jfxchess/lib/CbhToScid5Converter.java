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

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

import static org.asdfjkl.jfxchess.lib.ScidPosition.*;

/**
 * High-speed, bounded-memory direct binary converter from ChessBase `.cbh` and
 * `.2cbh` databases into SCID 5 (`.si5` / `.sn5` / `.sg5`) without creating
 * intermediate {@link Game} or {@link Board} ASTs.
 */
public final class CbhToScid5Converter {

    private static final int BATCH_GAMES = 4096;
    private static final int SG5_BLOCK_SIZE = 131072;
    private static final byte[] ZERO_PAD = new byte[SG5_BLOCK_SIZE];
    private static final int[] RATING_THRESHOLDS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 30, 40, 50
    };
    private static final int[] SQ_TO_HP_NIBBLE = new int[64];

    static {
        Arrays.fill(SQ_TO_HP_NIBBLE, -1);
        SQ_TO_HP_NIBBLE[(6 << 3) | 7] = 0;
        SQ_TO_HP_NIBBLE[(6 << 3) | 6] = 1;
        SQ_TO_HP_NIBBLE[(6 << 3) | 5] = 2;
        SQ_TO_HP_NIBBLE[(6 << 3) | 4] = 3;
        SQ_TO_HP_NIBBLE[(6 << 3) | 3] = 4;
        SQ_TO_HP_NIBBLE[(6 << 3) | 2] = 5;
        SQ_TO_HP_NIBBLE[(6 << 3) | 1] = 6;
        SQ_TO_HP_NIBBLE[(6 << 3) | 0] = 7;

        SQ_TO_HP_NIBBLE[(1 << 3) | 7] = 8;
        SQ_TO_HP_NIBBLE[(1 << 3) | 6] = 9;
        SQ_TO_HP_NIBBLE[(1 << 3) | 5] = 10;
        SQ_TO_HP_NIBBLE[(1 << 3) | 4] = 11;
        SQ_TO_HP_NIBBLE[(1 << 3) | 3] = 12;
        SQ_TO_HP_NIBBLE[(1 << 3) | 2] = 13;
        SQ_TO_HP_NIBBLE[(1 << 3) | 1] = 14;
        SQ_TO_HP_NIBBLE[(1 << 3) | 0] = 15;
    }

    private CbhToScid5Converter() {
    }

    /**
     * Converts a `.cbh` or `.2cbh` database into a new SCID 5 database at {@code targetSi5Path}.
     * On cancellation or error, partially created `.si5`, `.sn5`, and `.sg5` files are deleted.
     *
     * @return the number of non-guiding games written
     */
    public static int convert(Path sourcePath, Path targetSi5Path, ProgressListener listener) throws IOException {
        Objects.requireNonNull(sourcePath, "sourcePath");
        Objects.requireNonNull(targetSi5Path, "targetSi5Path");

        Path normalizedTargetBase = resolveScid5Base(targetSi5Path);
        Path si5 = Path.of(normalizedTargetBase.toString() + ".si5");
        Path sn5 = Path.of(normalizedTargetBase.toString() + ".sn5");
        Path sg5 = Path.of(normalizedTargetBase.toString() + ".sg5");

        Path parent = si5.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        String lowerSrc = sourcePath.toString().toLowerCase(Locale.ROOT);
        boolean isTwoCbh = lowerSrc.endsWith(".2cbh") || lowerSrc.endsWith(".2cbg")
                || lowerSrc.endsWith(".2cba") || lowerSrc.endsWith(".2lid");

        boolean completed = false;
        try {
            int count;
            if (isTwoCbh) {
                count = convertTwoCbh(sourcePath, si5, sn5, sg5, listener);
            } else {
                count = convertClassicCbh(sourcePath, si5, sn5, sg5, listener);
            }
            if (listener != null && listener.isCancelled()) {
                deleteTargetFiles(si5, sn5, sg5);
                return 0;
            }
            if (listener != null) {
                listener.onProgress(100);
            }
            completed = true;
            return count;
        } finally {
            if (!completed) {
                deleteTargetFiles(si5, sn5, sg5);
            }
        }
    }

    private static void deleteTargetFiles(Path si5, Path sn5, Path sg5) {
        try {
            Files.deleteIfExists(si5);
        } catch (IOException ignored) {
        }
        try {
            Files.deleteIfExists(sn5);
        } catch (IOException ignored) {
        }
        try {
            Files.deleteIfExists(sg5);
        } catch (IOException ignored) {
        }
    }

    private static Path resolveScid5Base(Path p) {
        Path norm = p.toAbsolutePath().normalize();
        String name = norm.getFileName().toString();
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".si5") || lower.endsWith(".sn5") || lower.endsWith(".sg5")) {
            String stem = name.substring(0, name.length() - 4);
            Path parent = norm.getParent();
            if (parent != null) {
                return parent.resolve(stem);
            }
            return Path.of(stem);
        }
        return norm;
    }

    private static final class ByteBufferWriter {
        byte[] buf;
        int len;

        ByteBufferWriter(int cap) {
            this.buf = new byte[cap];
        }

        void reset() {
            len = 0;
        }

        void write(int b) {
            if (len >= buf.length) {
                buf = Arrays.copyOf(buf, buf.length * 2);
            }
            buf[len++] = (byte) b;
        }

        void writeBytes(byte[] src, int off, int count) {
            if (count <= 0) {
                return;
            }
            if (len + count > buf.length) {
                int newCap = Math.max(buf.length * 2, len + count);
                buf = Arrays.copyOf(buf, newCap);
            }
            System.arraycopy(src, off, buf, len, count);
            len += count;
        }
    }

    private static final class Arena {
        int capacity = 1024;
        int count = 0;

        int[] fromSq = new int[capacity];
        int[] toSq = new int[capacity];
        int[] promo = new int[capacity];
        byte[] moveFlags = new byte[capacity]; // 1=castleK, 2=castleQ, 4=null
        int[] parent = new int[capacity];
        int[] firstChild = new int[capacity];
        int[] lastChild = new int[capacity];
        int[] nextSibling = new int[capacity];
        int[] prevSibling = new int[capacity];

        String[] preComment = new String[capacity];
        byte[] prePrio = new byte[capacity];
        String[] postComment = new String[capacity];
        byte[] postPrio = new byte[capacity];
        byte[] moveNag = new byte[capacity];
        byte[] evalNag = new byte[capacity];
        byte[] prefixNag = new byte[capacity];

        int[] cRevToArena = new int[capacity];
        int cRevCount = 0;
        int[] cbaSeqToArena = new int[capacity];
        int maxCbaSeq = 0;

        {
            Arrays.fill(cbaSeqToArena, -1);
        }

        void reset() {
            for (int i = 0; i < count; i++) {
                preComment[i] = null;
                prePrio[i] = 0;
                postComment[i] = null;
                postPrio[i] = 0;
                moveNag[i] = 0;
                evalNag[i] = 0;
                prefixNag[i] = 0;
            }
            if (count == 0) {
                Arrays.fill(cbaSeqToArena, -1);
            } else {
                for (int i = 0; i <= maxCbaSeq && i < cbaSeqToArena.length; i++) {
                    cbaSeqToArena[i] = -1;
                }
            }
            count = 1; // index 0 = root
            cRevCount = 0;
            maxCbaSeq = 0;
            parent[0] = -1;
            firstChild[0] = -1;
            lastChild[0] = -1;
            nextSibling[0] = -1;
            prevSibling[0] = -1;
        }

        private void ensureCapacity(int needed) {
            if (needed <= capacity) {
                return;
            }
            int newCap = Math.max(capacity * 2, needed);
            fromSq = Arrays.copyOf(fromSq, newCap);
            toSq = Arrays.copyOf(toSq, newCap);
            promo = Arrays.copyOf(promo, newCap);
            moveFlags = Arrays.copyOf(moveFlags, newCap);
            parent = Arrays.copyOf(parent, newCap);
            firstChild = Arrays.copyOf(firstChild, newCap);
            lastChild = Arrays.copyOf(lastChild, newCap);
            nextSibling = Arrays.copyOf(nextSibling, newCap);
            prevSibling = Arrays.copyOf(prevSibling, newCap);
            preComment = Arrays.copyOf(preComment, newCap);
            prePrio = Arrays.copyOf(prePrio, newCap);
            postComment = Arrays.copyOf(postComment, newCap);
            postPrio = Arrays.copyOf(postPrio, newCap);
            moveNag = Arrays.copyOf(moveNag, newCap);
            evalNag = Arrays.copyOf(evalNag, newCap);
            prefixNag = Arrays.copyOf(prefixNag, newCap);
            cRevToArena = Arrays.copyOf(cRevToArena, newCap);
            int oldLen = cbaSeqToArena.length;
            cbaSeqToArena = Arrays.copyOf(cbaSeqToArena, newCap);
            Arrays.fill(cbaSeqToArena, oldLen, newCap, -1);
            capacity = newCap;
        }

        int addChild(int parentIdx, int from, int to, int scidPromo, byte flags) {
            int idx = count;
            ensureCapacity(idx + 1);
            count++;
            fromSq[idx] = from;
            toSq[idx] = to;
            promo[idx] = scidPromo;
            moveFlags[idx] = flags;
            parent[idx] = parentIdx;
            firstChild[idx] = -1;
            lastChild[idx] = -1;
            nextSibling[idx] = -1;
            int prevLast = lastChild[parentIdx];
            prevSibling[idx] = prevLast;
            if (prevLast == -1) {
                firstChild[parentIdx] = idx;
            } else {
                nextSibling[prevLast] = idx;
            }
            lastChild[parentIdx] = idx;
            return idx;
        }

        void mapCbaSeq(int seq, int nodeIdx) {
            if (seq < 0) {
                return;
            }
            if (seq >= cbaSeqToArena.length) {
                int oldLen = cbaSeqToArena.length;
                int newLen = Math.max(oldLen * 2, seq + 64);
                cbaSeqToArena = Arrays.copyOf(cbaSeqToArena, newLen);
                Arrays.fill(cbaSeqToArena, oldLen, newLen, -1);
            }
            if (cbaSeqToArena[seq] == -1) {
                cbaSeqToArena[seq] = nodeIdx;
            }
            if (seq > maxCbaSeq) {
                maxCbaSeq = seq;
            }
        }

        void assignCRevMainlineFirst() {
            cRevCount = 0;
            collectCRev(0);
        }

        private void collectCRev(int nodeIdx) {
            cRevToArena[cRevCount++] = nodeIdx;
            int child = lastChild[nodeIdx];
            while (child != -1) {
                collectCRev(child);
                child = prevSibling[child];
            }
        }

        void setPreText(int nodeIdx, String text, int prio) {
            if (nodeIdx < 0 || nodeIdx >= count || text == null || text.isEmpty()) {
                return;
            }
            int curPrio = prePrio[nodeIdx] & 0xFF;
            if (prio > curPrio) {
                preComment[nodeIdx] = text.trim();
                prePrio[nodeIdx] = (byte) prio;
            } else if (prio == curPrio) {
                preComment[nodeIdx] = CbCommon.appendCommentPart(preComment[nodeIdx], text);
            }
        }

        void setPostText(int nodeIdx, String text, int prio) {
            if (nodeIdx < 0 || nodeIdx >= count || text == null || text.isEmpty()) {
                return;
            }
            int curPrio = postPrio[nodeIdx] & 0xFF;
            if (prio > curPrio) {
                postComment[nodeIdx] = text.trim();
                postPrio[nodeIdx] = (byte) prio;
            } else if (prio == curPrio) {
                postComment[nodeIdx] = CbCommon.appendCommentPart(postComment[nodeIdx], text);
            }
        }

        String getCombinedComment(int nodeIdx) {
            String pre = preComment[nodeIdx];
            String post = postComment[nodeIdx];
            if (pre == null || pre.isEmpty()) {
                if (post == null) {
                    return "";
                }
                return post;
            }
            if (post == null || post.isEmpty()) {
                return pre;
            }
            return pre + " " + post;
        }
    }

    private static final class ScidPosPool {
        ScidPosition[] pool = new ScidPosition[128];
        int top = 0;

        ScidPosPool() {
            for (int i = 0; i < pool.length; i++) {
                pool[i] = new ScidPosition();
            }
        }

        void reset() {
            top = 0;
        }

        ScidPosition acquire() {
            if (top >= pool.length) {
                int oldLen = pool.length;
                pool = Arrays.copyOf(pool, oldLen * 2);
                for (int i = oldLen; i < pool.length; i++) {
                    pool[i] = new ScidPosition();
                }
            }
            return pool[top++];
        }

        void release() {
            if (top > 0) {
                top--;
            }
        }
    }

    private static final class FastNamebaseWriter {
        private final OutputStream sn5Out;
        private final HashMap<String, Integer> players = new HashMap<>(4096);
        private final HashMap<String, Integer> events = new HashMap<>(2048);
        private final HashMap<String, Integer> sites = new HashMap<>(2048);
        private final HashMap<String, Integer> rounds = new HashMap<>(256);
        private int nextPlayerId = 0;
        private int nextEventId = 0;
        private int nextSiteId = 0;
        private int nextRoundId = 0;
        private final byte[] varintScratch = new byte[10];

        FastNamebaseWriter(OutputStream sn5Out) {
            this.sn5Out = sn5Out;
        }

        int getOrAddPlayer(String name) throws IOException {
            return getOrAdd(players, Scid5Namebase.NAME_PLAYER, name);
        }

        int getOrAddEvent(String name) throws IOException {
            return getOrAdd(events, Scid5Namebase.NAME_EVENT, name);
        }

        int getOrAddSite(String name) throws IOException {
            return getOrAdd(sites, Scid5Namebase.NAME_SITE, name);
        }

        int getOrAddRound(String name) throws IOException {
            return getOrAdd(rounds, Scid5Namebase.NAME_ROUND, name);
        }

        private int getOrAdd(HashMap<String, Integer> map, int type, String raw) throws IOException {
            String name;
            if (raw != null) {
                name = raw;
            } else {
                name = "";
            }
            Integer existing = map.get(name);
            if (existing != null) {
                return existing;
            }
            int id;
            if (type == Scid5Namebase.NAME_PLAYER) {
                id = nextPlayerId++;
            } else if (type == Scid5Namebase.NAME_EVENT) {
                id = nextEventId++;
            } else if (type == Scid5Namebase.NAME_SITE) {
                id = nextSiteId++;
            } else {
                id = nextRoundId++;
            }
            map.put(name, id);
            byte[] strBytes = name.getBytes(StandardCharsets.UTF_8);
            long v = (((long) strBytes.length) << 3) | (type & 0x07);
            int p = 0;
            while ((v & ~0x7FL) != 0) {
                varintScratch[p++] = (byte) ((v & 0x7F) | 0x80);
                v >>>= 7;
            }
            varintScratch[p++] = (byte) (v & 0x7F);
            sn5Out.write(varintScratch, 0, p);
            if (strBytes.length > 0) {
                sn5Out.write(strBytes);
            }
            return id;
        }
    }

    private static final class Scid5RecordEmitter {
        final ByteBufferWriter tagsBuf = new ByteBufferWriter(128);
        final ByteBufferWriter movesBuf = new ByteBufferWriter(4096);
        final ByteBufferWriter commentsBuf = new ByteBufferWriter(1024);
        final ByteBufferWriter fullBlobBuf = new ByteBufferWriter(8192);
        final ArrayList<String> comments = new ArrayList<>(16);
        final int[] hpDepartures = new int[16];
        int hpCount = 0;
        int homePawnMask = 0xFFFF;
        int commentCount = 0;
        int variationCount = 0;
        int nagCount = 0;
        int mainlinePlies = 0;
        int flags = 0;
        final ScidPosition scidPos = new ScidPosition();
        final ScidPosition endPos = new ScidPosition();
        final ScidPosPool posPool = new ScidPosPool();
        final byte[] si5Record = new byte[Scid5Index.INDEX_ENTRY_SIZE];
        final ByteBuffer si5Wrapper = ByteBuffer.wrap(si5Record).order(ByteOrder.LITTLE_ENDIAN);
        final byte[] hpData = new byte[8];
        long currentSg5Offset = 0L;

        long writeGame(
                Arena arena,
                boolean customStart,
                String customFen,
                int whiteId,
                int blackId,
                int eventId,
                int siteId,
                int roundId,
                int whiteElo,
                int blackElo,
                int gameDatePacked,
                int scidResultCode,
                int scidEcoCode,
                String annotator,
                String source,
                OutputStream sg5Out,
                OutputStream si5Out) throws IOException {

            tagsBuf.reset();
            movesBuf.reset();
            commentsBuf.reset();
            fullBlobBuf.reset();
            comments.clear();
            hpCount = 0;
            homePawnMask = 0xFFFF;
            commentCount = 0;
            variationCount = 0;
            nagCount = 0;
            mainlinePlies = 0;
            flags = 0;
            posPool.reset();

            // Extra tags (codes 243=Annotator, 249=Source) in sorted tag-name order
            if (annotator != null && !annotator.isEmpty()) {
                writeCommonExtraTag(243, annotator);
            }
            if (source != null && !source.isEmpty()) {
                writeCommonExtraTag(249, source);
            }
            tagsBuf.write(0x00);

            if (customStart && customFen != null && !customFen.isEmpty()) {
                flags |= Scid5GameInfo.FLAG_CUSTOM_START;
                scidPos.setupFen(customFen);
            } else {
                customStart = false;
                scidPos.stdStart();
            }
            endPos.copyFrom(scidPos);

            String rootComment = arena.getCombinedComment(0);
            if (!rootComment.isEmpty()) {
                movesBuf.write(0x0C);
                comments.add(rootComment);
                commentCount++;
            }
            emitNodeNags(arena, 0);

            emitSubtree(arena, 0, true);
            movesBuf.write(0x0F);

            // Start board section
            int startFlags = 0;
            if (customStart) {
                startFlags |= 1;
            }
            if ((flags & Scid5GameInfo.FLAG_PROMO) != 0) {
                startFlags |= 2;
            }
            if ((flags & Scid5GameInfo.FLAG_UNDER_PROMO) != 0) {
                startFlags |= 4;
            }

            fullBlobBuf.writeBytes(tagsBuf.buf, 0, tagsBuf.len);
            fullBlobBuf.write(startFlags);
            if (customStart) {
                byte[] fenBytes = customFen.getBytes(StandardCharsets.US_ASCII);
                fullBlobBuf.writeBytes(fenBytes, 0, fenBytes.length);
                fullBlobBuf.write(0x00);
            }
            fullBlobBuf.writeBytes(movesBuf.buf, 0, movesBuf.len);
            for (int i = 0; i < comments.size(); i++) {
                byte[] cb = comments.get(i).getBytes(StandardCharsets.UTF_8);
                fullBlobBuf.writeBytes(cb, 0, cb.length);
                fullBlobBuf.write(0x00);
            }

            int dataSize = fullBlobBuf.len;
            long currentBlock = currentSg5Offset / SG5_BLOCK_SIZE;
            long endBlock = (currentSg5Offset + dataSize - 1) / SG5_BLOCK_SIZE;
            long actualOffset = currentSg5Offset;
            if (endBlock != currentBlock) {
                long nextBlockOffset = (currentBlock + 1) * SG5_BLOCK_SIZE;
                int pad = (int) (nextBlockOffset - currentSg5Offset);
                sg5Out.write(ZERO_PAD, 0, pad);
                actualOffset = nextBlockOffset;
            }
            sg5Out.write(fullBlobBuf.buf, 0, dataSize);
            currentSg5Offset = actualOffset + dataSize;

            Arrays.fill(hpData, (byte) 0);
            int clampedHp = Math.min(hpCount, 16);
            for (int i = 0; i < clampedHp; i++) {
                int byteIdx = i / 2;
                int nibble = hpDepartures[i];
                if ((i & 1) == 0) {
                    hpData[byteIdx] |= (byte) ((nibble & 0x0F) << 4);
                } else {
                    hpData[byteIdx] |= (byte) (nibble & 0x0F);
                }
            }

            int finalMatSig = computeFinalMatSig(endPos);
            int halfMoves = Math.min(mainlinePlies, 1023);
            int commentRating = encodeRating(commentCount);
            int variationRating = encodeRating(variationCount);
            int nagRating = encodeRating(nagCount);

            writeIndexEntry(
                    commentRating,
                    variationRating,
                    nagRating,
                    whiteId,
                    blackId,
                    eventId,
                    siteId,
                    roundId,
                    Math.min(whiteElo, 4095),
                    Math.min(blackElo, 4095),
                    gameDatePacked,
                    halfMoves,
                    flags,
                    dataSize,
                    actualOffset,
                    finalMatSig,
                    clampedHp,
                    scidResultCode,
                    scidEcoCode
            );
            si5Out.write(si5Record, 0, Scid5Index.INDEX_ENTRY_SIZE);
            return currentSg5Offset;
        }

        private void writeCommonExtraTag(int commonCode, String val) {
            byte[] valBytes = val.getBytes(StandardCharsets.UTF_8);
            int vLen = Math.min(valBytes.length, 255);
            tagsBuf.write(commonCode);
            tagsBuf.write(vLen);
            tagsBuf.writeBytes(valBytes, 0, vLen);
        }

        private void writeIndexEntry(
                int commentRating,
                int variationRating,
                int nagRating,
                int whiteId,
                int blackId,
                int eventId,
                int siteId,
                int roundId,
                int whiteElo,
                int blackElo,
                int gameDate,
                int halfMoves,
                int idxFlags,
                int dataSize,
                long offset,
                int finalMatSig,
                int homePawnCount,
                int scidResultCode,
                int ecoCode) {
            si5Wrapper.clear();
            int w0 = ((commentRating & 0x0F) << 28) | (whiteId & 0x0FFFFFFF);
            int w1 = ((variationRating & 0x0F) << 28) | (blackId & 0x0FFFFFFF);
            int w2 = ((nagRating & 0x0F) << 28) | (eventId & 0x0FFFFFFF);
            int w3 = siteId;
            int w4 = roundId & 0x7FFFFFFF;
            int w5 = ((whiteElo & 0x0FFF) << 20) | (gameDate & 0x0FFFFF);
            int w6 = ((blackElo & 0x0FFF) << 20);
            int w7 = ((halfMoves & 0x03FF) << 22) | (idxFlags & 0x003FFFFF);
            int w8 = ((dataSize & 0x01FFFF) << 15) | ((int) ((offset >>> 32) & 0x7FFF));
            int w9 = (int) offset;
            int w10 = finalMatSig & 0x00FFFFFF;
            int w11 = ((homePawnCount & 0xFF) << 24)
                    | ((scidResultCode & 0x03) << 16)
                    | (ecoCode & 0xFFFF);

            si5Wrapper.putInt(w0);
            si5Wrapper.putInt(w1);
            si5Wrapper.putInt(w2);
            si5Wrapper.putInt(w3);
            si5Wrapper.putInt(w4);
            si5Wrapper.putInt(w5);
            si5Wrapper.putInt(w6);
            si5Wrapper.putInt(w7);
            si5Wrapper.putInt(w8);
            si5Wrapper.putInt(w9);
            si5Wrapper.putInt(w10);
            si5Wrapper.putInt(w11);
            si5Wrapper.put(hpData, 0, 8);
        }

        private void emitSubtree(Arena arena, int current, boolean isMainline) {
            int node = current;
            while (true) {
                int mainChild = arena.lastChild[node];
                if (mainChild == -1) {
                    return;
                }
                int sibling = arena.prevSibling[mainChild];
                boolean hasSidelines = (sibling != -1);
                ScidPosition beforeMain = null;
                if (hasSidelines) {
                    beforeMain = posPool.acquire();
                    beforeMain.copyFrom(scidPos);
                }
                if (!encodeSingleArenaMove(arena, mainChild, isMainline)) {
                    if (hasSidelines) {
                        posPool.release();
                    }
                    return;
                }
                if (hasSidelines) {
                    ScidPosition afterMain = posPool.acquire();
                    afterMain.copyFrom(scidPos);
                    while (sibling != -1) {
                        variationCount++;
                        movesBuf.write(0x0D);
                        scidPos.copyFrom(beforeMain);
                        if (encodeSingleArenaMove(arena, sibling, false)) {
                            emitSubtree(arena, sibling, false);
                        }
                        movesBuf.write(0x0E);
                        sibling = arena.prevSibling[sibling];
                    }
                    scidPos.copyFrom(afterMain);
                    posPool.release();
                    posPool.release();
                }
                node = mainChild;
            }
        }

        private boolean encodeSingleArenaMove(Arena arena, int nodeIdx, boolean isMainline) {
            byte mf = arena.moveFlags[nodeIdx];
            if ((mf & 0x04) != 0) {
                movesBuf.write(0x00);
                scidPos.toMove = 1 - scidPos.toMove;
            } else {
                int from = arena.fromSq[nodeIdx];
                int to = arena.toSq[nodeIdx];
                int promo = arena.promo[nodeIdx];
                if (from < 0 || from >= 64 || to < 0 || to >= 64) {
                    return false;
                }
                int pieceNum = scidPos.listPos[from];
                int pieceType = scidPos.board[from];
                if (pieceNum < 0 || pieceType == EMPTY) {
                    return false;
                }

                if (promo == QUEEN) {
                    flags |= Scid5GameInfo.FLAG_PROMO;
                } else if (promo != EMPTY) {
                    flags |= Scid5GameInfo.FLAG_UNDER_PROMO;
                }

                if (pieceNum == 0) {
                    boolean isCastleK = (mf & 0x01) != 0
                            || ((scidPos.toMove == WHITE) && from == 4 && to == 6)
                            || ((scidPos.toMove == BLACK) && from == 60 && to == 62);
                    boolean isCastleQ = (mf & 0x02) != 0
                            || ((scidPos.toMove == WHITE) && from == 4 && to == 2)
                            || ((scidPos.toMove == BLACK) && from == 60 && to == 58);

                    if (isCastleK) {
                        movesBuf.write(0x0A);
                        scidPos.doMove(from, to, EMPTY, true, false);
                    } else if (isCastleQ) {
                        movesBuf.write(0x09);
                        scidPos.doMove(from, to, EMPTY, false, true);
                    } else {
                        int moveCode = kingDiffToCode(to - from);
                        if (moveCode < 0) {
                            return false;
                        }
                        movesBuf.write(moveCode);
                        scidPos.doMove(from, to, EMPTY, false, false);
                    }
                } else if (pieceType == PAWN) {
                    int step;
                    if (scidPos.toMove == WHITE) {
                        step = to - from;
                    } else {
                        step = from - to;
                    }
                    int moveCode;
                    if (promo != EMPTY) {
                        int base;
                        if (step == 7) {
                            base = 0;
                        } else if (step == 8) {
                            base = 1;
                        } else if (step == 9) {
                            base = 2;
                        } else {
                            return false;
                        }
                        int promoOff;
                        if (promo == QUEEN) {
                            promoOff = 0;
                        } else if (promo == ROOK) {
                            promoOff = 1;
                        } else if (promo == BISHOP) {
                            promoOff = 2;
                        } else if (promo == KNIGHT) {
                            promoOff = 3;
                        } else {
                            promoOff = 0;
                        }
                        moveCode = 3 + (promoOff * 3) + base;
                    } else {
                        if (step == 16) {
                            moveCode = 15;
                        } else if (step == 7) {
                            moveCode = 0;
                        } else if (step == 8) {
                            moveCode = 1;
                        } else if (step == 9) {
                            moveCode = 2;
                        } else {
                            return false;
                        }
                    }
                    movesBuf.write((pieceNum << 4) | moveCode);
                    scidPos.doMove(from, to, promo, false, false);
                } else if (pieceType == KNIGHT) {
                    int moveCode = knightDiffToCode(to - from);
                    if (moveCode < 0) {
                        return false;
                    }
                    movesBuf.write((pieceNum << 4) | moveCode);
                    scidPos.doMove(from, to, EMPTY, false, false);
                } else if (pieceType == BISHOP) {
                    int fromCol = from & 7;
                    int fromRow = from >> 3;
                    int toCol = to & 7;
                    int toRow = to >> 3;
                    int moveCode;
                    if ((toCol - fromCol) == (toRow - fromRow)) {
                        moveCode = toCol;
                    } else if ((toCol - fromCol) == -(toRow - fromRow)) {
                        moveCode = 8 + toCol;
                    } else {
                        return false;
                    }
                    movesBuf.write((pieceNum << 4) | moveCode);
                    scidPos.doMove(from, to, EMPTY, false, false);
                } else if (pieceType == ROOK) {
                    int fromCol = from & 7;
                    int fromRow = from >> 3;
                    int toCol = to & 7;
                    int toRow = to >> 3;
                    int moveCode;
                    if (toRow == fromRow) {
                        moveCode = toCol;
                    } else if (toCol == fromCol) {
                        moveCode = 8 + toRow;
                    } else {
                        return false;
                    }
                    movesBuf.write((pieceNum << 4) | moveCode);
                    scidPos.doMove(from, to, EMPTY, false, false);
                } else if (pieceType == QUEEN) {
                    int fromCol = from & 7;
                    int fromRow = from >> 3;
                    int toCol = to & 7;
                    int toRow = to >> 3;
                    if (toRow == fromRow) {
                        movesBuf.write((pieceNum << 4) | toCol);
                    } else if (toCol == fromCol) {
                        movesBuf.write((pieceNum << 4) | (8 + toRow));
                    } else {
                        movesBuf.write((pieceNum << 4) | fromCol);
                        movesBuf.write(to + 64);
                    }
                    scidPos.doMove(from, to, EMPTY, false, false);
                } else {
                    return false;
                }

                if (isMainline) {
                    int fromNibble = SQ_TO_HP_NIBBLE[from];
                    if (fromNibble != -1 && (homePawnMask & (1 << fromNibble)) != 0) {
                        homePawnMask &= ~(1 << fromNibble);
                        if (hpCount < 16) {
                            hpDepartures[hpCount++] = fromNibble;
                        }
                    }
                    int toNibble = SQ_TO_HP_NIBBLE[to];
                    if (toNibble != -1 && (homePawnMask & (1 << toNibble)) != 0) {
                        homePawnMask &= ~(1 << toNibble);
                        if (hpCount < 16) {
                            hpDepartures[hpCount++] = toNibble;
                        }
                    }
                }
            }

            if (isMainline) {
                mainlinePlies++;
                endPos.copyFrom(scidPos);
            }
            emitNodeNags(arena, nodeIdx);
            String c = arena.getCombinedComment(nodeIdx);
            if (!c.isEmpty()) {
                movesBuf.write(0x0C);
                comments.add(c);
                commentCount++;
            }
            return true;
        }

        private void emitNodeNags(Arena arena, int nodeIdx) {
            int n1 = arena.moveNag[nodeIdx] & 0xFF;
            if (n1 != 0) {
                movesBuf.write(0x0B);
                movesBuf.write(n1);
                nagCount++;
            }
            int n2 = arena.evalNag[nodeIdx] & 0xFF;
            if (n2 != 0 && n2 != n1) {
                movesBuf.write(0x0B);
                movesBuf.write(n2);
                nagCount++;
            }
            int n3 = arena.prefixNag[nodeIdx] & 0xFF;
            if (n3 != 0 && n3 != n1 && n3 != n2) {
                movesBuf.write(0x0B);
                movesBuf.write(n3);
                nagCount++;
            }
        }
    }

    private static int kingDiffToCode(int diff) {
        if (diff == -9) {
            return 1;
        } else if (diff == -8) {
            return 2;
        } else if (diff == -7) {
            return 3;
        } else if (diff == -1) {
            return 4;
        } else if (diff == 1) {
            return 5;
        } else if (diff == 7) {
            return 6;
        } else if (diff == 8) {
            return 7;
        } else if (diff == 9) {
            return 8;
        }
        return -1;
    }

    private static int knightDiffToCode(int diff) {
        if (diff == -17) {
            return 1;
        } else if (diff == -15) {
            return 2;
        } else if (diff == -10) {
            return 3;
        } else if (diff == -6) {
            return 4;
        } else if (diff == 6) {
            return 5;
        } else if (diff == 10) {
            return 6;
        } else if (diff == 15) {
            return 7;
        } else if (diff == 17) {
            return 8;
        }
        return -1;
    }

    private static int encodeRating(int count) {
        for (int i = RATING_THRESHOLDS.length - 1; i >= 0; i--) {
            if (count >= RATING_THRESHOLDS[i]) {
                return i;
            }
        }
        return 0;
    }

    private static int computeFinalMatSig(ScidPosition pos) {
        int wp = 0;
        int wn = 0;
        int wb = 0;
        int wr = 0;
        int wq = 0;
        int bp = 0;
        int bn = 0;
        int bb = 0;
        int br = 0;
        int bq = 0;

        for (int sq = 0; sq < 64; sq++) {
            int type = pos.board[sq];
            int col = pos.color[sq];
            if (col == WHITE) {
                if (type == PAWN) {
                    wp++;
                } else if (type == KNIGHT) {
                    wn++;
                } else if (type == BISHOP) {
                    wb++;
                } else if (type == ROOK) {
                    wr++;
                } else if (type == QUEEN) {
                    wq++;
                }
            } else if (col == BLACK) {
                if (type == PAWN) {
                    bp++;
                } else if (type == KNIGHT) {
                    bn++;
                } else if (type == BISHOP) {
                    bb++;
                } else if (type == ROOK) {
                    br++;
                } else if (type == QUEEN) {
                    bq++;
                }
            }
        }

        return (Math.min(bp, 15) & 0x0F)
                | ((Math.min(bn, 3) & 0x03) << 4)
                | ((Math.min(bb, 3) & 0x03) << 6)
                | ((Math.min(br, 3) & 0x03) << 8)
                | ((Math.min(bq, 3) & 0x03) << 10)
                | ((Math.min(wp, 15) & 0x0F) << 12)
                | ((Math.min(wn, 3) & 0x03) << 16)
                | ((Math.min(wb, 3) & 0x03) << 18)
                | ((Math.min(wr, 3) & 0x03) << 20)
                | ((Math.min(wq, 3) & 0x03) << 22);
    }

    private static int convertClassicCbh(
            Path sourcePath,
            Path si5Path,
            Path sn5Path,
            Path sg5Path,
            ProgressListener listener) throws IOException {

        Path base = CbhChessDatabase.resolveBasePath(sourcePath.toString());
        Path cbh = CbhChessDatabase.resolveExistingSibling(base, ".cbh");
        if (!Files.exists(cbh)) {
            throw new FileNotFoundException("CBH index file not found: " + cbh);
        }
        Path cbg = CbhChessDatabase.resolveExistingSibling(base, ".cbg");
        Path cba = CbhChessDatabase.resolveExistingSibling(base, ".cba");

        CbhNamebase namebase = new CbhNamebase();
        namebase.load(base);

        int[] playerMap = new int[namebase.getPlayerCount()];
        Arrays.fill(playerMap, -1);
        int[] eventMap = new int[namebase.getTournamentCount()];
        Arrays.fill(eventMap, -1);
        int[] siteMap = new int[namebase.getTournamentCount()];
        Arrays.fill(siteMap, -1);
        int[] roundSimpleMap = new int[256];
        Arrays.fill(roundSimpleMap, -1);
        HashMap<Integer, Integer> roundSubMap = new HashMap<>(64);
        String[] annotators = new String[namebase.getAnnotatorCount()];
        String[] sources = new String[namebase.getSourceCount()];

        long[] batchCbgOffset = new long[BATCH_GAMES];
        long[] batchCbaOffset = new long[BATCH_GAMES];
        int[] batchWhiteId = new int[BATCH_GAMES];
        int[] batchBlackId = new int[BATCH_GAMES];
        int[] batchTourId = new int[BATCH_GAMES];
        int[] batchAnnoId = new int[BATCH_GAMES];
        int[] batchSrcId = new int[BATCH_GAMES];
        int[] batchDate = new int[BATCH_GAMES];
        int[] batchRes = new int[BATCH_GAMES];
        int[] batchRound = new int[BATCH_GAMES];
        int[] batchSubround = new int[BATCH_GAMES];
        int[] batchWElo = new int[BATCH_GAMES];
        int[] batchBElo = new int[BATCH_GAMES];
        int[] batchEco = new int[BATCH_GAMES];

        byte[][] batchCbgPayloads = new byte[BATCH_GAMES][];
        byte[][] batchCbaPayloads = new byte[BATCH_GAMES][];
        Integer[] sortOrder = new Integer[BATCH_GAMES];

        byte[] readBuf = new byte[BATCH_GAMES * CbhChessDatabase.CBH_RECORD_SIZE];
        byte[] hdr4 = new byte[4];
        byte[] hdr14 = new byte[14];

        CbhCrypto.RosterState roster = new CbhCrypto.RosterState();
        CbhCrypto.RosterState[] rosterStack = new CbhCrypto.RosterState[64];
        int[] nodeStack = new int[64];
        for (int i = 0; i < 64; i++) {
            rosterStack[i] = new CbhCrypto.RosterState();
        }
        Arena arena = new Arena();
        Scid5RecordEmitter emitter = new Scid5RecordEmitter();

        int convertedCount = 0;

        try (FileChannel cbhCh = FileChannel.open(cbh, StandardOpenOption.READ);
             FileChannel cbgCh = openOptionalReadChannel(cbg);
             FileChannel cbaCh = openOptionalReadChannel(cba);
             BufferedOutputStream si5Out = new BufferedOutputStream(
                     Files.newOutputStream(si5Path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING), 262144);
             BufferedOutputStream sn5Out = new BufferedOutputStream(
                     Files.newOutputStream(sn5Path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING), 262144);
             BufferedOutputStream sg5Out = new BufferedOutputStream(
                     Files.newOutputStream(sg5Path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING), 262144)) {

            FastNamebaseWriter nameWriter = new FastNamebaseWriter(sn5Out);
            long fileSize = cbhCh.size();
            if (fileSize <= CbhChessDatabase.CBH_HEADER_SIZE) {
                return 0;
            }
            long cbgSize = 0L;
            if (cbgCh != null) {
                cbgSize = cbgCh.size();
            }
            long cbaSize = 0L;
            if (cbaCh != null) {
                cbaSize = cbaCh.size();
            }

            int totalRecords = (int) ((fileSize - CbhChessDatabase.CBH_HEADER_SIZE) / CbhChessDatabase.CBH_RECORD_SIZE);
            long pos = CbhChessDatabase.CBH_HEADER_SIZE;
            int rawIdx = 0;

            while (rawIdx < totalRecords) {
                if (listener != null && listener.isCancelled()) {
                    return convertedCount;
                }
                int recWant = Math.min(BATCH_GAMES, totalRecords - rawIdx);
                int bytesRead = CbhChessDatabase.readUpToAt(
                        cbhCh, pos, readBuf, recWant * CbhChessDatabase.CBH_RECORD_SIZE);
                int recsRead = bytesRead / CbhChessDatabase.CBH_RECORD_SIZE;
                if (recsRead <= 0) {
                    break;
                }
                pos += (long) recsRead * CbhChessDatabase.CBH_RECORD_SIZE;
                rawIdx += recsRead;

                int batchCount = 0;
                for (int i = 0; i < recsRead; i++) {
                    int off = i * CbhChessDatabase.CBH_RECORD_SIZE;
                    int fl = readBuf[off] & 0xFF;
                    if ((fl & 0x01) != 0 && (fl & 0x02) == 0 && (fl & 0x80) == 0) {
                        batchCbgOffset[batchCount] = CbCommon.readU32BE(readBuf, off + 0x01);
                        batchCbaOffset[batchCount] = CbCommon.readU32BE(readBuf, off + 0x05);
                        batchWhiteId[batchCount] = CbCommon.readU24BE(readBuf, off + 0x09);
                        batchBlackId[batchCount] = CbCommon.readU24BE(readBuf, off + 0x0C);
                        batchTourId[batchCount] = CbCommon.readU24BE(readBuf, off + 0x0F);
                        batchAnnoId[batchCount] = CbCommon.readU24BE(readBuf, off + 0x12);
                        batchSrcId[batchCount] = CbCommon.readU24BE(readBuf, off + 0x15);
                        batchDate[batchCount] = CbCommon.readU24BE(readBuf, off + 0x18);
                        batchRes[batchCount] = readBuf[off + 0x1B] & 0xFF;
                        batchRound[batchCount] = readBuf[off + 0x1D] & 0xFF;
                        batchSubround[batchCount] = readBuf[off + 0x1E] & 0xFF;
                        batchWElo[batchCount] = CbCommon.readU16BE(readBuf, off + 0x1F);
                        batchBElo[batchCount] = CbCommon.readU16BE(readBuf, off + 0x21);
                        batchEco[batchCount] = CbCommon.readU16BE(readBuf, off + 0x23);
                        sortOrder[batchCount] = batchCount;
                        batchCbgPayloads[batchCount] = null;
                        batchCbaPayloads[batchCount] = null;
                        batchCount++;
                    }
                }

                if (batchCount == 0) {
                    continue;
                }

                // Read CBG blocks in ascending file offset order
                if (cbgCh != null) {
                    Arrays.sort(sortOrder, 0, batchCount, (a, b) -> Long.compare(batchCbgOffset[a], batchCbgOffset[b]));
                    for (int k = 0; k < batchCount; k++) {
                        int idx = sortOrder[k];
                        long off = batchCbgOffset[idx];
                        if (off >= 26L && off + 4L <= cbgSize) {
                            if (CbhChessDatabase.readUpToAt(cbgCh, off, hdr4, 4) == 4) {
                                int h = CbCommon.readI32BE(hdr4, 0);
                                if ((h & 0x80000000) == 0) {
                                    int blockLen = h & 0x00FFFFFF;
                                    if (blockLen >= 4 && blockLen <= 16_777_216) {
                                        int totalRead = Math.min(blockLen, (int) (cbgSize - off));
                                        byte[] full = new byte[totalRead];
                                        System.arraycopy(hdr4, 0, full, 0, 4);
                                        if (totalRead > 4) {
                                            ByteBuffer bb = ByteBuffer.wrap(full, 4, totalRead - 4);
                                            long cur = off + 4L;
                                            while (bb.hasRemaining()) {
                                                int n = cbgCh.read(bb, cur);
                                                if (n < 0) {
                                                    break;
                                                }
                                                cur += n;
                                            }
                                        }
                                        batchCbgPayloads[idx] = full;
                                    }
                                }
                            }
                        }
                    }
                }

                // Read CBA blocks in ascending file offset order
                if (cbaCh != null) {
                    Arrays.sort(sortOrder, 0, batchCount, (a, b) -> Long.compare(batchCbaOffset[a], batchCbaOffset[b]));
                    for (int k = 0; k < batchCount; k++) {
                        int idx = sortOrder[k];
                        long off = batchCbaOffset[idx];
                        if (off > 0L && off + 14L <= cbaSize) {
                            if (CbhChessDatabase.readUpToAt(cbaCh, off, hdr14, 14) == 14) {
                                int totLen = CbCommon.readI32BE(hdr14, 10);
                                if (totLen > 14 && totLen <= 16_777_216) {
                                    int bodyLen = (int) Math.min((long) totLen - 14L, cbaSize - (off + 14L));
                                    if (bodyLen > 0) {
                                        byte[] cbaBody = new byte[bodyLen];
                                        int actual = CbhChessDatabase.readUpToAt(cbaCh, off + 14L, cbaBody, bodyLen);
                                        if (actual > 0) {
                                            batchCbaPayloads[idx] = cbaBody;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Encode batch sequentially
                for (int i = 0; i < batchCount; i++) {
                    if (listener != null && listener.isCancelled()) {
                        return convertedCount;
                    }

                    arena.reset();
                    arena.mapCbaSeq(0, 0);

                    boolean customStart = false;
                    String customFen = null;
                    byte[] cbgData = batchCbgPayloads[i];
                    if (cbgData != null && cbgData.length >= 4) {
                        int hdr1 = CbCommon.readI32BE(cbgData, 0);
                        int posInBlock = 4;
                        if ((hdr1 & 0x40000000) != 0 && cbgData.length >= 32) {
                            customFen = CbhCrypto.decodeCustomSetup(cbgData, 4, roster);
                            customStart = true;
                            posInBlock = 32;
                        } else {
                            roster.resetStandard();
                        }

                        int curNode = 0;
                        int mc = 0;
                        int moveOnlySeq = 0;
                        boolean hasSeenPop = false;
                        int depth = 0;

                        while (posInBlock < cbgData.length) {
                            int dec = (cbgData[posInBlock++] - mc) & 0xFF;
                            int kind = CbhCrypto.OP_KIND[dec];
                            if (kind == CbhCrypto.OP_PUSH) {
                                if (depth < rosterStack.length) {
                                    rosterStack[depth].copyFrom(roster);
                                    nodeStack[depth] = curNode;
                                    depth++;
                                }
                                continue;
                            }
                            if (kind == CbhCrypto.OP_POP) {
                                if (depth == 0) {
                                    break;
                                }
                                depth--;
                                roster.copyFrom(rosterStack[depth]);
                                curNode = nodeStack[depth];
                                hasSeenPop = true;
                                continue;
                            }
                            if (kind == CbhCrypto.OP_SKIP) {
                                continue;
                            }

                            int fromSq = -1;
                            int toSq = -1;
                            int scidPromo = EMPTY;
                            byte mf = 0;
                            if (kind == CbhCrypto.OP_ESCAPE) {
                                if (posInBlock + 2 > cbgData.length) {
                                    break;
                                }
                                int x1 = cbgData[posInBlock++] & 0xFF;
                                int x2 = cbgData[posInBlock++] & 0xFF;
                                int d1 = (x1 - mc) & 0xFF;
                                int d2 = (x2 - mc) & 0xFF;
                                mc = (mc + 1) & 0xFF;
                                int b1 = CbhCrypto.SBOX_INV[d1];
                                int b2 = CbhCrypto.SBOX_INV[d2];
                                if (b1 < 0 || b2 < 0) {
                                    break;
                                }
                                int promoIdx = (b1 >>> 4) & 0x03;
                                int tsqCm = ((b1 & 0x0F) << 2) | ((b2 >>> 6) & 0x03);
                                int fsqCm = b2 & 0x3F;
                                fromSq = ((fsqCm & 7) << 3) | (fsqCm >> 3);
                                toSq = ((tsqCm & 7) << 3) | (tsqCm >> 3);
                                int promoConst = CbhCrypto.promoCodeToConstant(promoIdx);
                                scidPromo = CbhChessDatabase.toScidPromo(promoConst);
                                roster.applyMove(fromSq, toSq, promoConst);
                            } else {
                                mc = (mc + 1) & 0xFF;
                                if (kind == CbhCrypto.OP_NULL) {
                                    mf = 0x04;
                                    roster.toMove = 1 - roster.toMove;
                                } else if (kind == CbhCrypto.OP_CASTLE_K) {
                                    int baseRank;
                                    if (roster.toMove == 0) {
                                        baseRank = 0;
                                    } else {
                                        baseRank = 56;
                                    }
                                    fromSq = baseRank + 4;
                                    toSq = baseRank + 6;
                                    mf = 0x01;
                                    roster.applyCastle(true);
                                } else if (kind == CbhCrypto.OP_CASTLE_Q) {
                                    int baseRank;
                                    if (roster.toMove == 0) {
                                        baseRank = 0;
                                    } else {
                                        baseRank = 56;
                                    }
                                    fromSq = baseRank + 4;
                                    toSq = baseRank + 2;
                                    mf = 0x02;
                                    roster.applyCastle(false);
                                } else if (kind == CbhCrypto.OP_MOVE) {
                                    int pt = CbhCrypto.OP_PIECE_TYPE[dec];
                                    int slot = CbhCrypto.OP_SLOT[dec];
                                    int dx = CbhCrypto.OP_DX[dec];
                                    int dy = CbhCrypto.OP_DY[dec];
                                    fromSq = roster.getPieceSquare(pt, slot);
                                    if (fromSq < 0) {
                                        break;
                                    }
                                    int fx = fromSq & 7;
                                    int fy = fromSq >> 3;
                                    int tx;
                                    int ty;
                                    if (roster.toMove == 1 && pt == CbhCrypto.ROSTER_PAWN) {
                                        tx = (fx - dx) & 7;
                                        ty = (fy - dy) & 7;
                                    } else {
                                        tx = (fx + dx) & 7;
                                        ty = (fy + dy) & 7;
                                    }
                                    toSq = (ty << 3) | tx;
                                    roster.applyMove(fromSq, toSq, CONSTANTS.EMPTY);
                                } else {
                                    break;
                                }
                            }

                            int child = arena.addChild(curNode, fromSq, toSq, scidPromo, mf);
                            curNode = child;
                            moveOnlySeq++;
                            int cbaKey;
                            if (hasSeenPop) {
                                cbaKey = moveOnlySeq - 1;
                            } else {
                                cbaKey = moveOnlySeq;
                            }
                            if (cbaKey > 0) {
                                arena.mapCbaSeq(cbaKey, child);
                            }
                        }
                    }

                    byte[] cbaBody = batchCbaPayloads[i];
                    if (cbaBody != null) {
                        applyCbaToArena(cbaBody, arena);
                    }

                    int wid = resolveCbhPlayer(batchWhiteId[i], namebase, playerMap, nameWriter);
                    int bid = resolveCbhPlayer(batchBlackId[i], namebase, playerMap, nameWriter);
                    int eid = resolveCbhEvent(batchTourId[i], namebase, eventMap, nameWriter);
                    int sid = resolveCbhSite(batchTourId[i], namebase, siteMap, nameWriter);
                    int rid = resolveRound(batchRound[i], batchSubround[i], roundSimpleMap, roundSubMap, nameWriter);

                    String annoStr = "";
                    int aid = batchAnnoId[i];
                    if (aid >= 0 && aid < annotators.length) {
                        if (annotators[aid] == null) {
                            annotators[aid] = namebase.getAnnotator(aid);
                        }
                        annoStr = annotators[aid];
                    }
                    String srcStr = "";
                    int srcId = batchSrcId[i];
                    if (srcId >= 0 && srcId < sources.length) {
                        if (sources[srcId] == null) {
                            sources[srcId] = namebase.getSource(srcId);
                        }
                        srcStr = sources[srcId];
                    }

                    emitter.writeGame(
                            arena,
                            customStart,
                            customFen,
                            wid,
                            bid,
                            eid,
                            sid,
                            rid,
                            batchWElo[i],
                            batchBElo[i],
                            CbCommon.toScidPackedDate(batchDate[i]),
                            CbCommon.toScidResultCode(batchRes[i]),
                            CbCommon.toScidEcoCode(batchEco[i]),
                            annoStr,
                            srcStr,
                            sg5Out,
                            si5Out
                    );
                    convertedCount++;
                }

                if (listener != null && totalRecords > 0) {
                    int pct = (int) (((long) rawIdx * 100L) / totalRecords);
                    listener.onProgress(Math.min(99, pct));
                }
            }

            sn5Out.flush();
            sg5Out.flush();
            si5Out.flush();
        }
        return convertedCount;
    }

    private static void applyCbaToArena(byte[] data, Arena arena) {
        int p = 0;
        int len = data.length;
        while (p + 6 <= len) {
            int mvIdx = CbCommon.readU24BE(data, p);
            if (mvIdx == 0xFFFFFF) {
                mvIdx = 0;
            }
            int chunkType = data[p + 3] & 0xFF;
            int chunkLen = CbCommon.readU16BE(data, p + 4);
            if (chunkLen < 6 || p + chunkLen > len) {
                break;
            }
            if (mvIdx < arena.cbaSeqToArena.length) {
                int nodeIdx = arena.cbaSeqToArena[mvIdx];
                if (nodeIdx >= 0) {
                    if (chunkType == 0x02 || chunkType == 0x82) {
                        if (chunkLen >= 8) {
                            int lang = data[p + 7] & 0xFF;
                            String txt = CbCommon.decodeCommentText(data, p + 8, chunkLen - 8, true);
                            if (!txt.isEmpty()) {
                                int prio = CbCommon.cbaLanguagePriority(lang);
                                if (chunkType == 0x82) {
                                    arena.setPreText(nodeIdx, txt, prio);
                                } else {
                                    arena.setPostText(nodeIdx, txt, prio);
                                }
                            }
                        }
                    } else if (chunkType == 0x03) {
                        int payLen = chunkLen - 6;
                        if (payLen >= 1) {
                            int n1 = CbCommon.mapMoveNag(data[p + 6] & 0xFF);
                            if (n1 > 0) {
                                arena.moveNag[nodeIdx] = (byte) n1;
                            }
                            if (payLen >= 2) {
                                int n2 = CbCommon.mapEvalNag(data[p + 7] & 0xFF);
                                if (n2 > 0) {
                                    arena.evalNag[nodeIdx] = (byte) n2;
                                }
                            }
                            if (payLen >= 3) {
                                int n3 = CbCommon.mapPrefixNag(data[p + 8] & 0xFF);
                                if (n3 > 0) {
                                    arena.prefixNag[nodeIdx] = (byte) n3;
                                }
                            }
                        }
                    }
                }
            }
            p += chunkLen;
        }
    }

    private static int convertTwoCbh(
            Path sourcePath,
            Path si5Path,
            Path sn5Path,
            Path sg5Path,
            ProgressListener listener) throws IOException {

        Path base = TwoCbhChessDatabase.resolveBasePath(sourcePath.toString());
        Path twoCbh = TwoCbhChessDatabase.resolveExistingSibling(base, ".2cbh");
        if (!Files.exists(twoCbh)) {
            throw new FileNotFoundException("2CBH index file not found: " + twoCbh);
        }
        Path twoCbg = TwoCbhChessDatabase.resolveExistingSibling(base, ".2cbg");
        Path twoCba = TwoCbhChessDatabase.resolveExistingSibling(base, ".2cba");
        Path twoLid = TwoCbhChessDatabase.resolveExistingSibling(base, ".2lid");

        TwoCbhNamebase namebase = new TwoCbhNamebase();
        namebase.open(twoLid);

        int playerCap = (int) Math.min(namebase.getEntryCount(0) + 1L, 4_000_000L);
        int tourCap = (int) Math.min(namebase.getEntryCount(1) + 1L, 4_000_000L);
        int[] playerMap = new int[Math.max(16, playerCap)];
        Arrays.fill(playerMap, -1);
        int[] eventMap = new int[Math.max(16, tourCap)];
        Arrays.fill(eventMap, -1);
        int[] siteMap = new int[Math.max(16, tourCap)];
        Arrays.fill(siteMap, -1);

        int[] roundSimpleMap = new int[256];
        Arrays.fill(roundSimpleMap, -1);
        HashMap<Integer, Integer> roundSubMap = new HashMap<>(64);
        HashMap<Integer, String> annoCache = new HashMap<>(256);
        HashMap<Integer, String> srcCache = new HashMap<>(128);

        long[] batchCbgOffset = new long[BATCH_GAMES];
        long[] batchCbaOffset = new long[BATCH_GAMES];
        int[] batchWhiteId = new int[BATCH_GAMES];
        int[] batchBlackId = new int[BATCH_GAMES];
        int[] batchTourId = new int[BATCH_GAMES];
        int[] batchAnnoId = new int[BATCH_GAMES];
        int[] batchSrcId = new int[BATCH_GAMES];
        int[] batchDate = new int[BATCH_GAMES];
        int[] batchRes = new int[BATCH_GAMES];
        int[] batchRound = new int[BATCH_GAMES];
        int[] batchSubround = new int[BATCH_GAMES];
        int[] batchWElo = new int[BATCH_GAMES];
        int[] batchBElo = new int[BATCH_GAMES];
        int[] batchEco = new int[BATCH_GAMES];

        byte[][] batchCbgPayloads = new byte[BATCH_GAMES][];
        byte[][] batchCbaPayloads = new byte[BATCH_GAMES][];
        Integer[] sortOrder = new Integer[BATCH_GAMES];

        byte[] readBuf = new byte[BATCH_GAMES * TwoCbhChessDatabase.TWO_CBH_RECORD_SIZE];
        byte[] hdr24 = new byte[24];
        String[] fenOut = new String[1];
        int[] nodeStack = new int[64];

        Arena arena = new Arena();
        Scid5RecordEmitter emitter = new Scid5RecordEmitter();
        int convertedCount = 0;

        try (FileChannel cbhCh = FileChannel.open(twoCbh, StandardOpenOption.READ);
             FileChannel cbgCh = openOptionalReadChannel(twoCbg);
             FileChannel cbaCh = openOptionalReadChannel(twoCba);
             BufferedOutputStream si5Out = new BufferedOutputStream(
                     Files.newOutputStream(si5Path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING), 262144);
             BufferedOutputStream sn5Out = new BufferedOutputStream(
                     Files.newOutputStream(sn5Path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING), 262144);
             BufferedOutputStream sg5Out = new BufferedOutputStream(
                     Files.newOutputStream(sg5Path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING), 262144)) {

            FastNamebaseWriter nameWriter = new FastNamebaseWriter(sn5Out);
            long fileSize = cbhCh.size();
            if (fileSize <= TwoCbhChessDatabase.TWO_CBH_HEADER_SIZE) {
                return 0;
            }
            long cbgSize = 0L;
            if (cbgCh != null) {
                cbgSize = cbgCh.size();
            }
            long cbaSize = 0L;
            if (cbaCh != null) {
                cbaSize = cbaCh.size();
            }

            int totalRecords = (int) ((fileSize - TwoCbhChessDatabase.TWO_CBH_HEADER_SIZE)
                    / TwoCbhChessDatabase.TWO_CBH_RECORD_SIZE);
            long pos = TwoCbhChessDatabase.TWO_CBH_HEADER_SIZE;
            int rawIdx = 0;

            while (rawIdx < totalRecords) {
                if (listener != null && listener.isCancelled()) {
                    return convertedCount;
                }
                int recWant = Math.min(BATCH_GAMES, totalRecords - rawIdx);
                int bytesRead = TwoCbhChessDatabase.readUpToAt(
                        cbhCh, pos, readBuf, recWant * TwoCbhChessDatabase.TWO_CBH_RECORD_SIZE);
                int recsRead = bytesRead / TwoCbhChessDatabase.TWO_CBH_RECORD_SIZE;
                if (recsRead <= 0) {
                    break;
                }
                pos += (long) recsRead * TwoCbhChessDatabase.TWO_CBH_RECORD_SIZE;
                rawIdx += recsRead;

                int batchCount = 0;
                for (int i = 0; i < recsRead; i++) {
                    int off = i * TwoCbhChessDatabase.TWO_CBH_RECORD_SIZE;
                    long fl = CbCommon.readI64LE(readBuf, off);
                    if ((fl & 0x01L) != 0L && (fl & 0x02L) == 0L && (fl & 0x80L) == 0L) {
                        batchCbgOffset[batchCount] = CbCommon.readI64LE(readBuf, off + 0x08);
                        batchCbaOffset[batchCount] = CbCommon.readI64LE(readBuf, off + 0x10);
                        batchWhiteId[batchCount] = TwoCbhChessDatabase.clampIntId(CbCommon.readI64LE(readBuf, off + 0x18));
                        batchBlackId[batchCount] = TwoCbhChessDatabase.clampIntId(CbCommon.readI64LE(readBuf, off + 0x20));
                        batchTourId[batchCount] = TwoCbhChessDatabase.clampIntId(CbCommon.readI64LE(readBuf, off + 0x28));
                        batchAnnoId[batchCount] = TwoCbhChessDatabase.clampIntId(CbCommon.readI64LE(readBuf, off + 0x30));
                        batchSrcId[batchCount] = TwoCbhChessDatabase.clampIntId(CbCommon.readI64LE(readBuf, off + 0x38));
                        batchRes[batchCount] = readBuf[off + 0x58] & 0xFF;
                        batchRound[batchCount] = readBuf[off + 0x5A] & 0xFF;
                        batchSubround[batchCount] = readBuf[off + 0x5C] & 0xFF;
                        batchWElo[batchCount] = CbCommon.readU16LE(readBuf, off + 0x60);
                        batchBElo[batchCount] = CbCommon.readU16LE(readBuf, off + 0x70);
                        batchEco[batchCount] = CbCommon.readU16LE(readBuf, off + 0x80);
                        batchDate[batchCount] = CbCommon.readI32LE(readBuf, off + 0xBC);
                        sortOrder[batchCount] = batchCount;
                        batchCbgPayloads[batchCount] = null;
                        batchCbaPayloads[batchCount] = null;
                        batchCount++;
                    }
                }

                if (batchCount == 0) {
                    continue;
                }

                // Read 2CBG blocks in ascending file offset order
                if (cbgCh != null) {
                    Arrays.sort(sortOrder, 0, batchCount, (a, b) -> Long.compare(batchCbgOffset[a], batchCbgOffset[b]));
                    for (int k = 0; k < batchCount; k++) {
                        int idx = sortOrder[k];
                        long off = batchCbgOffset[idx];
                        if (off >= 0L && off + 24L <= cbgSize) {
                            if (TwoCbhChessDatabase.readUpToAt(cbgCh, off, hdr24, 24) == 24) {
                                long magic = CbCommon.readI64LE(hdr24, 0);
                                int pLen = CbCommon.readI32LE(hdr24, 8);
                                if (magic == TwoCbhChessDatabase.TWO_CBG_MAGIC && pLen >= 4 && pLen <= 16_777_216) {
                                    int actualLen = (int) Math.min((long) pLen, cbgSize - (off + 24L));
                                    if (actualLen >= 4) {
                                        byte[] payload = new byte[actualLen];
                                        TwoCbhChessDatabase.readUpToAt(cbgCh, off + 24L, payload, actualLen);
                                        batchCbgPayloads[idx] = payload;
                                    }
                                }
                            }
                        }
                    }
                }

                // Read 2CBA blocks in ascending file offset order
                if (cbaCh != null) {
                    Arrays.sort(sortOrder, 0, batchCount, (a, b) -> Long.compare(batchCbaOffset[a], batchCbaOffset[b]));
                    for (int k = 0; k < batchCount; k++) {
                        int idx = sortOrder[k];
                        long off = batchCbaOffset[idx];
                        if (off > 12L && off + 24L <= cbaSize) {
                            if (TwoCbhChessDatabase.readUpToAt(cbaCh, off, hdr24, 24) == 24) {
                                long magic = CbCommon.readI64LE(hdr24, 0);
                                int pLen = CbCommon.readI32LE(hdr24, 8);
                                if (magic == TwoCbhChessDatabase.TWO_CBG_MAGIC && pLen >= 4 && pLen <= 16_777_216) {
                                    int actualLen = (int) Math.min((long) pLen, cbaSize - (off + 24L));
                                    if (actualLen >= 4) {
                                        byte[] payload = new byte[actualLen];
                                        TwoCbhChessDatabase.readUpToAt(cbaCh, off + 24L, payload, actualLen);
                                        batchCbaPayloads[idx] = payload;
                                    }
                                }
                            }
                        }
                    }
                }

                // Encode batch sequentially
                for (int i = 0; i < batchCount; i++) {
                    if (listener != null && listener.isCancelled()) {
                        return convertedCount;
                    }

                    arena.reset();
                    boolean customStart = false;
                    String customFen = null;
                    byte[] payload = batchCbgPayloads[i];
                    if (payload != null && payload.length >= 4) {
                        int wordCount = payload.length / 2;
                        int w0 = CbCommon.readU16LE(payload, 0);
                        int w1 = CbCommon.readU16LE(payload, 2);
                        int wIdx = wordCount;
                        if (w0 == 0x0001 && w1 == 0xFFFC) {
                            wIdx = 2;
                        } else if (w0 == 0x0001 && w1 == 0xFFFB) {
                            fenOut[0] = null;
                            wIdx = TwoCbhMoveTable.decodeSetup(payload, payload.length, fenOut);
                            if (fenOut[0] != null) {
                                customStart = true;
                                customFen = fenOut[0];
                            }
                        }

                        int curNode = 0;
                        int prevNode = -1;
                        int depth = 0;
                        while (wIdx < wordCount) {
                            int w = CbCommon.readU16LE(payload, wIdx * 2);
                            wIdx++;
                            if (w == 0xFFFD) {
                                if (prevNode != -1 && depth < nodeStack.length) {
                                    nodeStack[depth++] = prevNode;
                                }
                                continue;
                            }
                            if (w == 0xFFFF) {
                                if (depth == 0) {
                                    break;
                                }
                                curNode = nodeStack[--depth];
                                prevNode = arena.parent[curNode];
                                continue;
                            }
                            if (w == 0xFFFA) {
                                prevNode = curNode;
                                curNode = arena.addChild(curNode, -1, -1, EMPTY, (byte) 0x04);
                            } else if (w >= 1 && w <= TwoCbhMoveTable.TABLE_SIZE) {
                                int packed = TwoCbhMoveTable.MOVES[w];
                                int fromSq = TwoCbhMoveTable.fromSq64(packed);
                                int toSq = TwoCbhMoveTable.toSq64(packed);
                                int promo = CbhChessDatabase.toScidPromo(TwoCbhMoveTable.promoConst(packed));
                                byte mf = 0;
                                if (w == 45353 || w == 45355) {
                                    mf = 0x02;
                                } else if (w == 45354 || w == 45356) {
                                    mf = 0x01;
                                }
                                prevNode = curNode;
                                curNode = arena.addChild(curNode, fromSq, toSq, promo, mf);
                            }
                        }
                    }

                    byte[] cbaPayload = batchCbaPayloads[i];
                    if (cbaPayload != null && cbaPayload.length >= 4) {
                        arena.assignCRevMainlineFirst();
                        applyTwoCbaToArena(cbaPayload, arena);
                    }

                    int wid = resolveTwoCbhPlayer(batchWhiteId[i], namebase, playerMap, nameWriter);
                    int bid = resolveTwoCbhPlayer(batchBlackId[i], namebase, playerMap, nameWriter);
                    int eid = resolveTwoCbhEvent(batchTourId[i], namebase, eventMap, nameWriter);
                    int sid = resolveTwoCbhSite(batchTourId[i], namebase, siteMap, nameWriter);
                    int rid = resolveRound(batchRound[i], batchSubround[i], roundSimpleMap, roundSubMap, nameWriter);

                    String annoStr = "";
                    int aid = batchAnnoId[i];
                    if (aid >= 0) {
                        annoStr = annoCache.get(aid);
                        if (annoStr == null) {
                            annoStr = namebase.getAnnotator(aid);
                            if (annoCache.size() < 32768) {
                                annoCache.put(aid, annoStr);
                            }
                        }
                    }
                    String srcStr = "";
                    int srcId = batchSrcId[i];
                    if (srcId >= 0) {
                        srcStr = srcCache.get(srcId);
                        if (srcStr == null) {
                            srcStr = namebase.getSource(srcId);
                            if (srcCache.size() < 4096) {
                                srcCache.put(srcId, srcStr);
                            }
                        }
                    }

                    emitter.writeGame(
                            arena,
                            customStart,
                            customFen,
                            wid,
                            bid,
                            eid,
                            sid,
                            rid,
                            batchWElo[i],
                            batchBElo[i],
                            CbCommon.toScidPackedDate(batchDate[i]),
                            CbCommon.toScidResultCode(batchRes[i]),
                            CbCommon.toScidEcoCode(batchEco[i]),
                            annoStr,
                            srcStr,
                            sg5Out,
                            si5Out
                    );
                    convertedCount++;
                }

                if (listener != null && totalRecords > 0) {
                    int pct = (int) (((long) rawIdx * 100L) / totalRecords);
                    listener.onProgress(Math.min(99, pct));
                }
            }

            sn5Out.flush();
            sg5Out.flush();
            si5Out.flush();
        } finally {
            namebase.close();
        }
        return convertedCount;
    }

    private static void applyTwoCbaToArena(byte[] data, Arena arena) {
        if (CbCommon.readU16LE(data, 0) != 0x2000) {
            return;
        }
        int limit = data.length;
        int q = 2;
        while (q + 2 <= limit) {
            int marker = CbCommon.readU16LE(data, q);
            if (marker == 0xFFFF) {
                break;
            }
            if (q + 8 > limit) {
                break;
            }
            int mvIdx = CbCommon.readI32LE(data, q);
            int itemCount = CbCommon.readI32LE(data, q + 4);
            q += 8;
            if (mvIdx < 0 || itemCount < 0 || itemCount > 100000) {
                break;
            }
            int nodeIdx = -1;
            if (mvIdx < arena.cRevCount) {
                nodeIdx = arena.cRevToArena[mvIdx];
            }
            boolean bail = false;
            for (int itIdx = 0; itIdx < itemCount; itIdx++) {
                if (q + 2 > limit) {
                    bail = true;
                    break;
                }
                int itemType = CbCommon.readU16LE(data, q);
                q += 2;
                if (itemType == 0x0002 || itemType == 0x0082) {
                    if (q + 8 > limit) {
                        bail = true;
                        break;
                    }
                    int lang = CbCommon.readU16LE(data, q + 2);
                    int slen = CbCommon.readI32LE(data, q + 4);
                    q += 8;
                    if (slen < 0 || q + slen > limit) {
                        bail = true;
                        break;
                    }
                    if (slen > 0 && nodeIdx >= 0) {
                        String text = CbCommon.decodeCommentText(data, q, slen, false);
                        if (!text.isEmpty()) {
                            int prio = CbCommon.twoCbaLanguagePriority(lang);
                            if (itemType == 0x0082) {
                                arena.setPreText(nodeIdx, text, prio);
                            } else {
                                arena.setPostText(nodeIdx, text, prio);
                            }
                        }
                    }
                    q += slen;
                } else if (itemType == 0x0003) {
                    if (q + 3 > limit) {
                        bail = true;
                        break;
                    }
                    if (nodeIdx >= 0) {
                        int n1 = CbCommon.mapMoveNag(data[q] & 0xFF);
                        int n2 = CbCommon.mapEvalNag(data[q + 1] & 0xFF);
                        int n3 = CbCommon.mapPrefixNag(data[q + 2] & 0xFF);
                        if (n1 > 0) {
                            arena.moveNag[nodeIdx] = (byte) n1;
                        }
                        if (n2 > 0) {
                            arena.evalNag[nodeIdx] = (byte) n2;
                        }
                        if (n3 > 0) {
                            arena.prefixNag[nodeIdx] = (byte) n3;
                        }
                    }
                    q += 3;
                } else if (itemType == 0x0004 || itemType == 0x0005) {
                    if (q + 4 > limit) {
                        bail = true;
                        break;
                    }
                    int byteLen = CbCommon.readI32LE(data, q);
                    q += 4;
                    if (byteLen < 0 || q + byteLen > limit) {
                        bail = true;
                        break;
                    }
                    q += byteLen;
                } else if (itemType == 0x0014 || itemType == 0x0018) {
                    q += 1;
                } else if (itemType == 0x0007 || itemType == 0x0022 || itemType == 0x0023) {
                    q += 4;
                } else if (itemType == 0x0021) {
                    q += 6;
                } else if (itemType == 0x0009) {
                    q += 12;
                } else if (itemType == 0x0013) {
                    int nextPos = TwoCbhChessDatabase.skipRefGameItem(data, q, limit);
                    if (nextPos < 0) {
                        bail = true;
                        break;
                    }
                    q = nextPos;
                } else {
                    bail = true;
                    break;
                }
                if (q > limit) {
                    bail = true;
                    break;
                }
            }
            if (bail) {
                break;
            }
        }
    }

    private static FileChannel openOptionalReadChannel(Path p) throws IOException {
        if (p != null && Files.exists(p)) {
            return FileChannel.open(p, StandardOpenOption.READ);
        }
        return null;
    }

    private static int resolveCbhPlayer(
            int id, CbhNamebase nb, int[] cache, FastNamebaseWriter writer) throws IOException {
        if (id >= 0 && id < cache.length) {
            int mapped = cache[id];
            if (mapped >= 0) {
                return mapped;
            }
            mapped = writer.getOrAddPlayer(nb.getPlayer(id));
            cache[id] = mapped;
            return mapped;
        }
        return writer.getOrAddPlayer("");
    }

    private static int resolveCbhEvent(
            int id, CbhNamebase nb, int[] cache, FastNamebaseWriter writer) throws IOException {
        if (id >= 0 && id < cache.length) {
            int mapped = cache[id];
            if (mapped >= 0) {
                return mapped;
            }
            mapped = writer.getOrAddEvent(nb.getEvent(id));
            cache[id] = mapped;
            return mapped;
        }
        return writer.getOrAddEvent("");
    }

    private static int resolveCbhSite(
            int id, CbhNamebase nb, int[] cache, FastNamebaseWriter writer) throws IOException {
        if (id >= 0 && id < cache.length) {
            int mapped = cache[id];
            if (mapped >= 0) {
                return mapped;
            }
            mapped = writer.getOrAddSite(nb.getSite(id));
            cache[id] = mapped;
            return mapped;
        }
        return writer.getOrAddSite("");
    }

    private static int resolveTwoCbhPlayer(
            int id, TwoCbhNamebase nb, int[] cache, FastNamebaseWriter writer) throws IOException {
        if (id >= 0 && id < cache.length) {
            int mapped = cache[id];
            if (mapped >= 0) {
                return mapped;
            }
            mapped = writer.getOrAddPlayer(nb.readPlayerUncached(id));
            cache[id] = mapped;
            return mapped;
        }
        if (id >= 0) {
            return writer.getOrAddPlayer(nb.getPlayer(id));
        }
        return writer.getOrAddPlayer("");
    }

    private static int resolveTwoCbhEvent(
            int id, TwoCbhNamebase nb, int[] cache, FastNamebaseWriter writer) throws IOException {
        if (id >= 0 && id < cache.length) {
            int mapped = cache[id];
            if (mapped >= 0) {
                return mapped;
            }
            mapped = writer.getOrAddEvent(nb.getEvent(id));
            cache[id] = mapped;
            return mapped;
        }
        if (id >= 0) {
            return writer.getOrAddEvent(nb.getEvent(id));
        }
        return writer.getOrAddEvent("");
    }

    private static int resolveTwoCbhSite(
            int id, TwoCbhNamebase nb, int[] cache, FastNamebaseWriter writer) throws IOException {
        if (id >= 0 && id < cache.length) {
            int mapped = cache[id];
            if (mapped >= 0) {
                return mapped;
            }
            mapped = writer.getOrAddSite(nb.getSite(id));
            cache[id] = mapped;
            return mapped;
        }
        if (id >= 0) {
            return writer.getOrAddSite(nb.getSite(id));
        }
        return writer.getOrAddSite("");
    }

    private static int resolveRound(
            int round,
            int subround,
            int[] simpleMap,
            HashMap<Integer, Integer> subMap,
            FastNamebaseWriter writer) throws IOException {
        if (subround == 0 && round >= 0 && round < simpleMap.length) {
            int mapped = simpleMap[round];
            if (mapped >= 0) {
                return mapped;
            }
            mapped = writer.getOrAddRound(CbCommon.formatRound(round, 0));
            simpleMap[round] = mapped;
            return mapped;
        }
        int key = (round << 8) | (subround & 0xFF);
        Integer existing = subMap.get(key);
        if (existing != null) {
            return existing;
        }
        int mapped = writer.getOrAddRound(CbCommon.formatRound(round, subround));
        subMap.put(key, mapped);
        return mapped;
    }
}
