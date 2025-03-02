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

import java.util.ArrayList;

public class EngineOutputView implements StateChangeListener {

    final GameModel gameModel;

    private final Text engineId;
    private final Text depth;
    private final Text nps;
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

    public EngineOutputView(GameModel gameModel, TextFlow txtEngineOut) {

        this.gameModel = gameModel;

        this.engineId = new Text("Stockfish (internal)");
        this.depth = new Text("");
        this.nps = new Text("");
        Text pvLine = new Text("");
        this.pvLines = new ArrayList<Text>();
        this.pvLines.add(pvLine);

        Text spacer1 = new Text("     ");
        Text spacer2 = new Text("     ");

        this.txtEngineOut = txtEngineOut;

        this.txtEngineOut.getChildren().addAll(
                this.engineId,
                spacer1,
                this.depth,
                spacer2,
                this.nps,
                new Text(System.lineSeparator()),
                new Text(System.lineSeparator()),
                pvLines.get(0),
                new Text(System.lineSeparator()));
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
        for(Text pvLine : pvLines) {
            pvLine.setText("");
        }
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
            if(!pvLinesAreEnabled) {
                return;
            }
            
            if(infos.length > 5 && !infos[5].isEmpty()) {
                pvLines.get(0).setText(infos[5]);
            }

            for(int i=6;i<infos.length;i++) {
                if(pvLines.size() > i-5) {
                    if(!infos[i].isEmpty()) {
                        pvLines.get(i-5).setText(infos[i]);
                    } else {
                        pvLines.get(i-5).setText("");
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
        txtEngineOut.getChildren().remove(7, cntChildren);
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
            gameModel.setMultiPvChange(false);
        }
        // I think that if we always reset the pvlines here then there's 
        // no need for the wasMultiPvCganged functionality in Gamemodel.
	// This is the only place where it's used, so it could just as well
	// be removed.
        resetPVLines();
        // The last depth-text from analysis mode remained after mode change 
        // to playing black or white (or to a new game).
        depth.setText("");
        // There was a similar problem with nps (but this didn't seem to
	// help completely).
	nps.setText("");
    }

}
