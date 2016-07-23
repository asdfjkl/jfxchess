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


#ifndef BOARDVIEWCONTROLLER_H
#define BOARDVIEWCONTROLLER_H

#include <QWidget>
#include "model/game_model.h"
#include "piece_images.h"
#include "chessboard.h"

class BoardViewController : public Chessboard
{
    Q_OBJECT
public:
    explicit BoardViewController(GameModel *gameModel = 0, QWidget *parent = 0);

private:

    QPoint *colorClickSrc;

    QColor transpRed;
    QColor red;
    QColor transpYellow;
    QColor yellow;
    QColor green;
    QColor transpGreen;
    QColor selArrowColor;
    QColor selFieldColor;

    bool colorClick;

    GameModel *gameModel;

    void drawBoard(QPaintEvent *e, QPainter *q);
    QPoint* getBoardPosition(int x, int y);
    uint8_t xyToBoardIdx(int x, int y);
    void touchPiece(int x, int y, int mouse_x, int mouse_y);
    void resetMove();
    void drawArrow(chess::Arrow *, int boardOffsetX, int boardOffsetY, int squareSize, QPainter *);
    void handleColoringOnKeyPress(QPoint *pos);
    void handleColoringonKeyRelease(QPoint *pos);
    void applyMove(chess::Move *m);

protected:
     void paintEvent(QPaintEvent *e);
     void resizeEvent(QResizeEvent *e);
     void mousePressEvent(QMouseEvent *m);
     void mouseMoveEvent(QMouseEvent *m);
     void mouseReleaseEvent(QMouseEvent *m);

signals:

public slots:
     void flipBoard();
     void onStateChange();

};

#endif // BOARDVIEWCONTROLLER_H
