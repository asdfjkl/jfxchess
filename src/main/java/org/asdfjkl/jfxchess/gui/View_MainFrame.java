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
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.*;

public class View_MainFrame extends JFrame
        implements PropertyChangeListener {

    private final Model_JFXChess model;
    private final Controller_UI controller_UI;
    private final Controller_Engine controller_Engine;
    private final Controller_Pgn controller_Pgn;
    private final Workspace workspace;

    private JTabbedPane gameTabs;
    private final Map<GameSession, GameTabView> gameTabViews =
            new IdentityHashMap<>();
    private WindowManager windowManager;
    private CommandContext commandContext;

    KeyStroke pasteKey = KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK);
    KeyStroke copyKey = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK);
    KeyStroke openKey = KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK);
    KeyStroke saveKey = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK);
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
        workspace = model.getWorkspace();
        if (workspace == null) {
            throw new IllegalStateException("View_MainFrame requires a workspace");
        }
        model.addListener(this);
        workspace.addPropertyChangeListener(this::workspacePropertyChange);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                model.save();
            }
        });

        controller_UI = new Controller_UI(model);
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

        // ===== Main Content =====
        JComponent mainContent = createMainContent();

        // ===== Menu Bar =====
        setJMenuBar(createMenuBar());

        // ===== Tool Bar =====
        JToolBar toolBar = createToolBar();
        toolBar.putClientProperty("JToolBar.isRollover", true);

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
        jmiNewGame.addActionListener(command(controller_Engine.startNewGame()));
        gameMenu.add(jmiNewGame);

        JMenuItem jmiOpenFile = new JMenuItem("Open File");
        jmiOpenFile.addActionListener(command(controller_Pgn.openFile()));
        jmiOpenFile.setAccelerator(openKey);
        gameMenu.add(jmiOpenFile);
        JMenuItem jmiSaveGame = new JMenuItem("Save Game");
        gameMenu.add(jmiSaveGame);
        jmiSaveGame.addActionListener(command(controller_Pgn.saveGame()));
        jmiSaveGame.setAccelerator(saveKey);
        JMenuItem jmiDetachGame = new JMenuItem("Detach Game Window");
        jmiDetachGame.addActionListener(command(e -> detachActiveGame()));
        gameMenu.add(jmiDetachGame);
        gameMenu.addSeparator();
        JMenuItem jmiPrintGame = new JMenuItem("Print Game");
        gameMenu.add(jmiPrintGame);
        jmiPrintGame.addActionListener(command(controller_UI.printGame()));
        JMenuItem jmiPrintPosition =  new JMenuItem("Print Position");
        gameMenu.add(jmiPrintPosition);
        jmiPrintPosition.addActionListener(command(controller_UI.printFen()));
        gameMenu.addSeparator();
        JMenuItem jmiQuit = new JMenuItem("Quit");
        jmiQuit.addActionListener(e -> { dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)); });
        gameMenu.add(jmiQuit);

        JMenu editMenu = new JMenu("Edit");
        JMenuItem jmiCopyGame = new JMenuItem("Copy Game");
        jmiCopyGame.addActionListener(command(controller_UI.copyPgnToClipboard()));
        jmiCopyGame.setAccelerator(copyKey);
        editMenu.add(jmiCopyGame);
        JMenuItem jmiCopyFEN = new JMenuItem("Copy Position (FEN)");
        jmiCopyFEN.addActionListener(command(controller_UI.copyFenToClipboard()));
        editMenu.add(jmiCopyFEN);

        JMenuItem jmiCopyImage = new JMenuItem("Copy Position (Image)");
        jmiCopyImage.addActionListener(command(controller_UI.copyBitmapToClipboard()));
        editMenu.add(jmiCopyImage);

        JMenuItem jmiPaste =  new JMenuItem("Paste Game/Position");
        jmiPaste.addActionListener(command(controller_UI.pasteFenOrGame()));
        jmiPaste.setAccelerator(pasteKey);
        editMenu.add(jmiPaste);
        editMenu.addSeparator();

        JMenuItem jmiEditGameData = new JMenuItem("Edit Game Data");
        jmiEditGameData.addActionListener(command(controller_UI.editGameData()));
        editMenu.add(jmiEditGameData);

        JMenuItem jmiSetupPosition = new JMenuItem("Setup Position");
        editMenu.add(jmiSetupPosition);
        jmiSetupPosition.addActionListener(command(controller_UI.setupNewPosition()));
        jmiSetupPosition.setAccelerator(setupPosKey);
        editMenu.addSeparator();
        JMenuItem jmiFlipBoard = new JMenuItem("Flip Board");
        editMenu.add(jmiFlipBoard);
        jmiFlipBoard.addActionListener(command(controller_UI.flipBoard()));
        jmiFlipBoard.setAccelerator(flipKey);

        JMenu modeMenu = new JMenu("Engine");

        JMenuItem jmiStartEngine = new JMenuItem("Start Engine");
        jmiStartEngine.addActionListener(command(controller_Engine.startAnalysisMode()));
        jmiStartEngine.setAccelerator(turnEngineOnKey);
        modeMenu.add(jmiStartEngine);
        JMenuItem jmiStopEngine = new JMenuItem("Stop Engine");
        jmiStopEngine.addActionListener(command(controller_Engine.startEnterMovesMode()));
        jmiStopEngine.setAccelerator(turnEngineOffKey);
        modeMenu.add(jmiStopEngine);
        JMenuItem jmiFullGameAnalysis = new JMenuItem("Full Game Analysis");
        jmiFullGameAnalysis.addActionListener(command(controller_Engine.startGameAnalysisMode()));
        modeMenu.add(jmiFullGameAnalysis);
        JMenuItem jmiPlayoutPosition = new JMenuItem("Playout Position");
        jmiPlayoutPosition.addActionListener(command(controller_Engine.startPlayoutPositionMode()));
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

        JMenuItem jmiSetFontSize = new JMenuItem("Set Font Size");
        jmiSetFontSize.addActionListener(controller_UI.changeFontSize());
        viewMenu.add(jmiSetFontSize);

        JMenuItem jmiResetLayout = new JMenuItem("Reset Window Layout");
        jmiResetLayout.addActionListener(e -> {
            setSize(1000, 700);
            setLocationRelativeTo(null);
            revalidate();

            SwingUtilities.invokeLater(() -> {
                getSelectedGameTabView().resetLayout();
            });
        });
        viewMenu.add(jmiResetLayout);

        JMenu databaseMenu = new JMenu("Database");
        JMenuItem jmiDatabase = new JMenuItem("Browse Database");
        jmiDatabase.addActionListener(controller_Pgn.showDatabase());
        databaseMenu.add(jmiDatabase);
        JMenuItem jmiNextGameinDatabase = new JMenuItem("Next Game");
        databaseMenu.add(jmiNextGameinDatabase);
        jmiNextGameinDatabase.addActionListener(command(controller_Pgn.goToNextGameInDatabase()));
        JMenuItem jmiPreviousGameinDatabase = new JMenuItem("Previous Game");
        databaseMenu.add(jmiPreviousGameinDatabase);
        jmiPreviousGameinDatabase.addActionListener(command(controller_Pgn.goToPrevGameInDatabase()));

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
        btnTbNew.addActionListener(command(controller_Engine.startNewGame()));
        JButton btnTbOpen = createToolButton("Open File", "open_folder.svg");
        toolBar.add(btnTbOpen);
        btnTbOpen.addActionListener(command(controller_Pgn.openFile()));
        JButton btnTbSave = createToolButton("Save Game", "file_save.svg");
        btnTbSave.addActionListener(command(controller_Pgn.saveGame()));
        toolBar.add(btnTbSave);

        toolBar.addSeparator();

        JButton btnTbPrint = createToolButton("Print Game", "print.svg");
        toolBar.add(btnTbPrint);
        btnTbPrint.addActionListener(command(controller_UI.printGame()));
        JButton btnTbFlip = createToolButton("Flip Board", "flip3.svg");
        toolBar.add(btnTbFlip);
        btnTbFlip.addActionListener(command(controller_UI.flipBoard()));

        toolBar.addSeparator();

        JButton btnTbCopyGame = createToolButton("Copy Game", "copy1.svg");
        toolBar.add(btnTbCopyGame);
        btnTbCopyGame.addActionListener(command(controller_UI.copyPgnToClipboard()));
        JButton btnTbCopyPosition = createToolButton("Copy Position (FEN)", "copy2.svg");
        toolBar.add(btnTbCopyPosition);
        btnTbCopyPosition.addActionListener(command(controller_UI.copyFenToClipboard()));
        JButton btnTbPaste = createToolButton("Paste Game/Position", "paste.svg");
        toolBar.add(btnTbPaste);
        btnTbPaste.addActionListener(command(controller_UI.pasteFenOrGame()));
        JButton btnTbSetupPosition = createToolButton("Setup Position", "setup_new_position.svg");
        toolBar.add(btnTbSetupPosition);
        btnTbSetupPosition.addActionListener(command(controller_UI.setupNewPosition()));

        toolBar.addSeparator();

        JButton btnTbFullAnalysis = createToolButton("Full Game Analysis", "game_analysis.svg");
        toolBar.add(btnTbFullAnalysis);
        btnTbFullAnalysis.addActionListener(command(controller_Engine.startGameAnalysisMode()));

        toolBar.addSeparator();

        JButton btnTbBrowseDatabase = createToolButton("Browse Database", "database.svg");
        toolBar.add(btnTbBrowseDatabase);
        btnTbBrowseDatabase.addActionListener(controller_Pgn.showDatabase());
        JButton btnTbDatabasePrevGame = createToolButton("Previous Game", "arrow_left_alt.svg");
        toolBar.add(btnTbDatabasePrevGame);
        btnTbDatabasePrevGame.addActionListener(command(controller_Pgn.goToPrevGameInDatabase()));
        JButton btnTbDatabaseNextGame = createToolButton("Next Game", "arrow_right_alt.svg");
        toolBar.add(btnTbDatabaseNextGame);
        btnTbDatabaseNextGame.addActionListener(command(controller_Pgn.goToNextGameInDatabase()));

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
        gameTabs = new JTabbedPane();
        gameTabs.addChangeListener(e -> selectTabSession());
        windowManager = new WindowManager(model, workspace, this, gameTabs);
        commandContext = new CommandContext(model, workspace, gameTabs, windowManager);
        for (GameSession session : workspace.getSessions()) {
            attachGameTab(session);
        }
        GameSession activeSession = workspace.getActiveSession();
        if (activeSession != null) {
            activateSession(activeSession);
        }
        return gameTabs;
    }

    private void workspacePropertyChange(PropertyChangeEvent event) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> workspacePropertyChange(event));
            return;
        }
        if ("sessionAdded".equals(event.getPropertyName())) {
            attachGameTab((GameSession) event.getNewValue());
        }
        if ("sessionRemoved".equals(event.getPropertyName())) {
            removeGameTab((GameSession) event.getOldValue());
        }
        if ("activeSessionChanged".equals(event.getPropertyName())) {
            activateSession((GameSession) event.getNewValue());
        }
    }

    private void attachGameTab(GameSession session) {
        if (gameTabViews.containsKey(session)) {
            return;
        }
        GameTabView tabView = new GameTabView(
                model,
                session,
                controller_UI,
                controller_Engine,
                commandContext
        );
        gameTabViews.put(session, tabView);
        gameTabs.addTab("Game " + gameTabViews.size(), tabView);
    }

    private void activateSession(GameSession session) {
        if (session == null) {
            return;
        }
        attachGameTab(session);
        if (model.getGameSession() != session) {
            model.setGameSession(session);
        }
        GameTabView tabView = gameTabViews.get(session);
        if (windowManager.isDetached(session)) {
            windowManager.activateDetachedWindow(session);
            return;
        }
        if (gameTabs.getSelectedComponent() != tabView) {
            gameTabs.setSelectedComponent(tabView);
        }
    }

    private void selectTabSession() {
        Component selectedComponent = gameTabs.getSelectedComponent();
        for (Map.Entry<GameSession, GameTabView> entry : gameTabViews.entrySet()) {
            if (entry.getValue() == selectedComponent &&
                    workspace.getActiveSession() != entry.getKey()) {
                workspace.setActiveSession(entry.getKey());
                return;
            }
        }
    }

    private GameTabView getSelectedGameTabView() {
        GameTabView tabView = gameTabViews.get(workspace.getActiveSession());
        if (tabView == null) {
            throw new IllegalStateException("No active game tab");
        }
        return tabView;
    }

    private void detachActiveGame() {
        GameSession session = workspace.getActiveSession();
        if (session == null) {
            return;
        }
        windowManager.detach(session, getSelectedGameTabView());
    }

    private void removeGameTab(GameSession session) {
        GameTabView tabView = gameTabViews.remove(session);
        if (tabView == null) {
            return;
        }
        if (windowManager.isDetached(session)) {
            windowManager.disposeDetachedSession(session);
        } else {
            gameTabs.remove(tabView);
        }
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
            windowManager.updateLookAndFeel();

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

        getSelectedGameTabView().setDividerLocations(dividerHorizontal, dividerVertical);
    }

    public int getHorizontalDividerLocation() {
        return getSelectedGameTabView().getHorizontalDividerLocation();
    }

    public int getVerticalDividerLocation() {
        return getSelectedGameTabView().getVerticalDividerLocation();
    }

    public void assignKeyShortcuts() {
        // Keyboard Shortcuts
        shortcuts.put(
                moveForwardKey,
                selectedSessionBoardCommand(Controller_Board::moveForward)
        );
        shortcuts.put(
                moveBackKey,
                selectedSessionBoardCommand(Controller_Board::moveBack)
        );
        shortcuts.put(
                seekFirstKey,
                selectedSessionBoardCommand(Controller_Board::seekToBeginning)
        );
        shortcuts.put(
                seekEndKey,
                selectedSessionBoardCommand(Controller_Board::seekToEnd)
        );
        shortcuts.put(
                copyKey,
                command(controller_UI.copyPgnToClipboard())
        );
        shortcuts.put(
                openKey,
                command(controller_Pgn.openFile())
        );
        shortcuts.put(
                saveKey,
                command(controller_Pgn.saveGame())
        );
        shortcuts.put(
                pasteKey,
                command(controller_UI.pasteFenOrGame())
        );
        shortcuts.put(
                flipKey,
                command(controller_UI.flipBoard())
        );
        shortcuts.put(
                setupPosKey,
                command(controller_UI.setupNewPosition())
        );
        shortcuts.put(
                turnEngineOnKey,
                command(controller_Engine.startAnalysisMode())
        );
        shortcuts.put(
                turnEngineOffKey,
                command(controller_Engine.startEnterMovesMode())
        );
    }

    private ActionListener command(ActionListener listener) {
        return commandContext.bind(listener);
    }

    private ActionListener selectedSessionBoardCommand(
            java.util.function.Function<Controller_Board, ActionListener> action) {
        return command(event -> action.apply(new Controller_Board(
                model, model.getGameSession())).actionPerformed(event));
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("switchLaf".equals(evt.getPropertyName())) {
            setLookAndFeel(model.getLookAndFeel());
        }
    }

}
