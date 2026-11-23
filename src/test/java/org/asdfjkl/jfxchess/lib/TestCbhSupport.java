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

import org.asdfjkl.jfxchess.gui.DialogDatabase;
import org.asdfjkl.jfxchess.gui.DialogSave;
import org.asdfjkl.jfxchess.gui.Workspace;

import javax.swing.JButton;
import java.awt.Component;
import java.awt.Container;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Clean-room unit tests for CBH and 2CBH read-only databases, direct SCID5
 * streaming conversion, read-only GUI/workspace guards, and search.
 * All fixture files are synthetically generated in memory using newly invented games.
 */
public final class TestCbhSupport {

    private TestCbhSupport() {
    }

    public static void runAll() {
        syntheticCbhStandardAndFeaturesTest();
        syntheticTwoCbhAnd2LidEdgeCasesTest();
        syntheticCbhAndTwoCbhToScid5RoundTripTest();
        readOnlyGuiGuardsAndWorkspaceTest();
    }

    public static void syntheticCbhStandardAndFeaturesTest() {
        System.out.println("Testing synthetic classic CBH (.cbh/.cbg/.cba/.cbp/.cbt/.cbc/.cbs)...");
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("jfxchess_cbh_test_");
            // 1. Test both 32-byte header (Modern) & 10-byte .cba header (Legacy Fritz6)
            Path modernBase = tempDir.resolve("synth_modern");
            writeSyntheticCbhDatabase(modernBase, 32, 10);

            verifyCbhDatabase(modernBase.toString() + ".cbh");
            // Open via auxiliary file (.cbg / .cbp)
            verifyCbhDatabase(modernBase.toString() + ".cbg");
            verifyCbhDatabase(modernBase.toString() + ".cbp");

            // 2. Test 28-byte header (Legacy Fritz6/7/8) & 26-byte .cba header
            Path legacyBase = tempDir.resolve("synth_legacy");
            writeSyntheticCbhDatabase(legacyBase, 28, 26);
            verifyCbhDatabase(legacyBase.toString() + ".cbh");

            System.out.println("  syntheticCbhStandardAndFeaturesTest PASSED.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            deleteDirRecursive(tempDir);
        }
    }

    private static void verifyCbhDatabase(String openPath) throws IOException {
        CbhChessDatabase db = new CbhChessDatabase();
        try {
            db.open(openPath);
            assertTrue(db.isReadOnly(), "CBH database must be read-only");
            assertTrue(db.isOpen(), "CBH database should report open");
            db.scanGames();

            ArrayList<GameInfo> index = db.getIndex();
            assertEquals(2, index.size(), "Guiding text (0x03) and deleted (0x81) records must be skipped");

            GameInfo g0Info = index.get(0);
            assertEquals("Fischer, Bobby", g0Info.getWhite(), "White name");
            assertEquals("Spassky, Boris", g0Info.getBlack(), "Black name");
            assertEquals("Fischer", g0Info.getWhiteSurname(), "White surname");
            assertEquals("Winter Cup", g0Info.getEvent(), "Event title");
            assertEquals("Zurich", g0Info.getSite(), "Event site");
            assertEquals("2025.03.14", g0Info.getDate(), "Game date");
            assertEquals("3.2", g0Info.getRound(), "Round with subround");
            assertEquals("1-0", g0Info.getResult(), "Result");
            assertEquals("B01", g0Info.getEco(), "ECO");
            assertEquals("2750", g0Info.getWhiteElo(), "White ELO");
            assertEquals("2690", g0Info.getBlackElo(), "Black ELO");

            Game g0 = db.loadGame(g0Info);
            assertEquals("Test Annotator", g0.getHeader("Annotator"), "Annotator tag");
            assertEquals("Test Source", g0.getHeader("Source"), "Source tag");

            GameNode root = g0.getRootNode();
            assertEquals("Opening prep!", root.getComment(), "Root prefers Universal over German");

            // Mainline: 1. e4 d5 2. exd5 (2... Nc6 3. Nc3) 2... e5 3. dxe6 b5 4. e7 b4 5. exf8=Q ... 7... b1=N+
            assertTrue(root.getVariations().size() == 1, "Root has 1 child");
            GameNode n1 = root.getVariation(0); // 1. e4
            assertEquals("e4", n1.getSan(), "Move 1 SAN");
            assertEquals("Sharp 1.Qf7", n1.getComment(), "Move 1 prefers English and converts figurines");
            assertTrue(n1.getNags().contains(1), "Move 1 has NAG $1");
            assertTrue(n1.getNags().contains(10), "Move 1 has mapped eval NAG $10 (from 0x0B)");
            assertEquals(1, n1.getColoredFields().size(), "Move 1 has colored square");
            assertEquals(4, n1.getColoredFields().get(0).x, "Colored square file e");
            assertEquals(3, n1.getColoredFields().get(0).y, "Colored square rank 4");
            assertEquals(1, n1.getArrows().size(), "Move 1 has arrow");

            GameNode d5 = n1.getVariation(0);
            GameNode exd5 = d5.getVariation(0);
            assertEquals(2, exd5.getVariations().size(), "Branch point has mainline + sideline");
            GameNode mainE5 = exd5.getVariation(0);
            GameNode varNc6 = exd5.getVariation(1);
            assertEquals("e5", mainE5.getSan(), "Mainline variation(0) is 2...e5");
            assertEquals("Nc6", varNc6.getSan(), "Sideline variation(1) is 2...Nc6");
            assertEquals("Side comment", varNc6.getComment(), "Variation comment mapped properly");
            assertEquals("Nc3", varNc6.getVariation(0).getSan(), "Variation move 3.Nc3");

            // Post-POP annotation on mainline move 3.dxe6 (EP) using (moveSeq - 1) alignment
            GameNode dxe6 = mainE5.getVariation(0);
            assertEquals("dxe6", dxe6.getSan(), "En passant capture");
            assertEquals("En passant!", dxe6.getComment(), "Post-POP annotation aligned cleanly");

            // Check promotions down mainline: 3...b5 4.e7 b4 5.exf8=Q+ Kd7 6.Nc3 b3 7.Nb5 b2 8.Ke2 b1=N
            GameNode promoW = dxe6.getVariation(0).getVariation(0).getVariation(0).getVariation(0);
            assertEquals("exf8=Q+", promoW.getSan(), "White promotion via 0x29 escape");
            GameNode promoB = promoW.getVariation(0).getVariation(0).getVariation(0)
                    .getVariation(0).getVariation(0).getVariation(0).getVariation(0);
            assertEquals("b1=N", promoB.getSan(), "Black underpromotion via 0x29 escape");

            // Custom setup game (Game 1): single White pawn on h5 occupying slot 0 (Pa), custom castling WQ
            GameInfo g1Info = index.get(1);
            assertEquals("1/2-1/2", g1Info.getResult(), "Game 1 result");
            Game g1 = db.loadGame(g1Info);
            String fen = g1.getRootNode().getBoard().fen();
            assertTrue(fen.contains("4k3") && fen.contains("7P") && fen.contains("Q"),
                    "Custom setup FEN should preserve king/rook/h5-pawn and WQ castling: " + fen);
            assertEquals(1, g1.getRootNode().getVariations().size(), "Custom game has 1 move");
            assertEquals("h6", g1.getRootNode().getVariation(0).getSan(), "Lone h5 pawn uses slot 0 (Pa)");

            // Verify read-only guards throw UnsupportedOperationException
            assertThrowsUnsupported(() -> db.createNew("ignored.cbh"), "createNew");
            assertThrowsUnsupported(() -> db.appendGame(new Game()), "appendGame");
            assertThrowsUnsupported(() -> db.importGames(db, index, null), "importGames");
            assertThrowsUnsupported(() -> db.replaceGame(new Game(), g0Info), "replaceGame");
            assertThrowsUnsupported(() -> db.deleteGame(g0Info), "deleteGame");

            // Header search
            SearchPattern hp = new SearchPattern();
            hp.setSearchGameData(true);
            hp.setWhiteName("Fischer");
            db.search(hp);
            assertEquals(1, db.getSearchResults().size(), "Header player search matches 1 game");

            // Position search (custom setup board or after e8=Q)
            SearchPattern posPat = new SearchPattern();
            posPat.setSearchGameData(false);
            posPat.setSearchPosition(true);
            posPat.setSearchTurn(SearchPattern.TURN_BLACK);
            posPat.setSearchBoard(promoW.getBoard().makeCopy());
            db.search(posPat);
            assertEquals(1, db.getSearchResults().size(), "Position search finds promoted Q position");
            db.resetSearch();
            assertTrue(!db.isSearchActive(), "resetSearch deactivates search");
        } finally {
            db.close();
        }
    }

    public static void syntheticTwoCbhAnd2LidEdgeCasesTest() {
        System.out.println("Testing synthetic 2CBH (.2cbh/.2cbg/.2cba/.2lid)...");
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("jfxchess_2cbh_test_");
            // Case A: Slot 3 (Annotators) empty -> fallback to Slot 0, plus truncated final .2lid bucket!
            Path baseA = tempDir.resolve("two_slot3_empty");
            writeSyntheticTwoCbhDatabase(baseA, false, true);

            TwoCbhChessDatabase db = new TwoCbhChessDatabase(baseA.toString() + ".2cbh");
            try {
                assertTrue(db.isReadOnly(), "2CBH must be read-only");
                db.scanGames();
                ArrayList<GameInfo> idx = db.getIndex();
                assertEquals(2, idx.size(), "Guiding text & deleted records skipped in 2CBH");

                GameInfo g0 = idx.get(0);
                assertEquals("Alpha, Alice", g0.getWhite(), "Truncated last .2lid bucket decodes safely");
                assertEquals("Beta, Bob", g0.getBlack(), "Black name");
                assertEquals("London Open", g0.getEvent(), "Slot 1 Event (Title comes second)");
                assertEquals("London", g0.getSite(), "Slot 1 Site (Place comes first)");
                assertEquals("C42", g0.getEco(), "2CBH ECO");

                Game loaded0 = db.loadGame(g0);
                assertEquals("Gamma, Greg", loaded0.getHeader("Annotator"),
                        "When Slot 3 count == 0, annotator_id indexes Slot 0 Players");
                assertEquals("Custom Publisher Press", loaded0.getHeader("Source"), "Slot 2 Source");

                GameNode root = loaded0.getRootNode();
                assertEquals("Root UF KQNBR!", normalizeSpaces(root.getComment()),
                        "Root UTF-8 PUA figurines decoded: " + root.getComment());

                // Mainline + C_rev check + 0x0013 skip verification
                GameNode n1 = root.getVariation(0); // 1. e4
                assertEquals("e4", n1.getSan(), "1.e4");
                assertEquals("PreWin Qf4", n1.getComment(),
                        "0x0082 + Win-1252 fallback figurines normalized & multi-lang EN selected");

                GameNode n1Reply = n1.getVariation(0); // 1...e5
                assertEquals(2, n1Reply.getVariations().size(), "Branch at move 2");
                GameNode mainNf3 = n1Reply.getVariation(0); // mainline C_rev mv=3
                GameNode varBc4 = n1Reply.getVariation(1);  // sideline C_rev mv=5
                assertEquals("Nf3", mainNf3.getSan(), "Mainline variation(0)");
                assertEquals("Bc4", varBc4.getSan(), "Sideline variation(1)");
                assertEquals("After ref citation", varBc4.getComment(),
                        "0x0013 skipped cleanly without corrupting later C_rev comments");

                // Game 1 custom setup (0xFFFB + 0xC02D..0xC30C piece words)
                Game loaded1 = db.loadGame(idx.get(1));
                String fen1 = loaded1.getRootNode().getBoard().fen();
                assertTrue(fen1.contains("1k6") && fen1.contains("4P3"),
                        "2CBG custom setup decoded: " + fen1);
                GameNode g1M1 = loaded1.getRootNode().getVariation(0);
                GameNode g1M2 = g1M1.getVariation(0);
                GameNode g1M3 = g1M2.getVariation(0);
                assertEquals("e7", g1M1.getSan(), "Setup move e7");
                assertEquals("Kc7", g1M2.getSan(), "Setup move Kc7");
                assertEquals("e8=N+", g1M3.getSan(), "Underpromotion in 2CBG setup game");

                // Position search in 2CBH
                SearchPattern posPat = new SearchPattern();
                posPat.setSearchGameData(false);
                posPat.setSearchPosition(true);
                posPat.setSearchTurn(SearchPattern.TURN_BLACK);
                posPat.setSearchBoard(varBc4.getBoard().makeCopy());
                db.search(posPat);
                assertEquals(1, db.getSearchResults().size(), "2CBH position search finds variation move");

                assertThrowsUnsupported(() -> db.createNew("x.2cbh"), "2CBH createNew");
                assertThrowsUnsupported(() -> db.appendGame(loaded0), "2CBH appendGame");
                assertThrowsUnsupported(() -> db.deleteGame(g0), "2CBH deleteGame");
            } finally {
                db.close();
            }

            // Case B: Slot 3 non-empty
            Path baseB = tempDir.resolve("two_slot3_nonempty");
            writeSyntheticTwoCbhDatabase(baseB, true, false);
            TwoCbhChessDatabase dbB = new TwoCbhChessDatabase(baseB.toString() + ".2lid");
            try {
                dbB.scanGames();
                Game gB = dbB.loadGame(dbB.getIndex().get(0));
                assertEquals("Slot3Annotator", gB.getHeader("Annotator"),
                        "When Slot 3 count > 0, annotator_id reads Slot 3");
            } finally {
                dbB.close();
            }

            System.out.println("  syntheticTwoCbhAnd2LidEdgeCasesTest PASSED.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            deleteDirRecursive(tempDir);
        }
    }

    public static void syntheticCbhAndTwoCbhToScid5RoundTripTest() {
        System.out.println("Testing CBH and 2CBH direct streaming conversion to SCID5...");
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("jfxchess_conv_test_");

            // 1. CBH -> SCID5 round-trip equivalence
            Path cbhBase = tempDir.resolve("src_cbh");
            writeSyntheticCbhDatabase(cbhBase, 32, 26);
            Path scidFromCbh = tempDir.resolve("out_from_cbh.si5");

            AtomicInteger lastPct = new AtomicInteger(-1);
            int cbhConverted = CbhToScid5Converter.convert(
                    Path.of(cbhBase.toString() + ".cbh"),
                    scidFromCbh,
                    new ProgressListener() {
                        @Override
                        public void onProgress(int percent) {
                            lastPct.set(percent);
                        }

                        @Override
                        public boolean isCancelled() {
                            return false;
                        }
                    }
            );
            assertEquals(2, cbhConverted, "Converted game count from CBH");
            assertEquals(100, lastPct.get(), "Final progress 100%");
            assertRoundTripEquivalent(
                    Path.of(cbhBase.toString() + ".cbh"),
                    scidFromCbh,
                    "CBH -> SCID5"
            );

            // 2. 2CBH -> SCID5 round-trip equivalence
            Path twoBase = tempDir.resolve("src_2cbh");
            writeSyntheticTwoCbhDatabase(twoBase, false, true);
            Path scidFrom2Cbh = tempDir.resolve("out_from_2cbh.si5");
            int twoConverted = CbhToScid5Converter.convert(
                    Path.of(twoBase.toString() + ".2cbh"),
                    scidFrom2Cbh,
                    null
            );
            assertEquals(2, twoConverted, "Converted game count from 2CBH");
            assertRoundTripEquivalent(
                    Path.of(twoBase.toString() + ".2cbh"),
                    scidFrom2Cbh,
                    "2CBH -> SCID5"
            );

            // 3. Cancellation cleans up partial target files
            Path cancelTarget = tempDir.resolve("cancelled_out.si5");
            CbhToScid5Converter.convert(
                    Path.of(twoBase.toString() + ".2cbh"),
                    cancelTarget,
                    new ProgressListener() {
                        @Override
                        public void onProgress(int percent) {
                        }

                        @Override
                        public boolean isCancelled() {
                            return true;
                        }
                    }
            );
            assertTrue(!Files.exists(cancelTarget), "Cancelled .si5 must be deleted");
            assertTrue(!Files.exists(tempDir.resolve("cancelled_out.sn5")), "Cancelled .sn5 must be deleted");
            assertTrue(!Files.exists(tempDir.resolve("cancelled_out.sg5")), "Cancelled .sg5 must be deleted");

            System.out.println("  syntheticCbhAndTwoCbhToScid5RoundTripTest PASSED.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            deleteDirRecursive(tempDir);
        }
    }

    private static void assertRoundTripEquivalent(Path srcDbPath, Path scidSi5Path, String label) throws IOException {
        PgnPrinter printer = new PgnPrinter();
        ChessDatabase srcDb = ChessDatabase.openDatabase(srcDbPath.toString());
        Scid5ChessDatabase scidDb = new Scid5ChessDatabase(scidSi5Path.toString());
        try {
            srcDb.scanGames();
            scidDb.scanGames();
            ArrayList<GameInfo> srcIdx = srcDb.getIndex();
            ArrayList<GameInfo> scidIdx = scidDb.getIndex();
            assertEquals(srcIdx.size(), scidIdx.size(), label + " index size match");

            for (int i = 0; i < srcIdx.size(); i++) {
                GameInfo sInfo = srcIdx.get(i);
                GameInfo dInfo = scidIdx.get(i);
                assertEquals(sInfo.getWhite(), dInfo.getWhite(), label + " White[" + i + "]");
                assertEquals(sInfo.getBlack(), dInfo.getBlack(), label + " Black[" + i + "]");
                assertEquals(sInfo.getEvent(), dInfo.getEvent(), label + " Event[" + i + "]");
                assertEquals(sInfo.getSite(), dInfo.getSite(), label + " Site[" + i + "]");
                assertEquals(sInfo.getDate(), dInfo.getDate(), label + " Date[" + i + "]");
                assertEquals(sInfo.getRound(), dInfo.getRound(), label + " Round[" + i + "]");
                assertEquals(sInfo.getResult(), dInfo.getResult(), label + " Result[" + i + "]");
                assertEquals(sInfo.getEco(), dInfo.getEco(), label + " ECO[" + i + "]");
                assertEquals(sInfo.getWhiteElo(), dInfo.getWhiteElo(), label + " WhiteElo[" + i + "]");

                Game gSrc = srcDb.loadGame(sInfo);
                Game gDst = scidDb.loadGame(dInfo);
                assertEquals(gSrc.getHeader("Annotator"), gDst.getHeader("Annotator"), label + " Annotator[" + i + "]");
                assertEquals(gSrc.getHeader("Source"), gDst.getHeader("Source"), label + " Source[" + i + "]");

                String pgnSrc = printer.printGame(gSrc);
                String pgnDst = printer.printGame(gDst);
                assertEquals(pgnSrc, pgnDst, label + " PGN tree equivalence game " + i);
            }
        } finally {
            scidDb.close();
            srcDb.close();
        }
    }

    public static void readOnlyGuiGuardsAndWorkspaceTest() {
        System.out.println("Testing DialogSave, DialogDatabase, and Workspace read-only integration...");
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("jfxchess_ro_gui_test_");
            Path cbhBase = tempDir.resolve("ws_cbh");
            writeSyntheticCbhDatabase(cbhBase, 32, 26);

            Workspace ws = new Workspace();
            ChessDatabase viaCbh = ws.getOrCreateDatabase(Path.of(cbhBase.toString() + ".cbh"));
            ChessDatabase viaCbg = ws.getOrCreateDatabase(Path.of(cbhBase.toString() + ".cbg"));
            ChessDatabase viaCbp = ws.getOrCreateDatabase(Path.of(cbhBase.toString() + ".cbp"));
            assertTrue(viaCbh == viaCbg && viaCbh == viaCbp,
                    "Workspace must normalize .cbg/.cbp to the canonical .cbh instance");
            assertTrue(viaCbh.isReadOnly(), "Workspace database must be read-only");
            viaCbh.scanGames();

            // 1. DialogSave enablement tests
            DialogSave dlgNoDb = new DialogSave(null, true, false);
            assertButtonEnabled(dlgNoDb, "Save (Replace)", false);
            assertButtonEnabled(dlgNoDb, "Save as New (Append)", true);
            dlgNoDb.dispose();

            DialogSave dlgWritable = new DialogSave(null, true, true);
            assertButtonEnabled(dlgWritable, "Save (Replace)", true);
            assertButtonEnabled(dlgWritable, "Save as New (Append)", true);
            dlgWritable.dispose();

            DialogSave dlgReadOnly = new DialogSave(null, false, false);
            assertButtonEnabled(dlgReadOnly, "Save (Replace)", false);
            assertButtonEnabled(dlgReadOnly, "Save as New (Append)", false);
            dlgReadOnly.dispose();

            // 2. DialogDatabase title + Delete disabled for read-only DB
            DialogDatabase dlgDb = new DialogDatabase(null, viaCbh, null, null);
            assertTrue(dlgDb.getTitle().endsWith("[Read-Only]"),
                    "DialogDatabase title must show [Read-Only]: " + dlgDb.getTitle());
            assertButtonEnabled(dlgDb, "Delete Game", false);
            dlgDb.dispose();

            viaCbh.close();
            System.out.println("  readOnlyGuiGuardsAndWorkspaceTest PASSED.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            deleteDirRecursive(tempDir);
        }
    }

    // -------------------------------------------------------------------------
    // Synthetic Classic CBH Builder
    // -------------------------------------------------------------------------

    private static void writeSyntheticCbhDatabase(Path base, int entityHdrSize, int cbaHdrSize) throws IOException {
        // Players (.cbp, recSize=67)
        // [0]: Fischer, Bobby
        // [1]: Spassky, Boris  (with a -999 deleted entry at [2] to test deleted guard)
        byte[] cbp = buildCbhEntityFile(entityHdrSize, 67, 3, (buf, off, idx) -> {
            if (idx == 0) {
                putWin1252Fixed(buf, off + 9, 30, "Fischer");
                putWin1252Fixed(buf, off + 39, 20, "Bobby");
            } else if (idx == 1) {
                putWin1252Fixed(buf, off + 9, 30, "Spassky");
                putWin1252Fixed(buf, off + 39, 20, "Boris");
            } else {
                putI32LE(buf, off, -999);
                putWin1252Fixed(buf, off + 9, 30, "Deleted Player");
            }
        });
        Files.write(Path.of(base.toString() + ".cbp"), cbp);

        // Tournaments (.cbt, recSize=99)
        byte[] cbt = buildCbhEntityFile(entityHdrSize, 99, 1, (buf, off, idx) -> {
            putWin1252Fixed(buf, off + 9, 40, "Winter Cup");
            putWin1252Fixed(buf, off + 49, 30, "Zurich");
        });
        Files.write(Path.of(base.toString() + ".cbt"), cbt);

        // Annotators (.cbc, recSize=62)
        byte[] cbc = buildCbhEntityFile(entityHdrSize, 62, 1, (buf, off, idx) ->
                putWin1252Fixed(buf, off + 9, 45, "Test Annotator"));
        Files.write(Path.of(base.toString() + ".cbc"), cbc);

        // Sources (.cbs, recSize=68)
        byte[] cbs = buildCbhEntityFile(entityHdrSize, 68, 1, (buf, off, idx) ->
                putWin1252Fixed(buf, off + 9, 25, "Test Source"));
        Files.write(Path.of(base.toString() + ".cbs"), cbs);

        // Encode CBG game 0:
        // 1. e4 d5 2. exd5 (2...Nc6 [Side comment]) 2... e5 3. dxe6(EP) a5 4. e7 h5 5. e8=Q (0x29) b1=N+ (0x29)
        int[] encGame0 = buildCbgStandardGame0();
        // Encode CBG game 1 (Custom setup: K e1, R a1, P h5, k e8; wtm; WQ castle; 1. h6):
        byte[] g1Body = buildCbgCustomGame1();

        ByteArrayOutputStream cbgOut = new ByteArrayOutputStream();
        byte[] cbgHdr = new byte[26];
        putU16BE(cbgHdr, 0, 26);
        cbgOut.writeBytes(cbgHdr);

        long g0CbgOffset = cbgOut.size();
        for (int b : encGame0) {
            cbgOut.write(b);
        }
        long guidingCbgOffset = cbgOut.size();
        // Guiding text unencoded HTML block (0x80000000 | len)
        byte[] html = "<html>Guiding article</html>".getBytes(StandardCharsets.UTF_8);
        byte[] guidingBlock = new byte[4 + html.length];
        putI32BE(guidingBlock, 0, 0x80000000 | guidingBlock.length);
        System.arraycopy(html, 0, guidingBlock, 4, html.length);
        cbgOut.writeBytes(guidingBlock);

        long g1CbgOffset = cbgOut.size();
        cbgOut.writeBytes(g1Body);
        Files.write(Path.of(base.toString() + ".cbg"), cbgOut.toByteArray());

        // Encode CBA annotations for game 0:
        // Test pre-POP vs post-POP sequence alignment + multi-lang + figurines + 0x03/0x04/0x05!
        ByteArrayOutputStream cbaOut = new ByteArrayOutputStream();
        byte[] cbaHdr = new byte[cbaHdrSize];
        putU16BE(cbaHdr, 0, cbaHdrSize);
        cbaOut.writeBytes(cbaHdr);

        long g0CbaOffset = cbaOut.size();
        ByteArrayOutputStream g0Chunks = new ByteArrayOutputStream();
        // Root mvIdx=0: German (0x35) + Universal (0x00) -> Universal wins!
        writeCbaTextChunk(g0Chunks, 0xFFFFFF, 0x02, 0x35, "Vorbereitung".getBytes(CbCommon.WIN1252));
        writeCbaTextChunk(g0Chunks, 0, 0x02, 0x00, "Opening prep!".getBytes(CbCommon.WIN1252));
        // Move 1 (1.e4): French + Universal + English (0x2A) with figurine 0xA3 -> "Q", NAG $1 + $10 (0x0B), square e4, arrow e2->e4
        writeCbaTextChunk(g0Chunks, 1, 0x02, 0x31, "Fort".getBytes(CbCommon.WIN1252));
        writeCbaTextChunk(g0Chunks, 1, 0x02, 0x00, "Strong".getBytes(CbCommon.WIN1252));
        writeCbaTextChunk(g0Chunks, 1, 0x02, 0x2A,
                new byte[]{'S', 'h', 'a', 'r', 'p', ' ', '1', '.', (byte) 0xA3, 'f', '7'});
        writeCbaNagChunk(g0Chunks, 1, new byte[]{1, 0x0B});
        writeCbaSquareChunk(g0Chunks, 1, 2, 4, 3); // e4: sq0 = 4*8 + 3 = 35 -> sq1 = 36
        writeCbaArrowChunk(g0Chunks, 1, 2, 4, 1, 4, 3); // e2 -> e4
        // Sideline move 4 (2...Nc6, before POP): cbaKey = 4
        writeCbaTextChunk(g0Chunks, 4, 0x02, 0x00, "Side comment".getBytes(CbCommon.WIN1252));
        // Mainline 3.dxe6 (EP) is moveOnlySeq=7 AFTER POP (since variation has 2 moves 4,5 & 2...e5 is 6) -> cbaKey = 7 - 1 = 6!
        writeCbaTextChunk(g0Chunks, 6, 0x02, 0x2A, "En passant!".getBytes(CbCommon.WIN1252));

        byte[] chunkBytes = g0Chunks.toByteArray();
        byte[] cbaBlockHdr = new byte[14];
        putI32BE(cbaBlockHdr, 10, 14 + chunkBytes.length);
        cbaOut.writeBytes(cbaBlockHdr);
        cbaOut.writeBytes(chunkBytes);
        Files.write(Path.of(base.toString() + ".cba"), cbaOut.toByteArray());

        // Write .cbh with 4 records: [0]=g0, [1]=guiding(0x03), [2]=deleted(0x81), [3]=g1
        byte[] cbh = new byte[46 + 4 * 46];
        cbh[4] = 46;
        cbh[5] = 1;
        putI32BE(cbh, 6, 5);
        int datePacked = (2025 << 9) | (3 << 5) | 14;
        int ecoB01 = 102 << 7; // B01

        writeCbhRecord(cbh, 46, 0x01, g0CbgOffset, g0CbaOffset, 0, 1, 0, 0, 0, datePacked, 2, 3, 2, 2750, 2690, ecoB01);
        writeCbhRecord(cbh, 46 + 46, 0x03, guidingCbgOffset, 0, 0, 0, 0, 0, 0, datePacked, 3, 0, 0, 0, 0, 0);
        writeCbhRecord(cbh, 46 + 2 * 46, 0x81, g0CbgOffset, 0, 0, 1, 0, 0, 0, datePacked, 0, 0, 0, 0, 0, 0);
        writeCbhRecord(cbh, 46 + 3 * 46, 0x01, g1CbgOffset, 0, 1, 0, 0, 0, 0, datePacked, 1, 4, 0, 2600, 2600, 1 << 7);

        Files.write(Path.of(base.toString() + ".cbh"), cbh);
    }

    @FunctionalInterface
    private interface EntityPopulator {
        void populate(byte[] buf, int recOffset, int id);
    }

    private static byte[] buildCbhEntityFile(int hdrSize, int recSize, int count, EntityPopulator populator) {
        byte[] data = new byte[hdrSize + count * recSize];
        putI32LE(data, 0, count);
        putI32LE(data, 12, recSize - 9);
        if (hdrSize == 32) {
            putI32LE(data, 24, 4);
        }
        for (int i = 0; i < count; i++) {
            populator.populate(data, hdrSize + i * recSize, i);
        }
        return data;
    }

    private static void writeCbhRecord(
            byte[] cbh, int off, int flags, long cbgOff, long cbaOff,
            int whiteId, int blackId, int tourId, int annoId, int srcId,
            int datePacked, int res, int round, int subround, int wElo, int bElo, int eco) {
        cbh[off] = (byte) flags;
        putI32BE(cbh, off + 0x01, (int) cbgOff);
        putI32BE(cbh, off + 0x05, (int) cbaOff);
        putU24BE(cbh, off + 0x09, whiteId);
        putU24BE(cbh, off + 0x0C, blackId);
        putU24BE(cbh, off + 0x0F, tourId);
        putU24BE(cbh, off + 0x12, annoId);
        putU24BE(cbh, off + 0x15, srcId);
        putU24BE(cbh, off + 0x18, datePacked);
        cbh[off + 0x1B] = (byte) res;
        cbh[off + 0x1D] = (byte) round;
        cbh[off + 0x1E] = (byte) subround;
        putU16BE(cbh, off + 0x1F, wElo);
        putU16BE(cbh, off + 0x21, bElo);
        putU16BE(cbh, off + 0x23, eco);
    }

    private static int[] buildCbgStandardGame0() {
        // Legal game exercising castling-free legal moves, variation (2 moves before POP), SKIP, EP, W+B promotions:
        // 1. e4 (seq=1) d5 (seq=2) 2. exd5 (seq=3)
        // PUSH -> 2... Nc6 (seq=4) 3. Nc3 (N1 b1 +1,+2 = 0x3D, seq=5) -> POP
        // SKIP 0x9F
        // 2... e5 (seq=6) 3. dxe6 (EP, seq=7) b5 (seq=8) 4. e7 (seq=9) b4 (seq=10)
        // 5. exf8=Q+ (0x29: e7=38 -> f8=47: b1=47>>2=11, b2=((47&3)<<6)|38=192|38=230)
        // 5... Kxf8 (Ke8 +1,0 = 0xD8)? No, 5... Kd7 (Ke8 -1,-1 = 0xB1)
        // 6. Nc3 (N1 b1 +1,+2 = 0x3D) b3 (0x64)
        // 7. Nb1 (N1 c3 -1,-2 = 0xD4)? No wait clear b1: 7. Nb5 (N1 c3 -1,+2 = 0xFA) b2 (0x64)
        // 8. Qf3 (Qe8=f8 is 47 -> d6: Q1 -2,-2?! Let's keep Queen on f8: move Ke1 0,+1 -> Ke2 (0x49))
        // 8... b1=N (0x29: promo=1 N, b2=9 -> b1=8: b1=(1<<4)|(8>>2)=18, b2=9)
        List<Integer> dec = new ArrayList<>();
        dec.add(0xFF); // 1. e4
        dec.add(0x0B); // 1... d5
        dec.add(0x36); // 2. exd5
        dec.add(0xDC); // PUSH
        dec.add(0xDD); // 2... Nc6
        dec.add(0x3D); // 3. Nc3 (N1: +1, +2)
        dec.add(0x0C); // POP
        dec.add(0x9F); // SKIP
        dec.add(0xFF); // 2... e5
        dec.add(0x15); // 3. dxe6 (EP)
        dec.add(0x17); // 3... b5
        dec.add(0x84); // 4. e7
        dec.add(0x64); // 4... b4
        dec.add(0x29); // 5. exf8=Q+
        dec.add(0xA5); // SBOX[11] = Q1(0,+1) = 0xA5
        dec.add(0xA3); // SBOX[230] = 0xA3
        dec.add(0xB1); // 5... Kd7 (K: -1, -1)
        dec.add(0x3D); // 6. Nc3 (N1: +1, +2 -> frees b1!)
        dec.add(0x64); // 6... b3
        dec.add(0xFA); // 7. Nb5 (N1: -1, +2)
        dec.add(0x64); // 7... b2
        dec.add(0x49); // 8. Ke2 (K: 0, +1)
        dec.add(0x29); // 8... b1=N
        dec.add(0x79); // SBOX[18] = 0x79
        dec.add(0x76); // SBOX[9]  = 0x76
        dec.add(0x0C); // POP EOF

        return encryptCbg(dec, false, null);
    }

    private static byte[] buildCbgCustomGame1() {
        // Setup: K e1 (cm=32), R a1 (cm=0), P h5 (cm=60), k e8 (cm=39), wtm=0, castling=WQ(1), fullmove=1
        // Play 1. h5-h6 (slot 0 = Pa (0,+1)=0x2D)
        byte[] setup28 = new byte[28];
        setup28[0] = 0x01;
        setup28[1] = 0x00; // wtm, no EP
        setup28[2] = 0x01; // WQ castling
        setup28[3] = 0x01; // fullmove 1
        int bitPos = 0;
        for (int cm = 0; cm < 64; cm++) {
            int code = -1;
            if (cm == 0) {
                code = 0b10101; // W Rook a1
            } else if (cm == 32) {
                code = 0b10001; // W King e1
            } else if (cm == 39) {
                code = 0b11001; // B King e8
            } else if (cm == 60) {
                code = 0b10110; // W Pawn h5
            }
            if (code == -1) {
                bitPos++;
            } else {
                for (int b = 4; b >= 0; b--) {
                    writeBit(setup28, 4, bitPos++, (code >>> b) & 1);
                }
            }
        }
        List<Integer> dec = new ArrayList<>();
        dec.add(0x2D); // 1. h6 (Pa slot 0)
        dec.add(0x0C); // POP EOF
        int[] enc = encryptCbg(dec, true, setup28);
        byte[] out = new byte[enc.length];
        for (int i = 0; i < enc.length; i++) {
            out[i] = (byte) enc[i];
        }
        return out;
    }

    private static void writeBit(byte[] buf, int baseOff, int bitIdx, int val) {
        if (val != 0) {
            int byteIdx = baseOff + (bitIdx >>> 3);
            int shift = 7 - (bitIdx & 7);
            buf[byteIdx] |= (byte) (1 << shift);
        }
    }

    private static int[] encryptCbg(List<Integer> tokens, boolean customSetup, byte[] setup28) {
        ByteArrayOutputStream pay = new ByteArrayOutputStream();
        if (customSetup && setup28 != null) {
            pay.writeBytes(setup28);
        }
        int mc = 0;
        for (int i = 0; i < tokens.size(); i++) {
            int dec = tokens.get(i) & 0xFF;
            if (dec == 0x29) {
                int rawEsc = (0x29 + mc) & 0xFF;
                int raw1 = (tokens.get(i + 1) + mc) & 0xFF;
                int raw2 = (tokens.get(i + 2) + mc) & 0xFF;
                pay.write(rawEsc);
                pay.write(raw1);
                pay.write(raw2);
                mc = (mc + 1) & 0xFF;
                i += 2;
            } else if (dec == 0xDC || dec == 0x0C || dec == 0x9F) {
                pay.write((dec + mc) & 0xFF);
            } else {
                pay.write((dec + mc) & 0xFF);
                mc = (mc + 1) & 0xFF;
            }
        }
        byte[] body = pay.toByteArray();
        int totalLen = 4 + body.length;
        int hdr = totalLen;
        if (customSetup) {
            hdr |= 0x40000000;
        }
        int[] res = new int[totalLen];
        res[0] = (hdr >>> 24) & 0xFF;
        res[1] = (hdr >>> 16) & 0xFF;
        res[2] = (hdr >>> 8) & 0xFF;
        res[3] = hdr & 0xFF;
        for (int i = 0; i < body.length; i++) {
            res[4 + i] = body[i] & 0xFF;
        }
        return res;
    }

    private static void writeCbaTextChunk(ByteArrayOutputStream out, int mvIdx, int type, int lang, byte[] textBytes) {
        int len = 8 + textBytes.length;
        byte[] b = new byte[len];
        putU24BE(b, 0, mvIdx);
        b[3] = (byte) type;
        putU16BE(b, 4, len);
        b[6] = 0;
        b[7] = (byte) lang;
        System.arraycopy(textBytes, 0, b, 8, textBytes.length);
        out.writeBytes(b);
    }

    private static void writeCbaNagChunk(ByteArrayOutputStream out, int mvIdx, byte[] nags) {
        int len = 6 + nags.length;
        byte[] b = new byte[len];
        putU24BE(b, 0, mvIdx);
        b[3] = 0x03;
        putU16BE(b, 4, len);
        System.arraycopy(nags, 0, b, 6, nags.length);
        out.writeBytes(b);
    }

    private static void writeCbaSquareChunk(ByteArrayOutputStream out, int mvIdx, int color, int file, int rank) {
        byte[] b = new byte[8];
        putU24BE(b, 0, mvIdx);
        b[3] = 0x04;
        putU16BE(b, 4, 8);
        b[6] = (byte) color;
        b[7] = (byte) (file * 8 + rank + 1);
        out.writeBytes(b);
    }

    private static void writeCbaArrowChunk(
            ByteArrayOutputStream out, int mvIdx, int color, int fx, int fy, int tx, int ty) {
        byte[] b = new byte[9];
        putU24BE(b, 0, mvIdx);
        b[3] = 0x05;
        putU16BE(b, 4, 9);
        b[6] = (byte) color;
        b[7] = (byte) (fx * 8 + fy + 1);
        b[8] = (byte) (tx * 8 + ty + 1);
        out.writeBytes(b);
    }

    // -------------------------------------------------------------------------
    // Synthetic Modern 2CBH Builder
    // -------------------------------------------------------------------------

    private static void writeSyntheticTwoCbhDatabase(
            Path base, boolean populateSlot3, boolean truncateLastBucket) throws IOException {

        // 1. .2lid (184B BE header + 4,234B buckets)
        int[] slotSizes = {1024, 1120, 220, 1024, 314, 532};
        int stride = 4234;
        int buckets = 3; // Players: [0]=Alpha, [1]=Beta, [2]=Gamma
        int slot3Cnt;
        if (populateSlot3) {
            slot3Cnt = 1;
        } else {
            slot3Cnt = 0;
        }
        long[] slotCounts = {3, 1, 1, slot3Cnt, 0, 0};

        int totalSize = 184 + buckets * stride;
        if (truncateLastBucket) {
            // Trim trailing padding on the final bucket (just after Slot 0 data)
            totalSize = 184 + 2 * stride + 40;
        }
        byte[] lid = new byte[totalSize];
        putI32BE(lid, 0, 184);
        putI32BE(lid, 4, 6);
        for (int s = 0; s < 6; s++) {
            int doff = 8 + s * 20;
            putI32BE(lid, doff, slotSizes[s]);
            putI64BE(lid, doff + 4, slotCounts[s]);
            putI64BE(lid, doff + 12, -1L);
        }
        // Players (Slot 0 at +0)
        write2LidTwoStrings(lid, 184, "Alpha", "Alice");
        write2LidTwoStrings(lid, 184 + stride, "Beta", "Bob");
        write2LidTwoStrings(lid, 184 + 2 * stride, "Gamma", "Greg");
        // Tournament (Slot 1 at +1024): Place FIRST, Title SECOND!
        write2LidTwoStrings(lid, 184 + 1024, "London", "London Open");
        // Source (Slot 2 at +2144)
        write2LidTwoStrings(lid, 184 + 2144, "Custom Publisher Press", "Editor");
        if (populateSlot3) {
            write2LidTwoStrings(lid, 184 + 2364, "Slot3Annotator", "");
        }
        Files.write(Path.of(base.toString() + ".2lid"), lid);

        // 2. .2cbg
        // Game 0: 1. e4 e5 (2. Nf3) vs (2. Bc4 Nc6) -> encoded in 2CBG variation-before-mainline:
        //         w(e2e4), w(e7e5), w(Bf1c4), 0xFFFD, w(Nb8c6), 0xFFFF, w(Ng1f3)
        int wE4 = findTwoCbgMoveWord(4, 1, 4, 3, CONSTANTS.EMPTY);
        int wE5 = findTwoCbgMoveWord(4, 6, 4, 4, CONSTANTS.EMPTY);
        int wBc4 = findTwoCbgMoveWord(5, 0, 2, 3, CONSTANTS.EMPTY);
        int wNc6 = findTwoCbgMoveWord(1, 7, 2, 5, CONSTANTS.EMPTY);
        int wNf3 = findTwoCbgMoveWord(6, 0, 5, 2, CONSTANTS.EMPTY);

        ByteArrayOutputStream tcbgOut = new ByteArrayOutputStream();
        long g0CbgOff = tcbgOut.size();
        writeTwoCbgBlock(tcbgOut, new int[]{
            0x0001, 0xFFFC, wE4, wE5, wBc4, 0xFFFD, wNc6, 0xFFFF, wNf3
        });

        long guideCbgOff = tcbgOut.size();
        byte[] guidePay = "<!DOCTYPE HTML>".getBytes(StandardCharsets.UTF_8);
        writeTwoCbgRawBlock(tcbgOut, guidePay);

        // Game 1 custom setup: K e1, P e6, k b7 -> 1. e6-e7 Kb7-c7 2. e7-e8=N+
        long g1CbgOff = tcbgOut.size();
        int pieceWKingE1 = 0xC000 + 45 + 0 * 64 + (4 * 8 + 0);
        int pieceBKingB7 = 0xC000 + 45 + 5 * 64 + (1 * 8 + 6);
        int pieceWPawnE6 = 0xC000 + 685 + 4 * 6 + (5 - 1);
        int wE7 = findTwoCbgMoveWord(4, 5, 4, 6, CONSTANTS.EMPTY);
        int wKc7 = findTwoCbgMoveWord(1, 6, 2, 6, CONSTANTS.EMPTY);
        int wE8N = findTwoCbgMoveWord(4, 6, 4, 7, CONSTANTS.KNIGHT);
        writeTwoCbgBlock(tcbgOut, new int[]{
            0x0001, 0xFFFB, 1, 0, 0, pieceWKingE1, pieceBKingB7, pieceWPawnE6, 0xFFFC,
            wE7, wKc7, wE8N
        });
        Files.write(Path.of(base.toString() + ".2cbg"), tcbgOut.toByteArray());

        // 3. .2cba: dummy 16 bytes at offset 0 so valid annotations sit at > 12 (12 is empty sentinel)
        ByteArrayOutputStream tcbaOut = new ByteArrayOutputStream();
        tcbaOut.writeBytes(new byte[16]);
        long g0CbaOff = tcbaOut.size();
        ByteArrayOutputStream cbaBody = new ByteArrayOutputStream();
        writeU16LE(cbaBody, 0x2000);
        // Group mvIdx=0 (root): UTF-8 PUA figurines
        writeI32LE(cbaBody, 0);
        writeI32LE(cbaBody, 1);
        writeU16LE(cbaBody, 0x0002);
        write2CbaText(cbaBody, 0, "Root UF \uE022\uE023\uE024\uE025\uE026\uE027!".getBytes(StandardCharsets.UTF_8));
        // Group mvIdx=1 (1.e4): German + English Win-1252 pre-comment (0x0082) + 0x0018 fixed skip
        writeI32LE(cbaBody, 1);
        writeI32LE(cbaBody, 3);
        writeU16LE(cbaBody, 0x0018);
        cbaBody.write(1);
        writeU16LE(cbaBody, 0x0082);
        write2CbaText(cbaBody, 1, "Deutsch".getBytes(CbCommon.WIN1252));
        writeU16LE(cbaBody, 0x0082);
        write2CbaText(cbaBody, 0, new byte[]{'P', 'r', 'e', 'W', 'i', 'n', ' ', (byte) 0xA3, 'f', '4'});
        // Group mvIdx=4 (2.Bc4 in C_rev DFS: 0=root, 1=e4, 2=e5, 3=Nf3 [main], 4=Bc4, 5=Nc6)
        writeI32LE(cbaBody, 4);
        writeI32LE(cbaBody, 2);
        writeU16LE(cbaBody, 0x0013);
        writeDummyRefGame0013(cbaBody);
        writeU16LE(cbaBody, 0x0002);
        write2CbaText(cbaBody, 7, "After ref citation".getBytes(StandardCharsets.UTF_8));
        writeU16LE(cbaBody, 0xFFFF);
        writeTwoCbgRawBlock(tcbaOut, cbaBody.toByteArray());
        Files.write(Path.of(base.toString() + ".2cba"), tcbaOut.toByteArray());

        // 4. .2cbh
        byte[] tcbh = new byte[192 + 4 * 192];
        putU16LE(tcbh, 0x0A, 192);
        putI64LE(tcbh, 0x10, 5);
        int date = (2024 << 9) | (11 << 5) | 5;
        int ecoC42 = 243 << 7; // C42
        long g0AnnoId;
        if (populateSlot3) {
            g0AnnoId = 0;
        } else {
            g0AnnoId = 2;
        }
        writeTwoCbhRec(tcbh, 192, 0x01010001L, g0CbgOff, g0CbaOff, 0, 1, 0, g0AnnoId, 0, 2, 1, 0, 2800, 2780, ecoC42, date);
        writeTwoCbhRec(tcbh, 192 + 192, 0x03010003L, guideCbgOff, 12L, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, date);
        writeTwoCbhRec(tcbh, 192 + 2 * 192, 0x01010081L, g0CbgOff, 12L, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, date);
        writeTwoCbhRec(tcbh, 192 + 3 * 192, 0x01010001L, g1CbgOff, 12L, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, ecoC42, date);
        Files.write(Path.of(base.toString() + ".2cbh"), tcbh);
    }

    private static void write2LidTwoStrings(byte[] lid, int slotStart, String s1, String s2) {
        int pos = slotStart + 4;
        byte[] b1 = s1.getBytes(StandardCharsets.UTF_8);
        putI32LE(lid, pos, b1.length);
        System.arraycopy(b1, 0, lid, pos + 4, b1.length);
        pos += 4 + b1.length;
        byte[] b2 = s2.getBytes(StandardCharsets.UTF_8);
        putI32LE(lid, pos, b2.length);
        System.arraycopy(b2, 0, lid, pos + 4, b2.length);
    }

    private static int findTwoCbgMoveWord(int fx, int fy, int tx, int ty, int promo) {
        int wantFrom = (fy << 3) | fx;
        int wantTo = (ty << 3) | tx;
        for (int i = 1; i <= TwoCbhMoveTable.TABLE_SIZE; i++) {
            int p = TwoCbhMoveTable.MOVES[i];
            if (TwoCbhMoveTable.fromSq64(p) == wantFrom
                    && TwoCbhMoveTable.toSq64(p) == wantTo
                    && TwoCbhMoveTable.promoConst(p) == promo) {
                return i;
            }
        }
        throw new IllegalStateException("Move word not found: " + fx + "," + fy + "->" + tx + "," + ty);
    }

    private static void writeTwoCbgBlock(ByteArrayOutputStream out, int[] words) {
        byte[] pay = new byte[words.length * 2];
        for (int i = 0; i < words.length; i++) {
            pay[i * 2] = (byte) (words[i] & 0xFF);
            pay[i * 2 + 1] = (byte) ((words[i] >>> 8) & 0xFF);
        }
        writeTwoCbgRawBlock(out, pay);
    }

    private static void writeTwoCbgRawBlock(ByteArrayOutputStream out, byte[] pay) {
        byte[] hdr = new byte[24];
        putI64LE(hdr, 0, TwoCbhChessDatabase.TWO_CBG_MAGIC);
        putI32LE(hdr, 8, pay.length);
        putI32LE(hdr, 12, 100);
        out.writeBytes(hdr);
        out.writeBytes(pay);
    }

    private static void write2CbaText(ByteArrayOutputStream out, int lang, byte[] textBytes) {
        writeU16LE(out, 0);
        writeU16LE(out, lang);
        writeI32LE(out, textBytes.length);
        out.writeBytes(textBytes);
    }

    private static void writeDummyRefGame0013(ByteArrayOutputStream out) {
        out.writeBytes(new byte[10]);
        for (int s = 0; s < 6; s++) {
            out.write(3);
            out.writeBytes(new byte[]{'r', 'e', 'f'});
        }
        out.writeBytes(new byte[79]);
        out.writeBytes(new byte[]{1, 0, 1, 0, 0});
        writeI32LE(out, 2);
        out.writeBytes(new byte[]{'T', '1'});
        out.writeBytes(new byte[]{1, 0, 1, 0, 0});
        writeI32LE(out, 2);
        out.writeBytes(new byte[]{'T', '2'});
        out.writeBytes(new byte[29]);
        writeI32LE(out, 2); // 2 moves * 5 bytes
        out.writeBytes(new byte[10]);
        out.writeBytes(new byte[4]);
    }

    private static void writeTwoCbhRec(
            byte[] buf, int off, long flags, long cbgOff, long cbaOff,
            long whiteId, long blackId, long tourId, long annoId, long srcId,
            int res, int round, int subround, int wElo, int bElo, int eco, int datePacked) {
        putI64LE(buf, off, flags);
        putI64LE(buf, off + 0x08, cbgOff);
        putI64LE(buf, off + 0x10, cbaOff);
        putI64LE(buf, off + 0x18, whiteId);
        putI64LE(buf, off + 0x20, blackId);
        putI64LE(buf, off + 0x28, tourId);
        putI64LE(buf, off + 0x30, annoId);
        putI64LE(buf, off + 0x38, srcId);
        buf[off + 0x58] = (byte) res;
        buf[off + 0x5A] = (byte) round;
        buf[off + 0x5C] = (byte) subround;
        putU16LE(buf, off + 0x60, wElo);
        putU16LE(buf, off + 0x70, bElo);
        putU16LE(buf, off + 0x80, eco);
        putI32LE(buf, off + 0xBC, datePacked);
    }

    // -------------------------------------------------------------------------
    // Helpers & assertions
    // -------------------------------------------------------------------------

    private static String normalizeSpaces(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    private static void assertButtonEnabled(Container root, String text, boolean expected) {
        JButton btn = findButton(root, text);
        assertTrue(btn != null, "Button not found: " + text);
        assertEquals(expected, btn.isEnabled(), "Button '" + text + "' enabled state");
    }

    private static JButton findButton(Container c, String text) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JButton jb && text.equals(jb.getText())) {
                return jb;
            }
            if (comp instanceof Container child) {
                JButton hit = findButton(child, text);
                if (hit != null) {
                    return hit;
                }
            }
        }
        return null;
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws Exception;
    }

    private static void assertThrowsUnsupported(ThrowingAction action, String label) {
        try {
            action.run();
        } catch (UnsupportedOperationException expected) {
            return;
        } catch (Exception other) {
            throw new AssertionError(label + " threw unexpected " + other.getClass().getName(), other);
        }
        throw new AssertionError(label + " expected UnsupportedOperationException but succeeded");
    }

    private static void assertTrue(boolean cond, String msg) {
        if (!cond) {
            throw new AssertionError(msg);
        }
    }

    private static void assertEquals(Object expected, Object actual, String msg) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(msg + " (expected=<" + expected + "> actual=<" + actual + ">)");
        }
    }

    private static void deleteDirRecursive(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (var stream = Files.walk(dir)) {
            List<Path> paths = stream.sorted((a, b) -> b.compareTo(a)).toList();
            for (Path p : paths) {
                Files.deleteIfExists(p);
            }
        } catch (IOException ignored) {
        }
    }

    private static void putWin1252Fixed(byte[] buf, int off, int maxLen, String s) {
        byte[] bytes = s.getBytes(CbCommon.WIN1252);
        int len = Math.min(bytes.length, maxLen);
        System.arraycopy(bytes, 0, buf, off, len);
    }

    private static void putI32LE(byte[] buf, int off, int v) {
        ByteBuffer.wrap(buf, off, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(v);
    }

    private static void putU16LE(byte[] buf, int off, int v) {
        buf[off] = (byte) (v & 0xFF);
        buf[off + 1] = (byte) ((v >>> 8) & 0xFF);
    }

    private static void putI64LE(byte[] buf, int off, long v) {
        ByteBuffer.wrap(buf, off, 8).order(ByteOrder.LITTLE_ENDIAN).putLong(v);
    }

    private static void putI32BE(byte[] buf, int off, int v) {
        ByteBuffer.wrap(buf, off, 4).order(ByteOrder.BIG_ENDIAN).putInt(v);
    }

    private static void putI64BE(byte[] buf, int off, long v) {
        ByteBuffer.wrap(buf, off, 8).order(ByteOrder.BIG_ENDIAN).putLong(v);
    }

    private static void putU24BE(byte[] buf, int off, int v) {
        buf[off] = (byte) ((v >>> 16) & 0xFF);
        buf[off + 1] = (byte) ((v >>> 8) & 0xFF);
        buf[off + 2] = (byte) (v & 0xFF);
    }

    private static void putU16BE(byte[] buf, int off, int v) {
        buf[off] = (byte) ((v >>> 8) & 0xFF);
        buf[off + 1] = (byte) (v & 0xFF);
    }

    private static void writeU16LE(ByteArrayOutputStream out, int v) {
        out.write(v & 0xFF);
        out.write((v >>> 8) & 0xFF);
    }

    private static void writeI32LE(ByteArrayOutputStream out, int v) {
        out.write(v & 0xFF);
        out.write((v >>> 8) & 0xFF);
        out.write((v >>> 16) & 0xFF);
        out.write((v >>> 24) & 0xFF);
    }
}
