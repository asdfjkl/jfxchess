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

import java.util.UUID;

/**
 * Lightweight lazy-resolving metadata record for a game in a modern `.2cbh`
 * database.
 */
public final class TwoCbhGameInfo extends GameInfo {

    private final TwoCbhNamebase namebase;
    private final int rawIndex;
    private final long cbgOffset;
    private final long cbaOffset;
    private final int whiteId;
    private final int blackId;
    private final int tournamentId;
    private final int packedDate;
    private final int elos;
    private final int ecoRaw;
    private final int roundAndResult;

    TwoCbhGameInfo(
            TwoCbhNamebase namebase,
            int rawIndex,
            long cbgOffset,
            long cbaOffset,
            int whiteId,
            int blackId,
            int tournamentId,
            int packedDate,
            int resultByte,
            int round,
            int subround,
            int whiteElo,
            int blackElo,
            int ecoRaw) {
        super(new UUID(0L, rawIndex));
        this.namebase = namebase;
        this.rawIndex = rawIndex;
        this.cbgOffset = cbgOffset;
        this.cbaOffset = cbaOffset;
        this.whiteId = whiteId;
        this.blackId = blackId;
        this.tournamentId = tournamentId;
        this.packedDate = packedDate;
        this.elos = ((whiteElo & 0xFFFF) << 16) | (blackElo & 0xFFFF);
        this.ecoRaw = ecoRaw & 0xFFFF;
        this.roundAndResult = ((round & 0xFF) << 16) | ((subround & 0xFF) << 8) | (resultByte & 0xFF);
        super.markValid();
    }

    int getRawIndex() {
        return rawIndex;
    }

    long getCbgOffset() {
        return cbgOffset;
    }

    long getCbaOffset() {
        return cbaOffset;
    }

    int getWhiteId() {
        return whiteId;
    }

    int getBlackId() {
        return blackId;
    }

    int getTournamentId() {
        return tournamentId;
    }

    int getPackedDate() {
        return packedDate;
    }

    int getResultByte() {
        return roundAndResult & 0xFF;
    }

    int getRoundNumber() {
        return (roundAndResult >>> 16) & 0xFF;
    }

    int getSubroundNumber() {
        return (roundAndResult >>> 8) & 0xFF;
    }

    int getWhiteEloVal() {
        return (elos >>> 16) & 0xFFFF;
    }

    int getBlackEloVal() {
        return elos & 0xFFFF;
    }

    int getEcoRaw() {
        return ecoRaw;
    }

    @Override
    public String getEvent() {
        return namebase.getEvent(tournamentId);
    }

    @Override
    public String getSite() {
        return namebase.getSite(tournamentId);
    }

    @Override
    public String getDate() {
        return CbCommon.decodeDate(packedDate);
    }

    @Override
    public String getRound() {
        return CbCommon.formatRound(getRoundNumber(), getSubroundNumber());
    }

    @Override
    public String getWhite() {
        return namebase.getPlayer(whiteId);
    }

    @Override
    public String getBlack() {
        return namebase.getPlayer(blackId);
    }

    @Override
    public String getResult() {
        return CbCommon.decodeResult(getResultByte());
    }

    @Override
    public String getEco() {
        return CbCommon.decodeEco(ecoRaw);
    }

    @Override
    public String getWhiteElo() {
        int val = getWhiteEloVal();
        if (val <= 0) {
            return "";
        }
        return Integer.toString(val);
    }

    @Override
    public String getBlackElo() {
        int val = getBlackEloVal();
        if (val <= 0) {
            return "";
        }
        return Integer.toString(val);
    }
}
