#include "pickcolorboard.h"
#include "QDebug"

PickColorBoard::PickColorBoard(QWidget *parent) :
    Chessboard(parent)
{

}

void PickColorBoard::paintEvent(QPaintEvent *e) {
    Chessboard::paintEvent(e);
}


void PickColorBoard::resizeEvent(QResizeEvent *e) {
    Chessboard::resizeEvent(e);
}

void PickColorBoard::setPieceType(int pieceType) {
    this->style->pieceType = pieceType;
}

void PickColorBoard::setBoardStyle(int styleType) {
    this->style->boardStyle = styleType;
}

void PickColorBoard::setBoardColors(QColor borderColor, QColor darkSquare,
                                        QColor lightSquare, QColor coordinates, int styleType) {
    this->style->borderColor = borderColor;
    this->style->darkSquare = darkSquare;
    this->style->lightSquare = lightSquare;
    this->style->coordinateColor = coordinates;
    this->style->styleType = styleType;
}

void PickColorBoard::setBoardColors(QColor borderColor, QPixmap darkSquare,
                                        QPixmap lightSquare, QColor coordinates, int styleType) {
    this->style->borderColor = borderColor;
    this->style->darkSquareTexture = darkSquare;
    this->style->lightSquareTexture = lightSquare;
    this->style->coordinateColor = coordinates;
    this->style->styleType = styleType;
}
