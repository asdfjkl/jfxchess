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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) {

        if (args.length == 0) {
            boolean success = runAllTests();
            if (!success) {
                System.exit(1);
            }
            return;
        }

        boolean runAll = false;
        boolean runLogic = false;
        boolean runPgn = false;
        boolean runSession = false;
        boolean runPerft = false;
        boolean runScid = false;
        boolean runCbh = false;
        boolean hasOption = false;

        for (String arg : args) {
            if ("--help".equals(arg) || "-h".equals(arg) || "help".equals(arg)) {
                printHelp();
                return;
            } else if ("--all-tests".equals(arg) || "--all".equals(arg) || "run-all-tests".equals(arg)) {
                runAll = true;
                hasOption = true;
            } else if ("--logic-tests".equals(arg) || "--logic".equals(arg)) {
                runLogic = true;
                hasOption = true;
            } else if ("--pgn-tests".equals(arg) || "--pgn".equals(arg)) {
                runPgn = true;
                hasOption = true;
            } else if ("--run-session-tests".equals(arg) || "--session-tests".equals(arg) || "--session".equals(arg)) {
                runSession = true;
                hasOption = true;
            } else if ("--perft".equals(arg)) {
                runPerft = true;
                hasOption = true;
            } else if ("--scid-tests".equals(arg) || "--scid".equals(arg) || "--scid5-tests".equals(arg)) {
                runScid = true;
                hasOption = true;
            } else if ("--cbh-tests".equals(arg) || "--cbh".equals(arg)) {
                runCbh = true;
                hasOption = true;
            }
        }

        if (hasOption) {
            TestCases cases = new TestCases();
            Map<String, Runnable> selectedTests = new LinkedHashMap<>();

            if (runAll || runLogic) {
                selectedTests.put("fenTest", cases::fenTest);
                selectedTests.put("runBitSetTest", cases::runBitSetTest);
                selectedTests.put("runSanTest", cases::runSanTest);
                selectedTests.put("targetedSanDisambiguationTest", cases::targetedSanDisambiguationTest);
                selectedTests.put("sanResolverRobustnessAndNeverCrashTest", cases::sanResolverRobustnessAndNeverCrashTest);
                selectedTests.put("runZobristTest", cases::runZobristTest);
                selectedTests.put("gameTreeVariationManipulationTest", cases::gameTreeVariationManipulationTest);
                selectedTests.put("uciPositionGenerationCustomFenTest", cases::uciPositionGenerationCustomFenTest);
            }

            if (runAll || runPgn) {
                selectedTests.put("runPgnPrintTest", cases::runPgnPrintTest);
                selectedTests.put("readGamesByStringTest", cases::readGamesByStringTest);
                selectedTests.put("pgnReadGameTest", cases::pgnReadGameTest);
                selectedTests.put("pgnReadMiddleGTest", cases::pgnReadMiddleGTest);
                selectedTests.put("pgnScanTest", cases::pgnScanTest);
                selectedTests.put("pgnReadSingleEntryTestOpenClose", cases::pgnReadSingleEntryTestOpenClose);
                selectedTests.put("pgnReadSingleEntryTestSeekWithinRAF", cases::pgnReadSingleEntryTestSeekWithinRAF);
                selectedTests.put("pgnReadAllMillBaseTest", cases::pgnReadAllMillBaseTest);
                selectedTests.put("runPosHashTest", cases::runPosHashTest);
                selectedTests.put("pgnMoveAmbiguityUTFTest", cases::pgnMoveAmbiguityUTFTest);
                selectedTests.put("pgnMiddleGReadWriteCompareTest", cases::pgnMiddleGReadWriteCompareTest);
                selectedTests.put("pgnGameInfoSurnameExtractionTest", cases::pgnGameInfoSurnameExtractionTest);
                selectedTests.put("corruptedPgnSafeRecoveryTest", cases::corruptedPgnSafeRecoveryTest);
            }

            if (runAll || runSession) {
                selectedTests.put("workspaceSessionIsolationTest", cases::workspaceSessionIsolationTest);
                selectedTests.put("pgnDocumentSessionSynchronizationTest", cases::pgnDocumentSessionSynchronizationTest);
                selectedTests.put("workspaceCrossSessionSyncAndStaleTest", cases::workspaceCrossSessionSyncAndStaleTest);
                selectedTests.put("chessDatabaseSessionSynchronizationTest", cases::chessDatabaseSessionSynchronizationTest);
                selectedTests.put("databaseLifecycleAndPersistenceTest", cases::databaseLifecycleAndPersistenceTest);
                selectedTests.put("browserTabBehaviorTest", cases::browserTabBehaviorTest);
                selectedTests.put("databaseMenuActionStateTest", cases::databaseMenuActionStateTest);
                selectedTests.put("databaseSequentialNavigationTest", cases::databaseSequentialNavigationTest);
                selectedTests.put("databasePositionSearchTest", SearchPositionTest::runAll);
                selectedTests.put("bookSanDisplayTest", cases::bookSanDisplayTest);
                selectedTests.put("highDpiScaleSplitTest", cases::highDpiScaleSplitTest);
                selectedTests.put("wdlRendererTest", cases::wdlRendererTest);
                selectedTests.put("bookViewLayoutTest", cases::bookViewLayoutTest);
                selectedTests.put("tabBarScalingAndIconTest", cases::tabBarScalingAndIconTest);
                selectedTests.put("splashAssetPackagingTest", cases::splashAssetPackagingTest);
            }

            if (runAll || runScid) {
                selectedTests.put("scid5ReadSampleDatabaseIndexTest", cases::scid5ReadSampleDatabaseIndexTest);
                selectedTests.put("scid5LoadGamesTest", cases::scid5LoadGamesTest);
                selectedTests.put("scid5RoundTripEncodeDecodeTest", cases::scid5RoundTripEncodeDecodeTest);
                selectedTests.put("scid5WriteOperationsTest", cases::scid5WriteOperationsTest);
                selectedTests.put("scid5SearchTest", cases::scid5SearchTest);
                selectedTests.put("databaseImportGamesTest", cases::databaseImportGamesTest);
            }

            if (runAll || runCbh) {
                selectedTests.put("syntheticCbhStandardAndFeaturesTest", TestCbhSupport::syntheticCbhStandardAndFeaturesTest);
                selectedTests.put("syntheticTwoCbhAnd2LidEdgeCasesTest", TestCbhSupport::syntheticTwoCbhAnd2LidEdgeCasesTest);
                selectedTests.put("syntheticCbhAndTwoCbhToScid5RoundTripTest", TestCbhSupport::syntheticCbhAndTwoCbhToScid5RoundTripTest);
                selectedTests.put("readOnlyGuiGuardsAndWorkspaceTest", TestCbhSupport::readOnlyGuiGuardsAndWorkspaceTest);
                selectedTests.put("cbhWriteOperationsAndRoundTripTest", TestCbhSupport::cbhWriteOperationsAndRoundTripTest);
                selectedTests.put("twoCbhWriteOperationsAndRoundTripTest", TestCbhSupport::twoCbhWriteOperationsAndRoundTripTest);
                selectedTests.put("cbhAndTwoCbhCrossFormatImportTest", TestCbhSupport::cbhAndTwoCbhCrossFormatImportTest);
                selectedTests.put("cbhAndTwoCbhWorkspaceSessionSyncTest", TestCbhSupport::cbhAndTwoCbhWorkspaceSessionSyncTest);
            }

            if (runAll || runPerft) {
                selectedTests.put("runPerfT", cases::runPerfT);
            }

            String suiteName;
            if (runAll) {
                suiteName = "All Tests";
            } else {
                ArrayList<String> parts = new ArrayList<>();
                if (runLogic) parts.add("Logic Tests");
                if (runPgn) parts.add("PGN Tests");
                if (runSession) parts.add("Session Tests");
                if (runScid) parts.add("SCID5 Tests");
                if (runCbh) {
                    parts.add("CBH/2CBH Tests");
                }
                if (runPerft) parts.add("Perft Tests");
                suiteName = String.join(", ", parts);
            }

            boolean success = runTests(suiteName, selectedTests);
            if (!success) {
                System.exit(1);
            }
            System.exit(0);
        }

        // Support legacy single-test argument triggers or file execution
        String firstArg = args[0];
        if ("database-lifecycle-test".equals(firstArg)) {
            new TestCases().databaseLifecycleAndPersistenceTest();
            System.exit(0);
        } else if ("workspace-session-isolation-test".equals(firstArg)) {
            new TestCases().workspaceSessionIsolationTest();
            System.exit(0);
        } else if ("pgn-document-session-synchronization-test".equals(firstArg)) {
            new TestCases().pgnDocumentSessionSynchronizationTest();
            System.exit(0);
        } else if ("chess-database-test".equals(firstArg)) {
            new TestCases().chessDatabaseSessionSynchronizationTest();
            System.exit(0);
        } else if ("pgn-game-info-surname-test".equals(firstArg)) {
            new TestCases().pgnGameInfoSurnameExtractionTest();
            System.exit(0);
        } else if ("browser-tab-behavior-test".equals(firstArg)) {
            new TestCases().browserTabBehaviorTest();
            System.exit(0);
        } else if ("database-menu-action-state-test".equals(firstArg)) {
            new TestCases().databaseMenuActionStateTest();
            System.exit(0);
        } else if ("game-tree-variation-test".equals(firstArg)) {
            new TestCases().gameTreeVariationManipulationTest();
            System.exit(0);
        } else if ("workspace-cross-session-sync-test".equals(firstArg)) {
            new TestCases().workspaceCrossSessionSyncAndStaleTest();
            System.exit(0);
        } else if ("database-sequential-nav-test".equals(firstArg)) {
            new TestCases().databaseSequentialNavigationTest();
            System.exit(0);
        } else if ("database-position-search-test".equals(firstArg)) {
            SearchPositionTest.runAll();
            System.exit(0);
        } else if ("uci-position-custom-fen-test".equals(firstArg)) {
            new TestCases().uciPositionGenerationCustomFenTest();
            System.exit(0);
        } else if ("targeted-san-test".equals(firstArg)) {
            new TestCases().targetedSanDisambiguationTest();
            System.exit(0);
        } else if ("san-robustness-test".equals(firstArg)) {
            new TestCases().sanResolverRobustnessAndNeverCrashTest();
            System.exit(0);
        } else if ("corrupted-pgn-test".equals(firstArg)) {
            new TestCases().corruptedPgnSafeRecoveryTest();
            System.exit(0);
        } else if ("database-import-games-test".equals(firstArg)) {
            new TestCases().databaseImportGamesTest();
            System.exit(0);
        } else if ("cbh-tests".equals(firstArg)) {
            TestCbhSupport.runAll();
            System.exit(0);
        } else if ("book-san-test".equals(firstArg)) {
            new TestCases().bookSanDisplayTest();
            System.exit(0);
        } else if ("high-dpi-scale-split-test".equals(firstArg)) {
            new TestCases().highDpiScaleSplitTest();
            System.exit(0);
        } else if ("wdl-renderer-test".equals(firstArg)) {
            new TestCases().wdlRendererTest();
            System.exit(0);
        } else if ("book-view-layout-test".equals(firstArg)) {
            new TestCases().bookViewLayoutTest();
            System.exit(0);
        } else if ("tab-bar-scaling-test".equals(firstArg)) {
            new TestCases().tabBarScalingAndIconTest();
            System.exit(0);
        } else if ("splash-asset-packaging-test".equals(firstArg)) {
            new TestCases().splashAssetPackagingTest();
            System.exit(0);
        } else {
            boolean printOutput = true;
            if (args.length > 1 && "noout".equals(args[1])) {
                printOutput = false;
            }
            processPgnFile(firstArg, printOutput);
        }
    }

    public static void printHelp() {
        System.out.println("Usage:");
        System.out.println("  java -jar jchesslib.jar [OPTIONS]");
        System.out.println("  java -jar jchesslib.jar <pgn-file> [noout]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  (no parameters)      Run all tests");
        System.out.println("  --all-tests          Run all tests (logic, pgn, session, scid5, perft)");
        System.out.println("  --perft              Run move generation perft tests (runPerfT)");
        System.out.println("  --logic-tests        Run core logic tests (fenTest, runBitSetTest, runSanTest, runZobristTest, tree, uci)");
        System.out.println("  --pgn-tests          Run all tests involving PGN");
        System.out.println("  --run-session-tests  Run session/database tests (workspace, pgn document, chess database, browser tab, seq nav)");
        System.out.println("  --scid-tests         Run tests for SCID5 database support");
        System.out.println("  --help, -h           Display this help message and exit");
    }

    public static boolean runAllTests() {
        TestCases cases = new TestCases();
        Map<String, Runnable> tests = new LinkedHashMap<>();

        // Logic tests
        tests.put("fenTest", cases::fenTest);
        tests.put("runBitSetTest", cases::runBitSetTest);
        tests.put("runSanTest", cases::runSanTest);
        tests.put("targetedSanDisambiguationTest", cases::targetedSanDisambiguationTest);
        tests.put("sanResolverRobustnessAndNeverCrashTest", cases::sanResolverRobustnessAndNeverCrashTest);
        tests.put("runZobristTest", cases::runZobristTest);
        tests.put("gameTreeVariationManipulationTest", cases::gameTreeVariationManipulationTest);
        tests.put("uciPositionGenerationCustomFenTest", cases::uciPositionGenerationCustomFenTest);

        // PGN tests
        tests.put("runPgnPrintTest", cases::runPgnPrintTest);
        tests.put("readGamesByStringTest", cases::readGamesByStringTest);
        tests.put("pgnReadGameTest", cases::pgnReadGameTest);
        tests.put("pgnReadMiddleGTest", cases::pgnReadMiddleGTest);
        tests.put("pgnScanTest", cases::pgnScanTest);
        tests.put("pgnReadSingleEntryTestOpenClose", cases::pgnReadSingleEntryTestOpenClose);
        tests.put("pgnReadSingleEntryTestSeekWithinRAF", cases::pgnReadSingleEntryTestSeekWithinRAF);
        tests.put("pgnReadAllMillBaseTest", cases::pgnReadAllMillBaseTest);
        tests.put("runPosHashTest", cases::runPosHashTest);
        tests.put("pgnMoveAmbiguityUTFTest", cases::pgnMoveAmbiguityUTFTest);
        tests.put("pgnMiddleGReadWriteCompareTest", cases::pgnMiddleGReadWriteCompareTest);
        tests.put("pgnGameInfoSurnameExtractionTest", cases::pgnGameInfoSurnameExtractionTest);
        tests.put("corruptedPgnSafeRecoveryTest", cases::corruptedPgnSafeRecoveryTest);

        // SCID5 tests
        tests.put("scid5ReadSampleDatabaseIndexTest", cases::scid5ReadSampleDatabaseIndexTest);
        tests.put("scid5LoadGamesTest", cases::scid5LoadGamesTest);
        tests.put("scid5RoundTripEncodeDecodeTest", cases::scid5RoundTripEncodeDecodeTest);
        tests.put("scid5WriteOperationsTest", cases::scid5WriteOperationsTest);
        tests.put("scid5SearchTest", cases::scid5SearchTest);

        // Session tests
        tests.put("workspaceSessionIsolationTest", cases::workspaceSessionIsolationTest);
        tests.put("pgnDocumentSessionSynchronizationTest", cases::pgnDocumentSessionSynchronizationTest);
        tests.put("workspaceCrossSessionSyncAndStaleTest", cases::workspaceCrossSessionSyncAndStaleTest);
        tests.put("chessDatabaseSessionSynchronizationTest", cases::chessDatabaseSessionSynchronizationTest);
        tests.put("browserTabBehaviorTest", cases::browserTabBehaviorTest);
        tests.put("databaseLifecycleAndPersistenceTest", cases::databaseLifecycleAndPersistenceTest);
        tests.put("databaseMenuActionStateTest", cases::databaseMenuActionStateTest);
        tests.put("databaseSequentialNavigationTest", cases::databaseSequentialNavigationTest);
        tests.put("databasePositionSearchTest", SearchPositionTest::runAll);
        tests.put("bookSanDisplayTest", cases::bookSanDisplayTest);
        tests.put("highDpiScaleSplitTest", cases::highDpiScaleSplitTest);
        tests.put("wdlRendererTest", cases::wdlRendererTest);
        tests.put("bookViewLayoutTest", cases::bookViewLayoutTest);
        tests.put("tabBarScalingAndIconTest", cases::tabBarScalingAndIconTest);
        tests.put("splashAssetPackagingTest", cases::splashAssetPackagingTest);

        // CBH / 2CBH tests
        tests.put("syntheticCbhStandardAndFeaturesTest", TestCbhSupport::syntheticCbhStandardAndFeaturesTest);
        tests.put("syntheticTwoCbhAnd2LidEdgeCasesTest", TestCbhSupport::syntheticTwoCbhAnd2LidEdgeCasesTest);
        tests.put("syntheticCbhAndTwoCbhToScid5RoundTripTest", TestCbhSupport::syntheticCbhAndTwoCbhToScid5RoundTripTest);
        tests.put("readOnlyGuiGuardsAndWorkspaceTest", TestCbhSupport::readOnlyGuiGuardsAndWorkspaceTest);
        tests.put("cbhWriteOperationsAndRoundTripTest", TestCbhSupport::cbhWriteOperationsAndRoundTripTest);
        tests.put("twoCbhWriteOperationsAndRoundTripTest", TestCbhSupport::twoCbhWriteOperationsAndRoundTripTest);
        tests.put("cbhAndTwoCbhCrossFormatImportTest", TestCbhSupport::cbhAndTwoCbhCrossFormatImportTest);
        tests.put("cbhAndTwoCbhWorkspaceSessionSyncTest", TestCbhSupport::cbhAndTwoCbhWorkspaceSessionSyncTest);

        // Perft tests
        tests.put("runPerfT", cases::runPerfT);

        return runTests("All Tests", tests);
    }

    public static boolean runTests(String suiteName, Map<String, Runnable> tests) {
        System.out.println("================================================================================");
        System.out.println("               JFXChess Library - Test Suite: " + suiteName);
        System.out.println("================================================================================");

        int total = tests.size();
        int passed = 0;
        int failed = 0;
        long totalStartTime = System.currentTimeMillis();
        Map<String, String> results = new LinkedHashMap<>();

        int current = 0;
        for (Map.Entry<String, Runnable> entry : tests.entrySet()) {
            current++;
            String testName = entry.getKey();
            Runnable testMethod = entry.getValue();

            System.out.println(String.format("\n[%d/%d] RUNNING: %s", current, total, testName));
            System.out.println("--------------------------------------------------------------------------------");
            long start = System.currentTimeMillis();
            try {
                testMethod.run();
                long elapsed = System.currentTimeMillis() - start;
                passed++;
                results.put(testName, String.format("pass (%d ms)", elapsed));
                System.out.println(String.format("--> %s: pass (%d ms)", testName, elapsed));
            } catch (Throwable t) {
                long elapsed = System.currentTimeMillis() - start;
                failed++;
                results.put(testName, String.format("FAIL: %s (%d ms)", t.getMessage(), elapsed));
                System.err.println(String.format("--> %s: FAIL (%d ms)", testName, elapsed));
                t.printStackTrace();
            }
        }

        long totalElapsed = System.currentTimeMillis() - totalStartTime;

        System.out.println("\n================================================================================");
        System.out.println("                              TEST EXECUTION REPORT                             ");
        System.out.println("================================================================================");
        for (Map.Entry<String, String> res : results.entrySet()) {
            System.out.println(String.format(" - %-42s : %s", res.getKey(), res.getValue()));
        }
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println(String.format(" Total Tests : %d", total));
        System.out.println(String.format(" pass        : %d", passed));
        System.out.println(String.format(" FAIL        : %d", failed));
        System.out.println(String.format(" Total Time  : %.2f s", totalElapsed / 1000.0));
        System.out.println("================================================================================");

        return failed == 0;
    }

    private static void processPgnFile(String filename, boolean printOutput) {
        try {
            PgnChessDatabase db = new PgnChessDatabase();
            db.open(filename);
            db.scanGames();
            PgnPrinter printer = new PgnPrinter();
            ArrayList<GameInfo> index = db.getIndex();
            for (int i = 0; i < index.size(); i++) {
                try {
                    Game g = db.loadGame(index.get(i));
                    if (printOutput) {
                        System.out.println(printer.printGame(g));
                        System.out.println("\n");
                    }
                } catch (IOException e) {
                    System.out.println("error reading: " + filename);
                    System.out.println("game nr......: " + i);
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.out.println("error opening database: " + filename);
            e.printStackTrace();
        }
    }
}
