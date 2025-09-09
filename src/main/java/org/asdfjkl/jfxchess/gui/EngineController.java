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

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

public class EngineController {

    final EngineThread engineThread;
    final BlockingQueue<String> cmdQueue = new LinkedBlockingQueue<String>();
    Engine currentEngine = null;
    private boolean inGoInfinite = false;
    
    public EngineController(ModeMenuController modeMenuController) {
        final AtomicReference<String> count = new AtomicReference<>();
        //cmdQueue = new LinkedBlockingQueue<String>();
        engineThread = new EngineThread(cmdQueue);
        engineThread.stringProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(final ObservableValue<? extends String> observable,
                                final String oldValue, final String newValue) {
                if (count.getAndSet(newValue) == null) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            String value = count.getAndSet(null);
                            modeMenuController.handleEngineInfo(value);
                        }
                    });
                }
            }
        });

        engineThread.start();
    }

    // public void testStartAndGoInf() {

    //     //engineThread.start();
    //     String cmdStockfish = "C:\\Program Files (x86)\\Jerry_Chess\\engine\\stockfish.exe";
    //     String goInf = "go infinite";

    //     try {
    //         cmdQueue.put("start "+cmdStockfish);
    //         cmdQueue.put(goInf);
    //     } catch (InterruptedException e) {
    //         e.printStackTrace();
    //     }

    // }

    public void sendCommand(String cmd) {
        //System.out.println(">>> "+cmd);
        if (cmd.equals("go infinite")) {
            inGoInfinite = true;
        } else {
            if (inGoInfinite && (!cmd.equals("stop"))
                    && (!cmd.equals("quit"))) {
                try {
                    cmdQueue.put("stop");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //inGoInfinite = false;
            }
            inGoInfinite = false;
        }
        try {
            cmdQueue.put(cmd);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stopEngine() {
        sendCommand("stop");
        sendCommand("quit");
        do {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (engineThread.engineIsOn());
    }

    public void restartEngine(Engine activeEngine) {
        currentEngine = activeEngine;
        // It's OK to send stop and quit even if the engine process
        // inside the engine thread is not running. These commands will
        // just be consumed by the engine thread in that case.
        stopEngine();
        // Restart engine.
        sendCommand("start " + activeEngine.getPath());
        int countMs = 0;
        do {
            try {
                Thread.sleep(10);
                countMs += 10;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (!engineThread.engineIsOn() && countMs < 1500);


        // Since the engine is either internal, or we have
        // been able to set up the engine in the
        // Mode->Engines... dialog, we know it's a UCI-engine.
        // But since some engines won't accept commands prior
        // to uci, so:
        sendCommand("uci");
        // Here the engine thread will wait for uciok from engine,
        // which is according to the UCI-protocol, but meanwhile we
        // continue with pumping setoption commands into the cmdQueue.
        for(EngineOption enOpt : activeEngine.options) {
            // Always send UCI_Elo even if it has default value.
            // It won't be used without UCI_LimitStrength.
            // (when we send UCI_LimitStrength explicitly in the program
            // we require that UCI_Elo should have been sent at startup, so
            // there will be a saved ELO-value to show in the EngineOutputView.)
            if(enOpt.isNotDefault() || enOpt.name.equals("UCI_Elo")) {
                sendCommand(enOpt.toUciCommand());
            }
        }
        // The engine thread requires an isready
        // before any other commands can be sent,
        // except for uci, setoption and quit.
        sendCommand("isready");
        sendCommand("ucinewgame");
        // An isready should be sent after ucinewgame.
        sendCommand("isready");
    }

    // public void setStockfishStrength(int level) {
    //     sendCommand("setoption name Skill Level value "+level);
    // }

    // public void setSkillLevelInternal(int engineStrength) {
    //     if(currentEngine != null && currentEngine.isInternal()) {
    //         sendCommand("setoption name Skill Level value " + engineStrength);
    //     }
    // }
    
    public void setUciLimitStrength(Boolean val) {
        if (currentEngine != null && currentEngine.supportsUciLimitStrength()) {
            sendCommand("setoption name UCI_LimitStrength value " + val);
        }
    }

    public void setUciElo(int elo) {
        sendCommand("setoption name UCI_Elo value " + elo);
    }

    public void setMultiPV(int n) {
        if (currentEngine != null && currentEngine.supportsMultiPV()) {
            sendCommand("setoption name MultiPV value " + n);
        }
        // (I had a problem with adding and removing PV-lines
        // in the outputview before the enginestart after edit-engines.)
        // So, in case the engine hasn't been started yet:
        engineThread.engineInfoSetPVLines(n);
    }

    public void sendNewPosition(String fen) {
        sendCommand("stop");
        sendCommand("position fen " + fen);
    }

    public void uciGoMoveTime(int milliseconds) {
        sendCommand("go movetime "+ milliseconds);
    }

    public void uciGoInfinite() {
        sendCommand("go infinite");
    }

    // The next method is for setting information in the
    // engineOutputView (indirectly via engineInfo) when the engine
    // has not been started yet, after editing engines or at
    // startup of the program when restoring engines.
    public void engineInfoSetValues(String engineID, int pvLines, boolean limitStrength, int elo) {
        engineThread.engineInfoSetID(engineID);
        engineThread.engineInfoSetPVLines(pvLines);
        engineThread.engineInfoSetLimitedStrength(limitStrength);
        engineThread.engineInfoSetStrength(elo);
    }
}
