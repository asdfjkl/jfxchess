/* JFXChess - A Chess Graphical User Interface
 * Copyright (C) 2026 Dominik Klein
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

import java.beans.PropertyChangeListener;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class EngineSession {

    private final BlockingQueue<String> commandQueue = new LinkedBlockingQueue<>();
    private final EngineThread engineThread = new EngineThread(commandQueue);
    private Engine activeEngine;
    private boolean shutdownRequested;

    public EngineSession() {
        engineThread.start();
    }

    public void sendCommand(String command) throws InterruptedException {
        commandQueue.put(command);
    }

    public boolean isEngineRunning() {
        return engineThread.engineIsOn();
    }

    public void setPvLines(int count) {
        engineThread.engineInfoSetPVLines(count);
    }

    public Engine getActiveEngine() {
        return activeEngine;
    }

    public void setActiveEngine(Engine activeEngine) {
        this.activeEngine = activeEngine;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        engineThread.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        engineThread.removePropertyChangeListener(listener);
    }

    public void stop() {
        shutdownRequested = true;
        engineThread.terminate();
    }

    public boolean isShutdownRequested() {
        return shutdownRequested;
    }
}
