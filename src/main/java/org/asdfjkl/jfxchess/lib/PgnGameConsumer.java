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

/**
 * Event-driven streaming consumer for PGN games parsed without AST allocation.
 * Implementations receive callbacks for headers, game start, moves, variations,
 * comments, NAGs, and results.
 *
 * <p>Returning {@code false} from {@link #onGameStart(Board)} or
 * {@link #onMove(Board, Move, int, boolean)} triggers immediate early-exit
 * in {@link PgnReader#scanGame(PgnByteLexer, PgnGameConsumer)}, skipping the
 * rest of the game movetext.</p>
 */
public interface PgnGameConsumer {

    default void onTag(String key, String value) {}

    default boolean onGameStart(Board rootBoard) {
        return true;
    }

    default boolean onMove(Board board, Move move, int ply, boolean isMainline) {
        return true;
    }

    default void onStartVariation() {}

    default void onEndVariation() {}

    default void onComment(String comment) {}

    default void onNag(int nag) {}

    default void onResult(int result) {}

    default void onGameEnd() {}
}
