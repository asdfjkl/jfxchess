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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Scid5Namebase {

    public static final int NAME_PLAYER = 0;
    public static final int NAME_EVENT = 1;
    public static final int NAME_SITE = 2;
    public static final int NAME_ROUND = 3;
    public static final int NAME_INFO = 4;

    private final ArrayList<String> players = new ArrayList<>();
    private final ArrayList<String> events = new ArrayList<>();
    private final ArrayList<String> sites = new ArrayList<>();
    private final ArrayList<String> rounds = new ArrayList<>();
    private final Map<String, String> dbInfo = new LinkedHashMap<>();

    private final Map<String, Integer> playerLookup = new HashMap<>();
    private final Map<String, Integer> eventLookup = new HashMap<>();
    private final Map<String, Integer> siteLookup = new HashMap<>();
    private final Map<String, Integer> roundLookup = new HashMap<>();

    public void clear() {
        players.clear();
        events.clear();
        sites.clear();
        rounds.clear();
        dbInfo.clear();

        playerLookup.clear();
        eventLookup.clear();
        siteLookup.clear();
        roundLookup.clear();
    }

    public void read(Path sn5Path) throws IOException {
        clear();
        if (!Files.exists(sn5Path)) {
            return;
        }
        byte[] bytes = Files.readAllBytes(sn5Path);
        int pos = 0;
        while (pos < bytes.length) {
            long v = 0;
            int shift = 0;
            while (pos < bytes.length) {
                int b = bytes[pos++] & 0xFF;
                v |= ((long) (b & 0x7F)) << shift;
                if ((b & 0x80) == 0) break;
                shift += 7;
            }
            int type = (int) (v & 0x07);
            int len = (int) (v >>> 3);
            if (pos + len > bytes.length) {
                break;
            }
            String str = new String(bytes, pos, len, StandardCharsets.UTF_8);
            pos += len;

            switch (type) {
                case NAME_PLAYER -> {
                    playerLookup.putIfAbsent(str, players.size());
                    players.add(str);
                }
                case NAME_EVENT -> {
                    eventLookup.putIfAbsent(str, events.size());
                    events.add(str);
                }
                case NAME_SITE -> {
                    siteLookup.putIfAbsent(str, sites.size());
                    sites.add(str);
                }
                case NAME_ROUND -> {
                    roundLookup.putIfAbsent(str, rounds.size());
                    rounds.add(str);
                }
                case NAME_INFO -> parseDbInfo(str);
            }
        }
    }

    private void parseDbInfo(String str) {
        String[] knownKeys = {"type", "description", "autoload", "flag1", "flag2", "flag3", "flag4", "flag5", "flag6"};
        for (String k : knownKeys) {
            if (str.startsWith(k)) {
                dbInfo.put(k, str.substring(k.length()));
                return;
            }
        }
        dbInfo.put(str, "");
    }

    public String getPlayer(int id) {
        if (id >= 0 && id < players.size()) {
            return players.get(id);
        }
        return (id == 0) ? "" : "?" + id;
    }

    public String getEvent(int id) {
        if (id >= 0 && id < events.size()) {
            return events.get(id);
        }
        return (id == 0) ? "" : "?" + id;
    }

    public String getSite(int id) {
        if (id >= 0 && id < sites.size()) {
            return sites.get(id);
        }
        return (id == 0) ? "" : "?" + id;
    }

    public String getRound(int id) {
        if (id >= 0 && id < rounds.size()) {
            return rounds.get(id);
        }
        return (id == 0) ? "" : "?" + id;
    }

    public String getInfo(String key) {
        return dbInfo.getOrDefault(key, "");
    }

    public int getPlayerCount() {
        return players.size();
    }

    public int getEventCount() {
        return events.size();
    }

    public int getSiteCount() {
        return sites.size();
    }

    public int getRoundCount() {
        return rounds.size();
    }

    public List<String> getPlayers() {
        return List.copyOf(players);
    }

    public List<String> getEvents() {
        return List.copyOf(events);
    }

    public List<String> getSites() {
        return List.copyOf(sites);
    }

    public List<String> getRounds() {
        return List.copyOf(rounds);
    }

    public Map<String, String> getDbInfo() {
        return Map.copyOf(dbInfo);
    }

    public Integer findExistingPlayer(String name) {
        return playerLookup.get(name);
    }

    public Integer findExistingEvent(String event) {
        return eventLookup.get(event);
    }

    public Integer findExistingSite(String site) {
        return siteLookup.get(site);
    }

    public Integer findExistingRound(String round) {
        return roundLookup.get(round);
    }

    public synchronized int findOrAdd(Path sn5Path, String name, int type) throws IOException {
        String cleanName = (name != null) ? name : "";
        Integer existing = switch (type) {
            case NAME_PLAYER -> playerLookup.get(cleanName);
            case NAME_EVENT -> eventLookup.get(cleanName);
            case NAME_SITE -> siteLookup.get(cleanName);
            case NAME_ROUND -> roundLookup.get(cleanName);
            default -> null;
        };
        if (existing != null) {
            return existing;
        }

        int id = switch (type) {
            case NAME_PLAYER -> players.size();
            case NAME_EVENT -> events.size();
            case NAME_SITE -> sites.size();
            case NAME_ROUND -> rounds.size();
            default -> 0;
        };

        byte[] strBytes = cleanName.getBytes(StandardCharsets.UTF_8);
        long v = (((long) strBytes.length) << 3) | (type & 0x07);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        long temp = v;
        while ((temp & ~0x7FL) != 0) {
            baos.write((int) ((temp & 0x7F) | 0x80));
            temp >>>= 7;
        }
        baos.write((int) (temp & 0x7F));
        baos.write(strBytes);
        byte[] recordBytes = baos.toByteArray();

        if (sn5Path != null) {
            Files.write(sn5Path, recordBytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }

        switch (type) {
            case NAME_PLAYER -> {
                playerLookup.put(cleanName, id);
                players.add(cleanName);
            }
            case NAME_EVENT -> {
                eventLookup.put(cleanName, id);
                events.add(cleanName);
            }
            case NAME_SITE -> {
                siteLookup.put(cleanName, id);
                sites.add(cleanName);
            }
            case NAME_ROUND -> {
                roundLookup.put(cleanName, id);
                rounds.add(cleanName);
            }
        }

        return id;
    }

    public synchronized int findOrAdd(String name, int type) {
        try {
            return findOrAdd(null, name, type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
