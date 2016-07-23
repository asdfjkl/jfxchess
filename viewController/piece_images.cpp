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


#include <QtSvg/QSvgRenderer>
#include <QtGui>
#include "piece_images.h"
#include "chess/board.h"
#include "chess/move.h"
#include "colorstyle.h"
#include <QDebug>
#include <assert.h>
#include "various/messagebox.h"
#include "model/game_model.h"

PieceImages::PieceImages(QString resourcePath)
{
    this->resourcePath = QString(resourcePath);

    QString merida = QString("merida");
    QString uscf = QString("uscf");
    QString old = QString("old");

    this->svg_images_merida = new QHash<int, QSvgRenderer*>();
    this->svg_images_old = new QHash<int, QSvgRenderer*>();
    this->svg_images_uscf = new QHash<int, QSvgRenderer*>();

    this->initSvgs(svg_images_merida, merida);
    this->initSvgs(svg_images_old, old);
    this->initSvgs(svg_images_uscf, uscf);

    this->rendered_svg_images_merida = new QHash<QString, QImage*>();
    this->rendered_svg_images_old = new QHash<QString, QImage*>();
    this->rendered_svg_images_uscf = new QHash<QString, QImage*>();

}

void PieceImages::initSvgs(QHash<int, QSvgRenderer*> *svg_images, QString &pieceType) {
    for(int i=1;i<7;i++) {
        for(int j=0;j<2;j++) {
            QString filename = QString(this->resourcePath + (QString("/res/pieces/")));
            filename.append(pieceType);
            filename.append("/");
            if(j == chess::BLACK) {
                filename.append("b");
            } else {
                filename.append("w");
            }
            if(i == chess::PAWN) {
                filename.append("p");
            } else if(i==chess::KNIGHT) {
                filename.append("n");
            } else if(i==chess::BISHOP) {
                filename.append("b");
            } else if(i==chess::ROOK) {
                filename.append("r");
            } else if(i==chess::QUEEN) {
                filename.append("q");
            } else if(i==chess::KING) {
                filename.append("k");
            }
            filename.append(".svg");
            QSvgRenderer *ren = new QSvgRenderer();
            ren->load(filename);
            svg_images->insert((i*10)+j, ren);
        }
    }
}

QImage* PieceImages::getPieceImage(uint8_t piece_type, bool color, int size, int type) {
    QHash<int, QSvgRenderer*> *svg_images;
    QHash<QString, QImage*> *rendered_svg_images;
    if(type == PIECE_STYLE_OLD) {
        svg_images = this->svg_images_old;
        rendered_svg_images = this->rendered_svg_images_old;
    } else if(type == PIECE_STYLE_USCF){
        svg_images = this->svg_images_uscf;
        rendered_svg_images = this->rendered_svg_images_uscf;
    } else {
        svg_images = this->svg_images_merida;
        rendered_svg_images = this->rendered_svg_images_merida;
    }
    QString idx = QString::number(piece_type).append("_").append(QString::number(color));
    idx.append("_").append(QString::number(size));

    if(rendered_svg_images->contains(idx)) {
        return rendered_svg_images->value(idx);
    } else {
        QImage* img = new QImage(size,size,QImage::Format_ARGB32);
        QColor fill = QColor(1,1,1,1);
        img->fill(fill);
        QPainter *painter = new QPainter();
        //qDebug() << "make piece image 3b";
        //qDebug() << "piece type: " << piece_type;
        painter->begin(img);
        if(color == chess::BLACK) {
            QSvgRenderer *ren = svg_images->value((piece_type*10)+1);
            assert(ren != 0);
            ren->render(painter);
        } else {            
            QSvgRenderer *ren = svg_images->value((piece_type*10));
            assert(ren != 0);
            ren->render(painter);
        }
        painter->end();
        delete painter;
        rendered_svg_images->insert(idx,img);
        return img;
    }
}
