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
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class View_EngineOutput extends JEditorPane implements PropertyChangeListener {

    Model_JFXChess model;

    String cachedInfo = "";

    public  View_EngineOutput(Model_JFXChess model) {
        this.model = model;

        // set up formatting
        HTMLEditorKit kit = new HTMLEditorKit();
        StyleSheet css = kit.getStyleSheet();
        css.addRule("body { font-family: sans-serif; }");
        css.addRule(
                "a { " +
                        "text-decoration: none; " +
                        "font-weight: normal; " +
                        "color: #333333; " +
                        "}"
        );
        setEditorKit(kit);
        setEditable(false);
        setFocusable(false);
        setContentType("text/html");

        /*
        htmlTest = "<table border=\"0\" cellspacing=\"0\" cellpadding=\"4\" width=\"100%\">" +
                "  <tr>" +
                "    <td>" +
                "      <table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" width=\"100%\">" +
                "        <tr>" +
                "          <td>Stockfish (internal)</td>" +
                "          <td>e2e4 (depth 37/53)</td>" +
                "          <td>554.0 kn/s</td>" +
                "          <td>hashfull 1000</td>" +
                "          <td>tbhits 0</td>" +
                "        </tr>" +
                "      </table>" +
                "    </td>" +
                "  </tr>" +
                "  <tr>" +
                "    <td>&nbsp;</td>" +
                "  </tr>" +
                "  <tr>" +
                "    <td>" +
                "      (0.18) 1. e4 c6 2. d4 d5 3. exd5 cxd5 4. c4 Nf6 5. Nc3 Nc6 6. Bg5 Bf5 9. Bf4 Ng6 10. Bg3" +
                "    </td>" +
                "  </tr>" +
                "  <tr>" +
                "    <td>" +
                "      (0.19) 1. e4 c6 2. d4 d5 3. exd5 cxd5 4. c4 Nf6 5. Nc3 Nc6 6. Bg5 Bf5 9. Bf4 Ng6 10. Bg3" +
                "    </td>" +
                "  </tr>" +
                "  <tr>" +
                "    <td>" +
                "      (0.19) 1. e4 c6 2. d4 d5 3. exd5 cxd5 4. c4 Nf6 5. Nc3 Nc6 6. Bg5 Bf5 9. Bf4 Ng6 10. Bg3" +
                "    </td>" +
                "  </tr>" +
                "  <tr>" +
                "    <td>" +
                "      (0.19) 1. e4 c6 2. d4 d5 3. exd5 cxd5 4. c4 Nf6 5. Nc3 Nc6 6. Bg5 Bf5 9. Bf4 Ng6 10. Bg3" +
                "    </td>" +
                "  </tr>" +
                "  <tr>" +
                "    <td>" +
                "      (0.19) 1. e4 c6 2. d4 d5 3. exd5 cxd5 4. c4 Nf6 5. Nc3 Nc6 6. Bg5 Bf5 9. Bf4 Ng6 10. Bg3" +
                "    </td>" +
                "  </tr>" +
                "  <tr>" +
                "    <td>" +
                "      (0.19) 1. e4 c6 2. d4 d5 3. exd5 cxd5 4. c4 Nf6 5. Nc3 Nc6 6. Bg5 Bf5 9. Bf4 Ng6 10. Bg3" +
                "    </td>" +
                "  </tr>" +
                "</table>";
        setText(htmlTest);
         */
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {

        if(evt.getPropertyName().equals("engineInfo")) {
            String info = model.getCurrentEngineInfo();
            if (info != null && info.length() > 5) {
                String engineName = "";
                if(model.activeEngine!=null) {
                    engineName = model.activeEngine.getName();
                    if(model.getMode() == Model_JFXChess.MODE_PLAY_WHITE || model.getMode() == Model_JFXChess.MODE_PLAY_BLACK) {
                        engineName += " (" + model.activeEngine.getUciElo() + ")";
                    }
                }
                String s = info.substring(5, info.length()).replace("ENGINE_ID", engineName);
                // we only set text on this widget if there is really
                // an update - in order to avoid flickering
                if(!s.equals(cachedInfo)) {
                    cachedInfo = s;
                    setText(s);
                }
            }
        }
    }
}

