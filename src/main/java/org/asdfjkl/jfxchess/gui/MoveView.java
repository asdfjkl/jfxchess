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

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.asdfjkl.jfxchess.lib.CONSTANTS;
import org.asdfjkl.jfxchess.lib.GameNode;
import org.asdfjkl.jfxchess.lib.HtmlPrinter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import java.util.ArrayList;

public class MoveView implements StateChangeListener {

    private final WebView webView;
    final GameModel gameModel;
    final HtmlPrinter htmlPrinter;
    int currentlyMarkedNode = -1;
    int x;
    int y;

    int rightClickedNode = -1;

    final String jsIsInView = "function isScrolledIntoView(el) {\n"+
            "var rect = el.getBoundingClientRect();\n"+
            "var elemTop = rect.top; \n"+
            "var elemBottom = rect.bottom; \n"+
            "var isVisible = (elemTop >= 0) && (elemBottom <= window.innerHeight); \n"+
            "return isVisible; \n"+
            "} ";

    public MoveView(GameModel gameModel) {
        this.webView = new WebView();
        webView.setMinWidth(1);
        webView.setMaxWidth(Double.MAX_VALUE);
        webView.setMaxHeight(Double.MAX_VALUE);
        try {
            if(gameModel.THEME == gameModel.STYLE_LIGHT) {
                webView.getEngine().setUserStyleSheetLocation(getClass().getClassLoader().getResource("css/webview_style_light.css").toExternalForm());
            } else {
                webView.getEngine().setUserStyleSheetLocation(getClass().getClassLoader().getResource("css/webview_style_dark.css").toExternalForm());
            }
        } catch (NullPointerException e) {
        }

        webView.setContextMenuEnabled(false);
        createContextMenu(webView);

        this.gameModel = gameModel;
        this.htmlPrinter = new HtmlPrinter();

        addEventListener();

    }

    private void createContextMenu(WebView webView) {

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

        webView.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                JSObject object = (JSObject) webView.getEngine().executeScript("document.elementFromPoint("
                        +e.getX()
                        +"," +  e.getY()+");");
                try {
                    int clickedNodeId = Integer.parseInt(object.toString().substring(1));
                    rightClickedNode = clickedNodeId;
                } catch (NumberFormatException nfe) {
                    // click was not on node link, i.e. parseInt failed
                    rightClickedNode = -1;
                } catch (IllegalArgumentException iae) {
                    // nextCurrent wasn't found
                    rightClickedNode = -1;
                }
                contextMenu.show(webView, e.getScreenX(), e.getScreenY());
            } else {
                contextMenu.hide();
            }
        });
    }

    private void onAddEditComment() {
        try {
            if(rightClickedNode >= 0) {
                GameNode selectedNode = gameModel.getGame().findNodeById(rightClickedNode);
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
        } catch (IllegalArgumentException e) {
        }
    }

    private void onDeleteComment() {
        try {
            if(rightClickedNode >= 0) {
                GameNode selectedNode = gameModel.getGame().findNodeById(rightClickedNode);
                selectedNode.setComment("");
                gameModel.getGame().setTreeWasChanged(true);
                gameModel.triggerStateChange();
            }
        } catch (IllegalArgumentException e) {
        }
    }

    private void onAddBlunderNAG() {
        try {
            if(rightClickedNode >= 0) {
                GameNode selectedNode = gameModel.getGame().findNodeById(rightClickedNode);
                selectedNode.addNag(CONSTANTS.NAG_BLUNDER);
                gameModel.getGame().setTreeWasChanged(true);
                gameModel.triggerStateChange();
            }
        } catch (IllegalArgumentException e) {
        }
    }

    private void onAddMistakeNAG() {
        try {
            if(rightClickedNode >= 0) {
                GameNode selectedNode = gameModel.getGame().findNodeById(rightClickedNode);
                selectedNode.addNag(CONSTANTS.NAG_MISTAKE);
                gameModel.getGame().setTreeWasChanged(true);
                gameModel.triggerStateChange();
            }
        } catch (IllegalArgumentException e) {
        }
    }

    private void onAddDubiousMoveNAG() {
        try {
            if(rightClickedNode >= 0) {
                GameNode selectedNode = gameModel.getGame().findNodeById(rightClickedNode);
                selectedNode.addNag(CONSTANTS.NAG_DUBIOUS_MOVE);
                gameModel.getGame().setTreeWasChanged(true);
                gameModel.triggerStateChange();
            }
        } catch (IllegalArgumentException e) {
        }
    }

    private void onAddInterestingMoveNAG() {
        try {
            if(rightClickedNode >= 0) {
                GameNode selectedNode = gameModel.getGame().findNodeById(rightClickedNode);
                selectedNode.addNag(CONSTANTS.NAG_SPECULATIVE_MOVE);
                gameModel.getGame().setTreeWasChanged(true);
                gameModel.triggerStateChange();
            }
        } catch (IllegalArgumentException e) {
        }
    }

    private void onAddGoodMoveNAG() {
        try {
            if(rightClickedNode >= 0) {
                GameNode selectedNode = gameModel.getGame().findNodeById(rightClickedNode);
                selectedNode.addNag(CONSTANTS.NAG_GOOD_MOVE);
                gameModel.getGame().setTreeWasChanged(true);
                gameModel.triggerStateChange();
            }
        } catch (IllegalArgumentException e) {
        }
    }

    private void onBrilliantMoveNAG() {
        try {
            if(rightClickedNode >= 0) {
                GameNode selectedNode = gameModel.getGame().findNodeById(rightClickedNode);
                selectedNode.addNag(CONSTANTS.NAG_BRILLIANT_MOVE);
                gameModel.getGame().setTreeWasChanged(true);
                gameModel.triggerStateChange();
            }
        } catch (IllegalArgumentException e) {
        }
    }

    private void onNoMoveAnnotation() {
        try {
            if(rightClickedNode >= 0) {
                GameNode selectedNode = gameModel.getGame().findNodeById(rightClickedNode);
                selectedNode.removeNagsInRange(0,CONSTANTS.MOVE_ANNOTATION_UPPER_LIMIT);
                gameModel.getGame().setTreeWasChanged(true);
                gameModel.triggerStateChange();
            }
        } catch (IllegalArgumentException e) {
        }
    }

    private void onAddUnclearNAG() {
        try {
            if(rightClickedNode >= 0) {
                GameNode selectedNode = gameModel.getGame().findNodeById(rightClickedNode);
                selectedNode.addNag(CONSTANTS.NAG_UNCLEAR_POSITION);
                gameModel.getGame().setTreeWasChanged(true);
                gameModel.triggerStateChange();
            }
        } catch (IllegalArgumentException e) {
        }
    }

    private void onAddDrawishNAG() {
        try {
            if(rightClickedNode >= 0) {
                GameNode selectedNode = gameModel.getGame().findNodeById(rightClickedNode);
                selectedNode.addNag(CONSTANTS.NAG_DRAWISH_POSITION);
                gameModel.getGame().setTreeWasChanged(true);
                gameModel.triggerStateChange();
            }
        } catch (IllegalArgumentException e) {
        }
    }

    private void onAddSlightAdvantageWhiteNAG() {
        try {
            if(rightClickedNode >= 0) {
                GameNode selectedNode = gameModel.getGame().findNodeById(rightClickedNode);
                selectedNode.addNag(CONSTANTS.NAG_WHITE_SLIGHT_ADVANTAGE);
                gameModel.getGame().setTreeWasChanged(true);
                gameModel.triggerStateChange();
            }
        } catch (IllegalArgumentException e) {
        }
    }

    private void onAddSlightAdvantageBlackNAG() {
        try {
            if(rightClickedNode >= 0) {
                GameNode selectedNode = gameModel.getGame().findNodeById(rightClickedNode);
                selectedNode.addNag(CONSTANTS.NAG_BLACK_SLIGHT_ADVANTAGE);
                gameModel.getGame().setTreeWasChanged(true);
                gameModel.triggerStateChange();
            }
        } catch (IllegalArgumentException e) {
        }
    }

    private void onAddAdvantageWhite() {
        try {
            if(rightClickedNode >= 0) {
                GameNode selectedNode = gameModel.getGame().findNodeById(rightClickedNode);
                selectedNode.addNag(CONSTANTS.NAG_WHITE_DECISIVE_ADVANTAGE);
                gameModel.getGame().setTreeWasChanged(true);
                gameModel.triggerStateChange();
            }
        } catch (IllegalArgumentException e) {
        }
    }

    private void onAddAdvantageBlack() {
        try {
            if(rightClickedNode >= 0) {
                GameNode selectedNode = gameModel.getGame().findNodeById(rightClickedNode);
                selectedNode.addNag(CONSTANTS.NAG_BLACK_DECISIVE_ADVANTAGE);
                gameModel.getGame().setTreeWasChanged(true);
                gameModel.triggerStateChange();
            }
        } catch (IllegalArgumentException e) {
        }
    }

    private void onNoPositionAnnotation() {
        try {
            if(rightClickedNode >= 0) {
                GameNode selectedNode = gameModel.getGame().findNodeById(rightClickedNode);
                selectedNode.removeNagsInRange(CONSTANTS.POSITION_ANNOTATION_LOWER_LIMIT,
                        CONSTANTS.POSITION_ANNOTATION_UPPER_LIMIT);
                gameModel.getGame().setTreeWasChanged(true);
                gameModel.triggerStateChange();
            }
        } catch (IllegalArgumentException e) {
        }
    }

    private void onRemoveAnnotation() {
        try {
            if(rightClickedNode >= 0) {
                GameNode selectedNode = gameModel.getGame().findNodeById(rightClickedNode);
                selectedNode.removeNagsInRange(0,CONSTANTS.POSITION_ANNOTATION_UPPER_LIMIT);
                gameModel.getGame().setTreeWasChanged(true);
                gameModel.triggerStateChange();
            }
        } catch (IllegalArgumentException e) {
        }
    }

    private void onVariantUp() {
        try {
            if(rightClickedNode >= 0) {
                GameNode selectedNode = gameModel.getGame().findNodeById(rightClickedNode);
                gameModel.getGame().moveUp(selectedNode);
                gameModel.getGame().setTreeWasChanged(true);
                gameModel.triggerStateChange();
            }
        } catch (IllegalArgumentException e) {
        }
    }

    private void onVariantDown() {
        try {
            if(rightClickedNode >= 0) {
                GameNode selectedNode = gameModel.getGame().findNodeById(rightClickedNode);
                gameModel.getGame().moveDown(selectedNode);
                gameModel.getGame().setTreeWasChanged(true);
                gameModel.triggerStateChange();
            }
        } catch (IllegalArgumentException e) {
        }
    }

    private void onDeleteVariant() {
        try {
            if(rightClickedNode >= 0) {
                GameNode selectedNode = gameModel.getGame().findNodeById(rightClickedNode);
                gameModel.getGame().delVariant(selectedNode);
                gameModel.getGame().setTreeWasChanged(true);
                gameModel.triggerStateChange();
            }
        } catch (IllegalArgumentException e) {
        }
    }

    private void onDeleteFromHere() {
        try {
            if(rightClickedNode >= 0) {
                GameNode selectedNode = gameModel.getGame().findNodeById(rightClickedNode);
                gameModel.getGame().delBelow(selectedNode);
                gameModel.getGame().setTreeWasChanged(true);
                gameModel.triggerStateChange();
            }
        } catch (IllegalArgumentException e) {
        }

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

    public WebView getWebView() {
        return webView;
    }

    public void addEventListener() {

        webView.getEngine().getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
            public void changed(ObservableValue ov, State oldState, State newState) {
                if (newState == Worker.State.SUCCEEDED) {
                    // note next classes are from org.w3c.dom domain
                    EventListener listener = new EventListener() {
                        public void handleEvent(Event ev) {
                            //var e = Window.e || e;
                            try {
                                int clickedNodeId = Integer.parseInt(ev.getTarget().toString().substring(1));
                                GameNode nextCurrent = gameModel.getGame().findNodeById(clickedNodeId);
                                gameModel.getGame().setCurrent(nextCurrent);
                                gameModel.triggerStateChange();
                            } catch (NumberFormatException e) {
                                // click was not on node link, i.e. parseInt failed
                            } catch (IllegalArgumentException e) {
                                // nextCurrent wasn't found
                            }
                        }
                    };

                    scrollTo(x, y);
                    updateMarkedNode();

                    Document doc = webView.getEngine().getDocument();
                    ((EventTarget) doc).addEventListener("click", listener, false);
                    ((EventTarget) doc).addEventListener("auxclick", listener, false);
                }
            }
        });

    }

    public void scrollTo(int x, int y) {
        webView.getEngine().executeScript("window.scrollTo(" + x + ", " + y + ")");
    }

    public int getVScrollValue() {
        return (Integer) webView.getEngine().executeScript("document.body.scrollTop");
    }

    public int getHScrollValue() {
        return (Integer) webView.getEngine().executeScript("document.body.scrollLeft");
    }

    public int getVScrollMax() {
        return (Integer) webView.getEngine().executeScript("document.body.scrollWidth");
    }

    public int getHScrollMax() {
        return (Integer) webView.getEngine().executeScript("document.body.scrollHeight");
    }

    private void scrollToNode(int nodeId) {
        String jsString = "document.getElementById('n"+ nodeId+"').scrollIntoView()";
        webView.getEngine().executeScript(jsString);
    }

    private boolean hasFocus(int nodeId) {
        String jsString = "isScrolledIntoView(document.getElementById('n"+ nodeId+"'))";
        webView.getEngine().executeScript(jsString);
        return (Boolean) webView.getEngine().executeScript(jsString);
    }

    private void updateMarkedNode() {
        // remove marker from old node
        if(currentlyMarkedNode >= 0) {
            if(webView.getEngine().getDocument() != null) {
                Element htmlOldNode = webView.getEngine().getDocument().getElementById("n" + currentlyMarkedNode);
                if (htmlOldNode != null) {
                    htmlOldNode.removeAttribute("class");
                }
            }
        }
        // add marker to current node
        int currentNodeId = this.gameModel.getGame().getCurrentNode().getId();
        if(webView.getEngine().getDocument() != null) {
            Element htmlCurrent = webView.getEngine().getDocument().getElementById("n" + currentNodeId);
            if (htmlCurrent != null) {
                htmlCurrent.setAttribute("class", "current");
                if (!hasFocus(currentNodeId)) {
                    scrollToNode(currentNodeId);
                }
            }
            // special case: if we are at the root, make sure that the node below root (if it exists) is visible
            if(this.gameModel.getGame().getCurrentNode() == this.gameModel.getGame().getRootNode()) {
                if (this.gameModel.getGame().getRootNode().hasChild()) {
                    int childId = this.gameModel.getGame().getRootNode().getVariation(0).getId();
                    Element htmlBelowRoot = webView.getEngine().getDocument().getElementById("n" + childId);
                    if (htmlBelowRoot != null) {
                        if (!hasFocus(childId)) {
                            scrollToNode(childId);
                        }
                    }
                }
            }
        }
        currentlyMarkedNode = currentNodeId;
    }


    @Override
    public void stateChange() {
        // if tree was changed, we need to update the webview
        // we also need to do so, if header was changed, since the
        // result might have changed
        if(gameModel.getGame().isTreeChanged()
            || gameModel.getGame().isHeaderChanged()) {
            // remember scrollbar position
            x = getVScrollValue();
            y = getVScrollValue();
            // load new html
            String htmlBody = htmlPrinter.printGame(gameModel.getGame());
            String htmlDoc = "<html><head><script>"+
                    this.jsIsInView + "</script></head><body>" +
                    htmlBody +
                    "</body></html>";
            webView.getEngine().loadContent(htmlDoc);
            gameModel.getGame().setTreeWasChanged(false);
        } else {
            // otherwise:
            // remove marking of old node
            // add marking of current node
            // (scroll to node)
            updateMarkedNode();
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
