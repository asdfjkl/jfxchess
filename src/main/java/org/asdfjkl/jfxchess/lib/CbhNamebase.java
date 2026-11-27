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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Compact raw-slab reader and deduplicating writer for classic ChessBase entity
 * tables (`.cbp`, `.cbt`, `.cbc`, `.cbs`). Handles both 28-byte (Fritz 6/7/8)
 * and 32-byte (Fritz 12 / Mega2024) headers dynamically.
 */
final class CbhNamebase {

    private static final int DELETED_SENTINEL = -999;
    static final int NO_ENTITY_ID = 0xFFFFFF;

    private static final int DEFAULT_HEADER_SIZE = 32;
    private static final int PLAYER_REC_SIZE = 67;
    private static final int TOURNAMENT_REC_SIZE = 99;
    private static final int ANNOTATOR_REC_SIZE = 62;
    private static final int SOURCE_REC_SIZE = 68;

    private static final class EntitySlab {
        Path path;
        byte[] data;
        int byteLen;
        int hdrSize;
        int recSize;
        int count;
        ByteArrayOutputStream pendingBytes;

        void clear() {
            path = null;
            data = null;
            byteLen = 0;
            hdrSize = 0;
            recSize = 0;
            count = 0;
            pendingBytes = null;
        }

        void ensureInMemoryCapacity(int needed) {
            if (data == null) {
                data = new byte[Math.max(4096, needed)];
            } else if (needed > data.length) {
                int newCap = Math.max(data.length * 2, needed);
                data = Arrays.copyOf(data, newCap);
            }
        }
    }

    private final EntitySlab players = new EntitySlab();
    private final EntitySlab tournaments = new EntitySlab();
    private final EntitySlab annotators = new EntitySlab();
    private final EntitySlab sources = new EntitySlab();

    private final Map<String, Integer> playerLookup = new HashMap<>();
    private final Map<String, Integer> tournamentLookup = new HashMap<>();
    private final Map<String, Integer> annotatorLookup = new HashMap<>();
    private final Map<String, Integer> sourceLookup = new HashMap<>();
    private boolean lookupsPopulated = false;
    private boolean batchMode = false;

    void clear() {
        players.clear();
        tournaments.clear();
        annotators.clear();
        sources.clear();
        playerLookup.clear();
        tournamentLookup.clear();
        annotatorLookup.clear();
        sourceLookup.clear();
        lookupsPopulated = false;
        batchMode = false;
    }

    void load(Path basePath) throws IOException {
        clear();
        loadSlab(resolveSibling(basePath, ".cbp"), players, PLAYER_REC_SIZE);
        loadSlab(resolveSibling(basePath, ".cbt"), tournaments, TOURNAMENT_REC_SIZE);
        loadSlab(resolveSibling(basePath, ".cbc"), annotators, ANNOTATOR_REC_SIZE);
        loadSlab(resolveSibling(basePath, ".cbs"), sources, SOURCE_REC_SIZE);
    }

    void createEmpty(Path basePath) throws IOException {
        clear();
        initEmptyFile(Path.of(basePath.toString() + ".cbp"), players, PLAYER_REC_SIZE);
        initEmptyFile(Path.of(basePath.toString() + ".cbt"), tournaments, TOURNAMENT_REC_SIZE);
        initEmptyFile(Path.of(basePath.toString() + ".cbc"), annotators, ANNOTATOR_REC_SIZE);
        initEmptyFile(Path.of(basePath.toString() + ".cbs"), sources, SOURCE_REC_SIZE);
    }

    private static Path resolveSibling(Path basePath, String ext) {
        Path direct = Path.of(basePath.toString() + ext);
        if (Files.exists(direct)) {
            return direct;
        }
        Path upper = Path.of(basePath.toString() + ext.toUpperCase(Locale.ROOT));
        if (Files.exists(upper)) {
            return upper;
        }
        return direct;
    }

    private static void initEmptyFile(Path path, EntitySlab slab, int recSize) throws IOException {
        byte[] hdr = buildEmptyHeader(DEFAULT_HEADER_SIZE, recSize);
        Files.write(path, hdr, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        slab.path = path;
        slab.data = Arrays.copyOf(hdr, Math.max(4096, hdr.length));
        slab.byteLen = hdr.length;
        slab.hdrSize = DEFAULT_HEADER_SIZE;
        slab.recSize = recSize;
        slab.count = 0;
    }

    private static byte[] buildEmptyHeader(int hdrSize, int recSize) {
        byte[] hdr = new byte[hdrSize];
        CbCommon.writeI32LE(hdr, 0, 0);
        CbCommon.writeI32LE(hdr, 4, -1);
        CbCommon.writeI32LE(hdr, 8, -1);
        CbCommon.writeI32LE(hdr, 12, recSize - 9);
        if (hdrSize >= 32) {
            CbCommon.writeI32LE(hdr, 24, 4);
        }
        return hdr;
    }

    private static void loadSlab(Path path, EntitySlab slab, int expectedRecSize) throws IOException {
        slab.path = path;
        slab.recSize = expectedRecSize;
        slab.hdrSize = DEFAULT_HEADER_SIZE;
        if (!Files.exists(path)) {
            return;
        }
        long fileSize = Files.size(path);
        if (fileSize < 28L) {
            return;
        }
        byte[] bytes = Files.readAllBytes(path);
        int totalRecords = CbCommon.readI32LE(bytes, 0);
        int payloadLen = CbCommon.readI32LE(bytes, 12);
        int recSize = 9 + payloadLen;
        if (recSize <= 9 || totalRecords < 0 || (long) totalRecords * recSize > bytes.length) {
            recSize = expectedRecSize;
            long body32 = bytes.length - 32L;
            long body28 = bytes.length - 28L;
            if (body32 >= 0 && body32 % recSize == 0) {
                slab.hdrSize = 32;
                slab.recSize = recSize;
                slab.count = (int) (body32 / recSize);
                slab.data = bytes;
                slab.byteLen = bytes.length;
                return;
            } else if (body28 >= 0 && body28 % recSize == 0) {
                slab.hdrSize = 28;
                slab.recSize = recSize;
                slab.count = (int) (body28 / recSize);
                slab.data = bytes;
                slab.byteLen = bytes.length;
                return;
            }
            return;
        }
        long hdrSizeLong = (long) bytes.length - ((long) totalRecords * recSize);
        if (hdrSizeLong < 0 || hdrSizeLong > bytes.length) {
            return;
        }
        slab.data = bytes;
        slab.byteLen = bytes.length;
        slab.hdrSize = (int) hdrSizeLong;
        slab.recSize = recSize;
        slab.count = totalRecords;
    }

    synchronized void beginBatch() {
        batchMode = true;
    }

    synchronized void flushBatch() throws IOException {
        batchMode = false;
        flushSlab(players);
        flushSlab(tournaments);
        flushSlab(annotators);
        flushSlab(sources);
    }

    private static void flushSlab(EntitySlab slab) throws IOException {
        if (slab.pendingBytes == null || slab.pendingBytes.size() == 0) {
            return;
        }
        byte[] pending = slab.pendingBytes.toByteArray();
        slab.pendingBytes.reset();
        persistAppendedBytes(slab, pending);
    }

    private void ensureLookups() {
        if (lookupsPopulated) {
            return;
        }
        for (int i = 0; i < players.count; i++) {
            if (!isDeleted(players, i)) {
                playerLookup.putIfAbsent(getPlayer(i), i);
            }
        }
        for (int i = 0; i < tournaments.count; i++) {
            if (!isDeleted(tournaments, i)) {
                tournamentLookup.putIfAbsent(getEvent(i) + "\u0000" + getSite(i), i);
            }
        }
        for (int i = 0; i < annotators.count; i++) {
            if (!isDeleted(annotators, i)) {
                annotatorLookup.putIfAbsent(getAnnotator(i), i);
            }
        }
        for (int i = 0; i < sources.count; i++) {
            if (!isDeleted(sources, i)) {
                sourceLookup.putIfAbsent(getSource(i), i);
            }
        }
        lookupsPopulated = true;
    }

    private static boolean isDeleted(EntitySlab slab, int id) {
        if (slab.data == null || id < 0 || id >= slab.count) {
            return true;
        }
        int off = slab.hdrSize + id * slab.recSize;
        if (off + 4 > slab.byteLen) {
            return true;
        }
        return CbCommon.readI32LE(slab.data, off) == DELETED_SENTINEL;
    }

    synchronized int findOrAddPlayer(String fullName) throws IOException {
        ensureLookups();
        String[] parts = CbCommon.splitPlayerName(fullName);
        byte[] rec = new byte[players.recSize > 0 ? players.recSize : PLAYER_REC_SIZE];
        initRecordPrefix(rec);
        CbCommon.writeFixedWin1252(rec, 9, 30, parts[0]);
        CbCommon.writeFixedWin1252(rec, 39, 20, parts[1]);
        String normLast = CbCommon.decodeFixedWin1252(rec, 9, 30);
        String normFirst = CbCommon.decodeFixedWin1252(rec, 39, 20);
        String key = CbCommon.formatPlayerName(normLast, normFirst);

        Integer existing = playerLookup.get(key);
        if (existing != null) {
            return existing;
        }
        int id = appendRecord(players, PLAYER_REC_SIZE, rec);
        playerLookup.put(key, id);
        return id;
    }

    synchronized int findOrAddTournament(String event, String site) throws IOException {
        ensureLookups();
        byte[] rec = new byte[tournaments.recSize > 0 ? tournaments.recSize : TOURNAMENT_REC_SIZE];
        initRecordPrefix(rec);
        CbCommon.writeFixedWin1252(rec, 9, 40, event);
        CbCommon.writeFixedWin1252(rec, 49, 30, site);
        String normEvent = CbCommon.decodeFixedWin1252(rec, 9, 40);
        String normSite = CbCommon.decodeFixedWin1252(rec, 49, 30);
        String key = normEvent + "\u0000" + normSite;

        Integer existing = tournamentLookup.get(key);
        if (existing != null) {
            return existing;
        }
        int id = appendRecord(tournaments, TOURNAMENT_REC_SIZE, rec);
        tournamentLookup.put(key, id);
        return id;
    }

    synchronized int findOrAddAnnotator(String annotator) throws IOException {
        if (annotator == null || annotator.trim().isEmpty()) {
            return NO_ENTITY_ID;
        }
        ensureLookups();
        byte[] rec = new byte[annotators.recSize > 0 ? annotators.recSize : ANNOTATOR_REC_SIZE];
        initRecordPrefix(rec);
        CbCommon.writeFixedWin1252(rec, 9, 45, annotator.trim());
        String key = CbCommon.decodeFixedWin1252(rec, 9, 45);
        if (key.isEmpty()) {
            return NO_ENTITY_ID;
        }

        Integer existing = annotatorLookup.get(key);
        if (existing != null) {
            return existing;
        }
        int id = appendRecord(annotators, ANNOTATOR_REC_SIZE, rec);
        annotatorLookup.put(key, id);
        return id;
    }

    synchronized int findOrAddSource(String source) throws IOException {
        if (source == null || source.trim().isEmpty()) {
            return NO_ENTITY_ID;
        }
        ensureLookups();
        byte[] rec = new byte[sources.recSize > 0 ? sources.recSize : SOURCE_REC_SIZE];
        initRecordPrefix(rec);
        CbCommon.writeFixedWin1252(rec, 9, 25, source.trim());
        String key = CbCommon.decodeFixedWin1252(rec, 9, 25);
        if (key.isEmpty()) {
            return NO_ENTITY_ID;
        }

        Integer existing = sourceLookup.get(key);
        if (existing != null) {
            return existing;
        }
        int id = appendRecord(sources, SOURCE_REC_SIZE, rec);
        sourceLookup.put(key, id);
        return id;
    }

    private static void initRecordPrefix(byte[] rec) {
        CbCommon.writeI32LE(rec, 0, -1);
        CbCommon.writeI32LE(rec, 4, -1);
        rec[8] = 0;
    }

    private int appendRecord(EntitySlab slab, int defaultRecSize, byte[] rec) throws IOException {
        if (slab.data == null || slab.byteLen < 28) {
            if (slab.path == null) {
                throw new IOException("Entity slab path not initialized");
            }
            initEmptyFile(slab.path, slab, defaultRecSize);
        }
        if (rec.length != slab.recSize) {
            byte[] resized = new byte[slab.recSize];
            System.arraycopy(rec, 0, resized, 0, Math.min(rec.length, slab.recSize));
            rec = resized;
        }
        int id = slab.count;
        int writeOff = slab.hdrSize + id * slab.recSize;
        slab.ensureInMemoryCapacity(writeOff + slab.recSize);
        System.arraycopy(rec, 0, slab.data, writeOff, slab.recSize);
        slab.byteLen = writeOff + slab.recSize;
        slab.count = id + 1;
        CbCommon.writeI32LE(slab.data, 0, slab.count);

        if (batchMode) {
            if (slab.pendingBytes == null) {
                slab.pendingBytes = new ByteArrayOutputStream(4096);
            }
            slab.pendingBytes.writeBytes(rec);
        } else {
            persistAppendedBytes(slab, rec);
        }
        return id;
    }

    private static void persistAppendedBytes(EntitySlab slab, byte[] appendedBytes) throws IOException {
        if (slab.path == null) {
            return;
        }
        try (FileChannel ch = FileChannel.open(slab.path,
                StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            long endPos = ch.size();
            writeAllAt(ch, endPos, appendedBytes);
            byte[] countBytes = new byte[4];
            CbCommon.writeI32LE(countBytes, 0, slab.count);
            writeAllAt(ch, 0L, countBytes);
        }
    }

    private static void writeAllAt(FileChannel ch, long offset, byte[] src) throws IOException {
        ByteBuffer bb = ByteBuffer.wrap(src);
        long pos = offset;
        while (bb.hasRemaining()) {
            int n = ch.write(bb, pos);
            pos += n;
        }
    }

    int getPlayerCount() {
        return players.count;
    }

    int getTournamentCount() {
        return tournaments.count;
    }

    int getAnnotatorCount() {
        return annotators.count;
    }

    int getSourceCount() {
        return sources.count;
    }

    String getPlayer(int id) {
        if (players.data == null || id < 0 || id >= players.count || players.recSize < 59) {
            return "";
        }
        int off = players.hdrSize + id * players.recSize;
        if (off + 59 > players.byteLen) {
            return "";
        }
        if (CbCommon.readI32LE(players.data, off) == DELETED_SENTINEL) {
            return "";
        }
        String last = CbCommon.decodeFixedWin1252(players.data, off + 9, 30);
        String first = CbCommon.decodeFixedWin1252(players.data, off + 39, 20);
        return CbCommon.formatPlayerName(last, first);
    }

    String getEvent(int id) {
        if (tournaments.data == null || id < 0 || id >= tournaments.count || tournaments.recSize < 49) {
            return "";
        }
        int off = tournaments.hdrSize + id * tournaments.recSize;
        if (off + 49 > tournaments.byteLen) {
            return "";
        }
        if (CbCommon.readI32LE(tournaments.data, off) == DELETED_SENTINEL) {
            return "";
        }
        return CbCommon.decodeFixedWin1252(tournaments.data, off + 9, 40);
    }

    String getSite(int id) {
        if (tournaments.data == null || id < 0 || id >= tournaments.count || tournaments.recSize < 79) {
            return "";
        }
        int off = tournaments.hdrSize + id * tournaments.recSize;
        if (off + 79 > tournaments.byteLen) {
            return "";
        }
        if (CbCommon.readI32LE(tournaments.data, off) == DELETED_SENTINEL) {
            return "";
        }
        return CbCommon.decodeFixedWin1252(tournaments.data, off + 49, 30);
    }

    String getAnnotator(int id) {
        if (annotators.data == null || id < 0 || id >= annotators.count || annotators.recSize < 54) {
            return "";
        }
        int off = annotators.hdrSize + id * annotators.recSize;
        if (off + 54 > annotators.byteLen) {
            return "";
        }
        if (CbCommon.readI32LE(annotators.data, off) == DELETED_SENTINEL) {
            return "";
        }
        return CbCommon.decodeFixedWin1252(annotators.data, off + 9, 45);
    }

    String getSource(int id) {
        if (sources.data == null || id < 0 || id >= sources.count || sources.recSize < 34) {
            return "";
        }
        int off = sources.hdrSize + id * sources.recSize;
        if (off + 34 > sources.byteLen) {
            return "";
        }
        if (CbCommon.readI32LE(sources.data, off) == DELETED_SENTINEL) {
            return "";
        }
        return CbCommon.decodeFixedWin1252(sources.data, off + 9, 25);
    }

    boolean[] buildPlayerMatchMask(String needleLower) {
        boolean[] mask = new boolean[players.count];
        for (int i = 0; i < players.count; i++) {
            String name = getPlayer(i);
            if (!name.isEmpty() && name.toLowerCase(Locale.ROOT).contains(needleLower)) {
                mask[i] = true;
            }
        }
        return mask;
    }

    boolean[] buildEventMatchMask(String needleLower) {
        boolean[] mask = new boolean[tournaments.count];
        for (int i = 0; i < tournaments.count; i++) {
            String name = getEvent(i);
            if (!name.isEmpty() && name.toLowerCase(Locale.ROOT).contains(needleLower)) {
                mask[i] = true;
            }
        }
        return mask;
    }

    boolean[] buildSiteMatchMask(String needleLower) {
        boolean[] mask = new boolean[tournaments.count];
        for (int i = 0; i < tournaments.count; i++) {
            String name = getSite(i);
            if (!name.isEmpty() && name.toLowerCase(Locale.ROOT).contains(needleLower)) {
                mask[i] = true;
            }
        }
        return mask;
    }
}
