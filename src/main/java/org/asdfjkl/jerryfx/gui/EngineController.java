package org.asdfjkl.jerryfx.gui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

public class EngineController {

    final EngineThread engineThread;
    final BlockingQueue<String> cmdQueue = new LinkedBlockingQueue<String>();;

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
