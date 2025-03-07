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

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.asdfjkl.jfxchess.lib.Board;
import org.asdfjkl.jfxchess.lib.PolyglotExtEntry;
import org.asdfjkl.jfxchess.lib.CONSTANTS;
import java.util.ArrayList;

public class EngineOutputView implements StateChangeListener {

    final GameModel gameModel;

    private final Text engineId;
    private final Text depth;
    private final Text nps;
    private final Text hashFull;
    private final Text tbHits;
    
    /*
    private final Text pv1;
    private final Text pv2;
    private final Text pv3;
    private final Text pv4;
    */

    private final ArrayList<Text> pvLines;

    private boolean isEnabled = true;
    
    private boolean pvLinesAreEnabled = true;

    private TextFlow txtEngineOut;

    // This is the index of the first pv-line
    // among the children in txtEngineOut.
    private final int FirstPVLineChildIndex = 11; 

    // This is the index of the first pv-line
    // in the split String infos.
    private final int FirstPVLineInfosIndex = 7; 

    public EngineOutputView(GameModel gameModel, TextFlow txtEngineOut) {

        this.gameModel = gameModel;

        this.engineId = new Text(CONSTANTS.INTERNAL_ENGINE_NAME);
        this.depth = new Text("");
        this.nps = new Text("");
        this.hashFull = new Text("");
        this.tbHits = new Text("");
        Text pvLine = new Text("");
        this.pvLines = new ArrayList<Text>();
        this.pvLines.add(pvLine);

        Text spacer1 = new Text("     ");
        Text spacer2 = new Text("     ");
        Text spacer3 = new Text("     ");
        Text spacer4 = new Text("     ");

        this.txtEngineOut = txtEngineOut;

        this.txtEngineOut.getChildren().addAll(
                this.engineId,
                spacer1,
                this.depth,
                spacer2,
                this.nps,
                spacer3,
                this.hashFull,
                spacer4,
                this.tbHits,
                new Text(System.lineSeparator()),
                new Text(System.lineSeparator()),
                pvLines.get(0),
                new Text(System.lineSeparator()));
        // The following line fetches the restored number
        // of pv-lines from gameModel and modifies pvLines
        // and txtEngineOut by adding lines and children.
        resetPVLines();
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
        hashFull.setText("");
        tbHits.setText("");
        for(Text pvLine : pvLines) {
            pvLine.setText("");
        }
    }

    public void setText(String info) {

        if(isEnabled) {

            //pv1.setText(info);
            // | id (Level MAX) | zobrist  |  nps | hashfull | tbhits | current Move + depth | eval+line pv1 | .. pv2 | ...pv3 | ...pv4 | ... | ...pv64 |

            // Note: All trailing empty matched strings will not 
            // be part of infos, so we have to check infos.length().
            // But there can be empty strings "in between".
            String[] infos = info.split("\\|");

            if (infos.length > 1 && !infos[1].isEmpty()) {
                engineId.setText(infos[1]);
            }
            if (infos.length > 3 && !infos[3].isEmpty()) {
                nps.setText(infos[3]);
            }
            if (infos.length > 4 && !infos[4].isEmpty()) {
                hashFull.setText(infos[4]);
            }
            if (infos.length > 5 && !infos[5].isEmpty()) {
                tbHits.setText(infos[5]);
            }
            if(infos.length > 6 && !infos[6].isEmpty()) {
                depth.setText(infos[6]);
            }

            if(!pvLinesAreEnabled) {
                return;
            }

            // Set but don't clear the first pvLine-text.
            if(infos.length > FirstPVLineInfosIndex && !infos[ FirstPVLineInfosIndex].isEmpty()) {
                pvLines.get(0).setText(infos[7]);
            }

            // Set the rest of the pvLine-texts or clear them if they are empty.
            for(int i=FirstPVLineInfosIndex+1;i<infos.length;i++) {
                if(pvLines.size() > i-FirstPVLineInfosIndex) {
                    if(!infos[i].isEmpty()) {
                        pvLines.get(i-FirstPVLineInfosIndex).setText(infos[i]);
                    } else {
                        pvLines.get(i-FirstPVLineInfosIndex).setText("");
                    }
                }
            }
        }
    }

    public void disablePVLines() {
        if (pvLinesAreEnabled) {
            resetPVLines();
            depth.setText("");
	    nps.setText("");
        }
        pvLinesAreEnabled = false;
    }
    
    public void enablePVLines() {
        pvLinesAreEnabled = true;
    }

    private void resetPVLines() {
        int cntChildren = txtEngineOut.getChildren().size();
        txtEngineOut.getChildren().remove(FirstPVLineChildIndex, cntChildren);
        pvLines.clear();
        for (int i = 0; i < gameModel.getMultiPv(); i++) {
            Text pvLine = new Text("");
            pvLines.add(pvLine);
            txtEngineOut.getChildren().add(pvLine);
            txtEngineOut.getChildren().add(new Text(System.lineSeparator()));
        }
    }

    @Override
    public void stateChange() {
        if(gameModel.wasMultiPvChanged()) {
            resetPVLines();
            gameModel.setMultiPvChange(false);
        }
        // The last depth-text from analysis mode remained after mode change 
        // to playing black or white (or to a new game).
        depth.setText("");
        // There was a similar problem with nps (but this didn't seem to
        // help completely).
	nps.setText("");
	hashFull.setText("");
	tbHits.setText("");
  }
}
