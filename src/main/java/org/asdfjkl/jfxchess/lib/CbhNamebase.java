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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Compact raw-slab reader for classic ChessBase entity tables
 * (`.cbp`, `.cbt`, `.cbc`, `.cbs`). Handles both 28-byte (Fritz 6/7/8)
 * and 32-byte (Fritz 12 / Mega2024) headers dynamically.
 */
final class CbhNamebase {

    private static final int DELETED_SENTINEL = -999;

    private static final class EntitySlab {
        byte[] data;
        int hdrSize;
        int recSize;
        int count;

        void clear() {
            data = null;
            hdrSize = 0;
            recSize = 0;
            count = 0;
        }
    }

    private final EntitySlab players = new EntitySlab();
    private final EntitySlab tournaments = new EntitySlab();
    private final EntitySlab annotators = new EntitySlab();
    private final EntitySlab sources = new EntitySlab();

    void clear() {
        players.clear();
        tournaments.clear();
        annotators.clear();
        sources.clear();
    }

    void load(Path basePath) throws IOException {
        clear();
        loadSlab(resolveSibling(basePath, ".cbp"), players, 67);
        loadSlab(resolveSibling(basePath, ".cbt"), tournaments, 99);
        loadSlab(resolveSibling(basePath, ".cbc"), annotators, 62);
        loadSlab(resolveSibling(basePath, ".cbs"), sources, 68);
    }

    private static Path resolveSibling(Path basePath, String ext) {
        Path direct = Path.of(basePath.toString() + ext);
        if (Files.exists(direct)) {
            return direct;
        }
        return Path.of(basePath.toString() + ext.toUpperCase(Locale.ROOT));
    }

    private static void loadSlab(Path path, EntitySlab slab, int expectedRecSize) throws IOException {
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
                return;
            } else if (body28 >= 0 && body28 % recSize == 0) {
                slab.hdrSize = 28;
                slab.recSize = recSize;
                slab.count = (int) (body28 / recSize);
                slab.data = bytes;
                return;
            }
            return;
        }
        long hdrSizeLong = (long) bytes.length - ((long) totalRecords * recSize);
        if (hdrSizeLong < 0 || hdrSizeLong > bytes.length) {
            return;
        }
        slab.data = bytes;
        slab.hdrSize = (int) hdrSizeLong;
        slab.recSize = recSize;
        slab.count = totalRecords;
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
        if (off + 59 > players.data.length) {
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
        if (off + 49 > tournaments.data.length) {
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
        if (off + 79 > tournaments.data.length) {
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
        if (off + 54 > annotators.data.length) {
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
        if (off + 34 > sources.data.length) {
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
