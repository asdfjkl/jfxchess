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
import org.asdfjkl.jfxchess.lib.ProgressListener;

import javax.swing.*;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ChessReplaceGameWorker extends SwingWorker<String, Integer> {

    private final ChessDatabase database;
    private final Game newGame;
    private final GameInfo currentGameInfo;
    private final ChessReplaceListener listener;

    public ChessReplaceGameWorker(ChessDatabase database,
                                  Game newGame,
                                  GameInfo currentGameInfo,
                                  ChessReplaceListener listener) {
        this.database = database;
        this.newGame = newGame;
        this.currentGameInfo = currentGameInfo;
        this.listener = listener;
    }

    @Override
    protected String doInBackground() {
        try {
            if (database != null) {
                database.replaceGame(newGame, currentGameInfo, new ProgressListener() {
                    @Override
                    public void onProgress(int percent) {
                        setProgress(percent);
                    }

                    @Override
                    public boolean isCancelled() {
                        return ChessReplaceGameWorker.this.isCancelled();
                    }
                });
                setProgress(100);
                return "SUCCESS";
            }
            return "CANCELLED";
        } catch (Exception e) {
            return stackTraceToString(e);
        }
    }

    @Override
    protected void done() {
        try {
            String result = get();
            if (listener != null) {
                listener.onReplaceFinished(result);
            }
        } catch (Exception e) {
            if (listener != null) {
                listener.onReplaceFinished(stackTraceToString(e));
            }
        }
    }

    private String stackTraceToString(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
