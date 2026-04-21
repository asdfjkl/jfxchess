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

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class DialogEngines extends JDialog {

    private final DefaultListModel<Engine> engineListModel;
    private JList<Engine> engineList;
    private JButton btnRemove;
    private JButton btnEdit;
    private JButton btnReset;
    private boolean isConfirmed = false;
    private Engine selectedEngine = null;

    public DialogEngines(Frame parent, ArrayList<Engine> engines, int idxActiveEngine) {
        super(parent, "Chess Engines", true);

        engineListModel = new DefaultListModel<>();
        for (Engine e : engines) {
            engineListModel.addElement(e.makeCopy());
        }

        initUI(idxActiveEngine);
        setSize(300, 350);
        setLocationRelativeTo(parent);
    }

    private void initUI(int idxActiveEngine) {
        setLayout(new BorderLayout(10, 10));

        // ===== CENTER PANEL (2 columns) =====
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        add(centerPanel, BorderLayout.CENTER);

        // LEFT: List
        engineList = new JList<>(engineListModel);
        JScrollPane listScroll = new JScrollPane(engineList);
        centerPanel.add(listScroll, BorderLayout.CENTER);
        engineList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        engineList.addListSelectionListener(e -> {
            listValueChanged(e);
        });

        // RIGHT: Buttons column
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        centerPanel.add(rightPanel, BorderLayout.EAST);

        JButton btnAdd = new JButton("Add");
        btnAdd.addActionListener(e -> {
            addEngine();
        });
        btnRemove = new JButton("Remove");
        btnRemove.addActionListener(e -> {
            removeEngine();
        });
        btnEdit = new JButton("Edit Parameters");
        btnEdit.addActionListener(e -> {
            editParameters();
        });
        btnReset = new JButton("Reset Parameters");
        btnReset.addActionListener(e -> {
            resetParameters();
        });

        // Determine max width/height
        Dimension maxSize = btnReset.getPreferredSize();

        maxSize = new Dimension(
                Math.max(Math.max(btnAdd.getPreferredSize().width, btnRemove.getPreferredSize().width),
                        Math.max(btnEdit.getPreferredSize().width, btnReset.getPreferredSize().width)),
                maxSize.height
        );

        // Apply same size to all buttons
        for (JButton b : new JButton[]{btnAdd, btnRemove, btnEdit, btnReset}) {
            b.setPreferredSize(maxSize);
            b.setMaximumSize(maxSize);
            b.setMinimumSize(maxSize);
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
        }


        rightPanel.add(btnAdd);
        rightPanel.add(Box.createVerticalStrut(5));
        rightPanel.add(btnRemove);

        // Expanding space
        rightPanel.add(Box.createVerticalGlue());

        rightPanel.add(btnEdit);
        rightPanel.add(Box.createVerticalStrut(5));
        rightPanel.add(btnReset);

        // ===== BOTTOM: OK / Cancel =====
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnOK = new JButton("OK");
        JButton btnCancel = new JButton("Cancel");

        btnOK.addActionListener(e -> {
            isConfirmed = true;
            dispose();
        });
        btnCancel.addActionListener(e -> dispose());

        bottomPanel.add(btnOK);
        bottomPanel.add(btnCancel);

        add(bottomPanel, BorderLayout.SOUTH);

        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(0,10,0,10));

        // at the last - at this point, buttons have been created, and the event
        // induced by list selection will trigger the buttons to be disabled
        // for the internal engine
        engineList.setSelectedIndex(idxActiveEngine);

        // for snap: snap's are sandboxed and we can't start external engines
        // deactivate the "add" button during build
        //btnAdd.setEnabled(false);
    }

    public ArrayList<Engine> getEngines() {
        ArrayList<Engine> engines = new ArrayList<>();
        for (int i = 0; i < engineListModel.size(); i++) {
            engines.add(engineListModel.get(i));
        }
        return engines;
    }

    private void editParameters() {
        Engine selectedEngine = engineList.getSelectedValue();
        DialogEngineOptions dlg = new DialogEngineOptions(this, selectedEngine.options);
        dlg.setVisible(true);
        if(dlg.isConfirmed()) {
            selectedEngine.options = dlg.getOptions();
        }
    }

    private void resetParameters() {
        Engine selectedEngine = engineList.getSelectedValue();
        for(EngineOption enOpt : selectedEngine.options) {
            enOpt.resetToDefault();
        }
    }

    private void removeEngine() {
        Engine selectedEngine = engineList.getSelectedValue();
        engineListModel.removeElement(selectedEngine);
    }

    private void addEngine() {

        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if(result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();


        // This first try-catch block will catch any exception thrown
        // inside and present it as part of a user alert.
        try {
            String line;

            String[] cmd = { file.getAbsolutePath() };
            Process engineProcess = Runtime.getRuntime().exec(cmd);

            if (!engineProcess.isAlive()) {
                throw new RuntimeException("Couldn't start engine process " + file.getAbsolutePath() + " ");
            }

            // This is a try-with-resources block (without a catch block).
            // When the execution leaves this block,normally or because of
            // an exception, bre.close(), bri.close() and bro.close() will
            // be called automatically, in that order.
            // Notice that bro.close() unexpectedly also kills the
            // engine-process in some way. So we don't have to do that
            // separately. Possible exceptions during close() will be
            // suppressed. (Previously the engine-process would stay alive
            // if it had been started right and an exception abrupt the
            // code-flow.)
            try (BufferedWriter bro = new BufferedWriter(new OutputStreamWriter(engineProcess.getOutputStream()));
                 BufferedReader bri = new BufferedReader(new InputStreamReader(engineProcess.getInputStream()));
                 BufferedReader bre = new BufferedReader(new InputStreamReader(engineProcess.getErrorStream()))) {

                for (int i = 0; i < 20; i++) {
                    try {
                        Thread.sleep(40);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // Send "uci" to the engine.
                try {
                    bro.write("uci\n");
                    bro.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException("Failed to send UCI-commands to the engine process. "
                            + file.getAbsolutePath()
                            + e.getClass() + ": " + e.getMessage());
                }

                for (int i = 0; i < 20; i++) {
                    try {
                        Thread.sleep(40);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                Engine engine = new Engine();
                engine.setPath(file.getAbsolutePath());

                // Read all the engine options.
                try {
                    while (bri.ready()) {
                        line = bri.readLine();
                        if(line.equals("uciok")){
                            // No more options
                            break;
                        }
                        if (line.startsWith("id name")) {
                            engine.setName(line.substring(7).trim());
                            continue;
                        }
                        if(line.startsWith("id author")) {
                            continue;
                        }
                        try {
                            EngineOption engineOption = new EngineOption();
                            boolean parsed = engineOption.parseUciOptionString(line);
                            if (parsed) {
                                engine.addEngineOption(engineOption);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw (new RuntimeException("Couldn't parse engine option: "
                                    + line + "  " + e.getClass() + ": " + e.getMessage()));
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException("Failed to read commands from the engine process "
                            + file.getAbsolutePath() + " "
                            + e.getClass() + ": " + e.getMessage());
                }

                // Don't know if this is meaningful, but since we have a bre...
                while (bre.ready()) {
                    System.err.println("Error message from engine: " + bre.readLine());
                }

                // Stop the engine
                try {
                    bro.write("stop\n");
                    bro.write("quit\n");
                    bro.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException("Failed to send stop and quit to the engine process. "
                            + file.getAbsolutePath() + " "
                            + e.getClass() + ": " + e.getMessage());
                }

                // Wait for engine to quit.
                try {
                    boolean finished = engineProcess.waitFor(500, TimeUnit.MILLISECONDS);
                    if (!finished) {
                        engineProcess.destroy();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Add engine to the engineList and make the list item selected.
                if (engine.getName() != null && !engine.getName().isEmpty()) {
                    engineListModel.addElement(engine);
                    int idx = engineListModel.indexOf(engine);
                    engineList.setSelectedIndex(idx);
                }
            } // end of try-with-resources
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
        }
    }

    private void listValueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting() == false) {
            if(!engineList.isSelectionEmpty()) {
                selectedEngine = engineList.getSelectedValue();
                if(engineList.getSelectedIndex() == 0) {
                    // never allow to remove internal engine
                    // or to mess parameters of internal engine
                    btnRemove.setEnabled(false);
                    btnEdit.setEnabled(false);
                    btnReset.setEnabled(false);
                } else {
                    btnRemove.setEnabled(true);
                    btnEdit.setEnabled(true);
                    btnReset.setEnabled(true);
                }
            }
        }
    }

    public boolean isConfirmed() {
        return isConfirmed;
    }

    public Engine getSelectedEngine() {
        // return Stockfish/internal as default, if
        // no selection is made
        if(selectedEngine == null) {
            return engineListModel.get(0);
        } else {
            return selectedEngine;
        }
    }

}
