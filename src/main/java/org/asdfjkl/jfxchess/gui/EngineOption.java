/* JFXChess - A Chess Graphical User Interface
 * Copyright (C) 2020-2025 Dominik Klein
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.asdfjkl.jfxchess.gui;

import java.util.ArrayList;

public class EngineOption {

    public static final int EN_OPT_TYPE_SPIN = 0;
    public static final int EN_OPT_TYPE_CHECK = 1;
    public static final int EN_OPT_TYPE_COMBO = 2;
    public static final int EN_OPT_TYPE_STRING = 3;
    public static final int EN_OPT_TYPE_BUTTON = 4;

    public int type = -1;
    public String name = "";

    public boolean checkStatusValue = false;
    public boolean checkStatusDefault = false;

    public int spinValue;
    public int spinMin;
    public int spinMax;
    public int spinDefault;

    public String comboDefault = "";
    public String comboValue = "";
    public final ArrayList<String> comboValues = new ArrayList<>();

    public String stringValue = "";
    public String stringDefault = "";

    public EngineOption makeCopy() {

        EngineOption copy = new EngineOption();
        copy.type = this.type;
        copy.name = this.name;
        copy.checkStatusValue = this.checkStatusValue;
        copy.checkStatusDefault = this.checkStatusDefault;

        copy.spinValue = this.spinValue;
        copy.spinMin = this.spinMin;
        copy.spinMax = this.spinMax;
        copy.spinDefault = this.spinDefault;

        copy.comboDefault = this.comboDefault;
        copy.comboValue = this.comboValue;
        for(String comboVal : comboValues) {
            copy.comboValues.add(comboVal);
        }
        copy.stringValue = this.stringValue;
        copy.stringDefault = this.stringDefault;

        return copy;
    }

    public void resetToDefault() {

        checkStatusValue = checkStatusDefault;
        spinValue = spinDefault;
        comboValue = comboDefault;
        stringValue = stringDefault;
    }

    public boolean isNotDefault() {
        if(type == EN_OPT_TYPE_CHECK || type == EN_OPT_TYPE_BUTTON) {
            return (checkStatusDefault != checkStatusValue);
        }

        if(type == EN_OPT_TYPE_SPIN) {
            return (spinValue != spinDefault);
        }

        if(type == EN_OPT_TYPE_COMBO) {
            return (!comboValue.equals(comboDefault));
        }

        if(type == EN_OPT_TYPE_STRING) {
            return (!stringDefault.equals(stringValue));
        }

        return true;
    }

    public String toUciCommand() {
        switch (type) {
            case EN_OPT_TYPE_CHECK:
                if (checkStatusValue) {
                    return "setoption name " + name + " value true";
                } else {
                    return "setoption name " + name + " value false";
                }
            case EN_OPT_TYPE_BUTTON:
                return "setoption name " + name;
            case EN_OPT_TYPE_COMBO:
                return "setoption name " + name + " value " + comboValue;
            case EN_OPT_TYPE_SPIN:
                return "setoption name " + name + " value " + spinValue;
            case EN_OPT_TYPE_STRING:
                return "setoption name " + name + " value " + stringValue;
            default:
                return "";
        }
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
            
            // First read the name
            name = getName(optionString);
            
            // Now that we are ready with the name, we cut
            // away everything before the word "type", so
            // the words in the name don't mess up the rest
            // of the parsing.
            
            // There is one engine called Gaviota, that had "default" inside 
            // the option-name as well as in the "value-part". That engine
            // couldn't be loaded by jfxchess. We got NumberFormatException
            // on the "type"-word after the first "default".            
            
            if (!optionString.contains("type")) {
                return false;
            }
            
            String optionString2ndPart = new String(
                      optionString.substring(
                      optionString.indexOf("type"),
                      optionString.length()));

            // Split the rest of the string in words and numbers
            String[] opts = optionString2ndPart.split(" ");

            if (optionString2ndPart.contains("type spin")) {
                type = EN_OPT_TYPE_SPIN;
                for (int i = 0; i < opts.length; i++) {
                    if (opts[i].equals("default") && i + 1 < opts.length) {
                        spinDefault = Integer.parseInt(opts[i + 1]);
                        spinValue = spinDefault;
                    }
                    if (opts[i].equals("min") && i + 1 < opts.length) {
                        spinMin = Integer.parseInt(opts[i + 1]);
                    }
                    if (opts[i].equals("max") && i + 1 < opts.length) {
                        spinMax = Integer.parseInt(opts[i + 1]);
                    }
                }
            }

            if (optionString2ndPart.contains("type check")) {
                type = EN_OPT_TYPE_CHECK;
                if (optionString2ndPart.contains("default true")) {
                    checkStatusDefault = true;
                    checkStatusValue = true;
                } else {
                    checkStatusDefault = false;
                    checkStatusValue = false;
                }
            }

            if (optionString2ndPart.contains("type button")) {
                type = EN_OPT_TYPE_BUTTON;
                checkStatusDefault = false;
                checkStatusValue = false;
            }
            
            if (optionString2ndPart.contains("type string")) {
                type = EN_OPT_TYPE_STRING;
                for (int i = 0; i < opts.length; i++) {
                    if (opts[i].equals("default") && i + 1 < opts.length) {
                        stringDefault = opts[i + 1];
                        stringValue = stringDefault;
                    }
                }
            }

            if (optionString2ndPart.contains("type combo")) {
                type = EN_OPT_TYPE_STRING;
                for (int i = 0; i < opts.length; i++) {
                    if (opts[i].equals("default") && i + 1 < opts.length) {
                        comboDefault = opts[i + 1];
                        comboValue = comboDefault;
                    }
                    if (opts[i].equals("var") && i + 1 < opts.length) {
                        comboValues.add(opts[i + 1]);
                        comboValue = comboDefault;
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
        switch (type) {
            case EN_OPT_TYPE_CHECK:
                return "option name " + name + " type check default " + checkStatusDefault;
            case EN_OPT_TYPE_BUTTON:
                return "option name " + name + " type button ";
            case EN_OPT_TYPE_COMBO:
                String s = "option name " + name + " type combo default " + comboDefault;
                for (String optionVar : comboValues) {
                    s += " var " + optionVar;
                }
                return s;
            case EN_OPT_TYPE_SPIN:
                return "option name " + name + " type spin default " + spinDefault + " min " + spinMin + " max " + spinMax;
            case EN_OPT_TYPE_STRING:
                return "option name " + name + " type string default " + stringValue;
            default:
                return "";
        }
    }


}
