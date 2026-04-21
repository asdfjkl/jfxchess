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
import org.asdfjkl.jfxchess.lib.PgnReader;
import org.asdfjkl.jfxchess.lib.ProgressListener;

import javax.swing.*;
import java.util.ArrayList;

public class PgnScanWorker extends SwingWorker<ArrayList<PgnGameInfo>, Integer> {

    private final String filename;
    private final PgnReader pgnReader;
    private final PgnScanListener pgnScanListener;

    public PgnScanWorker(String filename, PgnReader lib, PgnScanListener listener) {
        this.filename = filename;
        this.pgnReader = lib;
        this.pgnScanListener = listener;
    }

    @Override
    protected ArrayList<PgnGameInfo> doInBackground() throws Exception {

        return pgnReader.scanPgn(filename, new ProgressListener() {
            @Override
            public void onProgress(int percent) {
                setProgress(percent); // SwingWorker built-in support
            }

            @Override
            public boolean isCancelled() {
                return PgnScanWorker.this.isCancelled();
            }
        });
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