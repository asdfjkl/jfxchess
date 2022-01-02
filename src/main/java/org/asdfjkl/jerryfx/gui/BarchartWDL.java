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

        /*
        lblWins.setMaxWidth(Double.MAX_VALUE);
        lblDraws.setMaxWidth(Double.MAX_VALUE);
        lblLoss.setMaxWidth(Double.MAX_VALUE);
        */
        lblWins.setMaxWidth(10000);
        lblDraws.setMaxWidth(10000);
        lblLoss.setMaxWidth(10000);


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

        int sum = entry.getWins() + entry.getLosses()+ entry.getDraws();
        //System.out.println(entry.getMove() + " check 100: " + entry.getWins() + "/" + entry.getDraws() + "/" + entry.getLosses()+"/"+sum);

/*
        colConsWins.setPercentWidth(entry.getWins());
        colConsDraws.setPercentWidth(entry.getDraws());
        colConsloss.setPercentWidth(entry.getLosses());
*/

        lblWins.setPrefWidth(entry.getWins()*2.5);
        lblDraws.setPrefWidth(entry.getDraws()*2.5);
        lblLoss.setPrefWidth(entry.getLosses()*2.5);
        /*
        if(entry.getWins() == 0) {
            lblWins.setPrefWidth(.1);
        }
        if(entry.getDraws() == 0) {
            lblDraws.setPrefWidth(.1);
        }
        if(entry.getLosses() == 0) {
            lblLoss.setPrefWidth(.1);
        }*/
/*
        colConsWins.setPercentWidth(entry.getWins() / 100.);
        colConsDraws.setPercentWidth(entry.getDraws() / 100.);
        colConsloss.setPercentWidth(entry.getLosses() / 100.);
*/
        //colConsWins.setPercentWidth(0.98);
        //colConsDraws.setPercentWidth(0.01);
        //colConsloss.setPercentWidth(0.01);

    }

}
