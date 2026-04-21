/* JFXChess - A Chess Graphical User Interface
 * Copyright (C) 2020-2025 Dominik Klein
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

/*
 PolyglotExt is a simple bookformat
 of consecutive entries of 19 bytes each.
 Entries are stored in ascending order w.r.t.
 the zobrist hash; i.e. same as polyglot,
 but weights, learn etc. replaced by different data fields
 moves are encoded the same as in polyglot

 Entry                     #bytes
 uint64 zobrist              8
 uint16 move                 2
 uint32 count                4
 uint8 white win percentage  1
 uint8 draw percentage       1
 uint8 white loss percentage 1
 uint16 average elo          2
 -----------------------------
                            19
 */

public class PolyglotExtEntry {
    long key;
    int move;
    long count;
    int whiteWinPerc;
    int blackWinPerc;
    int drawPerc;
    int avgElo;
    String uci;

    public long getPosCount() { return count; }
    public void setPosCount(long count) { this.count = count; }

    public String getMove() { return uci; }
    public void setMove(String uci) { this.uci = uci; }

    public Integer getWins() { return whiteWinPerc; }
    public void setWins(Integer whiteWinPerc) { this.whiteWinPerc = whiteWinPerc; }

    public Integer getDraws() { return drawPerc; }
    public void setDraws(Integer drawPerc) { this.drawPerc = drawPerc; }

    public Integer getLosses() { return blackWinPerc; }
    public void setLosses(Integer blackWinPerc) { this.blackWinPerc = blackWinPerc; }

    public int getAvgELO() { return avgElo; }
    public void setAvgELO(int avgElo) { this.avgElo = avgElo; }

    @Override
    public String toString() {
        return "" + key + "   " + count + "  " + move + " " + uci + " " + whiteWinPerc + " " + drawPerc + " " + blackWinPerc + " " + avgElo;
    }


}