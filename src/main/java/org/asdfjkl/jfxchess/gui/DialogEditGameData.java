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
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.JMetroStyleClass;
import jfxtras.styles.jmetro.Style;
import org.asdfjkl.jfxchess.lib.CONSTANTS;

import java.util.HashMap;
import java.util.Map;

public class DialogEditGameData {

    Stage stage;
    final HashMap<String, String> pgnHeaders = new HashMap<>();
    final TextField site = new TextField();
    final Spinner<Integer> year = new Spinner<Integer>();
    final Spinner<Integer> month = new Spinner<Integer>();
    final Spinner<Integer> day = new Spinner<Integer>();
    final Spinner<Integer> round = new Spinner<Integer>();
    final TextField event = new TextField();
    final TextField whiteSurname = new TextField();
    final TextField whiteFirstname = new TextField();
    final TextField blackSurname = new TextField();
    final TextField blackFirstname = new TextField();
    final Spinner<Integer> eloWhite = new Spinner<Integer>();
    final Spinner<Integer> eloBlack = new Spinner<Integer>();
    boolean accepted = false;
    int gameResult = -1;
    final RadioButton rbWhiteWin = new RadioButton("1-0");
    final RadioButton rbBlackWin = new RadioButton("0-1");
    final RadioButton rbDraw = new RadioButton("1/2-1/2");
    final RadioButton rbUndecided = new RadioButton("*");

    public boolean show(HashMap<String, String> pgnHeaders, int gameResult, int colorTheme) {

        for (Map.Entry<String, String> entry : pgnHeaders.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            pgnHeaders.put(key,value);
        }
        this.gameResult = gameResult;

        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UTILITY);

        Button btnOk = new Button();
        btnOk.setText("OK");

        Button btnCancel = new Button();
        btnCancel.setText("Cancel");
        Region spacer = new Region();

        HBox hbButtons = new HBox();
        hbButtons.getChildren().addAll(spacer, btnOk, btnCancel);
        hbButtons.setHgrow(spacer, Priority.ALWAYS);
        hbButtons.setSpacing(10);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 0, 20));

        // we default the input TextFields to grow, if the user resizes
        // or needs more space for the e.g. event or site field
        ColumnConstraints clnTextFields = new ColumnConstraints(100,300,Double.MAX_VALUE);
        clnTextFields.setHgrow(Priority.ALWAYS);
        ColumnConstraints clnLabels = new ColumnConstraints(100);
        grid.getColumnConstraints().addAll(clnLabels, clnTextFields);

        // third value: read from PGN header or set to 0 by default
        SpinnerValueFactory<Integer> valueFactoryWhiteElo =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 5000, 0);
        eloWhite.setValueFactory(valueFactoryWhiteElo);
        eloWhite.setEditable(true);
        // set initial value
        eloWhite.getValueFactory().setValue(1900);
        eloWhite.setPrefWidth(80);

        SpinnerValueFactory<Integer> valueFactoryBlackElo =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 5000, 0);
        eloBlack.setValueFactory(valueFactoryBlackElo);
        eloBlack.setEditable(true);
        // set initial value
        eloBlack.getValueFactory().setValue(1900);
        eloBlack.setPrefWidth(80);

        SpinnerValueFactory<Integer> valueFactoryRound =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 999, 0);
        round.setValueFactory(valueFactoryRound);
        round.setEditable(true);
        // set initial value
        round.getValueFactory().setValue(42);
        round.setPrefWidth(80);

        /*
        site.setPromptText("Site");
        if(pgnHeaders.get("Site") != null) {
            site.setText(pgnHeaders.get("Site"));
        }

        date.setPromptText("Date");
        if(pgnHeaders.get("Date") != null) {
            date.setText(pgnHeaders.get("Date"));
        }

        round.setPromptText("Round");
        if(pgnHeaders.get("Round") != null) {
            round.setText(pgnHeaders.get("Round"));
        }

        event.setPromptText("Event");
        if(pgnHeaders.get("Event") != null) {
            event.setText(pgnHeaders.get("Event"));
        }

        white.setPromptText("White");
        if(pgnHeaders.get("White") != null) {
            white.setText(pgnHeaders.get("White"));
        }

        black.setPromptText("Black");
        if(pgnHeaders.get("Black") != null) {
            black.setText(pgnHeaders.get("Black"));
        }

        eloWhite.setPromptText("Elo White");
        if(pgnHeaders.get("WhiteElo") != null) {
            eloWhite.setText(pgnHeaders.get("WhiteElo"));
        }

        eloWhite.setPromptText("Elo White");
        if(pgnHeaders.get("WhiteElo") != null) {
            eloWhite.setText(pgnHeaders.get("WhiteElo"));
        }
        */

        // first all text information
        // then all number inputs
        grid.add(new Label("White Firstname:"), 0, 0);
        grid.add(whiteFirstname, 1, 0);
        grid.add(new Label("White Surname:"), 0, 1);
        grid.add(whiteSurname, 1, 1);

        grid.add(new Label("Black Firstname:"), 0, 2);
        grid.add(blackFirstname, 1, 2);
        grid.add(new Label("Black Surname:"), 0, 3);
        grid.add(blackSurname, 1, 3);

        grid.add(new Label("Site:"), 0, 4);
        grid.add(site, 1, 4);
        grid.add(new Label("Event:"), 0, 5);
        grid.add(event, 1, 5);

        grid.add(new Label("Round:"), 0, 6);
        grid.add(round, 1, 6);

        grid.add(new Label("Elo White:"), 0, 7);
        grid.add(eloWhite, 1, 7);

        grid.add(new Label("Elo Black:"), 0, 8);
        grid.add(eloBlack, 1, 8);

        grid.add(new Label("Result:"), 0,9);

        ToggleGroup radioGroup = new ToggleGroup();

        rbWhiteWin.setToggleGroup(radioGroup);
        rbBlackWin.setToggleGroup(radioGroup);
        rbDraw.setToggleGroup(radioGroup);
        rbUndecided.setToggleGroup(radioGroup);

        if(gameResult == CONSTANTS.RES_WHITE_WINS) {
            rbWhiteWin.setSelected(true);
        }
        if(gameResult == CONSTANTS.RES_BLACK_WINS) {
            rbBlackWin.setSelected(true);
        }
        if(gameResult == CONSTANTS.RES_DRAW) {
            rbDraw.setSelected(true);
        }
        if(gameResult == CONSTANTS.RES_UNDEF || gameResult == CONSTANTS.RES_ANY) {
            rbUndecided.setSelected(true);
        }

        HBox hbox = new HBox(rbWhiteWin, rbBlackWin, rbDraw, rbUndecided);
        hbox.setSpacing(10);
        hbox.setPadding(new Insets(0, 20, 20, 20));

        VBox vbox = new VBox();
        vbox.getChildren().addAll(grid, hbox, hbButtons);
        vbox.setSpacing(10);
        vbox.setPadding( new Insets(10));

        btnOk.setOnAction(e -> {
            btnOkClicked();
        });

        btnCancel.setOnAction(e -> {
            btnCancelClicked();
        });

        vbox.getStyleClass().add(JMetroStyleClass.BACKGROUND);
        Scene scene = new Scene(vbox);

        JMetro jMetro;
        if(colorTheme == GameModel.STYLE_LIGHT) {
            jMetro = new JMetro();
        } else {
            jMetro = new JMetro(Style.DARK);
        }
        jMetro.setScene(scene);

        stage.setScene(scene);
        stage.showAndWait();

        return accepted;
    }

    private void btnOkClicked() {
        accepted = true;
        /*
        pgnHeaders.put("Site", site.getText());
        pgnHeaders.put("Date", date.getText());
        pgnHeaders.put("Round", round.getText());
        pgnHeaders.put("Event", event.getText());
        pgnHeaders.put("White", white.getText());
        pgnHeaders.put("Black", black.getText());
        pgnHeaders.put("Eco", eco.getText());

        if(rbWhiteWin.isSelected()) {
            gameResult = CONSTANTS.RES_WHITE_WINS;
        }
        if(rbBlackWin.isSelected()) {
            gameResult = CONSTANTS.RES_BLACK_WINS;
        }
        if(rbDraw.isSelected()) {
            gameResult = CONSTANTS.RES_DRAW;
        }
        if(rbUndecided.isSelected()) {
            gameResult = CONSTANTS.RES_UNDEF;
        }
        */
        stage.close();
    }

    private void btnCancelClicked() {
        accepted = false;
        stage.close();
    }

}
