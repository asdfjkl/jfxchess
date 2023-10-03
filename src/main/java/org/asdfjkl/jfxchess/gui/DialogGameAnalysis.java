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
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.JMetroStyleClass;
import jfxtras.styles.jmetro.Style;


public class DialogGameAnalysis {

    Stage stage;
    boolean accepted = false;

    final Label lSecsPerMove = new Label("Sec(s) per Move");
    final Label lPawnThresh = new Label("Threshold (in pawns)");
    final Label lAnalysePlayers = new Label("Analyse Players:");

    final Spinner<Integer> sSecs = new Spinner<Integer>();
    final Spinner<Double> sPawnThreshold = new Spinner<Double>();

    final RadioButton rbBoth = new RadioButton("Both");
    final RadioButton rbWhite = new RadioButton("White");
    final RadioButton rbBlack = new RadioButton("Black");

    public boolean show(int currSecs, double currThreshold, int colorTheme) {

        //System.out.println(currSecs + " " + currThreshold);

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

        ToggleGroup radioGroupColors = new ToggleGroup();
        rbBoth.setToggleGroup(radioGroupColors);
        rbWhite.setToggleGroup(radioGroupColors);
        rbBlack.setToggleGroup(radioGroupColors);

        rbBoth.setSelected(true);

        SpinnerValueFactory<Integer> valueFactorySecs =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, 5);
        sSecs.setValueFactory(valueFactorySecs);
        sSecs.setEditable(true);
        sSecs.getValueFactory().setValue(currSecs);

        SpinnerValueFactory<Double> valueFactoryThres =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(0.1, 5.0, 0.5, 0.1);
        sPawnThreshold.setValueFactory(valueFactoryThres);
        sPawnThreshold.setEditable(true);
        sPawnThreshold.getValueFactory().setValue(currThreshold);

        HBox hbColors = new HBox(rbBoth, rbWhite, rbBlack);
        hbColors.setSpacing(10);
        hbColors.setPadding(new Insets(5, 20, 20, 0));

        GridPane grd = new GridPane();
        grd.add(lSecsPerMove, 0, 0);
        grd.add(sSecs, 1, 0);
        grd.add(lPawnThresh, 0, 1);
        grd.add(sPawnThreshold, 1, 1);
        grd.setHgap(10);
        grd.setVgap(10);
        //grd.setPadding(new Insets(5, 20, 20, 0));

        VBox vbox = new VBox();
        vbox.getChildren().addAll(
                grd,
                lAnalysePlayers,
                hbColors,
                hbButtons);
        vbox.setSpacing(10);
        vbox.setPadding( new Insets(15, 25, 10, 25));

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

}
