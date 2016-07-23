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


#ifndef DIALOG_PROMOTION_H
#define DIALOG_PROMOTION_H
#include "viewController/piece_images.h"
#include <QEvent>
#include <QDialog>


class DialogPromotion : public QDialog
{
    Q_OBJECT
public:
    explicit DialogPromotion(bool color, QWidget *parent = 0);
    int promotesTo;

private:
    int border;
    int pieceSize;
    bool color;
    int selectedIndex;
    PieceImages *img;
    void resizeTo(float ratio);
    void paintEvent(QPaintEvent *e);
    void mousePressEvent(QMouseEvent *m);
    void mouseReleaseEvent(QMouseEvent *m);
    void keyPressEvent(QKeyEvent *e);

    uint8_t piece_by_idx(int idx);
    uint8_t piecetype_by_idx(int idx);

};

#endif // DIALOG_PROMOTION_H
