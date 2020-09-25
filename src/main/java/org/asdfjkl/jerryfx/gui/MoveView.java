package org.asdfjkl.jerryfx.gui;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.asdfjkl.jerryfx.lib.GameNode;
import org.asdfjkl.jerryfx.lib.HtmlPrinter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import java.util.ArrayList;

public class MoveView implements StateChangeListener {

    private WebView webView;
    //private WebEngine webEngine;
    GameModel gameModel;
    HtmlPrinter htmlPrinter;
    int currentlyMarkedNode = -1;
    int x;
    int y;

    int rightClickedNode = -1;

    /*
    String jsIsInView =
            "function isScrolledIntoView(elem)\n" +
                    "{\n" +
                    "    var docViewTop = $(window).scrollTop();\n" +
                    "    var docViewBottom = docViewTop + $(window).height();\n" +
                    "\n" +
                    "    var elemTop = $(elem).offset().top;\n" +
                    "    var elemBottom = elemTop + $(elem).height();\n" +
                    "\n" +
                    "    return ((elemBottom <= docViewBottom) && (elemTop >= docViewTop));\n" +
                    "}";
*/
    String jsIsInView = "function isScrolledIntoView(el) {\n"+
        "var rect = el.getBoundingClientRect();\n"+
        "var elemTop = rect.top; \n"+
        "var elemBottom = rect.bottom; \n"+
        "var isVisible = (elemTop >= 0) && (elemBottom <= window.innerHeight); \n"+
        "return isVisible; \n"+
    "} ";

    public MoveView(GameModel gameModel) {
        this.webView = new WebView();
        //webView.resize(320,200);
        webView.setMinWidth(1);
        webView.setMaxWidth(Double.MAX_VALUE);
        webView.setMaxHeight(Double.MAX_VALUE);
        //webEngine = webView.getEngine();
        webView.getEngine().setUserStyleSheetLocation(getClass().getClassLoader().getResource("webview/style.css").toString());

        webView.setContextMenuEnabled(false);
        createContextMenu(webView);

        this.gameModel = gameModel;
        this.htmlPrinter = new HtmlPrinter();

        //String foo = "<html><body><a href=#213 id=\"bar\">foo</a></body></html>";
        //webEngine.loadContent(foo);

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
        MenuItem noMoveAnnoation = new MenuItem("No Move Annotation");
        moveAnnotation.getItems().addAll(blunder, mistake, dubiousMove, interestingMove, goodMove, brilliantMove, noMoveAnnoation);

        Menu posAnnotation = new Menu("Position Annotation");
        MenuItem unclear = new MenuItem("∞ Unclear");
        MenuItem drawish = new MenuItem("= Drawish");
        MenuItem slightAdvantageWhite = new MenuItem("+/= Slight Advantage White");
        MenuItem slightAdvantageBlack = new MenuItem("=/+ Slight Advantage Black");
        MenuItem advantageWhite = new MenuItem("+- Advantage White");
        MenuItem advantageBlack = new MenuItem("-+ Advantage Black");
        MenuItem noPosAnnotation = new MenuItem("No Position Annotation");
        posAnnotation.getItems().addAll(unclear, slightAdvantageWhite, slightAdvantageBlack, advantageWhite, advantageBlack, noPosAnnotation);

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

        noMoveAnnoation.setOnAction(e -> {
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
                System.out.println( webView.getEngine().executeScript("document.elementFromPoint("
                        +e.getX()
                        +"," +  e.getY()+").tagName;"));
                JSObject object = (JSObject) webView.getEngine().executeScript("document.elementFromPoint("
                        +e.getX()
                        +"," +  e.getY()+");");
                //int clickedNodeId = Integer.parseInt(e.getTarget().toString().substring(1));
                //GameNode nextCurrent = gameModel.getGame().findNodeById(clickedNodeId);
                System.out.println("got: n" + object);
                try {
                    int clickedNodeId = Integer.parseInt(object.toString().substring(1));
                    System.out.println("which results in node nr: "+clickedNodeId);
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

    }

    private void onDeleteComment() {

    }

    private void onAddBlunderNAG() {
        try {
            if(rightClickedNode >= 0) {
                GameNode nextCurrent = gameModel.getGame().findNodeById(rightClickedNode);
                nextCurrent.addNag(4);
                gameModel.triggerStateChange();
            }
        } catch (IllegalArgumentException e) {
        // nextCurrent wasn't found
        }
    }

    private void onAddMistakeNAG() {

    }

    private void onAddDubiousMoveNAG() {

    }

    private void onAddInterestingMoveNAG() {

    }

    private void onAddGoodMoveNAG() {

    }

    private void onBrilliantMoveNAG() {

    }

    private void onNoMoveAnnotation() {

    }

    private void onAddUnclearNAG() {

    }

    private void onAddDrawishNAG() {

    }

    private void onAddSlightAdvantageWhiteNAG() {

    }

    private void onAddSlightAdvantageBlackNAG() {

    }

    private void onAddAdvantageWhite() {

    }

    private void onAddAdvantageBlack() {

    }

    private void onNoPositionAnnotation() {

    }

    private void onRemoveAnnotation() {

    }

    private void onVariantUp() {

    }

    private void onVariantDown() {

    }

    private void onDeleteVariant() {

    }

    private void onDeleteFromHere() {

    }

    private void onDeleteAllComments() {

    }

    private void onDeleteAllVariants() {

    }

    public WebView getWebView() {
        return webView;
    }

    //public void updateView() {
        //estGetElement();
    //    String html = htmlPrinter.printGame(gameModel.getGame());
    //    webEngine.loadContent(html);
    //}

    public void addEventListener() {

        webView.getEngine().getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
            public void changed(ObservableValue ov, State oldState, State newState) {
                if (newState == Worker.State.SUCCEEDED) {
                    // note next classes are from org.w3c.dom domain
                    EventListener listener = new EventListener() {
                        public void handleEvent(Event ev) {
                            //System.out.println(ev.getType());
                            //var e = Window.e || e;
                            //System.out.println(ev.getTarget());
                            try {
                                int clickedNodeId = Integer.parseInt(ev.getTarget().toString().substring(1));
                                GameNode nextCurrent = gameModel.getGame().findNodeById(clickedNodeId);
                                System.out.println("got: n" + Integer.toString(clickedNodeId));
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

    //public void testGetElement() {
        //var foo = webEngine.getDocument().getElementById("bar");
    //    String html = htmlPrinter.printGame(gameModel.getGame());
    //    webEngine.loadContent(html);

        //System.out.println(html);
    //}

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
        boolean focused = (Boolean) webView.getEngine().executeScript(jsString);
        //System.out.println(focused);
        return focused;
    }

    private void updateMarkedNode() {
        // remove marker from old node
        if(currentlyMarkedNode >= 0) {
            Element htmlOldNode = webView.getEngine().getDocument().getElementById("n" + currentlyMarkedNode);
            if(htmlOldNode != null) {
                htmlOldNode.removeAttribute("class");
            }
        }
        // add marker to current node
        int currentNodeId = this.gameModel.getGame().getCurrentNode().getId();
        Element htmlCurrent = webView.getEngine().getDocument().getElementById("n" + currentNodeId);
        if (htmlCurrent != null) {
            htmlCurrent.setAttribute("class", "current");
            //System.out.println(htmlCurrent.toString());
            if(!hasFocus(currentNodeId)) {
                scrollToNode(currentNodeId);
            }
        }
        // special case: if we are at the root, make sure that the node below root (if it exists) is visible
        if(this.gameModel.getGame().getCurrentNode() == this.gameModel.getGame().getRootNode()) {
            if(this.gameModel.getGame().getRootNode().hasChild()) {
                int childId = this.gameModel.getGame().getRootNode().getVariation(0).getId();
                Element htmlBelowRoot = webView.getEngine().getDocument().getElementById("n" + childId);
                if (htmlBelowRoot != null) {
                    if(!hasFocus(childId)) {
                        scrollToNode(childId);
                    }
                }
            }
        }
        //scrollToNode(currentNodeId);
        currentlyMarkedNode = currentNodeId;
    }


    @Override
    public void stateChange() {
        // if tree was changed, we need to update the webview
        if(gameModel.getGame().isTreeChanged()) {
            // remember scrollbar position
            x = getVScrollValue();
            y = getVScrollValue();
            // load new html
            String htmlBody = htmlPrinter.printGame(gameModel.getGame());
            String htmlDoc = "<html><head><script>"+
                    this.jsIsInView + "</script></head><body>" +
                    htmlBody +
                    "</body></html>";
            //String htmlDoc = "<html><head></head><body>" +
            //        htmlBody +
            //        "</body></html>";
            //System.out.println(htmlBody);
            webView.getEngine().loadContent(htmlDoc);

        } else {
            // otherwise:
            // remove marking of old node
            // add marking of current node
            // (scroll to node)
            updateMarkedNode();
        }
    }

    public void goForward() {
        /*

            if(this->gameModel->getGame()->getCurrentNode()->hasVariations()) {
        DialogNextMove *d = new DialogNextMove(this->gameModel->getGame()->getCurrentNode(), this->parentWidget());
        //bool answer = d->exec();
        if(d->exec() == QDialog::Accepted) {
            int variation_index = d->selectedIndex;
            this->gameModel->getGame()->goToChild(variation_index);
        }
        delete d;
    } else {
        this->gameModel->getGame()->goToMainLineChild();
    }

         */
        ArrayList<GameNode> variations = this.gameModel.getGame().getCurrentNode().getVariations();
        if(variations.size() > 1) {
            ArrayList<String> nextMoves = new ArrayList<>();
            for(GameNode varI : variations) {
                nextMoves.add(varI.getSan());
            }
            int variationIdx = DialogNextMove.show(nextMoves);
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
        //System.out.println("tirgger");
        this.gameModel.triggerStateChange();
    }

    public void seekToRoot() {
        this.gameModel.getGame().goToRoot();
        this.gameModel.triggerStateChange();
    }

}
