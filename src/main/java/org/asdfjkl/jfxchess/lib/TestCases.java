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

import org.asdfjkl.jfxchess.gui.DialogSave;
import org.asdfjkl.jfxchess.gui.GameSession;
import org.asdfjkl.jfxchess.gui.Model_JFXChess;
import org.asdfjkl.jfxchess.gui.PgnSourceReference;
import org.asdfjkl.jfxchess.gui.Workspace;

import javax.swing.JButton;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class TestCases {

    public void fenTest() {

        System.out.println("TEST: fen reading & parsing");
        boolean allPassed = true;

        String[] fens = {
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
            "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2",
            "rnbqkbnr/pp1ppppp/8/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2"
        };

        for (int i = 0; i < fens.length; i++) {
            Board b = new Board(fens[i]);
            String fenOut = b.fen();
            if (fens[i].equals(fenOut)) {
                System.out.println("testing fen " + (i + 1) + " ... pass");
            } else {
                System.out.println("testing fen " + (i + 1) + " ... FAIL");
                System.out.println("  in : " + fens[i]);
                System.out.println("  out: " + fenOut);
                System.out.println(b);
                allPassed = false;
            }
        }

        if (!allPassed) {
            throw new RuntimeException("fenTest failed");
        }
    }

    public static String getPgnPath(String filename) {
        File f = new File(filename);
        if (f.exists()) return f.getAbsolutePath();

        String[] candidatePaths = {
            "src/main/resources/pgn/" + filename,
            "src/main/resources/" + filename,
            "src/test/resources/pgn/" + filename,
            "src/test/resources/" + filename,
            "resources/pgn/" + filename,
            "pgn/" + filename
        };
        for (String p : candidatePaths) {
            f = new File(p);
            if (f.exists()) return f.getAbsolutePath();
        }

        URL url = TestCases.class.getClassLoader().getResource("pgn/" + filename);
        if (url == null) {
            url = TestCases.class.getClassLoader().getResource(filename);
        }
        if (url != null) {
            if ("file".equals(url.getProtocol())) {
                try {
                    return new File(url.toURI()).getAbsolutePath();
                } catch (Exception e) {
                    return new File(url.getPath()).getAbsolutePath();
                }
            } else {
                try {
                    File tempFile = File.createTempFile("chess_" + filename, ".pgn");
                    tempFile.deleteOnExit();
                    try (InputStream in = url.openStream();
                         FileOutputStream out = new FileOutputStream(tempFile)) {
                        in.transferTo(out);
                    }
                    return tempFile.getAbsolutePath();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return filename;
    }

    public static String getScidPath(String filename) {
        File f = new File(filename);
        if (f.exists()) return f.getAbsolutePath();

        String[] candidatePaths = {
            "src/main/resources/scid5/" + filename,
            "src/main/resources/" + filename,
            "resources/scid5/" + filename,
            "scid5/" + filename
        };
        for (String p : candidatePaths) {
            f = new File(p);
            if (f.exists()) return f.getAbsolutePath();
        }

        URL url = TestCases.class.getClassLoader().getResource("scid5/" + filename);
        if (url == null) {
            url = TestCases.class.getClassLoader().getResource(filename);
        }
        if (url != null) {
            if ("file".equals(url.getProtocol())) {
                try {
                    return new File(url.toURI()).getAbsolutePath();
                } catch (Exception e) {
                    return new File(url.getPath()).getAbsolutePath();
                }
            } else {
                try {
                    File tempFile = File.createTempFile("scid5_" + filename, ".tmp");
                    tempFile.deleteOnExit();
                    try (InputStream in = url.openStream();
                         FileOutputStream out = new FileOutputStream(tempFile)) {
                        in.transferTo(out);
                    }
                    return tempFile.getAbsolutePath();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return filename;
    }

    private int countMoves(Board b, int depth) {
        ArrayList<Move> mvs = b.legalMoves();
        if (depth == 0) {
            return mvs.size();
        } else if (depth >= 3) {
            return mvs.parallelStream().mapToInt(mi -> {
                Board copy = b.makeCopy();
                copy.apply(mi);
                ArrayList<Move> mvs2 = copy.legalMoves();
                return mvs2.parallelStream().mapToInt(mi2 -> {
                    Board copy2 = copy.makeCopy();
                    copy2.apply(mi2);
                    return countMovesSerial(copy2, depth - 2);
                }).sum();
            }).sum();
        } else {
            return mvs.parallelStream().mapToInt(mi -> {
                Board copy = b.makeCopy();
                copy.apply(mi);
                return countMovesSerial(copy, depth - 1);
            }).sum();
        }
    }

    private int countMovesSerial(Board b, int depth) {
        ArrayList<Move> mvs = b.legalMoves();
        if (depth == 0) {
            return mvs.size();
        } else {
            int count = 0;
            for (Move mi : mvs) {
                b.apply(mi);
                int cnt_i = countMovesSerial(b.makeCopy(), depth - 1);
                count += cnt_i;
                b.undo();
            }
            return count;
        }
    }

    private boolean testPerft(String fen, int depth, int perftNumber, int expected) {
        Board b = new Board(fen);
        int computed = countMoves(b, depth);
        if (computed == expected) {
            System.out.println("testing perft " + perftNumber + " ... pass");
            return true;
        } else {
            System.out.println("testing perft " + perftNumber + " ... FAIL");
            System.out.println("  fen     : " + fen);
            System.out.println("  expected: " + expected);
            System.out.println("  computed: " + computed);
            return false;
        }
    }

    public void runPerfT() {

        System.out.println("TEST: PerfT");
        boolean allPassed = true;

        if (!testPerft("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", 0, 1, 20)) allPassed = false;
        if (!testPerft("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", 1, 2, 400)) allPassed = false;
        if (!testPerft("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", 2, 3, 8902)) allPassed = false;
        if (!testPerft("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", 3, 4, 197281)) allPassed = false;
        if (!testPerft("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", 4, 5, 4865609)) allPassed = false;

        // "Kiwipete" by Peter McKenzie, great for identifying bugs
        // perft 1 - 5
        if (!testPerft("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0", 0, 1, 48)) allPassed = false;
        if (!testPerft("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0", 1, 2, 2039)) allPassed = false;
        if (!testPerft("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0", 2, 3, 97862)) allPassed = false;
        if (!testPerft("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0", 3, 4, 4085603)) allPassed = false;

        if (!testPerft("8/3K4/2p5/p2b2r1/5k2/8/8/1q6 b - - 1 67", 0, 1, 50)) allPassed = false;
        if (!testPerft("8/3K4/2p5/p2b2r1/5k2/8/8/1q6 b - - 1 67", 1, 2, 279)) allPassed = false;

        if (!testPerft("rnbqkb1r/ppppp1pp/7n/4Pp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3", 4, 5, 11139762)) allPassed = false;
        if (!testPerft("rnbqkb1r/ppppp1pp/7n/4Pp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3", 4, 5, 11139762)) allPassed = false;

        if (!testPerft("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8", 0, 1, 44)) allPassed = false;
        if (!testPerft("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8", 1, 2, 1486)) allPassed = false;
        if (!testPerft("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8", 2, 3, 62379)) allPassed = false;
        if (!testPerft("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8", 3, 4, 2103487)) allPassed = false;
        if (!testPerft("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8", 4, 5, 89941194)) allPassed = false;

        if (!testPerft("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", 5, 6, 119060324)) allPassed = false;

        if (!testPerft("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 0", 5, 6, 11030083)) allPassed = false;
        if (!testPerft("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 0", 6, 7, 178633661)) allPassed = false;

        if (!testPerft("8/7p/p5pb/4k3/P1pPn3/8/P5PP/1rB2RK1 b - d3 0 28", 5, 6, 38633283)) allPassed = false;

        if (!testPerft("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0", 4, 5, 193690690)) allPassed = false;

        if (!allPassed) {
            throw new RuntimeException("runPerfT failed");
        }
    }

    public void runSanTest() {

        System.out.println("TEST: san computation");
        boolean allPassed = true;

        String[] expectedB0 = {
            "a4", "a3", "b4", "b3", "d4", "d3", "e4", "e3", "f3", "g4",
            "g3", "Na3", "Nf3", "Nh3", "Rc3c4", "Rd3", "Re3", "Rcf3", "Rg3", "Rh3",
            "Rb3", "Ra3", "Rff5", "Rf6", "Rxf7", "Rff3", "Rg4", "Rh4", "Re4", "Rd4",
            "Rfc4", "Rb4", "Ra4", "Rc6", "Rxc7", "Rc5c4", "Rd5", "Re5", "Rcf5", "Rg5",
            "Rh5", "Rb5", "Ra5"
        };
        Board b0 = new Board("rnbqkbnr/pppppppp/8/2R5/5R2/2R5/PPPPPPP1/1NBQKBN1 w - - 0 1");
        ArrayList<Move> b0Legals = b0.legalMoves();
        ArrayList<String> b0Errors = new ArrayList<>();
        boolean[] b0Hit = new boolean[expectedB0.length];
        if (b0Legals.size() != expectedB0.length) {
            b0Errors.add("expected " + expectedB0.length + " legal moves, but got " + b0Legals.size());
        }
        for (int i = 0; i < b0Legals.size(); i++) {
            Move mi = b0Legals.get(i);
            String san = b0.san(mi);
            if (i < expectedB0.length) {
                if (san.equals(expectedB0[i])) {
                    b0Hit[i] = true;
                } else {
                    b0Errors.add("mismatch at index " + i + ": expected '" + expectedB0[i] + "', got '" + san + "'");
                    for (int j = 0; j < expectedB0.length; j++) {
                        if (!b0Hit[j] && san.equals(expectedB0[j])) {
                            b0Hit[j] = true;
                            break;
                        }
                    }
                }
            } else {
                b0Errors.add("unexpected extra move at index " + i + ": '" + san + "'");
            }
        }
        for (int i = 0; i < expectedB0.length; i++) {
            if (!b0Hit[i]) {
                b0Errors.add("expected move not hit: '" + expectedB0[i] + "' (index " + i + ")");
            }
        }
        if (b0Errors.isEmpty()) {
            System.out.println("testing san 1 ... pass");
        } else {
            System.out.println("testing san 1 ... FAIL");
            for (String err : b0Errors) {
                System.out.println("  " + err);
            }
            allPassed = false;
        }

        String[] expectedB1 = {
            "a4", "a3", "b4", "b3", "d4", "d3", "e4", "e3", "f4", "f3",
            "g4", "g3", "Na3", "Nf3", "Nh3", "R3c4", "Rd3", "Re3", "Rf3", "Rg3",
            "Rh3", "Rb3", "Ra3", "Rc6", "Rxc7", "R5c4", "Rd5", "Re5", "Rf5", "Rg5",
            "Rh5", "Rb5", "Ra5"
        };
        Board b1 = new Board("rnbqkbnr/pppppppp/8/2R5/8/2R5/PPPPPPP1/1NBQKBN1 w - - 0 1");
        ArrayList<Move> b1Legals = b1.legalMoves();
        ArrayList<String> b1Errors = new ArrayList<>();
        boolean[] b1Hit = new boolean[expectedB1.length];
        if (b1Legals.size() != expectedB1.length) {
            b1Errors.add("expected " + expectedB1.length + " legal moves, but got " + b1Legals.size());
        }
        for (int i = 0; i < b1Legals.size(); i++) {
            Move mi = b1Legals.get(i);
            String san = b1.san(mi);
            if (i < expectedB1.length) {
                if (san.equals(expectedB1[i])) {
                    b1Hit[i] = true;
                } else {
                    b1Errors.add("mismatch at index " + i + ": expected '" + expectedB1[i] + "', got '" + san + "'");
                    for (int j = 0; j < expectedB1.length; j++) {
                        if (!b1Hit[j] && san.equals(expectedB1[j])) {
                            b1Hit[j] = true;
                            break;
                        }
                    }
                }
            } else {
                b1Errors.add("unexpected extra move at index " + i + ": '" + san + "'");
            }
        }
        for (int i = 0; i < expectedB1.length; i++) {
            if (!b1Hit[i]) {
                b1Errors.add("expected move not hit: '" + expectedB1[i] + "' (index " + i + ")");
            }
        }
        if (b1Errors.isEmpty()) {
            System.out.println("testing san 2 ... pass");
        } else {
            System.out.println("testing san 2 ... FAIL");
            for (String err : b1Errors) {
                System.out.println("  " + err);
            }
            allPassed = false;
        }

        if (!allPassed) {
            throw new RuntimeException("runSanTest failed");
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
        boolean[] expected = { false, false, true, true, true };
        String[] bitNames = { "knight", "bishop", "rook", "queen", "king" };

        ArrayList<String> errors = new ArrayList<>();
        for (int k = 0; k < expected.length; k++) {
            boolean actual = b_temp.isKthBitSet(attackIdx1, k);
            if (actual == expected[k]) {
                System.out.println("testing bit " + k + " (" + bitNames[k] + ") ... pass");
            } else {
                System.out.println("testing bit " + k + " (" + bitNames[k] + ") ... FAIL");
                errors.add("bit " + k + " (" + bitNames[k] + "): expected " + expected[k] + ", got " + actual);
            }
        }

        if (!errors.isEmpty()) {
            System.out.println("FAIL");
            for (String err : errors) {
                System.out.println("  " + err);
            }
            throw new RuntimeException("runBitSetTest failed");
        }
    }

    public static PgnParsedContent extractPgnContent(String pgn) {
        PgnParsedContent parsed = new PgnParsedContent();
        if (pgn == null) {
            parsed.rawLines = new String[0];
            return parsed;
        }

        parsed.rawLines = pgn.split("\\r?\\n", -1);
        boolean inComment = false;

        for (int lineIdx = 0; lineIdx < parsed.rawLines.length; lineIdx++) {
            int lineNum = lineIdx + 1;
            String rawLine = parsed.rawLines[lineIdx];
            String line = rawLine.trim();

            if (line.isEmpty() || line.startsWith("%")) {
                continue;
            }

            if (!inComment && line.startsWith("[")) {
                int startTagIdx = 0;
                while (startTagIdx < line.length() && line.charAt(startTagIdx) == '[') {
                    int endIdx = line.indexOf(']', startTagIdx);
                    if (endIdx != -1) {
                        String tag = line.substring(startTagIdx, endIdx + 1).trim();
                        parsed.tags.add(new PgnTagInfo(tag, lineNum, rawLine));
                        startTagIdx = endIdx + 1;
                        while (startTagIdx < line.length() && Character.isWhitespace(line.charAt(startTagIdx))) {
                            startTagIdx++;
                        }
                    } else {
                        break;
                    }
                }

                if (startTagIdx < line.length()) {
                    String remainder = line.substring(startTagIdx);
                    for (int i = 0; i < remainder.length(); i++) {
                        char c = remainder.charAt(i);
                        if (c == '{') inComment = true;
                        if (!Character.isWhitespace(c)) {
                            parsed.normMoves.append(c);
                            parsed.normMoveLineNumbers.add(lineNum);
                        }
                        if (c == '}') inComment = false;
                    }
                }
                continue;
            }

            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '{') inComment = true;
                if (!Character.isWhitespace(c)) {
                    parsed.normMoves.append(c);
                    parsed.normMoveLineNumbers.add(lineNum);
                }
                if (c == '}') inComment = false;
            }
        }

        return parsed;
    }

    public boolean comparePgnFiles(String expectedPath, String actualPath) {
        try {
            String expected = Files.readString(Path.of(expectedPath), StandardCharsets.UTF_8);
            String actual = Files.readString(Path.of(actualPath), StandardCharsets.UTF_8);
            return comparePgnStrings(expected, actual);
        } catch (IOException e) {
            System.out.println("FAIL: IOException comparing PGN files: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean comparePgnStrings(String pgn1, String pgn2) {
        if (pgn1 == null && pgn2 == null) {
            return true;
        }
        if (pgn1 == null || pgn2 == null) {
            System.out.println("FAIL: one of the PGN strings is null");
            System.out.println("PGN 1:\n" + pgn1);
            System.out.println("PGN 2:\n" + pgn2);
            return false;
        }

        PgnParsedContent content1 = extractPgnContent(pgn1);
        PgnParsedContent content2 = extractPgnContent(pgn2);

        // 1. Compare tags independent of order
        ArrayList<PgnTagInfo> missingIn2 = new ArrayList<>();
        for (PgnTagInfo t1 : content1.tags) {
            boolean found = false;
            for (PgnTagInfo t2 : content2.tags) {
                if (t1.normTag.equals(t2.normTag)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                missingIn2.add(t1);
            }
        }

        ArrayList<PgnTagInfo> missingIn1 = new ArrayList<>();
        for (PgnTagInfo t2 : content2.tags) {
            boolean found = false;
            for (PgnTagInfo t1 : content1.tags) {
                if (t2.normTag.equals(t1.normTag)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                missingIn1.add(t2);
            }
        }

        if (!missingIn2.isEmpty() || !missingIn1.isEmpty() || content1.tags.size() != content2.tags.size()) {
            System.out.println("FAIL: PGN tags do not match (ignoring order and whitespace)");
            System.out.println("  Source tags count:     " + content1.tags.size());
            System.out.println("  Comparison tags count: " + content2.tags.size());
            if (!missingIn2.isEmpty()) {
                System.out.println("  Tags in source but missing in comparison (" + missingIn2.size() + "):");
                for (int i = 0; i < Math.min(missingIn2.size(), 10); i++) {
                    PgnTagInfo ti = missingIn2.get(i);
                    System.out.println("    - Line " + ti.lineNumber + ": " + ti.rawTag);
                    System.out.println("      Source line text: \"" + ti.lineContent.trim() + "\"");
                }
                if (missingIn2.size() > 10) {
                    System.out.println("    ... and " + (missingIn2.size() - 10) + " more");
                }
            }
            if (!missingIn1.isEmpty()) {
                System.out.println("  Tags in comparison but missing in source (" + missingIn1.size() + "):");
                for (int i = 0; i < Math.min(missingIn1.size(), 10); i++) {
                    PgnTagInfo ti = missingIn1.get(i);
                    System.out.println("    - Line " + ti.lineNumber + ": " + ti.rawTag);
                    System.out.println("      Comparison line text: \"" + ti.lineContent.trim() + "\"");
                }
                if (missingIn1.size() > 10) {
                    System.out.println("    ... and " + (missingIn1.size() - 10) + " more");
                }
            }
            return false;
        }

        // 2. Compare move parts (modulo whitespace and newlines)
        String m1 = content1.normMoves.toString();
        String m2 = content2.normMoves.toString();

        if (m1.equals(m2)) {
            return true;
        } else {
            System.out.println("FAIL: PGN move parts do not match (ignoring whitespace/line-breaks)");
            System.out.println("  Source move length (normalized):     " + m1.length());
            System.out.println("  Comparison move length (normalized): " + m2.length());

            int minLen = Math.min(m1.length(), m2.length());
            int diffIdx = -1;
            for (int i = 0; i < minLen; i++) {
                if (m1.charAt(i) != m2.charAt(i)) {
                    diffIdx = i;
                    break;
                }
            }
            if (diffIdx == -1) {
                diffIdx = minLen;
            }

            System.out.println("  First mismatch at normalized character index: " + diffIdx);

            if (diffIdx < content1.normMoveLineNumbers.size()) {
                int srcLine = content1.normMoveLineNumbers.get(diffIdx);
                System.out.println("  Source affected line: " + srcLine);
                if (srcLine >= 1 && srcLine <= content1.rawLines.length) {
                    System.out.println("  Source line text:     \"" + content1.rawLines[srcLine - 1].trim() + "\"");
                }
            } else if (!content1.normMoveLineNumbers.isEmpty()) {
                int lastLine = content1.normMoveLineNumbers.get(content1.normMoveLineNumbers.size() - 1);
                System.out.println("  Source ended at line: " + lastLine);
            }

            if (diffIdx < content2.normMoveLineNumbers.size()) {
                int compLine = content2.normMoveLineNumbers.get(diffIdx);
                System.out.println("  Comparison affected line: " + compLine);
                if (compLine >= 1 && compLine <= content2.rawLines.length) {
                    System.out.println("  Comparison line text:     \"" + content2.rawLines[compLine - 1].trim() + "\"");
                }
            } else if (!content2.normMoveLineNumbers.isEmpty()) {
                int lastLine = content2.normMoveLineNumbers.get(content2.normMoveLineNumbers.size() - 1);
                System.out.println("  Comparison ended at line: " + lastLine);
            }

            int start = Math.max(0, diffIdx - 40);
            int end1 = Math.min(m1.length(), diffIdx + 40);
            int end2 = Math.min(m2.length(), diffIdx + 40);

            if (diffIdx < m1.length()) {
                System.out.println("  Source text near mismatch:     ... " + m1.substring(start, end1) + " ...");
            }
            if (diffIdx < m2.length()) {
                System.out.println("  Comparison text near mismatch: ... " + m2.substring(start, end2) + " ...");
            }

            if (pgn1.length() < 2000 && pgn2.length() < 2000) {
                System.out.println("Expected:\n" + pgn1);
                System.out.println("Generated:\n" + pgn2);
            }
            return false;
        }
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
        String generatedPgn = printer.printGame(g);
        String expectedPgn = "[Event \"Knaurs Schachbuch\"]\n" +
                "[Site \"Paris\"]\n" +
                "[Date \"1859.??.??\"]\n" +
                "[Round \"1\"]\n" +
                "[White \"Morphy\"]\n" +
                "[Black \"NN\"]\n" +
                "[Result \"1-0\"]\n" +
                "[ECO \"C56\"]\n" +
                "\n" +
                "1. e4 e5 2. Nf3 Nc6 3. Bc4 Nf6 4. d4 exd4 5. O-O Nxe4 * ";

        if (comparePgnStrings(expectedPgn, generatedPgn)) {
            System.out.println("testing simple pgn print ... pass");
        } else {
            System.out.println("testing simple pgn print ... FAIL");
            throw new RuntimeException("runPgnPrintTest failed");
        }
    }

    public void pgnScanTest() {

        System.out.println("TEST: scanning PGN for game offsets");
        String pgnFile = getPgnPath("test_pgn_03.pgn");
        PgnChessDatabase db = new PgnChessDatabase();

        String[] expectedLines = {
            "[Event \"Barbera Open\"]",
            "[Event \"Hastings\"]",
            "[Event \"Reykjavik Open\"]",
            "[Event \"Barbera Open\"]",
            "[Event \"London Chess Classic\"]",
            "[Event \"Wijk aan Zee\"]",
            "[Event \"Barbera Open\"]",
            "[Event \"Baden-Baden\"]",
            "[Event \"Dortmund\"]",
            "[Event \"St. Petersburg\"]"
        };

        ArrayList<String> errors = new ArrayList<>();
        try {
            db.open(pgnFile);
            db.scanGames();
            ArrayList<GameInfo> entries = db.getIndex();

            if (entries.size() < expectedLines.length) {
                errors.add("expected at least " + expectedLines.length + " entries, but got " + entries.size());
            }

            int checkCount = Math.min(expectedLines.length, entries.size());
            for (int i = 0; i < checkCount; i++) {
                PgnGameInfo pgnInfo = (PgnGameInfo) entries.get(i);
                long offset_i = pgnInfo.getOffset();
                RandomAccessFile raf = null;
                try {
                    raf = new RandomAccessFile(pgnFile, "r");
                    raf.seek(offset_i);
                    String line = raf.readLine();
                    if (expectedLines[i].equals(line)) {
                        System.out.println("testing offset " + (i + 1) + " ... pass");
                    } else {
                        System.out.println("testing offset " + (i + 1) + " ... FAIL");
                        errors.add("mismatch at index " + i + ": expected '" + expectedLines[i] + "', got '" + line + "'");
                    }
                } catch (IOException e) {
                    System.out.println("testing offset " + (i + 1) + " ... FAIL");
                    errors.add("IOException reading offset " + i + " (" + offset_i + "): " + e.getMessage());
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
        } catch (IOException e) {
            errors.add("IOException opening database: " + e.getMessage());
            e.printStackTrace();
        }

        if (!errors.isEmpty()) {
            System.out.println("FAIL");
            for (String err : errors) {
                System.out.println("  " + err);
            }
            throw new RuntimeException("pgnScanTest failed");
        }
    }


    public void pgnReadGameTest() {

        System.out.println("TEST: reading single PGN game");
        String pgnFile = getPgnPath("test_pgn_01.pgn");
        String expectedPgn = "[Event \"Paris Opera\"]\n" +
                "[Site \"Paris FRA\"]\n" +
                "[Date \"1858.10.21\"]\n" +
                "[Round \"1\"]\n" +
                "[White \"Morphy, Paul\"]\n" +
                "[Black \"Duke of Brunswick and Count Isouard\"]\n" +
                "[Result \"1-0\"]\n" +
                "[ECO \"C41\"]\n\n" +
                "1. e4 e5 2. Nf3 d6 3. d4 Bg4 4. dxe5 Bxf3 5. Qxf3 dxe5 6. Bc4 Nf6 7. Qb3 Qe7 8. " +
                "Nc3 c6 9. Bg5 b5 10. Nxb5 cxb5 11. Bxb5+ Nbd7 12. O-O-O Rd8 13. Rxd7 Rxd7 14. " +
                "Rd1 Qe6 15. Bxd7+ Nxd7 16. Qb8+ Nxb8 17. Rd8# 1-0";

        PgnChessDatabase db = new PgnChessDatabase();
        PgnPrinter printer = new PgnPrinter();
        boolean passed = false;
        try {
            db.open(pgnFile);
            db.scanGames();
            Game g = db.loadGame(0);
            String pgn = printer.printGame(g);
            if (comparePgnStrings(expectedPgn, pgn)) {
                System.out.println("testing read single pgn game ... pass");
                passed = true;
            } else {
                System.out.println("testing read single pgn game ... FAIL");
            }
        } catch (IOException e) {
            System.out.println("testing read single pgn game ... FAIL");
            System.out.println("FAIL: IOException while reading PGN file: " + e.getMessage());
            e.printStackTrace();
        }

        if (!passed) {
            throw new RuntimeException("pgnReadGameTest failed");
        }
    }

    public void pgnReadMiddleGTest() {

        System.out.println("TEST: reading all games from test_pgn_02.pgn");
        String middleg = getPgnPath("test_pgn_02.pgn");

        PgnChessDatabase db = new PgnChessDatabase();
        boolean allPassed = true;
        try {
            db.open(middleg);
            db.scanGames();
            ArrayList<GameInfo> index = db.getIndex();
            for (int i = 0; i < index.size(); i++) {
                Game g = db.loadGame(index.get(i));
                if (g != null && g.getRootNode() != null) {
                    System.out.println("testing read game " + (i + 1) + " ... pass");
                } else {
                    System.out.println("testing read game " + (i + 1) + " ... FAIL");
                    allPassed = false;
                }
            }
        } catch (IOException e) {
            System.out.println("FAIL: IOException while reading " + middleg + ": " + e.getMessage());
            e.printStackTrace();
            allPassed = false;
        }

        if (!allPassed) {
            throw new RuntimeException("pgnReadMiddleGTest failed");
        }
    }

    public void pgnReadAllMillBaseTest() {

        System.out.println("TEST: reading all games from test_pgn_03.pgn");
        String millbase = getPgnPath("test_pgn_03.pgn");
        PgnChessDatabase db = new PgnChessDatabase();

        boolean allPassed = true;
        try {
            db.open(millbase);
            db.scanGames();
            ArrayList<GameInfo> index = db.getIndex();
            if (index.size() == 12) {
                System.out.println("testing scan offsets count (12) ... pass");
            } else {
                System.out.println("testing scan offsets count ... FAIL (expected 12, got " + index.size() + ")");
                throw new RuntimeException("pgnReadAllMillBaseTest scan failed");
            }

            for (int i = 0; i < index.size(); i++) {
                Game g = db.loadGame(index.get(i));
                if (g == null || g.getRootNode() == null) {
                    allPassed = false;
                    System.out.println("testing read game " + (i + 1) + " ... FAIL");
                }
            }
            if (allPassed) {
                System.out.println("testing read all " + index.size() + " games ... pass");
            }
        } catch (IOException e) {
            System.out.println("FAIL: IOException while reading " + millbase + ": " + e.getMessage());
            e.printStackTrace();
            allPassed = false;
        }

        if (!allPassed) {
            throw new RuntimeException("pgnReadAllMillBaseTest failed");
        }
    }

    // using PgnChessDatabase header index verification
    public void pgnReadSingleEntryTestOpenClose() {

        System.out.println("TEST: scanning PGN database, reading headers and verifying search");
        String millbase = getPgnPath("test_pgn_03.pgn");
        PgnChessDatabase db = new PgnChessDatabase();

        try {
            db.open(millbase);
            db.scanGames();

            int matchCount = 0;
            for (GameInfo info : db.getIndex()) {
                if ("Barbera Open".equals(info.getEvent())) {
                    matchCount += 1;
                }
            }

            if (matchCount == 3) {
                System.out.println("testing matching 'Barbera Open' headers (3) ... pass");
            } else {
                System.out.println("testing matching 'Barbera Open' headers ... FAIL (expected 3, got " + matchCount + ")");
                throw new RuntimeException("pgnReadSingleEntryTestOpenClose failed");
            }
        } catch (IOException e) {
            System.out.println("FAIL: IOException: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("pgnReadSingleEntryTestOpenClose failed");
        }
    }

    // using PgnChessDatabase pattern search and reset
    public void pgnReadSingleEntryTestSeekWithinRAF() {

        System.out.println("TEST: database search by pattern on PgnChessDatabase");
        String millbase = getPgnPath("test_pgn_03.pgn");
        PgnChessDatabase db = new PgnChessDatabase();

        try {
            db.open(millbase);
            db.scanGames();

            SearchPattern pattern = new SearchPattern();
            pattern.setEvent("Barbera Open");
            db.search(pattern);

            ArrayList<GameInfo> results = db.getSearchResults();
            if (results.size() == 3 && db.isSearchActive()) {
                System.out.println("testing matching 'Barbera Open' search results (3) ... pass");
            } else {
                System.out.println("testing matching 'Barbera Open' search results ... FAIL (expected 3, got " + results.size() + ")");
                throw new RuntimeException("pgnReadSingleEntryTestSeekWithinRAF failed");
            }

            db.resetSearch();
            if (!db.isSearchActive() && db.getSearchResults().isEmpty()) {
                System.out.println("testing reset search ... pass");
            } else {
                System.out.println("testing reset search ... FAIL");
                throw new RuntimeException("pgnReadSingleEntryTestSeekWithinRAF resetSearch failed");
            }
        } catch (IOException e) {
            System.out.println("FAIL: IOException: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("pgnReadSingleEntryTestSeekWithinRAF failed");
        }
    }

    public void readGamesByStringTest() {

        System.out.println("TEST: reading PGN game from string");
        String s = "[Event \"Berlin\"]\n" +
                "[Site \"Berlin GER\"]\n" +
                "[Date \"1852.??.??\"]\n" +
                "[Round \"?\"]\n" +
                "[White \"Adolf Anderssen\"]\n" +
                "[Black \"Jean Dufresne\"]\n" +
                "[Result \"1-0\"]\n" +
                "[ECO \"C52\"]\n" +
                "[BlackElo \"?\"]\n" +
                "[EventDate \"?\"]\n" +
                "[WhiteElo \"?\"]\n" +
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
        String pgnOut = printer.printGame(g);
        if (comparePgnStrings(s, pgnOut)) {
            System.out.println("testing read pgn from string ... pass");
        } else {
            System.out.println("testing read pgn from string ... FAIL");
            throw new RuntimeException("readGamesByStringTest failed");
        }

    }

    public void runPosHashTest() {

        System.out.println("TEST: reading single PGN game, trying to find starting pos after 1d4 by pos hash");
        Board b1 = new Board("rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq d3 0 1");
        Board b2 = new Board("rnbqkbnr/ppppppp1/8/7p/3P4/8/PPP1PPPP/RNBQKBNR w KQkq h6 0 2");
        long key1 = b1.getPositionHash();
        long key2 = b2.getPositionHash();

        final boolean expectedResult1 = true;
        final boolean expectedResult2 = false;

        String kingbase = getPgnPath("test_pgn_04.pgn");
        PgnChessDatabase db = new PgnChessDatabase();
        boolean passed = false;
        ArrayList<String> errors = new ArrayList<>();

        try {
            db.open(kingbase);
            db.scanGames();
            Game g = db.loadGame(0);
            boolean actualResult1 = g.containsPosition(key1, 0, 100);
            boolean actualResult2 = g.containsPosition(key2, 0, 100);

            if (actualResult1 == expectedResult1) {
                System.out.println("testing pos hash key 1 (1.d4) ... pass");
            } else {
                System.out.println("testing pos hash key 1 (1.d4) ... FAIL");
                errors.add("key1 search: expected " + expectedResult1 + ", got " + actualResult1 + " (key=" + key1 + ")");
            }

            if (actualResult2 == expectedResult2) {
                System.out.println("testing pos hash key 2 (non-matching) ... pass");
            } else {
                System.out.println("testing pos hash key 2 (non-matching) ... FAIL");
                errors.add("key2 search: expected " + expectedResult2 + ", got " + actualResult2 + " (key=" + key2 + ")");
            }

            if (errors.isEmpty()) {
                passed = true;
            }
        } catch (IOException e) {
            errors.add("IOException: " + e.getMessage());
            e.printStackTrace();
        }

        if (!passed) {
            System.out.println("FAIL");
            for (String err : errors) {
                System.out.println("  " + err);
            }
            throw new RuntimeException("runPosHashTest failed");
        }
    }


    public void runZobristTest() {

        System.out.println("TEST: zobrist hashing");

        String[][] testCases = {
            {"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", "463b96181691fc9c"},
            {"rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1", "823c9b50fd114196"},
            {"rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2", "756b94461c50fb0"},
            {"rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR b KQkq - 0 2", "662fafb965db29d4"},
            {"rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3", "22a48b5a8e47ff78"},
            {"rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPPKPPP/RNBQ1BNR b kq - 0 3", "652a607ca3f242c1"},
            {"rnbq1bnr/ppp1pkpp/8/3pPp2/8/8/PPPPKPPP/RNBQ1BNR w - - 0 4", "fdd303c946bdd9"},
            {"rnbqkbnr/p1pppppp/8/8/PpP4P/8/1P1PPPP1/RNBQKBNR b KQkq c3 0 3", "3c8123ea7b067637"},
            {"rnbqkbnr/p1pppppp/8/8/P6P/R1p5/1P1PPPP1/1NBQKBNR b Kkq - 0 4", "5c3f9b829b279560"}
        };

        ArrayList<String> errors = new ArrayList<>();
        for (int i = 0; i < testCases.length; i++) {
            String fen = testCases[i][0];
            String expectedHex = testCases[i][1];
            long expectedKey = Long.parseUnsignedLong(expectedHex, 16);

            Board b = new Board(fen);
            long actualKey = b.getZobrist();

            if (actualKey == expectedKey) {
                System.out.println("testing zobrist " + (i + 1) + " ... pass");
            } else {
                System.out.println("testing zobrist " + (i + 1) + " ... FAIL");
                errors.add("case " + (i + 1) + " (" + fen + "): expected " + expectedHex + " (" + expectedKey + "), got " + Long.toHexString(actualKey) + " (" + actualKey + ")");
            }
        }

        if (!errors.isEmpty()) {
            System.out.println("FAIL");
            for (String err : errors) {
                System.out.println("  " + err);
            }
            throw new RuntimeException("runZobristTest failed");
        }
    }

    public void pgnMoveAmbiguityUTFTest() {

        System.out.println("TEST: PGN stress test (unicode, comments, disambiguations)");
        String pgnFile = getPgnPath("test_pgn_05.pgn");

        String expectedGame1 = "[Event \"チェス世界選手権 1997 ★ Deep Blue vs Kasparov\"]\n" +
                "[Site \"Köln & München, Deutschland Übersee-Halle № 42\"]\n" +
                "[Date \"1997.05.11\"]\n" +
                "[Round \"6\"]\n" +
                "[White \"Deep Blue (IBM 计算机)\"]\n" +
                "[Black \"Каспаров, Гарри Кимович\"]\n" +
                "[Result \"1-0\"]\n" +
                "[ECO \"B17\"]\n\n" +
                "1. e4 c6 2. d4 d5 3. Nc3 dxe4 4. Nxe4 Nd7 { 【重要局】Каспаров выбирает надёжную защиту Каро-Канн. } " +
                "5. Ng5 Ngf6 { Ein scharfer Eröffnungszweig mit typischer Figurenentwicklung! } " +
                "6. Bd3 e6 7. N1f3 { ★ N1f3! Der Springer springt von g1 nach f3: Wunderschöne Springer-Disambiguierung! } " +
                "h6 8. Nxe6 { ⚡ Жертва коня! Ein spektakuläres Figurenopfer erschüttert die schwarze Königsstellung. } " +
                "Qe7 9. O-O fxe6 10. Bg6+ Kd8 11. Bf4 b5 12. a4 Bb7 13. Re1 Nd5 14. Bg3 Kc8 15. axb5 cxb5 16. Qd3 Bc6 " +
                "17. Bf5 exf5 18. Rxe7 Bxe7 19. c4 { 【終局】Белые побеждают в матче! Großartiger Sieg für den Schachcomputer. 1-0 } 1-0";

        String expectedGame2 = "[Event \"Матч за звание чемпиона мира по шахматам 1972 ⚡\"]\n" +
                "[Site \"Рейкьявик, Исландия — Laugardalshöll № 6\"]\n" +
                "[Date \"1972.07.23\"]\n" +
                "[Round \"6\"]\n" +
                "[White \"Фишер, Роберт Джеймс\"]\n" +
                "[Black \"Спасский, Борис Васильевич\"]\n" +
                "[Result \"1-0\"]\n" +
                "[ECO \"D59\"]\n\n" +
                "1. c4 e6 2. Nf3 d5 3. d4 Nf6 4. Nc3 Be7 5. Bg5 O-O 6. e3 h6 7. Bh4 b6 8. cxd5 Nxd5 9. Bxe7 Qxe7 " +
                "10. Nxd5 exd5 11. Rc1 Be6 12. Qa4 c5 13. Qa3 Rc8 14. Bb5 a6 15. dxc5 bxc5 16. O-O Ra7 17. Be2 Nd7 " +
                "18. Nd4 Qf8 19. Nxe6 fxe6 20. e4 d4 21. f4 Qe7 22. e5 Rb8 23. Bc4 Kh8 24. Qh3 Nf8 25. b3 a5 26. f5 exf5 " +
                "27. Rxf5 Nh7 28. Rcf1 { ⚡ Doppelung auf der f-Linie: Rcf1! } Qd8 29. Qg3 Re7 30. h4 Rbb7 " +
                "{ ♜ Ладья b8 переходит на b7 для защиты: Rbb7. } 31. e6 Rbc7 32. Qe5 Qe8 33. a4 Qd8 34. R1f2 " +
                "{ ★ Turmmanöver auf der ersten Reihe: R1f2! } Qe8 35. R2f3 " +
                "{ ➜ Und weiter: R2f3! Weiße Schwerfiguren dominieren das Brett. } Qd8 36. Bd3 Qe8 37. Qe4 Nf6 " +
                "38. Rxf6 gxf6 39. Rxf6 Kg8 40. Bc4 Kh8 41. Qf4 " +
                "{ 【名局】Победа белых! Boris Spassky klatschte Beifall für diesen Meisterstreich. 1-0 } 1-0";

        String[] expectedGames = { expectedGame1, expectedGame2 };

        PgnChessDatabase db = new PgnChessDatabase();
        PgnPrinter printer = new PgnPrinter();
        boolean allPassed = true;
        try {
            db.open(pgnFile);
            db.scanGames();
            ArrayList<GameInfo> index = db.getIndex();

            if (index.size() != expectedGames.length) {
                System.out.println("testing scan offsets count ... FAIL (expected " + expectedGames.length + ", got " + index.size() + ")");
                throw new RuntimeException("pgnStressTest scan failed");
            }

            for (int i = 0; i < index.size(); i++) {
                Game g = db.loadGame(index.get(i));
                String printed = printer.printGame(g);
                if (comparePgnStrings(expectedGames[i], printed)) {
                    System.out.println("testing stress game " + (i + 1) + " ... pass");
                } else {
                    System.out.println("testing stress game " + (i + 1) + " ... FAIL");
                    allPassed = false;
                }
            }
        } catch (IOException e) {
            System.out.println("FAIL: IOException while reading " + pgnFile + ": " + e.getMessage());
            e.printStackTrace();
            allPassed = false;
        }

        if (!allPassed) {
            throw new RuntimeException("pgnStressTest failed");
        }
    }

    public void pgnMiddleGReadWriteCompareTest() {

        System.out.println("TEST: scanning test_pgn_06.pgn, writing to temp.pgn via PgnChessDatabase and comparing");
        String pgnFile = getPgnPath("test_pgn_06.pgn");

        File tempFile = new File("temp.pgn");
        tempFile.deleteOnExit();

        boolean readWriteSuccess = true;

        try {
            PgnChessDatabase srcDb = new PgnChessDatabase();
            srcDb.open(pgnFile);
            srcDb.scanGames();

            PgnChessDatabase destDb = new PgnChessDatabase();
            destDb.createNew(tempFile.getAbsolutePath());

            for (GameInfo info : srcDb.getIndex()) {
                Game g = srcDb.loadGame(info);
                destDb.appendGame(g);
            }
        } catch (IOException e) {
            System.out.println("FAIL: IOException during read/write: " + e.getMessage());
            e.printStackTrace();
            readWriteSuccess = false;
        }

        if (!readWriteSuccess) {
            throw new RuntimeException("pgnMiddleGReadWriteCompareTest read/write failed");
        }

        if (comparePgnFiles(pgnFile, tempFile.getAbsolutePath())) {
            System.out.println("testing test_pgn_06.pgn read, write and compare ... pass");
        } else {
            System.out.println("testing test_pgn_06.pgn read, write and compare ... FAIL");
            throw new RuntimeException("pgnMiddleGReadWriteCompareTest comparison failed");
        }
    }

    public void pgnGameInfoSurnameExtractionTest() {
        System.out.println("TEST: GameInfo surname extraction and versus title formatting");

        if (!"Morphy".equals(GameInfo.extractSurname("Morphy, Paul"))) {
            throw new RuntimeException("Failed surname extraction for 'Morphy, Paul'");
        }
        if (!"Morphy".equals(GameInfo.extractSurname("Paul Morphy"))) {
            throw new RuntimeException("Failed surname extraction for 'Paul Morphy'");
        }
        if (!"Karpov".equals(GameInfo.extractSurname("Karpov, Anatoly"))) {
            throw new RuntimeException("Failed surname extraction for 'Karpov, Anatoly'");
        }
        if (!"Stockfish 16".equals(GameInfo.extractSurname("Stockfish 16"))) {
            throw new RuntimeException("Failed surname extraction for 'Stockfish 16'");
        }
        if (!"N.N.".equals(GameInfo.extractSurname("?"))) {
            throw new RuntimeException("Failed surname extraction for '?'");
        }
        if (!"N.N.".equals(GameInfo.extractSurname(null))) {
            throw new RuntimeException("Failed surname extraction for null");
        }
        if (!"N.N.".equals(GameInfo.extractSurname("N.N."))) {
            throw new RuntimeException("Failed surname extraction for 'N.N.'");
        }

        if (!"Morphy vs. Brunswick".equals(GameInfo.formatVersusTitle("Morphy, Paul", "Duke of Brunswick"))) {
            throw new RuntimeException("Failed formatVersusTitle");
        }

        GameInfo info = new GameInfo();
        info.setWhite("Kasparov, Garry");
        info.setBlack("Karpov, Anatoly");
        if (!"Kasparov".equals(info.getWhiteSurname())) {
            throw new RuntimeException("Failed info.getWhiteSurname()");
        }
        if (!"Karpov".equals(info.getBlackSurname())) {
            throw new RuntimeException("Failed info.getBlackSurname()");
        }
        if (!"Kasparov vs. Karpov".equals(info.getVersusTitle())) {
            throw new RuntimeException("Failed info.getVersusTitle()");
        }

        System.out.println("testing surname extraction ... pass");
    }

    public void chessDatabaseSessionSynchronizationTest() {
        System.out.println("TEST: PgnChessDatabase lifecycle, listeners, append, replace and delete");

        File tempDbFile;
        try {
            tempDbFile = File.createTempFile("test_sync_", ".pgn");
            tempDbFile.deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        PgnChessDatabase db = new PgnChessDatabase();
        ArrayList<ChessDatabaseEvent> events = new ArrayList<>();
        ChessDatabaseListener listener = events::add;
        db.addListener(listener);

        try {
            db.createNew(tempDbFile.getAbsolutePath());
            if (!db.isOpen()) {
                throw new RuntimeException("Database should be open after createNew");
            }
            if (events.isEmpty() || events.get(events.size() - 1).getType() != ChessDatabaseEvent.Type.DATABASE_OPENED) {
                throw new RuntimeException("Expected DATABASE_OPENED event");
            }

            // Append game 1
            Game g1 = new Game();
            g1.setHeader("Event", "Sync Event 1");
            g1.setHeader("White", "Player 1");
            g1.setHeader("Black", "Player 2");
            g1.setHeader("Result", "1-0");
            g1.getRootNode().setBoard(new Board(true));
            g1.applyMove(new Move("e2e4"));

            GameInfo info1 = db.appendGame(g1);
            if (db.getIndex().size() != 1) {
                throw new RuntimeException("Expected 1 game after append");
            }
            if (events.get(events.size() - 1).getType() != ChessDatabaseEvent.Type.GAME_APPENDED) {
                throw new RuntimeException("Expected GAME_APPENDED event");
            }

            // Append game 2
            Game g2 = new Game();
            g2.setHeader("Event", "Sync Event 2");
            g2.setHeader("White", "Player 3");
            g2.setHeader("Black", "Player 4");
            g2.setHeader("Result", "0-1");
            g2.getRootNode().setBoard(new Board(true));
            g2.applyMove(new Move("d2d4"));

            GameInfo info2 = db.appendGame(g2);
            if (db.getIndex().size() != 2) {
                throw new RuntimeException("Expected 2 games after second append");
            }

            // Replace game 1
            Game g1Updated = new Game();
            g1Updated.setHeader("Event", "Sync Event 1 Updated");
            g1Updated.setHeader("White", "Player 1");
            g1Updated.setHeader("Black", "Player 2");
            g1Updated.setHeader("Result", "1/2-1/2");
            g1Updated.getRootNode().setBoard(new Board(true));
            g1Updated.applyMove(new Move("e2e4"));
            g1Updated.applyMove(new Move("e7e5"));

            db.replaceGame(g1Updated, info1);
            if (events.get(events.size() - 1).getType() != ChessDatabaseEvent.Type.GAME_REPLACED) {
                throw new RuntimeException("Expected GAME_REPLACED event");
            }
            Game loadedUpdated = db.loadGame(0);
            if (!"Sync Event 1 Updated".equals(loadedUpdated.getHeader("Event"))) {
                throw new RuntimeException("Replaced game does not reflect updated header");
            }

            // Delete game 2
            GameInfo currentInfo2 = db.getIndex().get(1);
            db.deleteGame(currentInfo2);
            if (events.get(events.size() - 1).getType() != ChessDatabaseEvent.Type.GAME_DELETED) {
                throw new RuntimeException("Expected GAME_DELETED event");
            }
            if (db.getIndex().size() != 1) {
                throw new RuntimeException("Expected 1 game after delete");
            }

            db.removeListener(listener);
            db.close();
            if (db.isOpen()) {
                throw new RuntimeException("Database should be closed");
            }

            System.out.println("testing chess database session synchronization ... pass");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void pgnDocumentSessionSynchronizationTest() {
        System.out.println("TEST: PgnGameInfo modifiedGame caching and synchronization");

        PgnGameInfo pgnInfo = new PgnGameInfo();
        if (pgnInfo.isModified()) {
            throw new RuntimeException("New PgnGameInfo should not be modified");
        }
        if (pgnInfo.getModifiedGame() != null) {
            throw new RuntimeException("New PgnGameInfo should not have modifiedGame");
        }

        Game g = new Game();
        g.setHeader("Event", "Buffer Test");
        pgnInfo.setModifiedGame(g);
        pgnInfo.setModified(true);

        if (!pgnInfo.isModified() || pgnInfo.getModifiedGame() != g) {
            throw new RuntimeException("Modified game buffer not retained");
        }

        pgnInfo.setModified(false);
        pgnInfo.setModifiedGame(null);
        if (pgnInfo.isModified() || pgnInfo.getModifiedGame() != null) {
            throw new RuntimeException("Clearing modified status failed");
        }

        System.out.println("testing pgn document session synchronization ... pass");
    }

    public void databaseLifecycleAndPersistenceTest() {
        System.out.println("TEST: Database lifecycle, detachment and persistence (PGN and SCID5)");
        Path path = null;
        try {
            path = Files.createTempFile("jfxchess-db-lifecycle-", ".pgn");
            Workspace workspace = new Workspace();
            ChessDatabase database = workspace.getOrCreateDatabase(path);

            Model_JFXChess model = new Model_JFXChess();
            model.setWorkspace(workspace);

            // 1. Initial creation
            database.createNew(path.toString());
            model.setActiveDatabase(database);

            if (!database.isOpen()) {
                throw new AssertionError("Database should be open after createNew");
            }
            if (!database.getIndex().isEmpty()) {
                throw new AssertionError("New database should be empty");
            }

            Game g1 = new Game();
            g1.getRootNode().setBoard(new Board(true));
            g1.setHeader("White", "Alice");
            g1.setHeader("Black", "Bob");
            GameSession s1 = workspace.createSession(g1);

            Game g2 = new Game();
            g2.getRootNode().setBoard(new Board(true));
            g2.setHeader("White", "Charlie");
            g2.setHeader("Black", "Dave");
            GameSession s2 = workspace.createSession(g2);

            // Append g1 to database
            GameInfo info1 = database.appendGame(g1);
            s1.setPgnSourceReference(new PgnSourceReference(path, info1.getId(), database.getRevision()));

            if (s1.getPgnSourceReference() == null) {
                throw new AssertionError("s1 should be attached to database");
            }
            if (s2.getPgnSourceReference() != null) {
                throw new AssertionError("s2 should be detached");
            }

            // Test Close Database detaches all open sessions
            model.closeActiveDatabase();
            if (model.getActiveDatabase() != null) {
                throw new AssertionError("Active database should be null after close");
            }
            if (database.isOpen()) {
                throw new AssertionError("Database should be closed");
            }
            if (s1.getPgnSourceReference() != null || s2.getPgnSourceReference() != null) {
                throw new AssertionError("All sessions must be detached on database close");
            }

            // Test creating a new database detaches all open sessions
            database.open(path.toString());
            database.scanGames();
            model.setActiveDatabase(database);
            s1.setPgnSourceReference(new PgnSourceReference(path, info1.getId(), database.getRevision()));

            Path secondDbPath = Files.createTempFile("jfxchess-db2-", ".pgn");
            ChessDatabase db2 = workspace.getOrCreateDatabase(secondDbPath);
            db2.createNew(secondDbPath.toString());
            model.setActiveDatabase(db2);
            model.detachAllSessions();

            if (s1.getPgnSourceReference() != null || s2.getPgnSourceReference() != null) {
                throw new AssertionError("All sessions must be detached when new database is created");
            }
            Files.deleteIfExists(secondDbPath);

            // Re-open first database and re-attach s1
            database.open(path.toString());
            database.scanGames();
            model.setActiveDatabase(database);
            GameInfo currentInfo1 = database.getIndex().get(0);
            s1.setPgnSourceReference(new PgnSourceReference(database.getPath(), currentInfo1.getId(), database.getRevision()));

            // Test DialogSave button enablement
            DialogSave dlgAttached = new DialogSave(null, true);
            JButton btnReplaceAttached = (JButton) dlgAttached.getContentPane().getComponent(0);
            if (!btnReplaceAttached.isEnabled()) {
                throw new AssertionError("Save (Replace) must be enabled when attached");
            }
            dlgAttached.dispose();

            DialogSave dlgDetached = new DialogSave(null, false);
            JButton btnReplaceDetached = (JButton) dlgDetached.getContentPane().getComponent(0);
            if (btnReplaceDetached.isEnabled()) {
                throw new AssertionError("Save (Replace) must be disabled when detached");
            }
            dlgDetached.dispose();

            // Test persistence on save()
            model.save();

            // Scenario A: Checksum/timestamp matches upon restore
            Model_JFXChess modelRestore = new Model_JFXChess();
            Workspace workspaceRestore = new Workspace();
            modelRestore.setWorkspace(workspaceRestore);
            modelRestore.restore();

            if (modelRestore.getActiveDatabase() == null || !modelRestore.getActiveDatabase().isOpen()) {
                throw new AssertionError("Active database should be restored and open");
            }
            if (workspaceRestore.getSessions().size() != 2) {
                throw new AssertionError("Expected 2 restored sessions, got: " + workspaceRestore.getSessions().size());
            }
            GameSession restoredS1 = workspaceRestore.getSessions().get(0);
            GameSession restoredS2 = workspaceRestore.getSessions().get(1);
            if (restoredS1.getPgnSourceReference() == null) {
                throw new AssertionError("Restored s1 should be linked to database index");
            }
            if (restoredS2.getPgnSourceReference() != null) {
                throw new AssertionError("Restored s2 should be detached");
            }
            if (modelRestore.isDatabaseModifiedWarning()) {
                throw new AssertionError("No warning expected when checksum matches");
            }

            // Scenario B: Last modified time changed on disk
            Files.setLastModifiedTime(path, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis() + 60000));
            Model_JFXChess modelRestoreMod = new Model_JFXChess();
            Workspace workspaceRestoreMod = new Workspace();
            modelRestoreMod.setWorkspace(workspaceRestoreMod);
            modelRestoreMod.restore();

            if (modelRestoreMod.getActiveDatabase() == null || !modelRestoreMod.getActiveDatabase().isOpen()) {
                throw new AssertionError("Modified database should still be opened");
            }
            if (!modelRestoreMod.isDatabaseModifiedWarning()) {
                throw new AssertionError("Expected database modified warning when last modified time changed");
            }
            for (GameSession sess : workspaceRestoreMod.getSessions()) {
                if (sess.getPgnSourceReference() != null) {
                    throw new AssertionError("All sessions must be detached when database modified time changed");
                }
            }

            // Scenario C: File missing
            Files.deleteIfExists(path);
            Model_JFXChess modelRestoreMissing = new Model_JFXChess();
            Workspace workspaceRestoreMissing = new Workspace();
            modelRestoreMissing.setWorkspace(workspaceRestoreMissing);
            modelRestoreMissing.restore();

            if (modelRestoreMissing.getActiveDatabase() != null) {
                throw new AssertionError("Database should not be opened when file is missing");
            }
            for (GameSession sess : workspaceRestoreMissing.getSessions()) {
                if (sess.getPgnSourceReference() != null) {
                    throw new AssertionError("All sessions must be detached when database file is missing");
                }
            }

            // === SCID5 Database Lifecycle & Persistence Test ===
            Path scidTempDir = Files.createTempDirectory("scid5_test_");
            Path scidBasePath = scidTempDir.resolve("testbase");
            String scidFilename = scidBasePath.toString() + ".si5";

            Workspace scidWorkspace = new Workspace();
            ChessDatabase scidDb = scidWorkspace.getOrCreateDatabase(Path.of(scidFilename));
            Model_JFXChess scidModel = new Model_JFXChess();
            scidModel.setWorkspace(scidWorkspace);

            scidDb.createNew(scidFilename);
            scidModel.setActiveDatabase(scidDb);

            if (!scidDb.isOpen()) {
                throw new AssertionError("SCID5 database should be open after createNew");
            }
            if (!scidDb.getIndex().isEmpty()) {
                throw new AssertionError("New SCID5 database should be empty");
            }

            Game scidG1 = new Game();
            scidG1.getRootNode().setBoard(new Board(true));
            scidG1.setHeader("White", "Morphy");
            scidG1.setHeader("Black", "Anderssen");
            GameSession scidS1 = scidWorkspace.createSession(scidG1);

            GameInfo scidInfo1 = scidDb.appendGame(scidG1);
            scidS1.setPgnSourceReference(new PgnSourceReference(scidDb.getPath(), scidInfo1.getId(), scidDb.getRevision()));

            if (scidS1.getPgnSourceReference() == null) {
                throw new AssertionError("scidS1 should be attached to SCID5 database");
            }

            // Persistence with SCID5
            scidModel.save();

            Model_JFXChess scidRestore = new Model_JFXChess();
            Workspace scidWorkspaceRestore = new Workspace();
            scidRestore.setWorkspace(scidWorkspaceRestore);
            scidRestore.restore();

            if (scidRestore.getActiveDatabase() == null || !scidRestore.getActiveDatabase().isOpen()) {
                throw new AssertionError("Active SCID5 database should be restored and open");
            }
            GameSession scidRestoredS1 = scidWorkspaceRestore.getSessions().get(0);
            if (scidRestoredS1.getPgnSourceReference() == null) {
                throw new AssertionError("Restored SCID5 session should be linked to database index");
            }

            // Clean up SCID5 temp files
            try {
                Files.deleteIfExists(Path.of(scidBasePath + ".si5"));
                Files.deleteIfExists(Path.of(scidBasePath + ".sn5"));
                Files.deleteIfExists(Path.of(scidBasePath + ".sg5"));
                Files.deleteIfExists(scidTempDir);
            } catch (Exception ignored) { }

            System.out.println("TEST: Database lifecycle, detachment and persistence passed");
        } catch (Exception exception) {
            throw new AssertionError("Database lifecycle and persistence test failed", exception);
        } finally {
            if (path != null) {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignored) { }
            }
        }
    }

    public void workspaceSessionIsolationTest() {
        System.out.println("TEST: workspace session isolation between multiple database instances");

        File temp1, temp2;
        try {
            temp1 = File.createTempFile("iso_1_", ".pgn");
            temp2 = File.createTempFile("iso_2_", ".pgn");
            temp1.deleteOnExit();
            temp2.deleteOnExit();

            PgnChessDatabase db1 = new PgnChessDatabase();
            PgnChessDatabase db2 = new PgnChessDatabase();

            db1.createNew(temp1.getAbsolutePath());
            db2.createNew(temp2.getAbsolutePath());

            Game g = new Game();
            g.setHeader("Event", "Isolation Event");
            g.getRootNode().setBoard(new Board(true));
            g.applyMove(new Move("e2e4"));

            db1.appendGame(g);

            if (db1.getIndex().size() != 1) {
                throw new RuntimeException("db1 should have 1 game");
            }
            if (!db2.getIndex().isEmpty()) {
                throw new RuntimeException("db2 should have 0 games (isolation breached)");
            }

            SearchPattern pattern = new SearchPattern();
            pattern.setEvent("Isolation Event");
            db1.search(pattern);

            if (!db1.isSearchActive() || db2.isSearchActive()) {
                throw new RuntimeException("Search active state leaked between databases");
            }

            db1.close();
            if (db1.isOpen() || !db2.isOpen()) {
                throw new RuntimeException("Closing db1 affected db2 state");
            }

            db2.close();
            System.out.println("testing workspace session isolation ... pass");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void browserTabBehaviorTest() {
        System.out.println("TEST: browser tab behavior");
        Workspace workspace = new Workspace();

        Game g1 = new Game();
        g1.getRootNode().setBoard(new Board(true));
        g1.setHeader("White", "Kasparov, Garry");
        g1.setHeader("Black", "Karpov, Anatoly");
        GameSession s1 = workspace.createSession(g1);

        Game g2 = new Game();
        g2.getRootNode().setBoard(new Board(true));
        g2.setHeader("White", "Fischer, Robert J.");
        g2.setHeader("Black", "Spassky, Boris V.");
        GameSession s2 = workspace.createSession(g2);

        Game g3 = new Game();
        g3.getRootNode().setBoard(new Board(true));
        g3.setHeader("White", "Carlsen, Magnus");
        g3.setHeader("Black", "Caruana, Fabiano");
        GameSession s3 = workspace.createSession(g3);

        if (!"Kasparov vs. Karpov".equals(s1.getGame().getVersusTitle())) {
            throw new AssertionError("Failed s1 versus title");
        }
        if (!"Fischer vs. Spassky".equals(s2.getGame().getVersusTitle())) {
            throw new AssertionError("Failed s2 versus title");
        }
        if (!"Carlsen vs. Caruana".equals(s3.getGame().getVersusTitle())) {
            throw new AssertionError("Failed s3 versus title");
        }

        // Test context switch when closing an intermediate tab (s2):
        // Context should switch to the tab to the left (s1)
        List<GameSession> sessions = workspace.getSessions();
        int index = sessions.indexOf(s2);
        GameSession targetSession = (index > 0) ? sessions.get(index - 1) : sessions.get(1);
        if (targetSession != s1) {
            throw new AssertionError("Target session for closing s2 must be s1 (left tab)");
        }
        workspace.setActiveSession(targetSession);
        workspace.closeSession(s2);

        if (workspace.getActiveSession() != s1) {
            throw new AssertionError("Active session must be s1 after closing s2");
        }
        if (workspace.getSessions().size() != 2) {
            throw new AssertionError("Workspace should have 2 sessions left");
        }

        // Test context switch when closing the leftmost tab (s1):
        // Since index == 0, context switches to the next available tab (s3)
        sessions = workspace.getSessions();
        index = sessions.indexOf(s1);
        targetSession = (index > 0) ? sessions.get(index - 1) : sessions.get(1);
        if (targetSession != s3) {
            throw new AssertionError("Target session for closing s1 must be s3");
        }
        workspace.setActiveSession(targetSession);
        workspace.closeSession(s1);

        if (workspace.getActiveSession() != s3) {
            throw new AssertionError("Active session must be s3 after closing s1");
        }
        if (workspace.getSessions().size() != 1) {
            throw new AssertionError("Workspace should have 1 session left");
        }

        // Single session left: verify protection condition
        boolean singleTabProtected = workspace.getSessions().size() <= 1;
        if (!singleTabProtected) {
            throw new AssertionError("Should be single tab protected");
        }

        System.out.println("TEST: browser tab behavior passed");
    }

    public void scid5ReadSampleDatabaseIndexTest() {
        System.out.println("TEST: reading SCID5 sample database index and namebase");
        String scidFile = getScidPath("sample_db.si5");
        Scid5ChessDatabase db = new Scid5ChessDatabase();
        try {
            db.open(scidFile);
            db.scanGames();

            if (!db.isOpen()) {
                throw new RuntimeException("SCID5 database should be open");
            }

            ArrayList<GameInfo> index = db.getIndex();
            if (index.size() != 26) {
                throw new RuntimeException("Expected 26 games in sample_db, got: " + index.size());
            }

            // Game 0: Morphy vs Duke of Brunswick & Count Isouard (Paris Opera)
            GameInfo g0 = index.get(0);
            if (!(g0 instanceof Scid5GameInfo scid0)) {
                throw new RuntimeException("Game 0 should be Scid5GameInfo");
            }
            if (!"Morphy, Paul".equals(scid0.getWhite())) {
                throw new RuntimeException("Game 0 White mismatch: " + scid0.getWhite());
            }
            if (!"Duke of Brunswick and Count Isouard".equals(scid0.getBlack())) {
                throw new RuntimeException("Game 0 Black mismatch: " + scid0.getBlack());
            }
            if (!"Paris Opera".equals(scid0.getEvent())) {
                throw new RuntimeException("Game 0 Event mismatch: " + scid0.getEvent());
            }
            if (!"Paris FRA".equals(scid0.getSite())) {
                throw new RuntimeException("Game 0 Site mismatch: " + scid0.getSite());
            }
            if (!"1858.10.21".equals(scid0.getDate())) {
                throw new RuntimeException("Game 0 Date mismatch: " + scid0.getDate());
            }
            if (!"1".equals(scid0.getRound())) {
                throw new RuntimeException("Game 0 Round mismatch: " + scid0.getRound());
            }
            if (!"1-0".equals(scid0.getResult())) {
                throw new RuntimeException("Game 0 Result mismatch: " + scid0.getResult());
            }
            if (!"C41".equals(scid0.getEco())) {
                throw new RuntimeException("Game 0 ECO mismatch: " + scid0.getEco());
            }
            if (scid0.getSg5Offset() != 0 || scid0.getSg5Length() != 38) {
                throw new RuntimeException("Game 0 offset/length mismatch: off=" + scid0.getSg5Offset() + " len=" + scid0.getSg5Length());
            }
            if (scid0.getHalfMoves() != 33) {
                throw new RuntimeException("Game 0 halfMoves mismatch: " + scid0.getHalfMoves());
            }
            if (!"Morphy".equals(scid0.getWhiteSurname())) {
                throw new RuntimeException("Game 0 white surname mismatch: " + scid0.getWhiteSurname());
            }

            // Game 4: Fischer vs Spassky
            GameInfo g4 = index.get(4);
            if (!"Fischer, Robert James".equals(g4.getWhite()) || !"Spassky, Boris V".equals(g4.getBlack())) {
                throw new RuntimeException("Game 4 players mismatch: " + g4.getWhite() + " vs " + g4.getBlack());
            }
            if (!"World Championship Match 1972".equals(g4.getEvent())) {
                throw new RuntimeException("Game 4 event mismatch: " + g4.getEvent());
            }
            if (!"1972.07.23".equals(g4.getDate())) {
                throw new RuntimeException("Game 4 date mismatch: " + g4.getDate());
            }

            // Game 13: Kramnik vs Anand (promotion game)
            Scid5GameInfo scid13 = (Scid5GameInfo) index.get(13);
            if (!"Kramnik, Vladimir".equals(scid13.getWhite()) || !"Anand, Viswanathan".equals(scid13.getBlack())) {
                throw new RuntimeException("Game 13 players mismatch: " + scid13.getWhite() + " vs " + scid13.getBlack());
            }
            if (!scid13.hasPromotion()) {
                throw new RuntimeException("Game 13 should have promotion flag set");
            }

            // Search functionality: White player contains "Fischer"
            SearchPattern fischerSearch = new SearchPattern();
            fischerSearch.setWhiteName("Fischer");
            db.search(fischerSearch);
            if (!db.isSearchActive() || db.getSearchResults().size() != 2) {
                throw new RuntimeException("Expected 2 Fischer search results, got: " + db.getSearchResults().size());
            }

            // Search by Event: "World Championship Match 1972" -> 1 match (Game 4)
            SearchPattern eventSearch = new SearchPattern();
            eventSearch.setEvent("World Championship Match 1972");
            db.search(eventSearch);
            if (!db.isSearchActive() || db.getSearchResults().size() != 1) {
                throw new RuntimeException("Expected 1 WCM 1972 search result, got: " + db.getSearchResults().size());
            }

            db.resetSearch();
            if (db.isSearchActive() || !db.getSearchResults().isEmpty()) {
                throw new RuntimeException("Reset search failed");
            }

            db.close();
            if (db.isOpen()) {
                throw new RuntimeException("Database should be closed");
            }

            System.out.println("testing reading SCID5 sample database index ... pass");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void scid5LoadGamesTest() {
        try {
            String dbPath = getScidPath("sample_db.si5");
            Scid5ChessDatabase db = new Scid5ChessDatabase();
            db.open(dbPath);
            db.scanGames();

            ArrayList<GameInfo> index = db.getIndex();
            if (index.size() != 26) {
                throw new RuntimeException("Expected 26 games in SCID5 database, got " + index.size());
            }

            // 1. Verify Game 0: Morphy Opera Game
            Game g0 = db.loadGame(index.get(0));
            if (!"Morphy, Paul".equals(g0.getHeader("White"))) {
                throw new RuntimeException("Game 0 White header mismatch: " + g0.getHeader("White"));
            }
            if (!"Duke of Brunswick and Count Isouard".equals(g0.getHeader("Black"))) {
                throw new RuntimeException("Game 0 Black header mismatch: " + g0.getHeader("Black"));
            }
            if (g0.getResult() != CONSTANTS.RES_WHITE_WINS) {
                throw new RuntimeException("Game 0 result mismatch: " + g0.getResult());
            }
            if (!"C41".equals(g0.getHeader("ECO"))) {
                throw new RuntimeException("Game 0 ECO mismatch: " + g0.getHeader("ECO"));
            }
            // End node must be checkmate with Rd8#
            GameNode end0 = g0.getEndNode();
            if (!end0.getBoard().isCheckmate()) {
                throw new RuntimeException("Game 0 final position should be checkmate");
            }
            if (!"Rd8#".equals(end0.getSan())) {
                throw new RuntimeException("Game 0 final move should be Rd8#, got: " + end0.getSan());
            }

            // 2. Verify Game 4: Fischer vs Spassky (Game 6)
            Game g4 = db.loadGame(4);
            if (!"Fischer, Robert James".equals(g4.getHeader("White"))) {
                throw new RuntimeException("Game 4 White mismatch: " + g4.getHeader("White"));
            }
            if (!"Spassky, Boris V".equals(g4.getHeader("Black"))) {
                throw new RuntimeException("Game 4 Black mismatch: " + g4.getHeader("Black"));
            }
            int g4Plies = 0;
            GameNode node4 = g4.getRootNode();
            while (node4.hasChild()) {
                node4 = node4.getVariation(0);
                g4Plies++;
            }
            if (g4Plies != 81) {
                throw new RuntimeException("Game 4 plies mismatch, expected 81, got: " + g4Plies);
            }

            // 3. Verify Game 13: Kramnik vs Anand (promotions)
            Game g13 = db.loadGame(13);
            if (!"Kramnik, Vladimir".equals(g13.getHeader("White"))) {
                throw new RuntimeException("Game 13 White mismatch: " + g13.getHeader("White"));
            }
            boolean foundPromo = false;
            GameNode node13 = g13.getRootNode();
            while (node13.hasChild()) {
                node13 = node13.getVariation(0);
                if (node13.getMove() != null && node13.getMove().promotionPiece != 0) {
                    foundPromo = true;
                    break;
                }
            }
            if (!foundPromo) {
                throw new RuntimeException("Game 13 expected pawn promotion move");
            }

            // 4. Verify Game 19: Deep Blue vs Kasparov (Comments)
            Game g19 = db.loadGame(19);
            if (!index.get(19).getWhite().equals(g19.getHeader("White")) || !g19.getHeader("White").startsWith("Deep Blue")) {
                throw new RuntimeException("Game 19 White mismatch: " + g19.getHeader("White"));
            }
            int commentCount = 0;
            GameNode node19 = g19.getRootNode();
            while (node19.hasChild()) {
                node19 = node19.getVariation(0);
                if (node19.getComment() != null && !node19.getComment().isEmpty()) {
                    commentCount++;
                }
            }
            if (commentCount != 5) {
                throw new RuntimeException("Game 19 expected 5 comments, found: " + commentCount);
            }

            // 5. In-memory buffer test: modified game buffering
            Scid5GameInfo scid0 = (Scid5GameInfo) index.get(0);
            Game dummyGame = new Game();
            dummyGame.setHeader("White", "Custom Player White");
            dummyGame.setHeader("Black", "Custom Player Black");
            scid0.setModifiedGame(dummyGame);
            scid0.setModified(true);

            Game loadedModified = db.loadGame(scid0);
            if (!"Custom Player White".equals(loadedModified.getHeader("White"))) {
                throw new RuntimeException("Expected in-memory modified game to be returned");
            }

            // Reset modified flag -> should reload original from disk
            scid0.setModified(false);
            scid0.setModifiedGame(null);
            Game reloadedOriginal = db.loadGame(scid0);
            if (!"Morphy, Paul".equals(reloadedOriginal.getHeader("White"))) {
                throw new RuntimeException("Expected original game from disk after unsetting modified");
            }

            db.close();
            System.out.println("testing loading games from SCID5 database ... pass");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void scid5RoundTripEncodeDecodeTest() {
        try {
            System.out.println("TEST: SCID5 round-trip encode and decode for all sample games");
            String dbPath = getScidPath("sample_db.si5");
            Scid5ChessDatabase db = new Scid5ChessDatabase();
            db.open(dbPath);
            db.scanGames();

            ArrayList<GameInfo> index = db.getIndex();
            PgnPrinter printer = new PgnPrinter();

            for (int i = 0; i < index.size(); i++) {
                Scid5GameInfo info = (Scid5GameInfo) index.get(i);
                Game origGame = db.loadGame(info);

                // 1. Encode game into SCID5 blob
                Scid5EncodeResult enc = Scid5MoveEncoder.encode(origGame);
                if (enc.data == null || enc.dataSize == 0) {
                    throw new RuntimeException("Game " + i + " encoded blob is empty");
                }

                // Verify metadata matches index
                if (enc.halfMoves != info.getHalfMoves()) {
                    throw new RuntimeException("Game " + i + " halfMoves mismatch: expected "
                            + info.getHalfMoves() + ", got " + enc.halfMoves);
                }
                if (enc.finalMatSig != info.getFinalMatSig()) {
                    throw new RuntimeException(String.format(
                            "Game %d finalMatSig mismatch: expected 0x%06X, got 0x%06X",
                            i, info.getFinalMatSig(), enc.finalMatSig));
                }
                if (enc.homePawnCount != info.getHomePawnCount()) {
                    throw new RuntimeException("Game " + i + " homePawnCount mismatch: expected "
                            + info.getHomePawnCount() + ", got " + enc.homePawnCount);
                }
                if (!Arrays.equals(enc.homePawnData, info.getHomePawnData())) {
                    throw new RuntimeException(String.format(
                            "Game %d homePawnData mismatch", i));
                }

                // 2. Decode back into a new Game object
                Game roundTripped = Scid5MoveDecoder.decode(enc.data, 0, enc.dataSize, info);

                // 3. Compare original vs round-tripped PGN
                String origPgn = printer.printGame(origGame);
                String roundPgn = printer.printGame(roundTripped);

                if (!comparePgnStrings(origPgn, roundPgn)) {
                    throw new RuntimeException("Game " + i + " round-trip PGN mismatch!\nOriginal:\n"
                            + origPgn + "\nRound-tripped:\n" + roundPgn);
                }
            }

            // 4. Verify custom FEN starting position round-trip with promotions & sub-variations
            String customPgn = "[Event \"Custom FEN Test\"]\n" +
                    "[Site \"Test Site\"]\n" +
                    "[Date \"2024.01.01\"]\n" +
                    "[Round \"1\"]\n" +
                    "[White \"Custom White\"]\n" +
                    "[Black \"Custom Black\"]\n" +
                    "[Result \"*\"]\n" +
                    "[SetUp \"1\"]\n" +
                    "[FEN \"8/P5k1/8/8/8/8/6K1/8 w - - 0 1\"]\n" +
                    "\n" +
                    "1. a8=Q ( 1. a8=N { underpromotion variation } 1... Kf6 ) 1... Kg6 { nice king move } *";

            PgnReader reader = new PgnReader();
            Game customGame = reader.readGame(customPgn);
            Scid5EncodeResult encCustom = Scid5MoveEncoder.encode(customGame);

            if ((encCustom.flags & Scid5GameInfo.FLAG_CUSTOM_START) == 0) {
                throw new RuntimeException("Expected FLAG_CUSTOM_START to be set");
            }
            if ((encCustom.flags & Scid5GameInfo.FLAG_PROMO) == 0) {
                throw new RuntimeException("Expected FLAG_PROMO to be set");
            }
            if ((encCustom.flags & Scid5GameInfo.FLAG_UNDER_PROMO) == 0) {
                throw new RuntimeException("Expected FLAG_UNDER_PROMO to be set");
            }

            Scid5GameInfo customInfo = new Scid5GameInfo();
            customInfo.setWhite("Custom White");
            customInfo.setBlack("Custom Black");
            customInfo.setEvent("Custom FEN Test");
            customInfo.setSite("Test Site");
            customInfo.setDate("2024.01.01");
            customInfo.setRound("1");
            customInfo.setResult("*");

            Game roundTrippedCustom = Scid5MoveDecoder.decode(encCustom.data, 0, encCustom.dataSize, customInfo);
            String origCustomPgn = printer.printGame(customGame);
            String roundCustomPgn = printer.printGame(roundTrippedCustom);

            if (!comparePgnStrings(origCustomPgn, roundCustomPgn)) {
                throw new RuntimeException("Custom FEN game round-trip mismatch!\nOriginal:\n"
                        + origCustomPgn + "\nRound-tripped:\n" + roundCustomPgn);
            }

            db.close();
            System.out.println("testing SCID5 round-trip encode and decode ... pass");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void scid5WriteOperationsTest() {
        System.out.println("TEST: SCID5 write operations (createNew, appendGame, replaceGame, deleteGame, persistence)");
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("scid5_write_test_");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Path baseDb = tempDir.resolve("test_write_db");
        String filename = baseDb.toString();

        try {
            Scid5ChessDatabase db = new Scid5ChessDatabase();
            ArrayList<ChessDatabaseEvent> events = new ArrayList<>();
            ChessDatabaseListener listener = events::add;
            db.addListener(listener);

            // 1. Test createNew
            db.createNew(filename);
            if (!db.isOpen()) {
                throw new RuntimeException("Database should be open after createNew");
            }
            if (events.isEmpty() || events.get(events.size() - 1).getType() != ChessDatabaseEvent.Type.DATABASE_OPENED) {
                throw new RuntimeException("Expected DATABASE_OPENED event");
            }
            if (!db.getIndex().isEmpty()) {
                throw new RuntimeException("New database index should be empty");
            }

            Path si5 = Path.of(filename + ".si5");
            Path sn5 = Path.of(filename + ".sn5");
            Path sg5 = Path.of(filename + ".sg5");
            if (!Files.exists(si5) || !Files.exists(sn5) || !Files.exists(sg5)) {
                throw new RuntimeException("Companion files .si5, .sn5, .sg5 must exist on disk");
            }

            // 2. Append Game 1
            Game g1 = new Game();
            g1.setHeader("Event", "World Championship 1985");
            g1.setHeader("Site", "Moscow RUS");
            g1.setHeader("Date", "1985.11.09");
            g1.setHeader("Round", "24");
            g1.setHeader("White", "Kasparov, Garry");
            g1.setHeader("Black", "Karpov, Anatoly");
            g1.setHeader("Result", "1-0");
            g1.setHeader("ECO", "B44");
            g1.setHeader("WhiteElo", "2700");
            g1.setHeader("BlackElo", "2720");
            g1.getRootNode().setBoard(new Board(true));
            g1.applyMove(new Move("e2e4"));
            g1.applyMove(new Move("c7c5"));
            g1.applyMove(new Move("g1f3"));
            g1.applyMove(new Move("e7e6"));
            g1.applyMove(new Move("d2d4"));
            g1.applyMove(new Move("c5d4"));
            g1.applyMove(new Move("f3d4"));
            g1.applyMove(new Move("b8c6"));

            GameInfo info1 = db.appendGame(g1);
            if (info1 == null) {
                throw new RuntimeException("appendGame returned null GameInfo");
            }
            if (db.getIndex().size() != 1) {
                throw new RuntimeException("Expected 1 game after first append, got: " + db.getIndex().size());
            }
            if (events.get(events.size() - 1).getType() != ChessDatabaseEvent.Type.GAME_APPENDED) {
                throw new RuntimeException("Expected GAME_APPENDED event");
            }

            // 3. Append Game 2
            Game g2 = new Game();
            g2.setHeader("Event", "World Championship 1960");
            g2.setHeader("Site", "Moscow RUS");
            g2.setHeader("Date", "1960.03.15");
            g2.setHeader("Round", "6");
            g2.setHeader("White", "Tal, Mihail");
            g2.setHeader("Black", "Botvinnik, Mikhail");
            g2.setHeader("Result", "1-0");
            g2.setHeader("ECO", "C18");
            g2.setHeader("WhiteElo", "2600");
            g2.setHeader("BlackElo", "2650");
            g2.getRootNode().setBoard(new Board(true));
            g2.applyMove(new Move("e2e4"));
            g2.applyMove(new Move("e7e6"));
            g2.applyMove(new Move("d2d4"));
            g2.applyMove(new Move("d7d5"));
            g2.applyMove(new Move("b1c3"));
            g2.applyMove(new Move("f8b4"));

            GameInfo info2 = db.appendGame(g2);
            if (info2 == null) {
                throw new RuntimeException("appendGame returned null GameInfo for second game");
            }
            if (db.getIndex().size() != 2) {
                throw new RuntimeException("Expected 2 games after second append, got: " + db.getIndex().size());
            }

            // Verify loaded games
            Game loaded1 = db.loadGame(0);
            if (!"Kasparov, Garry".equals(loaded1.getHeader("White")) || !"Karpov, Anatoly".equals(loaded1.getHeader("Black"))) {
                throw new RuntimeException("Loaded game 1 player mismatch");
            }
            if (!"1-0".equals(loaded1.getHeader("Result")) || !"B44".equals(loaded1.getHeader("ECO"))) {
                throw new RuntimeException("Loaded game 1 header mismatch");
            }

            Game loaded2 = db.loadGame(info2);
            if (!"Tal, Mihail".equals(loaded2.getHeader("White")) || !"Botvinnik, Mikhail".equals(loaded2.getHeader("Black"))) {
                throw new RuntimeException("Loaded game 2 player mismatch");
            }

            // 4. Test replaceGame: in-place overwrite branch (shorter game)
            Game g1Small = new Game();
            g1Small.setHeader("Event", "Short Game");
            g1Small.setHeader("White", "Kasparov, Garry");
            g1Small.setHeader("Black", "Karpov, Anatoly");
            g1Small.setHeader("Result", "1/2-1/2");
            g1Small.getRootNode().setBoard(new Board(true));
            g1Small.applyMove(new Move("e2e4"));
            g1Small.applyMove(new Move("e7e5"));

            db.replaceGame(g1Small, info1);
            if (events.get(events.size() - 1).getType() != ChessDatabaseEvent.Type.GAME_REPLACED) {
                throw new RuntimeException("Expected GAME_REPLACED event for in-place replace");
            }
            Game loadedSmall = db.loadGame(0);
            if (!"Short Game".equals(loadedSmall.getHeader("Event"))) {
                throw new RuntimeException("In-place replace did not update Event header");
            }

            // 5. Test replaceGame: append branch (larger game exceeding original allocation)
            Game g1Updated = new Game();
            g1Updated.setHeader("Event", "World Championship 1985 Game 24 (Updated)");
            g1Updated.setHeader("Site", "Moscow RUS");
            g1Updated.setHeader("Date", "1985.11.09");
            g1Updated.setHeader("Round", "24");
            g1Updated.setHeader("White", "Kasparov, Garry");
            g1Updated.setHeader("Black", "Karpov, Anatoly");
            g1Updated.setHeader("Result", "1/2-1/2");
            g1Updated.setHeader("ECO", "B44");
            g1Updated.setHeader("WhiteElo", "2700");
            g1Updated.setHeader("BlackElo", "2720");
            g1Updated.getRootNode().setBoard(new Board(true));
            g1Updated.applyMove(new Move("e2e4"));
            g1Updated.applyMove(new Move("c7c5"));
            g1Updated.applyMove(new Move("g1f3"));
            g1Updated.applyMove(new Move("e7e6"));
            g1Updated.applyMove(new Move("d2d4"));
            g1Updated.applyMove(new Move("c5d4"));
            g1Updated.applyMove(new Move("f3d4"));
            g1Updated.applyMove(new Move("b8c6"));
            g1Updated.applyMove(new Move("d4b5"));
            g1Updated.applyMove(new Move("d7d6"));
            g1Updated.applyMove(new Move("c2c4"));
            g1Updated.applyMove(new Move("g8f6"));
            GameNode lastNode = g1Updated.getEndNode();
            lastNode.setComment("Sharp Paulsen variation");
            lastNode.addNag(1);

            db.replaceGame(g1Updated, info1);
            if (events.get(events.size() - 1).getType() != ChessDatabaseEvent.Type.GAME_REPLACED) {
                throw new RuntimeException("Expected GAME_REPLACED event for append replace");
            }
            Game loadedUpdated = db.loadGame(0);
            if (!"World Championship 1985 Game 24 (Updated)".equals(loadedUpdated.getHeader("Event"))) {
                throw new RuntimeException("Append replace did not update Event header");
            }
            if (!"1/2-1/2".equals(loadedUpdated.getHeader("Result"))) {
                throw new RuntimeException("Append replace did not update Result");
            }
            GameNode endNode = loadedUpdated.getEndNode();
            if (!"Sharp Paulsen variation".equals(endNode.getComment())) {
                throw new RuntimeException("Append replace comment missing, got: " + endNode.getComment());
            }
            if (endNode.getNags().isEmpty() || endNode.getNags().get(0) != 1) {
                throw new RuntimeException("Append replace NAG missing");
            }

            // 6. Test deleteGame
            GameInfo currentInfo2 = db.getIndex().get(1);
            db.deleteGame(currentInfo2);
            if (events.get(events.size() - 1).getType() != ChessDatabaseEvent.Type.GAME_DELETED) {
                throw new RuntimeException("Expected GAME_DELETED event");
            }
            if (db.getIndex().size() != 1) {
                throw new RuntimeException("Expected 1 active game after delete, got: " + db.getIndex().size());
            }
            if (!"Kasparov, Garry".equals(db.getIndex().get(0).getWhite())) {
                throw new RuntimeException("Remaining game should be Kasparov");
            }
            if (db.getAllEntries().size() != 2) {
                throw new RuntimeException("allEntries should retain both physical records");
            }
            Scid5GameInfo deletedInfo = (Scid5GameInfo) db.getAllEntries().get(1);
            if (!deletedInfo.isDeleted()) {
                throw new RuntimeException("Physical record in allEntries must have isDeleted() == true");
            }

            // 7. Verify persistence across close and re-open
            db.removeListener(listener);
            db.close();
            if (db.isOpen()) {
                throw new RuntimeException("Database should be closed");
            }

            // Re-open via ChessDatabase.openDatabase factory method
            ChessDatabase db2 = ChessDatabase.openDatabase(filename + ".si5");
            if (!(db2 instanceof Scid5ChessDatabase scidDb2)) {
                throw new RuntimeException("Factory openDatabase should return Scid5ChessDatabase instance");
            }
            scidDb2.scanGames();

            if (scidDb2.getIndex().size() != 1) {
                throw new RuntimeException("Re-opened database should have 1 active game, got: " + scidDb2.getIndex().size());
            }
            Game reloaded1 = scidDb2.loadGame(0);
            if (!"World Championship 1985 Game 24 (Updated)".equals(reloaded1.getHeader("Event"))) {
                throw new RuntimeException("Re-opened database game 0 Event header mismatch");
            }
            if (!"1/2-1/2".equals(reloaded1.getHeader("Result"))) {
                throw new RuntimeException("Re-opened database game 0 Result mismatch");
            }

            // Check includeDeleted on reloaded database
            scidDb2.setIncludeDeleted(true);
            if (scidDb2.getIndex().size() != 2) {
                throw new RuntimeException("Expected 2 games with includeDeleted=true, got: " + scidDb2.getIndex().size());
            }
            Scid5GameInfo reloadedDeleted = (Scid5GameInfo) scidDb2.getIndex().get(1);
            if (!reloadedDeleted.isDeleted()) {
                throw new RuntimeException("Deleted game on disk must have isDeleted() == true after scan");
            }
            scidDb2.close();

            // Clean up temporary files
            Files.deleteIfExists(si5);
            Files.deleteIfExists(sn5);
            Files.deleteIfExists(sg5);
            Files.deleteIfExists(tempDir);

            System.out.println("testing SCID5 write operations ... pass");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void scid5SearchTest() {
        System.out.println("TEST: SCID5 search by header patterns and progress reporting");
        String scidFile = getScidPath("sample_db.si5");
        Scid5ChessDatabase db = new Scid5ChessDatabase();

        try {
            db.open(scidFile);
            db.scanGames();

            // 1. Search by Event: "Paris Opera"
            SearchPattern eventPattern = new SearchPattern();
            eventPattern.setEvent("Paris Opera");
            db.search(eventPattern);

            ArrayList<GameInfo> eventResults = db.getSearchResults();
            if (eventResults.size() != 2 || !db.isSearchActive()) {
                throw new RuntimeException("Expected 2 results for 'Paris Opera', got: " + eventResults.size());
            }
            for (GameInfo g : eventResults) {
                if (!g.getEvent().contains("Paris Opera")) {
                    throw new RuntimeException("Event search result mismatch: " + g.getEvent());
                }
            }

            // 2. Search by Player with ignoreNameColor = true: "Kasparov"
            SearchPattern kasparovPattern = new SearchPattern();
            kasparovPattern.setWhiteName("Kasparov");
            kasparovPattern.setIgnoreNameColor(true);
            db.search(kasparovPattern);

            ArrayList<GameInfo> kasparovResults = db.getSearchResults();
            if (kasparovResults.isEmpty()) {
                throw new RuntimeException("Expected at least 1 Kasparov game in sample_db");
            }
            for (GameInfo g : kasparovResults) {
                boolean contains = g.getWhite().contains("Kasparov") || g.getBlack().contains("Kasparov");
                if (!contains) {
                    throw new RuntimeException("Result does not contain Kasparov: " + g.getVersusTitle());
                }
            }

            // 3. Search by Result: draws only ("1/2-1/2")
            SearchPattern drawPattern = new SearchPattern();
            drawPattern.setResultWhiteWins(false);
            drawPattern.setResultBlackWins(false);
            drawPattern.setResultUndef(false);
            drawPattern.setResultDraw(true);
            db.search(drawPattern);

            ArrayList<GameInfo> drawResults = db.getSearchResults();
            if (drawResults.isEmpty()) {
                throw new RuntimeException("Expected at least 1 draw game in sample_db");
            }
            for (GameInfo g : drawResults) {
                if (!"1/2-1/2".equals(g.getResult())) {
                    throw new RuntimeException("Result search expected '1/2-1/2', got: " + g.getResult());
                }
            }

            // 4. Test ProgressListener and cancellation
            boolean[] progressCalled = new boolean[]{false};
            boolean[] cancelled = new boolean[]{false};
            ProgressListener listener = new ProgressListener() {
                @Override
                public void onProgress(int percent) {
                    progressCalled[0] = true;
                }
                @Override
                public boolean isCancelled() {
                    return cancelled[0];
                }
            };
            db.search(eventPattern, listener);
            if (!progressCalled[0]) {
                throw new RuntimeException("ProgressListener onProgress was not invoked during search");
            }

            // Cancellation test
            cancelled[0] = true;
            db.search(eventPattern, listener);

            // 5. Test resetSearch
            db.resetSearch();
            if (db.isSearchActive() || !db.getSearchResults().isEmpty()) {
                throw new RuntimeException("resetSearch failed to clear active state and results");
            }

            db.close();
            System.out.println("testing SCID5 search ... pass");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

