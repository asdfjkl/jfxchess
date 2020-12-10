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

package org.asdfjkl.jerryfx.gui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

public class EngineController {

    final EngineThread engineThread;
    final BlockingQueue<String> cmdQueue = new LinkedBlockingQueue<>();

    public EngineController(ModeMenuController modeMenuController) {

        final AtomicReference<String> count = new AtomicReference<>();
        //cmdQueue = new LinkedBlockingQueue<String>();
        engineThread = new EngineThread(cmdQueue);
        engineThread.stringProperty().addListener(new ChangeListener<>() {
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
        // Don't start the thread until we're ready to use the engine!
        engineThread.start();

    }

    public void testStartAndGoInf() {

        //engineThread.start();
        String cmdStockfish = "C:\\Program Files (x86)\\Jerry_Chess\\engine\\stockfish.exe";
        String goInf = "go infinite";

        try {
            cmdQueue.put("start "+cmdStockfish);
            cmdQueue.put(goInf);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void sendCommand(String cmd) {
        try {
            cmdQueue.put(cmd);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setStockfishStrength(int level) {
        try {
            cmdQueue.put("setoption name Skill Level value "+level);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void uciGoMoveTime(int milliseconds) {
        try {
            cmdQueue.put("go movetime "+milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
