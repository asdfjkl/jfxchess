/* JFXChess - A Chess Graphical User Interface
 * Copyright (C) 2020-2025 Dominik Klein
 * Copyright (C) 2025 Torsten Torell
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

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import org.asdfjkl.jfxchess.lib.*;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import atlantafx.base.theme.NordLight;
import atlantafx.base.theme.NordDark;

/**
 * JavaFX App
 */
public class App extends Application implements StateChangeListener {

    Text txtGameData;
    GameModel gameModel;
    EngineOutputView engineOutputView;

    ToggleButton tglEngineOnOff;
    SplitPane spChessboardMoves;
    SplitPane spMain;

    ModeMenuController modeMenuController;

    RadioMenuItem itmEnterMoves;

    ToolBar tbMainWindow;
    CheckMenuItem itmToggleToolbar;

    Button btnThreads = new Button();

    // private RadioMenuItem itmPlayAsWhite = new RadioMenuItem("Play as White");
    // private RadioMenuItem itmPlayAsBlack = new RadioMenuItem("Play as Black");

    String moveBuffer = "";
    private String dragNDropFilePath;

    final KeyCombination keyCombinationCopy = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN);
    final KeyCombination keyCombinationPaste = new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN);
    final KeyCombination keyCombinationSave = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
    final KeyCombination keyCombinationOpen = new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN);
    final KeyCombination keyCombinationNextGame = new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.CONTROL_DOWN);
    final KeyCombination keyCombinationPreviousGame = new KeyCodeCombination(KeyCode.LEFT, KeyCombination.CONTROL_DOWN);
    final KeyCombination keyCombinationEnterPosition = new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN);
    final KeyCombination keyCombinationFlipBoard = new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN);
    final KeyCombination keyCombinationAnalysis = new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN);
    final KeyCombination keyCombinationNewGame = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);
    final KeyCombination keyCombinationEnterMoves = new KeyCodeCombination(KeyCode.M, KeyCombination.CONTROL_DOWN);



    @Override
    public void start(Stage stage) {

        //TestCases tests = new TestCases();
        //tests.runPosHashTest();
        //tests.pgnScanTest();
        //tests.pgnScanSTRTest();
        //tests.testPolyglot();
        //tests.readGamesByStringTest();
        //tests.pgnReadAllMillBaseTest();

        gameModel = new GameModel();
        gameModel.restoreModel();
        gameModel.restoreBoardStyle();
        gameModel.restoreEngines();
        gameModel.restoreGameAnalysisThresholds();
        gameModel.restoreNewGameSettings();
        gameModel.restoreTheme();
        gameModel.restorePaths();
        gameModel.setStageRef(stage);
        gameModel.restoreExtBookPath();
        gameModel.loadExtBook();
        ScreenGeometry screenGeometry = gameModel.restoreScreenGeometry();
        boolean appWindowWasMaximized = gameModel.restoreWindowMaxStatus();
        gameModel.getGame().setTreeWasChanged(true);
        gameModel.getGame().setHeaderWasChanged(true);
        gameModel.maxCpus = Runtime.getRuntime().availableProcessors();

        // MENU
        MenuBar mnuBar = new MenuBar();

        Menu mnuFile = new Menu("Game");
        Menu mnuEdit = new Menu("Edit");
        Menu mnuMode = new Menu("Mode");
        Menu mnuView = new Menu("View");
        Menu mnuDatabase = new Menu("Database");
        Menu mnuHelp = new Menu("Help");

        // File Menu
        MenuItem itmNew = new MenuItem("New...");
        itmNew.setAccelerator(keyCombinationNewGame);
        MenuItem itmOpenFile = new MenuItem("Open File");
        itmOpenFile.setAccelerator(keyCombinationOpen);
        MenuItem itmSaveCurrentGameAs = new MenuItem("Save Game");
        itmSaveCurrentGameAs.setAccelerator(keyCombinationSave);
        MenuItem itmPrintGame = new MenuItem("Print Game");
        MenuItem itmPrintPosition = new MenuItem("Print Position");
        MenuItem itmSavePositionAsImage = new MenuItem("Save Position As Image");
        MenuItem itmQuit = new MenuItem("Quit");

        mnuFile.getItems().addAll(itmNew, itmOpenFile, itmSaveCurrentGameAs,
                new SeparatorMenuItem(), itmPrintGame, itmPrintPosition, itmSavePositionAsImage,
                new SeparatorMenuItem(), itmQuit);

        // Edit Menu
        MenuItem itmCopyGame = new MenuItem("Copy Game");
        itmCopyGame.setAccelerator(keyCombinationCopy);
        MenuItem itmCopyPosition = new MenuItem("Copy Position (FEN)");
        MenuItem itmCopyImage = new MenuItem("Copy Position (Image)");
        MenuItem itmPaste = new MenuItem("Paste Game/Position");
        itmPaste.setAccelerator(keyCombinationPaste);
        MenuItem itmEditGame = new MenuItem("Edit Game Data");
        MenuItem itmEnterPosition = new MenuItem("Enter Position");
        itmEnterPosition.setAccelerator(keyCombinationEnterPosition);
        MenuItem itmFlipBoard = new MenuItem("Flip Board");
        itmFlipBoard.setAccelerator(keyCombinationFlipBoard);

        mnuEdit.getItems().addAll(itmCopyGame, itmCopyPosition, itmCopyImage, itmPaste,
                new SeparatorMenuItem(), itmEditGame, itmEnterPosition,
                new SeparatorMenuItem(), itmFlipBoard);

        // Mode Menu
        RadioMenuItem itmAnalysis = new RadioMenuItem("Analysis");
        itmAnalysis.setAccelerator(keyCombinationAnalysis);
        // itmPlayAsWhite.setAccelerator(keyCombinationPlayWhite);
        // itmPlayAsBlack.setAccelerator(keyCombinationPlayBlack);
        itmEnterMoves = new RadioMenuItem("Enter Moves");
        itmEnterMoves.setAccelerator(keyCombinationEnterMoves);

        RadioMenuItem itmFullGameAnalysis = new RadioMenuItem("Full Game Analysis");
        RadioMenuItem itmPlayOutPosition = new RadioMenuItem("Play Out Position");

        ToggleGroup tglMode = new ToggleGroup();
        tglMode.getToggles().add(itmAnalysis);
        // tglMode.getToggles().add(itmPlayAsWhite);
        // tglMode.getToggles().add(itmPlayAsBlack);
        tglMode.getToggles().add(itmEnterMoves);
        tglMode.getToggles().add(itmFullGameAnalysis);
        tglMode.getToggles().add(itmPlayOutPosition);

        MenuItem itmEngines = new MenuItem("Engines...");
        MenuItem itmSelectBook = new MenuItem("Select Book...");

        mnuMode.getItems().addAll(itmAnalysis, // itmPlayAsWhite, itmPlayAsBlack,
                itmEnterMoves, itmFullGameAnalysis, itmPlayOutPosition,
                new SeparatorMenuItem(), itmEngines, itmSelectBook);

        // View Menu
        MenuItem itmFullscreen = new MenuItem("Fullscreen");
        itmFullscreen.setAccelerator(new KeyCodeCombination(KeyCode.F11));
        itmToggleToolbar = new CheckMenuItem("Show Toolbar");
        itmToggleToolbar.setSelected(true);
        MenuItem itmAppearance = new MenuItem("Appearance");
        MenuItem itmResetLayout = new MenuItem("Reset Layout");

        mnuView.getItems().addAll(itmFullscreen, itmToggleToolbar, itmAppearance, itmResetLayout);

        // Database Menu
        MenuItem itmBrowseDatabase = new MenuItem("Browse Database");
        MenuItem itmNextGame = new MenuItem("Next Game");
        itmNextGame.setAccelerator(keyCombinationNextGame);
        MenuItem itmPreviousGame = new MenuItem("Previous Game");
        itmPreviousGame.setAccelerator(keyCombinationPreviousGame);

        mnuDatabase.getItems().addAll(itmBrowseDatabase, itmNextGame, itmPreviousGame);

        // Help Menu
        MenuItem itmAbout = new MenuItem("About");
        MenuItem itmJerryHomepage = new MenuItem("JFXChess - Homepage");

        mnuHelp.getItems().addAll(itmAbout, itmJerryHomepage);

        mnuBar.getMenus().addAll(mnuFile, mnuEdit, mnuMode, mnuView, mnuDatabase, mnuHelp);

        // TOOLBAR
        tbMainWindow = new ToolBar();
        Button btnNew = new Button("New");
        btnNew.setGraphic(new ImageView( new Image("icons/document-new.png")));
        btnNew.setContentDisplay(ContentDisplay.TOP);

        Button btnOpen = new Button("Open File");
        btnOpen.setGraphic(new ImageView( new Image("icons/document-open.png")));
        btnOpen.setContentDisplay(ContentDisplay.TOP);

        Button btnSaveAs = new Button("Save Game");
        btnSaveAs.setGraphic(new ImageView( new Image("icons/document-save.png")));
        btnSaveAs.setContentDisplay(ContentDisplay.TOP);

        Button btnPrint = new Button("Print Game");
        btnPrint.setGraphic(new ImageView( new Image("icons/document-print.png")));
        btnPrint.setContentDisplay(ContentDisplay.TOP);

        Button btnFlipBoard = new Button("Flip Board");
        btnFlipBoard.setGraphic(new ImageView( new Image("icons/view-refresh.png")));
        btnFlipBoard.setContentDisplay(ContentDisplay.TOP);

        Button btnCopyGame = new Button("Copy Game");
        btnCopyGame.setGraphic(new ImageView( new Image("icons/edit-copy-pgn.png")));
        btnCopyGame.setContentDisplay(ContentDisplay.TOP);

        Button btnCopyPosition = new Button("Copy Position");
        btnCopyPosition.setGraphic(new ImageView( new Image("icons/edit-copy-fen.png")));
        btnCopyPosition.setContentDisplay(ContentDisplay.TOP);

        Button btnPaste = new Button("Paste");
        btnPaste.setGraphic(new ImageView( new Image("icons/edit-paste.png")));
        btnPaste.setContentDisplay(ContentDisplay.TOP);

        Button btnEnterPosition = new Button("Enter Position");
        btnEnterPosition.setGraphic(new ImageView( new Image("icons/document-enter-position.png")));
        btnEnterPosition.setContentDisplay(ContentDisplay.TOP);

        Button btnFullAnalysis = new Button("Full Analysis");
        btnFullAnalysis.setGraphic(new ImageView( new Image("icons/emblem-system.png")));
        btnFullAnalysis.setContentDisplay(ContentDisplay.TOP);

        Button btnBrowseGames = new Button("Browse Games");
        btnBrowseGames.setGraphic(new ImageView( new Image("icons/database.png")));
        btnBrowseGames.setContentDisplay(ContentDisplay.TOP);

        Button btnPrevGame = new Button("Prev. Game");
        btnPrevGame.setGraphic(new ImageView( new Image("icons/go-previous-blue.png")));
        btnPrevGame.setContentDisplay(ContentDisplay.TOP);

        Button btnNextGame = new Button("Next Game");
        btnNextGame.setGraphic(new ImageView( new Image("icons/go-next-blue.png")));
        btnNextGame.setContentDisplay(ContentDisplay.TOP);

        Button btnAbout = new Button("About");
        btnAbout.setGraphic(new ImageView( new Image("icons/help-browser.png")));
        btnAbout.setContentDisplay(ContentDisplay.TOP);

        tbMainWindow.getItems().addAll(btnNew, btnOpen, btnSaveAs, btnPrint, new Separator(),
                btnFlipBoard, new Separator(),
                btnCopyGame, btnCopyPosition, btnPaste, btnEnterPosition, new Separator(),
                btnFullAnalysis, new Separator(),
                btnBrowseGames, btnPrevGame, btnNextGame,  btnAbout);


        // Text & Edit Button for Game Data
        txtGameData = new Text("FOO BAR");
        txtGameData.setTextAlignment(TextAlignment.CENTER);
        txtGameData.getStyleClass().add("generic-widget");
        Button btnEditGameData = new Button();
        btnEditGameData.setGraphic(new ImageView( new Image("icons/document_properties_small.png")));
        Region spacerGameDataLeft = new Region();
        Region spacerGameDataRight = new Region();

        HBox hbGameData = new HBox();
        hbGameData.getChildren().addAll(spacerGameDataLeft, txtGameData, spacerGameDataRight, btnEditGameData);
        HBox.setHgrow(spacerGameDataLeft, Priority.ALWAYS);
        HBox.setHgrow(spacerGameDataLeft, Priority.ALWAYS);
        HBox.setHgrow(spacerGameDataRight, Priority.ALWAYS);

        MoveView moveView = new MoveView(gameModel);

        // Navigation Buttons
        Button btnMoveBegin = new Button();
        btnMoveBegin.setGraphic(new ImageView( new Image("icons/go-first.png")));
        Button btnMoveEnd = new Button();
        btnMoveEnd.setGraphic(new ImageView( new Image("icons/go-last.png")));
        Button btnMovePrev = new Button();
        btnMovePrev.setGraphic(new ImageView( new Image("icons/go-previous.png")));
        Button btnMoveNext = new Button();
        btnMoveNext.setGraphic(new ImageView( new Image("icons/go-next.png")));

        HBox hbGameNavigation = new HBox();
        hbGameNavigation.setAlignment(Pos.CENTER);
        hbGameNavigation.getChildren().addAll(btnMoveBegin, btnMovePrev, btnMoveNext, btnMoveEnd);

        TabPane tabPaneMovesNotationBook = new TabPane();

        BookView bookView = new BookView(gameModel);

        ScrollPane spMoves = new ScrollPane();
        moveView.flow.setPrefHeight(4000);
        spMoves.setContent(moveView.flow);

        spMoves.setFitToWidth(true);   // makes the TextFlow fit horizontally
        spMoves.setFitToHeight(false);

        //Tab tabMoves = new Tab("Moves", moveView.getWebView());
        Tab tabMoves = new Tab("Moves", spMoves);
        Tab tabNotation = new Tab("Score Sheet"  , new Label("score sheet"));
        //Tab tabBook = new Tab("Book" , new Label("book"));
        Tab tabBook = new Tab("Book" , bookView.bookTable);



        //tabPaneMovesNotationBook.getTabs().addAll(tabMoves, tabNotation, tabBook);
        tabPaneMovesNotationBook.getTabs().addAll(tabMoves, tabBook);
        tabPaneMovesNotationBook.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPaneMovesNotationBook.getStyleClass().add("underlined");

        VBox vbGameDataMovesNavigation = new VBox();
        vbGameDataMovesNavigation.getChildren().addAll(hbGameData, tabPaneMovesNotationBook, hbGameNavigation);
        hbGameNavigation.setMaxHeight(200);
        VBox.setVgrow(spMoves, Priority.ALWAYS);
        //VBox.setVgrow(moveView.flow, Priority.ALWAYS);

        // put together  Chessboard | Game Navigation
        Chessboard chessboard = new Chessboard(gameModel);
        chessboard.boardStyle = gameModel.boardStyle;
        chessboard.resize(100,100);
        //chessboard.updateCanvas(); Already done inside resize().

        spChessboardMoves = new SplitPane();
        spChessboardMoves.getItems().addAll(chessboard, vbGameDataMovesNavigation);
        spChessboardMoves.setMaxHeight(Double.MAX_VALUE);

        // Buttons for Engine On/Off and TextFlow for Engine Output
        tglEngineOnOff = new ToggleButton("Off");
        Label lblMultiPV = new Label("Lines:");
        Button btnAddEngineLine = new Button();
        btnAddEngineLine.setText("+");
        Button btnRemoveEngineLine = new Button();
        btnRemoveEngineLine.setText("-");

        //btnThreads.setText("1 Thread(s)");
        int currThreads = gameModel.activeEngine.getNrThreads();
        btnThreads.setText(currThreads + " Thread(s)");

        Button btnSelectEngine = new Button();
        btnSelectEngine.setGraphic(new ImageView( new Image("icons/document_properties_small.png")));
        HBox hbEngineControl = new HBox();
        Region spacerEngineControl = new Region();
        hbEngineControl.getChildren().addAll(tglEngineOnOff, lblMultiPV,
                btnAddEngineLine, btnRemoveEngineLine,
                btnThreads, spacerEngineControl, new Label("Engines: "),
		btnSelectEngine);
        hbEngineControl.setAlignment(Pos.CENTER);
        HBox.setMargin(lblMultiPV, new Insets(0,5,0,15));
        HBox.setMargin(btnThreads, new Insets(0,5,0,15));
        HBox.setHgrow(spacerEngineControl, Priority.ALWAYS);
        TextFlow txtEngineOut = new TextFlow();
        txtEngineOut.setPadding(new Insets(10,10,10,10));

        ScrollPane scpEngineOut = new ScrollPane(txtEngineOut);
        scpEngineOut.setFitToWidth(true);
        scpEngineOut.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        txtEngineOut.prefWidthProperty().bind(scpEngineOut.widthProperty().subtract(20));
        scpEngineOut.prefHeightProperty().bind(txtEngineOut.heightProperty());

        VBox vbBottom = new VBox();
        vbBottom.getChildren().addAll(hbEngineControl, scpEngineOut);
        vbBottom.setMinHeight(10);

        // put everything excl. the bottom Engine part into one VBox
        VBox vbMainUpperPart = new VBox();
        vbMainUpperPart.getChildren().addAll(mnuBar, tbMainWindow, spChessboardMoves);
        VBox.setVgrow(spChessboardMoves, Priority.ALWAYS);

        // add another split pane for main window part and engine output
        spMain = new SplitPane();
        spMain.setOrientation(Orientation.VERTICAL);
        spMain.getItems().addAll(vbMainUpperPart, vbBottom);
        spMain.getStyleClass().add("generic-widget");

        GameMenuController gameMenuController = new GameMenuController(gameModel);

        itmOpenFile.setOnAction(actionEvent -> { gameMenuController.handleOpenGame(); } );
        btnOpen.setOnAction(actionEvent -> { gameMenuController.handleOpenGame(); } );

        itmSavePositionAsImage.setOnAction(actionEvent -> { gameMenuController.handleSaveBoardPicture(chessboard);});

        btnMoveNext.setOnAction(event -> {
            if(!gameModel.blockGUI) {
                moveView.goForward();
            }});
        btnMoveBegin.setOnAction(event -> {
            if(!gameModel.blockGUI) {
                moveView.seekToRoot();
            }});
        btnMovePrev.setOnAction(event -> {
            if(!gameModel.blockGUI) {
                moveView.goBack();
            }});
        btnMoveEnd.setOnAction(event -> {
            if(!gameModel.blockGUI) {
                moveView.seekToEnd();
            }});

        // events
        gameModel.addListener(chessboard);
        gameModel.addListener(moveView);
        gameModel.addListener(bookView);
        gameModel.addListener(this);

        // connect mode controller
        engineOutputView = new EngineOutputView(gameModel, txtEngineOut);
        modeMenuController = new ModeMenuController(gameModel, engineOutputView);
        gameModel.addListener(engineOutputView);

        EditMenuController editMenuController = new EditMenuController(gameModel);

        gameModel.addListener(modeMenuController);
        modeMenuController.activateEnterMovesMode();
        modeMenuController.setEngineNameAndInfoToOuptput();

        itmSaveCurrentGameAs.setOnAction(e -> {
           gameMenuController.handleSaveCurrentGame();
        });

        itmPrintGame.setOnAction(actionEvent -> {
            gameMenuController.handlePrintGame(stage);
        });

        itmPrintPosition.setOnAction(actionEvent -> {
            gameMenuController.handlePrintPosition(stage);
        });

        /*
        itmPlayAsWhite.setOnAction(actionEvent -> {
            if(itmPlayAsWhite.isSelected()) {
                if(gameModel.getMode() != GameModel.MODE_PLAY_WHITE) {
                    itmPlayAsWhite.setSelected(true);
                    tglEngineOnOff.setSelected(true);
                    tglEngineOnOff.setText("On");
                    modeMenuController.activatePlayWhiteMode();
                }
            }
        }); */

        /*
        itmPlayAsBlack.setOnAction(actionEvent -> {
            if(itmPlayAsBlack.isSelected()) {
                if(gameModel.getMode() != GameModel.MODE_PLAY_BLACK) {
                    itmPlayAsBlack.setSelected(true);
                    tglEngineOnOff.setSelected(true);
                    tglEngineOnOff.setText("On");
                    modeMenuController.activatePlayBlackMode();
                }
            }
        }); */

        itmPlayOutPosition.setOnAction(actionEvent -> {
            if(gameModel.getMode() != GameModel.MODE_PLAYOUT_POSITION) {
                itmPlayOutPosition.setSelected(true);
                tglEngineOnOff.setSelected(true);
                tglEngineOnOff.setText("on");
                modeMenuController.activatePlayoutPositionMode();
            }
        });

        itmAnalysis.setOnAction(actionEvent -> {
            if(itmAnalysis.isSelected()) {
                if(gameModel.getMode() != GameModel.MODE_ANALYSIS) {
                    itmAnalysis.setSelected(true);
                    tglEngineOnOff.setSelected(true);
                    tglEngineOnOff.setText("On");
                    modeMenuController.activateAnalysisMode();
                }
            }
        });

        itmEnterMoves.setOnAction(actionEvent -> {
            tglEngineOnOff.setSelected(false);
            tglEngineOnOff.setText("Off");
            modeMenuController.activateEnterMovesMode();
        });

        itmFullGameAnalysis.setOnAction(actionEvent -> {
            handleFullGameAnalysis();
        });

        tglEngineOnOff.setOnAction(actionEvent -> {
            if(tglEngineOnOff.isSelected()) {
                itmAnalysis.setSelected(true);
                tglEngineOnOff.setText("On");
                modeMenuController.activateAnalysisMode();
            } else {
                itmEnterMoves.setSelected(true);
                tglEngineOnOff.setText("Off");
                modeMenuController.activateEnterMovesMode();
            }
        });


        btnAddEngineLine.setOnAction(actionEvent -> {
            int currentMultiPv = gameModel.getMultiPv();
            if (currentMultiPv < gameModel.activeEngine.getMaxMultiPV() &&
		            currentMultiPv < GameModel.MAX_PV) {
                gameModel.setMultiPv(currentMultiPv + 1);
                modeMenuController.engineSetOptionMultiPV(gameModel.getMultiPv());
                gameModel.triggerStateChange();
            }
        });

        btnRemoveEngineLine.setOnAction(actionEvent -> {
            int currentMultiPv = gameModel.getMultiPv();
            if (currentMultiPv > 1) {
                gameModel.setMultiPv(currentMultiPv - 1);
                modeMenuController.engineSetOptionMultiPV(gameModel.getMultiPv());
                gameModel.triggerStateChange();
            }
        });

        btnThreads.setOnAction(actionEvent -> {
            if(!gameModel.blockGUI) {
                if(gameModel.activeEngine.supportsMultiThread()) {
                    int currentNrThreads = gameModel.activeEngine.getNrThreads();
                    int maxThreads = Math.min(gameModel.maxCpus - 1,  gameModel.activeEngine.getMaxThreads());
                    DialogThreads dlgThreads = new DialogThreads();
                    boolean accepted = dlgThreads.show(stage, currentNrThreads, maxThreads);
                    if(accepted) {
                        int newNrThreads = dlgThreads.spThreads.getValue();
                        if(newNrThreads != currentNrThreads) {
                            gameModel.activeEngine.setThreads(newNrThreads);
                            modeMenuController.engineSetThreads(newNrThreads);
                            btnThreads.setText(newNrThreads + " Thread(s)");
                        }
                    }
                }
            }
        });

        itmNew.setOnAction(e -> {
            handleNewGame();
        });

        itmQuit.setOnAction(event -> {
            onExit(stage);
        });

        itmEditGame.setOnAction(e -> {
            editMenuController.editGameData();
        });

        btnEditGameData.setOnAction(e -> {
            editMenuController.editGameData();
        });

        itmEnterPosition.setOnAction(e -> {
            double height = Math.max(stage.getHeight() * 0.6, 520);
            editMenuController.enterPosition(height, chessboard.boardStyle);
        });

        itmFullscreen.setOnAction(e -> {
            stage.setFullScreen(true);
        });

        itmAppearance.setOnAction(e -> {
            DialogAppearance dlg = new DialogAppearance();
            double height = Math.max(stage.getHeight() * 0.6, 520);
            double width = height * 1.4;
            boolean accepted = dlg.show(stage, chessboard.boardStyle, width, height, gameModel.THEME);
            if(accepted) {
                chessboard.boardStyle.setColorStyle(dlg.appearanceBoard.boardStyle.getColorStyle());
                chessboard.boardStyle.setPieceStyle(dlg.appearanceBoard.boardStyle.getPieceStyle());
                gameModel.boardStyle = chessboard.boardStyle;
                gameModel.THEME = dlg.selectedColorTheme;
                gameModel.triggerStateChange();
            }
        });

        itmToggleToolbar.setOnAction(e -> {
            if(itmToggleToolbar.isSelected()) {
                tbMainWindow.setVisible(true);
                tbMainWindow.setManaged(true);
            } else {
                tbMainWindow.setVisible(false);
                tbMainWindow.setManaged(false);
            }
        });

        itmEngines.setOnAction(e -> {
            modeMenuController.editEngines();
        });

        itmSelectBook.setOnAction(e -> {

            Stage fileChooserStage = new Stage();
            fileChooserStage.initModality(Modality.APPLICATION_MODAL);
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Opening Book (.bin)");
            fileChooser.setInitialDirectory(gameModel.getJarPath().toFile());
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("BIN", "*.bin")
            );
            File file = fileChooser.showOpenDialog(fileChooserStage);
            if (file != null) {
                gameModel.extBookPath = file.toString();
                gameModel.loadExtBook();
                gameModel.triggerStateChange();
            }
        });

        btnSelectEngine.setOnAction(e -> {
            modeMenuController.editEngines();
        });

        itmCopyGame.setOnAction(e -> {
            editMenuController.copyGame();
        });

        itmCopyPosition.setOnAction(e -> {
            editMenuController.copyPosition();
        });

        itmCopyImage.setOnAction(e -> {
                editMenuController.copyImage();
        });

        itmPaste.setOnAction(e -> {

            // don't allow this while a user is playing a game
            if(!gameModel.blockGUI) {
                String pasteString = Clipboard.getSystemClipboard().getString().trim();
                editMenuController.paste(pasteString);
            }
        });

        itmResetLayout.setOnAction(e -> {
            Rectangle2D screenBounds = Screen.getPrimary().getBounds();
            stage.setWidth(screenBounds.getWidth() * ScreenGeometry.DEFAULT_WIDTH_RATIO);
            stage.setHeight(screenBounds.getHeight() * ScreenGeometry.DEFAULT_HEIGHT_RATIO);
            spChessboardMoves.setDividerPosition(0, ScreenGeometry.DEFAULT_MOVE_DIVIDER_RATIO);
            spMain.setDividerPosition(0, ScreenGeometry.DEFAULT_MAIN_DIVIDER_RATIO);
            stage.centerOnScreen();
            gameModel.triggerStateChange();
        });

        itmFlipBoard.setOnAction(e -> {
            gameModel.setFlipBoard(!gameModel.getFlipBoard());
            gameModel.triggerStateChange();
        });

        itmBrowseDatabase.setOnAction(e -> {
            // don't allow this during a game
            if(!gameModel.blockGUI) {
                gameMenuController.handleBrowseDatabase();
            }
        });

        itmNextGame.setOnAction(e -> {
            // don't allow this during a game
            if(!gameModel.blockGUI) {
                gameMenuController.handleNextGame();
            }
        });

        itmPreviousGame.setOnAction(e -> {
            // don't allow this during a game
            if(!gameModel.blockGUI) {
                gameMenuController.handlePrevGame();
            }
        });

        itmAbout.setOnAction(event -> {
            DialogAbout.show(stage, gameModel);
        });

        itmJerryHomepage.setOnAction(event -> {
            getHostServices().showDocument("https://github.com/asdfjkl/jfxchess");
        });

        stage.setOnCloseRequest(event -> {
            event.consume();
            onExit(stage);
        });

        // buttons
        btnNew.setOnAction(e -> {
            handleNewGame();
        });

        btnOpen.setOnAction(e -> {
            gameMenuController.handleOpenGame();
        });

        btnSaveAs.setOnAction(e -> {
            gameMenuController.handleSaveCurrentGame();
        });

        btnPrint.setOnAction(e -> {
            gameMenuController.handlePrintGame(stage);
        });

        btnFlipBoard.setOnAction(e -> {
            gameModel.setFlipBoard(!gameModel.getFlipBoard());
            gameModel.triggerStateChange();
        });

        btnCopyGame.setOnAction(e -> {
            editMenuController.copyGame();
        });

        btnCopyPosition.setOnAction(e -> {
            editMenuController.copyPosition();
        });

        btnPaste.setOnAction(e -> {
            // don't allow this during a game
            if(!gameModel.blockGUI) {
                String pasteString = Clipboard.getSystemClipboard().getString().trim();
                editMenuController.paste(pasteString);
            }
        });

        btnEnterPosition.setOnAction(e -> {
            double height = Math.max(stage.getHeight() * 0.6, 520);
            editMenuController.enterPosition(height, chessboard.boardStyle);
        });

        btnFullAnalysis.setOnAction(e -> {
            handleFullGameAnalysis();
        });

        btnBrowseGames.setOnAction(e -> {
            // don't allow this during a game
            if(!gameModel.blockGUI) {
                gameMenuController.handleBrowseDatabase();
            }
        });

        btnPrevGame.setOnAction(e -> {
            // don't allow this during a game
            if(!gameModel.blockGUI) {
                gameMenuController.handlePrevGame();
            }
        });

        btnNextGame.setOnAction(e -> {
            // don't allow this during a game
            if(!gameModel.blockGUI) {
                gameMenuController.handleNextGame();
            }
        });

        btnAbout.setOnAction(e -> {
            DialogAbout.show(stage, gameModel);
        });

        vbMainUpperPart.setOnDragOver((DragEvent event) -> {
            // accept it only if it is  not dragged from the same node
            // and if it has one pgn-file */
            Dragboard db = event.getDragboard();
            if (event.getGestureSource() != vbMainUpperPart
                    && db.hasFiles()) {
                if (db.getFiles().size() == 1
                        && db.getFiles().get(0).getName().endsWith(".pgn")) {
                    /* allow only for copying */
                    event.acceptTransferModes(TransferMode.COPY);
                }
            }
            event.consume();
        });

        vbMainUpperPart.setOnDragDropped((DragEvent event) -> {
            // data dropped
            Dragboard db = event.getDragboard();
            if (db.hasFiles() && db.getFiles().size() == 1
                    && db.getFiles().get(0).getName().endsWith(".pgn")) {
                String filePath = db.getFiles().get(0).getAbsolutePath();
                File file = new File(filePath);
                gameMenuController.openFile(file);
                event.setDropCompleted(true);
            }
            event.consume();
        });



        Scene scene = new Scene(spMain);

        scene.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
            /* to enter moves via keyboard */
            if (event.getCode() == KeyCode.A ) {
                addToMoveBuffer("a");
            }
            if (event.getCode() == KeyCode.B ) {
                addToMoveBuffer("b");
            }
            if (event.getCode() == KeyCode.C ) {
                addToMoveBuffer("c");
            }
            if (event.getCode() == KeyCode.D ) {
                addToMoveBuffer("d");
            }
            if (event.getCode() == KeyCode.E ) {
                addToMoveBuffer("e");
            }
            if (event.getCode() == KeyCode.F ) {
                addToMoveBuffer("f");
            }
            if (event.getCode() == KeyCode.G ) {
                addToMoveBuffer("g");
            }
            if (event.getCode() == KeyCode.H ) {
                addToMoveBuffer("h");
            }
            if (event.getCode() == KeyCode.Q ) {
                addToMoveBuffer("q");
            }
            if (event.getCode() == KeyCode.R ) {
                addToMoveBuffer("r");
            }
            if (event.getCode() == KeyCode.N ) {
                addToMoveBuffer("n");
            }
            if (event.getCode() == KeyCode.B ) {
                addToMoveBuffer("b");
            }
            if (event.getCode() == KeyCode.DIGIT1 ) {
                addToMoveBuffer("1");
            }
            if (event.getCode() == KeyCode.DIGIT2 ) {
                addToMoveBuffer("2");
            }
            if (event.getCode() == KeyCode.DIGIT3 ) {
                addToMoveBuffer("3");
            }
            if (event.getCode() == KeyCode.DIGIT4 ) {
                addToMoveBuffer("4");
            }
            if (event.getCode() == KeyCode.DIGIT5 ) {
                addToMoveBuffer("5");
            }
            if (event.getCode() == KeyCode.DIGIT6 ) {
                addToMoveBuffer("6");
            }
            if (event.getCode() == KeyCode.DIGIT7 ) {
                addToMoveBuffer("7");
            }
            if (event.getCode() == KeyCode.DIGIT8 ) {
                addToMoveBuffer("8");
            }

            /* other gui controls */
            if (event.getCode() == KeyCode.RIGHT) {
                // don't allow this during a game
                if(!gameModel.blockGUI) {
                    moveView.goForward();
                }
            }
            if (event.getCode() == KeyCode.LEFT) {
                // don't allow this during a game
                if(!gameModel.blockGUI) {
                    moveView.goBack();
                }
            }
            if (event.getCode() == KeyCode.HOME) {
                // don't allow this during a game
                if(!gameModel.blockGUI) {
                    moveView.seekToRoot();
                }
            }
            if (event.getCode() == KeyCode.END) {
                // don't allow this during a game
                if(!gameModel.blockGUI) {
                    moveView.seekToEnd();
                }
            }
            if (keyCombinationEnterPosition.match(event)) {
                // setup position
                double height = Math.max(stage.getHeight() * 0.6, 520);
                editMenuController.enterPosition(height, chessboard.boardStyle);
            }
            if (keyCombinationFlipBoard.match(event)) {
                gameModel.setFlipBoard(!gameModel.getFlipBoard());
                gameModel.triggerStateChange();
            }
            if(keyCombinationAnalysis.match(event)) {
                // enter analysis mode
                if(gameModel.getMode() != GameModel.MODE_ANALYSIS) {
                    itmAnalysis.setSelected(true);
                    tglEngineOnOff.setSelected(true);
                    tglEngineOnOff.setText("On");
                    modeMenuController.activateAnalysisMode();
                }
            }
            /*
            if(keyCombinationPlayWhite.match(event)) {
                if(gameModel.getMode() != GameModel.MODE_PLAY_WHITE) {
                    itmPlayAsWhite.setSelected(true);
                    tglEngineOnOff.setSelected(true);
                    tglEngineOnOff.setText("On");
                    modeMenuController.activatePlayWhiteMode();
                }
            }
            if(keyCombinationPlayBlack.match(event)) {
                if(gameModel.getMode() != GameModel.MODE_PLAY_BLACK) {
                    itmPlayAsBlack.setSelected(true);
                    tglEngineOnOff.setSelected(true);
                    tglEngineOnOff.setText("On");
                    modeMenuController.activatePlayBlackMode();
                }
            } */
            if(keyCombinationEnterMoves.match(event)|| event.getCode() == KeyCode.ESCAPE) {
                // enter moves mode
                tglEngineOnOff.setSelected(false);
                tglEngineOnOff.setText("Off");
                modeMenuController.activateEnterMovesMode();
            }
            if(event.getCode() == KeyCode.F11) {
                stage.setFullScreen(true);
            }
            if(keyCombinationNextGame.match(event)) {
                // don't allow this during a game
                if(!gameModel.blockGUI) {
                    gameMenuController.handleNextGame();
                }
            }
            if(keyCombinationPreviousGame.match(event)) {
                // don't allow this during a game
                if(!gameModel.blockGUI) {
                    gameMenuController.handlePrevGame();
                }
            }
            if(keyCombinationCopy.match(event)) {
                editMenuController.copyGame();
            }
            if(keyCombinationPaste.match(event)) {
                // don't allow this during a game
                if(!gameModel.blockGUI) {
                    String pasteString = Clipboard.getSystemClipboard().getString().trim();
                    editMenuController.paste(pasteString);
                }
            }
            if(keyCombinationOpen.match(event)) {
                gameMenuController.handleOpenGame();
            }
            if(keyCombinationSave.match(event)) {
                gameMenuController.handleSaveCurrentGame();
            }
            event.consume();
        });

        itmEnterMoves.setSelected(true);


        // Setup Style
        if(gameModel.THEME == gameModel.STYLE_LIGHT) {
            Application.setUserAgentStylesheet(new NordLight().getUserAgentStylesheet());
        } else {
            Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());
        }
        scene.getStylesheets().add(getClass().getResource("/css/generic-widget.css").toString());

        SplashScreen splash = SplashScreen.getSplashScreen();
        if (splash != null && splash.isVisible()) {
            splash.close();
        }



        stage.setScene(scene);
        // restore previous screen geometry
        // first check if app was maximized when closing. Then set to max and don't bother
        // with further restoring
        if(appWindowWasMaximized) {
            stage.setMaximized(true);
        } else {
            // get primary screen bounds and set these to scene or
            // restore previously set screen geometry
            Rectangle2D screenBounds = Screen.getPrimary().getBounds();
            if(screenGeometry.isValid()) {
                stage.setX(screenGeometry.xOffset);
                stage.setY(screenGeometry.yOffset);
                stage.setWidth(screenGeometry.width);
                stage.setHeight(screenGeometry.height);
            } else {
                stage.setWidth(screenBounds.getWidth() * ScreenGeometry.DEFAULT_WIDTH_RATIO);
                stage.setHeight(screenBounds.getHeight() * ScreenGeometry.DEFAULT_HEIGHT_RATIO);
            }
        }

        boolean toolBarVisible = gameModel.restoreToolbarVisibility();
        if(toolBarVisible) {
            itmToggleToolbar.setSelected(true);
            tbMainWindow.setVisible(true);
            tbMainWindow.setManaged(true);
        } else {
            itmToggleToolbar.setSelected(false);
            tbMainWindow.setVisible(false);
            tbMainWindow.setManaged(false);
        }

        gameModel.triggerStateChange();

        // un-focus any default button etc.
        spMain.requestFocus();

        stage.getIcons().add(new Image("icons/app_icon.png"));
        stage.setTitle("JFXChess");
        stage.show();

        // recover divider ratios at the last step, as show() might trigger resizes
        spChessboardMoves.setDividerPosition(0, screenGeometry.moveDividerRatio);
        spMain.setDividerPosition(0, screenGeometry.mainDividerRatio);

     }

    @Override
    public void stateChange() {

        //if(gameModel.getGame().isHeaderChanged()) {
            String white = gameModel.getGame().getHeader("White");
            String black = gameModel.getGame().getHeader("Black");
            String site = gameModel.getGame().getHeader("Site");
            String date = gameModel.getGame().getHeader("Date");

            String label = white + " - " + black + "\n" + site + ", " + date;
            txtGameData.setText(label);
        //}
        if(gameModel.getMode() == GameModel.MODE_ENTER_MOVES) {
	    // To show entermoves as selected in the menu after e.g. editing engines.
	    // Previously the latest used menuitem was still selected.
            itmEnterMoves.setSelected(true);
            tglEngineOnOff.setSelected(false);
            tglEngineOnOff.setText("Off");
        } else {
            tglEngineOnOff.setSelected(true);
            tglEngineOnOff.setText("On");
        }
    }


    public static void main(String[] args) {
        launch();
    }

    private void addToMoveBuffer(String s) {

        /* TODO
           this is buggy for now, remove for release 4.5
         */

        /*
        moveBuffer += s;
        // don't allow this during a game when
        // it's not the player's turn, i.e. when the GUI is blocked
        if(!gameModel.blockGUI) {
            if (moveBuffer.length() == 4) {
                try {
                    Move move = new Move(moveBuffer);
                    Board board = gameModel.getGame().getCurrentNode().getBoard();
                    if (!board.isLegalAndPromotes(move)) {
                        if (board.isLegal(move)) {
                            gameModel.getGame().applyMove(move);
                            gameModel.triggerStateChange();
                        }
                        moveBuffer = "";
                    }
                } catch (IllegalArgumentException e) {
                    // ignore
                }
            }
            if (moveBuffer.length() == 5) {
                try {
                    Move move = new Move(moveBuffer);
                    Board board = gameModel.getGame().getCurrentNode().getBoard();
                    if (board.isLegal(move)) {
                        gameModel.getGame().applyMove(move);
                        gameModel.triggerStateChange();
                        moveBuffer = "";
                    }
                } catch (IllegalArgumentException e) {
                        // ignore
                }
                moveBuffer = "";
            }
        }
         */
    }

    private void onExit(Stage stage) {

        gameModel.saveModel();
        ScreenGeometry g = new ScreenGeometry(stage.getX(),
                stage.getY(), stage.getWidth(), stage.getHeight(),
                spChessboardMoves.getDividerPositions()[0],
                spMain.getDividerPositions()[0]);
        gameModel.saveScreenGeometry(g);
        gameModel.saveWindowMaxStatus(stage.isMaximized());
        gameModel.saveBoardStyle();
        gameModel.saveEngines();
        gameModel.saveGameAnalysisThresholds();
        gameModel.saveNewGameSettings();
        gameModel.saveToolbarVisibility(tbMainWindow.isVisible());
        gameModel.saveTheme();
        gameModel.savePaths();
        gameModel.saveExtBookPath();

        modeMenuController.stopEngine();
        ArrayList<Task> runningTasks = gameModel.getPgnDatabase().getRunningTasks();
        for (Task task : runningTasks) {
            task.cancel();
        }

        stage.close();
    }

    private void handleNewGame() {
        DialogNewGame dlg = new DialogNewGame();
        int result = dlg.show(gameModel.getStageRef());
        if(result >= 0) {
            if(result == DialogNewGame.ENTER_ANALYSE) {
                // clean up current game, but otherwise not much to do
                gameModel.wasSaved = false;
                gameModel.currentPgnDatabaseIdx = -1;
                gameModel.setComputerThinkTimeSecs(3);
                Game g = new Game();
                Board b = new Board(true);
                g.getRootNode().setBoard(b);
                gameModel.setGame(g);
                gameModel.getGame().setTreeWasChanged(true);
                gameModel.getGame().setHeaderWasChanged(true);
                modeMenuController.activateEnterMovesMode();
            }
            if(result == DialogNewGame.PLAY_BOTS) {
                DialogPlayBots dlgPlay = new DialogPlayBots();
                boolean playAccepted = dlgPlay.show(gameModel.getStageRef(),
                        gameModel.botEngines);
                if(playAccepted) {
                    gameModel.wasSaved = false;
                    gameModel.currentPgnDatabaseIdx = -1;
                    gameModel.setComputerThinkTimeSecs(3);
                    Game g = new Game();
                    Board b;
                    if(dlgPlay.startInitial) {
                        b = new Board(true);
                    } else {
                        b = gameModel.getGame().getCurrentNode().getBoard().makeCopy();
                    }
                    g.getRootNode().setBoard(b);
                    gameModel.setGame(g);
                    gameModel.getGame().setTreeWasChanged(true);
                    gameModel.getGame().setHeaderWasChanged(true);
                    gameModel.selectedPlayEngine = gameModel.botEngines.get(dlgPlay.selectedIndex);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
                    String formattedDate = LocalDate.now().format(formatter);
                    g.setHeader("Date", formattedDate);
                    g.setHeader("Event", "Training Game JFXChess");
                    if(dlgPlay.playWhite) {
                        g.setHeader("White", "N.N.");
                        g.setHeader("Black", gameModel.selectedPlayEngine.getName());
                        g.setHeader("BlackElo", ((BotEngine) gameModel.selectedPlayEngine).getElo());
                        gameModel.setFlipBoard(false);
                        // itmPlayAsWhite.setSelected(true);
                        modeMenuController.activatePlayWhiteMode();
                    } else {
                        g.setHeader("Black", "N.N.");
                        g.setHeader("White", gameModel.selectedPlayEngine.getName());
                        g.setHeader("WhiteElo", ((BotEngine) gameModel.selectedPlayEngine).getElo());
                        gameModel.setFlipBoard(true);
                        // itmPlayAsBlack.setSelected(true);
                        modeMenuController.activatePlayBlackMode();
                    }
                }
            }
            if(result == DialogNewGame.PLAY_UCI) {
                DialogPlayEngine dlgUci = new DialogPlayEngine();
                boolean uciAccepted = dlgUci.show(gameModel.getStageRef(), gameModel.engines);
                if(uciAccepted) {
                    gameModel.wasSaved = false;
                    gameModel.currentPgnDatabaseIdx = -1;
                    gameModel.setComputerThinkTimeSecs(3);
                    Game g = new Game();
                    Board b;
                    if(dlgUci.startInitial) {
                        b = new Board(true);
                    } else {
                        b = gameModel.getGame().getCurrentNode().getBoard().makeCopy();
                    }
                    g.getRootNode().setBoard(b);
                    gameModel.setGame(g);
                    gameModel.getGame().setTreeWasChanged(true);
                    gameModel.getGame().setHeaderWasChanged(true);
                    gameModel.selectedPlayEngine = gameModel.engines.get(dlgUci.selectedIndex);
                    if(gameModel.selectedPlayEngine.supportsUciLimitStrength()) {
                        int newElo = (int) dlgUci.sliderStrength.getValue();
                        if (newElo >= gameModel.selectedPlayEngine.getMinUciElo()
                                && newElo <= gameModel.selectedPlayEngine.getMaxUciElo()) {
                            gameModel.selectedPlayEngine.setUciElo(newElo);
                        }
                    }
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
                    String formattedDate = LocalDate.now().format(formatter);
                    g.setHeader("Date", formattedDate);
                    g.setHeader("Event", "Training Game JFXChess");
                    if(dlgUci.playWhite) {
                        g.setHeader("White", "N.N.");
                        g.setHeader("Black", gameModel.selectedPlayEngine.getName());
                        g.setHeader("BlackElo", String.valueOf(gameModel.selectedPlayEngine.getUciElo()));
                        gameModel.setFlipBoard(false);
                        // itmPlayAsWhite.setSelected(true);
                        modeMenuController.activatePlayWhiteMode();
                    } else {
                        g.setHeader("Black", "N.N.");
                        g.setHeader("White", gameModel.selectedPlayEngine.getName());
                        g.setHeader("WhiteElo", String.valueOf(gameModel.selectedPlayEngine.getUciElo()));
                        gameModel.setFlipBoard(true);
                        // itmPlayAsBlack.setSelected(true);
                        modeMenuController.activatePlayBlackMode();
                    }
                }
            }
        }
    }


    private void handleFullGameAnalysis() {
        DialogGameAnalysis dlg = new DialogGameAnalysis();
        boolean accepted = dlg.show(gameModel.getStageRef(),
                gameModel.getGameAnalysisThinkTimeSecs(),
                ((double) gameModel.getGameAnalysisThreshold()));
        if(accepted) {
            itmEnterMoves.setSelected(true);
            tglEngineOnOff.setSelected(true);
            tglEngineOnOff.setText("On");

            if(dlg.rbBoth.isSelected()) {
                gameModel.setGameAnalysisForPlayer(GameModel.BOTH_PLAYERS);
            }
            if(dlg.rbWhite.isSelected()) {
                gameModel.setGameAnalysisForPlayer(CONSTANTS.IWHITE);
            }
            if(dlg.rbBlack.isSelected()) {
                gameModel.setGameAnalysisForPlayer(CONSTANTS.IBLACK);
            }
            gameModel.setGameAnalysisThinkTimeSecs(dlg.sSecs.getValue());
            gameModel.setGameAnalysisThreshold(dlg.sPawnThreshold.getValue());
            gameModel.setMode(GameModel.MODE_GAME_ANALYSIS);
            modeMenuController.activateGameAnalysisMode();
        }
    }

}
