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

import java.util.ArrayList;
import java.util.Collections;

public class GameNode {

    static int id;
    private final int nodeId;
    private Board board = null;
    private Move move = null; // move leading to this node
    private GameNode parent = null;
    private String comment;
    private String sanCache;
    private final ArrayList<GameNode> variations;
    private ArrayList<Integer> nags;

    protected static int initId() {
        return id++;
    }

    public GameNode() {
        this.nodeId = initId();
        this.variations = new ArrayList<GameNode>();
        this.nags = new ArrayList<Integer>();
        //this.board = new Board(true);
        //this.board.resetToStartingPosition();
        this.comment = "";
        this.sanCache = "";
    }

    public int getId() {
        return this.nodeId;
    }

    public Board getBoard() {
        return this.board;
    }

    public void setBoard(Board b) {
        this.board = b;
    }

    public String getSan(Move m) {
        //return this.board.san(m) + "(" + m.getUci() +")";
        return this.board.san(m);
    }

    public String getSan() {
        if(this.sanCache.isEmpty() && this.parent != null) {
            this.sanCache = this.parent.getSan(this.move);
        }
        return this.sanCache;
    }

    public GameNode getParent() {
        return this.parent;
    }

    public Move getMove() {
        return this.move;
    }

    public void setMove(Move m) {
        this.move = m;
    }

    public void setParent(GameNode node) {
        this.parent = node;
    }

    public void setComment(String s) {
        this.comment = s;
    }

    public String getComment() {
        return this.comment;
    }

    public GameNode getVariation(int i) {
        if(this.variations.size() > i) {
            return this.variations.get(i);
        } else {
            throw new IllegalArgumentException("there are only "+this.variations.size() + " variations, but index "+i + "requested");
        }
    }

    public void deleteVariation(int i) {
        if(this.variations.size() > i) {
            this.variations.remove(i);
        } else {
            throw new IllegalArgumentException("there are only "+this.variations.size() + " variations, " +
                    "but index "+i + "requested for deletion");
        }
    }

    public ArrayList<GameNode> getVariations() {
        return this.variations;
    }

    public void addVariation(GameNode node) {
        this.variations.add(node);
    }

    public boolean hasVariations() {
        return this.variations.size() > 1;
    }

    public boolean hasChild() { return  this.variations.size() > 0; }

    public boolean isLeaf() {
        return this.variations.size() == 0;
    }

    public void addNag(int n) {
        if(!nags.contains(n)) {
            this.nags.add(n);
        }
    }

    public ArrayList<Integer> getNags() {
        return this.nags;
    }

    public int getDepth() {
        if(this.parent == null) {
            return 0;
        } else {
            return this.parent.getDepth() + 1;
        }
    }

    public void removeNagsInRange(int start, int stop) {
        ArrayList<Integer> filteredNags = new ArrayList<>();
        for(Integer i : this.nags) {
            if(!(start <= i && i <= stop)) {
                filteredNags.add(i);
            }
        }
        this.nags = filteredNags;
    }

    public void sortNags() {
        Collections.sort(this.nags);
    }

}
