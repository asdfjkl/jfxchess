package org.asdfjkl.jerryfx.gui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import jfxtras.styles.jmetro.JMetro;


public class DialogGameAnalysis {

    Stage stage;
    boolean accepted = false;

    Label lSecsPerMove = new Label("Sec(s) per Move");
    Label lPawnThresh = new Label("Threshold (in pawns)");
    Label lAnalysePlayers = new Label("Analyse Players:");

    Spinner<Integer> sSecs = new Spinner<Integer>();
    Spinner<Double> sPawnThreshold = new Spinner<Double>();

    RadioButton rbBoth = new RadioButton("Both");
    RadioButton rbWhite = new RadioButton("White");
    RadioButton rbBlack = new RadioButton("Black");

    public boolean show(int currSecs, double currThreshold) {

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

        SpinnerValueFactory<Double> valueFactoryThres =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(0.1, 1.0, 0.5, 0.1);
        sPawnThreshold.setValueFactory(valueFactoryThres);
        sPawnThreshold.setEditable(true);

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

}
