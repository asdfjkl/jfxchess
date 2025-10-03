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

package org.asdfjkl.jfxchess.gui;

import org.asdfjkl.jfxchess.lib.Board;

public class SearchPattern {

    public static final int SEARCH_IGNORE_ELO = 0;
    public static final int SEARCH_ONE_ELO = 1;
    public static final int SEARCH_BOTH_ELO = 2;
    public static final int SEARCH_AVG_ELO = 3;

    private boolean searchForHeader = false;
    private boolean searchForPosition = false;

    private String whiteName = "";
    private String blackName = "";
    private boolean ignoreNameColor = false;

    private String event = "";
    private String site = "";

    private boolean checkYear = false;
    private int minYear = 500;
    private int maxYear = 2100;

    private boolean checkEco = false;
    private String ecoStart = "A00";
    private String ecoStop = "E99";

    private boolean checkMoves = false;
    private int minMove = 1;
    private int maxMove = 99;

    private int checkElo = SEARCH_IGNORE_ELO;
    private int minElo = 1000;
    private int maxElo = 3000;

    private boolean resultUndef = true;
    private boolean resultDraw = true;
    private boolean resultWhiteWins = true;
    private boolean resultBlackWins = true;

    long positionHash = 0;

    private Board board = new Board();

    public void setBoard(Board board) {
        this.board = board;
        positionHash = board.getPositionHash();
    }

    public long getPositionHash() { return positionHash; }

    public void setPositionHash(long positionHash) { this.positionHash = positionHash; }

    public Board getBoard() {
        return board;
    }

    public void setSearchForHeader(boolean searchForHeader) {
        this.searchForHeader = searchForHeader;
    }

    public void setSearchForPosition(boolean searchForPosition) {
        this.searchForPosition = searchForPosition;
    }

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

    public void setCheckEco(boolean checkEco) {
        this.checkEco = checkEco;
    }

    public void setEcoStart(String ecoStart) {
        this.ecoStart = ecoStart;
    }

    public void setEcoStop(String ecoStop) {
        this.ecoStop = ecoStop;
    }

    public void setCheckMoves(boolean checkMoves) {
        this.checkMoves = checkMoves;
    }

    public void setMinMove(int minMove) {
        this.minMove = minMove;
    }

    public void setMaxMove(int maxMove) {
        this.maxMove = maxMove;
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

    public boolean isSearchForHeader() {
        return searchForHeader;
    }

    public boolean isSearchForPosition() {
        return searchForPosition;
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

    public boolean isCheckEco() {
        return checkEco;
    }

    public String getEcoStart() {
        return ecoStart;
    }

    public String getEcoStop() {
        return ecoStop;
    }

    public boolean isCheckMoves() {
        return checkMoves;
    }

    public int getMinMove() {
        return minMove;
    }

    public int getMaxMove() {
        return maxMove;
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

    public SearchPattern makeCopy() {

        SearchPattern copy = new SearchPattern();

        copy.searchForHeader = searchForHeader;
        copy.searchForPosition = searchForPosition;

        copy.whiteName = whiteName;
        copy.blackName = blackName;
        copy.ignoreNameColor = ignoreNameColor;

        copy.event = event;
        copy.site = site;

        copy.checkYear = checkYear;
        copy.minYear = minYear;
        copy.maxYear = maxYear;

        copy.checkEco = checkEco;
        copy.ecoStart = ecoStart;
        copy.ecoStop = ecoStop;

        copy.checkMoves = checkMoves;
        copy.minMove = minMove;
        copy.maxMove = maxMove;

        copy.checkElo = checkElo;
        copy.minElo = minElo;
        copy.maxElo = maxElo;

        copy.resultUndef = resultUndef;
        copy.resultDraw = resultDraw;
        copy.resultWhiteWins = resultWhiteWins;
        copy.resultBlackWins = resultBlackWins;

        return copy;

    }

    public boolean matchesHeader(PgnDatabaseEntry pgnDatabaseEntry) {

        if(!event.isEmpty() && !(pgnDatabaseEntry.getEvent().toLowerCase().contains(event.toLowerCase()))) {
            return false;
        }
        if(!site.isEmpty() && !(pgnDatabaseEntry.getSite().toLowerCase().contains(site.toLowerCase()))) {
            return false;
        }
        int year = -1;
        String[] dateSplit = pgnDatabaseEntry.getDate().split("\\.");
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
        if(year != -1) {
            if(year < minYear || year > maxYear) {
                return false;
            }
        }
        if(!ignoreNameColor) {
            if (!whiteName.isEmpty() && !(pgnDatabaseEntry.getWhite().toLowerCase().contains(whiteName.toLowerCase()))) {
                return false;
            }
            if (!blackName.isEmpty() && !(pgnDatabaseEntry.getBlack().toLowerCase().contains(blackName.toLowerCase()))) {
                return false;
            }
        } else {
            boolean matchWhiteName = false;
            boolean matchBlackName = false;
            // if whiteName is not empty, it has to appear somewhere
            if(!whiteName.isEmpty()) {
                if(pgnDatabaseEntry.getWhite().toLowerCase().contains(whiteName.toLowerCase())) {
                    matchWhiteName = true;
                }
                if(pgnDatabaseEntry.getBlack().toLowerCase().contains(whiteName.toLowerCase())) {
                    matchWhiteName = true;
                }
            } else {
                matchWhiteName = true;
            }
            if(!blackName.isEmpty()) {
                if(pgnDatabaseEntry.getWhite().toLowerCase().contains(blackName.toLowerCase())) {
                    matchBlackName = true;
                }
                if(pgnDatabaseEntry.getBlack().toLowerCase().contains(blackName.toLowerCase())) {
                    matchBlackName = true;
                }
            } else {
                matchBlackName = true;
            }
            if(!matchWhiteName || !matchBlackName) {
                return false;
            }
        }
        if(!resultBlackWins && pgnDatabaseEntry.getResult().equals("0-1")) {
            return false;
        }
        if(!resultWhiteWins && pgnDatabaseEntry.getResult().equals("1-0")) {
            return false;
        }
        if(!resultDraw && pgnDatabaseEntry.getResult().equals("1/2-1/2")) {
            return false;
        }
        if(!resultUndef && pgnDatabaseEntry.getResult().equals("*")) {
            return false;
        }
        if(!ecoStart.isEmpty() && !ecoStop.isEmpty()) {
            if(!pgnDatabaseEntry.getEco().isEmpty()) {
                if(pgnDatabaseEntry.getEco().toUpperCase().compareTo(ecoStart.toUpperCase()) < 0) {
                    return false;
                }
                if(pgnDatabaseEntry.getEco().toUpperCase().compareTo(ecoStop.toUpperCase()) > 0) {
                    return false;
                }
            }
        }
        return true;
    }

}
