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

import java.util.ArrayList;
import java.util.ArrayDeque;
import static org.asdfjkl.jfxchess.lib.CONSTANTS.*;

public class SearchPattern {

    public static final int SEARCH_IGNORE_ELO = 0;
    public static final int SEARCH_ONE_ELO = 1;
    public static final int SEARCH_BOTH_ELO = 2;
    public static final int SEARCH_AVG_ELO = 3;

    public static final int TURN_ANY = 0;
    public static final int TURN_WHITE = 1;
    public static final int TURN_BLACK = 2;

    private boolean searchGameData = true;
    private boolean searchPosition = false;

    private Board searchBoard = null;
    private int searchTurn = TURN_ANY;

    private boolean signaturesDirty = true;
    private Scid5StoredLine storedLineMatcher = null;
    private int targetHpSig = 0;
    private int targetHpMoved = 0;
    private boolean homePawnCheckDisabled = false;
    private int targetWp = 0, targetWn = 0, targetWb = 0, targetWr = 0, targetWq = 0, targetWAny = 0;
    private int targetBp = 0, targetBn = 0, targetBb = 0, targetBr = 0, targetBq = 0, targetBAny = 0;

    private String whiteName = "";
    private String blackName = "";
    private boolean ignoreNameColor = false;

    private String event = "";
    private String site = "";

    private boolean checkYear = false;
    private int minYear = 500;
    private int maxYear = 2100;

    private int checkElo = SEARCH_IGNORE_ELO;
    private int minElo = 1000;
    private int maxElo = 3000;

    private boolean resultUndef = true;
    private boolean resultDraw = true;
    private boolean resultWhiteWins = true;
    private boolean resultBlackWins = true;

    public void setWhiteName(String whiteName) {
        this.whiteName = whiteName;
    }

    public void setBlackName(String blackName) {
        this.blackName = blackName;
    }

    public void setIgnoreNameColor(boolean ignoreNameColor) {
        this.ignoreNameColor = ignoreNameColor;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public void setCheckYear(boolean checkYear) {
        this.checkYear = checkYear;
    }

    public void setMinYear(int minYear) {
        this.minYear = minYear;
    }

    public void setMaxYear(int maxYear) {
        this.maxYear = maxYear;
    }

    public void setCheckElo(int checkElo) {
        this.checkElo = checkElo;
    }

    public void setMinElo(int minElo) {
        this.minElo = minElo;
    }

    public void setMaxElo(int maxElo) {
        this.maxElo = maxElo;
    }

    public void setResultUndef(boolean resultUndef) {
        this.resultUndef = resultUndef;
    }

    public void setResultDraw(boolean resultDraw) {
        this.resultDraw = resultDraw;
    }

    public void setResultWhiteWins(boolean resultWhiteWins) {
        this.resultWhiteWins = resultWhiteWins;
    }

    public void setResultBlackWins(boolean resultBlackWins) {
        this.resultBlackWins = resultBlackWins;
    }

    public String getWhiteName() {
        return whiteName;
    }

    public String getBlackName() {
        return blackName;
    }

    public boolean isIgnoreNameColor() {
        return ignoreNameColor;
    }

    public String getEvent() {
        return event;
    }

    public String getSite() {
        return site;
    }

    public boolean isCheckYear() {
        return checkYear;
    }

    public int getMinYear() {
        return minYear;
    }

    public int getMaxYear() {
        return maxYear;
    }

    public int getCheckElo() {
        return checkElo;
    }

    public int getMinElo() {
        return minElo;
    }

    public int getMaxElo() {
        return maxElo;
    }

    public boolean isResultUndef() {
        return resultUndef;
    }

    public boolean isResultDraw() {
        return resultDraw;
    }

    public boolean isResultWhiteWins() {
        return resultWhiteWins;
    }

    public boolean isResultBlackWins() {
        return resultBlackWins;
    }

    public boolean isSearchGameData() {
        return searchGameData;
    }

    public void setSearchGameData(boolean searchGameData) {
        this.searchGameData = searchGameData;
    }

    public boolean isSearchPosition() {
        return searchPosition;
    }

    public void setSearchPosition(boolean searchPosition) {
        this.searchPosition = searchPosition;
    }

    public Board getSearchBoard() {
        return searchBoard;
    }

    public void setSearchBoard(Board searchBoard) {
        this.searchBoard = searchBoard;
        this.signaturesDirty = true;
    }

    public int getSearchTurn() {
        return searchTurn;
    }

    public void setSearchTurn(int searchTurn) {
        this.searchTurn = searchTurn;
        this.signaturesDirty = true;
    }

    public SearchPattern makeCopy() {

        SearchPattern copy = new SearchPattern();

        copy.whiteName = whiteName;
        copy.blackName = blackName;
        copy.ignoreNameColor = ignoreNameColor;

        copy.event = event;
        copy.site = site;

        copy.checkYear = checkYear;
        copy.minYear = minYear;
        copy.maxYear = maxYear;

        copy.checkElo = checkElo;
        copy.minElo = minElo;
        copy.maxElo = maxElo;

        copy.resultUndef = resultUndef;
        copy.resultDraw = resultDraw;
        copy.resultWhiteWins = resultWhiteWins;
        copy.resultBlackWins = resultBlackWins;
        copy.searchGameData = searchGameData;
        copy.searchPosition = searchPosition;
        copy.searchTurn = searchTurn;
        if (searchBoard != null) {
            copy.searchBoard = searchBoard.makeCopy();
            copy.signaturesDirty = true;
        }

        return copy;

    }

    public boolean matchesHeader(GameInfo pgnGameInfo) {

        if(!event.isEmpty() && !(pgnGameInfo.getEvent().toLowerCase().contains(event.toLowerCase()))) {
            return false;
        }
        if(!site.isEmpty() && !(pgnGameInfo.getSite().toLowerCase().contains(site.toLowerCase()))) {
            return false;
        }
        int year = -1;
        String[] dateSplit = pgnGameInfo.getDate().split("\\.");
        if(dateSplit.length > 0 && dateSplit[0].length() == 4) {
            try {
                year = Integer.parseInt(dateSplit[0]);
            } catch (NumberFormatException e) {
            }
        }
        if(dateSplit.length >= 3 && dateSplit[2].length() == 4) {
            try {
                year = Integer.parseInt(dateSplit[2]);
            } catch (NumberFormatException e) {
            }
        }
        if(year > 0) {
            if(year < minYear || year > maxYear) {
                return false;
            }
        }
        if(!ignoreNameColor) {
            if (!whiteName.isEmpty() && !(pgnGameInfo.getWhite().toLowerCase().contains(whiteName.toLowerCase()))) {
                return false;
            }
            if (!blackName.isEmpty() && !(pgnGameInfo.getBlack().toLowerCase().contains(blackName.toLowerCase()))) {
                return false;
            }
        } else {
            boolean matchWhiteName = false;
            boolean matchBlackName = false;
            // if whiteName is not empty, it has to appear somewhere
            if(!whiteName.isEmpty()) {
                if(pgnGameInfo.getWhite().toLowerCase().contains(whiteName.toLowerCase())) {
                    matchWhiteName = true;
                }
                if(pgnGameInfo.getBlack().toLowerCase().contains(whiteName.toLowerCase())) {
                    matchWhiteName = true;
                }
            } else {
                matchWhiteName = true;
            }
            if(!blackName.isEmpty()) {
                if(pgnGameInfo.getWhite().toLowerCase().contains(blackName.toLowerCase())) {
                    matchBlackName = true;
                }
                if(pgnGameInfo.getBlack().toLowerCase().contains(blackName.toLowerCase())) {
                    matchBlackName = true;
                }
            } else {
                matchBlackName = true;
            }
            if(!matchWhiteName || !matchBlackName) {
                return false;
            }
        }
        if(!resultBlackWins && pgnGameInfo.getResult().equals("0-1")) {
            return false;
        }
        if(!resultWhiteWins && pgnGameInfo.getResult().equals("1-0")) {
            return false;
        }
        if(!resultDraw && pgnGameInfo.getResult().equals("1/2-1/2")) {
            return false;
        }
        if(!resultUndef && pgnGameInfo.getResult().equals("*")) {
            return false;
        }

        int whiteElo;
        int blackElo;
        try {
            whiteElo = Integer.parseInt(pgnGameInfo.getWhiteElo());
        } catch (NumberFormatException e) {
            whiteElo = -1;
        }
        try {
            blackElo = Integer.parseInt(pgnGameInfo.getBlackElo());
        } catch (NumberFormatException e) {
            blackElo = -1;
        }
        if(checkElo == SEARCH_ONE_ELO) {
            boolean matchesEloCondition = false;
            if(whiteElo > 0 && whiteElo > minElo && whiteElo < maxElo) {
                matchesEloCondition = true;
            }
            if(blackElo > 0 && blackElo > minElo && blackElo < maxElo) {
                matchesEloCondition = true;
            }
            if(!matchesEloCondition) {
                return false;
            }
        }
        if(checkElo == SEARCH_BOTH_ELO) {
            boolean matchesWhiteEloCondition = false;
            boolean matchesBlackEloCondition = false;
            if(whiteElo > 0 && whiteElo > minElo && whiteElo < maxElo) {
                matchesWhiteEloCondition = true;
            }
            if(blackElo > 0 && blackElo > minElo && blackElo < maxElo) {
                matchesBlackEloCondition = true;
            }
            if(!(matchesWhiteEloCondition && matchesBlackEloCondition)) {
                return false;
            }
        }
        if(checkElo == SEARCH_AVG_ELO) {
            boolean matchesAverageEloCondition = false;
            int avgElo = -1;
            if(whiteElo > 0 && blackElo <= 0) {
                avgElo = whiteElo;
            }
            if(whiteElo <= 0 && blackElo > 0) {
                avgElo = blackElo;
            }
            if(whiteElo > 0 && blackElo > 0) {
                avgElo = (whiteElo + blackElo)/ 2;
            }
            if(avgElo > minElo && avgElo < maxElo) {
                matchesAverageEloCondition = true;
            }
            if(!matchesAverageEloCondition) {
                return false;
            }
        }
        return true;
    }

    public boolean matchesBoard(Board candidate) {
        if (candidate == null || searchBoard == null) {
            return false;
        }
        if (searchTurn == TURN_WHITE && candidate.turn != CONSTANTS.WHITE) {
            return false;
        }
        if (searchTurn == TURN_BLACK && candidate.turn != CONSTANTS.BLACK) {
            return false;
        }
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                int target = searchBoard.getPieceAt(x, y);
                int cand = candidate.getPieceAt(x, y);
                if (target == CONSTANTS.EMPTY) {
                    if (cand != CONSTANTS.EMPTY) {
                        return false;
                    }
                } else if (target == CONSTANTS.WHITE_ANY_PIECE) {
                    if (cand < CONSTANTS.WHITE_PAWN || cand > CONSTANTS.WHITE_KING) {
                        return false;
                    }
                } else if (target == CONSTANTS.BLACK_ANY_PIECE) {
                    if (cand < CONSTANTS.BLACK_PAWN || cand > CONSTANTS.BLACK_KING) {
                        return false;
                    }
                } else {
                    if (cand != target) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public boolean matchesPositionInGame(Game game) {
        if (!searchPosition || searchBoard == null) {
            return true;
        }
        if (game == null || game.getRootNode() == null) {
            return false;
        }
        ArrayDeque<GameNode> stack = new ArrayDeque<>();
        stack.push(game.getRootNode());
        while (!stack.isEmpty()) {
            GameNode node = stack.pop();
            Board b = node.getBoard();
            if (b != null && matchesBoard(b)) {
                return true;
            }
            ArrayList<GameNode> variations = node.getVariations();
            for (int i = variations.size() - 1; i >= 0; i--) {
                stack.push(variations.get(i));
            }
        }
        return false;
    }

    public Scid5StoredLine getStoredLineMatcher() {
        if (signaturesDirty) {
            computeSignatures();
        }
        return storedLineMatcher;
    }

    private void computeSignatures() {
        targetHpSig = 0;
        targetHpMoved = 0;
        homePawnCheckDisabled = false;
        targetWp = 0; targetWn = 0; targetWb = 0; targetWr = 0; targetWq = 0; targetWAny = 0;
        targetBp = 0; targetBn = 0; targetBb = 0; targetBr = 0; targetBq = 0; targetBAny = 0;

        if (searchBoard == null) {
            storedLineMatcher = null;
            signaturesDirty = false;
            return;
        }

        storedLineMatcher = new Scid5StoredLine(searchBoard, searchTurn);

        // 1. Material counts
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                int p = searchBoard.getPieceAt(x, y);
                switch (p) {
                    case WHITE_PAWN -> targetWp++;
                    case WHITE_KNIGHT -> targetWn++;
                    case WHITE_BISHOP -> targetWb++;
                    case WHITE_ROOK -> targetWr++;
                    case WHITE_QUEEN -> targetWq++;
                    case WHITE_ANY_PIECE -> targetWAny++;
                    case BLACK_PAWN -> targetBp++;
                    case BLACK_KNIGHT -> targetBn++;
                    case BLACK_BISHOP -> targetBb++;
                    case BLACK_ROOK -> targetBr++;
                    case BLACK_QUEEN -> targetBq++;
                    case BLACK_ANY_PIECE -> targetBAny++;
                }
            }
        }

        // 2. Home pawn signature (SCID hpSig)
        // White pawns on rank 2 (row 1), squares a2..h2 -> nibbles 15..8 (col 0..7 => nibble 15 - col)
        for (int col = 0; col < 8; col++) {
            int p = searchBoard.getPieceAt(col, 1);
            if (p == CONSTANTS.WHITE_ANY_PIECE) {
                homePawnCheckDisabled = true;
            } else if (p != CONSTANTS.WHITE_PAWN) {
                int nibble = 15 - col;
                targetHpSig |= (1 << nibble);
                targetHpMoved++;
            }
        }

        // Black pawns on rank 7 (row 6), squares a7..h7 -> nibbles 7..0 (col 0..7 => nibble 7 - col)
        for (int col = 0; col < 8; col++) {
            int p = searchBoard.getPieceAt(col, 6);
            if (p == CONSTANTS.BLACK_ANY_PIECE) {
                homePawnCheckDisabled = true;
            } else if (p != CONSTANTS.BLACK_PAWN) {
                int nibble = 7 - col;
                targetHpSig |= (1 << nibble);
                targetHpMoved++;
            }
        }

        signaturesDirty = false;
    }

    public boolean matchesScidPreFilter(Scid5GameInfo info) {
        if (!searchPosition || searchBoard == null || info == null) {
            return true;
        }
        // Custom start positions might not have standard pawn layouts
        if ((info.getFlags() & Scid5GameInfo.FLAG_CUSTOM_START) != 0) {
            return true;
        }

        if (signaturesDirty) {
            computeSignatures();
        }

        // 1. Home Pawn Pre-Filter (SCID hpSig_match)
        if (!homePawnCheckDisabled) {
            int hpCount = info.getHomePawnCount();
            if (hpCount == 16 && targetHpMoved == 16) {
                // All 16 pawns departed in both
            } else if (hpCount < targetHpMoved) {
                return false;
            } else {
                byte[] hpData = info.getHomePawnData();
                int sig = 0;
                for (int m = 0; m < targetHpMoved; m++) {
                    int byteIdx = m / 2;
                    int nibble = (m % 2 == 0) ? ((hpData[byteIdx] >> 4) & 0x0F) : (hpData[byteIdx] & 0x0F);
                    sig |= (1 << nibble);
                }
                if (sig != targetHpSig) {
                    return false;
                }
            }
        }

        // 2. Final Material Signature Pre-Filter (SCID less_mat)
        int finalMatSig = info.getFinalMatSig();
        int finalBp = finalMatSig & 0x0F;
        int finalWp = (finalMatSig >> 12) & 0x0F;

        int wpDiff = (targetWp + targetWAny) - finalWp;
        int bpDiff = (targetBp + targetBAny) - finalBp;

        if (wpDiff < 0 || bpDiff < 0) {
            return false;
        }

        int flags = info.getFlags();
        boolean promo = (flags & Scid5GameInfo.FLAG_PROMO) != 0;
        boolean upromo = (flags & Scid5GameInfo.FLAG_UNDER_PROMO) != 0;

        int finalBq = (finalMatSig >> 10) & 0x03;
        int finalWq = (finalMatSig >> 22) & 0x03;
        int wqDiff = (targetWq + targetWAny) - finalWq;
        int bqDiff = (targetBq + targetBAny) - finalBq;
        if (promo) {
            wqDiff += Math.max(0, wpDiff);
            bqDiff += Math.max(0, bpDiff);
        }
        if (wqDiff < 0 || bqDiff < 0) {
            return false;
        }

        if (!upromo) {
            int finalBn = (finalMatSig >> 4) & 0x03;
            int finalBb = (finalMatSig >> 6) & 0x03;
            int finalBr = (finalMatSig >> 8) & 0x03;
            int finalWn = (finalMatSig >> 16) & 0x03;
            int finalWb = (finalMatSig >> 18) & 0x03;
            int finalWr = (finalMatSig >> 20) & 0x03;

            if (targetWn + targetWAny < finalWn) return false;
            if (targetWb + targetWAny < finalWb) return false;
            if (targetWr + targetWAny < finalWr) return false;
            if (targetBn + targetBAny < finalBn) return false;
            if (targetBb + targetBAny < finalBb) return false;
            if (targetBr + targetBAny < finalBr) return false;
        }

        return true;
    }

}
