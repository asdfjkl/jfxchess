package org.asdfjkl.jerryfx.gui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import jfxtras.styles.jmetro.JMetro;

public class DialogSimpleAlert {

    Stage stage;

    public void show(String message) {

        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UTILITY);

        Button btnOk = new Button();
        btnOk.setText("OK");
        Region spacer = new Region();

        HBox hbButtons = new HBox();
        hbButtons.getChildren().addAll(spacer, btnOk);
        hbButtons.setHgrow(spacer, Priority.ALWAYS);
        hbButtons.setSpacing(10);

        Label lblMessage = new Label(message);
        VBox vbox = new VBox();
        vbox.setSpacing(10);
        vbox.setPadding( new Insets(10));
        vbox.getChildren().addAll(lblMessage, hbButtons);

        btnOk.setOnAction(e -> {
            btnOkClicked();
        });

        Scene scene = new Scene(vbox);

        JMetro jMetro = new JMetro();
        jMetro.setScene(scene);

        stage.setScene(scene);
        stage.showAndWait();

    }

    private void btnOkClicked() {
        stage.close();
    }
}
