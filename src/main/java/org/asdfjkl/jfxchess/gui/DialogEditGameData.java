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
import java.awt.*;
import java.text.NumberFormat;
import java.time.MonthDay;
import java.time.Year;
import java.util.HashMap;
import java.util.Map;

public class DialogEditGameData extends JDialog {

    private boolean confirmed = false;

    // Text fields
    private final JTextField whiteFirstName = new JTextField(15);
    private final JTextField whiteSurname = new JTextField(15);
    private final JTextField blackFirstName = new JTextField(15);
    private final JTextField blackSurname = new JTextField(15);
    private final JTextField site = new JTextField(15);
    private final JTextField event = new JTextField(15);

    // Spinners
    private JSpinner yearSpinner;
    private JSpinner monthSpinner;
    private JSpinner daySpinner;
    private JSpinner roundSpinner;
    private JSpinner eloWhiteSpinner;
    private JSpinner eloBlackSpinner;

    // Result buttons
    private final JRadioButton winWhite = new JRadioButton("1-0");
    private final JRadioButton winBlack = new JRadioButton("0-1");
    private final JRadioButton draw = new JRadioButton("1/2-1/2");
    private final JRadioButton unknown = new JRadioButton("*");

    HashMap<String, String> pgnHeaders = new HashMap<>();

    public DialogEditGameData(Frame parent,  HashMap<String, String> pgnHeaders) {

        super(parent, "Game Information", true);

        for (Map.Entry<String, String> entry : pgnHeaders.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            this.pgnHeaders.put(key,value);
        }

        initSpinners();
        initUI();

        pack();

        setLocationRelativeTo(parent);
    }

    private void initSpinners() {

        yearSpinner = new JSpinner(
                new SpinnerNumberModel(Year.now().getValue(), 1000, 9999, 1));
        installPlainNumberEditor(yearSpinner, "0000");

        monthSpinner = new JSpinner(
                new SpinnerNumberModel(MonthDay.now().getMonthValue(), 1, 12, 1));
        installPlainNumberEditor(monthSpinner, "0");

        daySpinner = new JSpinner(
                new SpinnerNumberModel(MonthDay.now().getDayOfMonth(), 1, 31, 1));
        installPlainNumberEditor(daySpinner, "0");

        // try to parse existing DD.MM.YYYY (or reverse) setting
        if(pgnHeaders.get("Date") != null) {
            String[] tmpDate = pgnHeaders.get("Date").split("\\.");
            if(tmpDate.length > 0 && tmpDate[0].length() == 4) { // hopefully YYYY.MM.DD
                try {
                    int iYear = Integer.parseInt(tmpDate[0].strip());
                    yearSpinner.setValue(iYear);
                } catch(NumberFormatException e) {
                    yearSpinner.setValue(0);
                }
                if(tmpDate.length > 1) {
                    try {
                        int iMonth = Integer.parseInt(tmpDate[1].strip());
                        monthSpinner.setValue(iMonth);
                    } catch(NumberFormatException e) {
                        monthSpinner.setValue(1);
                    }
                }
                if(tmpDate.length > 2) {
                    try {
                        int iDay = Integer.parseInt(tmpDate[2].strip());
                        daySpinner.setValue(iDay);
                    } catch(NumberFormatException e) {
                        daySpinner.setValue(1);
                    }
                }
            } else if (tmpDate.length > 2 && tmpDate[2].length() == 4) { // probably DD.MM.YYYY
                try {
                    int iYear = Integer.parseInt(tmpDate[2].strip());
                    yearSpinner.setValue(iYear);
                } catch(NumberFormatException e) {
                    yearSpinner.setValue(2000);
                }
                try {
                    int iMonth = Integer.parseInt(tmpDate[1].strip());
                    monthSpinner.setValue(iMonth);
                } catch(NumberFormatException e) {
                    monthSpinner.setValue(1);
                }
                try {
                    int iDay = Integer.parseInt(tmpDate[0].strip());
                    daySpinner.setValue(iDay);
                } catch(NumberFormatException e) {
                    daySpinner.setValue(1);
                }
            }
        }

        roundSpinner = new JSpinner(
                new SpinnerNumberModel(1, 1, 99, 1));
        installPlainNumberEditor(roundSpinner, "0");
        if(pgnHeaders.get("Round") != null) {
            try {
                int iRound = Integer.parseInt(pgnHeaders.get("Round"));
                roundSpinner.setValue(iRound);
            } catch (NumberFormatException e) {
                roundSpinner.setValue(1);
            }
        }

        eloWhiteSpinner = new JSpinner(
                new SpinnerNumberModel(0, 0, 3000, 1));
        installPlainNumberEditor(eloWhiteSpinner, "0000");
        if(pgnHeaders.get("WhiteElo") != null) {
            try {
                int we = Integer.parseInt(pgnHeaders.get("WhiteElo"));
                eloWhiteSpinner.setValue(we);
            } catch (NumberFormatException e) {
                eloWhiteSpinner.setValue(0);
            }
        }

        eloBlackSpinner = new JSpinner(
                new SpinnerNumberModel(0, 0, 3000, 1));
        installPlainNumberEditor(eloBlackSpinner, "0000");
        if(pgnHeaders.get("BlackElo") != null) {
            try {
                int we = Integer.parseInt(pgnHeaders.get("BlackElo"));
                eloBlackSpinner.setValue(we);
            } catch (NumberFormatException e) {
                eloBlackSpinner.setValue(0);
            }
        }

        fixSpinnerWidth(yearSpinner, 80);
        fixSpinnerWidth(monthSpinner, 80);
        fixSpinnerWidth(daySpinner, 80);
        fixSpinnerWidth(roundSpinner, 80);
        fixSpinnerWidth(eloWhiteSpinner, 80);
        fixSpinnerWidth(eloBlackSpinner, 80);

    }

    private void initUI() {

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // Add rows
        addRow(formPanel, gbc, row++, "White Firstname:", whiteFirstName);
        addRow(formPanel, gbc, row++, "White Surname:", whiteSurname);
        addRow(formPanel, gbc, row++, "Black Firstname:", blackFirstName);
        addRow(formPanel, gbc, row++, "Black Surname:", blackSurname);
        addRow(formPanel, gbc, row++, "Site:", site);
        addRow(formPanel, gbc, row++, "Event:", event);

        // try to set defaults for text input (for spinners this is done in initSpinners()
        if(pgnHeaders.get("White") != null) {
            String[] tmpWhiteName = pgnHeaders.get("White").split(",");
            if(tmpWhiteName.length > 0) {
                whiteSurname.setText(tmpWhiteName[0].strip());
            }
            if(tmpWhiteName.length > 1) {
                whiteFirstName.setText(tmpWhiteName[1].strip());
            }
        }

        if(pgnHeaders.get("Black") != null) {
            String[] tmpBlackName = pgnHeaders.get("Black").split(",");
            if(tmpBlackName.length > 0) {
                blackSurname.setText(tmpBlackName[0].strip());
            }
            if(tmpBlackName.length > 1) {
                blackFirstName.setText(tmpBlackName[1].strip());
            }
        }

        if(pgnHeaders.get("Site") != null) {
            site.setText(pgnHeaders.get("Site"));
        }

        if(pgnHeaders.get("Event") != null) {
            event.setText(pgnHeaders.get("Event"));
        }

        addRow(formPanel, gbc, row++, "Year:", yearSpinner);
        addRow(formPanel, gbc, row++, "Month:", monthSpinner);
        addRow(formPanel, gbc, row++, "Day:", daySpinner);
        addRow(formPanel, gbc, row++, "Round:", roundSpinner);

        addRow(formPanel, gbc, row++, "Elo White:", eloWhiteSpinner);
        addRow(formPanel, gbc, row++, "Elo Black:", eloBlackSpinner);

        // Result row
        JPanel resultPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        ButtonGroup group = new ButtonGroup();
        group.add(winWhite);
        group.add(winBlack);
        group.add(draw);
        group.add(unknown);

        unknown.setSelected(true);

        if(pgnHeaders.get("Result") != null) {
            String res = pgnHeaders.get("Result");
            if(res.equals("1-0")) {
                winWhite.setSelected(true);
            }
            if(res.equals("0-1")) {
                winBlack.setSelected(true);
            }
            if(res.equals("1/2-1/2")) {
                draw.setSelected(true);
            }
        }

        resultPanel.add(winWhite);
        resultPanel.add(winBlack);
        resultPanel.add(draw);
        resultPanel.add(unknown);

        addRow(formPanel, gbc, row++, "Result:", resultPanel);

        // Buttons
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        cancelButton.addActionListener(e -> dispose());

        JPanel buttonPanel =
                new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        // Layout
        setLayout(new BorderLayout());
        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void addRow(JPanel panel, GridBagConstraints gbc,
                        int row, String labelText, JComponent field) {

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;

        panel.add(new JLabel(labelText), gbc);

        gbc.gridx = 1;

        if (field instanceof JTextField) {

            // Text fields grow and fill
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;

        } else {

            // Spinners and others stay fixed
            gbc.weightx = 0.0;
            gbc.fill = GridBagConstraints.NONE;
        }

        panel.add(field, gbc);
    }

    // =======================
    // Getters
    // =======================

    public boolean isConfirmed() {
        return confirmed;
    }

    public HashMap<String, String> getData() {

        HashMap<String, String> pgnHeaders = new HashMap<>();

        pgnHeaders.put("Site", site.getText());
        if( (Integer) roundSpinner.getValue() >= 0) {
            pgnHeaders.put("Round", Integer.toString((Integer) roundSpinner.getValue()));
        }

        if(!whiteSurname.getText().isEmpty() && !whiteFirstName.getText().isEmpty()) {
            pgnHeaders.put("White", whiteSurname.getText()+", "+whiteFirstName.getText());
        } else if(!whiteSurname.getText().isEmpty() && whiteFirstName.getText().isEmpty()) {
            pgnHeaders.put("White", whiteSurname.getText());
        } else if(whiteSurname.getText().isEmpty() && !whiteFirstName.getText().isEmpty()) {
            pgnHeaders.put("White", whiteFirstName.getText());
        }

        if(!blackSurname.getText().isEmpty() && !blackFirstName.getText().isEmpty()) {
            pgnHeaders.put("Black", blackSurname.getText()+", "+blackFirstName.getText());
        } else if(!blackSurname.getText().isEmpty() && blackFirstName.getText().isEmpty()) {
            pgnHeaders.put("Black", blackSurname.getText());
        } else if(blackSurname.getText().isEmpty() && !blackFirstName.getText().isEmpty()) {
            pgnHeaders.put("Black", blackFirstName.getText());
        }

        if( (Integer) eloWhiteSpinner.getValue() > 0) {
            pgnHeaders.put("WhiteElo", Integer.toString((Integer) eloWhiteSpinner.getValue()));
        }
        if( (Integer) eloBlackSpinner.getValue() >= 0) {
            pgnHeaders.put("BlackElo", Integer.toString((Integer) eloBlackSpinner.getValue()));
        }

        String tmpDate = "";
        if((Integer) yearSpinner.getValue() > 0 && (Integer )yearSpinner.getValue() < 3000) {
            tmpDate += String.format("%04d", (Integer) yearSpinner.getValue());
        } else {
            tmpDate += "????";
        }
        if((Integer) monthSpinner.getValue() > 0 && (Integer) monthSpinner.getValue() <= 12) {
            tmpDate += "." + String.format("%02d", (Integer) monthSpinner.getValue());
        } else {
            tmpDate += "." + "??";
        }
        if((Integer) daySpinner.getValue() > 0 && (Integer) daySpinner.getValue() <= 31) {
            tmpDate += "." + String.format("%02d", (Integer) daySpinner.getValue());
        } else {
            tmpDate += "." + "??";
        }
        pgnHeaders.put("Date", tmpDate);
        pgnHeaders.put("Event", event.getText());
        pgnHeaders.put("Result", getResult());

        return pgnHeaders;
    }

    private void installPlainNumberEditor(JSpinner spinner, String pattern) {

        JSpinner.NumberEditor editor =
                new JSpinner.NumberEditor(spinner, pattern);

        NumberFormat format = editor.getFormat();
        format.setGroupingUsed(false);

        spinner.setEditor(editor);
    }

    private void fixSpinnerWidth(JSpinner spinner, int width) {

        Dimension d = spinner.getPreferredSize();

        d = new Dimension(width, d.height);

        spinner.setPreferredSize(d);
        spinner.setMinimumSize(d);
        spinner.setMaximumSize(d);
    }

    private String getResult() {

        if (winWhite.isSelected()) return "1-0";
        if (winBlack.isSelected()) return "0-1";
        if (draw.isSelected()) return "1/2-1/2";
        return "*";
    }
}