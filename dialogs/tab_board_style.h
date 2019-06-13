#ifndef TAB_BOARD_STYLE_H
#define TAB_BOARD_STYLE_H

#include <QWidget>
#include "viewController/pickcolorboard.h"

class TabBoardStyle : public QWidget
{
    Q_OBJECT
public:
    explicit TabBoardStyle(ColorStyle *currentStyle, QWidget *parent = nullptr);
    PickColorBoard* displayBoard;

signals:

public slots:

    void onMeridaPieces();
    void onUSCFPieces();
    void onOldPieces();

    void onBlueColor();
    void onGreenColor();
    void onBrownColor();
    void onWood();
    void onBlueMarbles();
    void onGreenMarbles();

};

#endif // TAB_BOARD_STYLE_H
