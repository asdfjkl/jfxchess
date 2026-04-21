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

import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class EngineThread extends Thread {

    static final Pattern REG_MOVES = Pattern.compile("\\s[a-z]\\d[a-z]\\d([a-z]{0,1})");
    static final Pattern REG_BESTMOVE = Pattern.compile("bestmove\\s([a-z]\\d[a-z]\\d[a-z]{0,1})");
    static final Pattern REG_STRENGTH = Pattern.compile("UCI_Elo value \\d+");

    private final BlockingQueue<String> cmdQueue;
    Process engineProcess;
    BufferedReader engineOutput;
    BufferedWriter engineInput;
    private long lastInfoUpdate = 0;
    private long lastBestmoveUpdate = 0;

    private final EngineInfo engineInfo;

    private boolean readyok = false;
    private boolean uciok = false;

    private String sharedString;
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    public EngineThread(BlockingQueue<String> cmdQueue) {
        this.engineInfo = new EngineInfo();
        this.cmdQueue = cmdQueue;
        lastInfoUpdate = System.currentTimeMillis();
        lastBestmoveUpdate = System.currentTimeMillis();
        setDaemon(true);
    }

    public synchronized boolean engineIsOn() {
        return (engineProcess != null && engineProcess.isAlive());
    }

    public void setSharedString(String newValue) {
        String oldValue = sharedString;
        sharedString = newValue;
        support.firePropertyChange("engineInfoFromThread", oldValue, newValue);
    }

    public String getSharedString() {
        return sharedString;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    public void engineInfoSetPVLines(int n)
    {
        engineInfo.nrPvLines = n;
    }


    private void take_write_and_flush(String cmd) {
        try {
            String ignored = cmdQueue.poll(100, TimeUnit.MILLISECONDS); // <- FIX
            if (ignored == null) {
                return;
            }
            engineInput.write(cmd + "\n");
            //System.out.println(">>> "+cmd);
            engineInput.flush();
        } catch (IOException e) {
            e.printStackTrace(System.out);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // <- FIX
        }
    }


    private void cleanupProcess() {
        if (engineIsOn()) {
            try {
                engineInput.write("quit\n");
                engineInput.flush();

                boolean finished = engineProcess.waitFor(500, TimeUnit.MILLISECONDS);
                if (!finished) {
                    engineProcess.destroy();
                }

            } catch (Exception e) {
                engineProcess.destroy(); // fallback
            }
        }
    }

    @Override
    public void run() {
        try { // wrap loop in try/finally for guaranteed cleanup

            while (!Thread.currentThread().isInterrupted()) {

                // Set the thread to loop at about 1000 times per second.
                // It Keeps CPU-load down and is probably more than enough.
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // restore interrupt flag
                    break;
                }

                // Process engine output
                if (engineOutput != null) {
                    int linesRead = 0;
                    try {
                        while (engineOutput.ready() && linesRead < 100) {
                            String line = engineOutput.readLine();

                            if (line.contains("readyok")) {
                                readyok = true;
                                continue;
                            }
                            if (line.contains("uciok")) {
                                uciok = true;
                                continue;
                            }
                            if (!line.isEmpty()) {
                                if (line.startsWith("bestmove")) {
                                    engineInfo.bestmove = "BESTMOVE|"
                                            + line.substring(9)
                                            +"|"+engineInfo.score.get(0)
                                            +"|"+String.join(" ", engineInfo.pvList)
                                            +"|"+engineInfo.seesMate.get(0)
                                            +"|"+engineInfo.mate.get(0)
                                            +"|"+engineInfo.zobrist;
                                    linesRead++;
                                    continue;
                                }
                                // update engine info with
                                // other output lines
                                engineInfo.update(line);
                            }
                            linesRead++;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // send update to GUI
                long currentMs = System.currentTimeMillis();
                if ((currentMs - lastInfoUpdate) > 100) {
                    setSharedString("INFO " + engineInfo.toHtml());
                    lastInfoUpdate = currentMs;
                }

                // we need to constantly send "bestmove". If we only send it once,
                // and the user keeps flooding the GUI with events, i.e. by frequently resizing
                // the window or other inputs, the GUI might skip to handle (the only one)
                // bestmove info. Instead, the GUI will receive bestmove frequently
                // but ignore the info, if already processed.
                if ((currentMs - lastBestmoveUpdate) > 800) {
                    setSharedString(engineInfo.bestmove);
                    lastBestmoveUpdate = currentMs;
                }

                if (!engineIsOn()) {
                    // engine is not running
                    if (!cmdQueue.isEmpty()) {
                        try {
                            // Here we dispose of (or consume) the next command
                            // sent to a dead engine, or start a new engine process
                            // if we find a start command.
                            // This makes it OK for the engineController to
                            // always send stop and quit first, when restarting
                            // an engine, without first checking if the engine is on.
                            // however, don't just take(), poll with poll(timeout) to
                            // avoid deadlocks
                            String cmd = cmdQueue.poll(100, TimeUnit.MILLISECONDS); // <- FIX
                            if (cmd == null) {
                                continue;
                            }
                            if (cmd.startsWith("start")) {
                                // reset engine info if we start
                                engineInfo.clear();
                                String engineCmd = cmd.substring(6);

                                try {
                                    this.engineProcess = new ProcessBuilder(engineCmd).start();
                                    this.engineInput = new BufferedWriter(
                                            new OutputStreamWriter(engineProcess.getOutputStream()));
                                    this.engineOutput = new BufferedReader(
                                            new InputStreamReader(engineProcess.getInputStream()));

                                    // reset "state" variables
                                    readyok = false;
                                    uciok = false;
                                } catch (IOException e) {
                                    e.printStackTrace(System.out);
                                }
                                this.engineInfo.strength = -1;
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                    continue;
                }

                // When we have come this far in the while-loop
                // we know that the process is alive -> engine is running.
                // The commands uci, quit, setoption and isready are
                // treated in special ways. We are not expecting any
                // other commands from the engine controller until isready
                // has been sent at least once, and we have received readyok
                // from the engine.
                if (!cmdQueue.isEmpty()) {
                    // The problem of sending stop first if we are in "go infinite"-mode
                    // is handled in EngineController.
                    // Don't remove from queue until we know which command it is.
                    // It could be some other command just waiting for us to
                    // pass the readyok check below, now or in the next loop, maybe.
                    String cmd = cmdQueue.peek();
                    if (cmd == null) {
                        // What to do here?
                        // Better luck next loop!
                        continue;
                    }

                    // even after stop, quit and waiting for the process to die,
                    // the process.isAlive() method can take time
                    // before it answers false. So when restarting, the start
                    // command was being sent to the dead engine as a normal
                    // command below. The following statement prevents that.
                    if (cmd.startsWith("start")) {
                        continue;
                    }

                    // The command uci must be sent immediately after startup.
                    // Some engines will not report readyok on isready directly
                    // after startup (like e.g. arasan). thus we require of the
                    // engine controller to always send 'uci' after starting an
                    // engine process by the start command.
                    if (cmd.equals("uci")) {
                        take_write_and_flush(cmd);
                        uciok = false;
                        // Set the uciok flag to false.
                        // This thread won't send any other commands
                        // until uciok has been received.
                        continue;
                    }

                    // Go no further until we have received uciok.
                    if (!uciok) {
                        continue;
                    }

                    // When we have reached this point in the while-loop
                    // we know that the engine is ready to receive other
                    // commands than uci. We could always be ready to send
                    // the quit command if it appears here, (even before isready).
                    if (cmd.equals("quit")) {
                        // reset engine info if we quit
                        engineInfo.clear();
                        take_write_and_flush(cmd);
                        try {
                            boolean finished = engineProcess.waitFor(500, TimeUnit.MILLISECONDS);
                            if (!finished) {
                                engineProcess.destroy();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt(); // raise interrupt
                            break;
                        }
                        continue;
                    }

                    // We can (and maybe should, according to the UCI-protocol),
                    // be ready to send the setoption commands directly after uciok
                    // has been received.
                    if (cmd.startsWith("setoption")) {
                        if (cmd.startsWith("setoption name MultiPV value")) {
                            engineInfo.nrPvLines = Integer.parseInt(cmd.substring(29));
                        }
                        take_write_and_flush(cmd);
                        continue;
                    }

                    if (cmd.equals("stop")) {
                        take_write_and_flush(cmd);
                        continue;
                    }

                    if (cmd.equals("isready")) {
                        readyok = false;
                        take_write_and_flush(cmd);
                        continue;
                    }

                    if (!readyok) {
                        continue;
                    }

                    if (cmd.startsWith("position")) {
                        Matcher matchMoves = REG_MOVES.matcher(cmd);
                        int cnt = 0;
                        while (matchMoves.find()) cnt++;
                        if (cnt > 0) engineInfo.halfmoves = cnt;
                    }

                    if ((cmd.startsWith("position fen")) || (cmd.startsWith("position startpos"))) {
                        engineInfo.setFen(cmd);
                    }

                    take_write_and_flush(cmd);
                }
            }

        } finally {
            cleanupProcess(); // ALWAYS executed
        }
    }


    public void terminate() {
        interrupt();
    }


}