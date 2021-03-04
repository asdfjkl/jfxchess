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

import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EngineThread extends Thread {

    static final Pattern REG_MOVES = Pattern.compile("\\s[a-z]\\d[a-z]\\d([a-z]{0,1})");
    static final Pattern REG_BESTMOVE = Pattern.compile("bestmove\\s([a-z]\\d[a-z]\\d[a-z]{0,1})");
    static final Pattern REG_STRENGTH = Pattern.compile("Skill Level value \\d+");

    private final StringProperty stringProperty;
    private final int counter = 0;

    private final BlockingQueue cmdQueue;
    Process engineProcess;
    BufferedReader engineOutput;
    BufferedWriter engineInput;
    private volatile boolean running = true;
    private long lastInfoUpdate = 0;
    private long lastBestmoveUpdate = 0;

    private final EngineInfo engineInfo;

    private boolean readyok = false;
    private boolean requestedReadyOk = false;
    //private boolean engineRunning = false;
    private boolean inGoInfinite = false;

    public EngineThread(BlockingQueue cmdQueue) {
        this.engineInfo = new EngineInfo();
        this.cmdQueue = cmdQueue;
        stringProperty = new SimpleStringProperty(this, "String", "");
        lastInfoUpdate = System.currentTimeMillis();
        lastBestmoveUpdate = System.currentTimeMillis();
        setDaemon(true);
    }

    public String getString() {
        return stringProperty.get();
    }

    public StringProperty stringProperty() {
        return stringProperty;
    }

    @Override
    public void run() {
        while (running) {
            if (this.isInterrupted()) {
                // here: delete process if it exists
                if(engineProcess != null && engineProcess.isAlive()) {
                    engineProcess.destroy();
                }
                running = false;
            }
            // process engine output
            if (engineOutput != null) {
                int linesRead = 0;
                try {
                    while (engineOutput.ready() && linesRead < 100) {
                        String line = engineOutput.readLine();
                        if(line.contains("readyok")) {
                            readyok = true;
                        } else {
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
                                            +"|"+engineInfo.mate.get(0)
                                            +"|"+engineInfo.zobrist;
                                } else {
                                    //System.out.println("thread: "+line);
                                    engineInfo.update(line);
                                }
                            }
                        }
                        //stringProperty.set(line);
                        linesRead++;
                    }
                } catch (IOException e) {
                }
            }
            // send update
            long currentMs = System.currentTimeMillis();
            if((currentMs - lastInfoUpdate) > 100) {
                stringProperty.set("INFO " + engineInfo.toString());
                //System.out.println("thread info: "+engineInfo.toString());
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
            if(engineProcess == null || !engineProcess.isAlive()) { // engine not running
                if(!cmdQueue.isEmpty()) {
                    try {
                        String cmd = (String) cmdQueue.take();
                        if (cmd.startsWith("start")) {
                            // reset engine info if we start
                            engineInfo.clear();
                            String engineCmd = cmd.substring(6);
                            try {
                                this.engineProcess = new ProcessBuilder(engineCmd).start();
                                this.engineInput = new BufferedWriter(new OutputStreamWriter(engineProcess.getOutputStream()));
                                this.engineOutput = new BufferedReader(new InputStreamReader(engineProcess.getInputStream()));
                                //engineRunning = true;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            this.engineInfo.strength = -1;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else { // process is alive -> engine is running
                if(!cmdQueue.isEmpty()) {
                    // if we are in go infty, first send stop
                    if(inGoInfinite) {
                        try {
                            engineInput.write("stop\n");
                            engineInput.flush();
                            inGoInfinite = false;
                            continue;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        // not in go infinite mode, then first
                        // check if engine is ready to receive commands
                        // but only do this once (check via requestedReadyOk)
                        if(!readyok) {
                            // the command uci must be send immediately after startup
                            // some engines will not report readyok on isready directly
                            // after startup (like e.g. arasan). thus always send
                            // 'uci' without waiting for isready
                            String cmd = (String) cmdQueue.peek();
                            if(cmd != null && cmd.equals("uci")) {
                                try {
                                    cmdQueue.take();
                                    engineInput.write("uci\n");
                                    engineInput.flush();
                                    continue;
                                } catch (InterruptedException | IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            if(!requestedReadyOk) {
                                try {
                                    engineInput.write("isready\n");
                                    engineInput.flush();
                                    requestedReadyOk = true;
                                    continue;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            // engine is ready to receive commands
                            // take command from queue
                            try {
                                String cmd = (String) cmdQueue.take();
                                // if the command is "position fen moves", first count the
                                // numbers of moves so far to generate move numbers in engine info
                                // todo: needed???
                                if(cmd.startsWith("sleep")) {
                                    Thread.sleep(10000);
                                }

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

                                // send and flush
                                try {
                                    this.engineInput.write(cmd + "\n");
                                    this.engineInput.flush();
                                    //System.out.println(cmd+"\n");
                                    // if we quit the engine, give some
                                    // time for the engine to quit
                                    if(cmd.contains("quit")) {
                                        Thread.sleep(500);
                                    }
                                    continue;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }


    public void terminate() {
        running = false;
    }

}
