#include "chessboard.h"
#include "various/resource_finder.h"
#include "colorstyle.h"
#include "assert.h"
#include <QSizePolicy>
#include <QPainter>
#include <QWindow>
#include <QDebug>
#include <math.h>

Chessboard::Chessboard(QWidget *parent) :
    QWidget(parent)
{
    QSizePolicy policy = QSizePolicy(QSizePolicy::Preferred, QSizePolicy::Preferred);
    this->setSizePolicy(policy);

    this->borderWidth = 12;
    this->style = new ColorStyle();
    this->pieceImages = new PieceImages(ResourceFinder::getPath());
    this->board = new chess::Board(true);

    this->moveSrc = new QPoint(-1,-1);
    this->drawGrabbedPiece = false;
    this->grabbedPiece = new GrabbedPiece{chess::EMPTY,chess::WHITE,-1,-1};
    this->flipBoard = false;

    this->drawGrabbedArrow = false;
    this->arrowGrabColor = new QColor(70,130,0);
    this->grabbedArrow = new chess::Arrow{QPoint(-1,-1), QPoint(-1,-1), *arrowGrabColor};

    this->lastMoveColor = new QColor(200,200,0,100);

    this->arrowGrabColor = new QColor(70,130,0);

    this->currentArrows = 0;
    this->currentColoredFields = 0;

    this->dpr = this->devicePixelRatio();

    this->lastMove = 0;
}

void Chessboard::calculateBoardSize(int *boardSize, int *squareSize) {

    int bSize = this->width();
    if(this->height() < bSize) {
        bSize = this->height();
    }
    int sSize = (bSize-(2*this->borderWidth))/8;
    bSize = 8 * sSize + 2 * this->borderWidth;

    *boardSize = bSize;
    *squareSize = sSize;
}

void Chessboard::setBoard(chess::Board *b) {
    this->board = b;
}

void Chessboard::setArrows(QList<chess::Arrow*> *arrows) {
    this->currentArrows = arrows;
}

void Chessboard::setColoredFields(QList<chess::ColoredField*> *fields) {
    this->currentColoredFields = fields;
}

void Chessboard::setGrabbedArrowFrom(int x, int y) {
    this->grabbedArrow->from.setX(x);
    this->grabbedArrow->from.setY(y);
}

void Chessboard::setGrabbedArrowTo(int x, int y) {
    this->grabbedArrow->to.setX(x);
    this->grabbedArrow->to.setY(y);
}

void Chessboard::paintEvent(QPaintEvent *event) {
    QPainter *painter = new QPainter();
    painter->begin(this);
    this->drawBoard(event, painter);
    painter->end();
}

void Chessboard::resizeEvent(QResizeEvent *) {
    this->setMinimumWidth(this->height());
}

void Chessboard::setColorStyle(ColorStyle *style) {
    this->style = style;
    this->update();
}

ColorStyle* Chessboard::getColorStyle() {
    return this->style;
}

void Chessboard::setFlipBoard(bool onOff) {
    this->flipBoard = onOff;
}

void Chessboard::setGrabbedPiece(int pieceType, int color) {
    this->grabbedPiece->piece_type = pieceType;
    this->grabbedPiece->color = color;
}

void Chessboard::drawBoard(QPaintEvent *, QPainter *painter) {

    // to have no border color when drawing board squares and pieces
    QPen penZero = QPen(Qt::black, 1, Qt::NoPen);
    painter->setPen(penZero);
    painter->setBrush(this->style->borderColor);

    int boardSize = 0;
    int squareSize = 0;
    this->calculateBoardSize(&boardSize, &squareSize);

    painter->drawRect(1,1, boardSize, boardSize);

    int boardOffsetX = this->borderWidth;
    int boardOffsetY = this->borderWidth;

    QColor light = this->style->lightSquare;
    QColor dark = this->style->darkSquare;

    QPixmap pxLight = this->style->lightSquareTexture;
    QPixmap pxDark = this->style->darkSquareTexture;

    if(this->flipBoard) {
        dark = this->style->lightSquare;
        light = this->style->darkSquare;
        pxLight = this->style->darkSquareTexture;
        pxDark = this->style->lightSquareTexture;
    }

    chess::Board* board = this->board;

    for(int i=0;i<8;i++) {
        for(int j=0;j<8;j++) {
            // draw alternatively light and dark squares
            if((j%2 == 0 && i%2==1) || (j%2 == 1 && i%2==0)) {
                if(this->style->boardStyle == BOARD_STYLE_TEXTURE) {
                    painter->setBrush(QBrush(pxLight));
                } else {
                    painter->setBrush(light);
                }
            } else {
                if(this->style->boardStyle == BOARD_STYLE_TEXTURE) {
                    painter->setBrush(QBrush(pxDark));
                } else {
                    painter->setBrush(dark);
                }
            }
            // draw the square
            int x = 0;
            if(this->flipBoard) {
                x = boardOffsetX+((7-i)*squareSize);
            } else {
                x = boardOffsetX+(i*squareSize);
            }
            // drawing coordinates are from top left
            // whereas chess coords are from bottom left
            int y = boardOffsetY+((7-j)*squareSize);
            painter->drawRect(x,y,squareSize,squareSize);
        }
    }

    // draw colored field of last move
    if(this->lastMove != 0) {
        QPoint xyFrom = lastMove->fromAsXY();
        int x = 0;
        int y = 0;
        if(this->flipBoard) {
            x = boardOffsetX+((7-xyFrom.x())*squareSize);
            y = boardOffsetY+((xyFrom.y())*squareSize);
        } else {
            x = boardOffsetX+(xyFrom.x()*squareSize);
            y = boardOffsetY+((7-xyFrom.y())*squareSize);
        }
        painter->setBrush(*this->lastMoveColor);
        painter->drawRect(x,y,squareSize,squareSize);

        QPoint xyTo = lastMove->toAsXY();
        if(this->flipBoard) {
            x = boardOffsetX+((7-xyTo.x())*squareSize);
            y = boardOffsetY+(xyTo.y()*squareSize);
        } else {
            x = boardOffsetX+(xyTo.x()*squareSize);
            y = boardOffsetY+((7-xyTo.y())*squareSize);
        }
        painter->drawRect(x,y,squareSize,squareSize);
    }

    for(int i=0;i<8;i++) {
        for(int j=0;j<8;j++) {
            // get square coords
            int x = 0;
            if(this->flipBoard) {
                x = boardOffsetX+((7-i)*squareSize);
            } else {
                x = boardOffsetX+(i*squareSize);
            }
            // drawing coordinates are from top left
            // whereas chess coords are from bottom left
            int y = boardOffsetY+((7-j)*squareSize);

            // draw the pieces
            uint8_t piece_type = 0;
            bool piece_color = 0;
            if(this->flipBoard) {
                piece_type = board->get_piece_type_at(i,7-j);
                piece_color = board->get_piece_color_at(i,7-j);
            } else {
                piece_type = board->get_piece_type_at(i,j);
                piece_color = board->get_piece_color_at(i,j);
            }
            int pieceStyle = this->style->pieceType;
            if(piece_type != chess::EMPTY)  {
                // skip piece that is currently picked up
                if(!this->flipBoard) {
                    if(!(this->drawGrabbedPiece && i==this->moveSrc->x() && j== this->moveSrc->y())) {
                        QImage *piece_image = this->pieceImages->getPieceImage(piece_type, piece_color, squareSize, this->dpr, pieceStyle);
                        painter->drawImage(x,y,*piece_image);
                    }
                } else {
                    if(!(this->drawGrabbedPiece && (7-i)==this->moveSrc->x() && j == this->moveSrc->y())) {
                        QImage *piece_image = this->pieceImages->getPieceImage(piece_type, piece_color, squareSize, this->dpr, pieceStyle);
                        painter->drawImage(x,y,*piece_image);
                    }
                }
            }
        }
    }

    // draw grabbed piece, if user is currently grabbing
    if(this->drawGrabbedPiece) {
        int offset = squareSize / 2;
        assert(this->grabbedPiece != 0);
        //assert(this->grabbedPiece->x - offset > 0);
        //assert(this->grabbedPiece->y - offset > 0);
        assert(this->pieceImages != 0);
        painter->drawImage(this->grabbedPiece->x - offset, this->grabbedPiece->y - offset,
                           *this->pieceImages->getPieceImage(this->grabbedPiece->piece_type,
                                                             this->grabbedPiece->color, squareSize, this->style->pieceType));
    }


    // draw colored fields
    if(this->currentColoredFields != 0) {
        for(int i=0; i<this->currentColoredFields->size();i++) {
            chess::ColoredField *ci = this->currentColoredFields->at(i);
            int x = 0;
            int y = 0;
            if(this->flipBoard) {
                x = boardOffsetX+((7-ci->field.x())*squareSize);
                y = boardOffsetY+(ci->field.y()*squareSize);
            } else {
                x = boardOffsetX+(ci->field.x()*squareSize);                
                y = boardOffsetY+((7-ci->field.y())*squareSize);
            }
            painter->setBrush(ci->color);
            painter->drawRect(x,y,squareSize,squareSize);
        }
    }


    // draw board coordinates
    painter->setPen(this->style->coordinateColor);
    painter->setFont(QFont(QString("Decorative"),8));

    for(int i=0;i<8;i++) {
        if(this->flipBoard) {
            QChar ch = QChar(((uint8_t) 65+(7-i)));
            QString idx = QString(ch);
            QString num = QString::number(i+1);

            painter->drawText(boardOffsetX+(i*squareSize) + (squareSize/2)-4,
                              boardOffsetY+(8*squareSize)+(this->borderWidth-3),idx);
            painter->drawText(4,boardOffsetY+(i*squareSize)+(squareSize/2)+4,num);
        } else {
            QChar ch = QChar(65+i);
            QString idx = QString(ch);
            QString num = QString::number(8-i);
            painter->drawText(boardOffsetX+(i*squareSize) + (squareSize/2)-4,
                              boardOffsetY+(8*squareSize)+(this->borderWidth-3),idx);
            painter->drawText(4,boardOffsetY+(i*squareSize)+(squareSize/2)+4,num);
        }
    }


    // draw arrows
    if(this->currentArrows != 0) {
        for(int i=0; i<this->currentArrows->size();i++) {
            chess::Arrow *ai = this->currentArrows->at(i);
            this->drawArrow(ai, boardOffsetX, boardOffsetY, squareSize, painter);
        }
    }

    // draw grabbed arrow (after drawing arrows, to overpaint)
    if(this->drawGrabbedArrow && this->grabbedArrow->from != QPoint(-1,-1)
            && this->grabbedArrow->to != QPoint(-1,-1) &&
           this->grabbedArrow->from != this->grabbedArrow->to) {
        this->drawArrow(this->grabbedArrow, boardOffsetX, boardOffsetY, squareSize, painter);
    }
}

void Chessboard::drawArrow(chess::Arrow *ai, int boardOffsetX,
                                    int boardOffsetY, int squareSize, QPainter *painter) {
    int x_from = 0;
    int x_to = 0;
    int y_from = 0;
    int y_to = 0;
    if(this->flipBoard) {
        x_from = boardOffsetX+((7-ai->from.x())*squareSize) + (squareSize/2);
        x_to = boardOffsetX+((7-ai->to.x())*squareSize) + (squareSize/2);
        y_from = boardOffsetY+((ai->from.y())*squareSize)+ (squareSize/2);
        y_to = boardOffsetY+((ai->to.y())*squareSize)+ (squareSize/2);
    } else {
        x_from = boardOffsetX+(ai->from.x()*squareSize)+ (squareSize/2);
        x_to = boardOffsetX+(ai->to.x()*squareSize)+ (squareSize/2);
        y_from = boardOffsetY+((7-ai->from.y())*squareSize)+ (squareSize/2);
        y_to = boardOffsetY+((7-ai->to.y())*squareSize)+ (squareSize/2);
    }

    //QPen pen = QPen(this->green, squareSize / 10);
    //painter->setPen(pen);

    // incredible annoying calculation to get arrow head
    QPoint fromPoint = QPoint(x_from, y_from);
    QPoint toPoint = QPoint(x_to, y_to);

    // added to toPoint to place arrow head
    // somewhere in the center
    float vx = -toPoint.x() + fromPoint.x();
    float vy = -toPoint.y() + fromPoint.y();

    // vectors correspond to the arrows
    float dx = toPoint.x() - fromPoint.x();
    float dy = toPoint.y() - fromPoint.y();

    float length = sqrt(dx * dx + dy * dy);

    float unitDx = dx / length;
    float unitDy = dy / length;

    // adjusted according to arrow length
    vx = vx * (squareSize/6 /length);
    vy = vy * (squareSize/6 /length);

    toPoint = QPoint(toPoint.x() - vx, toPoint.y() - vy );

    int arrowHeadBoxSize = squareSize/4;
    QPoint arrowPoint1 = QPoint(
                toPoint.x() - unitDx * arrowHeadBoxSize - unitDy * arrowHeadBoxSize,
                toPoint.y() - unitDy * arrowHeadBoxSize + unitDx * arrowHeadBoxSize);

    QPoint arrowPoint2 = QPoint(
                toPoint.x() - unitDx * arrowHeadBoxSize + unitDy * arrowHeadBoxSize,
                toPoint.y() - unitDy * arrowHeadBoxSize - unitDx * arrowHeadBoxSize);

    QPen pen = QPen(ai->color, 1);
    painter->setPen(pen);

    // draw arrow head
    QPainterPath path;
    path.moveTo(toPoint);
    path.lineTo(arrowPoint1);
    path.lineTo(arrowPoint2);
    path.lineTo(toPoint);

    painter->setRenderHint(QPainter::Antialiasing);
    painter->setPen (pen);
    painter->fillPath (path, QBrush(ai->color));

    pen = QPen(ai->color, squareSize/6);
    painter->setPen(pen);

    // take the old center coord to draw the
    // line to, so that the line doesn not
    // cover the arrow head due to the line's thickness
    QPoint to = QPoint(x_to, y_to);
    painter->drawLine(fromPoint,to);

}
