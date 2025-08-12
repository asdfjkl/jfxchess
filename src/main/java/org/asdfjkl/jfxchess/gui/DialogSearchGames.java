/* JerryFX - A Chess Graphical User Interface
 * Copyright (C) 2020 Dominik Klein
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

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.asdfjkl.jfxchess.lib.Board;

public class DialogSearchGames {

    Stage stage;
    boolean accepted = false;

    CheckBox searchHeader;
    CheckBox searchPosition;

    EnterPosBoard enterPosBoard;
    final Button btnFlipBoard = new Button("Flip Board");
    final Button btnInitialPosition = new Button("Initial Position");
    final Button btnClearBoard = new Button("Clear Board");
    final Button btnCurrentPosition = new Button("Current Position");
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

    public boolean show(Board board, BoardStyle currentBoardStyle, SearchPattern searchPattern) {

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
        lblAfter.setMinWidth(100);
        Label lblBefore = new Label("Before Move:");
        lblBefore.setMinWidth(100);
        spinnerAfter = new Spinner<Integer>();
        SpinnerValueFactory<Integer> valueFactoryAfter = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1);
        spinnerAfter.setValueFactory(valueFactoryAfter);
        spinnerAfter.setEditable(true);
        spinnerAfter.setMinWidth(70);

        spinnerBefore = new Spinner<Integer>();
        SpinnerValueFactory<Integer> valueFactoryBefore = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 40);
        spinnerBefore.setValueFactory(valueFactoryBefore);
        spinnerBefore.setEditable(true);
        spinnerBefore.setMinWidth(70);

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

        enterPosBoard = new EnterPosBoard(currentBoard.makeCopy());
        enterPosBoard.boardStyle = currentBoardStyle;
        // to make sure, the enter position board is displayed correctly
        //enterPosBoard.setHeight(200);

        HBox hbTabPosition = new HBox();
        hbTabPosition.getChildren().addAll(enterPosBoard, vbButtonsRight);
        //hbTabPosition.setHgrow(enterPosBoard, Priority.ALWAYS);
        hbTabPosition.setPrefWidth(1000);

        /*
         HEADER SEARCH TAB
         */
        GridPane gridHeaderSearch = new GridPane();
        gridHeaderSearch.setHgap(10);
        gridHeaderSearch.setVgap(8);

        Label lblWhite = new Label("White:");
        txtWhite = new TextField();
        Label lblBlack = new Label("Black:");
        txtBlack = new TextField();
        ignoreCbColors = new CheckBox("Ignore Colors");

        Label lblEvent = new Label("Event:");
        txtEvent = new TextField();
        Label lblSite = new Label("Site:");
        txtSite = new TextField();

        cbYear = new CheckBox("Year:");
        spinnerMinYear = new Spinner<>();
        spinnerMaxYear = new Spinner<>();
        spinnerMinYear.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(500, 2100, 500));
        spinnerMaxYear.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(500, 2100, 2100));
        spinnerMinYear.setEditable(true);
        spinnerMaxYear.setEditable(true);

        cbECO = new CheckBox("ECO:");
        ecoMin = new TextField("A00");
        ecoMax = new TextField("E99");

        // Year + ECO
        Label yearSep = new Label("to");
        yearSep.getStyleClass().add("muted");
        HBox hbYear = new HBox(8, spinnerMinYear, yearSep, spinnerMaxYear);
        hbYear.setAlignment(Pos.CENTER_LEFT);

        Label ecoSep = new Label("to");
        ecoSep.getStyleClass().add("muted");
        HBox hbEco = new HBox(8, ecoMin, ecoSep, ecoMax);
        hbEco.setAlignment(Pos.CENTER_LEFT);

        int row = 0;
        gridHeaderSearch.add(lblWhite, 0, row);
        gridHeaderSearch.add(txtWhite, 1, row++);

        gridHeaderSearch.add(lblBlack, 0, row);
        gridHeaderSearch.add(txtBlack, 1, row++);
        gridHeaderSearch.add(ignoreCbColors, 1, row++);

        gridHeaderSearch.add(lblEvent, 0, row);
        gridHeaderSearch.add(txtEvent, 1, row++);

        gridHeaderSearch.add(lblSite, 0, row);
        gridHeaderSearch.add(txtSite, 1, row++);

        gridHeaderSearch.add(cbYear, 0, row);
        gridHeaderSearch.add(hbYear, 1, row++);

        gridHeaderSearch.add(cbECO, 0, row);
        gridHeaderSearch.add(hbEco, 1, row++);

        // Elo section
        Label lblElo = new Label("Elo");
        lblElo.getStyleClass().add("heading"); // AtlantaFX heading style

        spinnerMinElo = new Spinner<>();
        spinnerMaxElo = new Spinner<>();
        spinnerMinElo.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1000, 3000, 1000));
        spinnerMaxElo.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1000, 3000, 3000));
        spinnerMinElo.setEditable(true);
        spinnerMaxElo.setEditable(true);
        spinnerMinElo.setMaxWidth(120);
        spinnerMaxElo.setMaxWidth(120);

        rbEloIgnore = new RadioButton("Ignore");
        rbEloOne = new RadioButton("One");
        rbEloBoth = new RadioButton("Both");
        rbEloAverage = new RadioButton("Average");

        ToggleGroup tgElo = new ToggleGroup();
        rbEloIgnore.setToggleGroup(tgElo);
        rbEloOne.setToggleGroup(tgElo);
        rbEloBoth.setToggleGroup(tgElo);
        rbEloAverage.setToggleGroup(tgElo);

        Label eloSep = new Label("to");
        eloSep.getStyleClass().add("muted");
        HBox hbEloMain = new HBox(10,
                spinnerMinElo, eloSep, spinnerMaxElo,
                rbEloIgnore, rbEloOne, rbEloBoth, rbEloAverage
        );
        hbEloMain.setAlignment(Pos.CENTER_LEFT);

        // Result section
        Label lblResult = new Label("Result");
        lblResult.getStyleClass().add("heading");

        cbResultWhiteWins = new CheckBox("1-0");
        cbResultBlackWins = new CheckBox("0-1");
        cbResultUnclear = new CheckBox("*");
        cbResultDraw = new CheckBox("1/2-1/2");

        HBox hbResult = new HBox(15,
                cbResultWhiteWins,
                cbResultBlackWins,
                cbResultUnclear,
                cbResultDraw
        );
        hbResult.setAlignment(Pos.CENTER_LEFT);

        // Reset button for header search
        Button resetHeaderSearch = new Button("Reset");
        resetHeaderSearch.setOnAction(e -> btnResetClicked());
        HBox hbButtonReset = new HBox(resetHeaderSearch);
        hbButtonReset.setAlignment(Pos.CENTER_RIGHT);

        // final vertical layout for position search tab
        VBox vboxGameData = new VBox(15,
                gridHeaderSearch,
                lblElo, hbEloMain,
                lblResult, hbResult,
                hbButtonReset
        );
        vboxGameData.setPadding(new Insets(15));



        TabPane tabPane = new TabPane();
        Tab tabHeader = new Tab("Game Data", vboxGameData);
        Tab tabPosition = new Tab("Position", hbTabPosition);
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

    private void btnResetClicked() {
        SearchPattern pattern = new SearchPattern();
        recoverFromSearchPattern(pattern);
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
        enterPosBoard.resetToStartingPosition();
    }

    private void btnClearBoardClicked() {
        enterPosBoard.clearBoard();
    }

    private void btnCurrentPositionClicked() {
        enterPosBoard.copyBoard(currentBoard);
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

        pattern.setBoard(enterPosBoard.makeBoardCopy());

        pattern.setSearchForHeader(searchHeader.isSelected());
        pattern.setSearchForPosition(searchPosition.isSelected());

        return pattern;
    }
}
