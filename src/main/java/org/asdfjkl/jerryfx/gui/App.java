package org.asdfjkl.jerryfx.gui;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.SVGUniverse;
import com.sun.javafx.application.HostServicesDelegate;
import com.sun.javafx.application.LauncherImpl;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.application.Preloader;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import jfxtras.styles.jmetro.JMetro;
import javafx.scene.image.ImageView;
import org.asdfjkl.jerryfx.SystemInfo;
import org.asdfjkl.jerryfx.lib.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;

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

    @Override
    public void start(Stage stage) {

        //TestCases tests = new TestCases();
        //tests.testPolyglot();
        //tests.readGamesByStringTest();

        gameModel = new GameModel();
        gameModel.restoreModel();
        ScreenGeometry screenGeometry = gameModel.restoreScreenGeometry();
        gameModel.getGame().setTreeWasChanged(true);

        // MENU
        MenuBar mnuBar = new MenuBar();

        Menu mnuFile = new Menu("Game");
        Menu mnuEdit = new Menu("Edit");
        Menu mnuMode = new Menu("Mode");
        Menu mnuDatabase = new Menu("Database");
        Menu mnuHelp = new Menu("Help");

        // File Menu
        MenuItem itmNew = new MenuItem("New...");
        MenuItem itmOpenFile = new MenuItem("Open File");
        MenuItem itmSaveCurrentGameAs = new MenuItem("Save Current Game As");
        MenuItem itmPrintGame = new MenuItem("Print Game");
        MenuItem itmPrintPosition = new MenuItem("Print Position");
        MenuItem itmSavePositionAsImage = new MenuItem("Save Position As Image");
        MenuItem itmQuit = new MenuItem("Quit");

        mnuFile.getItems().addAll(itmNew, itmOpenFile, itmSaveCurrentGameAs,
                new SeparatorMenuItem(), itmPrintGame, itmPrintPosition, itmSavePositionAsImage,
                new SeparatorMenuItem(), itmQuit);

        // Edit Menu
        MenuItem itmCopyGame = new MenuItem("Copy Game");
        MenuItem itmCopyPosition = new MenuItem("Copy Position");
        MenuItem itmPaste = new MenuItem("Paste Game/Position");
        MenuItem itmEditGame = new MenuItem("Edit Game Data");
        MenuItem itmEnterPosition = new MenuItem("Enter Position");
        MenuItem itmFlipBoard = new MenuItem("Flip Board");
        MenuItem itmShowSearchInfo = new MenuItem("Show/Hide Search Info");
        MenuItem itmAppearance = new MenuItem("Appearance");
        MenuItem itmResetLayout = new MenuItem("Reset Layout");

        mnuEdit.getItems().addAll(itmCopyGame, itmCopyPosition, itmPaste,
                new SeparatorMenuItem(), itmEditGame, itmEnterPosition,
                new SeparatorMenuItem(), itmFlipBoard, itmShowSearchInfo,
                new SeparatorMenuItem(), itmAppearance, itmResetLayout);

        // Mode Menu
        RadioMenuItem itmAnalysis = new RadioMenuItem("Analysis");
        RadioMenuItem itmPlayAsWhite = new RadioMenuItem("Play as White");
        RadioMenuItem itmPlayAsBlack = new RadioMenuItem("Play as Black");
        RadioMenuItem itmEnterMoves = new RadioMenuItem("Enter Moves");

        RadioMenuItem itmFullGameAnalysis = new RadioMenuItem("Full Game Analysis");
        RadioMenuItem itmPlayoutPosition = new RadioMenuItem("Play Out Position");

        ToggleGroup tglMode = new ToggleGroup();
        tglMode.getToggles().add(itmAnalysis);
        tglMode.getToggles().add(itmPlayAsWhite);
        tglMode.getToggles().add(itmPlayAsBlack);
        tglMode.getToggles().add(itmEnterMoves);
        tglMode.getToggles().add(itmFullGameAnalysis);
        tglMode.getToggles().add(itmPlayoutPosition);

        MenuItem itmEngines = new MenuItem("Engines...");

        mnuMode.getItems().addAll(itmAnalysis, itmPlayAsWhite, itmPlayAsBlack,
                itmEnterMoves, itmFullGameAnalysis, itmPlayoutPosition,
                new SeparatorMenuItem(), itmEngines);

        // Database Menu
        MenuItem itmBrowseDatabase = new MenuItem("Browse Database");
        MenuItem itmNextGame = new MenuItem("Next Game");
        MenuItem itmPreviousGame = new MenuItem("Previous Game");

        mnuDatabase.getItems().addAll(itmBrowseDatabase, itmNextGame, itmPreviousGame);

        // Help Menu
        MenuItem itmAbout = new MenuItem("About");
        MenuItem itmJerryHomepage = new MenuItem("JerryFX - Homepage");

        mnuHelp.getItems().addAll(itmAbout, itmJerryHomepage);

        mnuBar.getMenus().addAll(mnuFile, mnuEdit, mnuMode, mnuDatabase, mnuHelp);

        // TOOLBAR
        ToolBar tbMainWindow = new ToolBar();
        Button btnNew = new Button("New");
        btnNew.setGraphic(new ImageView( new Image("icons/document-new.png")));
        btnNew.setContentDisplay(ContentDisplay.TOP);

        Button btnOpen = new Button("Open");
        btnOpen.setGraphic(new ImageView( new Image("icons/document-open.png")));
        btnOpen.setContentDisplay(ContentDisplay.TOP);

        Button btnSaveAs = new Button("Save As");
        btnSaveAs.setGraphic(new ImageView( new Image("icons/document-save.png")));
        btnSaveAs.setContentDisplay(ContentDisplay.TOP);

        Button btnPrint = new Button("Print");
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
        btnPrevGame.setGraphic(new ImageView( new Image("icons/go-previous.png")));
        btnPrevGame.setContentDisplay(ContentDisplay.TOP);

        Button btnNextGame = new Button("Next Game");
        btnNextGame.setGraphic(new ImageView( new Image("icons/go-next.png")));
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
        //txtGameData = new Text("Kasparov, G. (Wh) - Kaprov, A. (B)\nSevilla, XX.YY.1993");
        txtGameData = new Text("");
        txtGameData.setTextAlignment(TextAlignment.CENTER);
        Button btnEditGameData = new Button();
        btnEditGameData.setGraphic(new ImageView( new Image("icons/document_properties_small.png")));
        Region spcrGameDataLeft = new Region();
        Region spcrGameDataRight = new Region();

        HBox hbGameData = new HBox();
        hbGameData.getChildren().addAll(spcrGameDataLeft, txtGameData, spcrGameDataRight, btnEditGameData);
        hbGameData.setHgrow(spcrGameDataLeft, Priority.ALWAYS);
        hbGameData.setHgrow(spcrGameDataRight, Priority.ALWAYS);

        // Create a WebView
        /*
        WebView viewMoves = new WebView();
        viewMoves.resize(320,200);
        viewMoves.setMinWidth(1);
        viewMoves.setMaxWidth(Double.MAX_VALUE);
        viewMoves.setMaxHeight(Double.MAX_VALUE);
         */
        MoveView moveView = new MoveView(gameModel);

        // Navigation Buttons
        Button btnMoveBegin = new Button();
        btnMoveBegin.setGraphic(new ImageView( new Image("icons/ic_first_page_black.png")));
        Button btnMoveEnd = new Button();
        btnMoveEnd.setGraphic(new ImageView( new Image("icons/ic_last_page_black.png")));
        Button btnMovePrev = new Button();
        btnMovePrev.setGraphic(new ImageView( new Image("icons/ic_chevron_left_black.png")));
        Button btnMoveNext = new Button();
        btnMoveNext.setGraphic(new ImageView( new Image("icons/ic_chevron_right_black.png")));

        HBox hbGameNavigation = new HBox();
        hbGameNavigation.setAlignment(Pos.CENTER);
        hbGameNavigation.getChildren().addAll(btnMoveBegin, btnMovePrev, btnMoveNext, btnMoveEnd);

        VBox vbGameDataMovesNavigation = new VBox();
        vbGameDataMovesNavigation.getChildren().addAll(hbGameData, moveView.getWebView(), hbGameNavigation);
        vbGameDataMovesNavigation.setVgrow(moveView.getWebView(), Priority.ALWAYS);

        // put together  Chessboard | Game Navigation
        Chessboard chessboard = new Chessboard(gameModel);
        chessboard.resize(100,100);
        chessboard.updateCanvas();

        spChessboardMoves = new SplitPane();
        spChessboardMoves.getItems().addAll(chessboard, vbGameDataMovesNavigation);
        spChessboardMoves.setMaxHeight(Double.MAX_VALUE);

        // Buttons for Engine On/Off and TextFlow for Engine Output
        tglEngineOnOff = new ToggleButton("Off");
        Label lblMultiPV = new Label("Lines:");
        ComboBox<Integer> cmbMultiPV = new ComboBox<Integer>();
        cmbMultiPV.getItems().addAll(1,2,3,4);
        cmbMultiPV.setValue(1);
        Button btnSelectEngine = new Button();
        btnSelectEngine.setGraphic(new ImageView( new Image("icons/document_properties_small.png")));
        HBox hbEngineControl = new HBox();
        Region spcrEngineControl = new Region();
        hbEngineControl.getChildren().addAll(tglEngineOnOff, lblMultiPV,
                cmbMultiPV, spcrEngineControl, btnSelectEngine);
        hbEngineControl.setAlignment(Pos.CENTER);
        hbEngineControl.setMargin(lblMultiPV, new Insets(0,5,0,10));
        hbEngineControl.setHgrow(spcrEngineControl, Priority.ALWAYS);
        TextFlow txtEngineOut = new TextFlow();
        txtEngineOut.setPadding(new Insets(10,10,10,10));
        VBox vbBottom = new VBox();
        vbBottom.getChildren().addAll(hbEngineControl, txtEngineOut);
        vbBottom.setMinHeight(10);

        // put everything excl. the bottom Engine part into one VBox
        VBox vbMainUpperPart = new VBox();
        vbMainUpperPart.getChildren().addAll(mnuBar, tbMainWindow, spChessboardMoves);
        vbMainUpperPart.setVgrow(spChessboardMoves, Priority.ALWAYS);

        // add another split pane for main window part and engine output
        spMain = new SplitPane();
        spMain.setOrientation(Orientation.VERTICAL);
        spMain.getItems().addAll(vbMainUpperPart, vbBottom);

        GameMenuController gameMenuController = new GameMenuController(gameModel);

        // events
        gameModel.addListener(chessboard);
        gameModel.addListener(moveView);
        gameModel.addListener(this);

        itmOpenFile.setOnAction(actionEvent -> { gameMenuController.handleOpenGame(); } );
        btnOpen.setOnAction(actionEvent -> { gameMenuController.handleOpenGame(); } );

        itmSavePositionAsImage.setOnAction(actionEvent -> { gameMenuController.handleSaveBoardPicture(chessboard);});

        btnMoveNext.setOnAction(event -> moveView.goForward());
        btnMoveBegin.setOnAction(event -> moveView.seekToRoot());
        btnMovePrev.setOnAction(event -> moveView.goBack());
        btnMoveEnd.setOnAction(event -> moveView.seekToEnd());

        // connect mode controller
        engineOutputView = new EngineOutputView(txtEngineOut);
        ModeMenuController modeMenuController = new ModeMenuController(gameModel, engineOutputView);
        EngineController engineController = new EngineController(modeMenuController);
        modeMenuController.setEngineController(engineController);

        EditMenuController editMenuController = new EditMenuController(gameModel);

        gameModel.addListener(modeMenuController);
        modeMenuController.activateEnterMovesMode();

        itmSaveCurrentGameAs.setOnAction(e -> {
           gameMenuController.handleSaveCurrentGame();
        });

        itmPrintGame.setOnAction(actionEvent -> {
            gameMenuController.handlePrintGame(stage);
        });

        itmPrintPosition.setOnAction(actionEvent -> {
            gameMenuController.handlePrintPosition(stage);
        });

        itmPlayAsWhite.setOnAction(actionEvent -> {
            if(itmPlayAsWhite.isSelected()) {
                if(gameModel.getMode() != GameModel.MODE_PLAY_WHITE) {
                    itmPlayAsWhite.setSelected(true);
                    tglEngineOnOff.setSelected(true);
                    tglEngineOnOff.setText("On");
                    modeMenuController.activatePlayWhiteMode();
                }
            }
        });

        itmPlayAsBlack.setOnAction(actionEvent -> {
            if(itmPlayAsBlack.isSelected()) {
                if(gameModel.getMode() != GameModel.MODE_PLAY_BLACK) {
                    itmPlayAsBlack.setSelected(true);
                    tglEngineOnOff.setSelected(true);
                    tglEngineOnOff.setText("On");
                    modeMenuController.activatePlayBlackMode();
                }
            }
        });

        itmPlayoutPosition.setOnAction(actionEvent -> {
            if(gameModel.getMode() != GameModel.MODE_PLAYOUT_POSITION) {
                itmPlayoutPosition.setSelected(true);
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
            } else {
                /*
                itmEnterMoves.setSelected(true);
                tglEngineOnOff.setSelected(false);
                tglEngineOnOff.setText("Off");
                modeMenuController.activateEnterMovesMode();

                 */
            }
        });

        itmEnterMoves.setOnAction(actionEvent -> {
            tglEngineOnOff.setSelected(false);
            tglEngineOnOff.setText("Off");
            modeMenuController.activateEnterMovesMode();
        });

        itmFullGameAnalysis.setOnAction(actionEvent -> {
            DialogGameAnalysis dlg = new DialogGameAnalysis();
            boolean accepted = dlg.show(3, 0.5);
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
                gameModel.setGameAnalysisThinkTime(dlg.sSecs.getValue());
                gameModel.setGameAnalysisThreshold((int) (dlg.sPawnThreshold.getValue()*100.0));
                gameModel.setMode(GameModel.MODE_GAME_ANALYSIS);
                modeMenuController.activateGameAnalysisMode();
            }
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

        cmbMultiPV.setOnAction(actionEvent -> {
            int multiPv = (Integer) cmbMultiPV.getValue();
            if(multiPv != gameModel.getMultiPv()) {
                gameModel.setMultiPv(multiPv);
                engineController.sendCommand("setoption name MultiPV value "+multiPv);
                gameModel.triggerStateChange();
            }
        });

        itmNew.setOnAction(e -> {
            DialogNewGame dlg = new DialogNewGame();
            boolean accepted = dlg.show(gameModel.activeEngine.isInternal(),
                    gameModel.getEngineStrength(),
                    gameModel.getComputerThinkTimeSecs());
            if(accepted) {
                gameModel.wasSaved = false;
                gameModel.setComputerThinkTimeSecs(dlg.thinkTime);
                gameModel.setEngineStrength(dlg.strength);
                Game g = new Game();
                g.getRootNode().setBoard(new Board(true));
                gameModel.setGame(g);
                gameModel.getGame().setTreeWasChanged(true);
                if(dlg.rbWhite.isSelected()) {
                    gameModel.setFlipBoard(false);
                } else {
                    gameModel.setFlipBoard(true);
                }
                if(dlg.rbComputer.isSelected()) {
                    if(dlg.rbWhite.isSelected()) {
                        modeMenuController.activatePlayWhiteMode();
                    } else {
                        modeMenuController.activatePlayBlackMode();
                    }
                } else {
                    modeMenuController.activateEnterMovesMode();
                }
            }
        });

        itmQuit.setOnAction(event -> {
            onExit(stage);
        });

        itmEditGame.setOnAction(e -> {
            DialogEditGameData dlg = new DialogEditGameData();
            boolean accteped = dlg.show(gameModel.getGame().getPgnHeaders(), gameModel.getGame().getResult());
            if(accteped) {
                for (Map.Entry<String, String> entry : dlg.pgnHeaders.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    gameModel.getGame().setHeader(key, value);
                }
                gameModel.getGame().setResult(dlg.gameResult);
                gameModel.triggerStateChange();
            }
        });

        itmEnterPosition.setOnAction(e -> {

            Board board = gameModel.getGame().getCurrentNode().getBoard();
            DialogEnterPosition dlg = new DialogEnterPosition();
            double height = Math.max(stage.getHeight() * 0.6, 520);
            double width = height * 1.6;
            boolean accepted = dlg.show(board, width, height);
            if(accepted) {
                Board newBoard = dlg.currentBoard;
                if(newBoard.isConsistent()) {
                    Game g = new Game();
                    g.getRootNode().setBoard(newBoard);
                    gameModel.setGame(g);
                    gameModel.getGame().setTreeWasChanged(true);
                    gameModel.triggerStateChange();
                }
            }
        });

        itmCopyGame.setOnAction(e -> {
            editMenuController.copyGame();
        });

        itmCopyPosition.setOnAction(e -> {
            editMenuController.copyPosition();
        });

        itmPaste.setOnAction(e -> {
            String pasteString = Clipboard.getSystemClipboard().getString();
            editMenuController.paste(pasteString);
        });

        itmResetLayout.setOnAction(e -> {
            Rectangle2D screenBounds = Screen.getPrimary().getBounds();
            stage.setWidth(screenBounds.getWidth() * ScreenGeometry.DEFAULT_WIDTH_RATIO);
            stage.setHeight(screenBounds.getHeight() * ScreenGeometry.DEFAULT_HEIGHT_RATIO);
            spChessboardMoves.setDividerPosition(0, screenGeometry.DEFAULT_MOVE_DIVIDER_RATIO);
            spMain.setDividerPosition(0, screenGeometry.DEFAULT_MAIN_DIVIDER_RATIO);
            stage.centerOnScreen();
            gameModel.triggerStateChange();
        });

        itmFlipBoard.setOnAction(e -> {
            gameModel.setFlipBoard(!gameModel.getFlipBoard());
            gameModel.triggerStateChange();
        });

        itmShowSearchInfo.setOnAction(e -> {
            if(engineOutputView.isEnabled()) {
                engineOutputView.disableOutput();
            } else {
                engineOutputView.enableOutput();
            }
            gameModel.triggerStateChange();
        });

        itmAbout.setOnAction(event -> {
            DialogAbout.show();
        });

        itmJerryHomepage.setOnAction(event -> {
            getHostServices().showDocument("https://github.com/asdfjkl/jerry");
        });

        stage.setOnCloseRequest(event -> {
            event.consume();
            onExit(stage);
        });


        /*
        PgnReader reader = new PgnReader();
        try {
            OptimizedRandomAccessFile raf = new OptimizedRandomAccessFile("C:\\Users\\user\\MyFiles\\workspace\\test_databases\\middleg.pgn", "r");
            raf.seek(0);
            Game g = reader.readGame(raf);
            gameModel.game = g;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }*/


        Scene scene = new Scene(spMain);

        scene.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
            if (event.getCode() == KeyCode.RIGHT) {
                moveView.goForward();
            }
            if (event.getCode() == KeyCode.LEFT) {
                moveView.goBack();
            }
            if (event.getCode() == KeyCode.HOME) {
                moveView.seekToRoot();
            }
            if (event.getCode() == KeyCode.END) {
                moveView.seekToEnd();
            }
            event.consume();
        });

        itmEnterMoves.setSelected(true);


        JMetro jMetro = new JMetro();
        jMetro.setScene(scene);

        SplashScreen splash = SplashScreen.getSplashScreen();

        if (splash != null && splash.isVisible()) {
            System.out.println("Is visible");

            splash.close();
        }

        stage.setScene(scene);
        // get primary screen bounds and set these to scene or
        // restore previously stet screen geometry
        Rectangle2D screenBounds = Screen.getPrimary().getBounds();
        if(screenGeometry.isValid()) {
            System.out.println(screenGeometry);
            stage.setX(screenGeometry.xOffset);
            stage.setY(screenGeometry.yOffset);
            stage.setWidth(screenGeometry.width);
            stage.setHeight(screenGeometry.height);
        } else {
            //stage.setX(screenBounds.);
            //stage.setY(screenGeometry.yOffset);
            System.out.println("screen bounds: "+screenBounds.getWidth());
            stage.setWidth(screenBounds.getWidth() * ScreenGeometry.DEFAULT_WIDTH_RATIO);
            stage.setHeight(screenBounds.getHeight() * ScreenGeometry.DEFAULT_HEIGHT_RATIO);
        }
        spChessboardMoves.setDividerPosition(0, screenGeometry.moveDividerRatio);
        spMain.setDividerPosition(0, screenGeometry.mainDividerRatio);

        gameModel.triggerStateChange();

        stage.show();

    }

    @Override
    public void stateChange() {

        if(gameModel.getGame().isTreeChanged()) {
            //txtGameData = new Text("Kasparov, G. (Wh) - Kaprov, A. (B)\nSevilla, XX.YY.1993");
            String white = gameModel.getGame().getHeader("White");
            String black = gameModel.getGame().getHeader("Black");
            String site = gameModel.getGame().getHeader("Site");
            String date = gameModel.getGame().getHeader("Date");

            String label = white + " - " + black + "\n" + site + ", " + date;
            txtGameData.setText(label);
        }
        if(gameModel.getMode() == GameModel.MODE_ENTER_MOVES) {
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

    private void onExit(Stage stage) {

        gameModel.saveModel();
        ScreenGeometry g = new ScreenGeometry(stage.getX(),
                stage.getY(), stage.getWidth(), stage.getHeight(),
                spChessboardMoves.getDividerPositions()[0],
                spMain.getDividerPositions()[0]);
        gameModel.saveScreenGeometry(g);

        stage.close();
    }

}