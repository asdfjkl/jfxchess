/* Jerry - A Chess Graphical User Interface
 * Copyright (C) 2014-2016 Dominik Klein
 * Copyright (C) 2015-2016 Karl Josef Klein
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


#include "moveviewcontroller.h"
#include "chess/pgn_printer.h"
#include "chess/gui_printer.h"
#include "chess/game.h"
#include <QDebug>
#include <QMenu>
#include <QTextDocument>
#include <QTextFragment>
#include "dialogs/dialog_nextmove.h"
#include "dialogs/dialog_plaintext.h"
#include "chess/pgn_reader.h"

MoveViewController::MoveViewController(GameModel *gameModel, QWidget *parent) :
    QTextBrowser(parent)
{
    this->pos_rightclick = new QPoint(-1,-1);
    this->gameModel = gameModel;
    this->document = new QTextDocument();
    this->viewport()->setCursor(Qt::ArrowCursor);
    this->guiPrinter = new chess::GuiPrinter();
    this->setContextMenuPolicy(Qt::CustomContextMenu);

    connect(this, &MoveViewController::anchorClicked, this, &MoveViewController::onAnchorClicked);
    connect(this, &MoveViewController::customContextMenuRequested, this, &MoveViewController::showContextMenu);
}

void MoveViewController::showContextMenu(const QPoint &pos) {
    QMenu *menu = new QMenu(this);

    this->pos_rightclick->setX(pos.x());
    this->pos_rightclick->setY(pos.y());

    QMenu *subMoveAnnotation = new QMenu(menu);
    subMoveAnnotation->setTitle(this->tr("Move Annotation"));

    QAction *annBlunder = subMoveAnnotation->addAction(this->tr("?? Blunder"));
    QAction *annMistake = subMoveAnnotation->addAction(this->tr("? Mistake"));
    QAction *annDubious = subMoveAnnotation->addAction(this->tr("?! Dubious Move"));
    QAction *annInteresting = subMoveAnnotation->addAction(this->tr("!? Interesting Move"));
    QAction *annGood = subMoveAnnotation->addAction(this->tr("! Good Move"));
    QAction *annBrilliant = subMoveAnnotation->addAction(this->tr("!! Brilliant Move"));
    QAction *annEmpty = subMoveAnnotation->addAction(this->tr("No Annotation"));

    connect(annBlunder, &QAction::triggered, this, [this] { annotation(chess::NAG_BLUNDER, 0, 9); });
    connect(annMistake, &QAction::triggered, this, [this] { annotation(chess::NAG_MISTAKE, 0, 9); });
    connect(annDubious, &QAction::triggered, this, [this] { annotation(chess::NAG_DUBIOUS_MOVE, 0, 9); });
    connect(annInteresting, &QAction::triggered, this, [this] { annotation(chess::NAG_SPECULATIVE_MOVE, 0, 9); });
    connect(annGood, &QAction::triggered, this, [this] { annotation(chess::NAG_GOOD_MOVE, 0, 9); });
    connect(annBrilliant, &QAction::triggered, this, [this] { annotation(chess::NAG_BRILLIANT_MOVE, 0, 9); });
    connect(annEmpty, &QAction::triggered, this, [this] { annotation(0, 0, 9); });

    QMenu *subPosAnnotation = new QMenu(menu);
    subPosAnnotation->setTitle(this->tr("Position Annotation"));

    QAction *posUnclear = subPosAnnotation->addAction(this->tr("∞ Unclear"));
    QAction *posDrawish = subPosAnnotation->addAction(this->tr("= Drawish"));
    QAction *posWhiteModBetter = subPosAnnotation->addAction(this->tr("+/= Slight Advantage White"));
    QAction *posBlackModBetter = subPosAnnotation->addAction(this->tr("-/= Slight Advantage Black"));
    QAction *posWhiteMuchBetter = subPosAnnotation->addAction(this->tr("+- White Better"));
    QAction *posBlackMuchBetter = subPosAnnotation->addAction(this->tr("-+ Black Better"));
    QAction *posNoAnnotation = subPosAnnotation->addAction(this->tr("No Annotation"));

    connect(posUnclear, &QAction::triggered, this, [this] { annotation(chess::NAG_UNCLEAR_POSITION, 9, 23); });
    connect(posDrawish, &QAction::triggered, this, [this] { annotation(chess::NAG_DRAWISH_POSITION, 9, 23); });
    connect(posWhiteModBetter, &QAction::triggered, this, [this] { annotation(chess::NAG_WHITE_MODERATE_ADVANTAGE, 9, 23); });
    connect(posBlackModBetter, &QAction::triggered, this, [this] { annotation(chess::NAG_BLACK_MODERATE_ADVANTAGE, 9, 23); });
    connect(posWhiteMuchBetter, &QAction::triggered, this, [this] { annotation(chess::NAG_WHITE_DECISIVE_ADVANTAGE, 9, 23); });
    connect(posBlackMuchBetter, &QAction::triggered, this, [this] { annotation(chess::NAG_BLACK_DECISIVE_ADVANTAGE, 9, 23); });
    connect(posNoAnnotation, &QAction::triggered, this, [this] { annotation(0, 9, 23); });

    posUnclear->setData(chess::NAG_UNCLEAR_POSITION);
    posDrawish->setData(chess::NAG_DRAWISH_POSITION);
    posWhiteModBetter->setData(chess::NAG_WHITE_MODERATE_ADVANTAGE);
    posBlackModBetter->setData(chess::NAG_BLACK_MODERATE_ADVANTAGE);
    posWhiteMuchBetter->setData(chess::NAG_WHITE_DECISIVE_ADVANTAGE);
    posBlackMuchBetter->setData(chess::NAG_BLACK_DECISIVE_ADVANTAGE);
    posNoAnnotation->setData(0);

    QAction *addComment = menu->addAction(this->tr("Add/Edit Comment"));
    QAction *delComment = menu->addAction(this->tr("Delete Comment"));

    menu->addMenu(subMoveAnnotation);
    menu->addMenu(subPosAnnotation);

    connect(addComment, &QAction::triggered, this, &MoveViewController::addComment);
    connect(delComment, &QAction::triggered, this, &MoveViewController::deleteComment);

    QAction *remAnnotation = menu->addAction(this->tr("Remove Annotations"));
    connect(remAnnotation, &QAction::triggered, this, &MoveViewController::removeAnnotations);

    menu->addSeparator();

    QAction *varUp = menu->addAction(this->tr("Move Variant Up"));
    QAction *varDown = menu->addAction(this->tr("Move Variant Down"));
    QAction *delVar = menu->addAction(this->tr("Delete Variant"));
    QAction *delHere = menu->addAction(this->tr("Delete From Here"));

    connect(varUp, &QAction::triggered, this, &MoveViewController::variantUp);
    connect(varDown, &QAction::triggered, this, &MoveViewController::variantDown);
    connect(delVar, &QAction::triggered, this, &MoveViewController::deleteVariant);
    connect(delHere, &QAction::triggered, this, &MoveViewController::deleteFromHere);


    menu->addSeparator();

    QAction *delAllComments = menu->addAction(this->tr("Delete All Comments"));
    QAction *delAllVariants = menu->addAction(this->tr("Delete All Variants"));

    connect(delAllComments, &QAction::triggered, this, &MoveViewController::removeAllComments);
    connect(delAllVariants, &QAction::triggered, this, &MoveViewController::removeAllVariants);

    menu->exec(QCursor::pos());
}

chess::GameNode* MoveViewController::findNodeOnRightclick() {
    QString s_node = this->anchorAt(*this->pos_rightclick);
    // first try to find the node on which the user right-clicked on
    if(s_node.count() > 1) {
        s_node = s_node.mid(1,s_node.count()-1);
    }
    int nodeNumber = s_node.toInt();
    return this->gameModel->getGame()->findNodeById(nodeNumber);
}


void MoveViewController::annotation(int nag, int min, int max) {
    try {
        chess::GameNode *node = this->findNodeOnRightclick();
        // first remove old relevant nags, which
        // are in min<n<max
        node->removeNagsInRange(min, max);
        node->appendNag(nag);
        node->sortNags();
        this->gameModel->getGame()->setResult(true);
        this->gameModel->getGame()->setTreeWasChanged(true);
        this->gameModel->triggerStateChange();
    } catch(std::invalid_argument &e) {
        // if the node with the supplied id cannot be found
        // something is just wrong. just ignore and do nothing
    }
}

void MoveViewController::removeAnnotations() {
    this->annotation(0,0,120);
}

void MoveViewController::onAnchorClicked(const QUrl& link) {
    QString s_node = link.toString();
    if(s_node.count() > 1) {
        s_node = s_node.mid(1,s_node.count()-1);
    }
    int nodeNumber = s_node.toInt();
    try {
        chess::GameNode *node = this->gameModel->getGame()->findNodeById(nodeNumber);
        this->gameModel->getGame()->setCurrent(node);
        this->gameModel->triggerStateChange();
    } catch(std::invalid_argument &e) {
        // if the node with the supplied id cannot be found
        // something is just wrong. just ignore and do nothing
    }
}


void MoveViewController::addComment() {
    try {
        chess::GameNode *node = this->findNodeOnRightclick();
        DialogPlainText *d = new DialogPlainText();
        d->plainTextEdit->setPlainText(node->getComment());
        bool answer = d->exec();
        if(answer) {
            node->setComment(d->savedText);
            this->gameModel->getGame()->setTreeWasChanged(true);
            this->gameModel->triggerStateChange();
        }

    } catch(std::invalid_argument &e) {
        // if the node with the supplied id cannot be found
        // something is just wrong. just ignore and do nothing
    }


}


void MoveViewController::deleteComment() {
    try {
        chess::GameNode *node = this->findNodeOnRightclick();
        QString e = QString("");
        node->setComment(e);
        this->gameModel->getGame()->setTreeWasChanged(true);
        this->gameModel->triggerStateChange();
    } catch(std::invalid_argument &e) {
        // if the node with the supplied id cannot be found
        // something is just wrong. just ignore and do nothing
    }
}


void MoveViewController::variantUp() {
    QString s_node = this->anchorAt(*this->pos_rightclick);
    // first try to find the node on which the user right-clicked on
    if(s_node.count() > 1) {
        s_node = s_node.mid(1,s_node.count()-1);
    }
    int nodeNumber = s_node.toInt();
    try {
        chess::GameNode *node = this->gameModel->getGame()->findNodeById(nodeNumber);
        this->gameModel->getGame()->moveUp(node);
        this->gameModel->getGame()->setTreeWasChanged(true);
        this->gameModel->triggerStateChange();
    } catch(std::invalid_argument &e) {
        // if the node with the supplied id cannot be found
        // something is just wrong. just ignore and do nothing
    }

}


void MoveViewController::variantDown() {
    try {
        chess::GameNode *node = this->findNodeOnRightclick();
        this->gameModel->getGame()->moveDown(node);
        this->gameModel->getGame()->setTreeWasChanged(true);
        this->gameModel->triggerStateChange();
    } catch(std::invalid_argument &e) {
        // if the node with the supplied id cannot be found
        // something is just wrong. just ignore and do nothing
    }
}

void MoveViewController::deleteVariant() {
    try {
        chess::GameNode *node = this->findNodeOnRightclick();
        this->gameModel->getGame()->delVariant(node);
        this->gameModel->getGame()->setTreeWasChanged(true);
        this->gameModel->triggerStateChange();
    } catch(std::invalid_argument &e) {
        // if the node with the supplied id cannot be found
        // something is just wrong. just ignore and do nothing
    }
}

void MoveViewController::deleteFromHere() {
    try {
        chess::GameNode *node = this->findNodeOnRightclick();
        this->gameModel->getGame()->delBelow(node);
        this->gameModel->getGame()->setTreeWasChanged(true);
        this->gameModel->triggerStateChange();
    } catch(std::invalid_argument &e) {
        // if the node with the supplied id cannot be found
        // something is just wrong. just ignore and do nothing
    }

}

void MoveViewController::removeAllComments() {
    this->gameModel->getGame()->removeAllComments();
    this->gameModel->getGame()->setTreeWasChanged(true);
    this->gameModel->triggerStateChange();
}

void MoveViewController::removeAllVariants() {
    this->gameModel->getGame()->removeAllVariants();
    this->gameModel->getGame()->setTreeWasChanged(true);
    this->gameModel->triggerStateChange();
}

void MoveViewController::setSource(const QUrl&)
{
}

void MoveViewController::selectAndMarkAnchor(const QString& link)
{
    QTextBlock block = this->document->begin();
    while(block != this->document->end())
    {
        QTextBlock::iterator it;
        for(it = block.begin(); !it.atEnd(); ++it)
        {
            QTextFragment fragment = it.fragment();
            if(!fragment.isValid())
            {
                continue;
            }
            QTextCharFormat format = fragment.charFormat();
            if(format.isAnchor() && format.anchorHref() == link)
            {
                QTextCursor cursor = this->textCursor();
                cursor.setPosition(fragment.position());
                int len = 0;
                bool finished = false;
                // we want to mark (highlight) everything from the
                // start of the anchor until the end of the move
                // the end of the move is indicated by an empty space
                // (there is always an empty space after a move)
                while(!finished && !it.atEnd()) {
                    if(it.fragment().text().startsWith(" ") || len >= 6) {
                        finished = true;
                    } else {
                        len+= it.fragment().text().length();
                        it++;
                    }
                }
                cursor.setPosition(fragment.position() + len, QTextCursor::KeepAnchor);
                setTextCursor(cursor);
                ensureCursorVisible();
                return;
            }
        }
    block = block.next();
    }
}

void MoveViewController::onForwardClick() {
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
    this->gameModel->triggerStateChange();
}

void MoveViewController::onScrollBack() {
    this->gameModel->getGame()->goToParent();
    this->gameModel->triggerStateChange();
}

void MoveViewController::onScrollForward() {
    this->gameModel->getGame()->goToMainLineChild();
    this->gameModel->triggerStateChange();
}

void MoveViewController::onSeekToBeginning() {
    this->gameModel->getGame()->goToRoot();
    this->gameModel->triggerStateChange();
}

void MoveViewController::onSeekToEnd() {
    this->gameModel->getGame()->goToEnd();
    this->gameModel->triggerStateChange();
}

void MoveViewController::onBackwardClick() {
    this->gameModel->getGame()->goToParent();
    this->gameModel->triggerStateChange();
}

void MoveViewController::keyPressEvent(QKeyEvent *e) {
    int key = e->key();
    if(key == Qt::Key_Left) {
        this->onBackwardClick();
    } else if(key == Qt::Key_Right) {
        this->onForwardClick();
    } else if(key == Qt::Key_Home) {
        this->onSeekToBeginning();
    } else if(key == Qt::Key_End) {
        this->onSeekToEnd();
    }

}

void MoveViewController::onStateChange() {

    if(this->gameModel->getGame()->isTreeChanged()) {
        this->document->clear();
        QString format = "a:link { color: #000000; text-decoration: none}";
        if(!this->gameModel->fontStyle->moveWindowFontSize.isEmpty()) {
            format.append(QString(" * { font-size: ").append(this->gameModel->fontStyle->moveWindowFontSize).append("pt; }"));
        }
        this->document->setDefaultStyleSheet(format);
        chess::Game *g = this->gameModel->getGame();
        QString sl = this->guiPrinter->printGame(*g);
        this->document->setHtml(sl);
        this->setDocument(this->document);
        this->gameModel->getGame()->setTreeWasChanged(false);
    }
    if(this->gameModel->getGame()->getCurrentNode()->getId() ==
            this->gameModel->getGame()->getRootNode()->getId()) {
        QTextCursor cursor = this->textCursor();
        cursor.setPosition(0);
        setTextCursor(cursor);
        ensureCursorVisible();
    } else {
        int s_node = this->gameModel->getGame()->getCurrentNode()->getId();
        this->selectAndMarkAnchor(QString("#").append(QString::number((s_node))));
    }
    this->setFocus();
}

