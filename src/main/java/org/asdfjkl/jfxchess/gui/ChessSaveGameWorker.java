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

import org.asdfjkl.jfxchess.lib.ChessDatabase;
import org.asdfjkl.jfxchess.lib.Game;
import org.asdfjkl.jfxchess.lib.GameInfo;

import javax.swing.*;
import java.io.IOException;

public class ChessSaveGameWorker extends SwingWorker<GameInfo, Integer> {

    private final ChessDatabase database;
    private final Game game;
    private final ChessSaveListener listener;

    public ChessSaveGameWorker(ChessDatabase database, Game game, ChessSaveListener listener) {
        this.database = database;
        this.game = game;
        this.listener = listener;
    }

    @Override
    protected GameInfo doInBackground() throws Exception {
        if (database == null) {
            throw new IOException("No chess database is open");
        }
        setProgress(50);
        GameInfo info = database.appendGame(game);
        setProgress(100);
        return info;
    }

    @Override
    protected void done() {
        try {
            if (!isCancelled()) {
                GameInfo info = get();
                if (listener != null) {
                    listener.onSaveFinished(info, null);
                }
            }
        } catch (Exception e) {
            if (listener != null) {
                listener.onSaveFinished(null, e);
            }
        }
    }
}
