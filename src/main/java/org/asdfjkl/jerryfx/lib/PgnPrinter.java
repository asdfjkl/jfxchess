/* JerryFX - A Chess Graphical User Interface
 * Copyright (C) 2020 Dominik Klein
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

package org.asdfjkl.jerryfx.lib;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class PgnPrinter {

    StringBuilder pgn;
    StringBuilder currentLine;
    int variationDepth;
    boolean forceMoveNumber;

    public PgnPrinter() {
        this.pgn = new StringBuilder();
        this.currentLine = new StringBuilder();
        this.variationDepth = 0;
        this.forceMoveNumber = true;
    }

    private void reset() {
        this.pgn = new StringBuilder();
        this.currentLine = new StringBuilder();
        this.variationDepth = 0;
        this.forceMoveNumber = true;
    }

    private void flushCurrentLine() {
        if(this.currentLine.length() != 0) {
            this.pgn.append(this.currentLine.toString().trim());
            this.pgn.append("\n");
            this.currentLine.setLength(0);
        }
    }

    private void writeToken(String token) {
        if(80 - this.currentLine.length() < token.length()) {
            this.flushCurrentLine();
        }
        this.currentLine.append(token);
    }

    private void writeLine(String line) {
        this.flushCurrentLine();
        this.pgn.append(line.trim()+"\n");
    }

    private void printHeaders(Game g) {
        String tag = "[Event \"" + g.getHeader("Event") + "\"]";
        pgn.append(tag+"\n");
        tag = "[Site \"" + g.getHeader("Site") + "\"]";
        pgn.append(tag+"\n");
        tag = "[Date \"" + g.getHeader("Date") + "\"]";
        pgn.append(tag+"\n");
        tag = "[Round \"" + g.getHeader("Round") + "\"]";
        pgn.append(tag+"\n");
        tag = "[White \"" + g.getHeader("White") + "\"]";
        pgn.append(tag+"\n");
        tag = "[Black \"" + g.getHeader("Black") + "\"]";
        pgn.append(tag+"\n");
        tag = "[Result \"" + g.getHeader("Result") + "\"]";
        pgn.append(tag+"\n");
        ArrayList<String> allTags = g.getTags();
        for(String tag_i : allTags) {
            //System.out.println("tag out: " +tag_i);
            if(!tag_i.equals("Event") && !tag_i.equals("Site") && !tag_i.equals("Date") && !tag_i.equals("Round")
                    && !tag_i.equals("White") && !tag_i.equals("Black") && !tag_i.equals("Result" ))
            {
                String value_i = g.getHeader(tag_i);
                String tag_val = "[" + tag_i + " \"" + value_i + "\"]";
                //System.out.println("tag out val: " +tag_val);
                pgn.append(tag_val+"\n");
            }
        }
        // add fen string tag if root is not initial position
        Board rootBoard = g.getRootNode().getBoard();
        if(!rootBoard.isInitialPosition()) {
            String tag_fen = "[FEN \"" + rootBoard.fen() + "\"]";
            pgn.append(tag_fen+"\n");
        }
    }

    private void printMove(GameNode node) {
        Board b = node.getParent().getBoard();
        if(b.turn == CONSTANTS.WHITE) {
            String tkn = Integer.toString(b.fullmoveNumber);
            tkn += ". ";
            this.writeToken(tkn);
        }
        else if(this.forceMoveNumber) {
            String tkn = Integer.toString(b.fullmoveNumber);
            tkn += "... ";
            this.writeToken(tkn);
        }
        this.writeToken(node.getSan() + " ");
        this.forceMoveNumber = false;
    }

    private void printNag(int nag) {
        String tkn = "$" + Integer.toString(nag) + " ";
        this.writeToken(tkn);
    }

    private void printResult(int result) {
        String res = "";
        if(result == CONSTANTS.RES_WHITE_WINS) {
            res += "1-0";
        } else if(result == CONSTANTS.RES_BLACK_WINS) {
            res += "0-1";
        } else if(result == CONSTANTS.RES_DRAW) {
            res += "1/2-1/2";
        } else {
            res += "*";
        }
        this.writeToken(res + " ");
    }

    private void beginVariation() {
        this.variationDepth++;
        String tkn = "( ";
        this.writeToken(tkn);
        this.forceMoveNumber = true;
    }

    private void endVariation() {
        this.variationDepth--;
        String tkn = ") ";
        this.writeToken(tkn);
        this.forceMoveNumber = true;
    }

    private void printComment(String comment) {
        String temp_c = comment;
        String write = "{ " + temp_c.replace("}","").trim() + " } ";
        this.writeToken(write);
        //this->forceMoveNumber = false;
    }

    private void printGameContent(GameNode g) {

        Board b = g.getBoard();

        // first write mainline move, if there are variations
        int cntVar = g.getVariations().size();
        if(cntVar > 0) {
            GameNode mainVariation = g.getVariation(0);
            //System.out.println(g.getBoard());
            this.printMove(mainVariation);
            // write nags
            for(Integer ni : mainVariation.getNags()) {
                this.printNag(ni);
            }
            // write comments
            if(!mainVariation.getComment().isEmpty()) {
                this.printComment(mainVariation.getComment());
            }
        }

        // now handle all variations (sidelines)
        for(int i=1;i<cntVar;i++) {
            // first create variation start marker, and print the move
            GameNode var_i = g.getVariation(i);
            this.beginVariation();
            this.printMove(var_i);
            // next print nags
            for(Integer ni : var_i.getNags()) {
                this.printNag(ni);
            }
            // finally print comments
            if(!var_i.getComment().isEmpty()) {
                this.printComment(var_i.getComment());
            }

            // recursive call for all children
            this.printGameContent(var_i);

            // print variation end
            this.endVariation();
        }

        // finally do the mainline
        if(cntVar > 0) {
            GameNode mainVariation = g.getVariation(0);
            this.printGameContent(mainVariation);
        }
    }

    public String printGame(Game g) {

        this.reset();

        this.printHeaders(g);

        this.writeLine("");
        GameNode root = g.getRootNode();

        // special case if the root node has
        // a comment before the actual game starts
        if(!root.getComment().isEmpty()) {
            this.printComment(root.getComment());
        }

        this.printGameContent(root);
        this.printResult(g.getResult());
        this.pgn.append(this.currentLine.toString());

        return this.pgn.toString();
    }

    public void writeGame(Game g, String filename) {

        BufferedWriter out = null;
        String pgn = this.printGame(g);
        try {
            out = Files.newBufferedWriter(Path.of(filename));
            out.write(pgn);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
             if(out != null) {
                 try {
                     out.close();
                 } catch (IOException e) {
                     e.printStackTrace();
                 }
             }
        }
    }

}

