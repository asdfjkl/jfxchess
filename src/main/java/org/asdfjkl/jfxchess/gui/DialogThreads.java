package org.asdfjkl.jfxchess.gui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class DialogThreads {

    Stage stage;
    boolean accepted = false;

    final Label lblCpuThreads = new Label("CPU Threads");
    final Spinner<Integer> spThreads = new Spinner<Integer>();

    public boolean show(int currentThreads, int maxThreads) {

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

        SpinnerValueFactory<Integer> valueFactoryThreads =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, maxThreads, currentThreads);
        spThreads.setValueFactory(valueFactoryThreads);
        spThreads.setEditable(true);

        GridPane grd = new GridPane();
        grd.add(lblCpuThreads, 0, 0);
        grd.add(spThreads, 1, 0);
        grd.setHgap(10);
        grd.setVgap(10);

        VBox vbox = new VBox();
        vbox.getChildren().addAll(
                grd,
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





