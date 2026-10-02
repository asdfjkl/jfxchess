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

import java.util.Arrays;
import java.util.UUID;

public class Scid5GameInfo extends GameInfo {

    // SCID5 Index (.si5) attributes
    private int gameNumber = -1;
    private long sg5Offset = 0;
    private int sg5Length = 0;

    private int whiteId = 0;
    private int blackId = 0;
    private int eventId = 0;
    private int siteId = 0;
    private int roundId = 0;
    private int variant = 0; // 0 = standard chess, 1 = chess960

    private int whiteEloVal = 0;
    private int blackEloVal = 0;
    private int whiteRatingType = 0;
    private int blackRatingType = 0;

    private int gameDatePacked = 0;
    private int eventDatePacked = 0;
    private int halfMoves = 0;
    private int flags = 0;

    private static final String[] ECO_BASE_CACHE = new String[500];
    private static final String[] ELO_CACHE = new String[3500];

    static {
        for (int i = 0; i < 500; i++) {
            char letter = (char) ('A' + (i / 100));
            int num = i % 100;
            ECO_BASE_CACHE[i] = "" + letter + (num < 10 ? "0" : "") + num;
        }
        for (int i = 0; i < ELO_CACHE.length; i++) {
            ELO_CACHE[i] = String.valueOf(i);
        }
    }

    private static String cachedElo(int elo) {
        if (elo >= 0 && elo < ELO_CACHE.length) {
            return ELO_CACHE[elo];
        }
        return String.valueOf(elo);
    }

    private int storedLineCode = 0;
    private int finalMatSig = 0;
    private int homePawnCount = 0;
    private byte[] homePawnData = null;
    private int ecoCode = 0;

    private int commentRating = 0;
    private int variationRating = 0;
    private int nagRating = 0;

    // In-memory buffer support for active GUI sessions
    private boolean modifiedFlag = false;
    private Game modifiedGame = null;

    public Scid5GameInfo() {
        super();
    }

    public Scid5GameInfo(int gameNumber) {
        super(new UUID(0, gameNumber));
        this.gameNumber = gameNumber;
    }

    public Scid5GameInfo(UUID id) {
        super(id);
    }

    // Flag bitfield accessors (22 bits)
    public static final int FLAG_CUSTOM_START = 1;      // bit 0
    public static final int FLAG_PROMO = 2;             // bit 1
    public static final int FLAG_UNDER_PROMO = 4;       // bit 2
    public static final int FLAG_DELETE = 8;            // bit 3

    public boolean hasCustomStart() {
        return (flags & FLAG_CUSTOM_START) != 0;
    }

    public void setCustomStart(boolean customStart) {
        if (customStart) flags |= FLAG_CUSTOM_START;
        else flags &= ~FLAG_CUSTOM_START;
    }

    public boolean hasPromotion() {
        return (flags & FLAG_PROMO) != 0;
    }

    public void setPromotion(boolean promo) {
        if (promo) flags |= FLAG_PROMO;
        else flags &= ~FLAG_PROMO;
    }

    public boolean hasUnderPromotion() {
        return (flags & FLAG_UNDER_PROMO) != 0;
    }

    public void setUnderPromotion(boolean underPromo) {
        if (underPromo) flags |= FLAG_UNDER_PROMO;
        else flags &= ~FLAG_UNDER_PROMO;
    }

    public boolean isDeleted() {
        return (flags & FLAG_DELETE) != 0;
    }

    public void setDeleted(boolean deleted) {
        if (deleted) flags |= FLAG_DELETE;
        else flags &= ~FLAG_DELETE;
    }

    // Date packing / unpacking helpers
    public static String decodeDate(int packedDate) {
        if (packedDate == 0) {
            return "????.??.??";
        }
        int year = (packedDate >>> 9) & 0x7FF;
        int month = (packedDate >>> 5) & 0x0F;
        int day = packedDate & 0x1F;

        char[] b = new char[10];
        if (year > 0) {
            b[0] = (char) ('0' + (year / 1000) % 10);
            b[1] = (char) ('0' + (year / 100) % 10);
            b[2] = (char) ('0' + (year / 10) % 10);
            b[3] = (char) ('0' + year % 10);
        } else {
            b[0] = '?';
            b[1] = '?';
            b[2] = '?';
            b[3] = '?';
        }
        b[4] = '.';
        if (month > 0 && month <= 12) {
            b[5] = (char) ('0' + (month / 10));
            b[6] = (char) ('0' + (month % 10));
        } else {
            b[5] = '?';
            b[6] = '?';
        }
        b[7] = '.';
        if (day > 0 && day <= 31) {
            b[8] = (char) ('0' + (day / 10));
            b[9] = (char) ('0' + (day % 10));
        } else {
            b[8] = '?';
            b[9] = '?';
        }
        return new String(b);
    }

    public static int encodeDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank() || dateStr.contains("?")) {
            if (dateStr != null && dateStr.length() >= 4) {
                try {
                    int y = Integer.parseInt(dateStr.substring(0, 4));
                    return (y & 0x7FF) << 9;
                } catch (NumberFormatException ignored) {}
            }
            return 0;
        }
        String[] parts = dateStr.replace('-', '.').split("\\.");
        int year = 0;
        int month = 0;
        int day = 0;
        try {
            if (parts.length > 0 && !parts[0].isBlank()) year = Integer.parseInt(parts[0]);
            if (parts.length > 1 && !parts[1].isBlank()) month = Integer.parseInt(parts[1]);
            if (parts.length > 2 && !parts[2].isBlank()) day = Integer.parseInt(parts[2]);
        } catch (NumberFormatException ignored) {}

        return ((year & 0x7FF) << 9) | ((month & 0x0F) << 5) | (day & 0x1F);
    }

    // ECO packing / unpacking helpers
    public static String decodeEco(int code) {
        if (code == 0) return "";
        int c = code - 1;
        int base = c / 131;
        if (base > 499) return ""; // 500 ECO codes: A00 to E99

        int sub = c % 131;
        if (sub == 0) {
            return ECO_BASE_CACHE[base];
        }

        char letter = (char) ('A' + (base / 100));
        int num = base % 100;

        StringBuilder sb = new StringBuilder(5);
        sb.append(letter);
        if (num < 10) sb.append('0');
        sb.append(num);

        if (sub <= 26) {
            sb.append((char) ('a' + (sub - 1)));
        } else {
            sb.append(sub - 26);
        }
        return sb.toString();
    }

    public static int encodeEco(String eco) {
        if (eco == null || eco.length() < 3) return 0;
        char letter = Character.toUpperCase(eco.charAt(0));
        if (letter < 'A' || letter > 'E') return 0;

        try {
            int num = Integer.parseInt(eco.substring(1, 3));
            int base = (letter - 'A') * 100 + num;
            int sub = 0;
            if (eco.length() > 3) {
                char ch = eco.charAt(3);
                if (ch >= 'a' && ch <= 'z') sub = (ch - 'a') + 1;
                else if (ch >= 'A' && ch <= 'Z') sub = (ch - 'A') + 1;
                else if (ch >= '1' && ch <= '4') sub = (ch - '0') + 26;
            }
            return (base * 131 + sub) + 1;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // Result encoding / decoding
    public static String decodeResult(int resCode) {
        return switch (resCode) {
            case 1 -> "1-0";
            case 2 -> "0-1";
            case 3 -> "1/2-1/2";
            default -> "*";
        };
    }

    public static int encodeResult(String result) {
        if (result == null) return 0;
        String r = result.trim();
        if ("1-0".equals(r)) return 1;
        if ("0-1".equals(r)) return 2;
        if ("1/2-1/2".equals(r)) return 3;
        return 0;
    }

    // Getters and Setters
    public int getGameNumber() {
        return gameNumber;
    }

    public void setGameNumber(int gameNumber) {
        this.gameNumber = gameNumber;
    }

    public long getSg5Offset() {
        return sg5Offset;
    }

    public void setSg5Offset(long sg5Offset) {
        this.sg5Offset = sg5Offset;
    }

    public int getSg5Length() {
        return sg5Length;
    }

    public void setSg5Length(int sg5Length) {
        this.sg5Length = sg5Length;
    }

    public int getWhiteId() {
        return whiteId;
    }

    public void setWhiteId(int whiteId) {
        this.whiteId = whiteId;
    }

    public int getBlackId() {
        return blackId;
    }

    public void setBlackId(int blackId) {
        this.blackId = blackId;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public int getSiteId() {
        return siteId;
    }

    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }

    public int getRoundId() {
        return roundId;
    }

    public void setRoundId(int roundId) {
        this.roundId = roundId;
    }

    public int getVariant() {
        return variant;
    }

    public void setVariant(int variant) {
        this.variant = variant;
    }

    public int getWhiteEloVal() {
        return whiteEloVal;
    }

    public void setWhiteEloVal(int whiteEloVal) {
        this.whiteEloVal = whiteEloVal;
        if (whiteEloVal > 0) {
            super.setWhiteElo(cachedElo(whiteEloVal));
        }
    }

    public int getBlackEloVal() {
        return blackEloVal;
    }

    public void setBlackEloVal(int blackEloVal) {
        this.blackEloVal = blackEloVal;
        if (blackEloVal > 0) {
            super.setBlackElo(cachedElo(blackEloVal));
        }
    }

    public int getWhiteRatingType() {
        return whiteRatingType;
    }

    public void setWhiteRatingType(int whiteRatingType) {
        this.whiteRatingType = whiteRatingType;
    }

    public int getBlackRatingType() {
        return blackRatingType;
    }

    public void setBlackRatingType(int blackRatingType) {
        this.blackRatingType = blackRatingType;
    }

    public int getGameDatePacked() {
        return gameDatePacked;
    }

    public void setGameDatePacked(int gameDatePacked) {
        this.gameDatePacked = gameDatePacked;
        super.setDate(decodeDate(gameDatePacked));
    }

    public int getEventDatePacked() {
        return eventDatePacked;
    }

    public void setEventDatePacked(int eventDatePacked) {
        this.eventDatePacked = eventDatePacked;
    }

    public int getHalfMoves() {
        return halfMoves;
    }

    public void setHalfMoves(int halfMoves) {
        this.halfMoves = halfMoves;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public int getStoredLineCode() {
        return storedLineCode;
    }

    public void setStoredLineCode(int storedLineCode) {
        this.storedLineCode = storedLineCode;
    }

    public int getFinalMatSig() {
        return finalMatSig;
    }

    public void setFinalMatSig(int finalMatSig) {
        this.finalMatSig = finalMatSig;
    }

    public int getHomePawnCount() {
        return homePawnCount;
    }

    public void setHomePawnCount(int homePawnCount) {
        this.homePawnCount = homePawnCount;
    }

    public byte[] getHomePawnData() {
        return (homePawnData != null) ? homePawnData : new byte[8];
    }

    public void setHomePawnData(byte[] homePawnData) {
        this.homePawnData = homePawnData;
    }

    public int getEcoCode() {
        return ecoCode;
    }

    public void setEcoCode(int ecoCode) {
        this.ecoCode = ecoCode;
        super.setEco(decodeEco(ecoCode));
    }

    public int getCommentRating() {
        return commentRating;
    }

    public void setCommentRating(int commentRating) {
        this.commentRating = commentRating;
    }

    public int getVariationRating() {
        return variationRating;
    }

    public void setVariationRating(int variationRating) {
        this.variationRating = variationRating;
    }

    public int getNagRating() {
        return nagRating;
    }

    public void setNagRating(int nagRating) {
        this.nagRating = nagRating;
    }

    public boolean isModified() {
        return modifiedFlag;
    }

    public void setModified(boolean modified) {
        this.modifiedFlag = modified;
    }

    public Game getModifiedGame() {
        return modifiedGame;
    }

    public void setModifiedGame(Game modifiedGame) {
        this.modifiedGame = modifiedGame;
    }
}
