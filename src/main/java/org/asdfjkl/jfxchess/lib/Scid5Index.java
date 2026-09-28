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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

public class Scid5Index {

    public static final int INDEX_ENTRY_SIZE = 56;

    public static ArrayList<Scid5GameInfo> readAll(Path si5Path, Scid5Namebase namebase, ProgressListener listener)
            throws IOException {
        ArrayList<Scid5GameInfo> entries = new ArrayList<>();
        if (!Files.exists(si5Path)) {
            return entries;
        }

        byte[] bytes = Files.readAllBytes(si5Path);
        int totalGames = bytes.length / INDEX_ENTRY_SIZE;
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < totalGames; i++) {
            if (listener != null && i % 1000 == 0 && totalGames > 0) {
                listener.onProgress((int) ((long) i * 100 / totalGames));
            }
            Scid5GameInfo info = decodeEntry(buf, i, namebase);
            entries.add(info);
        }
        return entries;
    }

    public static Scid5GameInfo decodeEntry(ByteBuffer buf, int gameNumber, Scid5Namebase namebase) {
        int w0 = buf.getInt();
        int w1 = buf.getInt();
        int w2 = buf.getInt();
        int w3 = buf.getInt();
        int w4 = buf.getInt();
        int w5 = buf.getInt();
        int w6 = buf.getInt();
        int w7 = buf.getInt();
        int w8 = buf.getInt();
        int w9 = buf.getInt();
        int w10 = buf.getInt();
        int w11 = buf.getInt();
        byte[] hpData = new byte[8];
        buf.get(hpData);

        int commentRating = (w0 >>> 28) & 0x0F;
        int whiteId = w0 & 0x0FFFFFFF;

        int variationRating = (w1 >>> 28) & 0x0F;
        int blackId = w1 & 0x0FFFFFFF;

        int nagRating = (w2 >>> 28) & 0x0F;
        int eventId = w2 & 0x0FFFFFFF;

        int siteId = w3;

        int variant = (w4 >>> 31) & 1;
        int roundId = w4 & 0x7FFFFFFF;

        int whiteElo = (w5 >>> 20) & 0x0FFF;
        int gameDate = w5 & 0x0FFFFF;

        int blackElo = (w6 >>> 20) & 0x0FFF;
        int eventDate = w6 & 0x0FFFFF;

        int halfMoves = (w7 >>> 22) & 0x03FF;
        int flags = w7 & 0x003FFFFF;

        int dataSize = (w8 >>> 15) & 0x01FFFF;
        long offsetHigh = w8 & 0x7FFF;
        long offsetLow = w9 & 0xFFFFFFFFL;
        long offset = (offsetHigh << 32) | offsetLow;

        int storedLineCode = (w10 >>> 24) & 0xFF;
        int finalMatSig = w10 & 0x00FFFFFF;

        int homePawnCount = (w11 >>> 24) & 0xFF;
        int whiteRatingType = (w11 >>> 21) & 0x07;
        int blackRatingType = (w11 >>> 18) & 0x07;
        int resultCode = (w11 >>> 16) & 0x03;
        int ecoCode = w11 & 0xFFFF;

        Scid5GameInfo info = new Scid5GameInfo();
        info.setGameNumber(gameNumber);

        info.setCommentRating(commentRating);
        info.setWhiteId(whiteId);
        info.setWhite(namebase != null ? namebase.getPlayer(whiteId) : "");

        info.setVariationRating(variationRating);
        info.setBlackId(blackId);
        info.setBlack(namebase != null ? namebase.getPlayer(blackId) : "");

        info.setNagRating(nagRating);
        info.setEventId(eventId);
        info.setEvent(namebase != null ? namebase.getEvent(eventId) : "");

        info.setSiteId(siteId);
        info.setSite(namebase != null ? namebase.getSite(siteId) : "");

        info.setVariant(variant);
        info.setRoundId(roundId);
        info.setRound(namebase != null ? namebase.getRound(roundId) : "");

        info.setWhiteEloVal(whiteElo);
        info.setGameDatePacked(gameDate);

        info.setBlackEloVal(blackElo);
        info.setEventDatePacked(eventDate);

        info.setHalfMoves(halfMoves);
        info.setFlags(flags);

        info.setSg5Length(dataSize);
        info.setSg5Offset(offset);

        info.setStoredLineCode(storedLineCode);
        info.setFinalMatSig(finalMatSig);

        info.setHomePawnCount(homePawnCount);
        info.setWhiteRatingType(whiteRatingType);
        info.setBlackRatingType(blackRatingType);
        info.setResult(Scid5GameInfo.decodeResult(resultCode));
        info.setEcoCode(ecoCode);
        info.setHomePawnData(hpData);

        info.markValid();
        return info;
    }

    private static int pack(int high, int highBits, int low) {
        int lowBits = 32 - highBits;
        int lowMask = (lowBits == 32) ? -1 : ((1 << lowBits) - 1);
        return (high << lowBits) | (low & lowMask);
    }

    public static void encodeEntry(Scid5GameInfo info, ByteBuffer buf) {
        int w0 = pack(info.getCommentRating(), 4, info.getWhiteId());
        int w1 = pack(info.getVariationRating(), 4, info.getBlackId());
        int w2 = pack(info.getNagRating(), 4, info.getEventId());
        int w3 = info.getSiteId();
        int w4 = pack(info.getVariant(), 1, info.getRoundId());
        int w5 = pack(info.getWhiteEloVal(), 12, info.getGameDatePacked());
        int w6 = pack(info.getBlackEloVal(), 12, info.getEventDatePacked());
        int w7 = pack(info.getHalfMoves(), 10, info.getFlags());
        int w8 = pack(info.getSg5Length(), 17, (int) (info.getSg5Offset() >>> 32));
        int w9 = (int) info.getSg5Offset();
        int w10 = pack(info.getStoredLineCode(), 8, info.getFinalMatSig());

        int rtypesResult = (info.getWhiteRatingType() << 5) |
                (info.getBlackRatingType() << 2) |
                Scid5GameInfo.encodeResult(info.getResult());
        int w11 = pack((info.getHomePawnCount() << 8) | (rtypesResult & 0xFF), 16, info.getEcoCode());

        buf.putInt(w0);
        buf.putInt(w1);
        buf.putInt(w2);
        buf.putInt(w3);
        buf.putInt(w4);
        buf.putInt(w5);
        buf.putInt(w6);
        buf.putInt(w7);
        buf.putInt(w8);
        buf.putInt(w9);
        buf.putInt(w10);
        buf.putInt(w11);
        byte[] hp = info.getHomePawnData();
        if (hp == null || hp.length != 8) {
            hp = new byte[8];
        }
        buf.put(hp);
    }

    public static byte[] encodeEntryToBytes(Scid5GameInfo info) {
        ByteBuffer buf = ByteBuffer.allocate(INDEX_ENTRY_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        encodeEntry(info, buf);
        return buf.array();
    }

    public static void appendEntry(Path si5Path, Scid5GameInfo info) throws IOException {
        byte[] bytes = encodeEntryToBytes(info);
        Files.write(si5Path, bytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public static void updateEntry(Path si5Path, int gameNumber, Scid5GameInfo info) throws IOException {
        byte[] bytes = encodeEntryToBytes(info);
        long offset = (long) gameNumber * INDEX_ENTRY_SIZE;
        try (RandomAccessFile raf = new RandomAccessFile(si5Path.toFile(), "rw")) {
            raf.seek(offset);
            raf.write(bytes);
        }
    }
}
