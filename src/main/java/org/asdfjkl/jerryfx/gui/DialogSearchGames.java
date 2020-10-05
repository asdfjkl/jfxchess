package org.asdfjkl.jerryfx.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.JMetroStyleClass;
import jfxtras.styles.jmetro.Style;
import org.asdfjkl.jerryfx.lib.Board;

import java.util.function.DoubleFunction;

public class DialogSearchGames {

    Stage stage;
    boolean accepted = false;

    CheckBox searchHeader;
    CheckBox searchPosition;

    EnterPosBoard enterPosBoard;
    Button btnFlipBoard = new Button("Flip Board");
    Button btnInitialPosition = new Button("Initial Position");
    Button btnClearBoard = new Button("Clear Board");
    Button btnCurrentPosition = new Button("Current Position");
    Spinner<Integer> spinnerAfter;
    Spinner<Integer> spinnerBefore;
    TextField txtWhite;
    TextField txtBlack;
    CheckBox ignoreCbColors;
    TextField txtEvent;
    TextField txtSite;
    CheckBox cbYear;
    Spinner<Integer> spinnerMinYear;
    Spinner<Integer> spinnerMaxYear;
    CheckBox cbECO;
    TextField ecoMin;
    TextField ecoMax;
    Spinner<Integer> spinnerMinElo;
    Spinner<Integer> spinnerMaxElo;
    RadioButton rbEloIgnore;
    RadioButton rbEloOne;
    RadioButton rbEloBoth;
    RadioButton rbEloAverage;
    CheckBox cbResultWhiteWins;
    CheckBox cbResultBlackWins;
    CheckBox cbResultUnclear;
    CheckBox cbResultDraw;

    Board currentBoard;

    public boolean show(Board board, SearchPattern searchPattern) {

        this.currentBoard = board.makeCopy();

        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        Button btnOk = new Button();
        btnOk.setText("OK");

        Button btnCancel = new Button();
        btnCancel.setText("Cancel");
        Region spacer = new Region();

        HBox hbButtons = new HBox();
        hbButtons.getChildren().addAll(spacer, btnOk, btnCancel);
        hbButtons.setHgrow(spacer, Priority.ALWAYS);
        hbButtons.setSpacing(10);

        btnOk.setOnAction(e -> {
            btnOkClicked();
        });

        btnCancel.setOnAction(e -> {
            btnCancelClicked();
        });

        searchHeader = new CheckBox("Search Game Data");
        searchPosition = new CheckBox("Search Position");

        HBox hbCheckboxes = new HBox();
        hbCheckboxes.getChildren().addAll(searchHeader, searchPosition);
        hbCheckboxes.setSpacing(10);

        /*
         POSITION SEARCH TAB
         */
        HBox hbLblAfter = new HBox();
        HBox hbLblBefore = new HBox();
        Label lblAfter = new Label("After Move:");
        lblAfter.setMinWidth(80);
        Label lblBefore = new Label("Before Move:");
        lblBefore.setMinWidth(80);
        spinnerAfter = new Spinner<Integer>();
        SpinnerValueFactory<Integer> valueFactoryAfter = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1);
        spinnerAfter.setValueFactory(valueFactoryAfter);

        spinnerBefore = new Spinner<Integer>();
        SpinnerValueFactory<Integer> valueFactoryBefore = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 40);
        spinnerBefore.setValueFactory(valueFactoryBefore);

        hbLblAfter.getChildren().addAll(lblAfter, spinnerAfter);
        hbLblAfter.setAlignment(Pos.CENTER);
        hbLblBefore.getChildren().addAll(lblBefore, spinnerBefore);
        hbLblBefore.setAlignment(Pos.CENTER);

        VBox vbButtonsRight = new VBox();
        vbButtonsRight.setPrefWidth(140);
        Region spacer1 = new Region();
        Region spacer2 = new Region();
        btnFlipBoard.setMinWidth(vbButtonsRight.getPrefWidth());
        btnInitialPosition.setMinWidth(vbButtonsRight.getPrefWidth());
        btnClearBoard.setMinWidth(vbButtonsRight.getPrefWidth());
        btnCurrentPosition.setMinWidth(vbButtonsRight.getPrefWidth());

        btnFlipBoard.setOnAction(e -> {
            enterPosBoard.flipBoard = !enterPosBoard.flipBoard;
            enterPosBoard.updateCanvas();
        });

        btnCurrentPosition.setOnAction(e -> {
            btnCurrentPositionClicked();
        });

        btnInitialPosition.setOnAction(e -> {
            btnInitialPositionClicked();
        });

        btnClearBoard.setOnAction(e -> {
            btnClearBoardClicked();
        });

        vbButtonsRight.getChildren().addAll(hbLblAfter, hbLblBefore, spacer1,
                btnFlipBoard,
                spacer2,
                btnInitialPosition, btnClearBoard, btnCurrentPosition);
        vbButtonsRight.setSpacing(10);
        vbButtonsRight.setVgrow(spacer1, Priority.ALWAYS);
        vbButtonsRight.setPadding( new Insets(10,0,10,30));

        enterPosBoard = new EnterPosBoard(new Board(true));
        // to make sure, the enter position board is displayed correctly
        enterPosBoard.setHeight(350);

        HBox hbTabPosition = new HBox();
        hbTabPosition.getChildren().addAll(enterPosBoard, vbButtonsRight);
        hbTabPosition.setHgrow(enterPosBoard, Priority.ALWAYS);


        /*
         HEADER SEARCH TAB
         */
        GridPane gridHeaderSearch = new GridPane();
        Label lblWhite = new Label("White:");
        txtWhite = new TextField();
        Label lblBlack = new Label("Black:");
        txtBlack = new TextField();
        ignoreCbColors = new CheckBox("Ignore Colors");

        Label lblEvent = new Label("Event:");
        txtEvent = new TextField();
        Label lblSite = new Label("Site");
        txtSite = new TextField();

        cbYear = new CheckBox("Year:");
        spinnerMinYear = new Spinner<Integer>();
        SpinnerValueFactory<Integer> spinnerMinYearVF = new SpinnerValueFactory.IntegerSpinnerValueFactory(500, 2100, 500);
        spinnerMinYear.setValueFactory(spinnerMinYearVF);
        spinnerMaxYear = new Spinner<Integer>();
        SpinnerValueFactory<Integer> spinnerMaxYearVF = new SpinnerValueFactory.IntegerSpinnerValueFactory(500, 2100, 2100);
        spinnerMaxYear.setValueFactory(spinnerMaxYearVF);
        cbECO = new CheckBox("ECO:");
        ecoMin = new TextField("A00");
        ecoMax = new TextField("E99");

        GridPane gridLeft = new GridPane();
        gridLeft.setHgap(10);
        gridLeft.setVgap(20);
        gridLeft.setAlignment(Pos.CENTER);
        gridLeft.add(lblWhite, 0,0);
        gridLeft.add(txtWhite, 1, 0);
        gridLeft.add(lblBlack, 0, 1);
        gridLeft.add(txtBlack, 1, 1);
        gridLeft.add(ignoreCbColors, 1, 2);
        gridLeft.add(lblEvent, 0, 3);
        gridLeft.add(txtEvent, 1, 3);
        gridLeft.add(lblSite, 0, 4);
        gridLeft.add(txtSite, 1, 4);
        gridLeft.add(cbYear, 0, 5);
        gridLeft.add(cbECO, 0, 6);
        HBox hbYear = new HBox();
        hbYear.getChildren().addAll(spinnerMinYear, new Label("-"), spinnerMaxYear);
        hbYear.setSpacing(10);
        gridLeft.add(hbYear, 1, 5);
        HBox hbEco = new HBox();
        hbEco.getChildren().addAll(ecoMin, ecoMax);
        hbEco.setSpacing(10);
        gridLeft.add(hbEco, 1 , 6);

        spinnerMinElo = new Spinner<Integer>();
        spinnerMinElo.setMaxWidth(80);
        spinnerMaxElo = new Spinner<Integer>();
        spinnerMaxElo.setMaxWidth(80);
        SpinnerValueFactory<Integer> spinnerMinEloVF = new SpinnerValueFactory.IntegerSpinnerValueFactory(1000, 3000, 1000);
        spinnerMinElo.setValueFactory(spinnerMinEloVF);
        SpinnerValueFactory<Integer> spinnerMaxEloVF = new SpinnerValueFactory.IntegerSpinnerValueFactory(1000, 3000, 3000);
        spinnerMaxElo.setValueFactory(spinnerMaxEloVF);

        rbEloIgnore = new RadioButton("Ignore");
        rbEloOne = new RadioButton("One");
        rbEloBoth = new RadioButton("Both");
        rbEloAverage = new RadioButton("Average");
        ToggleGroup tgElo = new ToggleGroup();
        rbEloIgnore.setToggleGroup(tgElo);
        rbEloOne.setToggleGroup(tgElo);
        rbEloBoth.setToggleGroup(tgElo);
        rbEloAverage.setToggleGroup(tgElo);

        GridPane gridElo = new GridPane();
        gridElo.setHgap(20);
        gridElo.setVgap(10);
        HBox hbMinMaxElo = new HBox();
        hbMinMaxElo.getChildren().addAll(spinnerMinElo, new Label("-"), spinnerMaxElo);
        hbMinMaxElo.setAlignment(Pos.CENTER);
        hbMinMaxElo.setSpacing(10);

        gridElo.add(hbMinMaxElo, 0,0,2, 1);
        gridElo.add(rbEloIgnore, 0, 1);
        gridElo.add(rbEloOne, 1, 1);
        gridElo.add(rbEloBoth, 0, 2);
        gridElo.add(rbEloAverage, 1, 2);

        TitledPane paneElo = new TitledPane("Elo", gridElo);
        paneElo.setCollapsible(false);

        cbResultWhiteWins = new CheckBox("1-0");
        cbResultBlackWins = new CheckBox("0-1");
        cbResultUnclear = new CheckBox("*");
        cbResultDraw = new CheckBox("1/2-1/2");
        GridPane gridWin = new GridPane();
        gridWin.setHgap(20);
        gridWin.setVgap(10);
        gridWin.add(cbResultWhiteWins,0,0);
        gridWin.add(cbResultBlackWins, 1, 0);
        gridWin.add(cbResultUnclear, 0, 1);
        gridWin.add(cbResultDraw, 1, 1);

        TitledPane paneWin = new TitledPane("Result", gridWin);
        paneWin.setCollapsible(false);

        Button resetHeaderSearch = new Button("Reset");
        HBox hbButtonReset = new HBox();
        Region spacerResetButton = new Region();
        hbButtonReset.getChildren().addAll(spacerResetButton, resetHeaderSearch);
        hbButtonReset.setHgrow(spacerResetButton, Priority.ALWAYS);

        VBox vbEloResult = new VBox();
        Region spacerRight = new Region();
        vbEloResult.getChildren().addAll(paneElo, paneWin, spacerRight, hbButtonReset);
        vbEloResult.setSpacing(10);
        vbEloResult.setPadding( new Insets(30, 0, 0, 0));
        vbEloResult.setVgrow(spacerRight, Priority.ALWAYS);

        HBox hBoxGameData = new HBox();
        hBoxGameData.getChildren().addAll(gridLeft, vbEloResult);
        hBoxGameData.setSpacing(30);

        TabPane tabPane = new TabPane();
        Tab tabHeader = new Tab("GAME DATA", hBoxGameData);
        Tab tabPosition = new Tab("POSITION", hbTabPosition);
        tabPane.getTabs().addAll(tabHeader, tabPosition);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getStyleClass().add("underlined");

        VBox vbox = new VBox();
        vbox.getChildren().addAll(tabPane, hbCheckboxes, hbButtons);
        vbox.setVgrow(tabPane, Priority.ALWAYS);
        vbox.setSpacing(10);
        vbox.setPadding( new Insets(15, 25, 10, 25));

        recoverFromSearchPattern(searchPattern);

        Scene scene = new Scene(vbox);

        JMetro jMetro = new JMetro();
        jMetro.setScene(scene);
        stage.setScene(scene);
        stage.getIcons().add(new Image("icons/app_icon.png"));

        stage.showAndWait();

        return accepted;
    }

    private void btnOkClicked() {
        accepted = true;
        stage.close();
    }

    private void btnCancelClicked() {
        accepted = false;
        stage.close();
    }


    private void recoverFromSearchPattern(SearchPattern searchPattern) {

        spinnerAfter.getValueFactory().setValue(searchPattern.getMinMove());
        spinnerBefore.getValueFactory().setValue(searchPattern.getMaxMove());
        txtWhite.setText(searchPattern.getWhiteName());
        txtBlack.setText(searchPattern.getBlackName());
        ignoreCbColors.setSelected(searchPattern.isIgnoreNameColor());
        txtEvent.setText(searchPattern.getEvent());
        txtSite.setText(searchPattern.getSite());
        cbYear.setSelected(searchPattern.isCheckYear());
        spinnerMinYear.getValueFactory().setValue(searchPattern.getMinYear());
        spinnerMaxYear.getValueFactory().setValue(searchPattern.getMaxYear());
        cbECO.setSelected(searchPattern.isCheckEco());
        ecoMin.setText(searchPattern.getEcoStart());
        ecoMax.setText(searchPattern.getEcoStop());
        spinnerMinElo.getValueFactory().setValue(searchPattern.getMinElo());
        spinnerMaxElo.getValueFactory().setValue(searchPattern.getMaxElo());
        if(searchPattern.getCheckElo() == SearchPattern.SEARCH_IGNORE_ELO) {
            rbEloIgnore.setSelected(true);
        }
        if(searchPattern.getCheckElo() == SearchPattern.SEARCH_BOTH_ELO) {
            rbEloBoth.setSelected(true);
        }
        if(searchPattern.getCheckElo() == SearchPattern.SEARCH_AVG_ELO) {
            rbEloAverage.setSelected(true);
        }
        if(searchPattern.getCheckElo() == SearchPattern.SEARCH_ONE_ELO) {
            rbEloOne.setSelected(true);
        }
        cbResultWhiteWins.setSelected(searchPattern.isResultWhiteWins());
        cbResultBlackWins.setSelected(searchPattern.isResultBlackWins());
        cbResultUnclear.setSelected(searchPattern.isResultUndef());
        cbResultDraw.setSelected(searchPattern.isResultDraw());

        searchHeader.setSelected(searchPattern.isSearchForHeader());
        searchPosition.setSelected(searchPattern.isSearchForPosition());

    }

    private void btnInitialPositionClicked() {
        enterPosBoard.board = new Board(true);
        enterPosBoard.updateCanvas();
    }

    private void btnClearBoardClicked() {
        enterPosBoard.board = new Board(false);
        enterPosBoard.updateCanvas();
    }

    private void btnCurrentPositionClicked() {
        enterPosBoard.board = currentBoard.makeCopy();
        enterPosBoard.updateCanvas();
    }

    public SearchPattern getSearchPattern() {
        SearchPattern pattern = new SearchPattern();

        pattern.setMinMove(spinnerAfter.getValue());
        pattern.setMaxMove(spinnerBefore.getValue());
        pattern.setWhiteName(txtWhite.getText());
        pattern.setBlackName(txtBlack.getText());
        pattern.setIgnoreNameColor(ignoreCbColors.isSelected());
        pattern.setEvent(txtEvent.getText());
        pattern.setSite(txtSite.getText());
        pattern.setCheckYear(cbYear.isSelected());
        pattern.setMinYear(spinnerMinYear.getValue());
        pattern.setMaxYear(spinnerMaxYear.getValue());
        pattern.setCheckEco(cbECO.isSelected());
        pattern.setEcoStart(ecoMin.getText());
        pattern.setEcoStop(ecoMax.getText());
        pattern.setMinElo(spinnerMinElo.getValue());
        pattern.setMaxElo(spinnerMaxElo.getValue());
        if(rbEloIgnore.isSelected()) {
            pattern.setCheckElo(SearchPattern.SEARCH_IGNORE_ELO);
        }
        if(rbEloOne.isSelected()) {
            pattern.setCheckElo(SearchPattern.SEARCH_ONE_ELO);
        }
        if(rbEloAverage.isSelected()) {
            pattern.setCheckElo(SearchPattern.SEARCH_AVG_ELO);
        }
        if(rbEloBoth.isSelected()) {
            pattern.setCheckElo(SearchPattern.SEARCH_BOTH_ELO);
        }
        pattern.setResultWhiteWins(cbResultWhiteWins.isSelected());
        pattern.setResultBlackWins(cbResultBlackWins.isSelected());
        pattern.setResultDraw(cbResultDraw.isSelected());
        pattern.setResultUndef(cbResultUnclear.isSelected());

        pattern.setBoard(enterPosBoard.board.makeCopy());

        pattern.setSearchForHeader(searchHeader.isSelected());
        pattern.setSearchForPosition(searchPosition.isSelected());

        return pattern;
    }
}
