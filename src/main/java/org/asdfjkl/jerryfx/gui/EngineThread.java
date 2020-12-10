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

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.asdfjkl.jerryfx.engine.UciEngineProcess;

import java.io.*;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EngineThread extends Thread {

    static final Pattern REG_MOVES = Pattern.compile("\\s[a-z]\\d[a-z]\\d([a-z]{0,1})");
    static final Pattern REG_BESTMOVE = Pattern.compile("bestmove\\s([a-z]\\d[a-z]\\d[a-z]{0,1})");
    static final Pattern REG_STRENGTH = Pattern.compile("Skill Level value \\d+");

    private final StringProperty stringProperty;

    private final BlockingQueue<String> cmdQueue;
    UciEngineProcess engineProcess;
    private long lastInfoUpdate = System.currentTimeMillis();
    private long lastBestmoveUpdate = System.currentTimeMillis();

    private final EngineInfo engineInfo;

    private boolean readyok = false;
    private boolean inGoInfinite = false;

    public EngineThread(BlockingQueue<String> cmdQueue) {
        this.engineInfo = new EngineInfo();
        this.cmdQueue = cmdQueue;
        stringProperty = new SimpleStringProperty(this, "String", "");
        setDaemon(true);
    }

    // What on earth is this for?
    public String getString() {
        return stringProperty.get();
    }

    public StringProperty stringProperty() {
        return stringProperty;
    }

    @Override
    public void run() {
        while (true) {
            if(engineProcess == null) { // engine not running
                if(!cmdQueue.isEmpty()) {
                    startEngine();
                }
            }
            else { // process is alive -> engine is running
                try {
                    processEngineResponse();
                    sendUpdate();
                    serviceQueue();
                }
                catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                catch (InterruptedException e) {
                    return;
                }
            }
            if (this.isInterrupted()) {
                if(engineProcess != null) {
                    engineProcess.close();
                }
                return;
            }
        }
    }

    private void serviceQueue()
            throws IOException, InterruptedException {
        if (!cmdQueue.isEmpty()) {
            if (inGoInfinite) {
                stopThinking();
            }
            else {
                if (!readyok) {
                    initialise();
                }
                else {
                    processCommand();
                }
            }
        }
    }

    private void startEngine() {
        try {
            String cmd = cmdQueue.take();
            if (cmd.startsWith("start")) {
                // reset engine info if we start
                engineInfo.clear();
                File engineCmd = new File(cmd.substring(6));
                try {
                    this.engineProcess = new UciEngineProcess(engineCmd);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                this.engineInfo.strength = -1;
            }
        }
        catch (InterruptedException e) {
            // Propogate the interrupt
            interrupt();
        }
    }

    private void initialise()
            throws IOException, InterruptedException {
        // the command uci must be send immediately after startup
        // some engines will not report readyok on isready directly
        // after startup (like e.g. arasan). thus always send
        // 'uci' without waiting for isready
        // TODO: Doesn't peeking here lead us to an infinite loop as the non-uci command will never be taken off given we don't have a 'ready' engine?
        String cmd = cmdQueue.peek();
        if(cmd != null && cmd.equals("uci")) {
            cmdQueue.take();
            engineProcess.sendSynchronous("uci");
            engineProcess.sendSynchronous("isready");
            readyok = true;
        }
    }

    private void processCommand()
            throws InterruptedException, IOException {
        String cmd = cmdQueue.take();
        // if the command is "position fen moves", first count the
        // numbers of moves so far to generate move numbers in engine info
        // todo: needed???
        if(cmd.startsWith("position")) {
            Matcher matchMoves = REG_MOVES.matcher(cmd);
            int cnt = 0;
            while(matchMoves.find()) {
                cnt++;
            }
            if(cnt > 0) {
                engineInfo.halfmoves = cnt;
            }
        }

        if(cmd.startsWith("position fen")) {
            String fen = cmd.substring(13);
            engineInfo.setFen(fen);
        }

        if(cmd.startsWith("go infinite")) {
            inGoInfinite = true;
        }

        if(cmd.startsWith("setoption name Skill Level")) {
            Matcher matchExpressionStrength = REG_STRENGTH.matcher(cmd);
            if(matchExpressionStrength.find()) {
                engineInfo.strength = Integer.parseInt(matchExpressionStrength.group().substring(18));
            }
        }

        if(cmd.startsWith("setoption name MultiPV value")) {
            engineInfo.nrPvLines = Integer.parseInt(cmd.substring(29,30));
        }

        // reset engine info if we quit
        if(cmd.contains("quit")) {
            engineInfo.clear();
        }

        engineProcess.send(cmd);
    }

    private void sendUpdate() {
        long currentMs = System.currentTimeMillis();
        if((currentMs - lastInfoUpdate) > 100) {
            stringProperty.set("INFO " + engineInfo.toString());
            lastInfoUpdate = currentMs;
        }
        // we need to constantly send "bestmove". If we only send it once,
        // and the user keeps flooding the GUI with events (i.e. by frequently resizing
        // the window or other inputs, the GUI might skip to handle (the only one)
        // bestmove info. Instead, the GUI will receive bestmove frequently
        // but ignore the info, if already processed.
        if((currentMs - lastBestmoveUpdate) > 800) {
            stringProperty.set(engineInfo.bestmove);
            lastBestmoveUpdate = currentMs;
        }
    }

    private List<String> stopThinking()
            throws IOException {
        inGoInfinite = false;
        engineProcess.send("stop");
        return engineProcess.receive();
    }

    private void processEngineResponse()
            throws IOException {
        List<String> response = engineProcess.receive();
        for(String line: response) {
            if (!line.isEmpty()) {
                //System.out.println(line);
                //lastString = line;
                // todo: instead of directly setting bestmove,
                // try updating engine info
                if(line.startsWith("bestmove")) {
                    engineInfo.bestmove = "BESTMOVE|"
                            + line.substring(9)
                            +"|"+engineInfo.score.get(0)
                            +"|"+String.join(" ", engineInfo.pvList)
                            +"|"+engineInfo.seesMate.get(0)
                            +"|"+engineInfo.mate.get(0);
                } else {
                    engineInfo.update(line);
                }
            }
        }
    }

}
