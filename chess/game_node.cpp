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


#include "game_node.h"
#include <QDebug>
#include <iostream>
#include <assert.h>


namespace chess {

int GameNode::id = 0;

GameNode::GameNode() {

    this->variations = new QList<GameNode*>();
    this->board = new Board(true);
    this->comment = QString("");
    this->nags = new QList<int>();
    this->parent = 0;
    this->m = 0;
    this->nodeId = this->initId();
    this->san_cache = QString("");
    this->arrows = new QList<Arrow*>();
    this->coloredFields = new QList<ColoredField*>();
    this->depthCache = 0;
    this->userWasInformedAboutResult = false;

}

GameNode::~GameNode() {
    delete this->m;
    for(int i=0;i<this->arrows->size();i++) {
        delete this->arrows->at(i);
    }
    delete this->arrows;

    for(int i=0;i<this->coloredFields->size();i++) {
        delete this->coloredFields->at(i);
    }
    delete this->coloredFields;
    delete this->nags;
    delete this->board;
    for(int i=0;i<this->variations->size();i++) {
        delete this->variations->at(i);
    }
    this->variations->clear();
    delete this->variations;
}

void GameNode::setMove(Move *m) {
    assert(m!=0);
    this->m = m;
}

int GameNode::getDepth() {
    if(this->parent == 0) {
        return 0;
    } else {
        if(this->depthCache == 0) {
            return this->parent->getDepth() + 1;
        } else {
            return 0;
        }
    }
}

QString GameNode::getSan() {
    if(this->san_cache.isEmpty() && this->parent != 0) {
        Board *b = this->parent->getBoard();
        Move *m = this->m;
        //qDebug() << "node to: " << m->uci_string;
        //QString foo = m->uci();
        //qDebug() << foo;
        this->san_cache = b->san(*m);
    }
    return this->san_cache;
}

int GameNode::getId() {
    return this->nodeId;
}

Move* GameNode::getMove() {
    return this->m;
}

void GameNode::setParent(GameNode *p) {
    this->parent = p;
}

GameNode* GameNode::getParent() {
    //assert(this->parent != 0);
    return this->parent;
}

void GameNode::addNag(int n) {
    this->nags->append(n);
}

QList<int>* GameNode::getNags() {
    return this->nags;
}

void GameNode::setComment(QString &c) {
    this->comment = c;
}

QString GameNode::getComment() {
    return this->comment;
}

Board* GameNode::getBoard() {
    assert(this->board != 0);
    return this->board;
}

void GameNode::setBoard(Board *b) {
    assert(b != 0);
    delete this->board;
    this->board = b;
}


GameNode* GameNode::root() {
    GameNode* root = this;
    while(root->parent!=0) {
        root = root->parent;
    }
    return root;
}

QList<GameNode*>* GameNode::getVariations() {
    return this->variations;
}


GameNode* GameNode::getVariation(int i) {
    assert(this->variations->size() > i);
    return this->variations->at(i);
}

bool GameNode::hasVariations() {
    return this->getVariations()->count() > 1;
}

void GameNode::addVariation(GameNode *g) {
    assert(g != 0);
    this->variations->append(g);
    g->parent = this;
}

QList<Arrow*>* GameNode::getArrows() {
    return this->arrows;
}

QList<ColoredField*>* GameNode::getColoredFields() {
    return this->coloredFields;
}

bool GameNode::isLeaf() {
    if(this->variations->count() == 0) {
        return true;
    } else {
        return false;
    }
}

void GameNode::addOrDelArrow(Arrow *a) {
    bool addArrow = true;
    for(int i=0;i<this->arrows->size();i++) {
        Arrow *ai = this->arrows->at(i);
        if(ai->from.x() == a->from.x() && ai->from.y() == a->from.y()
                && ai->to.x() == a->to.x() && a->to.y() == ai->to.y()) {
            if(a->color == ai->color) {
                this->arrows->removeAt(i);
                addArrow = false;
                break;
            } else {
                this->arrows->removeAt(i);
                break;
            }
        }
    }
    if(addArrow) {
        this->arrows->append(a);
    } else {
        delete a;
    }
}

void GameNode::addOrDelColoredField(ColoredField *c) {
    assert(c != 0);
    bool addField = true;
    for(int i=0;i<this->coloredFields->size();i++) {
        ColoredField *ci = this->coloredFields->at(i);
        if(ci->field.x() == c->field.x() && ci->field.y() == c->field.y()) {
            if(ci->color == c->color) {
                this->coloredFields->removeAt(i);
                addField = false;
                break;
            } else {
                this->coloredFields->removeAt(i);
                break;
            }
        }
    }
    if(addField) {
        this->coloredFields->append(c);
    } else {
        delete c;
    }
}

}
