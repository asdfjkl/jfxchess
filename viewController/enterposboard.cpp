#include "enterposboard.h"
#include <QPainter>
#include <QMouseEvent>
#include "assert.h"
#include <QDebug>
#include "various/resource_finder.h"


EnterPosBoard::EnterPosBoard(const ColorStyle &style,
                             const chess::Board &currentBoard,
                             QWidget *parent, bool incl_joker_piece) :
    QWidget(parent),
    style(style),
    currentGameBoard(currentBoard)
{

    QSizePolicy policy = QSizePolicy(QSizePolicy::Expanding, QSizePolicy::Expanding);
    this->setSizePolicy(policy);

    this->borderWidth = 12;
    this->style = style;
    this->pieceImages = new PieceImages(ResourceFinder::getPath());

    this->selectedPiece = chess::WHITE_PAWN;

    this->incl_joker_piece = incl_joker_piece;
}


void EnterPosBoard::setTurn(bool turn) {
    if(turn) {
        this->board.turn = chess::WHITE;
    } else {
        this->board.turn = chess::BLACK;
    }
}

void EnterPosBoard::setCastlingRights(bool wking, bool wqueen, bool bking, bool bqueen) {

if(wking) {
this->board.set_castle_wking(true);
} else {
this->board.set_castle_wking(false);
}

if(wqueen) {
this->board.set_castle_wqueen(true);
} else {
this->board.set_castle_wqueen(false);
}

if(bking) {
this->board.set_castle_bking(true);
} else {
this->board.set_castle_bking(false);
}

if(bqueen) {
this->board.set_castle_bqueen(true);
} else {
this->board.set_castle_bqueen(false);
}

}

void EnterPosBoard::calculateBoardSize(int *boardSize, int *squareSize) {

    int bSize = this->width();
    if(this->height() < bSize) {
        bSize = this->height();
    }
    int sSize = qMax((bSize-(2*this->borderWidth))/8,1);
    bSize = qMax(8 * sSize + 2 * this->borderWidth,1);

    *boardSize = bSize;
    *squareSize = sSize;
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
        if(this->board.get_piece_at(q.x(), q.y()) == this->selectedPiece) {
            this->board.set_piece_at(q.x(),q.y(),chess::EMPTY);
            emit squareChanged();
        } else {
            this->board.set_piece_at(q.x(),q.y(),this->selectedPiece);
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
    int idx_pieces = 6;
    if(this->incl_joker_piece) {
        idx_pieces = 7;
    }
    if(x > this->borderWidth + 9*squareSize
            && x < this->borderWidth + 11*squareSize
            && y > this->borderWidth
            && y < this->borderWidth + idx_pieces*squareSize) {
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


/*
void EnterPosBoard::calculateBoardSize(int *boardSize, int *squareSize) {

    QSize size = this->size();
    int bSize = std::min(size.width(), size.height());
    int sSize = (bSize-(2*this->borderWidth))/8;
    bSize = 8 * sSize + 2 * this->borderWidth;
    *boardSize = bSize;
    *squareSize = sSize;
}*/

void EnterPosBoard::setToInitialPosition() {
    this->board = chess::Board(true);
    this->update();
}

void EnterPosBoard::setToCurrentBoard() {
    this->board = this->currentGameBoard;
    this->update();
}

void EnterPosBoard::clearBoard() {
    this->board = chess::Board();
    this->update();
}

chess::Board EnterPosBoard::getCurrentBoard() {
    return this->board;
}

void EnterPosBoard::drawBoard(QPaintEvent *event, QPainter *painter) {

    // to have no border color when drawing board squares and pieces
    QPen penZero = QPen(Qt::black, 1, Qt::NoPen);
    painter->setPen(penZero);
    painter->setBrush(this->style.borderColor);

    int boardSize = 0;
    int squareSize = 0;
    this->calculateBoardSize(&boardSize, &squareSize);

    painter->drawRect(1,1, boardSize, boardSize);

    int boardOffsetX = this->borderWidth;
    int boardOffsetY = this->borderWidth;

    QColor light = this->style.lightSquare;
    QColor dark = this->style.darkSquare;

    QPixmap pxLight = this->style.lightSquareTexture;
    QPixmap pxDark = this->style.darkSquareTexture;

    //chess::Board board = this->board;

    for(int i=0;i<8;i++) {
        for(int j=0;j<8;j++) {
            // draw alternatively light and dark squares
            if((j%2 == 0 && i%2==1) || (j%2 == 1 && i%2==0)) {
                if(this->style.boardStyle == BOARD_STYLE_TEXTURE) {
                    painter->setBrush(QBrush(pxLight));
                } else {
                    painter->setBrush(light);
                }
            } else {
                if(this->style.boardStyle == BOARD_STYLE_TEXTURE) {
                    painter->setBrush(QBrush(pxDark));
                } else {
                    painter->setBrush(dark);
                }
            }
            // draw the square
            int x = boardOffsetX+(i*squareSize);
            // drawing coordinates are from top left
            // whereas chess coords are from bottom left
            int y = boardOffsetY+((7-j)*squareSize);
            painter->drawRect(x,y,squareSize,squareSize);
        }
    }

    for(int i=0;i<8;i++) {
        for(int j=0;j<8;j++) {
            // get square coords
            int x = boardOffsetX+(i*squareSize);

            // drawing coordinates are from top left
            // whereas chess coords are from bottom left
            int y = boardOffsetY+((7-j)*squareSize);

            // draw the pieces
            uint8_t piece_type = 0;
            bool piece_color = 0;

            piece_type = board.get_piece_type_at(i,j);
            piece_color = board.get_piece_color_at(i,j);

            int pieceStyle = this->style.pieceType;
            if(piece_type != chess::EMPTY)  {
                QImage *piece_image = this->pieceImages->getPieceImage(piece_type, piece_color, squareSize, this->dpr, pieceStyle);
                painter->drawImage(x,y,*piece_image);
            }
        }
    }

    // draw board coordinates
    painter->setPen(penZero);
    painter->setBrush(this->style.borderColor);

    painter->setFont(QFont(QString("Decorative"),8));

    for(int i=0;i<8;i++) {
        QChar ch = QChar(65+i);
        QString idx = QString(ch);
        QString num = QString::number(8-i);
        painter->drawText(boardOffsetX+(i*squareSize) + (squareSize/2)-4,
                          boardOffsetY+(8*squareSize)+(this->borderWidth-3),idx);
        painter->drawText(4,boardOffsetY+(i*squareSize)+(squareSize/2)+4,num);
    }

    if(this->incl_joker_piece) {
        painter->drawRect(9*squareSize,1,2*squareSize+2*this->borderWidth,7 * squareSize + 2 * this->borderWidth);
    } else {
        painter->drawRect(9*squareSize,1,2*squareSize+2*this->borderWidth,6 * squareSize + 2 * this->borderWidth);
    }

    // consider draw the selection icon for the "joker" pieces
    // (i.e. any piece for black or white) only if this is
    // class instance is configured like that
    int max_piece_idx = 6;
    if(this->incl_joker_piece) {
        max_piece_idx = 7;
    }

    // draw the piece selection fields
    painter->setPen(penZero);

    for(int i=0;i<max_piece_idx;i++) {
        for(int j=0;j<2;j++) {
            if(this->style.boardStyle == BOARD_STYLE_TEXTURE) {
                painter->setBrush(QBrush(this->style.lightSquareTexture));
            } else {
                painter->setBrush(light);
            }
            if(this->selectedPiece == this->pickupPieces[i][j]) {
                if(this->style.boardStyle == BOARD_STYLE_TEXTURE) {
                    painter->setBrush(QBrush(this->style.darkSquareTexture));
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
            int pieceStyle = this->style.pieceType;

            QImage *piece_image = this->pieceImages->getPieceImage(piece_type, piece_color, squareSize, this->dpr, pieceStyle);
            assert(piece_image != 0);
            painter->drawImage(x,y,*piece_image);
        }
    }

}
