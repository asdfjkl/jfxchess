package org.asdfjkl.jerryfx.gui;

import java.util.ArrayList;

public class Engine {

    private String name = "";
    private String path = "";

    private boolean isInternal = false;

    public ArrayList<EngineOption> options = new ArrayList<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public boolean isInternal() { return isInternal; }
    public void setInternal(boolean internal) { this.isInternal = internal; }


}
