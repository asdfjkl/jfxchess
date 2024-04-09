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

import java.time.MonthDay;
import java.time.Year;
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
        eloWhite.getValueFactory().setValue(0);
        eloWhite.setPrefWidth(80);

        SpinnerValueFactory<Integer> valueFactoryBlackElo =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 5000, 0);
        eloBlack.setValueFactory(valueFactoryBlackElo);
        eloBlack.setEditable(true);
        // set initial value
        eloBlack.getValueFactory().setValue(0);
        eloBlack.setPrefWidth(80);

        SpinnerValueFactory<Integer> valueFactoryRound =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 999, 0);
        round.setValueFactory(valueFactoryRound);
        round.setEditable(true);
        // set initial value
        round.getValueFactory().setValue(0);
        round.setPrefWidth(60);

        SpinnerValueFactory<Integer> valueFactoryYear =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 3000, 0);
        year.setValueFactory(valueFactoryYear);
        year.setEditable(true);
        // set initial value
        year.getValueFactory().setValue(Year.now().getValue());
        year.setPrefWidth(80);

        SpinnerValueFactory<Integer> valueFactoryDay =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 31, 0);
        day.setValueFactory(valueFactoryDay);
        day.setEditable(true);
        // set initial value
        day.getValueFactory().setValue(MonthDay.now().getDayOfMonth());
        day.setPrefWidth(60);

        SpinnerValueFactory<Integer> valueFactoryMonth =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 12, 0);
        month.setValueFactory(valueFactoryMonth);
        month.setEditable(true);
        // set initial value
        month.getValueFactory().setValue(MonthDay.now().getMonthValue());
        month.setPrefWidth(60);

        site.setPromptText("Site");
        if(pgnHeaders.get("Site") != null) {
            site.setText(pgnHeaders.get("Site"));
        }

        event.setPromptText("Event");
        if(pgnHeaders.get("Event") != null) {
            event.setText(pgnHeaders.get("Event"));
        }

        whiteSurname.setPromptText("White Surname");
        whiteFirstname.setPromptText("White First Name");
        if(pgnHeaders.get("White") != null) {
            String[] tmpWhiteName = pgnHeaders.get("White").split(",");
            if(tmpWhiteName.length > 0) {
                whiteSurname.setText(tmpWhiteName[0].strip());
            }
            if(tmpWhiteName.length > 1) {
                whiteFirstname.setText(tmpWhiteName[1].strip());
            }
        }

        blackSurname.setPromptText("Black Surname");
        blackFirstname.setPromptText("Black First Name");
        if(pgnHeaders.get("Black") != null) {
            String[] tmpBlackName = pgnHeaders.get("Black").split(",");
            if(tmpBlackName.length > 0) {
                blackSurname.setText(tmpBlackName[0].strip());
            }
            if(tmpBlackName.length > 1) {
                blackFirstname.setText(tmpBlackName[1].strip());
            }
        }

        day.setPromptText("DD");
        year.setPromptText("YYYY");
        month.setPromptText("MM");
        if(pgnHeaders.get("Date") != null) {
            String[] tmpDate = pgnHeaders.get("Date").split("\\.");
            if(tmpDate.length > 0 && tmpDate[0].length() == 4) { // hopefully YYYY.MM.DD
                try {
                    int iYear = Integer.parseInt(tmpDate[0].strip());
                    year.getValueFactory().setValue(iYear);
                } catch(NumberFormatException e) {
                    year.getValueFactory().setValue(0);
                }
                if(tmpDate.length > 1) {
                    try {
                        int iMonth = Integer.parseInt(tmpDate[1].strip());
                        month.getValueFactory().setValue(iMonth);
                    } catch(NumberFormatException e) {
                        month.getValueFactory().setValue(0);
                    }
                }
                if(tmpDate.length > 2) {
                    try {
                        int iDay = Integer.parseInt(tmpDate[2].strip());
                        day.getValueFactory().setValue(iDay);
                    } catch(NumberFormatException e) {
                        day.getValueFactory().setValue(0);
                    }
                }
            } else if (tmpDate.length > 2 && tmpDate[2].length() == 4) { // probably DD.MM.YYYY
                try {
                    int iYear = Integer.parseInt(tmpDate[2].strip());
                    year.getValueFactory().setValue(iYear);
                } catch(NumberFormatException e) {
                    year.getValueFactory().setValue(0);
                }
                try {
                    int iMonth = Integer.parseInt(tmpDate[1].strip());
                    month.getValueFactory().setValue(iMonth);
                } catch(NumberFormatException e) {
                    month.getValueFactory().setValue(0);
                }
                try {
                    int iDay = Integer.parseInt(tmpDate[0].strip());
                    day.getValueFactory().setValue(iDay);
                } catch(NumberFormatException e) {
                    day.getValueFactory().setValue(0);
                }
            }
        }

        eloWhite.setPromptText("Elo White");
        if(pgnHeaders.get("WhiteElo") != null) {
            try {
                int we = Integer.parseInt(pgnHeaders.get("WhiteElo"));
                eloWhite.getValueFactory().setValue(we);
            } catch (NumberFormatException e) {
                eloWhite.getValueFactory().setValue(0);
            }
        }

        eloBlack.setPromptText("Elo Black");
        if(pgnHeaders.get("BlackElo") != null) {
            try {
                int we = Integer.parseInt(pgnHeaders.get("BlackElo"));
                eloBlack.getValueFactory().setValue(we);
            } catch (NumberFormatException e) {
                eloBlack.getValueFactory().setValue(0);
            }
        }

        round.setPromptText("Round");
        if(pgnHeaders.get("Round") != null) {
            try {
                int iRound = Integer.parseInt(pgnHeaders.get("Round"));
                round.getValueFactory().setValue(iRound);
            } catch (NumberFormatException e) {
                round.getValueFactory().setValue(0);
            }
        }


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

        grid.add(new Label("Year:"), 0, 6);
        grid.add(year, 1, 6);

        grid.add(new Label("Month:"), 0, 7);
        grid.add(month, 1, 7);

        grid.add(new Label("Day:"), 0, 8);
        grid.add(day, 1, 8);

        grid.add(new Label("Round:"), 0, 9);
        grid.add(round, 1, 9);

        grid.add(new Label("Elo White:"), 0, 10);
        grid.add(eloWhite, 1, 10);

        grid.add(new Label("Elo Black:"), 0, 11);
        grid.add(eloBlack, 1, 11);

        grid.add(new Label("Result:"), 0,12);

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

        pgnHeaders.put("Site", site.getText());
        if(round.getValue() >= 0) {
            pgnHeaders.put("Round", Integer.toString(round.getValue()));
        }

        if(!whiteSurname.getText().isEmpty() && !whiteFirstname.getText().isEmpty()) {
            pgnHeaders.put("White", whiteSurname.getText()+", "+whiteFirstname.getText());
        } else if(!whiteSurname.getText().isEmpty() && whiteFirstname.getText().isEmpty()) {
            pgnHeaders.put("White", whiteSurname.getText());
        } else if(whiteSurname.getText().isEmpty() && !whiteFirstname.getText().isEmpty()) {
            pgnHeaders.put("White", whiteFirstname.getText());
        }

        if(!blackSurname.getText().isEmpty() && !blackFirstname.getText().isEmpty()) {
            pgnHeaders.put("Black", blackSurname.getText()+", "+blackFirstname.getText());
        } else if(!blackSurname.getText().isEmpty() && blackFirstname.getText().isEmpty()) {
            pgnHeaders.put("Black", blackSurname.getText());
        } else if(blackSurname.getText().isEmpty() && !blackFirstname.getText().isEmpty()) {
            pgnHeaders.put("Black", blackFirstname.getText());
        }

        if(eloWhite.getValue() >= 0) {
            pgnHeaders.put("WhiteElo", Integer.toString(eloWhite.getValue()));
        }
        if(eloBlack.getValue() >= 0) {
            pgnHeaders.put("BlackElo", Integer.toString(eloBlack.getValue()));
        }

        String tmpDate = "";
        if(year.getValue() > 0 && year.getValue() < 3000) {
            tmpDate += String.format("%04d", year.getValue());
        } else {
            tmpDate += "????";
        }
        if(month.getValue() > 0 && month.getValue() <= 12) {
            tmpDate += "." + String.format("%02d", month.getValue());
        } else {
            tmpDate += "." + "??";
        }
        if(day.getValue() > 0 && day.getValue() <= 31) {
            tmpDate += "." + String.format("%02d", day.getValue());
        } else {
            tmpDate += "." + "??";
        }
        pgnHeaders.put("Date", tmpDate);

        pgnHeaders.put("Event", event.getText());

        if(rbWhiteWin.isSelected()) {
            gameResult = CONSTANTS.RES_WHITE_WINS;
            pgnHeaders.put("Result", "1-0");
        }
        if(rbBlackWin.isSelected()) {
            gameResult = CONSTANTS.RES_BLACK_WINS;
            pgnHeaders.put("Result", "0-1");
        }
        if(rbDraw.isSelected()) {
            gameResult = CONSTANTS.RES_DRAW;
            pgnHeaders.put("Result", "1/2-1/2");
        }
        if(rbUndecided.isSelected()) {
            gameResult = CONSTANTS.RES_UNDEF;
            pgnHeaders.put("Result", "*");
        }

        stage.close();
    }

    private void btnCancelClicked() {
        accepted = false;
        stage.close();
    }

}
