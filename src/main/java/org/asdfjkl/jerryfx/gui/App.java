package org.asdfjkl.jerryfx.gui;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.asdfjkl.jerryfx.SystemInfo;

/**
 * JavaFX App
 */
public class App extends Application {

    @Override
    public void start(Stage stage) {

        // MENU
        MenuBar menuBar = new MenuBar();

        Menu fileMenu = new Menu("Game");
        Menu editMenu = new Menu("Edit");
        Menu modeMenu = new Menu("Mode");
        Menu databaseMenu = new Menu("Database");
        Menu helpMenu = new Menu("Help");

        // File Menu
        MenuItem newItem = new MenuItem("New...");
        MenuItem openFileItem = new MenuItem("Open File");
        MenuItem saveCurrentAsItem = new MenuItem("Save Current Game As");
        SeparatorMenuItem separatorFile1 = new SeparatorMenuItem();
        MenuItem printGameItem = new MenuItem("Print Game");
        MenuItem printPositionItem = new MenuItem("Print Game");
        MenuItem savePositionAsImageItem = new MenuItem("Save Position As Image");
        SeparatorMenuItem separatorFile2 = new SeparatorMenuItem();
        MenuItem quitItem = new MenuItem("Quit");

        fileMenu.getItems().addAll(newItem, openFileItem, saveCurrentAsItem,
                separatorFile1, printGameItem, printPositionItem, savePositionAsImageItem,
                separatorFile2, quitItem);

        // Edit Menu
        MenuItem copyGameItem = new MenuItem("Copy Game");
        MenuItem copyPositionItem = new MenuItem("Copy Position");
        MenuItem pasteItem = new MenuItem("Paste Game/Position");
        SeparatorMenuItem separatorEdit1 = new SeparatorMenuItem();
        MenuItem editGameItem = new MenuItem("Edit Game Data");
        MenuItem enterPositionItem = new MenuItem("Enter Position");
        SeparatorMenuItem separatorEdit2 = new SeparatorMenuItem();
        CheckMenuItem flipEvalItem = new CheckMenuItem("Flip Board");
        CheckMenuItem showSearchInfoItem = new CheckMenuItem("Show Search Info");
        MenuItem appearanceItem = new MenuItem("Appearance");
        MenuItem resetLayoutItem = new MenuItem("Reset Layout");

        editMenu.getItems().addAll(copyGameItem, copyPositionItem, pasteItem,
                separatorEdit1, editGameItem, enterPositionItem, separatorEdit2,
                flipEvalItem, showSearchInfoItem, appearanceItem, resetLayoutItem);

        // Mode Menu
        RadioMenuItem moveAnalysisItem = new RadioMenuItem("Analysis");
        RadioMenuItem playAsWhiteItem = new RadioMenuItem("Play as White");
        RadioMenuItem playAsBlackItem = new RadioMenuItem("Play as Black");
        RadioMenuItem enterMovesItem = new RadioMenuItem("Enter Moves");

        ToggleGroup toggleGroupMode = new ToggleGroup();
        toggleGroupMode.getToggles().add(moveAnalysisItem);
        toggleGroupMode.getToggles().add(playAsWhiteItem);
        toggleGroupMode.getToggles().add(playAsBlackItem);
        toggleGroupMode.getToggles().add(enterMovesItem);
        SeparatorMenuItem separatorMode1 = new SeparatorMenuItem();
        MenuItem fullGameAnalysisItem = new MenuItem("Full Game Analysis");
        MenuItem playoutPositionItem = new MenuItem("Play Out Position");
        SeparatorMenuItem separatorMode2 = new SeparatorMenuItem();
        MenuItem enginesItem = new MenuItem("Engines...");

        modeMenu.getItems().addAll(moveAnalysisItem, playAsWhiteItem, playAsBlackItem,
                enterMovesItem, separatorMode1, fullGameAnalysisItem, playoutPositionItem,
                separatorMode2, enginesItem);

        // Database Menu
        MenuItem browseDatabaseItem = new MenuItem("Browse Database");
        MenuItem nextGameItem = new MenuItem("Next Game");
        MenuItem previousGameItem = new MenuItem("Previous Game");

        databaseMenu.getItems().addAll(browseDatabaseItem, nextGameItem, previousGameItem);

        // Help Menu
        MenuItem aboutItem = new MenuItem("About");
        MenuItem jerryHomepageItem = new MenuItem("JerryFX - Homepage");

        helpMenu.getItems().addAll(aboutItem, jerryHomepageItem);

        menuBar.getMenus().addAll(fileMenu, editMenu, modeMenu, databaseMenu, helpMenu);

        // TOOLBAR
        ToolBar toolBar = new ToolBar();
        Button btnNew = new Button("New");
        Button btnOpen = new Button("Open");
        Button btnSaveAs = new Button("Save As");
        Button btnPrint = new Button("Print");
        Button btnFlipBoard = new Button("Flip Board");
        Button btnCopyGame = new Button("Copy Game");
        Button btnCopyPosition = new Button("Copy Position");
        Button btnPaste = new Button("Paste");
        Button btnEnterPosition = new Button("Enter Position");
        Button btnFullAnalysis = new Button("Full Analysis");
        Button btnBrowseGames = new Button("Browse Games");
        Button btnPrevGame = new Button("Prev. Game");
        Button btnNextGame = new Button("Next Game");
        Button btnAbout = new Button("About");

        toolBar.getItems().addAll(btnNew, btnOpen, btnSaveAs, btnPrint, new Separator(),
                btnFlipBoard, new Separator(),
                btnCopyGame, btnCopyPosition, btnPaste, btnEnterPosition, new Separator(),
                btnFullAnalysis, new Separator(),
                btnBrowseGames, btnPrevGame, btnNextGame, btnAbout);


        // Label & Edit Button for Game Data
        //Label gameDataLabel = new Label("Kasparov, G. (Wh) - Kaprov, A. (B)\nSevilla, XX.YY.1993");
        //gameDataLabel.setMinHeight();
        //gameDataLabel.setTextAlignment(TextAlignment.CENTER);
        Text gameDataLabel = new Text("Kasparov, G. (Wh) - Kaprov, A. (B)\nSevilla, XX.YY.1993");
        gameDataLabel.setTextAlignment(TextAlignment.CENTER);
        Button btnEditGameData = new Button("e");
        Region spacerGameDataLeft = new Region();
        Region spacerGameDataRight = new Region();

        HBox hboxLabelGameData = new HBox();
        hboxLabelGameData.getChildren().addAll(spacerGameDataLeft, gameDataLabel, spacerGameDataRight, btnEditGameData);
        hboxLabelGameData.setHgrow(spacerGameDataLeft, Priority.ALWAYS);
        hboxLabelGameData.setHgrow(spacerGameDataRight, Priority.ALWAYS);

        // Create a WebView
        WebView moveView = new WebView();
        moveView.resize(320,200);
        moveView.setMinWidth(1);
        moveView.setMaxWidth(Double.MAX_VALUE);
        WebEngine webEngine = moveView.getEngine();
        //webEngine.load("http://eclipse.com");
        StackPane stackPane = new StackPane();
        //stackPane.getChildren().add(moveView);

        // Navigation Buttons
        Button btnMoveBegin = new Button("|<-");
        Button btnMoveEnd = new Button("->|");
        Button btnMovePrev = new Button("<-");
        Button btnMoveNext = new Button("->");

        HBox hboxGameNavigation = new HBox();
        hboxGameNavigation.setAlignment(Pos.CENTER);
        hboxGameNavigation.getChildren().addAll(btnMoveBegin, btnMovePrev, btnMoveNext, btnMoveEnd);

        VBox vboxMainRight = new VBox();
        vboxMainRight.getChildren().addAll(hboxLabelGameData, moveView, hboxGameNavigation);
        //vboxMainRight.getChildren().addAll(moveView);

        // put together  Chessboard | Game Navigation
        Chessboard chessboard = new Chessboard();
        chessboard.resize(640,480);
        chessboard.updateCanvas();

        //HBox hboxMainLeftRight = new HBox();
        SplitPane hboxMainLeftRight = new SplitPane();
        hboxMainLeftRight.getItems().addAll(chessboard, vboxMainRight);
        hboxMainLeftRight.setDividerPosition(0, 0.5);
        //ddAll(chessboard, vboxMainRight);
        //hboxMainLeftRight.setHgrow(chessboard, Priority.ALWAYS);
        //hboxMainLeftRight.setHgrow(vboxMainRight, Priority.ALWAYS);


        //MenuItem copyItem = new MenuItem("Copy");
        //MenuItem pasteItem = new MenuItem("Paste");

        // Add menuItems to the Menus
        //fileMenu.getItems().addAll(newItem, openFileItem, exitItem);
        //editMenu.getItems().addAll(copyItem, pasteItem);

        // Add Menus to the MenuBar




        Canvas canvas = new Canvas(300, 250);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        drawShapes(gc);

        VBox vboxMain = new VBox();
        vboxMain.getChildren().addAll(menuBar, toolBar, hboxMainLeftRight);

        //vboxMain.setFillWidth(true);
        //vboxMain.getChildren().addAll(chessboard);
        //vboxMain.setVgrow(chessboard, Priority.ALWAYS);

        var scene = new Scene(vboxMain, 640, 480);
        chessboard.updateCanvas();

        stage.setScene(scene);
        stage.show();
    }

    private void drawShapes(GraphicsContext gc) {
        gc.setFill(Color.GREEN);
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(5);
        gc.strokeLine(40, 10, 10, 40);
        gc.fillOval(10, 60, 30, 30);
        gc.strokeOval(60, 60, 30, 30);
        gc.fillRoundRect(110, 60, 30, 30, 10, 10);
        gc.strokeRoundRect(160, 60, 30, 30, 10, 10);
        gc.fillArc(10, 110, 30, 30, 45, 240, ArcType.OPEN);
        gc.fillArc(60, 110, 30, 30, 45, 240, ArcType.CHORD);
        gc.fillArc(110, 110, 30, 30, 45, 240, ArcType.ROUND);
        gc.strokeArc(10, 160, 30, 30, 45, 240, ArcType.OPEN);
        gc.strokeArc(60, 160, 30, 30, 45, 240, ArcType.CHORD);
        gc.strokeArc(110, 160, 30, 30, 45, 240, ArcType.ROUND);
        gc.fillPolygon(new double[]{10, 40, 10, 40},
                new double[]{210, 210, 240, 240}, 4);
        gc.strokePolygon(new double[]{60, 90, 60, 90},
                new double[]{210, 210, 240, 240}, 4);
        gc.strokePolyline(new double[]{110, 140, 110, 140},
                new double[]{210, 210, 240, 240}, 4);
    }

    public static void main(String[] args) {
        launch();
    }

}