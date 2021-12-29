package org.asdfjkl.jerryfx.gui;

import javafx.scene.control.TableCell;
import org.asdfjkl.jerryfx.lib.PolyglotExtEntry;

public class RatioCell extends TableCell<PolyglotExtEntry, PolyglotExtEntry> {

    BarchartWDL barchartWDL = null;


    public RatioCell(int theme) {
        super();
        barchartWDL = new BarchartWDL();
        barchartWDL.setTheme(theme);
    }

    @Override
    protected void updateItem(PolyglotExtEntry item, boolean empty) {
        super.updateItem(item, empty);

        if(empty || (item == null)) {
            setText(null);
            setGraphic(null);
        } else {
            barchartWDL.setWDLRatio(item);
            setGraphic(barchartWDL);
        }
    }

}