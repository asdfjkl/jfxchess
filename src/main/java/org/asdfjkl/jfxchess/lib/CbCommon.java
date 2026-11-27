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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Shared binary decoding, character encoding, metadata formatting, and figurine
 * normalization utilities for ChessBase CBH and 2CBH formats.
 */
final class CbCommon {

    static final Charset WIN1252 = Charset.forName("windows-1252");

    private static final String[] ECO_CACHE = new String[501];
    private static final String[] ROUND_CACHE = new String[128];

    static {
        ECO_CACHE[0] = "";
        for (int i = 1; i <= 500; i++) {
            int idx0 = i - 1;
            char letter = (char) ('A' + (idx0 / 100));
            int num = idx0 % 100;
            String s;
            if (num < 10) {
                s = letter + "0" + num;
            } else {
                s = letter + Integer.toString(num);
            }
            ECO_CACHE[i] = s;
        }
        ROUND_CACHE[0] = "";
        for (int i = 1; i < ROUND_CACHE.length; i++) {
            ROUND_CACHE[i] = Integer.toString(i);
        }
    }

    private CbCommon() {
    }

    static int readU16BE(byte[] b, int off) {
        return ((b[off] & 0xFF) << 8) | (b[off + 1] & 0xFF);
    }

    static int readU24BE(byte[] b, int off) {
        return ((b[off] & 0xFF) << 16)
                | ((b[off + 1] & 0xFF) << 8)
                | (b[off + 2] & 0xFF);
    }

    static int readI32BE(byte[] b, int off) {
        return ((b[off] & 0xFF) << 24)
                | ((b[off + 1] & 0xFF) << 16)
                | ((b[off + 2] & 0xFF) << 8)
                | (b[off + 3] & 0xFF);
    }

    static long readU32BE(byte[] b, int off) {
        return readI32BE(b, off) & 0xFFFFFFFFL;
    }

    static int readU16LE(byte[] b, int off) {
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8);
    }

    static int readI32LE(byte[] b, int off) {
        return (b[off] & 0xFF)
                | ((b[off + 1] & 0xFF) << 8)
                | ((b[off + 2] & 0xFF) << 16)
                | ((b[off + 3] & 0xFF) << 24);
    }

    static long readU32LE(byte[] b, int off) {
        return readI32LE(b, off) & 0xFFFFFFFFL;
    }

    static long readI64LE(byte[] b, int off) {
        long lo = readU32LE(b, off);
        long hi = readI32LE(b, off + 4);
        return (hi << 32) | lo;
    }

    static long readI64BE(byte[] b, int off) {
        long hi = readI32BE(b, off);
        long lo = readU32BE(b, off + 4);
        return (hi << 32) | lo;
    }

    static String decodeFixedWin1252(byte[] buf, int off, int maxLen) {
        if (off >= buf.length || maxLen <= 0) {
            return "";
        }
        int limit = Math.min(buf.length, off + maxLen);
        int end = off;
        while (end < limit && buf[end] != 0) {
            end++;
        }
        if (end == off) {
            return "";
        }
        while (end > off && buf[end - 1] == ' ') {
            end--;
        }
        if (end == off) {
            return "";
        }
        return new String(buf, off, end - off, WIN1252).trim();
    }

    static String decodeTextAuto(byte[] buf, int off, int len) {
        if (len <= 0 || off >= buf.length) {
            return "";
        }
        int end = Math.min(buf.length, off + len);
        while (end > off && buf[end - 1] == 0) {
            end--;
        }
        int actualLen = end - off;
        if (actualLen <= 0) {
            return "";
        }
        boolean allAscii = true;
        for (int i = off; i < end; i++) {
            if ((buf[i] & 0x80) != 0) {
                allAscii = false;
                break;
            }
        }
        if (allAscii) {
            return new String(buf, off, actualLen, StandardCharsets.US_ASCII);
        }
        if (isValidUtf8(buf, off, actualLen)) {
            return new String(buf, off, actualLen, StandardCharsets.UTF_8);
        }
        return new String(buf, off, actualLen, WIN1252);
    }

    private static boolean isValidUtf8(byte[] buf, int off, int len) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPORT);
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            CharBuffer unused = decoder.decode(ByteBuffer.wrap(buf, off, len));
            return unused != null;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    /**
     * Decodes a ChessBase comment payload and normalizes figurine glyphs into
     * ASCII SAN letters ({@code K, Q, N, B, R}, and {@code ""} for pawn).
     */
    static String decodeCommentText(byte[] buf, int off, int len, boolean preferWin1252) {
        if (len <= 0 || off >= buf.length) {
            return "";
        }
        int end = Math.min(buf.length, off + len);
        while (end > off && buf[end - 1] == 0) {
            end--;
        }
        int actualLen = end - off;
        if (actualLen <= 0) {
            return "";
        }

        boolean utf8Used = false;
        String raw;
        if (!preferWin1252 && isValidUtf8(buf, off, actualLen)) {
            raw = new String(buf, off, actualLen, StandardCharsets.UTF_8);
            utf8Used = true;
        } else if (preferWin1252 && containsUtf8Pua(buf, off, end) && isValidUtf8(buf, off, actualLen)) {
            raw = new String(buf, off, actualLen, StandardCharsets.UTF_8);
            utf8Used = true;
        } else {
            raw = new String(buf, off, actualLen, WIN1252);
        }

        return normalizeFigurines(raw, !utf8Used);
    }

    private static boolean containsUtf8Pua(byte[] buf, int off, int end) {
        for (int i = off; i + 2 < end; i++) {
            if ((buf[i] & 0xFF) == 0xEF && (buf[i + 1] & 0xFF) == 0x80) {
                int b2 = buf[i + 2] & 0xFF;
                if (b2 >= 0xA2 && b2 <= 0xA7) {
                    return true;
                }
            }
        }
        return false;
    }

    static String normalizeFigurines(String text, boolean replaceLegacyWin1252Symbols) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder sb = null;
        int n = text.length();
        for (int i = 0; i < n; i++) {
            char c = text.charAt(i);
            String rep = null;
            if (c == '\uE022') {
                rep = "K";
            } else if (c == '\uE023') {
                rep = "Q";
            } else if (c == '\uE024') {
                rep = "N";
            } else if (c == '\uE025') {
                rep = "B";
            } else if (c == '\uE026') {
                rep = "R";
            } else if (c == '\uE027') {
                rep = "";
            } else if (replaceLegacyWin1252Symbols) {
                if (c == '\u00A2') {
                    rep = "K";
                } else if (c == '\u00A3') {
                    rep = "Q";
                } else if (c == '\u00A4') {
                    rep = "N";
                } else if (c == '\u00A5') {
                    rep = "B";
                } else if (c == '\u00A6') {
                    rep = "R";
                } else if (c == '\u00A7') {
                    rep = "";
                } else if (c == '\u017E') {
                    // Win-1252 byte 0x9E is ChessBase diagram symbol
                    rep = "{#}";
                }
            }
            if (rep != null) {
                if (sb == null) {
                    sb = new StringBuilder(n);
                    sb.append(text, 0, i);
                }
                sb.append(rep);
            } else if (sb != null) {
                sb.append(c);
            }
        }
        if (sb == null) {
            return text;
        }
        return sb.toString();
    }

    static String formatPlayerName(String lastName, String firstName) {
        boolean hasLast = lastName != null && !lastName.isEmpty();
        boolean hasFirst = firstName != null && !firstName.isEmpty();
        if (hasLast && hasFirst) {
            return lastName + ", " + firstName;
        } else if (hasLast) {
            return lastName;
        } else if (hasFirst) {
            return firstName;
        }
        return "";
    }

    static String decodeEco(int rawEco) {
        int ecoIdx = rawEco >>> 7;
        if (ecoIdx <= 0 || ecoIdx > 500) {
            return "";
        }
        return ECO_CACHE[ecoIdx];
    }

    static int toScidEcoCode(int rawEco) {
        int ecoIdx = rawEco >>> 7;
        if (ecoIdx <= 0 || ecoIdx > 500) {
            return 0;
        }
        int base = ecoIdx - 1;
        return (base * 131) + 1;
    }

    static String decodeDate(int packedDate) {
        if (packedDate == 0) {
            return "????.??.??";
        }
        int year = packedDate >>> 9;
        int month = (packedDate >>> 5) & 0x0F;
        int day = packedDate & 0x1F;
        char[] c = new char[10];
        if (year > 0) {
            c[0] = (char) ('0' + ((year / 1000) % 10));
            c[1] = (char) ('0' + ((year / 100) % 10));
            c[2] = (char) ('0' + ((year / 10) % 10));
            c[3] = (char) ('0' + (year % 10));
        } else {
            c[0] = '?';
            c[1] = '?';
            c[2] = '?';
            c[3] = '?';
        }
        c[4] = '.';
        if (month >= 1 && month <= 12) {
            c[5] = (char) ('0' + (month / 10));
            c[6] = (char) ('0' + (month % 10));
        } else {
            c[5] = '?';
            c[6] = '?';
        }
        c[7] = '.';
        if (day >= 1 && day <= 31) {
            c[8] = (char) ('0' + (day / 10));
            c[9] = (char) ('0' + (day % 10));
        } else {
            c[8] = '?';
            c[9] = '?';
        }
        return new String(c);
    }

    static int toScidPackedDate(int cbPackedDate) {
        if (cbPackedDate <= 0) {
            return 0;
        }
        int year = (cbPackedDate >>> 9) & 0x7FF;
        int month = (cbPackedDate >>> 5) & 0x0F;
        int day = cbPackedDate & 0x1F;
        if (month > 12) {
            month = 0;
        }
        if (day > 31) {
            day = 0;
        }
        return (year << 9) | (month << 5) | day;
    }

    static int extractYear(int packedDate) {
        if (packedDate <= 0) {
            return -1;
        }
        int year = packedDate >>> 9;
        if (year <= 0) {
            return -1;
        }
        return year;
    }

    static String decodeResult(int resByte) {
        if (resByte == 0) {
            return "0-1";
        } else if (resByte == 1) {
            return "1/2-1/2";
        } else if (resByte == 2) {
            return "1-0";
        }
        return "*";
    }

    static int decodeResultConstant(int resByte) {
        if (resByte == 0) {
            return CONSTANTS.RES_BLACK_WINS;
        } else if (resByte == 1) {
            return CONSTANTS.RES_DRAW;
        } else if (resByte == 2) {
            return CONSTANTS.RES_WHITE_WINS;
        }
        return CONSTANTS.RES_UNDEF;
    }

    static int toScidResultCode(int resByte) {
        if (resByte == 0) {
            return 2;
        } else if (resByte == 1) {
            return 3;
        } else if (resByte == 2) {
            return 1;
        }
        return 0;
    }

    static String formatRound(int round, int subround) {
        if (round <= 0) {
            return "";
        }
        if (subround > 0) {
            return round + "." + subround;
        }
        if (round < ROUND_CACHE.length) {
            return ROUND_CACHE[round];
        }
        return Integer.toString(round);
    }

    static int cbaLanguagePriority(int lang) {
        if (lang == 0x2A) {
            return 3;
        } else if (lang == 0x00) {
            return 2;
        }
        return 1;
    }

    static int twoCbaLanguagePriority(int lang) {
        if (lang == 0) {
            return 3;
        } else if (lang == 7) {
            return 2;
        }
        return 1;
    }

    static int mapMoveNag(int raw) {
        if (raw <= 0) {
            return 0;
        }
        if (raw == 8) {
            return 7;
        }
        return raw & 0xFF;
    }

    static int mapEvalNag(int raw) {
        if (raw <= 0) {
            return 0;
        }
        if (raw == 0x0B) {
            return CONSTANTS.NAG_DRAWISH_POSITION;
        }
        return raw & 0xFF;
    }

    static int mapPrefixNag(int raw) {
        if (raw <= 0) {
            return 0;
        }
        return raw & 0xFF;
    }

    static String appendCommentPart(String existing, String addition) {
        if (addition == null || addition.isEmpty()) {
            if (existing == null) {
                return "";
            }
            return existing;
        }
        String trimmedAdd = addition.trim();
        if (trimmedAdd.isEmpty()) {
            if (existing == null) {
                return "";
            }
            return existing;
        }
        if (existing == null || existing.isEmpty()) {
            return trimmedAdd;
        }
        return existing + " " + trimmedAdd;
    }

    static void writeU16BE(byte[] b, int off, int v) {
        b[off] = (byte) ((v >>> 8) & 0xFF);
        b[off + 1] = (byte) (v & 0xFF);
    }

    static void writeU24BE(byte[] b, int off, int v) {
        b[off] = (byte) ((v >>> 16) & 0xFF);
        b[off + 1] = (byte) ((v >>> 8) & 0xFF);
        b[off + 2] = (byte) (v & 0xFF);
    }

    static void writeI32BE(byte[] b, int off, int v) {
        b[off] = (byte) ((v >>> 24) & 0xFF);
        b[off + 1] = (byte) ((v >>> 16) & 0xFF);
        b[off + 2] = (byte) ((v >>> 8) & 0xFF);
        b[off + 3] = (byte) (v & 0xFF);
    }

    static void writeI64BE(byte[] b, int off, long v) {
        writeI32BE(b, off, (int) (v >>> 32));
        writeI32BE(b, off + 4, (int) v);
    }

    static void writeU16LE(byte[] b, int off, int v) {
        b[off] = (byte) (v & 0xFF);
        b[off + 1] = (byte) ((v >>> 8) & 0xFF);
    }

    static void writeI32LE(byte[] b, int off, int v) {
        b[off] = (byte) (v & 0xFF);
        b[off + 1] = (byte) ((v >>> 8) & 0xFF);
        b[off + 2] = (byte) ((v >>> 16) & 0xFF);
        b[off + 3] = (byte) ((v >>> 24) & 0xFF);
    }

    static void writeI64LE(byte[] b, int off, long v) {
        writeI32LE(b, off, (int) v);
        writeI32LE(b, off + 4, (int) (v >>> 32));
    }

    static void writeFixedWin1252(byte[] buf, int off, int maxLen, String s) {
        if (s == null || s.isEmpty() || maxLen <= 0 || off >= buf.length) {
            return;
        }
        byte[] bytes = s.getBytes(WIN1252);
        int len = Math.min(bytes.length, Math.min(maxLen, buf.length - off));
        if (len > 0) {
            System.arraycopy(bytes, 0, buf, off, len);
        }
    }

    static String[] splitPlayerName(String fullName) {
        if (fullName == null) {
            return new String[]{"", ""};
        }
        String trimmed = fullName.trim();
        if (trimmed.isEmpty()) {
            return new String[]{"", ""};
        }
        int comma = trimmed.indexOf(',');
        if (comma >= 0) {
            String last = trimmed.substring(0, comma).trim();
            String first = trimmed.substring(comma + 1).trim();
            return new String[]{last, first};
        }
        return new String[]{trimmed, ""};
    }

    static int encodeDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return 0;
        }
        String[] parts = dateStr.trim().split("\\.");
        int year = 0;
        int month = 0;
        int day = 0;
        if (parts.length >= 1) {
            year = parseDatePart(parts[0], 0, 4095);
        }
        if (parts.length >= 2) {
            month = parseDatePart(parts[1], 1, 12);
        }
        if (parts.length >= 3) {
            day = parseDatePart(parts[2], 1, 31);
        }
        if (year == 0 && month == 0 && day == 0) {
            return 0;
        }
        return ((year & 0x7FFF) << 9) | ((month & 0x0F) << 5) | (day & 0x1F);
    }

    private static int parseDatePart(String s, int min, int max) {
        if (s == null) {
            return 0;
        }
        String trimmed = s.trim();
        if (trimmed.isEmpty() || trimmed.indexOf('?') >= 0) {
            return 0;
        }
        try {
            int val = Integer.parseInt(trimmed);
            if (val < min || val > max) {
                return 0;
            }
            return val;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static int encodeResult(String resultHeader, int resultConst) {
        if (resultHeader != null) {
            String r = resultHeader.trim();
            if ("0-1".equals(r)) {
                return 0;
            } else if ("1/2-1/2".equals(r)) {
                return 1;
            } else if ("1-0".equals(r)) {
                return 2;
            } else if ("*".equals(r)) {
                return 3;
            }
        }
        if (resultConst == CONSTANTS.RES_BLACK_WINS) {
            return 0;
        } else if (resultConst == CONSTANTS.RES_DRAW) {
            return 1;
        } else if (resultConst == CONSTANTS.RES_WHITE_WINS) {
            return 2;
        }
        return 3;
    }

    static int encodeEco(String ecoStr) {
        if (ecoStr == null) {
            return 0;
        }
        String s = ecoStr.trim().toUpperCase();
        if (s.length() < 3) {
            return 0;
        }
        char letter = s.charAt(0);
        char d1 = s.charAt(1);
        char d2 = s.charAt(2);
        if (letter < 'A' || letter > 'E' || d1 < '0' || d1 > '9' || d2 < '0' || d2 > '9') {
            return 0;
        }
        int num = (d1 - '0') * 10 + (d2 - '0');
        int ecoIdx = ((letter - 'A') * 100) + num + 1;
        return ecoIdx << 7;
    }

    static int[] parseRoundAndSubround(String roundStr) {
        if (roundStr == null || roundStr.isBlank()) {
            return new int[]{0, 0};
        }
        String trimmed = roundStr.trim();
        if ("?".equals(trimmed) || "-".equals(trimmed)) {
            return new int[]{0, 0};
        }
        int dot = trimmed.indexOf('.');
        int round;
        int subround = 0;
        if (dot >= 0) {
            round = parseClampedUint(trimmed.substring(0, dot), 255);
            subround = parseClampedUint(trimmed.substring(dot + 1), 255);
        } else {
            round = parseClampedUint(trimmed, 255);
        }
        return new int[]{round, subround};
    }

    static int parseElo(String eloStr) {
        if (eloStr == null || eloStr.isBlank()) {
            return 0;
        }
        return parseClampedUint(eloStr.trim(), 65535);
    }

    private static int parseClampedUint(String s, int max) {
        if (s == null || s.isBlank()) {
            return 0;
        }
        try {
            int val = Integer.parseInt(s.trim());
            if (val <= 0) {
                return 0;
            }
            return Math.min(val, max);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static int unmapMoveNag(int nag) {
        if (nag <= 0 || nag > CONSTANTS.MOVE_ANNOTATION_UPPER_LIMIT) {
            return 0;
        }
        if (nag == 7) {
            return 8;
        }
        return nag & 0xFF;
    }

    static int unmapEvalNag(int nag) {
        if (nag < CONSTANTS.POSITION_ANNOTATION_LOWER_LIMIT || nag > CONSTANTS.POSITION_ANNOTATION_UPPER_LIMIT) {
            return 0;
        }
        if (nag == CONSTANTS.NAG_DRAWISH_POSITION) {
            return 0x0B;
        }
        return nag & 0xFF;
    }

    static int unmapPrefixNag(int nag) {
        if (nag <= CONSTANTS.POSITION_ANNOTATION_UPPER_LIMIT || nag > 255) {
            return 0;
        }
        return nag & 0xFF;
    }

    static int[] extractNags(List<Integer> nags) {
        int moveNag = 0;
        int evalNag = 0;
        int prefixNag = 0;
        if (nags != null) {
            for (int nag : nags) {
                if (nag >= 1 && nag <= CONSTANTS.MOVE_ANNOTATION_UPPER_LIMIT) {
                    if (moveNag == 0) {
                        moveNag = unmapMoveNag(nag);
                    }
                } else if (nag >= CONSTANTS.POSITION_ANNOTATION_LOWER_LIMIT
                        && nag <= CONSTANTS.POSITION_ANNOTATION_UPPER_LIMIT) {
                    if (evalNag == 0) {
                        evalNag = unmapEvalNag(nag);
                    }
                } else if (nag > CONSTANTS.POSITION_ANNOTATION_UPPER_LIMIT && nag <= 255) {
                    if (prefixNag == 0) {
                        prefixNag = unmapPrefixNag(nag);
                    }
                }
            }
        }
        return new int[]{moveNag, evalNag, prefixNag};
    }

    static int extractCastlingMask(Board board) {
        int mask = 0;
        if (board == null) {
            return 0;
        }
        if (board.canCastleWhiteQueen()) {
            mask |= 0x01;
        }
        if (board.canCastleWhiteKing()) {
            mask |= 0x02;
        }
        if (board.canCastleBlackQueen()) {
            mask |= 0x04;
        }
        if (board.canCastleBlackKing()) {
            mask |= 0x08;
        }
        return mask;
    }

    static int parseEpFile(Board board) {
        if (board == null) {
            return 0;
        }
        String ep = board.getEnPassantSquare();
        if (ep == null || ep.length() < 2 || "-".equals(ep)) {
            return 0;
        }
        char c = Character.toLowerCase(ep.charAt(0));
        if (c >= 'a' && c <= 'h') {
            return (c - 'a') + 1;
        }
        return 0;
    }

    static boolean isCustomStart(Game game) {
        if (game == null || game.getRootNode() == null) {
            return false;
        }
        Board rootBoard = game.getRootNode().getBoard();
        if (rootBoard != null && !rootBoard.isInitialPosition()) {
            return true;
        }
        String fenHeader = game.getHeader("FEN");
        if (fenHeader != null && !fenHeader.isBlank()) {
            if (!fenHeader.trim().startsWith("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq")) {
                return true;
            }
        }
        return "1".equals(game.getHeader("SetUp")) || "1".equals(game.getHeader("Setup"));
    }

    static boolean hasNodeAnnotations(GameNode node) {
        if (node == null) {
            return false;
        }
        if (node.getComment() != null && !node.getComment().isEmpty()) {
            return true;
        }
        if (node.getNags() != null && !node.getNags().isEmpty()) {
            return true;
        }
        if (!node.getColoredFields().isEmpty()) {
            return true;
        }
        return !node.getArrows().isEmpty();
    }
}
