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
import org.asdfjkl.jfxchess.lib.GameInfo;
import org.asdfjkl.jfxchess.lib.ProgressListener;

import javax.swing.*;
import java.io.IOException;
import java.util.List;

public class ChessImportGamesWorker extends SwingWorker<Void, Integer> {

    private final ChessDatabase sourceDatabase;
    private final ChessDatabase targetDatabase;
    private final List<GameInfo> gamesToImport;

    public ChessImportGamesWorker(ChessDatabase sourceDatabase,
                                  ChessDatabase targetDatabase,
                                  List<GameInfo> gamesToImport) {
        this.sourceDatabase = sourceDatabase;
        this.targetDatabase = targetDatabase;
        this.gamesToImport = gamesToImport;
    }

    @Override
    protected Void doInBackground() throws Exception {
        try {
            if (sourceDatabase != null && targetDatabase != null && gamesToImport != null) {
                targetDatabase.importGames(sourceDatabase, gamesToImport, new ProgressListener() {
                    @Override
                    public void onProgress(int percent) {
                        int clamped = Math.max(0, Math.min(100, percent));
                        setProgress(clamped);
                    }

                    @Override
                    public boolean isCancelled() {
                        return ChessImportGamesWorker.this.isCancelled();
                    }
                });
            }
        } finally {
            if (sourceDatabase != null) {
                try {
                    sourceDatabase.close();
                } catch (IOException ignored) {
                }
            }
        }
        return null;
    }

    @Override
    protected void done() {
        try {
            if (!isCancelled()) {
                get();
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Error importing games: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
