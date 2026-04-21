/* JFXChess - A Chess Graphical User Interface
 * Copyright (C) 2020-2026 Dominik Klein
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

import org.asdfjkl.jfxchess.lib.CONSTANTS;
import org.asdfjkl.jfxchess.lib.GameNode;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class View_Moves extends JEditorPane {

    Model_JFXChess model;
    Controller_UI controller_UI;
    Controller_Board controller_Board;

    public View_Moves(Model_JFXChess model,
                      Controller_UI controller_UI,
                      Controller_Board controller_Board) {

        this.model = model;
        this.controller_UI = controller_UI;
        this.controller_Board = controller_Board;

        // set up formatting
        HTMLEditorKit kit = new HTMLEditorKit();
        StyleSheet css = kit.getStyleSheet();
        css.addRule("body { font-family: sans-serif; }");
        css.addRule(
                "a { " +
                        "text-decoration: none; " +
                        "font-weight: normal; " +
                        "color: #333333; " +
                        "}"
        );
        setEditorKit(kit);
        setEditable(false);
        setFocusable(false);
        setContentType("text/html");

        // left click
        addHyperlinkListener(e -> {
            onLinkClick(e);
        });

        // right click
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    handleRightClick(e);
                }
            }
        });
    }



    public void onLinkClick(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            String id;
            if (e.getURL() != null)
                id = e.getURL().getRef();
            else
                id = e.getDescription().replace("#", "");
            controller_Board.goToNode(Integer.parseInt(id));
        }
    }

    private void handleRightClick(MouseEvent e) {
        JEditorPane pane = (JEditorPane) e.getSource();
        int pos = pane.viewToModel2D(e.getPoint());

        if (pos >= 0) {
            HTMLDocument doc = (HTMLDocument) pane.getDocument();
            Element element = doc.getCharacterElement(pos);
            AttributeSet attrs = element.getAttributes();

            AttributeSet anchor =
                    (AttributeSet) attrs.getAttribute(HTML.Tag.A);

            if (anchor != null) {
                String href = (String) anchor.getAttribute(HTML.Attribute.HREF);

                if (href != null) {
                    int nodeId = Integer.parseInt(href.substring(1));
                    model.goToNode(nodeId);
                    showContextMenu(e, nodeId);
                }
            }
        }
    }

    private void showContextMenu(MouseEvent e, int nodeId) {
        JPopupMenu contextMenu = new JPopupMenu();

        JMenuItem addEditComment = new JMenuItem("Add/Edit Comment");
        contextMenu.add(addEditComment);
        JMenuItem deleteComment = new JMenuItem("Delete Comment");
        contextMenu.add(deleteComment);

        JMenu moveAnnotation = new JMenu("Move Annotation");
        JMenuItem blunder = new JMenuItem("?? Blunder");
        JMenuItem mistake = new JMenuItem("? Mistake");
        JMenuItem dubiousMove = new JMenuItem("?! Dubious Move");
        JMenuItem interestingMove = new JMenuItem("!? Interesting Move");
        JMenuItem goodMove = new JMenuItem("! Good Move");
        JMenuItem brilliantMove = new JMenuItem("!! Brilliant Move");
        JMenuItem noMoveAnnotation = new JMenuItem("No Move Annotation");
        moveAnnotation.add(blunder);
        moveAnnotation.add(mistake);
        moveAnnotation.add(dubiousMove);
        moveAnnotation.add(interestingMove);
        moveAnnotation.add(goodMove);
        moveAnnotation.add(brilliantMove);
        moveAnnotation.add(noMoveAnnotation);
        contextMenu.add(moveAnnotation);

        JMenu posAnnotation = new JMenu("Position Annotation");
        JMenuItem unclear = new JMenuItem("∞ Unclear");
        JMenuItem drawish = new JMenuItem("= Equal");
        JMenuItem slightAdvantageWhite = new JMenuItem("⩲ Slight Advantage White");
        JMenuItem slightAdvantageBlack = new JMenuItem("⩱ Slight Advantage Black");
        JMenuItem advantageWhite = new JMenuItem("+- Advantage White");
        JMenuItem advantageBlack = new JMenuItem("-+ Advantage Black");
        JMenuItem noPosAnnotation = new JMenuItem("No Position Annotation");
        posAnnotation.add(unclear);
        posAnnotation.add(drawish);
        posAnnotation.add(slightAdvantageWhite);
        posAnnotation.add(slightAdvantageBlack);
        posAnnotation.add(advantageWhite);
        posAnnotation.add(advantageBlack);
        posAnnotation.add(noPosAnnotation);
        contextMenu.add(posAnnotation);

        JMenuItem removeAnnotation = new JMenuItem("Remove Annotations");
        contextMenu.add(removeAnnotation);
        contextMenu.addSeparator();

        JMenuItem moveVariantUp = new JMenuItem("Move Variant Up");
        JMenuItem moveVariantDown = new JMenuItem("Move Variant Down");
        JMenuItem deleteVariant = new JMenuItem("Delete Variant");
        JMenuItem deleteFromHere = new JMenuItem("Delete From Here");
        contextMenu.add(moveVariantUp);
        contextMenu.add(moveVariantDown);
        contextMenu.add(deleteVariant);
        contextMenu.add(deleteFromHere);
        contextMenu.addSeparator();

        JMenuItem deleteAllComments = new JMenuItem("Delete All Comments");
        JMenuItem deleteAllVariants = new JMenuItem("Delete All Variants");
        contextMenu.add(deleteAllComments);
        contextMenu.add(deleteAllVariants);

        contextMenu.show(e.getComponent(), e.getX(), e.getY());

        addEditComment.addActionListener(controller_UI.editComment());
        deleteComment.addActionListener(controller_UI.deleteComment(nodeId));
        blunder.addActionListener(controller_UI.addMoveAnnotation(nodeId, CONSTANTS.NAG_BLUNDER));
        mistake.addActionListener(controller_UI.addMoveAnnotation(nodeId, CONSTANTS.NAG_MISTAKE));
        dubiousMove.addActionListener(controller_UI.addMoveAnnotation(nodeId, CONSTANTS.NAG_DUBIOUS_MOVE));
        interestingMove.addActionListener(controller_UI.addMoveAnnotation(nodeId, CONSTANTS.NAG_SPECULATIVE_MOVE));
        goodMove.addActionListener(controller_UI.addMoveAnnotation(nodeId, CONSTANTS.NAG_GOOD_MOVE));
        brilliantMove.addActionListener(controller_UI.addMoveAnnotation(nodeId, CONSTANTS.NAG_BRILLIANT_MOVE));
        noMoveAnnotation.addActionListener(controller_UI.removeMoveAnnotations(nodeId));

        unclear.addActionListener(controller_UI.addPosAnnotation(nodeId, CONSTANTS.NAG_DRAWISH_POSITION));
        drawish.addActionListener(controller_UI.addPosAnnotation(nodeId, CONSTANTS.NAG_DRAWISH_POSITION));
        slightAdvantageWhite.addActionListener(controller_UI.addPosAnnotation(nodeId, CONSTANTS.NAG_WHITE_SLIGHT_ADVANTAGE));
        slightAdvantageBlack.addActionListener(controller_UI.addPosAnnotation(nodeId, CONSTANTS.NAG_BLACK_SLIGHT_ADVANTAGE));
        advantageWhite.addActionListener(controller_UI.addPosAnnotation(nodeId, CONSTANTS.NAG_WHITE_DECISIVE_ADVANTAGE));
        advantageBlack.addActionListener(controller_UI.addPosAnnotation(nodeId, CONSTANTS.NAG_BLACK_DECISIVE_ADVANTAGE));
        noPosAnnotation.addActionListener(controller_UI.removePosAnnotations(nodeId));
        removeAnnotation.addActionListener(controller_UI.removeMoveAndPosAnnotations(nodeId));

        moveVariantUp.addActionListener(controller_UI.moveVariantUp(nodeId));
        moveVariantDown.addActionListener(controller_UI.moveVariantDown(nodeId));
        deleteVariant.addActionListener(controller_UI.deleteVariant(nodeId));
        deleteFromHere.addActionListener(controller_UI.deleteFromHere(nodeId));

        deleteAllComments.addActionListener(controller_UI.deleteAllComments());
        deleteAllVariants.addActionListener(controller_UI.deleteAllVariants());

    }

}
