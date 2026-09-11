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

import javax.swing.*;
import java.io.PrintWriter;
import java.io.StringWriter;

public class PgnReplaceGameWorker extends SwingWorker<String, Integer> {

    private final PgnDocument document;
    private final PgnGameId gameId;
    private final String text;
    private final PgnReplaceListener listener;

    public PgnReplaceGameWorker(PgnDocument document, PgnGameId gameId, String text,
                             PgnReplaceListener listener) {
        this.document = document;
        this.gameId = gameId;
        this.text = text;
        this.listener = listener;
    }

    @Override
    protected String doInBackground() {
        try {
            document.replaceGame(gameId, text);
            setProgress(100);
            return "SUCCESS";

        } catch (Exception e) {
            return stackTraceToString(e);
        }
    }

    @Override
    protected void done() {
        try {
            String result = get(); // safe: no exception escapes
            if (listener != null) {
                listener.onReplaceFinished(result);
            }
        } catch (Exception e) {
            // This should rarely happen now, but just in case:
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