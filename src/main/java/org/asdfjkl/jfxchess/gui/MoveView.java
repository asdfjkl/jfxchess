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

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.asdfjkl.jfxchess.lib.*;

import java.util.ArrayList;

public class MoveView implements StateChangeListener {

    final GameModel gameModel;

    public TextFlow flow;
    private ArrayList<Text> textList;

    int variationDepth;
    boolean forceMoveNumber;
    boolean newLine;

    private Text currentMarkedText;
    private Text newMarkedText;

    private final String STYLE_BASE = "-fx-font-family: Arial, Helvetica, sans-serif; -fx-font-size: 16;";
    private final String STYLE_SIDELINE_MOVE = STYLE_BASE + " -fx-cursor: hand;";
    private final String STYLE_MAINLINE_MOVE = STYLE_BASE + " -fx-font-weight: bold; -fx-cursor: hand;";
    private final String STYLE_MAINLINE = STYLE_BASE + " -fx-font-weight: bold;";
    private final String STYLE_COMMENT = STYLE_BASE + " -fx-fill: -color-fg-muted;";

    public MoveView(GameModel gameModel) {

        this.gameModel = gameModel;

        flow = new TextFlow();
        flow.setLineSpacing(1.1);
        flow.setStyle(//"-fx-border-color: -color-border-muted; " +
                //"-fx-border-width: 1; " +
                //"-fx-border-radius: 6;" +
                //"-fx-background-color: -color-bg-subtle;" +
                //"-fx-background-radius: 6;" +
                "-fx-padding: 8;");
        textList = new ArrayList<Text>();
        textList.clear();
        variationDepth = 0;
        forceMoveNumber = true;
        newLine = false;

        currentMarkedText = null;
        newMarkedText = null;

        ContextMenu contextMenu = createContextMenu();

        // Single handler for the entire TextFlow
        flow.setOnMouseClicked(event -> {
            if (event.getTarget() instanceof Text) {
                Text clickedText = (Text) event.getTarget();
                Object data = clickedText.getUserData();
                if (data instanceof Integer) {
                    int clickedNodeId = (Integer) data;
                    GameNode nextCurrent = gameModel.getGame().findNodeById(clickedNodeId);
                    gameModel.getGame().setCurrent(nextCurrent);
                    newMarkedText = clickedText;
                    gameModel.triggerStateChange();
                    if (event.getButton() == MouseButton.SECONDARY) {
                        contextMenu.show(flow, event.getScreenX(), event.getScreenY());
                    }
                }
            }
            if (event.getButton() != MouseButton.SECONDARY) {
                contextMenu.hide();
            }

        });

    }

    private void reset() {
        textList.clear();
        variationDepth = 0;
        forceMoveNumber = true;
        newLine = false;
    }

    private ContextMenu createContextMenu() {

        MenuItem addEditComment = new MenuItem("Add/Edit Comment");
        MenuItem deleteComment = new MenuItem("Delete Comment");

        Menu moveAnnotation = new Menu("Move Annotation");
        MenuItem blunder = new MenuItem("?? Blunder");
        MenuItem mistake = new MenuItem("? Mistake");
        MenuItem dubiousMove = new MenuItem("?! Dubious Move");
        MenuItem interestingMove = new MenuItem("!? Interesting Move");
        MenuItem goodMove = new MenuItem("! Good Move");
        MenuItem brilliantMove = new MenuItem("!! Brilliant Move");
        MenuItem noMoveAnnotation = new MenuItem("No Move Annotation");
        moveAnnotation.getItems().addAll(blunder, mistake, dubiousMove, interestingMove, goodMove, brilliantMove, noMoveAnnotation);

        Menu posAnnotation = new Menu("Position Annotation");
        MenuItem unclear = new MenuItem("∞ Unclear");
        MenuItem drawish = new MenuItem("= Equal");
        MenuItem slightAdvantageWhite = new MenuItem("⩲ Slight Advantage White");
        MenuItem slightAdvantageBlack = new MenuItem("⩱ Slight Advantage Black");
        MenuItem advantageWhite = new MenuItem("+- Advantage White");
        MenuItem advantageBlack = new MenuItem("-+ Advantage Black");
        MenuItem noPosAnnotation = new MenuItem("No Position Annotation");
        posAnnotation.getItems().addAll(unclear, drawish, slightAdvantageWhite, slightAdvantageBlack, advantageWhite, advantageBlack, noPosAnnotation);

        MenuItem removeAnnotation = new MenuItem("Remove Annotations");
        SeparatorMenuItem separator1 = new SeparatorMenuItem();

        MenuItem moveVariantUp = new MenuItem("Move Variant Up");
        MenuItem moveVariantDown = new MenuItem("Move Variant Down");
        MenuItem deleteVariant = new MenuItem("Delete Variant");
        MenuItem deleteFromHere = new MenuItem("Delete From Here");
        SeparatorMenuItem separator2 = new SeparatorMenuItem();

        MenuItem deleteAllComments = new MenuItem("Delete All Comments");
        MenuItem deleteAllVariants = new MenuItem("Delete All Variants");

        addEditComment.setOnAction(e -> {
            onAddEditComment();
        });

        deleteComment.setOnAction(e -> {
            onDeleteComment();
        });

        blunder.setOnAction(e -> {
            onAddBlunderNAG();
        });

        mistake.setOnAction(e -> {
            onAddMistakeNAG();
        });

        dubiousMove.setOnAction(e -> {
            onAddDubiousMoveNAG();
        });

        interestingMove.setOnAction(e -> {
            onAddInterestingMoveNAG();
        });

        goodMove.setOnAction(e -> {
            onAddGoodMoveNAG();
        });

        brilliantMove.setOnAction(e -> {
            onBrilliantMoveNAG();
        });

        noMoveAnnotation.setOnAction(e -> {
            onNoMoveAnnotation();
        });

        unclear.setOnAction(e -> {
            onAddUnclearNAG();
        });

        drawish.setOnAction(e -> {
            onAddDrawishNAG();
        });

        slightAdvantageWhite.setOnAction(e -> {
            onAddSlightAdvantageWhiteNAG();
        });

        slightAdvantageBlack.setOnAction(e -> {
            onAddSlightAdvantageBlackNAG();
        });

        advantageWhite.setOnAction(e -> {
            onAddAdvantageWhite();
        });

        advantageBlack.setOnAction(e -> {
            onAddAdvantageBlack();
        });

        noPosAnnotation.setOnAction(e -> {
            onNoPositionAnnotation();
        });

        removeAnnotation.setOnAction(e -> {
            onRemoveAnnotation();
        });

        moveVariantUp.setOnAction(e -> {
            onVariantUp();
        });

        moveVariantDown.setOnAction(e -> {
            onVariantDown();
        });

        deleteVariant.setOnAction(e -> {
            onDeleteVariant();
        });

        deleteFromHere.setOnAction(e -> {
            onDeleteFromHere();
        });

        deleteAllComments.setOnAction(e -> {
            onDeleteAllComments();
        });

        deleteAllVariants.setOnAction(e -> {
            onDeleteAllVariants();
        });

        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(addEditComment, deleteComment, moveAnnotation, posAnnotation, removeAnnotation, separator1,
                moveVariantUp, moveVariantDown, deleteVariant, deleteFromHere, separator2,
                deleteAllComments, deleteAllVariants);

        return contextMenu;

    }

    private void onAddMistakeNAG() {
        GameNode selectedNode = gameModel.getGame().getCurrentNode();
        selectedNode.addNag(CONSTANTS.NAG_MISTAKE);
        gameModel.getGame().setTreeWasChanged(true);
        gameModel.triggerStateChange();
    }

    private void onAddDubiousMoveNAG() {
        GameNode selectedNode = gameModel.getGame().getCurrentNode();
        selectedNode.addNag(CONSTANTS.NAG_DUBIOUS_MOVE);
        gameModel.getGame().setTreeWasChanged(true);
        gameModel.triggerStateChange();
    }

    private void onAddInterestingMoveNAG() {
        GameNode selectedNode = gameModel.getGame().getCurrentNode();
        selectedNode.addNag(CONSTANTS.NAG_SPECULATIVE_MOVE);
        gameModel.getGame().setTreeWasChanged(true);
        gameModel.triggerStateChange();
    }

    private void onAddGoodMoveNAG() {
        GameNode selectedNode = gameModel.getGame().getCurrentNode();
        selectedNode.addNag(CONSTANTS.NAG_GOOD_MOVE);
        gameModel.getGame().setTreeWasChanged(true);
        gameModel.triggerStateChange();
    }

    private void onBrilliantMoveNAG() {
        GameNode selectedNode = gameModel.getGame().getCurrentNode();
        selectedNode.addNag(CONSTANTS.NAG_BRILLIANT_MOVE);
        gameModel.getGame().setTreeWasChanged(true);
        gameModel.triggerStateChange();
    }

    private void onNoMoveAnnotation() {
        GameNode selectedNode = gameModel.getGame().getCurrentNode();
        selectedNode.removeNagsInRange(0,CONSTANTS.MOVE_ANNOTATION_UPPER_LIMIT);
        gameModel.getGame().setTreeWasChanged(true);
        gameModel.triggerStateChange();
    }

    private void onAddUnclearNAG() {
        GameNode selectedNode = gameModel.getGame().getCurrentNode();
        selectedNode.addNag(CONSTANTS.NAG_UNCLEAR_POSITION);
        gameModel.getGame().setTreeWasChanged(true);
        gameModel.triggerStateChange();
    }

    private void onAddDrawishNAG() {
        GameNode selectedNode = gameModel.getGame().getCurrentNode();
        selectedNode.addNag(CONSTANTS.NAG_DRAWISH_POSITION);
        gameModel.getGame().setTreeWasChanged(true);
        gameModel.triggerStateChange();
    }

    private void onAddSlightAdvantageWhiteNAG() {
        GameNode selectedNode = gameModel.getGame().getCurrentNode();
        selectedNode.addNag(CONSTANTS.NAG_WHITE_SLIGHT_ADVANTAGE);
        gameModel.getGame().setTreeWasChanged(true);
        gameModel.triggerStateChange();
    }

    private void onAddSlightAdvantageBlackNAG() {
        GameNode selectedNode = gameModel.getGame().getCurrentNode();
        selectedNode.addNag(CONSTANTS.NAG_BLACK_SLIGHT_ADVANTAGE);
        gameModel.getGame().setTreeWasChanged(true);
        gameModel.triggerStateChange();
    }

    private void onAddAdvantageWhite() {
        GameNode selectedNode = gameModel.getGame().getCurrentNode();
        selectedNode.addNag(CONSTANTS.NAG_WHITE_DECISIVE_ADVANTAGE);
        gameModel.getGame().setTreeWasChanged(true);
        gameModel.triggerStateChange();
    }

    private void onAddAdvantageBlack() {
        GameNode selectedNode = gameModel.getGame().getCurrentNode();
        selectedNode.addNag(CONSTANTS.NAG_BLACK_DECISIVE_ADVANTAGE);
        gameModel.getGame().setTreeWasChanged(true);
        gameModel.triggerStateChange();
    }

    private void onNoPositionAnnotation() {
        GameNode selectedNode = gameModel.getGame().getCurrentNode();
        selectedNode.removeNagsInRange(CONSTANTS.POSITION_ANNOTATION_LOWER_LIMIT,
                CONSTANTS.POSITION_ANNOTATION_UPPER_LIMIT);
        gameModel.getGame().setTreeWasChanged(true);
        gameModel.triggerStateChange();
    }

    private void onRemoveAnnotation() {
        GameNode selectedNode = gameModel.getGame().getCurrentNode();
        selectedNode.removeNagsInRange(0,CONSTANTS.POSITION_ANNOTATION_UPPER_LIMIT);
        gameModel.getGame().setTreeWasChanged(true);
        gameModel.triggerStateChange();
    }

    private void onVariantUp() {
        GameNode selectedNode = gameModel.getGame().getCurrentNode();
        gameModel.getGame().moveUp(selectedNode);
        gameModel.getGame().setTreeWasChanged(true);
        gameModel.triggerStateChange();
    }

    private void onVariantDown() {
        GameNode selectedNode = gameModel.getGame().getCurrentNode();
        gameModel.getGame().moveDown(selectedNode);
        gameModel.getGame().setTreeWasChanged(true);
        gameModel.triggerStateChange();
    }

    private void onDeleteVariant() {
        GameNode selectedNode = gameModel.getGame().getCurrentNode();
        gameModel.getGame().delVariant(selectedNode);
        gameModel.getGame().setTreeWasChanged(true);
        gameModel.triggerStateChange();
    }

    private void onDeleteFromHere() {
        GameNode selectedNode = gameModel.getGame().getCurrentNode();
        gameModel.getGame().delBelow(selectedNode);
        gameModel.getGame().setTreeWasChanged(true);
        gameModel.triggerStateChange();
    }

    private void onDeleteAllComments() {
        gameModel.getGame().removeAllComments();
        gameModel.getGame().setTreeWasChanged(true);
        gameModel.triggerStateChange();
    }

    private void onDeleteAllVariants() {
        gameModel.getGame().removeAllVariants();
        gameModel.getGame().setTreeWasChanged(true);
        gameModel.triggerStateChange();
    }

    private void onAddBlunderNAG() {
        GameNode selectedNode = gameModel.getGame().getCurrentNode();
        selectedNode.addNag(CONSTANTS.NAG_BLUNDER);
        gameModel.getGame().setTreeWasChanged(true);
        gameModel.triggerStateChange();
    }

    private void onDeleteComment() {
        GameNode selectedNode = gameModel.getGame().getCurrentNode();
        selectedNode.setComment("");
        gameModel.getGame().setTreeWasChanged(true);
        gameModel.triggerStateChange();
    }

    private void onAddEditComment() {
        GameNode selectedNode = gameModel.getGame().getCurrentNode();
        DialogEnterComment dlg = new DialogEnterComment();
        boolean accepted = dlg.show(gameModel.getStageRef(), selectedNode.getComment());
        if(accepted) {
            String newComment = dlg.textArea.getText();
            // filter invalid stuff, like { } etc.
            newComment = newComment.replace('\n', ' ');
            newComment = newComment.replace('{', ' ');
            newComment = newComment.replace('}', ' ');
            newComment = newComment.replace('\r', ' ');
            selectedNode.setComment(newComment);
        }
        gameModel.getGame().setTreeWasChanged(true);
        gameModel.triggerStateChange();
    }


    private void renderMove(GameNode node, boolean onMainline) {
        int nodeId = node.getId();
        Board b = node.getParent().getBoard();

        String toRender = "";
        if (b.turn == CONSTANTS.WHITE) {
            String tkn = Integer.toString(b.fullmoveNumber);
            tkn += ". ";
            Text tTkn = new Text(tkn);
            if (onMainline) {
                tTkn.setStyle(STYLE_MAINLINE);
            } else {
                tTkn.setStyle(STYLE_BASE);
            }
            //textList.add(tTkn);
            toRender += tkn;
        } else if (this.forceMoveNumber) {
            String tkn = Integer.toString(b.fullmoveNumber);
            tkn += "... ";
            Text tTkn = new Text(tkn);
            if (onMainline) {
                tTkn.setStyle(STYLE_MAINLINE);
            } else {
                tTkn.setStyle(STYLE_BASE);
            }
            //textList.add(tTkn);
            toRender += tkn;
        }
        toRender += node.getSan();
        //Text san = new Text(node.getSan());
        Text san = new Text(toRender);
        san.setUserData(nodeId);
        //san.setStyle("-fx-cursor: hand;");
        //san.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-cursor: hand;");
        if (onMainline) {
            san.setStyle(STYLE_MAINLINE_MOVE);
        } else {
            san.setStyle(STYLE_SIDELINE_MOVE);
        }
        textList.add(san);

        this.forceMoveNumber = false;
        this.newLine = false;
    }

    private void renderNag(int nag, boolean onMainline) {
        String tkn = "";
        switch (nag) {
            case 1:
                tkn += "!";
                break;
            case 2:
                tkn += "?";
                break;
            case 3:
                tkn += "!!";
                break;
            case 4:
                tkn += "??";
                break;
            case 5:
                tkn += "!?";
                break;
            case 6:
                tkn += "?!";
                break;
            case 10:
                tkn += "=";
                break;
            case 13:
                tkn += "∞";
                break;
            case 14:
                tkn += "⩲";
                break;
            case 15:
                tkn += "⩱";
                break;
            case 16:
                tkn += "±";
                break;
            case 17:
                tkn += "∓";
                break;
            case 18:
                tkn += "+−";
                break;
            case 19:
                tkn += "−+";
                break;
            case 22:
                tkn += "⨀";
                break;
            case 23:
                tkn += "⨀";
                break;
            case 132:
                tkn += "⇆";
                break;
            case 133:
                tkn += "⇆";
                break;
            default:
                tkn += "$ " + nag;

        }
        Text tTkn = new Text(tkn);
        if (onMainline) {
            tTkn.setStyle(STYLE_MAINLINE);
        } else {
            tTkn.setStyle(STYLE_BASE);
        }
        textList.add(tTkn);
    }

    private void renderResult(int result) {
        String res = "";
        if (result == CONSTANTS.RES_WHITE_WINS) {
            res += "1-0";
        } else if (result == CONSTANTS.RES_BLACK_WINS) {
            res += "0-1";
        } else if (result == CONSTANTS.RES_DRAW) {
            res += "1/2-1/2";
        } else {
            res += "*";
        }
        Text tTkn = new Text(res + " ");
        tTkn.setStyle(STYLE_MAINLINE);
        textList.add(tTkn);
    }

    private void beginVariation() {
        variationDepth++;

        if (variationDepth == 1) {
            // if we just started a new line due to
            // ending a previous variation directly below
            // mainline, we do not need to add another linebreak
            String tkn = "";
            if (newLine) {
                tkn += " [ ";
            } else {
                tkn += "\n [ ";
            }
            Text tTkn = new Text(tkn);
            tTkn.setStyle(STYLE_BASE);
            textList.add(tTkn);
            forceMoveNumber = true;
        } else {
            String tkn = "( ";
            Text tTkn = new Text(tkn);
            tTkn.setStyle(STYLE_BASE);
            textList.add(tTkn);
            forceMoveNumber = true;
        }
        newLine = false;
    }

    private void endVariation() {
        variationDepth--;
        if (variationDepth == 0) {
            String tkn = "]\n ";
            Text tTkn = new Text(tkn);
            tTkn.setStyle(STYLE_BASE);
            textList.add(tTkn);
            forceMoveNumber = true;
            newLine = true;
        } else {
            String tkn = ") ";
            Text tTkn = new Text(tkn);
            tTkn.setStyle(STYLE_BASE);
            textList.add(tTkn);
            forceMoveNumber = true;
        }
    }

    private void renderComment(String comment) {
        String write = " " + comment.replace("}", "").trim() + "  ";
        Text tTkn = new Text(write);
        tTkn.setStyle(STYLE_COMMENT);
        textList.add(tTkn);
    }

    private void renderGameContent(GameNode g, boolean onMainLine) {

        Board b = g.getBoard();

        // first write mainline move, if there are variations
        int cntVar = g.getVariations().size();
        if (cntVar > 0) {
            if (onMainLine) {
                // set bold font
            }
            GameNode mainVariation = g.getVariation(0);
            renderMove(mainVariation, onMainLine);
            // write nags
            for (Integer ni : mainVariation.getNags()) {
                renderNag(ni, onMainLine);
            }
            textList.add(new Text(" "));
            if (onMainLine) {
                // end bold font
            }
            // write comments
            if (!mainVariation.getComment().isEmpty()) {
                renderComment(mainVariation.getComment());
            }
        }

        // now handle all variations (sidelines)
        for (int i = 1; i < cntVar; i++) {
            // first create variation start marker, and print the move
            GameNode var_i = g.getVariation(i);
            beginVariation();
            renderMove(var_i, false);
            // next print nags
            for (Integer ni : var_i.getNags()) {
                renderNag(ni, false);
            }
            textList.add(new Text(" "));
            // finally print comments
            if (!var_i.getComment().isEmpty()) {
                renderComment(var_i.getComment());
            }

            // recursive call for all children
            renderGameContent(var_i, false);

            // print variation end
            this.endVariation();
        }

        // finally do the mainline
        if (cntVar > 0) {
            GameNode mainVariation = g.getVariation(0);
            renderGameContent(mainVariation, onMainLine);
        }
    }

    public void renderGame(Game g) {

        this.reset();

        GameNode root = g.getRootNode();

        // special case if the root node has
        // a comment before the actual game starts
        if (!root.getComment().isEmpty()) {
            renderComment(root.getComment());
        }

        renderGameContent(root, true);
        renderResult(g.getResult());

        flow.getChildren().clear();
        flow.getChildren().addAll(textList);

    }


    @Override
    public void stateChange() {

        if (gameModel.getGame().isTreeChanged()) {
            renderGame(gameModel.getGame());
            gameModel.getGame().setTreeWasChanged(false);
        }

        // kind of inefficient, as we have to loop over all text objects
        // and then do string search & replace over each much, but fast enough
        // in practice; also puts all tracking where we are in the game tree
        // to the model (gameModel)

        // loop over all text
        for(Text text : textList) {
            Object data = text.getUserData();
            if (data instanceof Integer) {
                // remove any marking if text does not represent the current node
                if((Integer) data != gameModel.getGame().getCurrentNode().getId()) {
                    String highlightCss = text.getStyle();
                    String removeHighlight = highlightCss.replace("-fx-fill: -color-danger-emphasis;", "");
                    text.setStyle(removeHighlight);
                }
                // add highlight if current node does not contain it already
                if((Integer) data == gameModel.getGame().getCurrentNode().getId()) {
                    String css = text.getStyle();
                    if(!css.contains("-fx-fill: -color-danger-emphasis;")) {
                        text.setStyle(css + " -fx-fill: -color-danger-emphasis;");
                    }
                }
            }
        }
    }

    public void goForward() {

        ArrayList<GameNode> variations = this.gameModel.getGame().getCurrentNode().getVariations();
        if(variations.size() > 1) {
            ArrayList<String> nextMoves = new ArrayList<>();
            for(GameNode varI : variations) {
                nextMoves.add(varI.getSan());
            }
            int variationIdx = DialogNextMove.show(gameModel.getStageRef(), nextMoves);
            if(variationIdx >= 0) {
                this.gameModel.getGame().goToChild(variationIdx);
                this.gameModel.triggerStateChange();
            }
        } else {
            this.gameModel.getGame().goToMainLineChild();
            this.gameModel.triggerStateChange();
        }
    }

    public void goBack() {
        this.gameModel.getGame().goToParent();
        this.gameModel.triggerStateChange();
    }

    public void seekToEnd() {
        this.gameModel.getGame().goToLeaf();
        this.gameModel.triggerStateChange();
    }

    public void seekToRoot() {
        this.gameModel.getGame().goToRoot();
        this.gameModel.triggerStateChange();
    }


}