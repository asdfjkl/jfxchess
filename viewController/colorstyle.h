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


#ifndef COLORSTYLE_H
#define COLORSTYLE_H
#include <QColor>
#include <QPixmap>

const int STYLE_BLUE = 0;
const int STYLE_BROWN = 1;
const int STYLE_GREEN = 2;
const int STYLE_WOOD = 3;
const int STYLE_MARBLE_GREEN = 4;
const int STYLE_MARBLE_BLUE = 5;

const int BOARD_STYLE_COLOR = 0;
const int BOARD_STYLE_TEXTURE = 1;

const int PIECE_STYLE_MERIDA = 0;
const int PIECE_STYLE_OLD = 1;
const int PIECE_STYLE_USCF = 2;

const QColor BORDER_BLUE = QColor(56,66,91);
const QColor DARK_SQUARE_BLUE = QColor(90,106,173);
const QColor LIGHT_SQUARE_BLUE = QColor(166,188,231);
const QColor COORDINATE_COLOR_BLUE = QColor(239,239,239);

const QColor BORDER_GREEN = QColor(72,81,63);
const QColor DARK_SQUARE_GREEN = QColor(118,150,86);
const QColor LIGHT_SQUARE_GREEN = QColor(238,238,210);
const QColor COORDINATE_COLOR_GREEN = QColor(239,239,239);

const QColor BORDER_BROWN = QColor(107,80,57);
const QColor DARK_SQUARE_BROWN = QColor(181,136,99);
const QColor LIGHT_SQUARE_BROWN = QColor(240,217,181);
const QColor COORDINATE_COLOR_BROWN = QColor(239,239,239);

const QColor BORDER_WOOD = QColor(107,80,57);
const QString DARK_SQUARE_WOOD = QString("res/board/wood_dark.jpg");
const QString LIGHT_SQUARE_WOOD = QString("res/board/wood_light.jpg");
const QColor COORDINATE_COLOR_WOOD = QColor(239,239,239);

const QColor BORDER_MARBLE_GREEN = QColor(72,81,63);
const QString DARK_SQUARE_MARBLE_GREEN = QString("res/board/marble_green_dark.jpg");
const QString LIGHT_SQUARE_MARBLE_GREEN = QString("res/board/marble_green_light.jpg");
const QColor COORDINATE_COLOR_MARBLE_GREEN = QColor(239,239,239);

const QColor BORDER_MARLBE_BLUE = QColor(56,66,91);
const QString DARK_SQUARE_MARBLE_BLUE = QString("res/board/marble_blue_dark.jpg");
const QString LIGHT_SQUARE_MARBLE_BLUE = QString("res/board/marble_blue_light.jpg");
const QColor COORDINATE_COLOR_MARBLE_BLUE = QColor(239,239,239);


class ColorStyle
{
public:
    ColorStyle(QString resourcePath);

    QColor borderColor;
    QColor darkSquare;
    QColor lightSquare;
    QColor coordinateColor;
    QPixmap darkSquareTexture;
    QPixmap lightSquareTexture;

    QString resPath;
    int boardStyle;
    int pieceType;
    int styleType;

    void setStyle(int styleType);
};

#endif // COLORSTYLE_H
