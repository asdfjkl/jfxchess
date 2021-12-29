package org.asdfjkl.jerryfx.gui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import org.asdfjkl.jerryfx.lib.PolyglotExtEntry;

public class BarchartWDL extends GridPane {

    private final Label lblWins = new Label();
    private final Label lblDraws = new Label();
    private final Label lblLoss = new Label();
    private final ColumnConstraints colConsWins = new ColumnConstraints();
    private final ColumnConstraints colConsDraws = new ColumnConstraints();
    private final ColumnConstraints colConsloss = new ColumnConstraints();

    public BarchartWDL() {

        lblWins.setAlignment(Pos.CENTER);
        lblDraws.setAlignment(Pos.CENTER);
        lblLoss.setAlignment(Pos.CENTER);

        lblWins.setMaxWidth(Double.MAX_VALUE);
        lblDraws.setMaxWidth(Double.MAX_VALUE);
        lblLoss.setMaxWidth(Double.MAX_VALUE);

        addRow(0, lblWins, lblDraws, lblLoss);

        getColumnConstraints().addAll(
                colConsWins,
                colConsDraws,
                colConsloss
        );
    }

    public void setTheme(int theme) {
        if(theme == GameModel.STYLE_LIGHT) {
            lblWins.setStyle("-fx-background-color : lightgreen");
            lblDraws.setStyle("-fx-background-color : rosybrown");
            lblLoss.setStyle("-fx-background-color : indianred");
        } else {
            lblWins.setStyle("-fx-background-color : darkgreen");
            lblDraws.setStyle("-fx-background-color : saddlebrown");
            lblLoss.setStyle("-fx-background-color : darkred");
        }
    }


    public void setWDLRatio(PolyglotExtEntry entry) {
        lblWins.setText(entry.getWins()+"%");
        lblDraws.setText(entry.getDraws()+"%");
        lblLoss.setText(entry.getLosses()+"%");

        colConsWins.setPercentWidth(entry.getWins());
        colConsDraws.setPercentWidth(entry.getDraws());
        colConsloss.setPercentWidth(entry.getLosses());
    }

}
