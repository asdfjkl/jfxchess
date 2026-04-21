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

package org.asdfjkl.jfxchess.gui;

import org.asdfjkl.jfxchess.lib.PgnGameInfo;

import java.util.ArrayList;

public class PgnDatabase {

    private ArrayList<PgnGameInfo> entries = new ArrayList<>();
    private ArrayList<PgnGameInfo> searchResult = new ArrayList<>();
    private int idxOfCurrentlyOpenedGame = - 1;
    private String absoluteFilename = "";

    private boolean isSearchActive = false;

    public ArrayList<PgnGameInfo> getEntries() {
        return entries;
    }

    public void setEntries(ArrayList<PgnGameInfo> entries) {
        this.entries = entries;
    }

    public ArrayList<PgnGameInfo> getSearchResult() {
        return searchResult;
    }

    public void setSearchResults(ArrayList<PgnGameInfo> searchResult) {
        this.searchResult = searchResult;
    }

    public int getIdxOfCurrentlyOpenedGame() {
        return idxOfCurrentlyOpenedGame;
    }

    public void setIdxOfCurrentlyOpenedGame(int idxOfCurrentlyOpenedGame) {
        this.idxOfCurrentlyOpenedGame = idxOfCurrentlyOpenedGame;
    }

    public String getAbsoluteFilename() {
        return absoluteFilename;
    }

    public void setAbsoluteFilename(String absoluteFilename) {
        this.absoluteFilename = absoluteFilename;
    }

    public boolean isSearchActive() {
        return isSearchActive;
    }

    public void setSearchActive(boolean searchActive) {
        isSearchActive = searchActive;
    }
}
