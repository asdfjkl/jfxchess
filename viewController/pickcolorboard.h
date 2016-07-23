#ifndef GUI_OPTION_BOARD_H
#define GUI_OPTION_BOARD_H

#include "chessboard.h"

class PickColorBoard : public Chessboard
{
    Q_OBJECT

public:
    explicit PickColorBoard(QWidget* parent = 0);

    void setPieceType(int pieceType);
    void setBoardStyle(int styleType);
    void setBoardColors(QColor borderColor, QColor darkSquare, QColor lightSquare, QColor coordinates, int styleType);
    void setBoardColors(QColor borderColor, QPixmap darkSquare, QPixmap lightSquare, QColor coordinates, int styleType);

private:

protected:
    void paintEvent(QPaintEvent *e);
    void resizeEvent(QResizeEvent *e);

signals:

public slots:


};

#endif // GUI_OPTION_BOARD_H
