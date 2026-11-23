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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Channel-backed, bounds-clamped, LRU-cached reader for ChessBase `.2lid`
 * fixed-stride unified entity stores. Never loads the full `.2lid` file into RAM.
 */
final class TwoCbhNamebase implements AutoCloseable {

    private static final int PLAYER_CACHE_LIMIT = 65536;
    private static final int EVENT_CACHE_LIMIT = 32768;
    private static final int MISC_CACHE_LIMIT = 8192;

    private FileChannel channel;
    private long fileSize;
    private int headerSize;
    private int slotCount;
    private long bucketStride;

    private final int[] slotSizes = new int[8];
    private final long[] slotOffsets = new long[8];
    private final long[] entryCounts = new long[8];

    private final Map<Integer, String> playerCache = createLru(PLAYER_CACHE_LIMIT);
    private final Map<Integer, String> eventCache = createLru(EVENT_CACHE_LIMIT);
    private final Map<Integer, String> siteCache = createLru(EVENT_CACHE_LIMIT);
    private final Map<Integer, String> sourceCache = createLru(MISC_CACHE_LIMIT);
    private final Map<Integer, String> annotatorCache = createLru(MISC_CACHE_LIMIT);

    private static Map<Integer, String> createLru(int capacity) {
        return new LinkedHashMap<>(Math.min(capacity, 1024), 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, String> eldest) {
                return size() > capacity;
            }
        };
    }

    synchronized void open(Path lidPath) throws IOException {
        close();
        if (lidPath == null || !Files.exists(lidPath)) {
            return;
        }
        fileSize = Files.size(lidPath);
        if (fileSize < 28L) {
            return;
        }
        channel = FileChannel.open(lidPath, StandardOpenOption.READ);
        byte[] hdr = new byte[(int) Math.min(184L, fileSize)];
        readFullyAt(channel, 0L, hdr, 0, hdr.length);
        if (hdr.length < 28) {
            return;
        }
        headerSize = CbCommon.readI32BE(hdr, 0);
        slotCount = CbCommon.readI32BE(hdr, 4);
        if (headerSize < 28 || slotCount <= 0) {
            return;
        }
        int parseSlots = Math.min(slotCount, 8);
        long off = 0L;
        for (int i = 0; i < parseSlots; i++) {
            int descOff = 8 + i * 20;
            if (descOff + 20 > hdr.length) {
                break;
            }
            int size = CbCommon.readI32BE(hdr, descOff);
            long cnt = CbCommon.readI64BE(hdr, descOff + 4);
            slotSizes[i] = Math.max(0, size);
            slotOffsets[i] = off;
            entryCounts[i] = Math.max(0L, cnt);
            off += Math.max(0, size);
        }
        bucketStride = off;
    }

    @Override
    public synchronized void close() throws IOException {
        if (channel != null) {
            try {
                channel.close();
            } finally {
                channel = null;
            }
        }
        fileSize = 0L;
        headerSize = 0;
        slotCount = 0;
        bucketStride = 0L;
        playerCache.clear();
        eventCache.clear();
        siteCache.clear();
        sourceCache.clear();
        annotatorCache.clear();
    }

    long getEntryCount(int slotIdx) {
        if (slotIdx < 0 || slotIdx >= slotCount) {
            return 0L;
        }
        return entryCounts[slotIdx];
    }

    String getPlayer(int id) {
        if (id < 0 || channel == null || slotCount <= 0 || id >= entryCounts[0]) {
            return "";
        }
        synchronized (playerCache) {
            String cached = playerCache.get(id);
            if (cached != null) {
                return cached;
            }
        }
        String decoded = readPlayerUncached(id);
        synchronized (playerCache) {
            playerCache.put(id, decoded);
        }
        return decoded;
    }

    String readPlayerUncached(int id) {
        byte[] buf = readSlotRecord(0, id, 256);
        if (buf == null) {
            return "";
        }
        return decodePlayerFromSlot(buf, 0, buf.length);
    }

    static String decodePlayerFromSlot(byte[] buf, int off, int avail) {
        if (avail < 8) {
            return "";
        }
        int[] cursor = new int[]{off + 4};
        int end = off + avail;
        String last = readLenString(buf, cursor, end);
        String first = readLenString(buf, cursor, end);
        return CbCommon.formatPlayerName(last, first);
    }

    String getEvent(int id) {
        if (id < 0 || channel == null || slotCount <= 1 || id >= entryCounts[1]) {
            return "";
        }
        synchronized (eventCache) {
            String cached = eventCache.get(id);
            if (cached != null) {
                return cached;
            }
        }
        String decodedEvent;
        String decodedSite;
        byte[] buf = readSlotRecord(1, id, 256);
        if (buf == null) {
            decodedSite = "";
            decodedEvent = "";
        } else {
            String[] pair = decodeTournamentFromSlot(buf, 0, buf.length);
            decodedSite = pair[0];
            decodedEvent = pair[1];
        }
        synchronized (eventCache) {
            eventCache.put(id, decodedEvent);
        }
        synchronized (siteCache) {
            siteCache.put(id, decodedSite);
        }
        return decodedEvent;
    }

    String getSite(int id) {
        if (id < 0 || channel == null || slotCount <= 1 || id >= entryCounts[1]) {
            return "";
        }
        synchronized (siteCache) {
            String cached = siteCache.get(id);
            if (cached != null) {
                return cached;
            }
        }
        getEvent(id);
        synchronized (siteCache) {
            String cached = siteCache.get(id);
            if (cached != null) {
                return cached;
            }
        }
        return "";
    }

    /**
     * In `.2lid` Slot 1 (Tournaments), Place comes BEFORE Title.
     * Returns {@code new String[]{ place, title }}.
     */
    static String[] decodeTournamentFromSlot(byte[] buf, int off, int avail) {
        if (avail < 8) {
            return new String[]{"", ""};
        }
        int[] cursor = new int[]{off + 4};
        int end = off + avail;
        String place = readLenString(buf, cursor, end);
        String title = readLenString(buf, cursor, end);
        return new String[]{place, title};
    }

    String getSource(int id) {
        if (id < 0 || channel == null || slotCount <= 2 || id >= entryCounts[2]) {
            return "";
        }
        synchronized (sourceCache) {
            String cached = sourceCache.get(id);
            if (cached != null) {
                return cached;
            }
        }
        byte[] buf = readSlotRecord(2, id, 220);
        String title = "";
        if (buf != null && buf.length >= 8) {
            int[] cursor = new int[]{4};
            title = readLenString(buf, cursor, buf.length);
        }
        synchronized (sourceCache) {
            sourceCache.put(id, title);
        }
        return title;
    }

    String getAnnotator(int id) {
        if (id < 0 || channel == null) {
            return "";
        }
        // When Slot 3 (Annotators) has 0 entries, annotator_id indexes Slot 0 (Players).
        if (slotCount <= 3 || entryCounts[3] == 0) {
            return getPlayer(id);
        }
        if (id >= entryCounts[3]) {
            return "";
        }
        synchronized (annotatorCache) {
            String cached = annotatorCache.get(id);
            if (cached != null) {
                return cached;
            }
        }
        byte[] buf = readSlotRecord(3, id, 256);
        String name = "";
        if (buf != null && buf.length >= 8) {
            int[] cursor = new int[]{4};
            name = readLenString(buf, cursor, buf.length);
        }
        synchronized (annotatorCache) {
            annotatorCache.put(id, name);
        }
        return name;
    }

    private byte[] readSlotRecord(int slotIdx, int id, int preferredMaxBytes) {
        FileChannel ch = this.channel;
        if (ch == null || bucketStride <= 0 || slotIdx >= slotCount) {
            return null;
        }
        long recordStart = (long) headerSize + ((long) id * bucketStride) + slotOffsets[slotIdx];
        if (recordStart >= fileSize) {
            return null;
        }
        int maxAllowed = slotSizes[slotIdx];
        if (preferredMaxBytes > 0 && preferredMaxBytes < maxAllowed) {
            maxAllowed = preferredMaxBytes;
        }
        long avail = fileSize - recordStart;
        int toRead = (int) Math.min(maxAllowed, avail);
        if (toRead < 8) {
            return null;
        }
        byte[] buf = new byte[toRead];
        try {
            int actuallyRead = readUpToAt(ch, recordStart, buf, 0, toRead);
            if (actuallyRead < 8) {
                return null;
            }
            if (actuallyRead < buf.length) {
                byte[] trimmed = new byte[actuallyRead];
                System.arraycopy(buf, 0, trimmed, 0, actuallyRead);
                return trimmed;
            }
            return buf;
        } catch (IOException e) {
            return null;
        }
    }

    private static String readLenString(byte[] buf, int[] cursor, int end) {
        int pos = cursor[0];
        if (pos + 4 > end) {
            cursor[0] = end;
            return "";
        }
        int len = CbCommon.readI32LE(buf, pos);
        pos += 4;
        if (len <= 0) {
            cursor[0] = pos;
            return "";
        }
        int actual = Math.min(len, end - pos);
        String s = CbCommon.decodeTextAuto(buf, pos, actual);
        cursor[0] = pos + actual;
        return s.trim();
    }

    private static void readFullyAt(FileChannel ch, long offset, byte[] dst, int off, int len) throws IOException {
        ByteBuffer bb = ByteBuffer.wrap(dst, off, len);
        long pos = offset;
        while (bb.hasRemaining()) {
            int n = ch.read(bb, pos);
            if (n < 0) {
                break;
            }
            pos += n;
        }
    }

    private static int readUpToAt(FileChannel ch, long offset, byte[] dst, int off, int len) throws IOException {
        ByteBuffer bb = ByteBuffer.wrap(dst, off, len);
        long pos = offset;
        int total = 0;
        while (bb.hasRemaining()) {
            int n = ch.read(bb, pos);
            if (n < 0) {
                break;
            }
            pos += n;
            total += n;
        }
        return total;
    }

    boolean[] buildPlayerMatchMask(String needleLower) throws IOException {
        long countLong = getEntryCount(0);
        if (channel == null || bucketStride <= 0 || countLong <= 0) {
            return new boolean[0];
        }
        int count = (int) Math.min(countLong, Integer.MAX_VALUE);
        boolean[] mask = new boolean[count];
        int chunkRecords = 1024;
        long chunkSpan = chunkRecords * bucketStride;
        byte[] chunk = new byte[(int) Math.min(chunkSpan, fileSize)];
        for (int startId = 0; startId < count; startId += chunkRecords) {
            int batch = Math.min(chunkRecords, count - startId);
            long batchStart = (long) headerSize + ((long) startId * bucketStride);
            if (batchStart >= fileSize) {
                break;
            }
            int bytesToRead = (int) Math.min((long) batch * bucketStride, fileSize - batchStart);
            int read = readUpToAt(channel, batchStart, chunk, 0, bytesToRead);
            for (int i = 0; i < batch; i++) {
                long rel = (long) i * bucketStride + slotOffsets[0];
                if (rel + 8 > read) {
                    break;
                }
                int avail = (int) Math.min(slotSizes[0], read - rel);
                String name = decodePlayerFromSlot(chunk, (int) rel, avail);
                if (!name.isEmpty() && name.toLowerCase(Locale.ROOT).contains(needleLower)) {
                    mask[startId + i] = true;
                }
            }
        }
        return mask;
    }

    boolean[] buildTournamentMatchMask(String needleLower, boolean matchSiteInsteadOfEvent) throws IOException {
        long countLong = getEntryCount(1);
        if (channel == null || bucketStride <= 0 || countLong <= 0) {
            return new boolean[0];
        }
        int count = (int) Math.min(countLong, Integer.MAX_VALUE);
        boolean[] mask = new boolean[count];
        int chunkRecords = 1024;
        long chunkSpan = chunkRecords * bucketStride;
        byte[] chunk = new byte[(int) Math.min(chunkSpan, fileSize)];
        for (int startId = 0; startId < count; startId += chunkRecords) {
            int batch = Math.min(chunkRecords, count - startId);
            long batchStart = (long) headerSize + ((long) startId * bucketStride);
            if (batchStart >= fileSize) {
                break;
            }
            int bytesToRead = (int) Math.min((long) batch * bucketStride, fileSize - batchStart);
            int read = readUpToAt(channel, batchStart, chunk, 0, bytesToRead);
            for (int i = 0; i < batch; i++) {
                long rel = (long) i * bucketStride + slotOffsets[1];
                if (rel + 8 > read) {
                    break;
                }
                int avail = (int) Math.min(slotSizes[1], read - rel);
                String[] pair = decodeTournamentFromSlot(chunk, (int) rel, avail);
                String target;
                if (matchSiteInsteadOfEvent) {
                    target = pair[0];
                } else {
                    target = pair[1];
                }
                if (!target.isEmpty() && target.toLowerCase(Locale.ROOT).contains(needleLower)) {
                    mask[startId + i] = true;
                }
            }
        }
        return mask;
    }
}
