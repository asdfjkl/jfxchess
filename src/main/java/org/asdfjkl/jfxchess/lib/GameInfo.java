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

import java.util.Objects;
import java.util.UUID;

public class GameInfo {

    private final UUID id;
    private String event = "";
    private String site = "";
    private String date = "";
    private String round = "";
    private String white = "";
    private String black = "";
    private String result = "";
    private String eco = "";
    private String whiteElo = "";
    private String blackElo = "";
    private boolean foundAtLeast1Tag = false;

    public GameInfo() {
        this(UUID.randomUUID());
    }

    public GameInfo(UUID id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public UUID getId() {
        return id;
    }

    public static String extractSurname(String name) {
        if (name == null) {
            return "N.N.";
        }
        String s = name.strip();
        if (s.isEmpty() || "?".equals(s) || "N.N.".equalsIgnoreCase(s)) {
            return "N.N.";
        }
        if (s.contains(",")) {
            String beforeComma = s.substring(0, s.indexOf(',')).strip();
            if (!beforeComma.isEmpty()) {
                return beforeComma;
            }
        }
        if (s.toLowerCase().startsWith("stockfish")) {
            return s;
        }
        int lastSpace = s.lastIndexOf(' ');
        if (lastSpace > 0 && lastSpace < s.length() - 1) {
            return s.substring(lastSpace + 1).strip();
        }
        return s;
    }

    public static String formatVersusTitle(String white, String black) {
        return extractSurname(white) + " vs. " + extractSurname(black);
    }

    public String getWhiteSurname() {
        return extractSurname(this.white);
    }

    public String getBlackSurname() {
        return extractSurname(this.black);
    }

    public String getVersusTitle() {
        return formatVersusTitle(this.white, this.black);
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getRound() {
        return round;
    }

    public void setRound(String round) {
        this.round = round;
    }

    public String getWhite() {
        return white;
    }

    public void setWhite(String white) {
        this.white = white;
    }

    public String getBlack() {
        return black;
    }

    public void setBlack(String black) {
        this.black = black;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public void setWhiteElo(String valueEncoded) {
        this.whiteElo = valueEncoded;
    }

    public String getWhiteElo() {
        return whiteElo;
    }

    public void setBlackElo(String valueEncoded) {
        this.blackElo = valueEncoded;
    }

    public String getBlackElo() {
        return blackElo;
    }

    public void setEco(String eco) {
        this.eco = eco;
    }

    public String getEco() {
        return eco;
    }

    public void markValid() {
        foundAtLeast1Tag = true;
    }

    public boolean isValid() {
        return foundAtLeast1Tag;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || !(other instanceof GameInfo)) {
            return false;
        }
        GameInfo that = (GameInfo) other;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
