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


#include "colorstyle.h"
#include "various/resource_finder.h"
#include "various/messagebox.h"

ColorStyle::ColorStyle(QString resourcePath)
{
    this->resPath = resourcePath;
    this->boardStyle = BOARD_STYLE_COLOR;
    this->pieceType = PIECE_STYLE_MERIDA;

    this->styleType = STYLE_BLUE;

    this->borderColor = BORDER_BLUE;
    this->coordinateColor = COORDINATE_COLOR_BLUE;
    this->darkSquare = DARK_SQUARE_BLUE;
    this->lightSquare = LIGHT_SQUARE_BLUE;
    this->darkSquareTexture = QPixmap(resPath + DARK_SQUARE_MARBLE_BLUE);
    this->lightSquareTexture = QPixmap(resPath + LIGHT_SQUARE_MARBLE_BLUE);
}

void ColorStyle::setStyle(int styleType) {
    if(styleType == STYLE_BROWN) {
        this->boardStyle = BOARD_STYLE_COLOR;
        this->styleType = STYLE_BROWN;
        this->borderColor = BORDER_BROWN;
        this->coordinateColor = COORDINATE_COLOR_BROWN;
        this->darkSquare = DARK_SQUARE_BROWN;
        this->lightSquare = LIGHT_SQUARE_BROWN;
    } else if(styleType == STYLE_GREEN) {
        this->boardStyle = BOARD_STYLE_COLOR;
        this->styleType = STYLE_GREEN;
        this->borderColor = BORDER_GREEN;
        this->coordinateColor = COORDINATE_COLOR_GREEN;
        this->darkSquare = DARK_SQUARE_GREEN;
        this->lightSquare = LIGHT_SQUARE_GREEN;
    } else if(styleType == STYLE_MARBLE_GREEN) {
        this->boardStyle = BOARD_STYLE_TEXTURE;
        this->styleType = STYLE_MARBLE_GREEN;
        this->borderColor = BORDER_MARBLE_GREEN;
        this->coordinateColor = COORDINATE_COLOR_MARBLE_GREEN;
        this->darkSquare = DARK_SQUARE_MARBLE_GREEN;
        this->lightSquare = LIGHT_SQUARE_MARBLE_GREEN;
        this->darkSquareTexture = QPixmap(resPath + DARK_SQUARE_MARBLE_GREEN);
        this->lightSquareTexture = QPixmap(resPath + LIGHT_SQUARE_MARBLE_GREEN);
    } else if(styleType == STYLE_MARBLE_BLUE) {
        this->boardStyle = BOARD_STYLE_TEXTURE;
        this->styleType = STYLE_MARBLE_BLUE;
        this->borderColor = BORDER_MARLBE_BLUE;
        this->coordinateColor = COORDINATE_COLOR_MARBLE_BLUE;
        this->darkSquare = DARK_SQUARE_MARBLE_BLUE;
        this->lightSquare = LIGHT_SQUARE_MARBLE_BLUE;
        this->darkSquareTexture = QPixmap(resPath + DARK_SQUARE_MARBLE_BLUE);
        this->lightSquareTexture = QPixmap(resPath + LIGHT_SQUARE_MARBLE_BLUE);
    } else if(styleType == STYLE_WOOD) {
        this->boardStyle = BOARD_STYLE_TEXTURE;
        this->styleType = STYLE_WOOD;
        this->borderColor = BORDER_WOOD;
        this->coordinateColor = COORDINATE_COLOR_WOOD;
        this->darkSquare = DARK_SQUARE_WOOD;
        this->lightSquare = LIGHT_SQUARE_WOOD;
        this->darkSquareTexture = QPixmap(resPath + DARK_SQUARE_WOOD);
        this->lightSquareTexture = QPixmap(resPath + LIGHT_SQUARE_WOOD);
    } else {
        // if unknown or default, set to blue
        this->boardStyle = BOARD_STYLE_COLOR;
        this->styleType = STYLE_BLUE;
        this->borderColor = BORDER_BLUE;
        this->coordinateColor = COORDINATE_COLOR_BLUE;
        this->darkSquare = DARK_SQUARE_BLUE;
        this->lightSquare = LIGHT_SQUARE_BLUE;
    }
}
