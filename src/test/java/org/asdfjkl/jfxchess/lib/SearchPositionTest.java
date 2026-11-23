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

import static org.asdfjkl.jfxchess.lib.CONSTANTS.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class SearchPositionTest {

    public static void runAll() {
        SearchPositionTest test = new SearchPositionTest();
        test.testBoardMatchingExact();
        test.testBoardMatchingWildcards();
        test.testBoardMatchingTurn();
        test.testMatchesPositionInGame();
        test.testPgnDatabasePositionSearch();
        test.testScid5PreFilter();
        test.testScid5DatabasePositionSearch();
        test.testPgnStreamingConsumer();
        test.testScidStreamingImport();
        System.out.println("ALL SEARCH POSITION TESTS PASSED!");
    }

    public void testBoardMatchingExact() {
        System.out.println("TEST: SearchPattern exact board matching");
        Board startBoard = new Board(true);
        SearchPattern pattern = new SearchPattern();
        pattern.setSearchPosition(true);
        pattern.setSearchBoard(new Board(true)); // default starting board
        pattern.setSearchTurn(SearchPattern.TURN_ANY);

        if (!pattern.matchesBoard(startBoard)) {
            throw new AssertionError("Starting board should match identical pattern board");
        }

        // Alter candidate board square e4 (col 4, row 3) to white pawn
        Board candidate = new Board(true);
        candidate.setPieceAt(4, 3, WHITE_PAWN);
        if (pattern.matchesBoard(candidate)) {
            throw new AssertionError("Candidate with pawn on e4 should not match starting board");
        }

        // Search pattern with pawn on e4 should match candidate
        SearchPattern p2 = new SearchPattern();
        p2.setSearchPosition(true);
        Board b2 = new Board(true);
        b2.setPieceAt(4, 3, WHITE_PAWN);
        p2.setSearchBoard(b2);
        p2.setSearchTurn(SearchPattern.TURN_ANY);
        if (!p2.matchesBoard(candidate)) {
            throw new AssertionError("Board with pawn on e4 should match pattern with pawn on e4");
        }
        System.out.println("  ... pass");
    }

    public void testBoardMatchingWildcards() {
        System.out.println("TEST: SearchPattern wildcard piece matching (WHITE_ANY_PIECE & BLACK_ANY_PIECE)");
        SearchPattern pattern = new SearchPattern();
        pattern.setSearchPosition(true);

        // Blank board except d4 has WHITE_ANY_PIECE and d5 has BLACK_ANY_PIECE
        Board searchBoard = new Board(false);
        searchBoard.setPieceAt(3, 3, WHITE_ANY_PIECE); // d4 (col 3, row 3)
        searchBoard.setPieceAt(3, 4, BLACK_ANY_PIECE); // d5 (col 3, row 4)
        pattern.setSearchBoard(searchBoard);
        pattern.setSearchTurn(SearchPattern.TURN_ANY);

        int[] whitePieces = {WHITE_PAWN, WHITE_KNIGHT, WHITE_BISHOP, WHITE_ROOK, WHITE_QUEEN, WHITE_KING};
        int[] blackPieces = {BLACK_PAWN, BLACK_KNIGHT, BLACK_BISHOP, BLACK_ROOK, BLACK_QUEEN, BLACK_KING};

        for (int wp : whitePieces) {
            for (int bp : blackPieces) {
                Board candidate = new Board(false);
                candidate.setPieceAt(3, 3, wp);
                candidate.setPieceAt(3, 4, bp);
                if (!pattern.matchesBoard(candidate)) {
                    throw new AssertionError("WHITE_ANY_PIECE and BLACK_ANY_PIECE should match piece IDs " + wp + ", " + bp);
                }
            }
        }

        // Opposite colors must fail
        Board candidateWrongColors = new Board(false);
        candidateWrongColors.setPieceAt(3, 3, BLACK_PAWN);
        candidateWrongColors.setPieceAt(3, 4, WHITE_PAWN);
        if (pattern.matchesBoard(candidateWrongColors)) {
            throw new AssertionError("WHITE_ANY_PIECE should not match BLACK_PAWN, BLACK_ANY_PIECE should not match WHITE_PAWN");
        }

        // Empty square where any piece is expected must fail
        Board candidateEmpty = new Board(false);
        if (pattern.matchesBoard(candidateEmpty)) {
            throw new AssertionError("Empty square should not match ANY_PIECE");
        }

        System.out.println("  ... pass");
    }

    public void testBoardMatchingTurn() {
        System.out.println("TEST: SearchPattern turn matching (TURN_WHITE, TURN_BLACK, TURN_ANY)");
        SearchPattern pattern = new SearchPattern();
        pattern.setSearchPosition(true);
        Board startBoard = new Board(true);
        pattern.setSearchBoard(startBoard);

        Board whiteTurnCandidate = new Board(true);
        whiteTurnCandidate.turn = WHITE;

        Board blackTurnCandidate = new Board(true);
        blackTurnCandidate.turn = BLACK;

        pattern.setSearchTurn(SearchPattern.TURN_ANY);
        if (!pattern.matchesBoard(whiteTurnCandidate) || !pattern.matchesBoard(blackTurnCandidate)) {
            throw new AssertionError("TURN_ANY should match regardless of candidate turn");
        }

        pattern.setSearchTurn(SearchPattern.TURN_WHITE);
        if (!pattern.matchesBoard(whiteTurnCandidate)) {
            throw new AssertionError("TURN_WHITE should match white candidate turn");
        }
        if (pattern.matchesBoard(blackTurnCandidate)) {
            throw new AssertionError("TURN_WHITE should NOT match black candidate turn");
        }

        pattern.setSearchTurn(SearchPattern.TURN_BLACK);
        if (pattern.matchesBoard(whiteTurnCandidate)) {
            throw new AssertionError("TURN_BLACK should NOT match white candidate turn");
        }
        if (!pattern.matchesBoard(blackTurnCandidate)) {
            throw new AssertionError("TURN_BLACK should match black candidate turn");
        }

        System.out.println("  ... pass");
    }

    public void testMatchesPositionInGame() {
        System.out.println("TEST: SearchPattern.matchesPositionInGame game tree traversal");
        PgnReader reader = new PgnReader();
        Game game;
        try {
            String pgnContent = java.nio.file.Files.readString(java.nio.file.Path.of(TestCases.getPgnPath("test_pgn_01.pgn")));
            game = reader.readGame(pgnContent);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (game == null) {
            throw new AssertionError("Failed to load test_pgn_01.pgn");
        }

        // 1. Initial board should match (root node)
        SearchPattern startPattern = new SearchPattern();
        startPattern.setSearchPosition(true);
        startPattern.setSearchBoard(new Board(true));
        startPattern.setSearchTurn(SearchPattern.TURN_ANY);
        if (!startPattern.matchesPositionInGame(game)) {
            throw new AssertionError("Starting board should match in Morphy game");
        }

        // 2. Final position (17. Rd8#)
        Board finalBoard = game.getEndNode().getBoard();
        SearchPattern finalPattern = new SearchPattern();
        finalPattern.setSearchPosition(true);
        finalPattern.setSearchBoard(finalBoard);
        finalPattern.setSearchTurn(SearchPattern.TURN_ANY);
        if (!finalPattern.matchesPositionInGame(game)) {
            throw new AssertionError("Final position should match in Morphy game");
        }

        // 3. Final position with WHITE_ANY_PIECE on d8 instead of White Rook
        Board wildcardBoard = finalBoard.makeCopy();
        wildcardBoard.setPieceAt(3, 7, WHITE_ANY_PIECE); // d8 is col 3, row 7
        SearchPattern wildcardPattern = new SearchPattern();
        wildcardPattern.setSearchPosition(true);
        wildcardPattern.setSearchBoard(wildcardBoard);
        wildcardPattern.setSearchTurn(SearchPattern.TURN_ANY);
        if (!wildcardPattern.matchesPositionInGame(game)) {
            throw new AssertionError("Final position with WHITE_ANY_PIECE on d8 should match Morphy game");
        }

        // 4. Final position with wrong piece (BLACK_ROOK on d8) should NOT match
        Board wrongBoard = finalBoard.makeCopy();
        wrongBoard.setPieceAt(3, 7, BLACK_ROOK);
        SearchPattern wrongPattern = new SearchPattern();
        wrongPattern.setSearchPosition(true);
        wrongPattern.setSearchBoard(wrongBoard);
        wrongPattern.setSearchTurn(SearchPattern.TURN_ANY);
        if (wrongPattern.matchesPositionInGame(game)) {
            throw new AssertionError("Final position with BLACK_ROOK on d8 should NOT match");
        }

        // 5. Test with searchTurn restriction
        finalPattern.setSearchTurn(SearchPattern.TURN_BLACK); // At 17. Rd8#, white moved, so turn is Black
        if (!finalPattern.matchesPositionInGame(game)) {
            throw new AssertionError("Final position has Black to move; should match TURN_BLACK");
        }
        finalPattern.setSearchTurn(SearchPattern.TURN_WHITE);
        if (finalPattern.matchesPositionInGame(game)) {
            throw new AssertionError("Final position has Black to move; should NOT match TURN_WHITE");
        }

        System.out.println("  ... pass");
    }

    public void testPgnDatabasePositionSearch() {
        System.out.println("TEST: PgnChessDatabase position search");
        PgnChessDatabase db = new PgnChessDatabase();
        try {
            db.open(TestCases.getPgnPath("test_pgn_02.pgn"));
            db.scanGames();
            if (db.getIndex().size() != 4) {
                throw new AssertionError("Expected 4 games in test_pgn_02.pgn, got " + db.getIndex().size());
            }

            Game morphyGame = db.loadGame(0);
            Board morphyEndBoard = morphyGame.getEndNode().getBoard();

            // 1. Position search for Morphy checkmate position -> exactly 1 match
            SearchPattern p1 = new SearchPattern();
            p1.setSearchPosition(true);
            p1.setSearchBoard(morphyEndBoard);
            p1.setSearchTurn(SearchPattern.TURN_ANY);
            db.search(p1);
            ArrayList<GameInfo> res1 = db.getSearchResults();
            if (res1.size() != 1) {
                throw new AssertionError("Expected 1 result for Morphy checkmate position, got " + res1.size());
            }
            if (!res1.get(0).getWhite().contains("Morphy")) {
                throw new AssertionError("Result game 0 should be Morphy, got: " + res1.get(0).getWhite());
            }

            // 2. Position search with wildcard: replace Rd8 with WHITE_ANY_PIECE
            Board wildcardBoard = morphyEndBoard.makeCopy();
            wildcardBoard.setPieceAt(3, 7, WHITE_ANY_PIECE);
            SearchPattern p2 = new SearchPattern();
            p2.setSearchPosition(true);
            p2.setSearchBoard(wildcardBoard);
            p2.setSearchTurn(SearchPattern.TURN_ANY);
            db.search(p2);
            ArrayList<GameInfo> res2 = db.getSearchResults();
            if (res2.size() != 1) {
                throw new AssertionError("Expected 1 result for Morphy wildcard position, got " + res2.size());
            }

            // 3. Combined search: searchGameData (White: "Anderssen") AND searchPosition (starting board)
            SearchPattern p3 = new SearchPattern();
            p3.setSearchGameData(true);
            p3.setWhiteName("Anderssen");
            p3.setSearchPosition(true);
            p3.setSearchBoard(new Board(true));
            p3.setSearchTurn(SearchPattern.TURN_ANY);
            db.search(p3);
            ArrayList<GameInfo> res3 = db.getSearchResults();
            // Anderssen games in test_pgn_02: Immortal and Evergreen (2 games)
            if (res3.size() != 2) {
                throw new AssertionError("Expected 2 games for Anderssen + starting board, got " + res3.size());
            }

            // 4. Combined search with mismatching header: White: "Fischer" AND searchPosition (Morphy end board)
            SearchPattern p4 = new SearchPattern();
            p4.setSearchGameData(true);
            p4.setWhiteName("Fischer");
            p4.setSearchPosition(true);
            p4.setSearchBoard(morphyEndBoard);
            p4.setSearchTurn(SearchPattern.TURN_ANY);
            db.search(p4);
            ArrayList<GameInfo> res4 = db.getSearchResults();
            if (!res4.isEmpty()) {
                throw new AssertionError("Expected 0 games for Fischer + Morphy end board, got " + res4.size());
            }

            // 5. Position search for position after 1. e4 e5
            // First 3 games start 1. e4 e5, Fischer starts 1. c4
            Board e4e5Board = new Board("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2");
            SearchPattern p5 = new SearchPattern();
            p5.setSearchPosition(true);
            p5.setSearchBoard(e4e5Board);
            p5.setSearchTurn(SearchPattern.TURN_ANY);
            db.search(p5);
            ArrayList<GameInfo> res5 = db.getSearchResults();
            if (res5.size() != 3) {
                throw new AssertionError("Expected 3 games for position after 1. e4 e5, got " + res5.size());
            }

            System.out.println("  ... pass");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void testScid5PreFilter() {
        System.out.println("TEST: Scid5 pre-filter (home pawns & finalMatSig)");
        Scid5GameInfo info = new Scid5GameInfo();
        info.setHomePawnCount(0); // All 16 pawns remained on home squares throughout game
        info.setHomePawnData(new byte[8]);

        // Search board where a2 is empty
        Board emptyA2 = new Board(true);
        emptyA2.setPieceAt(0, 1, EMPTY); // col 0, row 1 (a2) is empty

        SearchPattern pattern = new SearchPattern();
        pattern.setSearchPosition(true);
        pattern.setSearchBoard(emptyA2);

        // Pre-filter should reject because a2 pawn never moved in this game!
        if (pattern.matchesScidPreFilter(info)) {
            throw new AssertionError("Pre-filter should reject game where a2 pawn never moved but search board has a2 empty");
        }

        // If a2 has WHITE_PAWN, it should pass the home pawn pre-filter
        Board pawnA2 = new Board(true); // starting board has all 16 pawns on home squares
        pattern.setSearchBoard(pawnA2);
        if (!pattern.matchesScidPreFilter(info)) {
            throw new AssertionError("Pre-filter should accept starting board when all home pawns are present");
        }

        // If a2 has WHITE_ANY_PIECE, it should also pass
        Board wildcardA2 = new Board(true);
        wildcardA2.setPieceAt(0, 1, WHITE_ANY_PIECE);
        pattern.setSearchBoard(wildcardA2);
        if (!pattern.matchesScidPreFilter(info)) {
            throw new AssertionError("Pre-filter should accept WHITE_ANY_PIECE on home pawn square");
        }

        // Test finalMatSig: game with 1 white queen at the end
        // finalMatSig: bit 22..23 is wQ
        int matSigWith1Queen = (1 << 22);
        info.setFinalMatSig(matSigWith1Queen);
        Board boardNoQueens = new Board(false); // 0 queens, 0 any pieces
        pattern.setSearchBoard(boardNoQueens);
        if (pattern.matchesScidPreFilter(info)) {
            throw new AssertionError("Pre-filter should reject board with 0 queens when game ends with 1 queen without promotions");
        }

        System.out.println("  ... pass");
    }

    public void testScid5DatabasePositionSearch() {
        System.out.println("TEST: Scid5ChessDatabase position search");
        Scid5ChessDatabase db = new Scid5ChessDatabase();
        try {
            db.open(TestCases.getScidPath("sample_db.si5"));
            db.scanGames();
            if (db.getIndex().size() != 26) {
                throw new AssertionError("Expected 26 games in sample_db.si5, got " + db.getIndex().size());
            }

            Game morphyGame = db.loadGame(0);
            Board morphyEndBoard = morphyGame.getEndNode().getBoard();

            // 1. Search for Morphy final checkmate position (present in games 0, 1, and 5)
            SearchPattern p1 = new SearchPattern();
            p1.setSearchPosition(true);
            p1.setSearchBoard(morphyEndBoard);
            p1.setSearchTurn(SearchPattern.TURN_ANY);
            db.search(p1);
            ArrayList<GameInfo> res1 = db.getSearchResults();
            if (res1.size() != 3) {
                throw new AssertionError("Expected 3 matches for Morphy checkmate position in sample_db.si5, got " + res1.size());
            }

            // 2. Search for Morphy final checkmate position combined with whiteName = "Morphy"
            SearchPattern pMorphy = new SearchPattern();
            pMorphy.setSearchGameData(true);
            pMorphy.setWhiteName("Morphy");
            pMorphy.setSearchPosition(true);
            pMorphy.setSearchBoard(morphyEndBoard);
            pMorphy.setSearchTurn(SearchPattern.TURN_ANY);
            db.search(pMorphy);
            ArrayList<GameInfo> resMorphy = db.getSearchResults();
            if (resMorphy.size() != 2) {
                throw new AssertionError("Expected 2 matches for Morphy + checkmate position, got " + resMorphy.size());
            }
            for (GameInfo g : resMorphy) {
                if (!g.getWhite().contains("Morphy")) {
                    throw new AssertionError("White player must be Morphy, got: " + g.getWhite());
                }
            }

            // 2. Search for starting board -> all 26 games should match
            SearchPattern p2 = new SearchPattern();
            p2.setSearchPosition(true);
            p2.setSearchBoard(new Board(true));
            p2.setSearchTurn(SearchPattern.TURN_ANY);
            db.search(p2);
            ArrayList<GameInfo> res2 = db.getSearchResults();
            if (res2.size() != 26) {
                throw new AssertionError("Expected all 26 games to match starting board, got " + res2.size());
            }

            // 3. Combined search: searchGameData (White: "Kasparov") AND searchPosition (starting board)
            SearchPattern p3 = new SearchPattern();
            p3.setSearchGameData(true);
            p3.setWhiteName("Kasparov");
            p3.setSearchPosition(true);
            p3.setSearchBoard(new Board(true));
            p3.setSearchTurn(SearchPattern.TURN_ANY);
            db.search(p3);
            ArrayList<GameInfo> res3 = db.getSearchResults();
            if (res3.isEmpty()) {
                throw new AssertionError("Expected Kasparov games matching starting position");
            }
            for (GameInfo g : res3) {
                if (!g.getWhite().contains("Kasparov")) {
                    throw new AssertionError("White player must be Kasparov, got: " + g.getWhite());
                }
            }

            // 4. Combined search with header mismatch: White: "Kasparov" AND Morphy checkmate position -> 0 matches
            SearchPattern p4 = new SearchPattern();
            p4.setSearchGameData(true);
            p4.setWhiteName("Kasparov");
            p4.setSearchPosition(true);
            p4.setSearchBoard(morphyEndBoard);
            p4.setSearchTurn(SearchPattern.TURN_ANY);
            db.search(p4);
            ArrayList<GameInfo> res4 = db.getSearchResults();
            if (!res4.isEmpty()) {
                throw new AssertionError("Expected 0 matches for Kasparov + Morphy checkmate position, got " + res4.size());
            }

            System.out.println("  ... pass");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void testPgnStreamingConsumer() {
        System.out.println("TEST: PgnReader streaming consumer & early-exit");
        String pgn = """
                [Event "Streaming Test 1"]
                [Site "Test City"]
                [Date "2026.11.06"]
                [Round "1"]
                [White "Player A"]
                [Black "Player B"]
                [Result "1-0"]

                1. e4 e5 2. Nf3 (2. f4 d5 3. exd5) 2... Nc6 3. Bb5 a6 {Morphy defense} 4. Ba4 Nf6 $1 1-0

                [Event "Streaming Test 2 (Early Exit)"]
                [Site "Test City"]
                [Date "2026.11.06"]
                [Round "2"]
                [White "Player C"]
                [Black "Player D"]
                [Result "0-1"]

                1. d4 d5 2. c4 e6 3. Nc3 Nf6 4. Bg5 Be7 0-1
                """;

        try {
            PgnByteLexer lexer = new PgnByteLexer(pgn.getBytes(StandardCharsets.UTF_8));
            PgnReader reader = new PgnReader();

            // Test 1: Full scan of game 1
            class TestConsumer implements PgnGameConsumer {
                int tags = 0;
                int moves = 0;
                int variationsStarted = 0;
                int variationsEnded = 0;
                int comments = 0;
                int nags = 0;
                int result = -1;
                boolean gameEnded = false;

                @Override
                public void onTag(String key, String value) { tags++; }
                @Override
                public boolean onMove(Board b, Move m, int ply, boolean isMainline) { moves++; return true; }
                @Override
                public void onStartVariation() { variationsStarted++; }
                @Override
                public void onEndVariation() { variationsEnded++; }
                @Override
                public void onComment(String c) { comments++; }
                @Override
                public void onNag(int nag) { nags++; }
                @Override
                public void onResult(int res) { result = res; }
                @Override
                public void onGameEnd() { gameEnded = true; }
            }

            TestConsumer c1 = new TestConsumer();
            boolean read1 = reader.scanGame(lexer, c1);
            if (!read1 || !c1.gameEnded) {
                throw new AssertionError("Game 1 should have been read and ended");
            }
            if (c1.tags != 7) {
                throw new AssertionError("Expected 7 tags, got: " + c1.tags);
            }
            // Mainline: 1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Ba4 Nf6 = 8 moves
            // Variation: 2. f4 d5 3. exd5 = 3 moves
            // Total = 11 moves
            if (c1.moves != 11) {
                throw new AssertionError("Expected 11 moves, got: " + c1.moves);
            }
            if (c1.variationsStarted != 1 || c1.variationsEnded != 1) {
                throw new AssertionError("Expected 1 variation start and end");
            }
            if (c1.comments != 1) {
                throw new AssertionError("Expected 1 comment, got: " + c1.comments);
            }
            if (c1.nags != 1) {
                throw new AssertionError("Expected 1 NAG, got: " + c1.nags);
            }
            if (c1.result != CONSTANTS.RES_WHITE_WINS) {
                throw new AssertionError("Expected White wins, got: " + c1.result);
            }

            // Test 2: Early-exit on game 2 (abort on move 2)
            class EarlyExitConsumer implements PgnGameConsumer {
                int moves = 0;
                boolean gameEnded = false;
                @Override
                public boolean onMove(Board b, Move m, int ply, boolean isMainline) {
                    moves++;
                    return moves < 2; // return false on move 2 -> early exit!
                }
                @Override
                public void onGameEnd() { gameEnded = true; }
            }

            EarlyExitConsumer c2 = new EarlyExitConsumer();
            boolean read2 = reader.scanGame(lexer, c2);
            if (!read2) {
                throw new AssertionError("Game 2 should have been found");
            }
            if (c2.moves != 2) {
                throw new AssertionError("Expected early exit at move 2, got: " + c2.moves);
            }
            if (c2.gameEnded) {
                throw new AssertionError("Game 2 should NOT have fired onGameEnd due to early exit");
            }

            // Verify EOF
            boolean read3 = reader.scanGame(lexer, new TestConsumer());
            if (read3) {
                throw new AssertionError("Expected EOF after game 2");
            }

            System.out.println("  ... pass");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void testScidStreamingImport() {
        System.out.println("TEST: Scid5ChessDatabase.importPgn streaming import");
        try {
            Path tempDir = Files.createTempDirectory("jfxchess-scid-import-test");
            Path scidPath = tempDir.resolve("imported_db.si5");
            Path pgnPath = tempDir.resolve("sample.pgn");

            String pgn = """
                    [Event "Import Event 1"]
                    [Site "Online"]
                    [Date "2026.01.01"]
                    [Round "1"]
                    [White "Kasparov"]
                    [Black "Karpov"]
                    [Result "1-0"]

                    1. e4 c5 2. Nf3 d6 3. d4 cxd4 4. Nxd4 Nf6 5. Nc3 a6 1-0

                    [Event "Import Event 2"]
                    [Site "Online"]
                    [Date "2026.01.02"]
                    [Round "2"]
                    [White "Fischer"]
                    [Black "Spassky"]
                    [Result "1/2-1/2"]

                    1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Ba4 Nf6 1/2-1/2
                    """;
            Files.writeString(pgnPath, pgn);

            Scid5ChessDatabase db = new Scid5ChessDatabase();
            db.createNew(scidPath.toString());
            db.importPgn(pgnPath, null);

            ArrayList<GameInfo> index = db.getIndex();
            if (index.size() != 2) {
                throw new AssertionError("Expected 2 imported games, got " + index.size());
            }

            GameInfo g1 = index.get(0);
            if (!"Kasparov".equals(g1.getWhite()) || !"Karpov".equals(g1.getBlack())) {
                throw new AssertionError("Game 1 players mismatch: " + g1.getWhite() + " vs " + g1.getBlack());
            }
            if (!"1-0".equals(g1.getResult())) {
                throw new AssertionError("Game 1 result mismatch: " + g1.getResult());
            }

            Game loaded1 = db.loadGame(g1);
            if (loaded1 == null || loaded1.getRootNode().getVariations().isEmpty()) {
                throw new AssertionError("Game 1 should load correctly with moves");
            }

            GameInfo g2 = index.get(1);
            if (!"Fischer".equals(g2.getWhite()) || !"Spassky".equals(g2.getBlack())) {
                throw new AssertionError("Game 2 players mismatch: " + g2.getWhite() + " vs " + g2.getBlack());
            }
            if (!"1/2-1/2".equals(g2.getResult())) {
                throw new AssertionError("Game 2 result mismatch: " + g2.getResult());
            }

            db.close();

            // Clean up temporary test files
            Files.deleteIfExists(tempDir.resolve("imported_db.si5"));
            Files.deleteIfExists(tempDir.resolve("imported_db.sg5"));
            Files.deleteIfExists(tempDir.resolve("imported_db.sn5"));
            Files.deleteIfExists(pgnPath);
            Files.deleteIfExists(tempDir);

            System.out.println("  ... pass");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
