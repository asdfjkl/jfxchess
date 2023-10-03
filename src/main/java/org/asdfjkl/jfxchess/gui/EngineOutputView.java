/* JerryFX - A Chess Graphical User Interface
 * Copyright (C) 2020 Dominik Klein
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

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class EngineOutputView {

    private final Text engineId;
    private final Text depth;
    private final Text nps;
    private final Text pv1;
    private final Text pv2;
    private final Text pv3;
    private final Text pv4;

    private boolean isEnabled = true;

    public EngineOutputView(TextFlow txtEngineOut) {

        this.engineId = new Text("Stockfish (internal)");
        this.depth = new Text("");
        this.nps = new Text("");
        this.pv1 = new Text("");
        this.pv2 = new Text("");
        this.pv3 = new Text("");
        this.pv4 = new Text("");

        Text spacer1 = new Text("     ");
        Text spacer2 = new Text("     ");

        txtEngineOut.getChildren().addAll(
                this.engineId,
                spacer1,
                this.depth,
                spacer2,
                this.nps,
                new Text(System.lineSeparator()),
                new Text(System.lineSeparator()),
                this.pv1,
                new Text(System.lineSeparator()),
                this.pv2,
                new Text(System.lineSeparator()),
                this.pv3,
                new Text(System.lineSeparator()),
                this.pv4);

        //this.txtEngineOut.getChildren().addAll(currentEval);
    }

    public void enableOutput() { isEnabled = true; }

    public void disableOutput() {
        isEnabled = false;
        clearOutput();
    }

    public boolean isEnabled() { return isEnabled; }

    private void clearOutput() {

        engineId.setText("");
        depth.setText("");
        nps.setText("");
        pv1.setText("");
        pv2.setText("");
        pv3.setText("");
        pv4.setText("");
    }

    public void setText(String info) {

        if(isEnabled) {

            //pv1.setText(info);
            // | id (Level MAX) | zobrist  |  nps | current Move + depth | eval+line pv1 | .. pv2 | ...pv3 | ...pv4
            String[] infos = info.split("\\|");

            if (infos.length > 1 && !infos[1].isEmpty()) {
                engineId.setText(infos[1]);
            }
            if (infos.length > 3 && !infos[3].isEmpty()) {
                nps.setText(infos[3]);
            }
            if (infos.length > 4 && !infos[4].isEmpty()) {
                depth.setText(infos[4]);
            }
            if (infos.length > 5 && !infos[5].isEmpty()) {
                pv1.setText(infos[5]);
            }
            if (infos.length > 6 && !infos[6].isEmpty()) {
                pv2.setText(infos[6]);
            } else {
                pv2.setText("");
            }
            if (infos.length > 7 && !infos[7].isEmpty()) {
                pv3.setText(infos[7]);
            } else {
                pv3.setText("");
            }
            if (infos.length > 8 && !infos[8].isEmpty()) {
                pv4.setText(infos[8]);
            } else {
                pv4.setText("");
            }
        }
    }
}
