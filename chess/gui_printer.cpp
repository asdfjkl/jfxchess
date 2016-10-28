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


#include "gui_printer.h"
#include <QStringList>
#include <QHashIterator>
#include <iostream>
#include <QFile>
#include <QTextStream>
#include <QDebug>
#include <assert.h>
#include "game.h"
#include "pgn_printer.h"
#include "pgn_reader.h"

namespace chess {

GuiPrinter::GuiPrinter()
{
    this->pgn = QString("");
    this->currentLine = QString("");
    this->variationDepth = 0;
    this->forceMoveNumber = true;
    this->newLine = false;
}

void GuiPrinter::reset() {
    this->pgn = QString("");
    this->currentLine = QString("");
    this->variationDepth = 0;
    this->forceMoveNumber = false;
    this->newLine = false;
}

void GuiPrinter::writeToken(const QString &token) {
    this->pgn.append(token);
}

QString GuiPrinter::printGame(Game *g) {

    this->reset();

    GameNode *root = g->getRootNode();

    this->writeToken("<p style=\"line-height:110%\">");

    // special case if the root node has
    // a comment before the actual game starts
    if(!root->getComment().isEmpty()) {
        this->printComment(root->getComment());
    }
    this->printGameContent(root, true);
    this->printResult(g->getResult());
    this->pgn.append(this->currentLine);

    return pgn;

}

void GuiPrinter::printMove(GameNode *node) { //int nodeId, Board *b, Move *m) {
        int nodeId = node->getId();
        Board *b = node->getParent()->getBoard();
        assert(b != 0);
        QString s_nodeId = QString::number(nodeId);
        this->writeToken("<a name=\"");
        this->writeToken(s_nodeId);
        this->writeToken("\" href=\"#");
        this->writeToken(s_nodeId);
        this->writeToken("\">");

        if(b->turn == WHITE) {
            QString tkn = QString::number(b->fullmove_number);
            tkn.append(QString(". "));
            this->writeToken(tkn);
        }
        else if(this->forceMoveNumber) {
            QString tkn = QString::number(b->fullmove_number);
            tkn.append(QString("... "));
            this->writeToken(tkn);
        }
        this->writeToken(node->getSan());
        this->writeToken("</a> ");

    this->forceMoveNumber = false;
    this->newLine = false;
}

void GuiPrinter::printNag(int nag) {
    switch(nag) {
    case NAG_GOOD_MOVE:
        this->writeToken("! ");
        break;
    case NAG_MISTAKE:
        this->writeToken("? ");
        break;
    case NAG_BRILLIANT_MOVE:
        this->writeToken("!! ");
        break;
    case NAG_BLUNDER:
        this->writeToken("?? ");
        break;
    case NAG_SPECULATIVE_MOVE:
        this->writeToken("!? ");
        break;
    case NAG_DUBIOUS_MOVE:
        this->writeToken("?! ");
        break;
    case NAG_FORCED_MOVE:
        this->writeToken("□ ");
        break;
    case NAG_DRAWISH_POSITION:
        this->writeToken("= ");
        break;
    case NAG_UNCLEAR_POSITION:
        this->writeToken("∞ ");
        break;
    case NAG_WHITE_MODERATE_ADVANTAGE:
        this->writeToken("+/= ");
        break;
    case NAG_BLACK_MODERATE_ADVANTAGE:
        this->writeToken("-/= ");
        break;
    case NAG_WHITE_DECISIVE_ADVANTAGE:
        this->writeToken("+- ");
        break;
    case NAG_BLACK_DECISIVE_ADVANTAGE:
        this->writeToken("-+ ");
        break;
    case NAG_WHITE_ZUGZWANG:
        this->writeToken("⨀	 ");
        break;
    case NAG_BLACK_ZUGZWANG:
        this->writeToken("⨀	 ");
        break;
    case NAG_WHITE_HAS_ATTACK:
        this->writeToken("↑ ");
        break;
    case NAG_BLACK_HAS_ATTACK:
        this->writeToken("↑ ");
        break;
    default:
        QString tkn = QString("$").append(QString::number(nag)).append(" ");
        this->writeToken(tkn);
    }
}

void GuiPrinter::printResult(int result) {
    QString res = "";
    if(result == RES_WHITE_WINS) {
        res = QString("1-0");
    } else if(result == RES_BLACK_WINS) {
        res = QString("0-1");
    } else if(result == RES_DRAW) {
        res = QString("1/2-1/2");
    } else {
        res = QString("*");
    }
    this->writeToken(res.append(" "));
}

void GuiPrinter::beginVariation() {

    this->variationDepth++;
    QString tkn = QString("");
    if(this->variationDepth == 1) {
        // if we just started a new line due to
        // ending a previous variation directly below
        // mainline, we do not need to add another linebreak
        if(this->newLine) {
            tkn = QString("&nbsp;[ ");
        } else {
            tkn = QString("<br>&nbsp;[ ");
        }
        this->writeToken(tkn);
        this->forceMoveNumber = true;
    } else {
        QString tkn = QString("( ");
        this->writeToken(tkn);
        this->forceMoveNumber = true;
    }
    this->newLine = false;
}

void GuiPrinter::endVariation() {
    this->variationDepth--;
    if(this->variationDepth == 0) {
        QString tkn = QString("]<br> ");
        this->writeToken(tkn);
        this->forceMoveNumber = true;
        this->newLine = true;
    } else {
        QString tkn = QString(") ");
        this->writeToken(tkn);
        this->forceMoveNumber = true;
    }
}

void GuiPrinter::printComment(const QString &comment) {
    QString temp_c = QString(comment);
    QString write = QString("{ ").append(temp_c.replace("}","").trimmed()).append(" } ");
    this->writeToken(write);
    //this->forceMoveNumber = false;
}



void GuiPrinter::printGameContent(GameNode* g, bool onMainLine) {

    // first write mainline move, if there are variations
    int cntVar = g->getVariations()->count();
    if(cntVar > 0) {
        if(onMainLine) {
            this->writeToken("<b>");
        }
        GameNode* main_variation = g->getVariation(0);
        this->printMove(main_variation);
        // write nags
        QList<int> *nags = main_variation->getNags();
        for(int j=0;j<nags->count();j++) {
            int n = nags->at(j);
            this->printNag(n);
        }

        if(onMainLine) {
            this->writeToken("</b>");
        }
        // write comments
        if(!main_variation->getComment().isEmpty()) {
            this->printComment(main_variation->getComment());
        }
    }
    // now handle all variations (sidelines)
    for(int i=1;i<cntVar;i++) {
        // first create variation start marker, and print the move
        GameNode *var_i = g->getVariation(i);

        this->beginVariation();
        this->printMove(var_i);

        // next print nags

        QList<int> *nags = var_i->getNags();
        for(int j=0;j<nags->count();j++) {
            int n = nags->at(j);
            this->printNag(n);
        }
        // finally print comments
        if(!var_i->getComment().isEmpty()) {
            this->printComment(var_i->getComment());
        }
        // recursive call for all childs
        this->printGameContent(var_i, false);
        // print variation end
        this->endVariation();
    }

    // finally do the mainline
    if(cntVar > 0) {
        GameNode* main_variation = g->getVariation(0);
        this->printGameContent(main_variation, onMainLine && true);
    }
}


}
