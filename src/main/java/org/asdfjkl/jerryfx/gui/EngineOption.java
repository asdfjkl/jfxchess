package org.asdfjkl.jerryfx.gui;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class EngineOption {

    public static int EN_OPT_TYPE_SPIN = 0;
    public static int EN_OPT_TYPE_CHECK = 1;
    public static int EN_OPT_TYPE_COMBO = 2;
    public static int EN_OPT_TYPE_STRING = 3;
    //static final Pattern REG_EXP_OPTION_NAME = Pattern.compile("option name (.*?) type");

    public int type = -1;
    public String name = "";

    public boolean checkStatusValue = false;
    public boolean checkStatusDefault = false;

    public int spinValue;
    public int spinMin;
    public int spinMax;
    public int spinDefault;

    public String comboDefault;
    public String comboValue;
    public ArrayList<String> comboValues;

    public String stringValue;
    public String stringDefault;

    public String toUciCommand() {

        if(type == EN_OPT_TYPE_CHECK) {
            if(checkStatusValue) {
                return "setoption name " + name + " value true";
            } else {
                return "setoption name " + name + " value false";
            }
        }

        if(type == EN_OPT_TYPE_COMBO) {
            return "setoption name "+name+" value "+comboValue;
        }

        if(type == EN_OPT_TYPE_SPIN) {
            return "setoption name "+name+" value "+spinValue;
        }

        if(type == EN_OPT_TYPE_STRING) {
            return "setoption name "+name+" value "+stringValue;
        }
        return "";
    }

    private String getName(String optionString) {

        String[] opts = optionString.split(" ");
        String parsedName = "";
        for(int i=0;i<opts.length;i++) {
            if(opts[i].equals("name")) {
                for(int j=i+1;j<opts.length;j++) {
                    if(opts[j].equals("type")) {
                        return parsedName.trim();
                    } else {
                        parsedName += " "+opts[j];
                    }
                }
            }
        }
        return parsedName;
    }

    public boolean parseUciOptionString(String optionString) {

        if(optionString.startsWith("option name")) {

            if (optionString.contains("type spin")) {
                type = EN_OPT_TYPE_SPIN;
                name = getName(optionString);

                String[] opts = optionString.split(" ");
                for (int i = 0; i < opts.length; i++) {
                    if (opts[i].equals("default") && i + 1 < opts.length) {
                        spinDefault = Integer.parseInt(opts[i + 1]);
                    }
                    if (opts[i].equals("min") && i + 1 < opts.length) {
                        spinMin = Integer.parseInt(opts[i + 1]);
                    }
                    if (opts[i].equals("max") && i + 1 < opts.length) {
                        spinMax = Integer.parseInt(opts[i + 1]);
                    }
                }
            }

            if (optionString.contains("type check")) {
                type = EN_OPT_TYPE_CHECK;
                name = getName(optionString);

                if (optionString.contains("default true")) {
                    checkStatusDefault = true;
                } else {
                    checkStatusDefault = false;
                }
            }

            if (optionString.contains("type string")) {
                type = EN_OPT_TYPE_STRING;
                name = getName(optionString);
                String[] opts = optionString.split(" ");
                for (int i = 0; i < opts.length; i++) {
                    if (opts[i].equals("default") && i + 1 < opts.length) {
                        stringDefault = opts[i + 1];
                    }
                }
            }

            if (optionString.contains("type combo")) {
                type = EN_OPT_TYPE_STRING;
                name = getName(optionString);
                String[] opts = optionString.split(" ");
                for (int i = 0; i < opts.length; i++) {
                    if (opts[i].equals("default") && i + 1 < opts.length) {
                        comboDefault = opts[i + 1];
                    }
                    if (opts[i].equals("var") && i + 1 < opts.length) {
                        comboValues.add(opts[i + 1]);
                    }
                }
            }

            if (!name.isEmpty() && type >= 0) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public String toUciOptionString() {

        if(type == EN_OPT_TYPE_SPIN) {
            String s = "option name "+name+" type spin default "+spinDefault+" min "+spinMin+" max "+spinMax;
            return s;
        }

        if(type == EN_OPT_TYPE_CHECK) {
            String s = "option name "+name+" type check default "+checkStatusDefault;
            return s;
        }

        if(type == EN_OPT_TYPE_STRING) {
            String s = "option name "+name+" type string default "+spinDefault;
            return s;
        }

        if(type == EN_OPT_TYPE_COMBO) {
            String s = "option name "+name+" type combo default "+comboDefault;
            for(String optionVar : comboValues) {
                s += " var "+optionVar;
            }
            return s;
        }

        return "";
    }


}
