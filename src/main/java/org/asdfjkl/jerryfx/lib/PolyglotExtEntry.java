package org.asdfjkl.jerryfx.lib;

/*
 PolyglotExt is a simple bookformat
 of consecutive entries of 19 bytes each
 entries are stored in ascending order w.r.t.
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


    @Override
    public String toString() {
        return "" + key + "   " + count + "  " + move + " " + uci + " " + whiteWinPerc + " " + drawPerc + " " + blackWinPerc + " " + avgElo;
    }


}