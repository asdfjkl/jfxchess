#include "enterposboard.h"
#include <QPainter>
#include <QMouseEvent>
#include "assert.h"

EnterPosBoard::EnterPosBoard(ColorStyle *style, chess::Board *board, QWidget *parent) :
    Chessboard(parent)
{

    delete this->style;
    this->style = style;

    delete this->board;
    this->board = new chess::Board(board);

    this->currentGameBoard = new chess::Board(board);
    this->selectedPiece = chess::WHITE_PAWN;
}


void EnterPosBoard::paintEvent(QPaintEvent *e) {
    QPainter *painter = new QPainter();
    painter->begin(this);
    this->drawBoard(e, painter);
    painter->end();
}

void EnterPosBoard::resizeEvent(QResizeEvent *) {
    this->setMinimumWidth(this->height()*1.35);
}

void EnterPosBoard::mousePressEvent(QMouseEvent *m) {
    int x = m->x();
    int y = m->y();
    if(this->clickedOnPiceceSelector(x,y)) {
        this->selectedPiece = this->getSelectedPiece(x,y);
    } else if(this->clickedOnBoard(x,y)) {
        QPoint q = this->getBoardPosition(x,y);
        if(this->board->get_piece_at(q.x(), q.y()) == this->selectedPiece) {
            this->board->set_piece_at(q.x(),q.y(),chess::EMPTY);
            emit squareChanged();
        } else {
            this->board->set_piece_at(q.x(),q.y(),this->selectedPiece);
            emit squareChanged();
        }
    }
    this->update();
}

bool EnterPosBoard::clickedOnBoard(int x, int y) {
    int boardSize = 0;
    int squareSize = 0;
    this->calculateBoardSize(&boardSize, &squareSize);
    if(x > this->borderWidth && y > this->borderWidth
            && x < (boardSize - this->borderWidth)
            && y < (boardSize - this->borderWidth)) {
        return true;
    } else {
        return false;
    }
}

bool EnterPosBoard::clickedOnPiceceSelector(int x, int y) {
    int boardSize = 0;
    int squareSize = 0;
    this->calculateBoardSize(&boardSize, &squareSize);
    if(x > this->borderWidth + 9*squareSize
            && x < this->borderWidth + 11*squareSize
            && y > this->borderWidth
            && y < this->borderWidth + 6*squareSize) {
        return true;
    } else {
        return false;
    }
}

// get selected piece for mouse coordinates
// mouse coordinates must be on piece selector
uint8_t EnterPosBoard::getSelectedPiece(int x, int y) {
    int boardSize = 0;
    int squareSize = 0;
    this->calculateBoardSize(&boardSize, &squareSize);
    int x_idx = x - (this->borderWidth + 9*squareSize);
    int y_idx = y - this->borderWidth;
    x_idx = (x_idx / squareSize);
    y_idx = y_idx/squareSize;
    return this->pickupPieces[y_idx][x_idx];
}

// get board position for mouse coordinates
// board position is tuple (i,j) where i is x-axis from 0 to 7
// and y is y-axis
QPoint EnterPosBoard::getBoardPosition(int x, int y) {
    int boardSize = 0;
    int squareSize = 0;
    this->calculateBoardSize(&boardSize, &squareSize);
    int x_idx = x - this->borderWidth;
    int y_idx = y - this->borderWidth;
    x_idx = x_idx / squareSize;
    y_idx = 7 - (y_idx / squareSize);
    return QPoint(x_idx,y_idx);
}


void EnterPosBoard::calculateBoardSize(int *boardSize, int *squareSize) {

    QSize size = this->size();
    int bSize = std::min(size.width(), size.height());
    int sSize = (bSize-(2*this->borderWidth))/8;
    bSize = 8 * sSize + 2 * this->borderWidth;
    *boardSize = bSize;
    *squareSize = sSize;
}

void EnterPosBoard::setToInitialPosition() {
    delete this->board;
    this->board = new chess::Board(true);
    this->update();
}

void EnterPosBoard::setToCurrentBoard() {
    delete this->board;
    this->board = new chess::Board(this->currentGameBoard);
    this->update();
}

void EnterPosBoard::clearBoard() {
    delete this->board;
    this->board = new chess::Board();
    this->update();
}

chess::Board* EnterPosBoard::getCurrentBoard() {
    return this->board;
}

void EnterPosBoard::drawBoard(QPaintEvent *event, QPainter *painter) {

    Chessboard::drawBoard(event, painter);

    // to have no border color when drawing board squares and pieces
    QPen penZero = QPen(Qt::black, 1, Qt::NoPen);
    painter->setPen(penZero);
    painter->setBrush(this->style->borderColor);

    int boardSize = 0;
    int squareSize = 0;
    this->calculateBoardSize(&boardSize, &squareSize);

    painter->drawRect(9*squareSize,1,2*squareSize+2*this->borderWidth,6 * squareSize + 2 * this->borderWidth);

    int boardOffsetX = this->borderWidth;
    int boardOffsetY = this->borderWidth;

    QColor light = this->style->lightSquare;
    QColor dark = this->style->darkSquare;

    // draw the piece selection fields
    for(int i=0;i<6;i++) {
        for(int j=0;j<2;j++) {
            if(this->style->boardStyle == BOARD_STYLE_TEXTURE) {
                painter->setBrush(QBrush(this->style->lightSquareTexture));
            } else {
                painter->setBrush(light);
            }
            if(this->selectedPiece == this->pickupPieces[i][j]) {
                if(this->style->boardStyle == BOARD_STYLE_TEXTURE) {
                    painter->setBrush(QBrush(this->style->darkSquareTexture));
                } else {
                    painter->setBrush(dark);
                }
            }
            int x = boardOffsetX + ((9+j)*squareSize);
            int y = boardOffsetY + (i*squareSize);
            painter->drawRect(x,y,squareSize, squareSize);

            uint8_t piece_type = this->pickupPieces[i][j];
            //dirty hack
            if(piece_type > 0x80) {
                piece_type = piece_type - 0x80;
            }
            bool piece_color = chess::WHITE;
            if(j==1) {
                piece_color = chess::BLACK;
            }
            int pieceStyle = this->style->pieceType;

            QImage *piece_image = Chessboard::pieceImages->getPieceImage(piece_type, piece_color, squareSize, this->dpr, pieceStyle);
            assert(piece_image != 0);
            painter->drawImage(x,y,*piece_image);
        }
    }

}
