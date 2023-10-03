/* JerryFX - A Chess Graphical User Interface
 * Copyright (C) 2020 Dominik Klein
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

import org.asdfjkl.jfxchess.gui.PgnDatabaseEntry;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class TestCases {

    public void fenTest() {

        System.out.println("TEST: fen reading & parsing");
        // starting position
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        Board b = new Board(fen);
        System.out.println("in : " + fen);
        System.out.println("out: " + b.fen());
        System.out.println(b);

        fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1";
        b = new Board(fen);
        System.out.println("in : " + fen);
        System.out.println("out: " + b.fen());
        System.out.println(b);

        fen = "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2";
        b = new Board(fen);
        System.out.println("in : " + fen);
        System.out.println("out: " + b.fen());
        System.out.println(b);

        fen = "rnbqkbnr/pp1ppppp/8/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2";
        b = new Board(fen);
        System.out.println("in : " + fen);
        System.out.println("out: " + b.fen());
        System.out.println(b);

    }

    private int countMoves(Board b, int depth) {
        int count = 0;
        ArrayList<Move> mvs = b.legalMoves();
        if(depth == 0) {
            return mvs.size();
        } else {
            // recursive case: for each possible move, apply
            // the move, do the recursive call and undo the move
            for(Move mi : mvs ) {
                b.apply(mi);
                int cnt_i = countMoves(b.makeCopy(), depth - 1);
                count += cnt_i;
                b.undo();
            }
            return count;
        }
    }

    public void runPerfT() {

        System.out.println("TEST: PerfT");
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        Board b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 1, expected 20");
        int c = countMoves(b,0);
        System.out.println("computed: " + c);

        fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 2, expected 400");
        c = countMoves(b,1);
        System.out.println("computed: " + c);

        fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 3, expected 8902");
        c = countMoves(b,2);
        System.out.println("computed: " + c);

        fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 4, expected 197281");
        c = countMoves(b,3);
        System.out.println("computed: " + c);

        fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 5, expected 4865609");
        c = countMoves(b,4);
        System.out.println("computed: " + c);

        // "Kiwipete" by Peter McKenzie, great for identifying bugs
        // perft 1 - 5
        fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 1, expected 48");
        c = countMoves(b,0);
        System.out.println("computed: " + c);

        fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 2, expected 2039");
        c = countMoves(b,1);
        System.out.println("computed: " + c);

        fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 3, expected 97862");
        c = countMoves(b,2);
        System.out.println("computed: " + c);


        fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 4, expected 4085603");
        c = countMoves(b,3);
        System.out.println("computed: " + c);

        fen = "8/3K4/2p5/p2b2r1/5k2/8/8/1q6 b - - 1 67";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 1, expected 50");
        c = countMoves(b,0);
        System.out.println("computed: " + c);

        fen = "8/3K4/2p5/p2b2r1/5k2/8/8/1q6 b - - 1 67";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 2, expected 279");
        c = countMoves(b,1);
        System.out.println("computed: " + c);

        fen = "rnbqkb1r/ppppp1pp/7n/4Pp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 5, expected 11139762");
        c = countMoves(b,4);
        System.out.println("computed: " + c);

        fen = "rnbqkb1r/ppppp1pp/7n/4Pp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 5, expected 11139762");
        c = countMoves(b,4);
        System.out.println("computed: " + c);

        fen = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 1, expected 44");
        c = countMoves(b,0);
        System.out.println("computed: " + c);

        fen = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 2, expected 1486");
        c = countMoves(b,1);
        System.out.println("computed: " + c);

        fen = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 3, expected 62379");
        c = countMoves(b,2);
        System.out.println("computed: " + c);

        fen = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 4, expected 2103487");
        c = countMoves(b,3);
        System.out.println("computed: " + c);

        fen = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 5, expected 89941194");
        c = countMoves(b,4);
        System.out.println("computed: " + c);

        fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 6, expected 119060324");
        c = countMoves(b,5);
        System.out.println("computed: " + c);

        fen = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 0";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 6, expected 11030083");
        c = countMoves(b,5);
        System.out.println("computed: " + c);

        fen = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 0";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 7, expected 178633661");
        c = countMoves(b,6);
        System.out.println("computed: " + c);

        fen = "8/7p/p5pb/4k3/P1pPn3/8/P5PP/1rB2RK1 b - d3 0 28";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 6, expected 38633283");
        c = countMoves(b,5);
        System.out.println("computed: " + c);

        fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0";
        b = new Board(fen);
        System.out.println("Testing " + b.fen());
        System.out.println("Perft 5, expected 193690690");
        c = countMoves(b,4);
        System.out.println("computed: " + c);

    }

    public void runSanTest() {

        System.out.println("TEST: san computation");
        Board b0 = new Board("rnbqkbnr/pppppppp/8/2R5/5R2/2R5/PPPPPPP1/1NBQKBN1 w - - 0 1");
        System.out.println("rnbqkbnr/pppppppp/8/2R5/5R2/2R5/PPPPPPP1/1NBQKBN1 w - - 0 1");
        ArrayList<Move> b0Legals = b0.legalMoves();
        for(Move mi : b0Legals) {
            System.out.println(b0.san(mi));
        }

        Board b1 = new Board("rnbqkbnr/pppppppp/8/2R5/8/2R5/PPPPPPP1/1NBQKBN1 w - - 0 1");
        System.out.println("rnbqkbnr/pppppppp/8/2R5/8/2R5/PPPPPPP1/1NBQKBN1 w - - 0 1");
        ArrayList<Move> b1Legals = b1.legalMoves();
        for(Move mi : b1Legals) {
            System.out.println(b1.san(mi));
        }
    }

    public void runBitSetTest() {
        System.out.println("TEST: bit setting in int values");
        // e.g. distance one, i.e. index 1 (=left, up, down, right square) has
        // value 0x1C = MSB 00011100 LSB, i.e. king, queen, rook can
        // potentially attack
        // 0              Knight
        // 1              Bishop
        // 2              Rook
        // 3              Queen
        // 4              King
        Board b_temp = new Board();
        int attackIdx1 = CONSTANTS.ATTACK_TABLE[1];
        System.out.println("attackIdx1: " + attackIdx1);
        System.out.println("expectec: MSB 00011100 LSB");
        System.out.println("bit 0 (Knight): "+ b_temp.isKthBitSet(attackIdx1, 0));
        System.out.println("bit 1 (bishop): "+ b_temp.isKthBitSet(attackIdx1, 1));
        System.out.println("bit 2 (rook): "+ b_temp.isKthBitSet(attackIdx1, 2));
        System.out.println("bit 3 (queen): "+ b_temp.isKthBitSet(attackIdx1, 3));
        System.out.println("bit 4 (king): "+ b_temp.isKthBitSet(attackIdx1, 4));
    }

    public void runPgnPrintTest() {

        System.out.println("TEST: simple PGN printing");
        Game g = new Game();
        g.setHeader("Event", "Knaurs Schachbuch");
        g.setHeader("Site", "Paris");
        g.setHeader("Date", "1859.??.??");
        g.setHeader("Round", "1");
        g.setHeader("White", "Morphy");
        g.setHeader("Black", "NN");
        g.setHeader("Result", "1-0");
        g.setHeader("ECO", "C56");

        Board rootBoard = new Board(true);
        g.getRootNode().setBoard(rootBoard);

        g.applyMove(new Move("e2e4"));
        g.applyMove(new Move("e7e5"));
        g.applyMove(new Move("g1f3"));
        g.applyMove(new Move("b8c6"));
        g.applyMove(new Move("f1c4"));
        g.applyMove(new Move("g8f6"));
        g.applyMove(new Move("d2d4"));
        g.applyMove(new Move("e5d4"));
        g.applyMove(new Move("e1g1"));
        g.applyMove(new Move("f6e4"));

        PgnPrinter printer = new PgnPrinter();
        System.out.println(printer.printGame(g));
        printer.writeGame(g, "temp.pgn");

    }

    public void pgnScanTest() {

        System.out.println("TEST: scanning PGN for game offsets");
        String kingbase = "C:/Users/user/MyFiles/workspace/test_databases/KingBaseLite2016-03-E60-E99.pgn";
        String millbase = "C:/Users/user/MyFiles/workspace/test_databases/millionbase-2.22.pgn";
        String middleg = "C:/Users/user/MyFiles/workspace/test_databases/middleg.pgn";
        PgnReader reader = new PgnReader();
        if(reader.isIsoLatin1(millbase)) {
            reader.setEncodingIsoLatin1();
        }

        long startTime = System.currentTimeMillis();
        ArrayList<Long> offsets = reader.scanPgn(millbase);
        long stopTime = System.currentTimeMillis();

        long timeElapsed = stopTime - startTime;
        System.out.println("elapsed time: "+(timeElapsed/1000)+" secs");

        System.out.println(offsets.size());

        for(int i=0;i<10;i++) {
            long offset_i = offsets.get(i);
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(millbase, "r");
                try {
                    raf.seek(offset_i);
                    String line = raf.readLine();
                    System.out.println("START"+line+"STOP");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                if(raf != null) {
                    try {
                        raf.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void pgnScanSTRTest() {

        System.out.println("TEST: scanning PGN for game STR offsets");
        String kingbase = "C:/Users/user/MyFiles/workspace/test_databases/KingBaseLite2016-03-E60-E99.pgn";
        String millbase = "C:/Users/user/MyFiles/workspace/test_databases/millionbase-2.22.pgn";
        String middleg = "C:/Users/user/MyFiles/workspace/test_databases/middleg.pgn";
        PgnReader reader = new PgnReader();
        if(reader.isIsoLatin1(millbase)) {
            reader.setEncodingIsoLatin1();
        }

        long startTime = System.currentTimeMillis();
        ArrayList<PgnDatabaseEntry> entries = reader.scanPgnGetSTR(millbase);
        long stopTime = System.currentTimeMillis();

        long timeElapsed = stopTime - startTime;
        System.out.println("elapsed time: "+(timeElapsed/1000)+" secs");

        System.out.println(entries.size());

        for(int i=0;i<10;i++) {
            PgnDatabaseEntry entry_i = entries.get(i);
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(millbase, "r");
                try {
                    raf.seek(entry_i.getOffset());
                    String line = raf.readLine();
                    System.out.println("START"+line+"STOP");
                    System.out.println(entry_i.getEvent());
                    System.out.println("");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                if(raf != null) {
                    try {
                        raf.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void pgnReadGameTest() {

        System.out.println("TEST: reading single PGN game");
        String kingbase = "C:/Users/user/MyFiles/workspace/millbase_prev_last.pgn";
        OptimizedRandomAccessFile raf = null;
        PgnReader reader = new PgnReader();
        if(reader.isIsoLatin1(kingbase)) {
            reader.setEncodingIsoLatin1();
        }
        PgnPrinter printer = new PgnPrinter();
        try {
            raf = new OptimizedRandomAccessFile(kingbase, "r");
            Game g = reader.readGame(raf);
            //System.out.println("reading game ok");
            String pgn = printer.printGame(g);
            System.out.println(pgn);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pgnReadMiddleGTest() {

        System.out.println("TEST: reading all games from middleg.pgn");
        String middleg = "C:/Users/user/MyFiles/workspace/test_databases/middleg.pgn";

        OptimizedRandomAccessFile raf = null;
        PgnReader reader = new PgnReader();
        if(reader.isIsoLatin1(middleg)) {
            reader.setEncodingIsoLatin1();
        }
        ArrayList<Long> offsets = reader.scanPgn(middleg);

        PgnPrinter printer = new PgnPrinter();
        try {
            raf = new OptimizedRandomAccessFile(middleg, "r");
            for (int i=0;i<offsets.size();i++) {
                long offset_i = offsets.get(i);
                System.out.println("reading game nr: "+(i+1));
                raf.seek(offset_i);
                Game g = reader.readGame(raf);
                System.out.println("reading game ok");
                String pgn = printer.printGame(g);
                System.out.println(pgn);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pgnReadAllMillBaseTest() {

        System.out.println("TEST: reading all games from millionbase-2.22.pgn");
        String millbase = "C:/Users/user/MyFiles/workspace/test_databases/millionbase-2.22.pgn";
        PgnReader reader = new PgnReader();
        if(reader.isIsoLatin1(millbase)) {
            reader.setEncodingIsoLatin1();
        }

        long startTime = System.currentTimeMillis();
        ArrayList<Long> offsets = reader.scanPgn(millbase);
        long stopTime = System.currentTimeMillis();
        long timeElapsed = stopTime - startTime;
        System.out.println("elapsed time for scanning: " + (timeElapsed / 1000) + " secs");

        System.out.println(offsets.size());
        OptimizedRandomAccessFile raf = null;
        try {
            raf = new OptimizedRandomAccessFile(millbase, "r");
            startTime = System.currentTimeMillis();
            for (int i = 0; i < offsets.size(); i++) {
                long offset_i = offsets.get(i);
                //System.out.println("reading game "+i);
                if(i%100000 == 0) {
                    System.out.println("i: "+i);
                    stopTime = System.currentTimeMillis();
                    timeElapsed = stopTime - startTime;
                    System.out.println("elapsed time for reading each game: " + (timeElapsed / 1000) + " secs");
                }
                raf.seek(offset_i);
                Game g = reader.readGame(raf);
            }
            stopTime = System.currentTimeMillis();
            timeElapsed = stopTime - startTime;
            System.out.println("elapsed time for reading all games: " + (timeElapsed / 1000) + " secs");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    // using file open/close
    public void pgnReadSingleEntryTestOpenClose() {

        System.out.println("TEST: scanning offsets from PGN, and reading each hader w/ multiple fopen/close");
        String kingbase = "C:/Users/user/MyFiles/workspace/test_databases/KingBaseLite2016-03-E60-E99.pgn";
        String millbase = "C:/Users/user/MyFiles/workspace/test_databases/millionbase-2.22.pgn";
        String middleg = "C:/Users/user/MyFiles/workspace/test_databases/middleg.pgn";
        PgnReader reader = new PgnReader();
        if(reader.isIsoLatin1(millbase)) {
            reader.setEncodingIsoLatin1();
        }

        long startTime = System.currentTimeMillis();
        ArrayList<Long> offsets = reader.scanPgn(millbase);
        long stopTime = System.currentTimeMillis();
        long timeElapsed = stopTime - startTime;
        System.out.println("elapsed time for scanning: "+(timeElapsed/1000)+" secs");

        System.out.println(offsets.size());

        int matchCount = 0;
        startTime = System.currentTimeMillis();
        for(int i=0;i<offsets.size();i++) {
            long offset_i = offsets.get(i);
            HashMap<String,String> header = reader.readSingleHeader(millbase, offset_i);
            if(header.get("Event").equals("Barbera Open")) {
                matchCount += 1;
            }
        }
        stopTime = System.currentTimeMillis();
        timeElapsed = stopTime - startTime;
        System.out.println("elapsed time for reading each header: "+(timeElapsed/1000)+" secs");
        System.out.println("games matching event 'Barabara Open' "+matchCount);

    }

    // using raf that is kept open
    public void pgnReadSingleEntryTestSeekWithinRAF() {

        System.out.println("TEST: scanning offsets from PGN, and reading each hader, keeping file open");
        String kingbase = "C:/Users/user/MyFiles/workspace/test_databases/KingBaseLite2016-03-E60-E99.pgn";
        String millbase = "C:/Users/user/MyFiles/workspace/test_databases/millionbase-2.22.pgn";
        String middleg = "C:/Users/user/MyFiles/workspace/test_databases/middleg.pgn";
        PgnReader reader = new PgnReader();

        long startTime = System.currentTimeMillis();
        ArrayList<Long> offsets = reader.scanPgn(millbase);
        long stopTime = System.currentTimeMillis();
        long timeElapsed = stopTime - startTime;
        System.out.println("elapsed time for scanning: " + (timeElapsed / 1000) + " secs");

        System.out.println(offsets.size());
        OptimizedRandomAccessFile raf = null;
        try {
            raf = new OptimizedRandomAccessFile(millbase, "r");
            int matchCount = 0;
            startTime = System.currentTimeMillis();
            for (int i = 0; i < offsets.size(); i++) {
                long offset_i = offsets.get(i);
                HashMap<String, String> header = reader.readSingleHeader(raf, offset_i);
                if (header.get("Event").equals("Barbera Open")) {
                    matchCount += 1;
                }
            }
            stopTime = System.currentTimeMillis();
            timeElapsed = stopTime - startTime;
            System.out.println("elapsed time for reading each header: " + (timeElapsed / 1000) + " secs");
            System.out.println("games matching event 'Barabara Open' " + matchCount);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void readGamesByStringTest() {

        String s = "[Event \"Berlin\"]\n" +
                "[Site \"Berlin GER\"]\n" +
                "[Date \"1852.??.??\"]\n" +
                "[EventDate \"?\"]\n" +
                "[Round \"?\"]\n" +
                "[Result \"1-0\"]\n" +
                "[White \"Adolf Anderssen\"]\n" +
                "[Black \"Jean Dufresne\"]\n" +
                "[ECO \"C52\"]\n" +
                "[WhiteElo \"?\"]\n" +
                "[BlackElo \"?\"]\n" +
                "[PlyCount \"47\"]\n" +
                "\n" +
                "1.e4 e5 2.Nf3 Nc6 3.Bc4 Bc5 4.b4 Bxb4 5.c3 Ba5 6.d4 exd4 7.O-O\n" +
                "d3 8.Qb3 Qf6 9.e5 Qg6 10.Re1 Nge7 11.Ba3 b5 12.Qxb5 Rb8 13.Qa4\n" +
                "Bb6 14.Nbd2 Bb7 15.Ne4 Qf5 16.Bxd3 Qh5 17.Nf6+ gxf6 18.exf6\n" +
                "Rg8 19.Rad1 Qxf3 20.Rxe7+ Nxe7 21.Qxd7+ Kxd7 22.Bf5+ Ke8\n" +
                "23.Bd7+ Kf8 24.Bxe7# 1-0";
        PgnReader reader = new PgnReader();
        PgnPrinter printer = new PgnPrinter();
        Game g = reader.readGame(s);
        System.out.println(printer.printGame(g));

    }

    public void runPosHashTest() {

        Board b1 = new Board("rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq d3 0 1");
        Board b2 = new Board("rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq d3 0 1");
        long key1 = b1.getPositionHash();
        long key2 = b2.getPositionHash();
        System.out.println("Posh Hash1: "+key1);
        System.out.println("Posh Hash2: "+key2);

        System.out.println("TEST: reading single PGN game, trying to find starting pos after 1d4 by pos hash");
        String kingbase = "C:/Users/user/MyFiles/workspace/test_databases/KingBaseLite2016-03-E60-E99.pgn";
        OptimizedRandomAccessFile raf = null;
        PgnReader reader = new PgnReader();
        if(reader.isIsoLatin1(kingbase)) {
            reader.setEncodingIsoLatin1();
        }
        PgnPrinter printer = new PgnPrinter();
        try {
            raf = new OptimizedRandomAccessFile(kingbase, "r");
            Game g = reader.readGame(raf);
            //System.out.println("reading game ok");
            boolean hasStartingPos = g.containsPosition(key1, 0 , 100);
            System.out.println("found pos: "+hasStartingPos);
            //String pgn = printer.printGame(g);
            //System.out.println(pgn);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void runZobristTest() {

        System.out.println("TEST: zobrist hashing");

        Board b = new Board("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        long key = b.getZobrist();
        System.out.println("expected zobrist: 463b96181691fc9c");
        System.out.println("got zobrist.....: " + Long.toHexString(key));

        b = new Board("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
        key = b.getZobrist();
        System.out.println("expected zobrist: 823c9b50fd114196");
        System.out.println("got zobrist.....: " + Long.toHexString(key));

        b = new Board("rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2");
        key = b.getZobrist();
        System.out.println("expected zobrist: 756b94461c50fb0");
        System.out.println("got zobrist.....: " + Long.toHexString(key));

        b = new Board("rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR b KQkq - 0 2");
        key = b.getZobrist();
        System.out.println("expected zobrist: 662fafb965db29d4");
        System.out.println("got zobrist.....: " + Long.toHexString(key));

        b = new Board("rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3");
        key = b.getZobrist();
        System.out.println("expected zobrist: 22a48b5a8e47ff78");
        System.out.println("got zobrist.....: " + Long.toHexString(key));

        b = new Board("rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPPKPPP/RNBQ1BNR b kq - 0 3");
        key = b.getZobrist();
        System.out.println("expected zobrist: 652a607ca3f242c1");
        System.out.println("got zobrist.....: " + Long.toHexString(key));

        b = new Board("rnbq1bnr/ppp1pkpp/8/3pPp2/8/8/PPPPKPPP/RNBQ1BNR w - - 0 4");
        key = b.getZobrist();
        System.out.println("expected zobrist: fdd303c946bdd9");
        System.out.println("got zobrist.....: " + Long.toHexString(key));

        b = new Board("rnbqkbnr/p1pppppp/8/8/PpP4P/8/1P1PPPP1/RNBQKBNR b KQkq c3 0 3");
        key = b.getZobrist();
        System.out.println("expected zobrist: 3c8123ea7b067637");
        System.out.println("got zobrist.....: " + Long.toHexString(key));

        b = new Board("rnbqkbnr/p1pppppp/8/8/P6P/R1p5/1P1PPPP1/1NBQKBNR b Kkq - 0 4");
        key = b.getZobrist();
        System.out.println("expected zobrist: 5c3f9b829b279560");
        System.out.println("got zobrist.....: " + Long.toHexString(key));

    }

    public void testPolyglot() {
        //URL invalid = getClass().getClassLoader().getResource("foobar");

        Polyglot pg1 = new Polyglot();

        File file = null;
        URL book = getClass().getClassLoader().getResource("book/varied.bin");
        if(book != null) {
            file = new File(book.getFile());
            pg1.loadBook(file);
        }
        System.out.println(pg1.readFile);
        try {
            PolyglotEntry e = pg1.getEntryFromOffset(0x62c20);
            System.out.println(e.uci);

            ArrayList<String> entries = pg1.findMoves(0x463b96181691fc9cL);
            for(String uci : entries) {
                System.out.println(uci);
            }

            entries = pg1.findMoves(0x2d3888dac361814aL);
            for(String uci : entries) {
                System.out.println(uci);
            }

            entries = pg1.findMoves(0x823c9b50fd114196L);
            for(String uci : entries) {
                System.out.println(uci);
            }

            System.out.println(0x463b96181691fc9cL);
            System.out.println(0x2d3888dac361814aL);
            System.out.println(0x823c9b50fd114196L);

        } catch(IllegalArgumentException e) {
            e.printStackTrace();
        }
    }


}
