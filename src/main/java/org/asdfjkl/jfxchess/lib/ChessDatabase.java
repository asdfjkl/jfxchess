/* JFXChess - A Chess Graphical User Interface
 * Copyright (C) 2026 Dominik Klein
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
import java.nio.file.Path;
import java.util.ArrayList;

public interface ChessDatabase {

    void open(String filename) throws IOException;

    void createNew(String filename) throws IOException;

    void close() throws IOException;

    boolean isOpen();

    String getFilename();

    Path getPath();

    void scanGames(ProgressListener listener);

    void scanGames();

    ArrayList<GameInfo> getIndex();

    ArrayList<GameInfo> getSearchResults();

    boolean isSearchActive();

    void resetSearch();

    Game loadGame(GameInfo info) throws IOException;

    default Game loadGame(int index) throws IOException {
        ArrayList<GameInfo> indexList = getIndex();
        if (index < 0 || index >= indexList.size()) {
            throw new IndexOutOfBoundsException("Game index outside database: " + index);
        }
        return loadGame(indexList.get(index));
    }

    GameInfo appendGame(Game game) throws IOException;

    void replaceGame(Game newGame, GameInfo currentGameInfo, ProgressListener listener) throws IOException;

    void replaceGame(Game newGame, GameInfo currentGameInfo) throws IOException;

    void deleteGame(GameInfo info, ProgressListener listener) throws IOException;

    void deleteGame(GameInfo info) throws IOException;

    void search(SearchPattern pattern, ProgressListener listener);

    void search(SearchPattern pattern);

    void addListener(ChessDatabaseListener listener);

    void removeListener(ChessDatabaseListener listener);

    default int indexOf(GameInfo info) {
        return getIndex().indexOf(info);
    }
}
