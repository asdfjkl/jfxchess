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


#include <QPainter>
#include <QMouseEvent>
#include <QKeySequence>
#include "dialog_promotion.h"
#include "chess/board.h"
#include "viewController/piece_images.h"
#include <QDebug>
#include <QDesktopWidget>
#include <QApplication>
#include "assert.h"
#include "various/resource_finder.h"

DialogPromotion::DialogPromotion(bool color, QWidget *parent) :
    QDialog(parent)

{
    this->setWindowTitle(this->tr("Promotion"));
    this->resizeTo(0.15);

    int h = this->size().height() - (2*this->border);
    int w = (this->size().width()-(2*this->border))/4;
    // piece size
    this->pieceSize = h;
    if(w < h) {
        this->pieceSize = w;
    }
    this->color = color;
    this->promotesTo = chess::QUEEN;
    this->selectedIndex = 0;

    this->img = new PieceImages(ResourceFinder::getPath());
}

void DialogPromotion::resizeTo(float ratio) {

    int height = 0;
    int width = 0;
    this->border = 0;
    if(this->parentWidget() != 0) {
        int w_height = this->parentWidget()->size().height();
        this->border = w_height * 0.01;
        height = w_height * ratio + (2*this->border);
        width = (w_height * ratio) * 4 + (2* this->border);
    } else {
        QDesktopWidget *desktop = qApp->desktop();
        QSize availableSize = desktop->availableGeometry().size();
        int w_height = availableSize.height();
        this->border = w_height * 0.01;
        height = w_height * (ratio*0.6) + (2*this->border);
        width = (w_height * (ratio*0.6) * 4) + (2* this->border);
    }
    QSize newSize( width, height );
    this->resize(newSize);
}

void DialogPromotion::paintEvent(QPaintEvent *) {
    QPainter* painter = new QPainter();
    painter->begin(this);
    // draw images
    int h = this->size().height() - (2*this->border);
    int w = (this->size().width()-(2*this->border))/4;
    // piece size
    this->pieceSize = h;
    if(w < h) {
        this->pieceSize = w;
    }
    int s = this->pieceSize;

    QColor lightBlue2 = QColor(166,188,231);
    painter->setBrush(lightBlue2);
    for(int i=0;i<4;i++) {
        if(this->selectedIndex == i) {
            painter->drawRect(this->border+i*s,this->border,s,s);
        }
        painter->drawImage(this->border+i*s, this->border,
                           *this->img->getPieceImage(this->piecetype_by_idx(i),this->color,s));
    }
    painter->end();
    delete painter;
}

void DialogPromotion::mousePressEvent(QMouseEvent *m) {
    this->selectedIndex = (m->x() - this->border) / this->pieceSize;
    this->update();
}

void DialogPromotion::mouseReleaseEvent(QMouseEvent *) {
    this->promotesTo = this->piecetype_by_idx(this->selectedIndex);
    this->done(QDialog::Accepted);
}

void DialogPromotion::keyPressEvent(QKeyEvent *e) {
    int key = e->key();
    if(key == Qt::Key_Left) {
        this->selectedIndex--;
        if(this->selectedIndex < 0) {
            this->selectedIndex = 0;
        }
        this->update();
    } else if(key == Qt::Key_Right) {
        this->selectedIndex++;
        if(this->selectedIndex > 3) {
            this->selectedIndex = 3;
        }
        this->update();
    } else if(key == Qt::Key_Return) {
        this->promotesTo = this->piecetype_by_idx(this->selectedIndex);
        this->update();
        this->done(QDialog::Accepted);
    }
}

uint8_t DialogPromotion::piecetype_by_idx(int idx) {
    assert(idx >= 0 && idx <= 3);
    uint8_t piece;
    switch(idx) {
        case 0:
            piece = chess::QUEEN;
            break;
        case 1:
            piece = chess::ROOK;
            break;
        case 2:
            piece = chess::BISHOP;
            break;
        case 3:
            piece = chess::KNIGHT;
            break;
    }
    return piece;
}
