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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ChessDatabaseEvent {

    public enum Type {
        DATABASE_OPENED,
        INDEX_REBUILT,
        GAME_APPENDED,
        GAME_REPLACED,
        GAME_DELETED
    }

    private final Type type;
    private final ChessDatabase database;
    private final long revision;
    private final List<GameInfo> affectedGames;

    public ChessDatabaseEvent(Type type,
                              ChessDatabase database,
                              long revision,
                              List<GameInfo> affectedGames) {
        this.type = Objects.requireNonNull(type, "type");
        this.database = Objects.requireNonNull(database, "database");
        this.revision = revision;
        this.affectedGames = affectedGames != null
                ? List.copyOf(affectedGames)
                : Collections.emptyList();
    }

    public Type getType() {
        return type;
    }

    public ChessDatabase getDatabase() {
        return database;
    }

    public long getRevision() {
        return revision;
    }

    public List<GameInfo> getAffectedGames() {
        return affectedGames;
    }
}
