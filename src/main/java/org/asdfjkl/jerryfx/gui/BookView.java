package org.asdfjkl.jerryfx.gui;

public class BookView implements StateChangeListener {


    @Override
    public void stateChange() {
        System.out.println("bookview: state change received");
    }
}
