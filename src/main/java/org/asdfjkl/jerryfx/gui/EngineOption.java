package org.asdfjkl.jerryfx.gui;

import java.util.regex.Pattern;

public class EngineOption {

    public static int EN_OPT_TYPE_SPIN = 0;
    public static int EN_OPT_TYPE_CHECK = 1;
    public static int EN_OPT_TYPE_COMBO = 2;
    public static int EN_OPT_TYPE_STRING = 3;
    static final Pattern REG_EXP_OPTION_NAME = Pattern.compile("option name (.*?) type");


}
