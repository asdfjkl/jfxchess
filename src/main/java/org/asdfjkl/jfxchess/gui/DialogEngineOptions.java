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
import java.util.ArrayList;

import javax.swing.*;
import java.awt.*;

public class DialogEngineOptions extends JDialog {

    private final ArrayList<EngineOption> options;
    private final ArrayList<RowBinding> bindings = new ArrayList<>();
    private boolean confirmed = false;

    private static class RowBinding {
        EngineOption option;
        JComponent component;

        RowBinding(EngineOption option, JComponent component) {
            this.option = option;
            this.component = component;
        }
    }

    public DialogEngineOptions(Window parent, ArrayList<EngineOption> options) {
        super(parent, "Engine Options", ModalityType.APPLICATION_MODAL);
        this.options = new ArrayList<>();
        for(EngineOption option : options) {
            this.options.add(option.makeCopy());
        }

        setLayout(new BorderLayout());

        // Main panel with vertical layout
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        for (EngineOption opt : this.options) {
            // options "multipv" and "threads" as well as options
            // related to engine strength will be handled directly
            // by the GUI (via buttons and options)
            if(!opt.name.toLowerCase().contains("multipv")
                    && !opt.name.toLowerCase().contains("threads")
                    && !opt.name.toLowerCase().contains("uci_elo")
                    && !opt.name.toLowerCase().contains("uci_limitstrength")
                    && !opt.name.toLowerCase().contains("skill level"))
            {
                mainPanel.add(createOptionRow(opt));
            }
        }

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(scrollPane, BorderLayout.CENTER);

        // Bottom buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(e -> {
            applyChanges();
            confirmed = true;
            dispose();
        });

        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(420, 500));
        pack();
        setLocationRelativeTo(parent);
    }

    private JPanel createOptionRow(EngineOption opt) {

        JPanel row = new JPanel(new BorderLayout(10, 5));
        row.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JLabel label = new JLabel(opt.name);
        label.setPreferredSize(new Dimension(180, 25)); // consistent alignment
        row.add(label, BorderLayout.WEST);

        JComponent input;

        switch (opt.type) {

            case EngineOption.EN_OPT_TYPE_CHECK:
                JCheckBox checkBox = new JCheckBox();
                checkBox.setSelected(opt.checkStatusValue);
                input = checkBox;
                break;

            case EngineOption.EN_OPT_TYPE_SPIN:
                JSpinner spinner = new JSpinner(
                        new SpinnerNumberModel(
                                opt.spinValue,
                                opt.spinMin,
                                opt.spinMax,
                                1
                        )
                );
                input = spinner;
                break;

            case EngineOption.EN_OPT_TYPE_STRING:
                JTextField textField = new JTextField(opt.stringValue);
                input = textField;
                break;

            case EngineOption.EN_OPT_TYPE_COMBO:
                JComboBox<String> comboBox = new JComboBox<>(opt.comboValues.toArray(new String[0]));
                comboBox.setSelectedItem(opt.comboValue);
                input = comboBox;
                break;

            default:
                input = new JLabel("Unsupported");
        }

        row.add(input, BorderLayout.CENTER);

        // Store binding
        bindings.add(new RowBinding(opt, input));

        // Ensure rows stretch nicely
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        return row;
    }

    private void applyChanges() {
        for (RowBinding b : bindings) {
            EngineOption opt = b.option;
            JComponent input = b.component;

            switch (opt.type) {

                case EngineOption.EN_OPT_TYPE_CHECK:
                    opt.checkStatusValue = ((JCheckBox) input).isSelected();
                    break;

                case EngineOption.EN_OPT_TYPE_SPIN:
                    opt.spinValue = (Integer) ((JSpinner) input).getValue();
                    break;

                case EngineOption.EN_OPT_TYPE_STRING:
                    opt.stringValue = ((JTextField) input).getText();
                    break;

                case 2: // combo
                    opt.comboValue = (String) ((JComboBox<?>) input).getSelectedItem();
                    break;
            }
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public ArrayList<EngineOption> getOptions() {
        return options;
    }
}