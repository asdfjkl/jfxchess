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
import org.asdfjkl.jfxchess.lib.PgnGameInfo;
import org.asdfjkl.jfxchess.lib.PgnReader;
import org.asdfjkl.jfxchess.lib.ProgressListener;
import org.asdfjkl.jfxchess.lib.SearchPattern;

import javax.swing.*;
import java.util.ArrayList;

public class PgnSearchWorker extends SwingWorker<ArrayList<PgnGameInfo>, Integer> {

    private final ArrayList<PgnGameInfo> entries;
    private final ChessDatabase database;
    private final PgnScanListener pgnScanListener;
    private final SearchPattern pattern;

    public PgnSearchWorker(ChessDatabase database,
                           SearchPattern pattern,
                           PgnScanListener listener) {
        this.database = database;
        this.entries = null;
        this.pgnScanListener = listener;
        this.pattern = pattern;
    }

    public PgnSearchWorker(ArrayList<PgnGameInfo> entriesToSearch,
                           SearchPattern pattern,
                           PgnReader reader,
                           PgnScanListener listener) {
        this.entries = entriesToSearch;
        this.database = null;
        this.pgnScanListener = listener;
        this.pattern = pattern;
    }

    @Override
    protected ArrayList<PgnGameInfo> doInBackground() throws Exception {
        ProgressListener listener = new ProgressListener() {
            @Override
            public void onProgress(int percent) {
                setProgress(percent);
            }

            @Override
            public boolean isCancelled() {
                return PgnSearchWorker.this.isCancelled();
            }
        };

        if (database != null) {
            database.search(pattern, listener);
            ArrayList<PgnGameInfo> result = new ArrayList<>();
            for (GameInfo info : database.getSearchResults()) {
                if (info instanceof PgnGameInfo pgnInfo) {
                    result.add(pgnInfo);
                }
            }
            return result;
        }

        if (entries != null) {
            ArrayList<PgnGameInfo> matchingEntries = new ArrayList<>();
            for (int i = 0; i < entries.size(); i++) {
                if (isCancelled()) {
                    break;
                }
                PgnGameInfo gameInfo = entries.get(i);
                if (pattern.matchesHeader(gameInfo)) {
                    matchingEntries.add(gameInfo);
                }
                if (i % 10000 == 0 && !entries.isEmpty()) {
                    int percent = (int) ((long) i * 100 / entries.size());
                    setProgress(percent);
                }
            }
            return matchingEntries;
        }

        return new ArrayList<>();
    }

    @Override
    protected void done() {
        try {
            if (!isCancelled()) {
                ArrayList<PgnGameInfo> result = get();
                pgnScanListener.onScanFinished(result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}