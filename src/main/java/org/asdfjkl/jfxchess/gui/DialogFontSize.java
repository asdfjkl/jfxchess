package org.asdfjkl.jfxchess.gui;

import java.awt.*;
import javax.swing.*;

public class DialogFontSize extends JDialog {

    private static final Integer[] FONT_SIZES = {
            10, 11, 12, 13, 14, 16, 18, 20, 22, 24, 28, 36, 48
    };

    private boolean confirmed = false;

    private final JRadioButton rbMoveDefault =
            new JRadioButton("Default System Font Size");
    private final JRadioButton rbMoveCustom =
            new JRadioButton("Custom Font Size");

    private final JRadioButton rbEngineDefault =
            new JRadioButton("Default System Font Size");
    private final JRadioButton rbEngineCustom =
            new JRadioButton("Custom Font Size");

    private final JComboBox<Integer> cbMove =
            new JComboBox<>(FONT_SIZES);

    private final JComboBox<Integer> cbEngine =
            new JComboBox<>(FONT_SIZES);

    public DialogFontSize(
            Window owner,
            int currentMoveFont,
            boolean useCustomMoveFont,
            int currentEngineFont,
            boolean useCustomEngineFont)      {

        super(owner, "Font Size", ModalityType.APPLICATION_MODAL);

        ButtonGroup moveGroup = new ButtonGroup();
        moveGroup.add(rbMoveDefault);
        moveGroup.add(rbMoveCustom);

        ButtonGroup engineGroup = new ButtonGroup();
        engineGroup.add(rbEngineDefault);
        engineGroup.add(rbEngineCustom);

        if (useCustomMoveFont) {
            rbMoveCustom.setSelected(true);
        } else {
            rbMoveDefault.setSelected(true);
        }

        if (useCustomEngineFont) {
            rbEngineCustom.setSelected(true);
        } else {
            rbEngineDefault.setSelected(true);
        }

        cbMove.setSelectedItem(currentMoveFont);
        cbEngine.setSelectedItem(currentEngineFont);

        rbMoveDefault.addActionListener(e -> updateEnabledState());
        rbMoveCustom.addActionListener(e -> updateEnabledState());

        rbEngineDefault.addActionListener(e -> updateEnabledState());
        rbEngineCustom.addActionListener(e -> updateEnabledState());

        buildGui();

        updateEnabledState();

        pack();
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void buildGui() {


        JPanel movePanel = new JPanel(new GridBagLayout());
        movePanel.setBorder(
                BorderFactory.createTitledBorder("Move Editor"));

        JPanel enginePanel = new JPanel(new GridBagLayout());
        enginePanel.setBorder(
                BorderFactory.createTitledBorder("Engine Output"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // -------------------------------------------------
        // Move Editor panel
        // -------------------------------------------------

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        movePanel.add(rbMoveDefault, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        movePanel.add(rbMoveCustom, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        movePanel.add(cbMove, gbc);


        // -------------------------------------------------
        // Engine Output panel
        // -------------------------------------------------

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        enginePanel.add(rbEngineDefault, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        enginePanel.add(rbEngineCustom, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        enginePanel.add(cbEngine, gbc);


        // -------------------------------------------------
        // Center area containing both panels
        // -------------------------------------------------

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        centerPanel.add(movePanel);
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(enginePanel);


        // -------------------------------------------------
        // Buttons
        // -------------------------------------------------

        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        cancelButton.addActionListener(e -> {
            dispose();
        });

        JPanel buttonPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT));

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        getRootPane().setDefaultButton(okButton);


        // -------------------------------------------------
        // Dialog layout
        // -------------------------------------------------

        getContentPane().setLayout(
                new BorderLayout(8, 8));

        getContentPane().add(
                centerPanel,
                BorderLayout.CENTER);

        getContentPane().add(
                buttonPanel,
                BorderLayout.SOUTH);
    }

    private void updateEnabledState() {

        cbMove.setEnabled(rbMoveCustom.isSelected());
        cbEngine.setEnabled(rbEngineCustom.isSelected());
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public boolean useCustomMoveFont() {
        return rbMoveCustom.isSelected();
    }

    public boolean useCustomEngineFont() {
        return rbEngineCustom.isSelected();
    }

    public int getFontSizeMoveView() {
        return (Integer) cbMove.getSelectedItem();
    }

    public int getFontSizeEngineOutput() {
        return (Integer) cbEngine.getSelectedItem();
    }

}