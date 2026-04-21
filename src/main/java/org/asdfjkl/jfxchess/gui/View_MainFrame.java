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
import javax.swing.text.*;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.*;
import org.asdfjkl.jfxchess.lib.HtmlPrinter;

public class View_MainFrame extends JFrame
        implements PropertyChangeListener {

    private final Model_JFXChess model;
    private final Controller_UI controller_UI;
    private final Controller_Board controller_Board;
    private final Controller_Engine controller_Engine;
    private final Controller_Pgn controller_Pgn;

    public JSplitPane horizontalSplit;
    public JSplitPane verticalSplit;

    JLabel lblGameHeader;

    private JToggleButton btnEngineSwitch;
    private JButton btnThreads;

    View_Moves view_Moves;
    private JScrollPane scrollMoves;
    View_EngineOutput view_EngineOutput;
    View_Chessboard viewChessboard;

    HtmlPrinter htmlPrinter = new HtmlPrinter();
    String htmlString = "";

    private Object currentHighlight = null;

    KeyStroke pasteKey = KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK);
    KeyStroke copyKey = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK);
    KeyStroke flipKey = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK);
    KeyStroke setupPosKey = KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK);
    KeyStroke moveForwardKey = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0);
    KeyStroke moveBackKey = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0);
    KeyStroke seekFirstKey = KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0);
    KeyStroke seekEndKey = KeyStroke.getKeyStroke(KeyEvent.VK_END, 0);
    KeyStroke turnEngineOnKey = KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK);
    KeyStroke turnEngineOffKey = KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK);

    Map<KeyStroke, ActionListener> shortcuts = new HashMap<>();

    public View_MainFrame(Model_JFXChess model) {
        this.model = model;
        model.addListener(this);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                model.save();
            }
        });

        controller_UI = new Controller_UI(model);
        controller_Board = new Controller_Board(model);
        controller_Engine = new Controller_Engine(model);
        controller_Pgn = new Controller_Pgn(model);

        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(e -> {

                    if (!model.getShortcutsEnabled()) {
                        return false;
                    }
                    if (e.getID() != KeyEvent.KEY_PRESSED) {
                        return false;
                    }
                    KeyStroke ks = KeyStroke.getKeyStrokeForEvent(e);
                    ActionListener a = shortcuts.get(ks);
                    if (a != null) {
                        a.actionPerformed(new ActionEvent(
                                e.getSource(),
                                ActionEvent.ACTION_PERFORMED,
                                "shortcut"
                        ));
                        return true;
                    }
                    return false;
                });

        initUI();

    }

    private void initUI() {

        ArrayList<Image> icons = new ArrayList<>();

        icons.add(new ImageIcon(App.class.getResource("/icons/app_icon.png")).getImage());
        icons.add(new ImageIcon(App.class.getResource("/icons/app_icon@2x.png")).getImage());
        icons.add(new ImageIcon(App.class.getResource("/icons/app_icon@3x.png")).getImage());

        setTitle("JFXChess");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setIconImages(icons);

        // ===== Menu Bar =====
        setJMenuBar(createMenuBar());

        // ===== Tool Bar =====
        JToolBar toolBar = createToolBar();

        toolBar.putClientProperty("JToolBar.isRollover", true);

        // ===== Main Content =====
        JComponent mainContent = createMainContent();

        // ===== Key Shortcuts
        assignKeyShortcuts();

        // ===== Top Container (Toolbar + Content) =====
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(toolBar, BorderLayout.NORTH);
        topPanel.add(mainContent, BorderLayout.CENTER);

        setContentPane(topPanel);

    }

    // ----------------------------------------------------
    // Menu Bar
    // ----------------------------------------------------

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu gameMenu = new JMenu("Game");
        JMenuItem jmiNewGame = new JMenuItem("New Game");
        jmiNewGame.addActionListener(controller_Engine.startNewGame());
        gameMenu.add(jmiNewGame);

        JMenuItem jmiOpenFile = new JMenuItem("Open File");
        jmiOpenFile.addActionListener(controller_Pgn.openFile());
        gameMenu.add(jmiOpenFile);
        JMenuItem jmiSaveGame = new JMenuItem("Save Game");
        gameMenu.add(jmiSaveGame);
        jmiSaveGame.addActionListener(controller_Pgn.saveGame());
        gameMenu.addSeparator();
        JMenuItem jmiPrintGame = new JMenuItem("Print Game");
        gameMenu.add(jmiPrintGame);
        jmiPrintGame.addActionListener(controller_UI.printGame());
        JMenuItem jmiPrintPosition =  new JMenuItem("Print Position");
        gameMenu.add(jmiPrintPosition);
        jmiPrintPosition.addActionListener(controller_UI.printFen());
        gameMenu.addSeparator();
        JMenuItem jmiQuit = new JMenuItem("Quit");
        jmiQuit.addActionListener(e -> { dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)); });
        gameMenu.add(jmiQuit);

        JMenu editMenu = new JMenu("Edit");
        JMenuItem jmiCopyGame = new JMenuItem("Copy Game");
        jmiCopyGame.addActionListener(controller_UI.copyPgnToClipboard());
        jmiCopyGame.setAccelerator(copyKey);
        editMenu.add(jmiCopyGame);
        JMenuItem jmiCopyFEN = new JMenuItem("Copy Position (FEN)");
        jmiCopyFEN.addActionListener(controller_UI.copyFenToClipboard());
        editMenu.add(jmiCopyFEN);

        JMenuItem jmiCopyImage = new JMenuItem("Copy Position (Image)");
        jmiCopyImage.addActionListener(controller_UI.copyBitmapToClipboard());
        editMenu.add(jmiCopyImage);

        JMenuItem jmiPaste =  new JMenuItem("Paste Game/Position");
        jmiPaste.addActionListener(controller_UI.pasteFenOrGame());
        jmiPaste.setAccelerator(pasteKey);
        editMenu.add(jmiPaste);
        editMenu.addSeparator();

        JMenuItem jmiEditGameData = new JMenuItem("Edit Game Data");
        jmiEditGameData.addActionListener(controller_UI.editGameData());
        editMenu.add(jmiEditGameData);

        JMenuItem jmiSetupPosition = new JMenuItem("Setup Position");
        editMenu.add(jmiSetupPosition);
        jmiSetupPosition.addActionListener(controller_UI.setupNewPosition());
        jmiSetupPosition.setAccelerator(setupPosKey);
        editMenu.addSeparator();
        JMenuItem jmiFlipBoard = new JMenuItem("Flip Board");
        editMenu.add(jmiFlipBoard);
        jmiFlipBoard.addActionListener(controller_UI.flipBoard());
        jmiFlipBoard.setAccelerator(flipKey);

        JMenu modeMenu = new JMenu("Engine");

        JMenuItem jmiStartEngine = new JMenuItem("Start Engine");
        jmiStartEngine.addActionListener(controller_Engine.startAnalysisMode());
        jmiStartEngine.setAccelerator(turnEngineOnKey);
        modeMenu.add(jmiStartEngine);
        JMenuItem jmiStopEngine = new JMenuItem("Stop Engine");
        jmiStopEngine.addActionListener(controller_Engine.startEnterMovesMode());
        jmiStopEngine.setAccelerator(turnEngineOffKey);
        modeMenu.add(jmiStopEngine);
        JMenuItem jmiFullGameAnalysis = new JMenuItem("Full Game Analysis");
        jmiFullGameAnalysis.addActionListener(controller_Engine.startGameAnalysisMode());
        modeMenu.add(jmiFullGameAnalysis);
        JMenuItem jmiPlayoutPosition = new JMenuItem("Playout Position");
        jmiPlayoutPosition.addActionListener(controller_Engine.startPlayoutPositionMode());
        modeMenu.add(jmiPlayoutPosition);

        modeMenu.addSeparator();

        JMenuItem jmiEngines = new JMenuItem("Engine Settings");
        jmiEngines.addActionListener(controller_Engine.editEngines());
        modeMenu.add(jmiEngines);
        JMenuItem jmiSelectBook = new JMenuItem("Select Book");
        modeMenu.add(jmiSelectBook);
        jmiSelectBook.addActionListener(controller_UI.selectBookFile());

        JMenu viewMenu = new JMenu("View");
        JMenu themeSubMenu = new JMenu("Theme");

        JRadioButtonMenuItem jmiToFlatlafLight = new JRadioButtonMenuItem("FlatLaf Light");
        jmiToFlatlafLight.addActionListener(controller_UI.switchLaf(Model_JFXChess.THEME_FLATLAF_LIGHT));
        themeSubMenu.add(jmiToFlatlafLight);
        jmiToFlatlafLight.setSelected(model.getLookAndFeel().equals(Model_JFXChess.THEME_FLATLAF_LIGHT));

        JRadioButtonMenuItem jmiToFlatlafDark = new JRadioButtonMenuItem("FlatLaf Dark");
        jmiToFlatlafDark.addActionListener(controller_UI.switchLaf(Model_JFXChess.THEME_FLATLAF_DARK));
        themeSubMenu.add(jmiToFlatlafDark);
        jmiToFlatlafDark.setSelected(model.getLookAndFeel().equals(Model_JFXChess.THEME_FLATLAF_DARK));

        JRadioButtonMenuItem jmiToFlatlafIntellij = new JRadioButtonMenuItem("FlatLaf IJ");
        jmiToFlatlafIntellij.addActionListener(controller_UI.switchLaf(Model_JFXChess.THEME_FLATLAF_INTELLIJ));
        themeSubMenu.add(jmiToFlatlafIntellij);
        jmiToFlatlafIntellij.setSelected(model.getLookAndFeel().equals(Model_JFXChess.THEME_FLATLAF_INTELLIJ));

        JRadioButtonMenuItem jmiToFlatlafDarcula = new JRadioButtonMenuItem("FlatLaf Darcula");
        jmiToFlatlafDarcula.addActionListener(controller_UI.switchLaf(Model_JFXChess.THEME_FLATLAF_DARCULA));
        themeSubMenu.add(jmiToFlatlafDarcula);
        jmiToFlatlafDarcula.setSelected(model.getLookAndFeel().equals(Model_JFXChess.THEME_FLATLAF_DARCULA));

        JRadioButtonMenuItem jmiToFlatlaMacLight = new JRadioButtonMenuItem("FlatLaf Fruit Light");
        jmiToFlatlaMacLight.addActionListener(controller_UI.switchLaf(Model_JFXChess.THEME_FLATLAF_FRUIT_LIGHT));
        themeSubMenu.add(jmiToFlatlaMacLight);
        jmiToFlatlaMacLight.setSelected(model.getLookAndFeel().equals(Model_JFXChess.THEME_FLATLAF_FRUIT_LIGHT));

        JRadioButtonMenuItem jmiToFlatlaMacDark = new JRadioButtonMenuItem("FlatLaf Fruit Dark");
        jmiToFlatlaMacDark.addActionListener(controller_UI.switchLaf(Model_JFXChess.THEME_FLATLAF_FRUIT_DARK));
        themeSubMenu.add(jmiToFlatlaMacDark);
        jmiToFlatlaMacDark.setSelected(model.getLookAndFeel().equals(Model_JFXChess.THEME_FLATLAF_FRUIT_DARK));

        JRadioButtonMenuItem jmiToMetal = new JRadioButtonMenuItem("Swing Metal");
        jmiToMetal.addActionListener(controller_UI.switchLaf(Model_JFXChess.THEME_METAL));
        themeSubMenu.add(jmiToMetal);
        jmiToMetal.setSelected(model.getLookAndFeel().equals(Model_JFXChess.THEME_METAL));

        JRadioButtonMenuItem jmiToNimbus = new JRadioButtonMenuItem("Swing Nimbus");
        jmiToNimbus.addActionListener(controller_UI.switchLaf(Model_JFXChess.THEME_NIMBUS));
        themeSubMenu.add(jmiToNimbus);
        jmiToNimbus.setSelected(model.getLookAndFeel().equals(Model_JFXChess.THEME_NIMBUS));

        JRadioButtonMenuItem jmiToSysDefault = new JRadioButtonMenuItem("System Default");
        jmiToSysDefault.addActionListener(controller_UI.switchLaf(Model_JFXChess.THEME_SYSTEM));
        themeSubMenu.add(jmiToSysDefault);
        jmiToSysDefault.setSelected(model.getLookAndFeel().equals(Model_JFXChess.THEME_SYSTEM));

        ButtonGroup grpUiTheme = new ButtonGroup();
        grpUiTheme.add(jmiToFlatlafLight);
        grpUiTheme.add(jmiToFlatlafDark);
        grpUiTheme.add(jmiToFlatlafIntellij);
        grpUiTheme.add(jmiToFlatlafDarcula);
        grpUiTheme.add(jmiToFlatlaMacLight);
        grpUiTheme.add(jmiToFlatlaMacDark);
        grpUiTheme.add(jmiToMetal);
        grpUiTheme.add(jmiToNimbus);
        grpUiTheme.add(jmiToSysDefault);

        viewMenu.add(themeSubMenu);

        JMenu boardColorMenu = new JMenu("Board Color");
        JRadioButtonMenuItem jmiBoardColorBlue = new JRadioButtonMenuItem("Blue");
        jmiBoardColorBlue.addActionListener(controller_UI.switchBoardColor(BoardStyle.STYLE_BLUE));
        boardColorMenu.add(jmiBoardColorBlue);
        jmiBoardColorBlue.setSelected(model.getBoardStyle().getColorStyle() == BoardStyle.STYLE_BLUE);

        JRadioButtonMenuItem jmiBoardColorGreen = new JRadioButtonMenuItem("Green");
        jmiBoardColorGreen.addActionListener(controller_UI.switchBoardColor(BoardStyle.STYLE_GREEN));
        boardColorMenu.add(jmiBoardColorGreen);
        jmiBoardColorGreen.setSelected(model.getBoardStyle().getColorStyle() == BoardStyle.STYLE_GREEN);

        JRadioButtonMenuItem jmiBoardColorBrown = new JRadioButtonMenuItem("Brown");
        jmiBoardColorBrown.addActionListener(controller_UI.switchBoardColor(BoardStyle.STYLE_BROWN));
        boardColorMenu.add(jmiBoardColorBrown);
        jmiBoardColorBrown.setSelected(model.getBoardStyle().getColorStyle() == BoardStyle.STYLE_BROWN);

        viewMenu.add(boardColorMenu);

        ButtonGroup grpUiBoardColor = new ButtonGroup();
        grpUiBoardColor.add(jmiBoardColorBlue);
        grpUiBoardColor.add(jmiBoardColorGreen);
        grpUiBoardColor.add(jmiBoardColorBrown);

        JMenu pieceStyleMenu = new JMenu("Piece Style");
        JRadioButtonMenuItem jmiPieceStyleMerida = new JRadioButtonMenuItem("Merida");
        jmiPieceStyleMerida.addActionListener(controller_UI.switchPieceStyle(BoardStyle.PIECE_STYLE_MERIDA));
        pieceStyleMenu.add(jmiPieceStyleMerida);
        jmiPieceStyleMerida.setSelected(model.getBoardStyle().getPieceStyle() == BoardStyle.PIECE_STYLE_MERIDA);

        JRadioButtonMenuItem jmiPieceStyleOld = new JRadioButtonMenuItem("Old");
        jmiPieceStyleOld.addActionListener(controller_UI.switchPieceStyle(BoardStyle.PIECE_STYLE_OLD));
        pieceStyleMenu.add(jmiPieceStyleOld);
        jmiPieceStyleOld.setSelected(model.getBoardStyle().getPieceStyle() == BoardStyle.PIECE_STYLE_OLD);

        JRadioButtonMenuItem jmiPieceStyleUSCF = new JRadioButtonMenuItem("USCF");
        jmiPieceStyleUSCF.addActionListener(controller_UI.switchPieceStyle(BoardStyle.PIECE_STYLE_USCF));
        pieceStyleMenu.add(jmiPieceStyleUSCF);
        jmiPieceStyleUSCF.setSelected(model.getBoardStyle().getPieceStyle() == BoardStyle.PIECE_STYLE_USCF);

        viewMenu.add(pieceStyleMenu);

        ButtonGroup grpUiPieceStyle = new ButtonGroup();
        grpUiPieceStyle.add(jmiPieceStyleMerida);
        grpUiPieceStyle.add(jmiPieceStyleOld);
        grpUiPieceStyle.add(jmiPieceStyleUSCF);

        JMenuItem jmiResetLayout = new JMenuItem("Reset Window Layout");
        jmiResetLayout.addActionListener(e -> {
            setSize(1000, 700);
            setLocationRelativeTo(null);
            revalidate();

            SwingUtilities.invokeLater(() -> {
                verticalSplit.setResizeWeight(0.8);
                verticalSplit.setDividerLocation(450);

                horizontalSplit.setResizeWeight(0.7);
                horizontalSplit.setDividerLocation(600);

                viewChessboard.revalidate();
                viewChessboard.repaint();
            });
        });
        viewMenu.add(jmiResetLayout);

        JMenu databaseMenu = new JMenu("Database");
        JMenuItem jmiDatabase = new JMenuItem("Browse Database");
        jmiDatabase.addActionListener(controller_Pgn.showDatabase());
        databaseMenu.add(jmiDatabase);
        JMenuItem jmiNextGameinDatabase = new JMenuItem("Next Game");
        databaseMenu.add(jmiNextGameinDatabase);
        jmiNextGameinDatabase.addActionListener(controller_Pgn.goToNextGameInDatabase());
        JMenuItem jmiPreviousGameinDatabase = new JMenuItem("Previous Game");
        databaseMenu.add(jmiPreviousGameinDatabase);
        jmiPreviousGameinDatabase.addActionListener(controller_Pgn.goToPrevGameInDatabase());

        JMenu helpMenu = new JMenu("Help");
        JMenuItem jmiAbout = new JMenuItem("About");
        jmiAbout.addActionListener(controller_UI.showAbout());
        helpMenu.add(jmiAbout);
        JMenuItem jmiUrl = new JMenuItem("JFXChess Homepage");
        jmiUrl.addActionListener(controller_UI.goToHomepage());
        helpMenu.add(jmiUrl);

        menuBar.add(gameMenu);
        menuBar.add(editMenu);
        menuBar.add(modeMenu);
        menuBar.add(viewMenu);
        menuBar.add(databaseMenu);
        menuBar.add(helpMenu);

        return menuBar;
    }


    // ----------------------------------------------------
    // Tool Bar
    // ----------------------------------------------------

    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton btnTbNew = createToolButton("New Game", "open_in_new.svg");
        toolBar.add(btnTbNew);
        btnTbNew.addActionListener(controller_Engine.startNewGame());
        JButton btnTbOpen = createToolButton("Open File", "open_folder.svg");
        toolBar.add(btnTbOpen);
        btnTbOpen.addActionListener(controller_Pgn.openFile());
        JButton btnTbSave = createToolButton("Save Game", "file_save.svg");
        btnTbSave.addActionListener(controller_Pgn.saveGame());
        toolBar.add(btnTbSave);

        toolBar.addSeparator();

        JButton btnTbPrint = createToolButton("Print Game", "print.svg");
        toolBar.add(btnTbPrint);
        btnTbPrint.addActionListener(controller_UI.printGame());
        JButton btnTbFlip = createToolButton("Flip Board", "flip3.svg");
        toolBar.add(btnTbFlip);
        btnTbFlip.addActionListener(controller_UI.flipBoard());

        toolBar.addSeparator();

        JButton btnTbCopyGame = createToolButton("Copy Game", "copy1.svg");
        toolBar.add(btnTbCopyGame);
        btnTbCopyGame.addActionListener(controller_UI.copyPgnToClipboard());
        JButton btnTbCopyPosition = createToolButton("Copy Position (FEN)", "copy2.svg");
        toolBar.add(btnTbCopyPosition);
        btnTbCopyPosition.addActionListener(controller_UI.copyFenToClipboard());
        JButton btnTbPaste = createToolButton("Paste Game/Position", "paste.svg");
        toolBar.add(btnTbPaste);
        btnTbPaste.addActionListener(controller_UI.pasteFenOrGame());
        JButton btnTbSetupPosition = createToolButton("Setup Position", "setup_new_position.svg");
        toolBar.add(btnTbSetupPosition);
        btnTbSetupPosition.addActionListener(controller_UI.setupNewPosition());

        toolBar.addSeparator();

        JButton btnTbFullAnalysis = createToolButton("Full Game Analysis", "game_analysis.svg");
        toolBar.add(btnTbFullAnalysis);
        btnTbFullAnalysis.addActionListener(controller_Engine.startGameAnalysisMode());

        toolBar.addSeparator();

        JButton btnTbBrowseDatabase = createToolButton("Browse Database", "database.svg");
        toolBar.add(btnTbBrowseDatabase);
        btnTbBrowseDatabase.addActionListener(controller_Pgn.showDatabase());
        JButton btnTbDatabasePrevGame = createToolButton("Previous Game", "arrow_left_alt.svg");
        toolBar.add(btnTbDatabasePrevGame);
        btnTbDatabasePrevGame.addActionListener(controller_Pgn.goToPrevGameInDatabase());
        JButton btnTbDatabaseNextGame = createToolButton("Next Game", "arrow_right_alt.svg");
        toolBar.add(btnTbDatabaseNextGame);
        btnTbDatabaseNextGame.addActionListener(controller_Pgn.goToNextGameInDatabase());

        toolBar.addSeparator();

        JButton btnTbAbout = createToolButton("About", "about.svg");
        btnTbAbout.addActionListener(controller_UI.showAbout());
        toolBar.add(btnTbAbout);

        return toolBar;
    }

    private JButton createToolButton(String toolTipText, String fnIcon) {

        JButton btn = new JButton();
        String resIcn = "icons/" + fnIcon;
        btn.setIcon(new FlatSVGIcon(resIcn));
        btn.setToolTipText(toolTipText);
        btn.putClientProperty("JButton.buttonType", "toolBarButton");
        btn.setFocusable(false);

        return btn;
    }

    // ----------------------------------------------------
    // Main Content Area (Split Panes)
    // ----------------------------------------------------

    private JComponent createMainContent() {

        // ===== Left: Chessboard Placeholder =====
        viewChessboard = new View_Chessboard(model, controller_UI, controller_Board);

        // ===== Right: Game Header Pane/Button + Text Pane + Nav Buttons =====

        // ===== Multiline Label =====
        lblGameHeader = new JLabel(
                "<html><div style='text-align:center;'>N., N. - N., N.<br>" +
                        "Somewhere, 01.01.1900</div></html>"
        );

        lblGameHeader.setHorizontalAlignment(SwingConstants.CENTER);
        lblGameHeader.setVerticalAlignment(SwingConstants.CENTER);


        // ===== Button next to it =====
        JButton btnGameHeader = new JButton();
        btnGameHeader.putClientProperty("JButton.buttonType", "toolBarButton");
        btnGameHeader.setIcon(new FlatSVGIcon("icons/edit_game_header_18px.svg"));
        btnGameHeader.setToolTipText("Edit Game  Data");
        btnGameHeader.setFocusable(false);
        btnGameHeader.addActionListener(controller_UI.editGameData());

        // ===== Header panel (Label + Button) =====
        JPanel headerPanel = new JPanel(new BorderLayout(8, 0));

        headerPanel.add(lblGameHeader, BorderLayout.CENTER);
        headerPanel.add(btnGameHeader, BorderLayout.EAST);

        headerPanel.setBorder(
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        );

        // ===== TextPane for the navPanel
        view_Moves = new View_Moves(model, controller_UI, controller_Board);
        scrollMoves = new JScrollPane(view_Moves);

        // ===== View for opening book
        View_Book view_Book = new View_Book(model, controller_Board);
        JScrollPane scrollBook = new JScrollPane(view_Book);
        model.addListener(view_Book);

        // evaluation barchart
        View_Eval view_Eval = new View_Eval(model,6.0f);
        model.addListener(view_Eval);
        // temp: remove later
        //for (int i = 0; i < 30; i++) {
        //    view_Eval.setEvalAt(i, (float)(Math.sin(i * 0.2) * 3));
        //}

        // Navigation buttons panel
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JButton btnToStart = new JButton();
        btnToStart.putClientProperty("JButton.buttonType", "toolBarButton");
        btnToStart.setIcon(new FlatSVGIcon("icons/fast_rewind.svg"));
        btnToStart.setToolTipText("Seek To Beginning");
        btnToStart.setFocusable(false);
        btnToStart.addActionListener(controller_Board.seekToBeginning());

        JButton btnPrev = new JButton();
        btnPrev.putClientProperty("JButton.buttonType", "toolBarButton");
        btnPrev.setIcon(new FlatSVGIcon("icons/arrow_back.svg"));
        btnPrev.setToolTipText("Move Back");
        btnPrev.setFocusable(false);
        btnPrev.addActionListener(controller_Board.moveBack());

        JButton btnNext = new JButton();
        btnNext.putClientProperty("JButton.buttonType", "toolBarButton");
        btnNext.setIcon(new FlatSVGIcon("icons/play_arrow.svg"));
        btnNext.setToolTipText("Move Forward");
        btnNext.setFocusable(false);
        btnNext.addActionListener(controller_Board.moveForward());

        JButton btnToEnd = new JButton();
        btnToEnd.putClientProperty("JButton.buttonType", "toolBarButton");
        btnToEnd.setIcon(new FlatSVGIcon("icons/fast_forward.svg"));
        btnToEnd.setToolTipText("Seek to End");
        btnToEnd.setFocusable(false);
        btnToEnd.addActionListener(controller_Board.seekToEnd());

        navPanel.add(btnToStart);
        navPanel.add(btnPrev);
        navPanel.add(btnNext);
        navPanel.add(btnToEnd);

        // put view_moves and view_book inside a tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Moves", scrollMoves);
        tabbedPane.addTab("Book", scrollBook);

        // container for move-view/book-view and eval barchart
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(tabbedPane, BorderLayout.CENTER);

        view_Eval.setPreferredSize(new Dimension(0, (int) (navPanel.getPreferredSize().height * 1.5)));
        centerPanel.add(view_Eval, BorderLayout.SOUTH);

        // Container for right side
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(headerPanel, BorderLayout.NORTH);
        //rightPanel.add(tabbedPane, BorderLayout.CENTER);
        //rightPanel.add(view_eval, BorderLayout.SOUTH);
        rightPanel.add(centerPanel, BorderLayout.CENTER);
        rightPanel.add(navPanel, BorderLayout.SOUTH);

        // ===== Horizontal Split (Board | Right Pane) =====
        horizontalSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                viewChessboard,
                rightPanel
        );

        horizontalSplit.setResizeWeight(0.7);
        horizontalSplit.setDividerLocation(600);
        horizontalSplit.setContinuousLayout(true);


        // ===== Engine On/Off, Thread Buttons etc. above Engine Info =====
        JPanel bottomControlBar = new JPanel(new BorderLayout());
        // --- Left side group ---
        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));

        btnEngineSwitch = new JToggleButton("Start Engine");
        btnEngineSwitch.addActionListener(e -> {
            if(!btnEngineSwitch.isSelected()) {
                controller_Engine.activateEnterMovesMode();
            } else {
                controller_Engine.activateAnalysisMode();
            }
        });
        JButton btnAddLine = new JButton("+");
        btnAddLine.addActionListener(controller_Engine.incMultiPV());
        JButton btnRemoveLine = new JButton("-");
        btnRemoveLine.addActionListener(controller_Engine.decMultiPV());
        btnThreads  = new JButton("Set # Threads");
        btnThreads.addActionListener(controller_Engine.changeNrThreads());

        btnEngineSwitch.setFocusable(false);
        btnAddLine.setFocusable(false);
        btnRemoveLine.setFocusable(false);
        btnThreads.setFocusable(false);

        leftGroup.add(btnEngineSwitch);
        leftGroup.add(btnAddLine);
        leftGroup.add(btnRemoveLine);
        leftGroup.add(btnThreads);

        // --- Right side group ---
        JPanel rightGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 2));

        JButton btnEngines = new JButton();
        //int h = btnEngineSwitch.getHeight();
        btnEngines.setIcon(new FlatSVGIcon("icons/engine_18px.svg"));
        btnEngines.setToolTipText("Select Engine");
        btnEngines.setFocusable(false);
        btnEngines.addActionListener(controller_Engine.editEngines());

        rightGroup.add(btnEngines);

        // --- Assemble ---
        bottomControlBar.add(leftGroup, BorderLayout.WEST);
        bottomControlBar.add(rightGroup, BorderLayout.EAST);

        // ===== Bottom Text Pane =====
        view_EngineOutput = new View_EngineOutput(model);
        model.addListener(view_EngineOutput);
        JScrollPane bottomScroll = new JScrollPane(view_EngineOutput);

        // Container for bottom area
        JPanel bottomPanel = new JPanel(new BorderLayout());

        bottomPanel.add(bottomControlBar, BorderLayout.NORTH);
        bottomPanel.add(bottomScroll, BorderLayout.CENTER);

        // ===== Vertical Split (Top | Bottom) =====
        verticalSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                horizontalSplit,
                bottomPanel
        );

        verticalSplit.setResizeWeight(0.8);
        verticalSplit.setDividerLocation(450);
        verticalSplit.setContinuousLayout(true);

        return verticalSplit;
    }


    private void setLookAndFeel(String lafClass) {

        if(lafClass.equals("system.default")) {
            lafClass = UIManager.getSystemLookAndFeelClassName();
        }

        // clear custom default font when switching to non-FlatLaf LaF
        if( !(UIManager.getLookAndFeel() instanceof FlatLaf) )
            UIManager.put( "defaultFont", null );

        try {
            UIManager.setLookAndFeel(lafClass);

            SwingUtilities.updateComponentTreeUI(this);

            invalidate();
            validate();
            repaint();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setGeometry(ScreenGeometry g) {
        // restore screen geometry on main frame
        setSize(g.width, g.height);
        if(g.posX > 0 && g.posY > 0) {
            setLocation(g.posX, g.posY);
        }

        if (g.isMaximized) {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }

        // Restore divider (after layout is ready)
        int dividerHorizontal = g.dividerHorizontal;
        int dividerVertical = g.dividerVertical;

        horizontalSplit.setDividerLocation(dividerHorizontal);
        verticalSplit.setDividerLocation(dividerVertical);
    }

    private void updatePgnHeaders() {
        // update label
        HashMap<String, String> pgnHeaders = model.getGame().getPgnHeaders();
        String newGameInfo = "<html><div style='text-align:center;'>" +
                pgnHeaders.get("White") + " - " +
                pgnHeaders.get("Black") + "<br>" +
                pgnHeaders.get("Site");
        if(!(pgnHeaders.get("Date").isEmpty())) {
            newGameInfo = newGameInfo + ", " + pgnHeaders.get("Date");
        }
        newGameInfo += "</div></html>";
        lblGameHeader.setText(newGameInfo);
    }

    private void updateHighlightedMove() {
        try {
            int id = model.getGame().getCurrentNode().getId();
            HTMLDocument doc = (HTMLDocument) view_Moves.getDocument();
            Element element = doc.getElement("n" + id);

            Highlighter highlighter = view_Moves.getHighlighter();

            if (element == null) {
                // if root node, remove annotation before returning
                if(model.getGame().getCurrentNode() == model.game.getRootNode()
                        && currentHighlight != null) {
                    highlighter.removeHighlight(currentHighlight);
                }
                return;
            }

            int start = element.getStartOffset();
            int end = element.getEndOffset();

            if (currentHighlight != null) {
                highlighter.removeHighlight(currentHighlight);
            }

            currentHighlight = highlighter.addHighlight(
                    start,
                    end,
                    new DefaultHighlighter.DefaultHighlightPainter(Color.LIGHT_GRAY)
            );

            /*
            Rectangle r = view_Moves.modelToView(start);

            if (r != null) {
                view_Moves.scrollRectToVisible(r);
            }
             */

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void assignKeyShortcuts() {
        // Keyboard Shortcuts
        shortcuts.put(
                moveForwardKey,
                controller_Board.moveForward()
        );
        shortcuts.put(
                moveBackKey,
                controller_Board.moveBack()
        );
        shortcuts.put(
                seekFirstKey,
                controller_Board.seekToBeginning()
        );
        shortcuts.put(
                seekEndKey,
                controller_Board.seekToEnd()
        );
        shortcuts.put(
                copyKey,
                controller_UI.copyPgnToClipboard()
        );
        shortcuts.put(
                pasteKey,
                controller_UI.pasteFenOrGame()
        );
        shortcuts.put(
                flipKey,
                controller_UI.flipBoard()
        );
        shortcuts.put(
                turnEngineOnKey,
                controller_Engine.startAnalysisMode()
        );
        shortcuts.put(
                turnEngineOffKey,
                controller_Engine.startEnterMovesMode()
        );
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("switchLaf".equals(evt.getPropertyName())) {
            setLookAndFeel(model.getLookAndFeel());
        }
        if ("pgnHeadersChanged".equals(evt.getPropertyName())) {
            updatePgnHeaders();
        }
        if("currentGameNodeChanged".equals(evt.getPropertyName())) {
            updateHighlightedMove();
        }
        if("gameChanged".equals(evt.getPropertyName()) || "treeChanged".equals(evt.getPropertyName())) {
            htmlString =  htmlPrinter.printGame(model.getGame());
            view_Moves.setText(htmlString);
            view_Moves.setCaretPosition(0);
            updatePgnHeaders();
        }

        if("modeChanged".equals(evt.getPropertyName())) {
            int mode = model.getMode();
            switch(mode) {
                case Model_JFXChess.MODE_ANALYSIS:
                case Model_JFXChess.MODE_PLAY_WHITE:
                case Model_JFXChess.MODE_PLAY_BLACK:
                case Model_JFXChess.MODE_PLAYOUT_POSITION:
                case Model_JFXChess.MODE_GAME_ANALYSIS:
                    btnEngineSwitch.setText("Stop Engine");
                    btnEngineSwitch.setSelected(true);
                    break;
                case Model_JFXChess.MODE_ENTER_MOVES:
                    btnEngineSwitch.setText("Start Engine");
                    btnEngineSwitch.setSelected(false);
                    break;
            }
        }
    }

}

